package com.example.cocktailviewer.ui;

import android.os.Bundle;
import android.view.*;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cocktailviewer.MainActivity;
import com.example.cocktailviewer.R;
import com.example.cocktailviewer.data.RecipeRepository;
import com.example.cocktailviewer.model.Recipe;

import java.util.List;

public class RecipeDetailFragment extends Fragment {

    private static final String ARG_ID = "recipeId";

    public static RecipeDetailFragment newInstance(long recipeId) {
        RecipeDetailFragment f = new RecipeDetailFragment();
        Bundle b = new Bundle();
        b.putLong(ARG_ID, recipeId);
        f.setArguments(b);
        return f;
    }

    private RecipeRepository repo;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recipe_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        repo = ((MainActivity)requireActivity()).repo();

        long id = requireArguments().getLong(ARG_ID);
        repo.markViewed(id);

        Recipe r = repo.getRecipe(id);

        TextView tvTitle = v.findViewById(R.id.tvTitle);
        TextView tvIns = v.findViewById(R.id.tvInstructions);

        if (r == null) {
            tvTitle.setText("Not Found");
            tvIns.setText("");
            return;
        }
        tvTitle.setText(r.name);
        List<RecipeRepository.IngredientLine> lines =
                repo.getIngredientsOfRecipe(r.id);

        StringBuilder sb = new StringBuilder();

// 1️⃣ 필수재료
        for (RecipeRepository.IngredientLine l : lines) {
            if (!l.optional) {
                sb.append(l.name)
                        .append(" ")
                        .append(l.amount)
                        .append("\n");
            }
        }

// 2️⃣ 선택재료
        boolean hasOptional = false;
        for (RecipeRepository.IngredientLine l : lines) {
            if (l.optional) {
                hasOptional = true;
                break;
            }
        }

        if (hasOptional) {
            sb.append("\n");
            for (RecipeRepository.IngredientLine l : lines) {
                if (l.optional) {
                    sb.append("(선택) ")
                            .append(l.name)
                            .append(" ")
                            .append(l.amount)
                            .append("\n");
                }
            }
        }

// 3️⃣ 두칸 줄바꿈 + 레시피 본문
        sb.append("\n\n");
        sb.append(r.instructions);

        tvIns.setText(sb.toString());
    }
}
