package com.cs426.asel.ui.emails;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.cs426.asel.R;
import com.cs426.asel.databinding.FragmentEmailsBinding;
import com.google.android.material.snackbar.Snackbar;

public class EmailsFragment extends Fragment {
    private FragmentEmailsBinding binding;
    private RecyclerView emailListRecyclerView;
    private EmailsViewModel emailsViewModel; // Reference to the shared ViewModel

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Obtain EmailsViewModel from the activity's ViewModelProvider
        emailsViewModel = new ViewModelProvider(requireActivity()).get(EmailsViewModel.class);
        binding = FragmentEmailsBinding.inflate(inflater, container, false);

        emailsViewModel.fetchEmails();

        return binding.getRoot();
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

                                adapter.notifyItemRemoved(viewHolder.getAdapterPosition());
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

    class EmailListAdapter extends RecyclerView.Adapter<EmailListAdapter.EmailViewHolder> {
        @NonNull
        @Override
        public EmailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_new_email, parent, false);
            return new EmailViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull EmailViewHolder holder, int position) {
            holder.itemView.setOnClickListener(v -> {
                // TODO: Pass email ID to the fragment
                Bundle bundle = new Bundle();
                bundle.putInt("emailId", 1); // Replace 1 with the actual email ID you want to pass

                FragmentTransaction ft = requireActivity().getSupportFragmentManager().beginTransaction();
                EmailDetailFragment fragment = new EmailDetailFragment();
                fragment.setArguments(bundle);
                ft.replace(R.id.emailsContainer, fragment).addToBackStack(null).commit();
            });
        }

        @Override
        public int getItemCount() {
            // Update to reflect actual number of emails from ViewModel or LiveData
            return 5;
        }

        class EmailViewHolder extends RecyclerView.ViewHolder {

            public EmailViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }
}
