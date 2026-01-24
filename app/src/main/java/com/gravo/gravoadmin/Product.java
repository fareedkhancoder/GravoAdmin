package com.gravo.gravoadmin;

import androidx.room.Entity;        // New Import
import androidx.room.PrimaryKey;    // New Import
import androidx.room.TypeConverters; // New Import

import com.google.firebase.firestore.ServerTimestamp;
import com.google.firebase.firestore.DocumentId;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Entity(tableName = "drafts") // <-- NEW: Marks this as a database table
public class Product {

    // --- NEW: Local Database ID (Auto-Generated) ---
    // This is only for the local phone database, Firestore ignores it.
    @PrimaryKey(autoGenerate = true)
    private int draftId;

    @DocumentId
    private String name;
    private String description;
    private String brand;
    private String categoryId;
    private String sellerId;
    private double price;
    private double costPrice;
    private long discountPercent;
    private long stockQuantity;
    private int Clicks;

    // --- NEW: TypeConverters for Complex Data ---
    // Room cannot save Lists directly, so we tell it to use the Converter class.

    @TypeConverters(Converters.class)
    private List<String> imageUrls;

    @TypeConverters(Converters.class)
    private List<String> tags_lowercase;

    private boolean is_new;

    @ServerTimestamp
    @TypeConverters(Converters.class) // Converters also handle Date objects
    private Date createdAt;

    @TypeConverters(Converters.class)
    private Map<String, String> specifications;

    // Required empty public constructor for Firestore
    public Product() {}

    // --- NEW: Getter and Setter for local draftId ---
    public int getDraftId() { return draftId; }
    public void setDraftId(int draftId) { this.draftId = draftId; }

    // --- OLD FUNCTIONS (UNCHANGED) ---
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public double getCostPrice() { return costPrice; }
    public void setCostPrice(double costPrice) { this.costPrice = costPrice; }
    public long getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(long discountPercent) { this.discountPercent = discountPercent; }
    public long getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(long stockQuantity) { this.stockQuantity = stockQuantity; }
    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public List<String> getTags_lowercase() { return tags_lowercase; }
    public void setTags_lowercase(List<String> tags_lowercase) { this.tags_lowercase = tags_lowercase; }
    public boolean isIs_new() { return is_new; }
    public void setIs_new(boolean is_new) { this.is_new = is_new; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Map<String, String> getSpecifications() { return specifications; }
    public void setSpecifications(Map<String, String> specifications) { this.specifications = specifications; }

    public int getClicks() {
        return Clicks;
    }

    public void setClicks(int clicks) {
        Clicks = clicks;
    }
}