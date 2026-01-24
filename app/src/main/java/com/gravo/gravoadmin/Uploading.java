package com.gravo.gravoadmin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gravo.gravoadmin.Database.AppDatabase;
import java.util.List;

public class Uploading extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DraftsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_uploading);

        // 1. Setup Window Insets (Your existing code)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 2. Initialize RecyclerView
        recyclerView = findViewById(R.id.ordersRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 3. Initialize Adapter with Click Listener
        adapter = new DraftsAdapter(this, product -> {
            // HANDLE CLICK: Use the generic AddProductActivity (replace with your actual class name)
            Intent intent = new Intent(Uploading.this, AddProductActivity.class);
            intent.putExtra("DRAFT_ID", product.getDraftId()); // Pass ID to load it
            startActivity(intent);
            finish();
        });

        recyclerView.setAdapter(adapter);

        // 4. Setup Toolbar Back Button
        findViewById(R.id.toolbarMyOrders).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Load drafts every time the screen appears (in case user added/deleted one)
        loadDrafts();
    }

    private void loadDrafts() {
        // Fetch data from Room Database
        List<Product> myDrafts = AppDatabase.getDatabase(this).draftDao().getAllDrafts();

        // Update adapter
        if (adapter != null) {
            adapter.setList(myDrafts);
        }
    }
}