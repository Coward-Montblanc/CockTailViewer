package com.example.cocktailviewer.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cocktailviewer.MainActivity;
import com.example.cocktailviewer.R;
import com.example.cocktailviewer.data.RecipeRepository;
import com.example.cocktailviewer.model.Recipe;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.*;

public class HomeFragment extends Fragment {

    public static HomeFragment newInstance() { return new HomeFragment(); }

    private RecipeRepository repo;

    private EditText etSearch;
    private TextView tvAbvRange;
    private SeekBar sbMin, sbMax;
    private Spinner spSort;
    private Button btnRecent, btnFav;

    private boolean showRecent = false;
    private boolean showFav = false;

    private Button btnIngredientCombo;
    
    private HashSet<String> selectedCombo = null;

    private List<Recipe> baseList = new ArrayList<>();
    private RecipeGridAdapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        repo = ((MainActivity)requireActivity()).repo();

        etSearch = v.findViewById(R.id.etSearch);
        tvAbvRange = v.findViewById(R.id.tvAbvRange);
        sbMin = v.findViewById(R.id.sbAbvMin);
        sbMax = v.findViewById(R.id.sbAbvMax);
        spSort = v.findViewById(R.id.spSort);
        btnRecent = v.findViewById(R.id.btnRecent);
        btnFav = v.findViewById(R.id.btnFav);
        btnIngredientCombo = v.findViewById(R.id.btnIngredientCombo);

        Button btnRandom = v.findViewById(R.id.btnRandom);

        btnRandom.setOnClickListener(view -> {
            Long id = repo.getRandomRecipeId();
            if (id == null) {
                Toast.makeText(requireContext(), "레시피가 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, RecipeDetailFragment.newInstance(id))
                    .addToBackStack(null)
                    .commit();
        });

        RecyclerView rv = v.findViewById(R.id.rvGrid);
        rv.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        adapter = new RecipeGridAdapter(new RecipeGridAdapter.Listener() {
            @Override public void onClick(Recipe r) {
                // 상세로 이동
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
        FloatingActionButton fabToTop = v.findViewById(R.id.fabToTop);
        fabToTop.setOnClickListener(view -> rv.smoothScrollToPosition(0));
        fabToTop.hide();
        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                GridLayoutManager glm = (GridLayoutManager) recyclerView.getLayoutManager();
                if (glm == null) return;
                int first = glm.findFirstVisibleItemPosition();
                if (first > 6) fabToTop.show();
                else fabToTop.hide();
            }
        });

        setupSortSpinner();
        setupListeners();

        reload();
    }

    @Override
    public void onResume() {
        super.onResume();
        // 재료 토글/레시피 추가 후 돌아오면 갱신되도록
        reload();
    }

    private void setupSortSpinner() {
        // 간단히: 이름/도수/달콤/상큼 + (오름/내림은 항목에 포함)
        List<String> items = Arrays.asList(
                "이름(오름)", "이름(내림)",
                "도수(오름)", "도수(내림)",
                "달콤함(오름)", "달콤함(내림)",
                "상큼함(오름)", "상큼함(내림)"
        );
        ArrayAdapter<String> aa = new ArrayAdapter<>(requireContext(), R.layout.spinner_text_white, items);
        aa.setDropDownViewResource(R.layout.spinner_dropdown_white);
        spSort.setAdapter(aa);
        spSort.setSelection(0);
    }

    private void setupListeners() {
        TextWatcher tw = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
            @Override public void afterTextChanged(Editable s) {}
        };
        etSearch.addTextChangedListener(tw);

        SeekBar.OnSeekBarChangeListener sbListener = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // min > max 방지
                if (sbMin.getProgress() > sbMax.getProgress()) {
                    if (seekBar == sbMin) sbMax.setProgress(sbMin.getProgress());
                    else sbMin.setProgress(sbMax.getProgress());
                }
                updateAbvLabel();
                applyFilters();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };
        sbMin.setOnSeekBarChangeListener(sbListener);
        sbMax.setOnSeekBarChangeListener(sbListener);

        spSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { applyFilters(); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnRecent.setOnClickListener(v -> {
            showRecent = !showRecent;
            btnRecent.setAlpha(showRecent ? 1.0f : 0.5f);
            applyFilters();
        });

        btnFav.setOnClickListener(v -> {
            showFav = !showFav;
            btnFav.setAlpha(showFav ? 1.0f : 0.5f);
            applyFilters();
        });

        btnIngredientCombo.setOnClickListener(v -> {openIngredientComboDialog(); btnIngredientCombo.setAlpha(selectedCombo != null ? 1.0f : 0.5f);});

        // 초기 상태 표시
        btnRecent.setAlpha(0.5f);
        btnFav.setAlpha(0.5f);
        updateAbvLabel();
        btnIngredientCombo.setAlpha(0.5f);
    }

    private void updateAbvLabel() {
        tvAbvRange.setText("도수: " + sbMin.getProgress() + "~" + sbMax.getProgress() + "%");
    }

    private void reload() {
        // DB에서 craftable만 가져오기
        if (selectedCombo == null) {
            baseList = repo.getCraftableRecipes();
        } else {
            baseList = repo.getCraftableRecipesBySelectedNames(selectedCombo);
        }
        applyFilters();
    }

    private void applyFilters() {
        String q = etSearch.getText() == null ? "" : etSearch.getText().toString().trim().toLowerCase(Locale.ROOT);
        int min = sbMin.getProgress();
        int max = sbMax.getProgress();

        List<Recipe> list = new ArrayList<>(baseList);

        if (showRecent) {
            List<Long> recentIds = repo.getRecentIds(10);
            Map<Long, Integer> order = new HashMap<>();
            for (int i=0;i<recentIds.size();i++) order.put(recentIds.get(i), i);

            List<Recipe> filtered = new ArrayList<>();
            for (Recipe r : list) {
                if (order.containsKey(r.id)) filtered.add(r);
            }
            filtered.sort(Comparator.comparingInt(a -> order.getOrDefault(a.id, Integer.MAX_VALUE)));
            list = filtered;
        }

        if (showFav) {
            List<Recipe> filtered = new ArrayList<>();
            for (Recipe r : list) if (r.favorite) filtered.add(r);
            list = filtered;
        }

        if (!q.isEmpty()) {
            HashSet<Long> matched = repo.searchRecipeIds(q);

            List<Recipe> filtered = new ArrayList<>();
            for (Recipe r : list) {
                if (matched.contains(r.id)) filtered.add(r);
            }
            list = filtered;
        }

        List<Recipe> filtered = new ArrayList<>();
        for (Recipe r : list) {
            if (r.abv >= min && r.abv <= max) filtered.add(r);
        }
        list = filtered;

        sortInPlace(list);
        adapter.submit(list);
    }

    private void sortInPlace(List<Recipe> list) {
        int pos = spSort.getSelectedItemPosition();
        Comparator<Recipe> cmp;

        switch (pos) {
            case 0: cmp = Comparator.comparing(a -> a.name.toLowerCase(Locale.ROOT)); break;
            case 1: cmp = Comparator.comparing((Recipe a) -> a.name.toLowerCase(Locale.ROOT)).reversed(); break;
            case 2: cmp = Comparator.comparingInt(a -> a.abv); break;
            case 3: cmp = Comparator.comparingInt((Recipe a) -> a.abv).reversed(); break;
            case 4: cmp = Comparator.comparingInt(a -> a.sweet); break;
            case 5: cmp = Comparator.comparingInt((Recipe a) -> a.sweet).reversed(); break;
            case 6: cmp = Comparator.comparingInt(a -> a.sour); break;
            case 7: cmp = Comparator.comparingInt((Recipe a) -> a.sour).reversed(); break;
            default: cmp = Comparator.comparing(a -> a.name.toLowerCase(Locale.ROOT));
        }
        list.sort(cmp);
    }

    private void openIngredientComboDialog() {
        List<String> owned = repo.getOwnedIngredientDisplayNames();
        if (owned.isEmpty()) {
            Toast.makeText(requireContext(), "보유 재료가 없습니다. 재료 탭에서 보유 체크를 해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] items = owned.toArray(new String[0]);
        boolean[] checked = new boolean[items.length];

        // 기존 선택값 반영
        for (int i = 0; i < items.length; i++) {
            if (selectedCombo != null && selectedCombo.contains(norm(items[i]))) checked[i] = true;
        }

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("보유 재료 선택 (필수재료 기준)")
                .setMultiChoiceItems(items, checked, (dialog, which, isChecked) -> checked[which] = isChecked)

                // 해제: 조합검색 끄고 기본 craftable(전체 보유 기반)로 복귀
                .setNeutralButton("해제", (dialog, which) -> {
                    selectedCombo = null;
                    btnIngredientCombo.setAlpha(1.0f);
                    reload();
                })

                // 적용: 선택 집합으로 craftable 계산
                .setPositiveButton("검색", (dialog, which) -> {
                    HashSet<String> sel = new HashSet<>();
                    for (int i = 0; i < items.length; i++) {
                        if (checked[i]) sel.add(norm(items[i]));
                    }

                    if (sel.isEmpty()) {
                        Toast.makeText(requireContext(), "재료를 1개 이상 선택하세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    selectedCombo = sel;
                    btnIngredientCombo.setAlpha(1.0f);
                    reload();
                })

                .setNegativeButton("취소", null)
                .show();
    }
}