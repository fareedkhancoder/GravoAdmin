package com.gravo.gravoadmin;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ProductImageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_IMAGE = 0;
    private static final int VIEW_TYPE_ADD = 1;

    private final List<Uri> imageUris;
    private final OnImageInteractionListener listener;

    public interface OnImageInteractionListener {
        void onAddImageClick();
        void onRemoveImageClick(int position);
    }

    public ProductImageAdapter(List<Uri> imageUris, OnImageInteractionListener listener) {
        this.imageUris = imageUris;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        // The last item is always the "Add" button
        if (position == imageUris.size()) {
            return VIEW_TYPE_ADD;
        } else {
            return VIEW_TYPE_IMAGE;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_IMAGE) {
            View view = inflater.inflate(R.layout.item_product_image, parent, false);
            return new ImageViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_add_image, parent, false);
            return new AddImageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == VIEW_TYPE_IMAGE) {
            ImageViewHolder imageViewHolder = (ImageViewHolder) holder;
            Uri imageUri = imageUris.get(position);
            imageViewHolder.imageView.setImageURI(imageUri);
        }
        // No binding needed for AddImageViewHolder as it's static
    }

    @Override
    public int getItemCount() {
        // List size + 1 for the "Add" button
        return imageUris.size() + 1;
    }

    // ViewHolder for the existing images
    class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageButton removeButton;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_view_product);
            removeButton = itemView.findViewById(R.id.button_remove_image);

            removeButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onRemoveImageClick(position);
                }
            });
        }
    }

    // ViewHolder for the "Add Image" button
    class AddImageViewHolder extends RecyclerView.ViewHolder {
        AddImageViewHolder(@NonNull View itemView) {
            super(itemView);
            itemView.setOnClickListener(v -> listener.onAddImageClick());
        }
    }
}