package com.gravo.gravoadmin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderFragment extends Fragment {

    private static final String ARG_STATUS = "order_status";
    private static final String ARG_FILTER = "filter_type";

    private String status;
    private String filterType;

    private RecyclerView recyclerView;
    private TextView emptyView;
    private OrderAdapter adapter;
    private List<OrderModel> orderList;
    private FirebaseFirestore db;

    public static OrderFragment newInstance(String status, String filterType) {
        OrderFragment fragment = new OrderFragment();
        Bundle args = new Bundle();
        args.putString(ARG_STATUS, status);
        args.putString(ARG_FILTER, filterType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            status = getArguments().getString(ARG_STATUS);
            filterType = getArguments().getString(ARG_FILTER);
        }
        db = FirebaseFirestore.getInstance();
        orderList = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order_list, container, false);

        recyclerView = view.findViewById(R.id.recycler_view_orders);
        emptyView = view.findViewById(R.id.empty_view);

        setupRecyclerView();
        loadOrders();

        return view;
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new OrderAdapter(orderList);
        recyclerView.setAdapter(adapter);
    }

    private void loadOrders() {
        Query query = db.collection("orders");

        // 1. Filter by Status
        query = query.whereEqualTo("orderStatus", status);

        // 2. Apply Time Filter
        if (filterType != null) {
            Date[] timeRange = getTimeRangeForFilter(filterType);
            if (timeRange != null) {
                Date startTime = timeRange[0];
                Date endTime = timeRange[1];

                query = query.whereGreaterThanOrEqualTo("orderDate", startTime)
                        .whereLessThan("orderDate", endTime);
            }
        }

        // 3. Sort by Date
        query = query.orderBy("orderDate", Query.Direction.DESCENDING);

        // 4. Real-time Listener
        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.e("OrderFragment", "Listen failed.", error);
                    return;
                }

                orderList.clear();
                if (value != null) {
                    for (DocumentSnapshot doc : value) {
                        OrderModel order = doc.toObject(OrderModel.class);
                        orderList.add(order);
                    }
                }

                adapter.notifyDataSetChanged();
                updateEmptyView();
            }
        });
    }

    private void updateEmptyView() {
        if (orderList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            emptyView.setText("No " + status + " orders found.");
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    private Date[] getTimeRangeForFilter(String filter) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Date start = null;
        Date end = null;

        switch (filter.toLowerCase()) {
            case "morning":
                cal.set(Calendar.HOUR_OF_DAY, 5);
                start = cal.getTime();
                cal.set(Calendar.HOUR_OF_DAY, 12);
                end = cal.getTime();
                break;
            case "afternoon":
                cal.set(Calendar.HOUR_OF_DAY, 12);
                start = cal.getTime();
                cal.set(Calendar.HOUR_OF_DAY, 17);
                end = cal.getTime();
                break;
            case "evening":
                cal.set(Calendar.HOUR_OF_DAY, 17);
                start = cal.getTime();
                cal.add(Calendar.DAY_OF_YEAR, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                end = cal.getTime();
                break;
            default:
                return null;
        }
        return new Date[]{start, end};
    }

    // ---------------------------------------------------------
    //                   UPDATED ADAPTER CLASS
    // ---------------------------------------------------------
    private class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {
        private List<OrderModel> list;

        public OrderAdapter(List<OrderModel> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_card, parent, false);
            return new OrderViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
            OrderModel order = list.get(position);

            // 1. Order ID
            holder.tvOrderId.setText("ID: " + (order.getOrderId() != null ? order.getOrderId() : "Unknown"));

            // 2. Customer Name (Safe Check)
            String name = order.getCustomerName();
            holder.tvCustomerName.setText(name != null && !name.isEmpty() ? name : "Guest Customer");

            // 3. Date & Time (Locale Aware)
            if (order.getOrderDate() != null) {
                DateFormat dateFormat = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());
                holder.tvOrderTime.setText(dateFormat.format(order.getOrderDate().toDate()));
            } else {
                holder.tvOrderTime.setText("--:--");
            }

            // 4. Product Names & Item Count
            StringBuilder productNames = new StringBuilder();
            int itemCount = 0;

            if (order.getItems() != null) {
                itemCount = order.getItems().size();
                for (int i = 0; i < itemCount; i++) {
                    Map<String, Object> item = order.getItems().get(i);
                    if (item.containsKey("productName")) {
                        if (productNames.length() > 0) productNames.append(", ");
                        productNames.append(item.get("productName"));
                    }
                }
            }
            holder.tvItemCount.setText("(" + itemCount + ")");
            holder.tvProductNames.setText(productNames.toString());

            // 5. Total Amount (Indian Currency Format)
            // We use order.getTotalAmount() because it's safer than calculating in the loop
            double totalAmount = order.getTotalAmount();
            NumberFormat indianFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
            holder.tvOrderTotal.setText(indianFormat.format(totalAmount));


            // ---------------------------------------------------------------
            //                  DYNAMIC BUTTON LOGIC
            // ---------------------------------------------------------------
            String currentStatus = order.getOrderStatus();
            String tempNextStatus = "";
            String tempButtonText = "";
            boolean showButton = true;

            if (currentStatus != null) {
                switch (currentStatus) {
                    case "Pending":
                        tempButtonText = "Accept Order";
                        tempNextStatus = "Processing";
                        break;
                    case "Processing":
                        tempButtonText = "Ship Order";
                        tempNextStatus = "Shipped";
                        break;
                    case "Shipped":
                        tempButtonText = "Start Delivery";
                        tempNextStatus = "Out for Delivery";
                        break;
                    case "Out for Delivery":
                        tempButtonText = "Mark Delivered";
                        tempNextStatus = "Delivered";
                        break;
                    default:
                        // Hide button for "Delivered" or "Cancelled"
                        showButton = false;
                        break;
                }
            } else {
                showButton = false;
            }

            // Apply Visibility
            if (showButton) {
                holder.btnAccept.setVisibility(View.VISIBLE);
                holder.btnAccept.setText(tempButtonText);

                // IMPORTANT: Create FINAL variables for the listeners
                String finalNextStatus = tempNextStatus;
                String finalButtonText = tempButtonText;

                holder.btnAccept.setOnClickListener(v -> {
                    holder.btnAccept.setEnabled(false);
                    holder.btnAccept.setText("Updating...");

                    // Use 'holder.itemView.getContext()' for safety inside Adapter
                    db.collection("orders").document(order.getOrderId())
                            .update("orderStatus", finalNextStatus)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(holder.itemView.getContext(), "Status updated to: " + finalNextStatus, Toast.LENGTH_SHORT).show();
                                // The List updates automatically via SnapshotListener
                            })
                            .addOnFailureListener(e -> {
                                holder.btnAccept.setEnabled(true);
                                holder.btnAccept.setText(finalButtonText); // Restores text safely
                                Toast.makeText(holder.itemView.getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                });
            } else {
                holder.btnAccept.setVisibility(View.GONE);
            }

            holder.btnView.setOnClickListener(v -> {
                // Navigate to Details Activity
                // Intent intent = new Intent(getContext(), OrderDetailActivity.class);
                // intent.putExtra("ORDER_ID", order.getOrderId());
                // startActivity(intent);
                Toast.makeText(getContext(), "View Order clicked", Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class OrderViewHolder extends RecyclerView.ViewHolder {
            TextView tvOrderId, tvOrderTime, tvCustomerName, tvOrderTotal, tvItemCount, tvProductNames;
            com.google.android.material.button.MaterialButton btnView, btnAccept;

            public OrderViewHolder(@NonNull View itemView) {
                super(itemView);
                tvOrderId = itemView.findViewById(R.id.tvOrderId);
                tvOrderTime = itemView.findViewById(R.id.tvOrderTime);
                tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
                tvOrderTotal = itemView.findViewById(R.id.tvOrderTotal);
                tvItemCount = itemView.findViewById(R.id.tvItemCount);
                tvProductNames = itemView.findViewById(R.id.tvProductNames);
                btnView = itemView.findViewById(R.id.btnViewOrder);
                btnAccept = itemView.findViewById(R.id.btnAcceptOrder);
            }
        }
    }
}