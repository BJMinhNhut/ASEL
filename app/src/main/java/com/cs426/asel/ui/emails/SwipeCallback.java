package com.cs426.asel.ui.emails;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.cs426.asel.R;

public class SwipeCallback extends ItemTouchHelper.SimpleCallback {
    private final float swipeThreshold = 0.4f;
    private Drawable icon;
    private GradientDrawable backgroundColor;

    public SwipeCallback(int dragDirs, int swipeDirs) {
        super(dragDirs, swipeDirs);
        backgroundColor = new GradientDrawable();
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {

        float translationX = dX;


        if (dX < 0) { // swipe left
            icon = viewHolder.itemView.getContext().getResources().getDrawable(R.drawable.ic_delete);
            int iconMargin = (viewHolder.itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
            int iconTop = viewHolder.itemView.getTop() + (viewHolder.itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
            int iconBottom = iconTop + icon.getIntrinsicHeight();
            int iconLeft = viewHolder.itemView.getRight() - iconMargin - icon.getIntrinsicWidth();
            int iconRight = viewHolder.itemView.getRight() - iconMargin;

            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);

            backgroundColor.setBounds(
                    viewHolder.itemView.getRight() + (int) dX,
                    viewHolder.itemView.getTop(),
                    viewHolder.itemView.getRight(),
                    viewHolder.itemView.getBottom()
            );

            backgroundColor.setColor(
                    viewHolder.itemView.getContext().getResources().getColor(R.color.red)
            );

            translationX = Math.max(dX, (-1) * viewHolder.itemView.getWidth() * swipeThreshold);
        } else if (dX > 0) { // swipe right
            icon = viewHolder.itemView.getContext().getResources().getDrawable(R.drawable.ic_note_add);
            int iconLeft = viewHolder.itemView.getLeft() + icon.getIntrinsicWidth();
            int iconRight = viewHolder.itemView.getLeft() + icon.getIntrinsicWidth() + icon.getIntrinsicWidth();
            int iconTop = viewHolder.itemView.getTop() + (viewHolder.itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
            int iconBottom = iconTop + icon.getIntrinsicHeight();

            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);

            backgroundColor.setBounds(
                    viewHolder.itemView.getLeft(),
                    viewHolder.itemView.getTop(),
                    (int) dX,
                    viewHolder.itemView.getBottom()
            );

            backgroundColor.setColor(
                    viewHolder.itemView.getContext().getResources().getColor(R.color.green)
            );

        } else {
            backgroundColor.setBounds(0, 0, 0, 0);
            icon = null;

        }

        super.onChildDraw(c, recyclerView, viewHolder, translationX, dY, actionState, isCurrentlyActive);

        int alpha = (int)(Math.abs(translationX) / (viewHolder.itemView.getWidth() * swipeThreshold) * 255);
        backgroundColor.setAlpha(alpha);
        backgroundColor.draw(c);
        if (icon != null) {
            icon.setAlpha(alpha);
            icon.draw(c);
        }
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        return swipeThreshold;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

    }
}
