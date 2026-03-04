package com.example.cocktailviewer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.cocktailviewer.data.RecipeRepository;
import com.example.cocktailviewer.ui.HomeFragment;
import com.example.cocktailviewer.ui.IngredientFragment;
import com.example.cocktailviewer.ui.RecipeEditorFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;
import com.example.cocktailviewer.data.YamlIO;
import com.example.cocktailviewer.ui.AllRecipesFragment;
import com.example.cocktailviewer.ui.YamlTextImportFragment;
import com.example.cocktailviewer.ui.IngredientSuggestFragment;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private FloatingActionButton fabMenu;
    private RecipeRepository repo;

    private ActivityResultLauncher<String[]> importLauncher;
    private ActivityResultLauncher<String> exportLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(
                android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                        | android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
        );
        setContentView(R.layout.activity_main);
        FrameLayout drawerContainer = findViewById(R.id.drawerContainer);
        int screenW = getResources().getDisplayMetrics().widthPixels;
        int drawerW = (int) (screenW * 0.80f);

        DrawerLayout.LayoutParams lp = (DrawerLayout.LayoutParams) drawerContainer.getLayoutParams();
        lp.width = drawerW;
        drawerContainer.setLayoutParams(lp);

        repo = new RecipeRepository(this);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        ImageButton btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        setupImportExportLaunchers();
        setupDrawerMenu();

        if (savedInstanceState == null) {
            showHome();
        }
    }

    private void setupImportExportLaunchers() {
        importLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri == null) return;
                    try {
                        int n = YamlIO.importFromUri(this, repo, uri);
                        Toast.makeText(this, "Import 완료: " + n + "개", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Import 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
        );

        exportLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/yaml"),
                uri -> {
                    if (uri == null) return;
                    try {
                        YamlIO.exportToUri(this, repo, uri);
                        Toast.makeText(this, "Export 완료", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Export 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void setupDrawerMenu() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            drawerLayout.closeDrawers();

            if (id == R.id.menu_home) {
                showHome();
                return true;
            }
            if (id == R.id.menu_all_recipes) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, AllRecipesFragment.newInstance())
                        .addToBackStack(null)
                        .commit();
                return true;
            }
            if (id == R.id.menu_import_yaml) {
                importLauncher.launch(new String[]{"application/yaml", "text/yaml", "text/*"});
                return true;
            }
            if (id == R.id.menu_add_recipe) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, RecipeEditorFragment.newInstance())
                        .addToBackStack(null)
                        .commit();
                return true;
            }
            if (id == R.id.menu_export_yaml) {
                exportLauncher.launch("recipes.yaml");
                return true;
            }
            if (id == R.id.menu_ingredients) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, IngredientFragment.newInstance())
                        .addToBackStack(null)
                        .commit();
                return true;
            }
            if (id == R.id.menu_import_yaml_text) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, YamlTextImportFragment.newInstance())
                        .addToBackStack(null)
                        .commit();
                return true;
            }
            if (id == R.id.menu_ingredient_suggest) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, IngredientSuggestFragment.newInstance())
                        .addToBackStack(null)
                        .commit();
                return true;
            }
            return false;
        });
    }

    private void showHome() {
        getSupportFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, HomeFragment.newInstance())
                .commit();
    }

    private void notifyHomeRefresh() {
        // 간단 방식: HomeFragment가 onResume에서 다시 로딩하도록 만들기
        // 또는 FragmentResult API 사용
    }

    public RecipeRepository repo() {
        return repo;
    }
}