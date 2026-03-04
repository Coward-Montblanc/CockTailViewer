package com.example.cocktailviewer.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cocktailviewer.R;
import com.example.cocktailviewer.data.RecipeRepository;

import java.util.ArrayList;
import java.util.List;

public class IngredientSuggestAdapter extends RecyclerView.Adapter<IngredientSuggestAdapter.VH> {

    public interface Listener {
        void onClick(RecipeRepository.IngredientSuggest s);
    }

    private final Listener listener;
    private final ArrayList<RecipeRepository.IngredientSuggest> items = new ArrayList<>();

    public IngredientSuggestAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<RecipeRepository.IngredientSuggest> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ingredient_suggest, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        RecipeRepository.IngredientSuggest s = items.get(position);

        h.tvName.setText(s.name);
        h.tvCount.setText("사면 완성: " + s.count + "개");

        if (s.samples == null || s.samples.isEmpty()) {
            h.tvSamples.setText("예: (없음)");
        } else {
            String joined = String.join(", ", s.samples);
            h.tvSamples.setText("예: " + joined);
        }

        h.itemView.setOnClickListener(v -> listener.onClick(s));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvCount, tvSamples;
        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvCount = itemView.findViewById(R.id.tvCount);
            tvSamples = itemView.findViewById(R.id.tvSamples);
        }
    }
}