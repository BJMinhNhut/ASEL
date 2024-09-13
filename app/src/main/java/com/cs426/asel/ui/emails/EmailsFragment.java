package com.cs426.asel.ui.emails;

import android.app.AlertDialog;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Obtain EmailsViewModel from the activity's ViewModelProvider
        binding = FragmentEmailsBinding.inflate(inflater, container, false);
        userEmail = Utility.getUserEmail(requireContext());
        mailRepository = new MailRepository(requireContext(), userEmail);
        eventRepository = new EventRepository(requireContext(), userEmail);
        emailsViewModel = new ViewModelProvider(requireActivity()).get(EmailsViewModel.class);
        emailsViewModel.fetchAllEmailsID();
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

        final Observer<Boolean> loadObserver = new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean) {
                    showLoadIndicator();
                } else {
                    Log.d("EmailsFragment", "Hiding load");
                    MailList newMails = emailsViewModel.getMailListFrom(unread.size());
                    adapter.appendList(newMails);
                    unread.append(newMails);
                    hideLoadIndicator();
                }
            }
        };

        emailsViewModel.getIsLoading().observe(getViewLifecycleOwner(), loadObserver);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View root = binding.getRoot();
        emailListRecyclerView = root.findViewById(R.id.email_list_recycler_view);
        emailListRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        emailListRecyclerView.addItemDecoration(new SpaceItemDecoration(20));
        emailListRecyclerView.setAdapter(adapter);
        emailListRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (!recyclerView.canScrollVertically(1)
                        && binding.emailsTab.getSelectedTabPosition() == 0) {
                    emailsViewModel.loadMoreEmails();
                }
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

                            //TODO: publish event of mail

                            Snackbar.make(emailListRecyclerView, "Event of email added to calendar", Snackbar.LENGTH_LONG)
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

                                        // TODO: unpublish event of mail
                                    }).show();
                        }
                    }
                };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeCallback);
        itemTouchHelper.attachToRecyclerView(emailListRecyclerView);
    }

    @Override
    public void onResume() {
        super.onResume();
        emailsViewModel.fetchAllEmailsID();
    }

    private void hideLoadIndicator() {
        binding.loadingIndicator.setVisibility(View.GONE);
        binding.emailListRecyclerView.setVisibility(View.VISIBLE);
    }

    private void showLoadIndicator() {
        binding.loadingIndicator.setVisibility(View.VISIBLE);
        binding.emailListRecyclerView.setVisibility(View.GONE);
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

    class EmailListAdapter extends RecyclerView.Adapter<EmailListAdapter.EmailViewHolder> {
        private MailList mailList;

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

            notifyItemRangeChanged(position, position + newMailList.size());
        }

        public void removeMail(int position) {
            mailList.removeMail(position);
            notifyDataSetChanged();
        }

        public void insertMail(Mail mail, int position) {
            mailList.insertMailAt(mail, position);
            notifyItemInserted(position);
        }

        @NonNull
        @Override
        public EmailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_email, parent, false);
            return new EmailViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull EmailViewHolder holder, int position) {
            holder.itemView.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putString("emailId", mailList.getMail(position).getId()); // Replace 1 with the actual email ID you want to pass

                FragmentTransaction ft = getParentFragmentManager().beginTransaction();
                EmailDetailFragment fragment = new EmailDetailFragment();
                fragment.setArguments(bundle);
                ft.replace(R.id.emailsContainer, fragment).addToBackStack(null).commit();
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

                holder.eventTime.setText(eventTime);
                holder.eventTime.setVisibility(View.VISIBLE);
                holder.itemView.findViewById(R.id.time_icon).setVisibility(View.VISIBLE);
                holder.itemView.findViewById(R.id.divider).setVisibility(View.VISIBLE);


                if (mailList.getMail(position).getLocation() != null) {
                    holder.place.setText(mailList.getMail(position).getLocation());
                    holder.place.setVisibility(View.VISIBLE);
                    holder.itemView.findViewById(R.id.location_icon).setVisibility(View.VISIBLE);
                } else {
                    holder.place.setVisibility(View.GONE);
                    holder.itemView.findViewById(R.id.location_icon).setVisibility(View.GONE);
                }
            } else {
                holder.itemView.findViewById(R.id.divider).setVisibility(View.GONE);
                holder.eventTime.setVisibility(View.GONE);
                holder.place.setVisibility(View.GONE);
                holder.itemView.findViewById(R.id.location_icon).setVisibility(View.GONE);
                holder.itemView.findViewById(R.id.time_icon).setVisibility(View.GONE);
            }

            holder.senderName.setText(senderName);
            holder.senderDomain.setText(senderDomain);
            holder.title.setText(mailList.getMail(position).getTitle());
            holder.sendTime.setText(mailList.getMail(position).getSentTime());
            holder.tag.setText(mailList.getMail(position).getTag());
            holder.summary.setText(mailList.getMail(position).getSummary());
        }

        @Override
        public int getItemCount() {
            // Update to reflect actual number of emails from ViewModel or LiveData
            return mailList.size();
        }

        class EmailViewHolder extends RecyclerView.ViewHolder {
            TextView senderName;
            TextView senderDomain;
            TextView title;
            TextView sendTime;
            TextView eventTime;
            TextView place;
            TextView summary;
            TextView tag;

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
    }
}
