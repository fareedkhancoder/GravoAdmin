package com.gravo.gravoadmin;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.gravo.gravoadmin.OrderFragment;

public class ViewPagerAdapter extends FragmentStateAdapter {

    // Define your tab titles/statuses here
    private final String[] titles = new String[]{
            "Pending Orders",
            "Processing Orders",
            "Shipped Orders",
            "Out for Delivery",
            "Delivered"
    };

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Pass the specific status string to the fragment
        // e.g., if position is 0, it passes "Pending Orders"
        return OrderFragment.newInstance(titles[position]);
    }

    @Override
    public int getItemCount() {
        return titles.length; // Returns 5
    }
}