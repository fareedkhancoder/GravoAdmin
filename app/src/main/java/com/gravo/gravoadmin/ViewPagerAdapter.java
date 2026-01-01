package com.gravo.gravoadmin;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ViewPagerAdapter extends FragmentStateAdapter {

    private final String[] titles = new String[]{
            "Pending", // Updated to match Activity titles for consistency
            "Processing",
            "Shipped",
            "Out for Delivery",
            "Delivered"
    };

    private String filterType; // 1. Store the filter

    // 2. Update Constructor to accept filterType
    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity, String filterType) {
        super(fragmentActivity);
        this.filterType = filterType;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // 3. Pass both Status AND FilterType to the fragment
        return OrderFragment.newInstance(titles[position], filterType);
    }

    @Override
    public int getItemCount() {
        return titles.length;
    }
}