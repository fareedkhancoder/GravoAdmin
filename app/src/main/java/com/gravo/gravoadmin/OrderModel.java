package com.gravo.gravoadmin;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId; // Import this
import java.util.List;
import java.util.Map;

public class OrderModel {
    @DocumentId // This automatically grabs the Firestore Document ID
    private String orderId;

    private Timestamp orderDate;
    private String orderStatus;
    private Double totalAmount;
    private String customerName; // Ensure your Firestore has this field, or it will be null
    private List<Map<String, Object>> items;

    public OrderModel() {}

    public String getOrderId() { return orderId; }
    public Timestamp getOrderDate() { return orderDate; }
    public String getOrderStatus() { return orderStatus; }
    public String getCustomerName() { return customerName; }
    public List<Map<String, Object>> getItems() { return items; }
    public Double getTotalAmount() { return totalAmount; }


}