package com.example.cocktailviewer.ui;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cocktailviewer.MainActivity;
import com.example.cocktailviewer.R;
import com.example.cocktailviewer.data.RecipeRepository;
import com.example.cocktailviewer.model.Recipe;

import java.util.List;

public class AllRecipesFragment extends Fragment {

    public static AllRecipesFragment newInstance() { return new AllRecipesFragment(); }

    private RecipeRepository repo;
    private RecipeGridAdapter adapter;

    @Nullable
    @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_all_recipes, container, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        repo = ((MainActivity) requireActivity()).repo();

        RecyclerView rv = v.findViewById(R.id.rvAllRecipes);
        rv.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        adapter = new RecipeGridAdapter(new RecipeGridAdapter.Listener() {
            @Override public void onClick(Recipe r) {
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, RecipeDetailFragment.newInstance(r.id))
                        .addToBackStack(null)
                        .commit();
            }

            @Override public void onToggleFavorite(Recipe r) {
                repo.setFavorite(r.id, !r.favorite);
                reload();
            }
        });

        rv.setAdapter(adapter);

        reload();
    }

    private void reload() {
        List<Recipe> list = repo.getAllRecipes();   // ✅ 필터 없이 전체
        adapter.submit(list);
    }
    @Override
    public void onResume() {
        super.onResume();
        if (repo != null && adapter != null) {
            reload();
        }
    }
}