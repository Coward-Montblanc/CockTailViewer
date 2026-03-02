package com.example.cocktailviewer.ui;

import android.os.Bundle;
import android.view.*;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.cocktailviewer.MainActivity;
import com.example.cocktailviewer.R;
import com.example.cocktailviewer.data.RecipeRepository;
import com.example.cocktailviewer.model.Ingredient;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class IngredientFragment extends Fragment {

    public static IngredientFragment newInstance() { return new IngredientFragment(); }

    private RecipeRepository repo;

    private EditText etNew;
    private MaterialButton btnAdd;
    private IngredientAdapter adapter;

    @Nullable
    @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ingredients, container, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        repo = ((MainActivity)requireActivity()).repo();

        etNew = v.findViewById(R.id.etNewIngredient);
        btnAdd = v.findViewById(R.id.btnAddIngredient);

        androidx.recyclerview.widget.RecyclerView rv = v.findViewById(R.id.rvIngredients);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new IngredientAdapter(new IngredientAdapter.Listener() {
            @Override public void onToggleHas(Ingredient i, boolean has) {
                repo.setIngredientHas(i.id, has);
                reload();
            }

            @Override public void onDelete(Ingredient i) {
                repo.deleteIngredient(i.id);
                reload();
            }
        });
        rv.setAdapter(adapter);

        btnAdd.setOnClickListener(view -> {
            String name = etNew.getText() == null ? "" : etNew.getText().toString().trim();
            if (name.isEmpty()) return;

            repo.addIngredientIfNotExists(name);
            etNew.setText("");
            Toast.makeText(requireContext(), "추가됨: " + name, Toast.LENGTH_SHORT).show();
            reload();
        });

        reload();
    }

    private void reload() {
        List<Ingredient> list = repo.getAllIngredients();
        adapter.submit(list);
    }
}
