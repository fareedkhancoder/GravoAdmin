package com.gravo.gravoadmin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class OrderFragment extends Fragment {

    private static final String ARG_STATUS = "order_status";
    private String status;

    // 1. Helper method to create a new instance of this fragment with specific data
    public static OrderFragment newInstance(String status) {
        OrderFragment fragment = new OrderFragment();
        Bundle args = new Bundle();
        args.putString(ARG_STATUS, status);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            status = getArguments().getString(ARG_STATUS);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Create a simple layout for testing (or use your existing recycler view layout)
        // For now, I'm returning a simple TextView to prove it works
        TextView textView = new TextView(getContext());
        textView.setText("Showing: " + status);
        textView.setGravity(android.view.Gravity.CENTER);
        return textView;

        // LATER: Replace the above lines with:
        // return inflater.inflate(R.layout.fragment_order_list, container, false);
    }
}