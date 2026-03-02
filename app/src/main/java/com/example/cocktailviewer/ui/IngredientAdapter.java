package com.example.cocktailviewer.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cocktailviewer.R;
import com.example.cocktailviewer.model.Ingredient;

import java.util.ArrayList;
import java.util.List;

public class IngredientAdapter extends RecyclerView.Adapter<IngredientAdapter.VH> {

    public interface Listener {
        void onToggleHas(Ingredient i, boolean has);
        void onDelete(Ingredient i);
    }

    private final Listener listener;
    private final List<Ingredient> items = new ArrayList<>();

    public IngredientAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Ingredient> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ingredient, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int position) {
        Ingredient i = items.get(position);
        h.tvName.setText(i.name);

        h.swHas.setOnCheckedChangeListener(null);
        h.swHas.setChecked(i.has);
        h.swHas.setOnCheckedChangeListener((buttonView, isChecked) -> listener.onToggleHas(i, isChecked));

        h.btnDelete.setOnClickListener(v -> listener.onDelete(i));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName;
        Switch swHas;
        ImageButton btnDelete;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvIngredientName);
            swHas = itemView.findViewById(R.id.swHas);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
