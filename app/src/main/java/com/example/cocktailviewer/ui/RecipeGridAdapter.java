package com.example.cocktailviewer.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cocktailviewer.R;
import com.example.cocktailviewer.model.Recipe;

import java.util.ArrayList;
import java.util.List;

public class RecipeGridAdapter extends RecyclerView.Adapter<RecipeGridAdapter.VH> {

    public interface Listener {
        void onClick(Recipe r);
        void onToggleFavorite(Recipe r);
    }

    private final Listener listener;
    private final List<Recipe> items = new ArrayList<>();

    public RecipeGridAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Recipe> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recipe_grid, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Recipe r = items.get(position);
        h.tvName.setText(r.name);
        h.tvAbv.setText("도수: " + r.abv + "%");
        h.tvSweet.setText("달콤: " + stars(r.sweet));
        h.tvSour.setText("상큼: " + stars(r.sour));
        h.btnStar.setImageResource(r.favorite ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
        int rating = Math.max(0, Math.min(10, r.rating));
        h.tvRating.setText("별점: " + rating + "/10");

        h.itemView.setOnClickListener(v -> listener.onClick(r));
        h.btnStar.setOnClickListener(v -> listener.onToggleFavorite(r));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvAbv, tvSweet, tvSour, tvRating;
        ImageButton btnStar;
        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvAbv = itemView.findViewById(R.id.tvAbv);
            tvSweet = itemView.findViewById(R.id.tvSweet);
            tvSour = itemView.findViewById(R.id.tvSour);
            btnStar = itemView.findViewById(R.id.btnStar);
            tvRating = itemView.findViewById(R.id.tvRating);
        }
    }

    private static String stars(int v0to10) {
        int five = Math.max(0, Math.min(5, (int)Math.round(v0to10 / 2.0)));
        StringBuilder sb = new StringBuilder();
        for (int i=0;i<5;i++) sb.append(i < five ? "★" : "☆");
        return sb.toString();
    }
}