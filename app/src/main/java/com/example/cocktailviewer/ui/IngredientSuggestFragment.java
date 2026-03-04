package com.example.cocktailviewer.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cocktailviewer.MainActivity;
import com.example.cocktailviewer.R;
import com.example.cocktailviewer.data.RecipeRepository;

import java.util.List;

public class IngredientSuggestFragment extends Fragment {

    public static IngredientSuggestFragment newInstance() {
        return new IngredientSuggestFragment();
    }

    private RecipeRepository repo;
    private IngredientSuggestAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ingredient_suggest, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        repo = ((MainActivity) requireActivity()).repo();

        RecyclerView rv = v.findViewById(R.id.rvSuggest);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new IngredientSuggestAdapter(s -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("구매 처리")
                    .setMessage("'" + s.name + "' 를 구매했습니까?")
                    .setPositiveButton("예", (d, w) -> {
                        repo.setIngredientHas(s.name, true);
                        reload();
                    })
                    .setNegativeButton("아니오", null)
                    .show();
        });

        rv.setAdapter(adapter);

        reload();
    }

    private void reload() {
        List<RecipeRepository.IngredientSuggest> list = repo.getNextBuySuggestions(3);
        adapter.submit(list);
    }
}