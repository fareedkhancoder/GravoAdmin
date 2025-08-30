package com.gravo.gravoadmin;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ManualSpecAdapter extends RecyclerView.Adapter<ManualSpecAdapter.SpecViewHolder> {

    private final List<ManualSpecification> specList;
    private final OnSpecRemoveListener removeListener;

    public interface OnSpecRemoveListener {
        void onRemove(int position);
    }

    public ManualSpecAdapter(List<ManualSpecification> specList, OnSpecRemoveListener removeListener) {
        this.specList = specList;
        this.removeListener = removeListener;
    }

    @NonNull
    @Override
    public SpecViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_manual_spec_input, parent, false);
        return new SpecViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SpecViewHolder holder, int position) {
        holder.bind(specList.get(position));
    }

    @Override
    public int getItemCount() {
        return specList.size();
    }

    public Map<String, String> getSpecificationsMap() {
        // Filter out empty entries and collect into a Map
        return specList.stream()
                .filter(spec -> spec.getName() != null && !spec.getName().trim().isEmpty() &&
                        spec.getValue() != null && !spec.getValue().trim().isEmpty())
                .collect(Collectors.toMap(ManualSpecification::getName, ManualSpecification::getValue));
    }

    class SpecViewHolder extends RecyclerView.ViewHolder {
        TextInputEditText inputSpecName, inputSpecValue;
        ImageButton buttonRemoveSpec;

        public SpecViewHolder(@NonNull View itemView) {
            super(itemView);
            inputSpecName = itemView.findViewById(R.id.input_spec_name);
            inputSpecValue = itemView.findViewById(R.id.input_spec_value);
            buttonRemoveSpec = itemView.findViewById(R.id.button_remove_spec);

            buttonRemoveSpec.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    removeListener.onRemove(position);
                }
            });
        }

        void bind(ManualSpecification spec) {
            // Remove old listeners to prevent issues
            if (inputSpecName.getTag() instanceof TextWatcher) {
                inputSpecName.removeTextChangedListener((TextWatcher) inputSpecName.getTag());
            }
            if (inputSpecValue.getTag() instanceof TextWatcher) {
                inputSpecValue.removeTextChangedListener((TextWatcher) inputSpecValue.getTag());
            }

            // Set current data
            inputSpecName.setText(spec.getName());
            inputSpecValue.setText(spec.getValue());

            // Add new listeners to update the model as the user types
            TextWatcher nameWatcher = new SimpleTextWatcher(s -> spec.setName(s));
            inputSpecName.addTextChangedListener(nameWatcher);
            inputSpecName.setTag(nameWatcher);

            TextWatcher valueWatcher = new SimpleTextWatcher(s -> spec.setValue(s));
            inputSpecValue.addTextChangedListener(valueWatcher);
            inputSpecValue.setTag(valueWatcher);
        }
    }

    // Helper class to reduce boilerplate for TextWatcher
    private static class SimpleTextWatcher implements TextWatcher {
        private final java.util.function.Consumer<String> afterTextChanged;

        SimpleTextWatcher(java.util.function.Consumer<String> afterTextChanged) {
            this.afterTextChanged = afterTextChanged;
        }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) {
            afterTextChanged.accept(s.toString());
        }
    }
}