package com.cs426.asel.ui.emails;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.cs426.asel.R;

public class SwipeCallback extends ItemTouchHelper.SimpleCallback {
    private final float swipeThreshold = 0.4f;
    private boolean isSwipeEnabled = true;
    private Drawable icon;
    private final GradientDrawable backgroundColor;

    public SwipeCallback(int dragDirs, int swipeDirs) {
        super(dragDirs, swipeDirs);
        backgroundColor = new GradientDrawable();
        backgroundColor.setCornerRadius(15);
    }

    public void setSwipeEnabled(boolean enabled) {
        isSwipeEnabled = enabled;
    }

    public boolean isSwipeEnabled() {
        return isSwipeEnabled;
    }

    @Override
    public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        return isSwipeEnabled ? super.getSwipeDirs(recyclerView, viewHolder) : 0;
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (!isSwipeEnabled) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            return;
        }
        float translationX = dX;

        Drawable icon;
        if (dX < 0) { // swipe left
            icon = ResourcesCompat.getDrawable(viewHolder.itemView.getContext().getResources(), R.drawable.ic_trash, null);
            int iconMargin = (viewHolder.itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
            int iconTop = viewHolder.itemView.getTop() + iconMargin;
            int iconBottom = iconTop + icon.getIntrinsicHeight();
            int iconLeft = viewHolder.itemView.getRight() - iconMargin - icon.getIntrinsicWidth();
            int iconRight = iconLeft + icon.getIntrinsicWidth();

            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);

            backgroundColor.setBounds(
                    viewHolder.itemView.getRight() + (int) dX - 30,
                    viewHolder.itemView.getTop(),
                    viewHolder.itemView.getRight(),
                    viewHolder.itemView.getBottom()
            );

            backgroundColor.setColor(
                    ResourcesCompat.getColor(viewHolder.itemView.getContext().getResources(), R.color.error_medium, null)
            );

            translationX = Math.max(dX, (-1) * viewHolder.itemView.getWidth() * swipeThreshold);
        } else if (dX > 0) { // swipe right
            icon = ResourcesCompat.getDrawable(viewHolder.itemView.getContext().getResources(), R.drawable.ic_calendar_add, null);
            int iconMargin = (viewHolder.itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
            int iconLeft = viewHolder.itemView.getLeft() + iconMargin;
            int iconRight = iconLeft + icon.getIntrinsicWidth();
            int iconTop = viewHolder.itemView.getTop() + iconMargin;
            int iconBottom = iconTop + icon.getIntrinsicHeight();

            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);

            backgroundColor.setBounds(
                    viewHolder.itemView.getLeft(),
                    viewHolder.itemView.getTop(),
                    (int) dX + 30,
                    viewHolder.itemView.getBottom()
            );

            backgroundColor.setColor(
                    ResourcesCompat.getColor(viewHolder.itemView.getContext().getResources(), R.color.success_medium, null)
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
