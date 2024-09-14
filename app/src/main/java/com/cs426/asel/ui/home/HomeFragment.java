package com.cs426.asel.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cs426.asel.MainActivity;
import com.cs426.asel.R;
import com.cs426.asel.backend.Utility;
import com.cs426.asel.backend.Event;
import com.cs426.asel.backend.EventList;
import com.cs426.asel.backend.EventRepository;
import com.cs426.asel.backend.Mail;
import com.cs426.asel.backend.MailList;
import com.cs426.asel.backend.MailRepository;
import com.cs426.asel.backend.Utility;
import com.cs426.asel.databinding.FragmentHomeBinding;
import com.cs426.asel.ui.account.InfoViewModel;
import com.cs426.asel.ui.account.UpdateInfoFragment;
import com.cs426.asel.ui.emails.EmailsFragment;
import com.cs426.asel.ui.emails.EmailsViewModel;
import com.google.api.services.gmail.Gmail;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ImageView barcode;
    private InfoViewModel infoViewModel;
    private EmailsViewModel emailsViewModel;
    private TitleAdapter mailsAdapter, eventsAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        barcode = binding.barcode;

        eventsAdapter = new TitleAdapter(new ArrayList<>(), new ArrayList<>());
        mailsAdapter = new TitleAdapter(new ArrayList<>(), new ArrayList<>());

        binding.events.setAdapter(eventsAdapter);
        binding.events.addItemDecoration(new EmailsFragment.SpaceItemDecoration(5));
        binding.mails.setAdapter(mailsAdapter);
        binding.mails.addItemDecoration(new EmailsFragment.SpaceItemDecoration(5));

        binding.mails.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.events.setLayoutManager(new LinearLayoutManager(getContext()));

        infoViewModel = new ViewModelProvider(requireActivity()).get(InfoViewModel.class);
        emailsViewModel = new ViewModelProvider(requireActivity()).get(EmailsViewModel.class);
        observeViewModel();
        loadUserInfo();

        return root;
    }

    private void loadUserInfo() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences(
                "StudentInfo-" + Utility.getUserEmail(requireContext()), Context.MODE_PRIVATE);
        String fullName = sharedPreferences.getString("full_name", "");
        String studentId = sharedPreferences.getString("student_id", "");
        String birthdate = sharedPreferences.getString("birthday", "");
        String school = sharedPreferences.getString("school", "");
        String faculty = sharedPreferences.getString("faculty", "");
        String degree = sharedPreferences.getString("degree", "");
        String imageEncoded = sharedPreferences.getString("avatar_image", null);
        infoViewModel.setFullName(fullName);
        infoViewModel.setStudentId(studentId);
        infoViewModel.setBirthdate(birthdate);
        infoViewModel.setSchool(school);
        infoViewModel.setFaculty(faculty);
        infoViewModel.setDegree(degree);
        infoViewModel.setAvatar(imageEncoded);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserInfo();
        loadPreviewInfo();
    }

    private void observeViewModel() {
        infoViewModel.getFullName().observe(getViewLifecycleOwner(), fullName -> binding.fullname.setText(fullName));
        infoViewModel.getStudentId().observe(getViewLifecycleOwner(), studentId -> {
            binding.id.setText(studentId);
            Bitmap barcodeBitmap = generateBarcode(studentId);
            if (barcodeBitmap != null) {
                barcode.setVisibility(View.VISIBLE);
                barcode.setImageBitmap(barcodeBitmap);
            } else {
                barcode.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Failed to generate barcode", Toast.LENGTH_SHORT).show();
            }
        });
        infoViewModel.getBirthdate().observe(getViewLifecycleOwner(), birthdate -> binding.birthdate.setText(birthdate));
        infoViewModel.getSchool().observe(getViewLifecycleOwner(), school -> binding.school.setText(school));
        infoViewModel.getFaculty().observe(getViewLifecycleOwner(), faculty -> binding.faculty.setText(faculty));
        infoViewModel.getDegree().observe(getViewLifecycleOwner(), degree -> binding.degree.setText(degree));

        // Observe avatar and set the image in ImageView
        infoViewModel.getAvatar().observe(getViewLifecycleOwner(), avatarBase64 -> {
            if (avatarBase64 != null && !avatarBase64.isEmpty()) {
                byte[] decodedBytes = Base64.decode(avatarBase64, Base64.DEFAULT);
                Bitmap avatarBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                binding.avatar.setImageBitmap(avatarBitmap);
            } else {
                binding.avatar.setImageResource(R.drawable.avatar_default); // Set default avatar if none
            }
        });

        emailsViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                showLoading();
            } else {
                hideLoading();
            }
        });
    }

    private void hideLoading() {
        binding.eventAnnouncementText.setVisibility(View.GONE);
        binding.mailAnnouncementText.setVisibility(View.GONE);
        binding.mails.setVisibility(View.VISIBLE);
        binding.events.setVisibility(View.VISIBLE);

        loadPreviewInfo();
    }

    private void loadPreviewInfo() {
        String userEmail = Utility.getUserEmail(requireContext());
        Log.d("HomeFragment", "User email: " + userEmail);
        MailRepository mailRepository = new MailRepository(requireContext(), userEmail);
        EventRepository eventRepository = new EventRepository(requireContext(), userEmail);

        EventList eventList = new EventList();
        MailList mailList = mailRepository.getMailByRead(false, "send_time", false);

        EventList published = eventRepository.getEventsByPublished(true, "from_datetime", true);
        // Insert on going and upcoming events into eventList
        for (int i = 0; i < published.getSize(); i++) {
            Event event = published.getEvent(i);

            if (event.getStartTime().isAfter(Instant.now())) {
                eventList.addEvent(published.getEvent(i));
                continue;
            }

            int duration = event.getDuration();
            if (duration > 0 && event.getStartTime().plusSeconds(duration * 60).isAfter(Instant.now())) {
                eventList.addEvent(published.getEvent(i));
            }
        }

        if (eventList.getSize() == 0) {
            binding.eventAnnouncementText.setText("No event to show");
            binding.eventAnnouncementText.setVisibility(View.VISIBLE);
            binding.events.setVisibility(View.GONE);
        } else {
            binding.eventAnnouncementText.setVisibility(View.GONE);
            binding.events.setVisibility(View.VISIBLE);
            eventsAdapter.updateData(eventList);
        }

        if (mailList.size() == 0) {
            binding.mailAnnouncementText.setText("No mail to show");
            binding.mailAnnouncementText.setVisibility(View.VISIBLE);
            binding.mails.setVisibility(View.GONE);
        } else {
            binding.mailAnnouncementText.setVisibility(View.GONE);
            binding.mails.setVisibility(View.VISIBLE);
            mailsAdapter.updateData(mailList);
        }

        Log.d("HomeFragment", "Mails size: " + mailsAdapter.getItemCount());
        Log.d("HomeFragment", "Events size: " + eventsAdapter.getItemCount());
    }

    private void showLoading() {
        binding.eventAnnouncementText.setText("Loading...");
        binding.mailAnnouncementText.setText("Loading...");
        binding.eventAnnouncementText.setVisibility(View.VISIBLE);
        binding.mailAnnouncementText.setVisibility(View.VISIBLE);
        binding.mails.setVisibility(View.GONE);
        binding.events.setVisibility(View.GONE);
    }

    private Bitmap generateBarcode(String data) {
        try {
            if (data.length() % 2 != 0) {
                throw new IllegalArgumentException("Code 128-C must have an even number of digits");
            }

            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.CODE_128, 300, 30);

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? ResourcesCompat.getColor(getResources(), R.color.dark_darkest, null) : Color.TRANSPARENT);
                }
            }

            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static class TitleAdapter extends RecyclerView.Adapter<HomeFragment.TitleAdapter.TitleViewHolder> {
        private List<Instant> timeList;
        private List<String> titleList;
        private static final int PREVIEW_LIMIT = 5;

        public TitleAdapter(List<Instant> timeList, List<String> titleList) {
            this.timeList = timeList;
            this.titleList = titleList;
        }

        public void updateData(MailList mailList) {
            timeList.clear();
            titleList.clear();
            Log.d("HomeFragment", "Updating data, mailList size: " + mailList.size());
            for (int i = 0; i < Math.min(mailList.size(), PREVIEW_LIMIT); i++) {
                Mail mail = mailList.getMail(i);
                timeList.add(mail.getReceivedTime());
                titleList.add(mail.getTitle());

                Log.d("HomeFragment", "Adding mail: " + mail.getTitle());
                Log.d("HomeFragment", "Adding time: " + mail.getReceivedTime());
            }
            Log.d("HomeFragment", "Updated data");
            notifyDataSetChanged();
        }

        public void updateData(EventList eventList) {
            timeList.clear();
            titleList.clear();
            for (int i = 0; i < Math.min(eventList.getSize(), PREVIEW_LIMIT); i++) {
                Event event = eventList.getEvent(i);
                timeList.add(event.getStartTime());
                titleList.add(event.getTitle());
            }
            notifyDataSetChanged();
        }

        public static class TitleViewHolder extends RecyclerView.ViewHolder {
            public TextView time, title;

            public TitleViewHolder(View itemView) {
                super(itemView);
                time = itemView.findViewById(R.id.time);
                title = itemView.findViewById(R.id.title);
            }
        }

        @NonNull
        @Override
        public HomeFragment.TitleAdapter.TitleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_title, parent, false);
            return new HomeFragment.TitleAdapter.TitleViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull HomeFragment.TitleAdapter.TitleViewHolder holder, int position) {
            Instant timeInstant = timeList.get(position);
            String titleString = titleList.get(position);

            LocalDateTime startDateTime = LocalDateTime.ofInstant(timeInstant, ZoneId.systemDefault());

            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM");
            DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd");

            String startTime = timeFormatter.format(startDateTime);
            String startDay = dayFormatter.format(startDateTime);
            String startMonth = monthFormatter.format(startDateTime);

            String time = startMonth + " " + startDay + ", " + startTime;

            holder.time.setText(time);
            holder.title.setText(titleString);
        }

        @Override
        public int getItemCount() {
            return timeList.size();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}