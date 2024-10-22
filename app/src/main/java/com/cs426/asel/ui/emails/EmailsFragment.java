package com.cs426.asel.ui.emails;

import android.app.AlertDialog;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cs426.asel.R;
import com.cs426.asel.backend.EventRepository;
import com.cs426.asel.backend.Mail;
import com.cs426.asel.backend.MailList;
import com.cs426.asel.backend.MailRepository;
import com.cs426.asel.backend.Utility;
import com.cs426.asel.databinding.FragmentEmailsBinding;
import com.cs426.asel.ui.account.AccountViewModel;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class EmailsFragment extends Fragment {
    private MailList unread;
    private MailList read;
    private MailRepository mailRepository;
    private EventRepository eventRepository;
    private FragmentEmailsBinding binding;
    private RecyclerView emailListRecyclerView;
    private EmailsViewModel emailsViewModel; // Reference to the shared ViewModel
    private EmailListAdapter adapter;
    private SwipeCallback swipeCallback;
    private String userEmail;

    private int removedIndex = -1;
    private Mail removedMail;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!emailsViewModel.isFetchStarted()) {
            emailsViewModel.fetchAllEmailsID();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Obtain EmailsViewModel from the activity's ViewModelProvider
        Log.d("EmailsFragment", "onCreateView");
        binding = FragmentEmailsBinding.inflate(inflater, container, false);
        userEmail = Utility.getUserEmail(requireContext());
        mailRepository = new MailRepository(requireContext(), userEmail);
        eventRepository = new EventRepository(requireContext(), userEmail);
        emailsViewModel = new ViewModelProvider(requireActivity()).get(EmailsViewModel.class);
        emailsViewModel.fetchNextIdBatch();
        adapter = new EmailListAdapter();
        unread = new MailList();
        read = mailRepository.getMailByRead(true, "send_time", false);

        binding.emailsTab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        adapter.setMailList(unread);
                        swipeCallback.setSwipeEnabled(true);
                        break;
                    case 1:
                        adapter.setMailList(read);
                        swipeCallback.setSwipeEnabled(false);
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        final Observer<Boolean> loadObserver = aBoolean -> {
            if (aBoolean) {
                // Show loading indicator at the bottom of the list
                adapter.setLoading(true);
            } else {
                // Hide loading indicator
                adapter.setLoading(false);

                MailList newMails = emailsViewModel.getMailListFrom(unread.size());
                adapter.appendList(newMails);
                unread.append(newMails);
            }
        };

        emailsViewModel.getIsLoading().observe(getViewLifecycleOwner(), loadObserver);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View root = binding.getRoot();

        binding.infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] tags = {"Assignment", "Exam", "Meeting", "Course Material", "Other", "Spam"};
                int[] colors = {
                        ResourcesCompat.getColor(getResources(), R.color.tag_assignment, null),
                        ResourcesCompat.getColor(getResources(), R.color.tag_exam, null),
                        ResourcesCompat.getColor(getResources(), R.color.tag_meeting, null),
                        ResourcesCompat.getColor(getResources(), R.color.tag_course_material, null),
                        ResourcesCompat.getColor(getResources(), R.color.tag_other, null),
                        ResourcesCompat.getColor(getResources(), R.color.tag_spam, null)
                };

                LinearLayout layout = new LinearLayout(getContext());
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(20, 20, 20, 20);

                for (int i = 0; i < tags.length; i++) {
                    LinearLayout rowLayout = new LinearLayout(getContext());
                    rowLayout.setOrientation(LinearLayout.HORIZONTAL);
                    rowLayout.setPadding(20, 20, 20, 20);
                    rowLayout.setGravity(Gravity.CENTER_VERTICAL);

                    // Create the circle (using a View)
                    CardView circleView = new CardView(getContext());
                    LinearLayout.LayoutParams circleParams = new LinearLayout.LayoutParams(50, 50);
                    circleParams.setMargins(0, 0, 20, 0);
                    circleView.setLayoutParams(circleParams);
                    circleView.setRadius(15);
                    circleView.setCardElevation(0);
                    circleView.setCardBackgroundColor(colors[i]);

                    TextView textView = new TextView(getContext());
                    textView.setText(tags[i]);
                    textView.setTextAppearance(R.style.body_m);
                    textView.setTextColor(ResourcesCompat.getColor(getResources(), R.color.dark_darkest, null));

                    rowLayout.addView(circleView);
                    rowLayout.addView(textView);

                    layout.addView(rowLayout);
                }

                new AlertDialog.Builder(getContext())
                        .setTitle("Tag color descriptions:")
                        .setView(layout)
                        .setPositiveButton("OK", null)
                        .show();
            }
        });

        emailListRecyclerView = binding.emailListRecyclerView;
        emailListRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        emailListRecyclerView.addItemDecoration(new SpaceItemDecoration(30));
        emailListRecyclerView.setAdapter(adapter);
        emailListRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (!recyclerView.canScrollVertically(1)
                        && binding.emailsTab.getSelectedTabPosition() == 0) {
                    Log.d("EmailsFragment", "Loading more emails");
                    emailsViewModel.loadMoreEmails();
                }
            }
        });

        emailListRecyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                int action = e.getAction();
                if (action == MotionEvent.ACTION_MOVE) {
                    rv.getParent().requestDisallowInterceptTouchEvent(true);
                }

                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {

            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

            }
        });

        // Swipe to delete or quick add
        swipeCallback =
                new SwipeCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        if (!swipeCallback.isSwipeEnabled()) {
                            return;
                        }

                        if (direction == ItemTouchHelper.LEFT) {
                            // Left swipe to delete
                            AlertDialog dialog = new AlertDialog.Builder(getContext())
                                    .setTitle("Delete Email")
                                    .setMessage("Are you sure you want to delete this email?")
                                    .create();

                            dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes", (dialog1, which) -> {
                                // Delete email (implement the delete logic here)
                                Mail mail = unread.getMail(viewHolder.getAdapterPosition());

                                removedIndex = viewHolder.getAdapterPosition();
                                removedMail = mail;

                                unread.removeMail(removedIndex);
                                adapter.removeMail(removedIndex);
                                read.addMail(mail);

                                mailRepository.updateRead(removedMail.getId(), true);

                                dialog.dismiss();
                            });

                            dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No", (dialog12, which) -> {
                                dialog.dismiss();
                                adapter.notifyItemChanged(viewHolder.getAdapterPosition());
                            });

                            dialog.show();
                        } else if (direction == ItemTouchHelper.RIGHT) {
                            // Right swipe to quick add
                            Mail mail = unread.getMail(viewHolder.getBindingAdapterPosition());
                            int eventId = mail.getEvent().getID();
                            Log.d("EmailsFragment", "Publishing event ID: " + eventId);
                            eventRepository.setPublishEvent(eventId, true);
                            moveMailToRead(viewHolder.getBindingAdapterPosition(), mail);

                            Snackbar snackbar = Snackbar.make(emailListRecyclerView, "Event of email added to calendar", Snackbar.LENGTH_LONG)
                                    .setAction("Undo", v -> {
                                        // Undo the action (implement undo logic here)
                                        read.removeMail(read.size() - 1);
                                        unread.insertMailAt(removedMail, removedIndex);
                                        if (binding.emailsTab.getSelectedTabPosition() == 0) {
                                            adapter.insertMail(removedMail, removedIndex);
                                        } else {
                                            adapter.removeMail(adapter.mailList.size() - 1);
                                        }
                                        mailRepository.updateRead(removedMail.getId(), false);
                                    });

                            View snackbarView = snackbar.getView();
                            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) snackbarView.getLayoutParams();
                            params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, params.bottomMargin + 180); // Adjust the bottom margin as needed
                            snackbarView.setLayoutParams(params);

                            snackbar.show();
                        }
                    }
                };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeCallback);
        itemTouchHelper.attachToRecyclerView(emailListRecyclerView);
    }

    private void moveMailToRead(int index, Mail mail) {
        removedMail = mail;
        removedIndex = index;

        unread.removeMail(removedIndex);
        adapter.removeMail(removedIndex);
        read.addMail(removedMail);
        mailRepository.updateRead(removedMail.getId(), true);
    }

    public static class SpaceItemDecoration extends RecyclerView.ItemDecoration {
        private final int verticalSpaceHeight;

        public SpaceItemDecoration(int verticalSpaceHeight) {
            this.verticalSpaceHeight = verticalSpaceHeight;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            outRect.bottom = verticalSpaceHeight;
        }
    }

    class EmailListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int VIEW_TYPE_ITEM = 0;
        private static final int VIEW_TYPE_LOADING = 1;
        private MailList mailList;
        private boolean isLoading = false;

        public EmailListAdapter() {
            mailList = new MailList();
        }

        public void setMailList(MailList mailList) {
            this.mailList = mailList;
            notifyDataSetChanged();
        }

        public void appendList(MailList newMailList) {
            int position = this.mailList.size();
            this.mailList.append(newMailList);
            Log.d("EmailListAdapter", "Appending list from" + position + " to " + (position + newMailList.size()));
            notifyItemRangeInserted(position, newMailList.size());
        }

        public void removeMail(int position) {
            mailList.removeMail(position);
            notifyItemRemoved(position);
        }

        public void insertMail(Mail mail, int position) {
            mailList.insertMailAt(mail, position);
            notifyItemInserted(position);
        }

        // Set the loading state
        public void setLoading(boolean isLoading) {
            this.isLoading = isLoading;
            if (isLoading) {
                notifyItemInserted(mailList.size());
            } else {
                notifyItemRemoved(mailList.size());
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_ITEM) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_email, parent, false);
                return new EmailViewHolder(view);
            } else {
                // Inflate a progress bar layout for loading view
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_loading, parent, false);
                return new LoadingViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            // No binding logic needed for LoadingViewHolder (loading spinner)
            if (holder instanceof LoadingViewHolder) {
                return;
            }

            // cast the holder to EmailViewHolder
            EmailViewHolder emailHolder = (EmailViewHolder) holder;
            emailHolder.itemView.setOnClickListener(v -> {
                int curPosition = holder.getBindingAdapterPosition();
                Bundle bundle = new Bundle();
                bundle.putString("emailId", mailList.getMail(curPosition).getId()); // Replace 1 with the actual email ID you want to pass
                FragmentTransaction ft = getParentFragmentManager().beginTransaction();
                EmailDetailFragment fragment = new EmailDetailFragment();
                fragment.setArguments(bundle);
                ft.replace(R.id.emailsContainer, fragment).addToBackStack(null).commit();

                if (!mailList.getMail(curPosition).isRead()) {
                    removedIndex = curPosition;
                    removedMail = mailList.getMail(holder.getBindingAdapterPosition());
                    moveMailToRead(removedIndex, removedMail);
                }
            });

            String sender = mailList.getMail(position).getSender();
            String senderName = sender.substring(0, sender.indexOf("@"));
            String senderDomain = sender.substring(sender.indexOf("@"));

            Instant eventStartTime = mailList.getMail(position).getEventStartTime();
            int eventDuration = mailList.getMail(position).getEventDuration();
            if (eventStartTime != null) {
                String eventTime = eventStartTime.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"));
                if (eventDuration != 0) {
                    Instant eventEndTime = eventStartTime.plusSeconds(eventDuration * 60);
                    eventTime += " - " + eventEndTime.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"));
                }

                emailHolder.eventTime.setText(eventTime);
                emailHolder.eventTime.setVisibility(View.VISIBLE);
                emailHolder.itemView.findViewById(R.id.time_icon).setVisibility(View.VISIBLE);
                emailHolder.itemView.findViewById(R.id.divider).setVisibility(View.VISIBLE);


                if (mailList.getMail(position).getLocation() != null) {
                    emailHolder.place.setText(mailList.getMail(position).getLocation());
                    emailHolder.place.setVisibility(View.VISIBLE);
                    emailHolder.itemView.findViewById(R.id.location_icon).setVisibility(View.VISIBLE);
                } else {
                    emailHolder.place.setVisibility(View.GONE);
                    emailHolder.itemView.findViewById(R.id.location_icon).setVisibility(View.GONE);
                }
            } else {
                emailHolder.itemView.findViewById(R.id.divider).setVisibility(View.GONE);
                emailHolder.eventTime.setVisibility(View.GONE);
                emailHolder.place.setVisibility(View.GONE);
                emailHolder.itemView.findViewById(R.id.location_icon).setVisibility(View.GONE);
                emailHolder.itemView.findViewById(R.id.time_icon).setVisibility(View.GONE);
            }

            String tag = mailList.getMail(position).getTag();
            if (Objects.equals(tag, "Assignment")) {
                emailHolder.tag.setBackground(ResourcesCompat.getDrawable(getResources(), R.color.tag_assignment, null));
            } else if (Objects.equals(tag, "Exam")) {
                emailHolder.tag.setBackground(ResourcesCompat.getDrawable(getResources(), R.color.tag_exam, null));
            } else if (Objects.equals(tag, "Meeting")) {
                emailHolder.tag.setBackground(ResourcesCompat.getDrawable(getResources(), R.color.tag_meeting, null));
            } else if (Objects.equals(tag, "Course Material")) {
                emailHolder.tag.setBackground(ResourcesCompat.getDrawable(getResources(), R.color.tag_course_material, null));
            } else if (Objects.equals(tag, "Other")) {
                emailHolder.tag.setBackground(ResourcesCompat.getDrawable(getResources(), R.color.tag_other, null));
            } else {
                emailHolder.tag.setBackground(ResourcesCompat.getDrawable(getResources(), R.color.tag_spam, null));
            }

            emailHolder.senderName.setText(senderName);
            emailHolder.senderDomain.setText(senderDomain);
            emailHolder.title.setText(mailList.getMail(position).getTitle());
            emailHolder.sendTime.setText(mailList.getMail(position).getSentTime());
            emailHolder.summary.setText(mailList.getMail(position).getSummary());
        }

        @Override
        public int getItemViewType(int position) {
            // Check if the position is for the loading view
            return (position == mailList.size()) ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
        }

        @Override
        public int getItemCount() {
            // Include an extra item for the loading indicator if isLoading is true
            return mailList.size() + (isLoading ? 1 : 0);
        }


        class EmailViewHolder extends RecyclerView.ViewHolder {
            TextView senderName;
            TextView senderDomain;
            TextView title;
            TextView sendTime;
            TextView eventTime;
            TextView place;
            TextView summary;
            View tag;

            public EmailViewHolder(@NonNull View itemView) {
                super(itemView);
                senderName = itemView.findViewById(R.id.sender_name);
                senderDomain = itemView.findViewById(R.id.sender_domain);
                title = itemView.findViewById(R.id.subject);
                sendTime = itemView.findViewById(R.id.send_time);
                eventTime = itemView.findViewById(R.id.time);
                place = itemView.findViewById(R.id.location);
                summary = itemView.findViewById(R.id.summary);
                tag = itemView.findViewById(R.id.tag);
            }
        }

        // ViewHolder for loading indicator
        class LoadingViewHolder extends RecyclerView.ViewHolder {
            public LoadingViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }
}
