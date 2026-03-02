package com.example.cocktailviewer.ui;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cocktailviewer.R;

import java.util.ArrayList;
import java.util.List;

public class EditorIngredientAdapter extends RecyclerView.Adapter<EditorIngredientAdapter.VH> {

    public interface Listener {
        void onRemove(int position);
    }

    private final Listener listener;
    private final List<EditorIngredientRow> rows = new ArrayList<>();

    public EditorIngredientAdapter(Listener listener) {
        this.listener = listener;
    }

    public void addRow() {
        rows.add(new EditorIngredientRow());
        notifyItemInserted(rows.size() - 1);
    }

    public void removeRow(int pos) {
        if (pos < 0 || pos >= rows.size()) return;
        rows.remove(pos);
        notifyItemRemoved(pos);
        notifyItemRangeChanged(pos, rows.size() - pos);
    }

    public List<EditorIngredientRow> getRows() {
        return rows;
    }

    @NonNull
    @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_editor_ingredient_row, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int position) {
        EditorIngredientRow row = rows.get(position);

        // 기존 watcher 제거 후 재등록 (recycle 대응)
        h.bind(row);

        h.btnRemove.setOnClickListener(v -> listener.onRemove(h.getBindingAdapterPosition()));
    }

    @Override public int getItemCount() { return rows.size(); }

    static class VH extends RecyclerView.ViewHolder {
        EditText etName, etAmount;
        ImageButton btnRemove;

        TextWatcher nameWatcher;
        TextWatcher amountWatcher;

        VH(@NonNull View itemView) {
            super(itemView);
            etName = itemView.findViewById(R.id.etIngName);
            etAmount = itemView.findViewById(R.id.etAmount);
            btnRemove = itemView.findViewById(R.id.btnRemoveRow);
        }

        void bind(EditorIngredientRow row) {
            if (nameWatcher != null) etName.removeTextChangedListener(nameWatcher);
            if (amountWatcher != null) etAmount.removeTextChangedListener(amountWatcher);

            etName.setText(row.name);
            etAmount.setText(row.amount);

            nameWatcher = new SimpleWatcher(text -> row.name = text);
            amountWatcher = new SimpleWatcher(text -> row.amount = text);

            etName.addTextChangedListener(nameWatcher);
            etAmount.addTextChangedListener(amountWatcher);
        }
    }

    interface OnText { void onText(String s); }

    static class SimpleWatcher implements TextWatcher {
        private final OnText onText;
        SimpleWatcher(OnText onText) { this.onText = onText; }

        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            onText.onText(s == null ? "" : s.toString());
        }
        @Override public void afterTextChanged(Editable s) {}
    }
}