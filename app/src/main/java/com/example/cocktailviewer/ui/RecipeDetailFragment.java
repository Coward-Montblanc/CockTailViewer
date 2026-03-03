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
        TextView tvIngredients = v.findViewById(R.id.tvIngredients);
        TextView tvGlass = v.findViewById(R.id.tvGlass);
        TextView tvRecipe = v.findViewById(R.id.tvRecipe);
        TextView tvRatingValue = view.findViewById(R.id.tvRatingValue);
        SeekBar sbRating = view.findViewById(R.id.sbRating);

        if (r == null) {
            tvTitle.setText("Not Found");
            tvIngredients.setText("");
            tvGlass.setVisibility(View.GONE);
            tvRecipe.setText("");
            return;
        }
        v.findViewById(R.id.btnDelete).setOnClickListener(view -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("삭제")
                    .setMessage("이 레시피를 삭제합니까?")
                    .setPositiveButton("예", (d, w) -> {
                        repo.deleteRecipe(r.id);
                        requireActivity().getSupportFragmentManager().popBackStack();
                    })
                    .setNegativeButton("아니오", null)
                    .show();
        });
        v.findViewById(R.id.btnEdit).setOnClickListener(view -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, RecipeEditorFragment.newInstance(r.id))
                    .addToBackStack(null)
                    .commit();
        });
        sbRating.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvRatingValue.setText("별점: " + progress + "/10");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                int v = seekBar.getProgress();
                repo.setRating(recipe.id, v); // repo는 네가 쓰는 repository 인스턴스
                recipe.rating = v;            // 메모리 값도 갱신(옵션)
            }
        });
        tvTitle.setText(r.name);
        List<RecipeRepository.IngredientLine> lines =
                repo.getIngredientsOfRecipe(r.id);

        StringBuilder sb = new StringBuilder();

        // 필수재료
        for (RecipeRepository.IngredientLine l : lines) {
            if (!l.optional) {
                sb.append(l.name).append(" ").append(l.amount).append("\n");
            }
        }

        // 선택재료
        boolean hasOptional = false;
        for (RecipeRepository.IngredientLine l : lines) {
            if (l.optional) { hasOptional = true; break; }
        }
        if (hasOptional) {
            sb.append("\n");
            for (RecipeRepository.IngredientLine l : lines) {
                if (l.optional) {
                    sb.append("(선택) ").append(l.name).append(" ").append(l.amount).append("\n");
                }
            }
        }

        tvIngredients.setText(sb.toString().trim());
        tvRecipe.setText(r.instructions == null ? "" : r.instructions);

        sbRating.setProgress(Math.max(0, Math.min(10, recipe.rating)));
        tvRatingValue.setText("별점: " + sbRating.getProgress() + "/10");

        // glass 표시
        String g = (r.glass == null) ? "" : r.glass.trim();
        if (g.isEmpty()) {
            tvGlass.setVisibility(View.GONE);
        } else {
            tvGlass.setVisibility(View.VISIBLE);
            tvGlass.setText("잔: " + g);
        }
    }
}
