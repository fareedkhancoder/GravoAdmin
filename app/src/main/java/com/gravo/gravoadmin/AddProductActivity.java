package com.gravo.gravoadmin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AddProductActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    // UI Elements
    private ImageView productImageView;
    private Button selectImageButton;
    private Spinner categorySpinner;
    private Spinner fieldSpinner;
    private EditText valueEditText;
    private Button addFieldButton;
    private TextView addedFieldsTextView;
    private Button saveProductButton;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageReference;

    // Data holders
    private Uri imageUri;
    private final Map<String, Object> productData = new HashMap<>();
    private final List<String> categories = new ArrayList<>();
    private ArrayAdapter<String> categoryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // EdgeToEdge.enable(this); // Consider removing if it causes layout issues
        setContentView(R.layout.activity_add_product);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // Initialize UI
        initViews();

        // Setup Spinners
        setupFieldSpinner();
        setupCategorySpinner();
        fetchCategories();

        // Set Click Listeners
        selectImageButton.setOnClickListener(v -> openFileChooser());
        addFieldButton.setOnClickListener(v -> addField());
        saveProductButton.setOnClickListener(v -> saveProduct());
    }

    private void initViews() {
        productImageView = findViewById(R.id.productImageView);
        selectImageButton = findViewById(R.id.selectImageButton);
        categorySpinner = findViewById(R.id.categorySpinner);
        fieldSpinner = findViewById(R.id.fieldSpinner);
        valueEditText = findViewById(R.id.valueEditText);
        addFieldButton = findViewById(R.id.addFieldButton);
        addedFieldsTextView = findViewById(R.id.addedFieldsTextView);
        saveProductButton = findViewById(R.id.saveProductButton);
    }

    private void setupFieldSpinner() {
        String[] fields = {"Name", "Brand", "Description", "Tags", "price", "Selling Price", "Is New (true/false)", "MFG Date", "EXP Date"};
        ArrayAdapter<String> fieldAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, fields);
        fieldAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fieldSpinner.setAdapter(fieldAdapter);
    }

    private void setupCategorySpinner() {
        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);
    }

    private void fetchCategories() {
        db.collection("categories")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        categories.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Assuming each category document has a "name" field
                            String categoryName = document.getString("name");
                            if (categoryName != null) {
                                categories.add(categoryName);
                            }
                        }
                        categoryAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(AddProductActivity.this, "Error fetching categories", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            productImageView.setImageURI(imageUri);
        }
    }

    private void addField() {
        String field = fieldSpinner.getSelectedItem().toString();
        String value = valueEditText.getText().toString().trim();

        if (value.isEmpty()) {
            Toast.makeText(this, "Value cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add to map, converting to number/boolean if applicable
        try {
            if (field.toLowerCase().contains("price")) {
                productData.put(field.toLowerCase().replace(" ", "_"), Double.parseDouble(value));
            } else if (field.toLowerCase().contains("is new")) {
                productData.put("is_new", Boolean.parseBoolean(value));
            } else {
                productData.put(field.toLowerCase().replace(" ", "_"), value);
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid number for price fields.", Toast.LENGTH_SHORT).show();
            return;
        }


        // Update UI to show added field
        addedFieldsTextView.append(field + ": " + value + "\n");
        valueEditText.setText("");
    }

    private void saveProduct() {
        if (imageUri != null) {
            uploadImageAndSaveProduct();
        } else {
            // Save product without an image
            saveProductData(null);
        }
    }

    private void uploadImageAndSaveProduct() {
        final StorageReference fileReference = storageReference.child("product_images/" + UUID.randomUUID().toString());

        fileReference.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();
                    saveProductData(imageUrl);
                }))
                .addOnFailureListener(e -> Toast.makeText(AddProductActivity.this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void saveProductData(@Nullable String imageUrl) {
        if (categorySpinner.getSelectedItem() == null) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }
        String selectedCategory = categorySpinner.getSelectedItem().toString();
        productData.put("categoryId", selectedCategory);

        if (imageUrl != null) {
            // For extensibility, we save images as a list
            List<String> imageUrls = new ArrayList<>();
            imageUrls.add(imageUrl);
            productData.put("imageUrls", imageUrls); // Changed "images" to "imageUrls"
        }

        db.collection("products")
                .add(productData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(AddProductActivity.this, "Product saved successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // Go back to the previous activity
                })
                .addOnFailureListener(e -> Toast.makeText(AddProductActivity.this, "Error saving product: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
