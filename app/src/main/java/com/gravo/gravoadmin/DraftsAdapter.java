package com.gravo.gravoadmin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class DraftsAdapter extends RecyclerView.Adapter<DraftsAdapter.DraftViewHolder> {

    private List<Product> draftList = new ArrayList<>();
    private Context context;
    private OnDraftClickListener listener;

    // Interface to handle clicks (Edit draft)
    public interface OnDraftClickListener {
        void onDraftClick(Product product);
    }

    public DraftsAdapter(Context context, OnDraftClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setList(List<Product> list) {
        this.draftList = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DraftViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new DraftViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DraftViewHolder holder, int position) {
        Product product = draftList.get(position);

        // 1. Bind Name
        String name = product.getName();
        holder.tvProductName.setText((name == null || name.isEmpty()) ? "Untitled Draft" : name);

        // 2. Bind Price (Reuse the 'Selling_Prize' textview)
        holder.tvPrice.setText("Price: " + product.getPrice());

        // 3. Bind Status
        holder.tvStatus.setText("Local Draft");

        // 4. Bind Date (Optional: You can format date here if needed)
        holder.tvDate.setText("Draft ID: " + product.getDraftId());

        // 5. Handle Click
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDraftClick(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return draftList.size();
    }

    public static class DraftViewHolder extends RecyclerView.ViewHolder {
        TextView tvProductName, tvDate, tvStatus, tvPrice;

        public DraftViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProductName = itemView.findViewById(R.id.tvProductNames);
            tvDate = itemView.findViewById(R.id.upload_date);
            tvStatus = itemView.findViewById(R.id.orderStatusTextView);
            tvPrice = itemView.findViewById(R.id.Selling_Prize);
        }
    }
}