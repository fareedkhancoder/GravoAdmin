package com.gravo.gravoadmin;

import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class AddProductActivity extends AppCompatActivity {

    // --- MEMBER VARIABLES FOR PRODUCT IMAGES ---
    private ProductImageAdapter imageAdapter;
    private List<Uri> imageUris;
    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    imageUris.add(uri);
                    imageAdapter.notifyItemInserted(imageUris.size() - 1);
                }
            });

    // --- MEMBER VARIABLES FOR CATEGORY DIALOG ---
    private Uri categoryIconUri = null;
    private ImageView dialogCategoryIcon;
    private final ActivityResultLauncher<String> categoryIconPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    categoryIconUri = uri;
                    if (dialogCategoryIcon != null) {
                        dialogCategoryIcon.setImageURI(uri);
                    }
                }
            });

    // --- UI COMPONENTS ---
    private TextInputEditText inputProductName, inputBrand, inputDescription, inputTags;
    private TextInputEditText inputSellingPrice, inputDiscount, inputCostPrice, inputStockQuantity;
    private AutoCompleteTextView inputCategory;
    private MaterialSwitch switchIsNew;
    private Button buttonPublish;
    private ProgressDialog progressDialog;

    // --- FIREBASE ---
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;

    // --- DATA HOLDERS FOR CATEGORIES ---
    private final List<String> categoryNames = new ArrayList<>();
    private final Map<String, String> categoryNameToIdMap = new HashMap<>();
    private RecyclerView specRecyclerView;
    private ManualSpecAdapter manualSpecAdapter;
    private List<ManualSpecification> specificationsList;

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

        // Initialize UI components and listeners
        initializeViews();

        // Setup RecyclerView for product images
        setupRecyclerView();

        // Fetch categories from Firestore to populate dropdown
        fetchCategories();

        // Set listener for the publish button
        buttonPublish.setOnClickListener(v -> publishProduct());

        specRecyclerView = findViewById(R.id.recycler_view_specifications);
        Button buttonAddSpec = findViewById(R.id.button_add_specification);

        // Setup the new adapter
        specificationsList = new ArrayList<>();
        manualSpecAdapter = new ManualSpecAdapter(specificationsList, position -> {
            specificationsList.remove(position);
            manualSpecAdapter.notifyItemRemoved(position);
            manualSpecAdapter.notifyItemRangeChanged(position, specificationsList.size());
        });

        specRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        specRecyclerView.setAdapter(manualSpecAdapter);

        // Set listener for the "Add" button
        buttonAddSpec.setOnClickListener(v -> {
            specificationsList.add(new ManualSpecification());
            manualSpecAdapter.notifyItemInserted(specificationsList.size() - 1);
        });
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

        // Listener for category dropdown item selection
        inputCategory.setOnItemClickListener((parent, view, position, id) -> {
            String selectedItem = (String) parent.getItemAtPosition(position);

            // Check if the user clicked the special action item
            if (selectedItem.equals("--- Add New Category ---")) {
                // Clear the text field so it doesn't display the action text
                inputCategory.setText("", false);
                // Launch the dialog to add a new category
                showAddCategoryDialog();
            }
        });

        // Progress Dialog for loading states
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please Wait");
        progressDialog.setMessage("Working...");
        progressDialog.setCancelable(false);
    }

    /**
     * Fetches categories from Firestore and populates the dropdown menu.
     */
    private void fetchCategories() {
        firestore.collection("categories").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                categoryNames.clear();
                categoryNameToIdMap.clear();

                // Add the custom action item to the top of the list
                categoryNames.add("--- Add New Category ---");

                // Add categories fetched from Firestore
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String name = document.getString("name");
                    if (name != null) {
                        categoryNames.add(name);
                        categoryNameToIdMap.put(name, document.getId()); // Map name to document ID
                    }
                }
                // Update the adapter for the product category dropdown
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categoryNames);
                inputCategory.setAdapter(adapter);
            } else {
                Toast.makeText(this, "Failed to load categories.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Displays an AlertDialog to add a new category.
     */
    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_category, null);
        builder.setView(dialogView).setTitle("Add New Category");

        // Find views inside the custom dialog layout
        final TextInputEditText inputDialogCategoryName = dialogView.findViewById(R.id.input_dialog_category_name);
        final AutoCompleteTextView inputDialogParentCategory = dialogView.findViewById(R.id.input_dialog_parent_category);
        dialogCategoryIcon = dialogView.findViewById(R.id.image_category_icon);
        Button buttonSelectIcon = dialogView.findViewById(R.id.button_select_icon);

        // Populate the parent category dropdown with existing categories
        List<String> parentCategoryOptions = new ArrayList<>();
        parentCategoryOptions.add("None"); // Option for a top-level category
        parentCategoryOptions.addAll(categoryNameToIdMap.keySet()); // Add all real category names
        ArrayAdapter<String> parentAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, parentCategoryOptions);
        inputDialogParentCategory.setAdapter(parentAdapter);

        buttonSelectIcon.setOnClickListener(v -> categoryIconPickerLauncher.launch("image/*"));

        builder.setPositiveButton("Save", (dialog, which) -> {
            String categoryName = Objects.requireNonNull(inputDialogCategoryName.getText()).toString().trim();
            String parentCategoryName = inputDialogParentCategory.getText().toString().trim();

            if (TextUtils.isEmpty(categoryName)) {
                Toast.makeText(this, "Category name cannot be empty.", Toast.LENGTH_SHORT).show();
                return;
            }

            String parentCategoryId = null;
            if (!parentCategoryName.equals("None") && !parentCategoryName.isEmpty()) {
                parentCategoryId = categoryNameToIdMap.get(parentCategoryName);
            }

            saveCategory(categoryName, parentCategoryId);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    /**
     * Handles the process of saving a new category, starting with icon upload if necessary.
     */
    private void saveCategory(String name, String parentId) {
        progressDialog.setTitle("Saving Category");
        progressDialog.show();

        if (categoryIconUri != null) {
            // If an icon is selected, upload it to Firebase Storage first
            StorageReference storageRef = storage.getReference().child("category_icons/" + UUID.randomUUID().toString());
            storageRef.putFile(categoryIconUri)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            throw Objects.requireNonNull(task.getException());
                        }
                        return storageRef.getDownloadUrl();
                    })
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Uri downloadUri = task.getResult();
                            saveCategoryToFirestore(name, parentId, downloadUri.toString());
                        } else {
                            progressDialog.dismiss();
                            Toast.makeText(this, "Icon upload failed.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            // Save category data to Firestore without an icon URL
            saveCategoryToFirestore(name, parentId, null);
        }
    }

    /**
     * [cite_start]Writes the final category data object to the Firestore 'categories' collection[cite: 2].
     */
    private void saveCategoryToFirestore(String name, String parentId, String iconUrl) {
        String categoryId = firestore.collection("categories").document().getId();

        Map<String, Object> category = new HashMap<>();
        category.put("categoryId", categoryId);
        category.put("name", name);
        if (parentId != null) {
            category.put("parentCategoryId", parentId);
        }
        if (iconUrl != null) {
            category.put("iconUrl", iconUrl);
        }

        firestore.collection("categories").document(categoryId).set(category)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Category added successfully!", Toast.LENGTH_SHORT).show();
                    categoryIconUri = null; // Reset for the next use
                    fetchCategories(); // Refresh the dropdown list with the new category
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error saving category: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupRecyclerView() {
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

        ItemTouchHelper.Callback callback = new ImageItemTouchHelperCallback(imageAdapter, imageUris);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerViewImages);
    }

    private void publishProduct() {
        String name = inputProductName.getText().toString().trim();
        String priceStr = inputSellingPrice.getText().toString().trim();
        String stockStr = inputStockQuantity.getText().toString().trim();
        String categoryName = inputCategory.getText().toString().trim();

        if (imageUris.isEmpty() || TextUtils.isEmpty(name) || TextUtils.isEmpty(priceStr) || TextUtils.isEmpty(stockStr) || TextUtils.isEmpty(categoryName)) {
            Toast.makeText(this, "Product Images, Name, Price, Stock, and Category are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- IMPORTANT: Get categoryId from the map, not the name ---
        String categoryId = categoryNameToIdMap.get(categoryName);
        if (categoryId == null) {
            Toast.makeText(this, "Please select a valid category from the list.", Toast.LENGTH_LONG).show();
            return;
        }

        progressDialog.setTitle("Publishing Product");
        progressDialog.show();
        // Collect the specifications from the new adapter
        Map<String, String> specifications = manualSpecAdapter.getSpecificationsMap();


        Product product = new Product();
        product.setName(name);
        product.setBrand(inputBrand.getText().toString().trim());
        product.setDescription(inputDescription.getText().toString().trim());
        product.setPrice(Double.parseDouble(priceStr));
        product.setStockQuantity(Long.parseLong(stockStr));
        product.setIs_new(switchIsNew.isChecked());
        product.setCategoryId(categoryId); // Use the ID for the product document
        product.setSellerId(FirebaseAuth.getInstance().getCurrentUser().getUid());

        String costPriceStr = inputCostPrice.getText().toString().trim();
        if (!TextUtils.isEmpty(costPriceStr)) {
            product.setCostPrice(Double.parseDouble(costPriceStr));
        }

        String discountStr = inputDiscount.getText().toString().trim();
        if (!TextUtils.isEmpty(discountStr)) {
            product.setDiscountPercent(Long.parseLong(discountStr));
        }

        String tagsString = inputTags.getText().toString().toLowerCase().trim();
        if (!tagsString.isEmpty()) {
            List<String> tags = Arrays.asList(tagsString.split("\\s*,\\s*"));
            product.setTags_lowercase(tags);
        }

        product.setSpecifications(specifications);

        uploadImagesAndSaveProduct(product);
    }

    private void uploadImagesAndSaveProduct(Product product) {
        List<String> downloadUrls = new ArrayList<>();
        List<Task<Uri>> uploadTasks = new ArrayList<>();

        String productId = firestore.collection("products").document().getId();

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

        Tasks.whenAllSuccess(uploadTasks).addOnSuccessListener(urls -> {
            for (Object url : urls) {
                downloadUrls.add(url.toString());
            }
            product.setImageUrls(downloadUrls);

            firestore.collection("products").document(productId)
                    .set(product)
                    .addOnSuccessListener(aVoid -> {
                        progressDialog.dismiss();
                        Toast.makeText(AddProductActivity.this, "Product published successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(AddProductActivity.this, "Error saving product: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toast.makeText(AddProductActivity.this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}