package com.gravo.gravoadmin;

import com.google.firebase.Timestamp; // Make sure to import this!
import java.util.List;
import java.util.Map;

public class OrderModel {
    private Timestamp orderDate; // Changed from String to Timestamp
    private String orderStatus;
    private List<Map<String, Object>> items;

    public OrderModel() {
        // Empty constructor required for Firestore
    }

    public Timestamp getOrderDate() { return orderDate; }
    public String getOrderStatus() { return orderStatus; }
    public List<Map<String, Object>> getItems() { return items; }
}