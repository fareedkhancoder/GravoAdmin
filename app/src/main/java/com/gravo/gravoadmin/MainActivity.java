package com.gravo.gravoadmin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import androidx.annotation.Nullable;

public class MainActivity extends AppCompatActivity {

    // --- UI Components ---
    private ImageView btnAddProduct;
    private TextView tvRevenue, tvTotalOrders, tvInvestment, tvInventory;
    private TextView badgeMorning, badgeAfternoon, badgeEvening;
    private TextView btnSeeAllMorning, btnSeeAllAfternoon, btnSeeAllEvening;

    // --- Data & Firebase ---
    private FirebaseFirestore db;
    private ListenerRegistration dashboardListener; // Important: To stop listening when app closes

    // Counters
    private double totalDailyRevenue = 0.0;
    private int totalDailyOrders = 0;
    private int countMorning = 0, countAfternoon = 0, countEvening = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        initializeViews();
        setupListeners();

        FirebaseMessaging.getInstance().subscribeToTopic("admin_notifications")
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        // Log error if subscription fails
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Start listening to the database as soon as the app opens
        startRealtimeDashboard();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Stop listening when the user leaves the app to save battery/data
        if (dashboardListener != null) {
            dashboardListener.remove();
        }
    }

    private void initializeViews() {
        btnAddProduct = findViewById(R.id.AddProduct);
        tvRevenue = findViewById(R.id.tvRevenue);
        tvTotalOrders = findViewById(R.id.tvTotalOrders);
        tvInvestment = findViewById(R.id.tvInvestment);
        tvInventory = findViewById(R.id.tvInventoryCount);
        badgeMorning = findViewById(R.id.badge1);
        badgeAfternoon = findViewById(R.id.badge2);
        badgeEvening = findViewById(R.id.badge3);
        btnSeeAllMorning = findViewById(R.id.see_all_morning_orders_btn);
        btnSeeAllAfternoon = findViewById(R.id.see_all_afternoon_orders_btn);
        btnSeeAllEvening = findViewById(R.id.see_all_evening_orders_btn);
    }

    private void setupListeners() {
        btnAddProduct.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddProductActivity.class);
            startActivity(intent);
        });

        // "See All" Logic
        btnSeeAllMorning.setOnClickListener(v -> openOrderList("morning"));
        btnSeeAllAfternoon.setOnClickListener(v -> openOrderList("afternoon"));
        btnSeeAllEvening.setOnClickListener(v -> openOrderList("evening"));
    }

    private void openOrderList(String filterType) {
        try {
            Class<?> destinationClass = Class.forName("com.gravo.gravoadmin.OrderListActivity");
            Intent intent = new Intent(MainActivity.this, destinationClass);
            intent.putExtra("FILTER_TYPE", filterType);
            startActivity(intent);
        } catch (ClassNotFoundException e) {
            Toast.makeText(this, "Please create OrderListActivity", Toast.LENGTH_SHORT).show();
        }
    }

    private void startRealtimeDashboard() {
        // Calculate start of Today (Midnight)
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date startOfToday = cal.getTime();

        // REAL-TIME LISTENER: This runs every time the database changes!
        dashboardListener = db.collection("orders")
                .whereGreaterThanOrEqualTo("orderDate", startOfToday)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.e("Firestore", "Listen failed.", e);
                            return;
                        }

                        if (snapshots != null) {
                            resetCounters(); // Clear old numbers before recounting

                            for (DocumentSnapshot doc : snapshots) {
                                OrderModel order = doc.toObject(OrderModel.class);
                                if (order != null) {
                                    calculateMetrics(order);
                                }
                            }
                            updateDashboardUI(); // Update screen immediately
                        }
                    }
                });
    }

    private void resetCounters() {
        totalDailyRevenue = 0.0;
        totalDailyOrders = 0;
        countMorning = 0;
        countAfternoon = 0;
        countEvening = 0;
    }

    private void calculateMetrics(OrderModel order) {
        // 1. Revenue & Total Count
        double orderTotal = getOrderTotal(order.getItems());
        totalDailyRevenue += orderTotal;
        totalDailyOrders++;

        // 2. Time Based counting
        int hour = getHourFromTimestamp(order.getOrderDate());
        if (hour >= 5 && hour < 12) countMorning++;
        else if (hour >= 12 && hour < 17) countAfternoon++;
        else countEvening++;
    }

    private void updateDashboardUI() {
        tvRevenue.setText(formatCurrency(totalDailyRevenue));
        tvTotalOrders.setText(String.valueOf(totalDailyOrders));

        // Static placeholders
        tvInventory.setText("1,200");
        tvInvestment.setText("$25K");

        // Update Badges
        badgeMorning.setText(String.valueOf(countMorning));
        badgeAfternoon.setText(String.valueOf(countAfternoon));
        badgeEvening.setText(String.valueOf(countEvening));
    }

    // --- Helpers ---
    private double getOrderTotal(List<Map<String, Object>> items) {
        double sum = 0.0;
        if (items != null) {
            for (Map<String, Object> item : items) {
                if (item.containsKey("priceAtPurchase")) {
                    try {
                        sum += Double.parseDouble(String.valueOf(item.get("priceAtPurchase")));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return sum;
    }

    private int getHourFromTimestamp(Timestamp timestamp) {
        if (timestamp == null) return 0;
        Date date = timestamp.toDate();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.HOUR_OF_DAY);
    }

    private String formatCurrency(double amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
        return format.format(amount);
    }
}