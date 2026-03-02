package com.example.cocktailviewer.ui;

import android.os.Bundle;
import android.view.*;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cocktailviewer.MainActivity;
import com.example.cocktailviewer.R;
import com.example.cocktailviewer.data.RecipeRepository;
import com.example.cocktailviewer.model.Recipe;
import com.example.cocktailviewer.model.RecipeIngredient;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class RecipeEditorFragment extends Fragment {

    public static RecipeEditorFragment newInstance() { return new RecipeEditorFragment(); }

    private RecipeRepository repo;

    private EditText etName, etAbv, etSweet, etSour, etInstructions;
    private MaterialButton btnAddRequired, btnAddOptional, btnSave;

    private EditorIngredientAdapter requiredAdapter;
    private EditorIngredientAdapter optionalAdapter;

    @Nullable
    @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recipe_editor, container, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        repo = ((MainActivity)requireActivity()).repo();

        etName = v.findViewById(R.id.etName);
        etAbv = v.findViewById(R.id.etAbv);
        etSweet = v.findViewById(R.id.etSweet);
        etSour = v.findViewById(R.id.etSour);
        etInstructions = v.findViewById(R.id.etInstructions);

        btnAddRequired = v.findViewById(R.id.btnAddRequired);
        btnAddOptional = v.findViewById(R.id.btnAddOptional);
        btnSave = v.findViewById(R.id.btnSave);

        RecyclerView rvRequired = v.findViewById(R.id.rvRequired);
        RecyclerView rvOptional = v.findViewById(R.id.rvOptional);

        rvRequired.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvOptional.setLayoutManager(new LinearLayoutManager(requireContext()));

        requiredAdapter = new EditorIngredientAdapter(pos -> requiredAdapter.removeRow(pos));
        optionalAdapter = new EditorIngredientAdapter(pos -> optionalAdapter.removeRow(pos));

        rvRequired.setAdapter(requiredAdapter);
        rvOptional.setAdapter(optionalAdapter);

        // 기본으로 한 줄씩 만들어두면 UX가 좋음
        requiredAdapter.addRow();

        btnAddRequired.setOnClickListener(view -> requiredAdapter.addRow());
        btnAddOptional.setOnClickListener(view -> optionalAdapter.addRow());

        btnSave.setOnClickListener(view -> saveRecipe());
    }

    private void saveRecipe() {
        String name = txt(etName).trim();
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "칵테일 이름을 입력해줘", Toast.LENGTH_SHORT).show();
            return;
        }

        int abv = parseIntSafe(txt(etAbv), 0);
        int sweet = clamp(parseIntSafe(txt(etSweet), 0), 0, 10);
        int sour = clamp(parseIntSafe(txt(etSour), 0), 0, 10);
        String instructions = txt(etInstructions);

        Recipe r = new Recipe();
        r.name = name;
        r.abv = Math.max(0, abv);
        r.sweet = sweet;
        r.sour = sour;
        r.instructions = instructions == null ? "" : instructions;
        r.favorite = false;

        long recipeId = repo.upsertRecipeByName(r);

        List<RecipeIngredient> all = new ArrayList<>();
        for (EditorIngredientRow row : requiredAdapter.getRows()) {
            String n = safe(row.name);
            if (n.isEmpty()) continue;
            RecipeIngredient ri = new RecipeIngredient();
            ri.recipeId = recipeId;
            ri.ingredientName = n;
            ri.amount = safe(row.amount);
            ri.optional = false;
            all.add(ri);
        }
        for (EditorIngredientRow row : optionalAdapter.getRows()) {
            String n = safe(row.name);
            if (n.isEmpty()) continue;
            RecipeIngredient ri = new RecipeIngredient();
            ri.recipeId = recipeId;
            ri.ingredientName = n;
            ri.amount = safe(row.amount);
            ri.optional = true;
            all.add(ri);
        }

        repo.replaceRecipeIngredients(recipeId, all);

        Toast.makeText(requireContext(), "등록 완료: " + name, Toast.LENGTH_SHORT).show();
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private static String txt(EditText et) {
        return et.getText() == null ? "" : et.getText().toString();
    }

    private static int parseIntSafe(String s, int def) {
        try {
            String t = s == null ? "" : s.trim();
            if (t.isEmpty()) return def;
            return Integer.parseInt(t);
        } catch (Exception e) {
            return def;
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}