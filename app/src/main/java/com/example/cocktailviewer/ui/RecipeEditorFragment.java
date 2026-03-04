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
    private static final String ARG_EDIT_ID = "editRecipeId";

    public static RecipeEditorFragment newInstance() {
        return new RecipeEditorFragment();
    }

    public static RecipeEditorFragment newInstance(long recipeId) {
        RecipeEditorFragment f = new RecipeEditorFragment();
        Bundle b = new Bundle();
        b.putLong(ARG_EDIT_ID, recipeId);
        f.setArguments(b);
        return f;
    }

    private long editRecipeId = -1;
    private String originalName = null;

    private RecipeRepository repo;

    private EditText etName, etAbv, etSweet, etSour, etInstructions, etCategory, etCategoryDetail, etGlass;
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
        etCategory = v.findViewById(R.id.etCategory);
        etCategoryDetail = v.findViewById(R.id.etCategoryDetail);
        etGlass = v.findViewById(R.id.etGlass);

        btnAddRequired = v.findViewById(R.id.btnAddRequired);
        btnAddOptional = v.findViewById(R.id.btnAddOptional);
        btnSave = v.findViewById(R.id.btnSave);

        RecyclerView rvRequired = v.findViewById(R.id.rvRequired);
        RecyclerView rvOptional = v.findViewById(R.id.rvOptional);

        rvRequired.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvOptional.setLayoutManager(new LinearLayoutManager(requireContext()));

        requiredAdapter = new EditorIngredientAdapter(repo, pos -> requiredAdapter.removeRow(pos));
        optionalAdapter = new EditorIngredientAdapter(repo, pos -> optionalAdapter.removeRow(pos));

        rvRequired.setAdapter(requiredAdapter);
        rvOptional.setAdapter(optionalAdapter);

        requiredAdapter.addRow();

        btnAddRequired.setOnClickListener(view -> requiredAdapter.addRow());
        btnAddOptional.setOnClickListener(view -> optionalAdapter.addRow());

        btnSave.setOnClickListener(view -> saveRecipe());

        if (getArguments() != null && getArguments().containsKey(ARG_EDIT_ID)) {
            editRecipeId = getArguments().getLong(ARG_EDIT_ID, -1);
        }

        if (editRecipeId != -1) {
            Recipe r = repo.getRecipe(editRecipeId);
            if (r != null) {
                originalName = r.name;

                etName.setText(r.name);
                etCategory.setText(r.category == null ? "" : r.category);
                etCategoryDetail.setText(r.categoryDetail == null ? "" : r.categoryDetail);
                etAbv.setText(String.valueOf(r.abv));
                etSweet.setText(String.valueOf(r.sweet));
                etSour.setText(String.valueOf(r.sour));
                etInstructions.setText(r.instructions == null ? "" : r.instructions);
                etGlass.setText(r.glass == null ? "" : r.glass);

                // 재료 프리필
                List<RecipeIngredient> ris = repo.getRecipeIngredients(editRecipeId);
                requiredAdapter.clear();
                optionalAdapter.clear();

                boolean anyReq = false;
                for (RecipeIngredient ri : ris) {
                    if (!ri.optional) {
                        requiredAdapter.addRow(ri.ingredientName, ri.amount);
                        anyReq = true;
                    }
                }
                if (!anyReq) requiredAdapter.addRow();

                for (RecipeIngredient ri : ris) {
                    if (ri.optional) {
                        optionalAdapter.addRow(ri.ingredientName, ri.amount);
                    }
                }

                btnSave.setText("저장");
            }
        }
    }

    private void saveRecipe() {
        String name = txt(etName).trim();
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "칵테일 이름을 입력해야합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        int abv = parseIntSafe(txt(etAbv), 0);
        int sweet = clamp(parseIntSafe(txt(etSweet), 0), 0, 10);
        int sour = clamp(parseIntSafe(txt(etSour), 0), 0, 10);
        String instructions = txt(etInstructions);

        Recipe r = new Recipe();
        r.id = editRecipeId;
        r.name = name;
        r.category = txt(etCategory).trim();
        r.categoryDetail = txt(etCategoryDetail).trim();
        r.glass = txt(etGlass).trim();
        r.abv = Math.max(0, abv);
        r.sweet = sweet;
        r.sour = sour;
        r.instructions = instructions == null ? "" : instructions;


        if (editRecipeId == -1) {
            if (repo.existsRecipeName(name)) {
                toast("이미 같은 이름의 레시피가 있습니다.");
                return;
            }
            long newId = repo.upsertRecipeByName(r);
            r.rating = 0;
            saveIngredients(newId);
        } else {
            if (repo.existsRecipeNameExceptId(name, editRecipeId)) {
                toast("이미 같은 이름의 레시피가 있습니다.");
                return;
            }
            Recipe old = repo.getRecipe(editRecipeId);
            r.favorite = old != null && old.favorite;
            r.rating = (old != null) ? old.rating : 0;

            repo.updateRecipeById(r);
            saveIngredients(editRecipeId);
        }

        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private void saveIngredients(long recipeId) {
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