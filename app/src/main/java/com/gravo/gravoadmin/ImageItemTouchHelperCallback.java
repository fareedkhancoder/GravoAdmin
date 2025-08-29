package com.gravo.gravoadmin;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Collections;
import java.util.List;

public class ImageItemTouchHelperCallback extends ItemTouchHelper.Callback {

    private final ProductImageAdapter adapter;
    private final List<?> list; // Use wildcard to accept any list

    public ImageItemTouchHelperCallback(ProductImageAdapter adapter, List<?> list) {
        this.adapter = adapter;
        this.list = list;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        // Allows dragging to start on a long press
        return true;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        // We only want drag-and-drop, not swipe to dismiss
        return false;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        // Prevent the "Add" button from being dragged
        if (viewHolder instanceof ProductImageAdapter.AddImageViewHolder) {
            return 0;
        }

        // Allow dragging left and right
        final int dragFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
        final int swipeFlags = 0; // Swiping is disabled
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        int fromPosition = viewHolder.getAdapterPosition();
        int toPosition = target.getAdapterPosition();

        // Ensure we are not trying to move an item to the "Add" button's position
        if (toPosition == list.size()) {
            return false;
        }

        // Swap the items in the list
        Collections.swap(list, fromPosition, toPosition);

        // Notify the adapter of the move
        adapter.notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        // This will not be called because isItemViewSwipeEnabled() returns false
    }
}