package com.gravo.gravoadmin;

import static android.content.ContentValues.TAG;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
import com.gravo.gravoadmin.Database.AppDatabase;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class AddProductActivity extends AppCompatActivity {

    // --- MEMBER VARIABLES FOR PRODUCT IMAGES ---
    // --- MEMBER VARIABLES FOR PRODUCT IMAGES ---
    private ProductImageAdapter imageAdapter;
    private List<Uri> imageUris;

    // UPDATED: Changed from GetContent to GetMultipleContents
    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
                if (uris != null && !uris.isEmpty()) {
                    // Calculate the position where new images will start
                    int startPosition = imageUris.size();

                    // Add all selected URIs to the list
                    imageUris.addAll(uris);

                    // Notify the adapter that a range of items was inserted
                    imageAdapter.notifyItemRangeInserted(startPosition, uris.size());
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
    private Button buttonPublish, draft_button;
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
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_product);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

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
        if (getIntent().hasExtra("DRAFT_ID")) {
            int draftId = getIntent().getIntExtra("DRAFT_ID", -1);

            if (draftId != -1) {
                loadDraftData(draftId);
            }
        }
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
        draft_button = findViewById(R.id.button_save_draft);
        draft_button.setVisibility(View.GONE);


        inputProductName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed for this logic
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // This triggers every time the user types or deletes a character
                String input = s.toString().trim();

                if (input.isEmpty()) {
                    draft_button.setVisibility(View.GONE);
                } else {
                    draft_button.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed for this logic
            }
        });

        draft_button.setOnClickListener(v -> {
            String name = inputProductName.getText().toString().trim();
            // Do your save draft logic here...
            saveDraftToSQL();
            Intent intent = new Intent(AddProductActivity.this, Uploading.class);
            startActivity(intent);
            Toast.makeText(this, "Draft Saved: " + name, Toast.LENGTH_SHORT).show();
            finish();
        });


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



    private void loadDraftData(int draftId) {
        // 1. Get the draft from the database
        // Note: Database operations should strictly be async, but for a simple draft
        // fetch on the UI thread, this is acceptable for small data.
        Product draft = AppDatabase.getDatabase(this).draftDao().getDraftById(draftId);

        if (draft == null) return;

        // 2. Fill Simple Text Fields
        inputProductName.setText(draft.getName());
        inputDescription.setText(draft.getDescription());
        inputBrand.setText(draft.getBrand());

        // Convert numbers to string for EditTexts (handle 0.0 case if empty)
        if (draft.getPrice() > 0) inputSellingPrice.setText(String.valueOf(draft.getPrice()));
        if (draft.getCostPrice() > 0) inputCostPrice.setText(String.valueOf(draft.getCostPrice()));
        if (draft.getStockQuantity() > 0) inputStockQuantity.setText(String.valueOf(draft.getStockQuantity()));
        if (draft.getDiscountPercent() > 0) inputDiscount.setText(String.valueOf(draft.getDiscountPercent()));

        // 3. Handle Category
        // NOTE: Your database stores 'categoryId', but your input usually takes a Name.
        // If you have a helper to get Name from ID, use it here. Otherwise:
        inputCategory.setText(draft.getCategoryId());

        // 4. Handle Boolean Toggles
        switchIsNew.setChecked(draft.isIs_new());

        // 5. Handle Tags (List<String> -> Comma separated string)
        if (draft.getTags_lowercase() != null && !draft.getTags_lowercase().isEmpty()) {
            String tagStr = String.join(", ", draft.getTags_lowercase()); // Requires API 26+
            inputTags.setText(tagStr);
        }

        // --- 6. FIX: Load Specifications ---
        if (draft.getSpecifications() != null && !draft.getSpecifications().isEmpty()) {
            // CALL THE NEW FUNCTION WE CREATED IN STEP 1
            manualSpecAdapter.setSpecificationsFromMap(draft.getSpecifications());
        }

        // 7. Handle Images (String Paths -> Uris)
        if (draft.getImageUrls() != null && !draft.getImageUrls().isEmpty()) {
            imageUris.clear(); // Clear existing
            for (String path : draft.getImageUrls()) {
                File imgFile = new File(path);
                if (imgFile.exists()) {
                    imageUris.add(Uri.fromFile(imgFile));
                }
            }
            // Notify your images adapter
            if (imageAdapter != null) {
                imageAdapter.notifyDataSetChanged();
            }
        }
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
        product.setClicks(0);

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
    private void saveDraftToSQL() {
        Product draft = new Product();

        // 1. If editing an existing draft, keep the ID so it updates instead of creating new
        if (getIntent().hasExtra("DRAFT_ID")) {
            int draftId = getIntent().getIntExtra("DRAFT_ID", -1);
            if (draftId != -1) {
                draft.setDraftId(draftId);
            }
        }

        // 2. Set Basic Fields
        draft.setName(inputProductName.getText().toString());
        draft.setBrand(inputBrand.getText().toString());
        draft.setDescription(inputDescription.getText().toString());
        draft.setCategoryId(inputCategory.getText().toString());
        draft.setIs_new(switchIsNew.isChecked());

        // Set Numbers (safely handle empty strings)
        String price = inputSellingPrice.getText().toString();
        if (!price.isEmpty()) draft.setPrice(Double.parseDouble(price));

        String stock = inputStockQuantity.getText().toString();
        if (!stock.isEmpty()) draft.setStockQuantity(Long.parseLong(stock));

        String cost = inputCostPrice.getText().toString();
        if (!cost.isEmpty()) draft.setCostPrice(Double.parseDouble(cost));

        String discount = inputDiscount.getText().toString();
        if (!discount.isEmpty()) draft.setDiscountPercent(Long.parseLong(discount));

        // --- 3. NEW: SAVE TAGS ---
        String tagsString = inputTags.getText().toString().trim().toLowerCase();
        if (!tagsString.isEmpty()) {
            // Split by comma (e.g., "watch, smart, red" -> ["watch", "smart", "red"])
            List<String> tagsList = Arrays.asList(tagsString.split("\\s*,\\s*"));
            draft.setTags_lowercase(tagsList);
        }

        // 4. Save Images (Your existing logic)
        List<String> savedInternalImagePaths = new ArrayList<>();
        if (imageUris != null && !imageUris.isEmpty()) {
            for (Uri uri : imageUris) {
                String path = saveImageToInternalStorage(uri);
                if (path != null) savedInternalImagePaths.add(path);
            }
        }
        draft.setImageUrls(savedInternalImagePaths);

        // Firebase se current user ki ID nikaal kar save karein
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            draft.setSellerId(FirebaseAuth.getInstance().getCurrentUser().getUid());
        }

        // --- NEW: ADD CREATED AT DATE ---
        // Current time save karein
        draft.setCreatedAt(new java.util.Date());

        // 5. Save Specifications (Your existing logic)
        draft.setSpecifications(manualSpecAdapter.getSpecificationsMap());

        // 6. Insert/Update in Database
        AppDatabase.getDatabase(this).draftDao().insertDraft(draft);

        Toast.makeText(this, "Draft Saved!", Toast.LENGTH_SHORT).show();
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


    // COPY THIS ENTIRE FUNCTION INTO AddProductActivity.java

    private String saveImageToInternalStorage(android.net.Uri uri) {
        try {
            // 1. Get the bitmap from the Uri
            android.graphics.Bitmap bitmap = android.provider.MediaStore.Images.Media.getBitmap(getContentResolver(), uri);

            // 2. Create a unique filename (using timestamp ensures it is unique)
            String filename = "draft_img_" + System.currentTimeMillis() + ".jpg";

            // 3. Create the file in the app's private internal storage
            java.io.File directory = getFilesDir(); // Private folder for this app
            java.io.File file = new java.io.File(directory, filename);

            // 4. Compress the bitmap and write it to the file
            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, fos); // 100 = Max Quality
            fos.close();

            // 5. Return the absolute path (String) so we can save it in the database
            return file.getAbsolutePath();

        } catch (java.io.IOException e) {
            e.printStackTrace();
            return null; // Return null if something went wrong
        }
    }
}