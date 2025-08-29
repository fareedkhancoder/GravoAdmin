package com.gravo.gravoadmin;

import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AddProductActivity extends AppCompatActivity {

    private ProductImageAdapter imageAdapter;
    private List<Uri> imageUris;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    imageUris.add(uri);
                    imageAdapter.notifyItemInserted(imageUris.size() - 1);
                }
            });

    // UI Components
    private TextInputEditText inputProductName, inputBrand, inputDescription, inputTags;
    private TextInputEditText inputSellingPrice, inputDiscount, inputCostPrice, inputStockQuantity;
    private AutoCompleteTextView inputCategory;
    private MaterialSwitch switchIsNew;
    private Button buttonPublish;
    private ProgressDialog progressDialog;

    // Firebase
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        // Setup Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Initialize UI components
        initializeViews();

        // Setup RecyclerView logic
        setupRecyclerView();

        // Set listener for the publish button
        buttonPublish.setOnClickListener(v -> publishProduct());
    }

    private void initializeViews() {
        // Find all views by their ID
        inputProductName = findViewById(R.id.input_product_name);
        inputBrand = findViewById(R.id.input_brand);
        inputDescription = findViewById(R.id.input_description);
        inputCategory = findViewById(R.id.input_category);
        inputTags = findViewById(R.id.input_tags);
        inputSellingPrice = findViewById(R.id.input_selling_price);
        inputDiscount = findViewById(R.id.input_discount);
        inputCostPrice = findViewById(R.id.input_cost_price);
        inputStockQuantity = findViewById(R.id.input_stock_quantity);
        switchIsNew = findViewById(R.id.switch_is_new);
        buttonPublish = findViewById(R.id.button_publish);

        // Setup category dropdown (example categories)
        String[] categories = new String[] {"Electronics", "Apparel", "Home & Kitchen", "Books"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        inputCategory.setAdapter(adapter);

        // Progress Dialog for loading states
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Publishing Product");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
    }

    // --- THIS IS THE MISSING METHOD ---
    private void setupRecyclerView() {
        // RecyclerView and Image Picker
        RecyclerView recyclerViewImages = findViewById(R.id.recycler_view_images);
        imageUris = new ArrayList<>();

        ProductImageAdapter.OnImageInteractionListener listener = new ProductImageAdapter.OnImageInteractionListener() {
            @Override
            public void onAddImageClick() {
                imagePickerLauncher.launch("image/*");
            }

            @Override
            public void onRemoveImageClick(int position) {
                imageUris.remove(position);
                imageAdapter.notifyItemRemoved(position);
                imageAdapter.notifyItemRangeChanged(position, imageUris.size());
            }
        };

        imageAdapter = new ProductImageAdapter(imageUris, listener);

        recyclerViewImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerViewImages.setAdapter(imageAdapter);

        // Add Drag and Drop functionality
        ItemTouchHelper.Callback callback = new ImageItemTouchHelperCallback(imageAdapter, imageUris);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerViewImages);
    }

    private void publishProduct() {
        // Input Validation
        String name = inputProductName.getText().toString().trim();
        String priceStr = inputSellingPrice.getText().toString().trim();
        String stockStr = inputStockQuantity.getText().toString().trim();

        if (imageUris.isEmpty() || TextUtils.isEmpty(name) || TextUtils.isEmpty(priceStr) || TextUtils.isEmpty(stockStr)) {
            Toast.makeText(this, "Product Images, Name, Price, and Stock are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();

        // Create Product Object from UI data
        Product product = new Product();
        product.setName(name);
        product.setBrand(inputBrand.getText().toString().trim());
        product.setDescription(inputDescription.getText().toString().trim());
        product.setPrice(Double.parseDouble(priceStr));
        product.setStockQuantity(Long.parseLong(stockStr));
        product.setIs_new(switchIsNew.isChecked());
        product.setCategoryId(inputCategory.getText().toString());
        product.setSellerId("your_current_seller_id"); // Replace with actual seller ID

        // Handle potentially empty fields for numbers
        String costPriceStr = inputCostPrice.getText().toString().trim();
        if (!TextUtils.isEmpty(costPriceStr)) {
            product.setCostPrice(Double.parseDouble(costPriceStr));
        }

        String discountStr = inputDiscount.getText().toString().trim();
        if (!TextUtils.isEmpty(discountStr)) {
            product.setDiscountPercent(Long.parseLong(discountStr));
        }

        // Process tags
        String tagsString = inputTags.getText().toString().toLowerCase().trim();
        if (!tagsString.isEmpty()) {
            List<String> tags = Arrays.asList(tagsString.split("\\s*,\\s*"));
            product.setTags_lowercase(tags);
        }

        // Upload Images and Save Product Data
        uploadImagesAndSaveProduct(product);
    }

    private void uploadImagesAndSaveProduct(Product product) {
        List<String> downloadUrls = new ArrayList<>();
        List<Task<Uri>> uploadTasks = new ArrayList<>();

        // Generate a unique product ID
        String productId = firestore.collection("products").document().getId();
        product.setProductId(productId);

        StorageReference storageRef = storage.getReference().child("product_images/" + productId);

        for (int i = 0; i < imageUris.size(); i++) {
            Uri imageUri = imageUris.get(i);
            StorageReference imageRef = storageRef.child("image_" + i);
            Task<Uri> uploadTask = imageRef.putFile(imageUri).continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                return imageRef.getDownloadUrl();
            });
            uploadTasks.add(uploadTask);
        }

        // Wait for all image uploads to complete
        Tasks.whenAllSuccess(uploadTasks).addOnSuccessListener(urls -> {
            for (Object url : urls) {
                downloadUrls.add(url.toString());
            }
            product.setImageUrls(downloadUrls);

            // Save the complete product object to Firestore
            firestore.collection("products").document(productId)
                    .set(product)
                    .addOnSuccessListener(aVoid -> {
                        progressDialog.dismiss();
                        Toast.makeText(AddProductActivity.this, "Product published successfully!", Toast.LENGTH_SHORT).show();
                        finish(); // Close the activity
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(AddProductActivity.this, "Error saving product data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toast.makeText(AddProductActivity.this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}