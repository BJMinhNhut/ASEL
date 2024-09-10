package com.cs426.asel.ui.emails;

import android.app.AlertDialog;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.cs426.asel.MainActivity;
import com.cs426.asel.R;
import com.cs426.asel.backend.Mail;
import com.cs426.asel.backend.MailList;
import com.cs426.asel.databinding.FragmentEmailsBinding;
import com.cs426.asel.databinding.NewEmailItemBinding;
import com.google.android.material.snackbar.Snackbar;

public class EmailsFragment extends Fragment {
    private MailList unread;
    private MailList read;
    private FragmentEmailsBinding binding;
    private RecyclerView emailListRecyclerView;
    private EmailsViewModel emailsViewModel; // Reference to the shared ViewModel

    private int removedIndex = -1;
    private Mail removedMail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Obtain EmailsViewModel from the activity's ViewModelProvider
        emailsViewModel = new ViewModelProvider(requireActivity()).get(EmailsViewModel.class);
        binding = FragmentEmailsBinding.inflate(inflater, container, false);

        emailsViewModel.fetchEmails();
        read = new MailList(); // TODO: Initialize read emails

        binding.mailsRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.new_mail_radio_button) {
                EmailListAdapter adapter = new EmailListAdapter();
                adapter.setMailList(unread);
                adapter.notifyDataSetChanged();
            } else {
                EmailListAdapter adapter = new EmailListAdapter();
                adapter.setMailList(read);
                adapter.notifyDataSetChanged();
            }
        });

        binding.filterButton.setOnClickListener(v -> {
            // TODO: Implement filter logic
        });

        final Observer<Boolean> loadObserver = new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean) {
                    showLoadIndicator();
                } else {
                    Log.d("EmailsFragment", "Hiding load");

                    unread = emailsViewModel.getMailList();
                    EmailListAdapter adapter = new EmailListAdapter();
                    adapter.setMailList(unread);
                    emailListRecyclerView.setAdapter(adapter);

                    hideLoadIndicator();
                }
            }
        };

        emailsViewModel.getIsLoading().observe(getViewLifecycleOwner(), loadObserver);

        return binding.getRoot();
    }

    private void hideLoadIndicator() {
        binding.loadingIndicator.setVisibility(View.GONE);
        binding.emailListRecyclerView.setVisibility(View.VISIBLE);
    }

    private void showLoadIndicator() {
        binding.loadingIndicator.setVisibility(View.VISIBLE);
        binding.emailListRecyclerView.setVisibility(View.GONE);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View root = binding.getRoot();
        EmailListAdapter adapter = new EmailListAdapter();
        emailListRecyclerView = root.findViewById(R.id.email_list_recycler_view);
        emailListRecyclerView.setAdapter(adapter);

        // Swipe to delete or quick add
        SwipeCallback swipeCallback =
                new SwipeCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        if (direction == ItemTouchHelper.LEFT) {
                            // Left swipe to delete
                            AlertDialog dialog = new AlertDialog.Builder(getContext())
                                    .setTitle("Delete Email")
                                    .setMessage("Are you sure you want to delete this email?")
                                    .create();

                            dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes", (dialog1, which) -> {
                                // Delete email (implement the delete logic here)
                                Mail mail = read.getMail(viewHolder.getAdapterPosition());
                                unread.addMail(mail);
                                read.addMail(mail);

                                removedIndex = viewHolder.getAdapterPosition();
                                removedMail = mail;

                                adapter.notifyItemRemoved(viewHolder.getAdapterPosition());
                                dialog.dismiss();
                            });

                            dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No", (dialog12, which) -> {
                                dialog.dismiss();
                                adapter.notifyItemChanged(viewHolder.getAdapterPosition());

                            });

                            dialog.show();
                        } else if (direction == ItemTouchHelper.RIGHT) {
                            // Right swipe to quick add

                            adapter.notifyItemRemoved(viewHolder.getAdapterPosition());

                            Snackbar.make(emailListRecyclerView, "Event of email added to calendar", Snackbar.LENGTH_LONG)
                                    .setAction("Undo", v -> {
                                        // Undo the action (implement undo logic here)
                                        adapter.notifyItemInserted(viewHolder.getAdapterPosition());
                                    }).show();
                        }
                    }
                };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeCallback);
        itemTouchHelper.attachToRecyclerView(emailListRecyclerView);
    }

    public void updateEmailList(MailList mailList) {
        if (binding.mailsRadioGroup.getCheckedRadioButtonId() == R.id.new_mail_radio_button) {
            unread.append(mailList);
            EmailListAdapter adapter = new EmailListAdapter();
            adapter.setMailList(unread);
            emailListRecyclerView.setAdapter(adapter);
        } else {
            read.append(mailList);
            EmailListAdapter adapter = new EmailListAdapter();
            adapter.setMailList(read);
            emailListRecyclerView.setAdapter(adapter);
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

        public void appendList(MailList mailList) {
            this.mailList.append(mailList);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public EmailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.new_email_item, parent, false);
            return new EmailViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull EmailViewHolder holder, int position) {
            holder.itemView.setOnClickListener(v -> {
                // TODO: Pass email ID to the fragment
                Bundle bundle = new Bundle();
                bundle.putInt("emailId", 1); // Replace 1 with the actual email ID you want to pass

                NavHostFragment.findNavController(EmailsFragment.this)
                        .navigate(R.id.navigation_email_detail, bundle);
            });

            holder.senderName.setText(mailList.getMail(position).getSender());
//            holder.receiverName.setText(mailList.getMail(position).getReceiver());
            holder.title.setText(mailList.getMail(position).getTitle());
            holder.time.setText(mailList.getMail(position).getSentTime());
            holder.place.setText(mailList.getMail(position).getLocation());
        }

        @Override
        public int getItemCount() {
            // Update to reflect actual number of emails from ViewModel or LiveData
            return mailList.size();
        }

        class EmailViewHolder extends RecyclerView.ViewHolder {
            TextView senderName;
            TextView receiverName;
            TextView title;
            TextView time;
            TextView place;

            public EmailViewHolder(@NonNull View itemView) {
                super(itemView);
                senderName = itemView.findViewById(R.id.sender_name);
                receiverName = itemView.findViewById(R.id.receiver_name);
                title = itemView.findViewById(R.id.title);
                time = itemView.findViewById(R.id.time);
                place = itemView.findViewById(R.id.place);

            }
        }
    }
}
