package com.cs426.asel.ui.home;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.cs426.asel.R;
import com.cs426.asel.databinding.FragmentHomeBinding;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ImageView barcode;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        barcode = binding.barcode;

        Bitmap barcodeBitmap = generateBarcode("22125078");
        if (barcodeBitmap != null) {
            barcode.setImageBitmap(barcodeBitmap);
        } else {
            Toast.makeText(getContext(), "Failed to generate barcode", Toast.LENGTH_SHORT).show();
        }

        return root;
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
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.TRANSPARENT);
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

        public TitleAdapter(List<Instant> timeList, List<String> titleList) {
            this.timeList = timeList;
            this.titleList = titleList;
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