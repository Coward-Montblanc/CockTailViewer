package com.example.cocktailviewer.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.cocktailviewer.model.Ingredient;
import com.example.cocktailviewer.model.Recipe;
import com.example.cocktailviewer.model.RecipeIngredient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class RecipeRepository {

    private final DbHelper dbHelper;

    public RecipeRepository(Context context) {
        this.dbHelper = new DbHelper(context);
    }

    private int clamp0to10(int v) {
        return Math.max(0, Math.min(10, v));
    }

    // ---------- Ingredients ----------
    public long addIngredientIfNotExists(String name) {
        String n = safeTrim(name);
        if (n.isEmpty()) return -1;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", n);
        cv.put("has", 0);
        return db.insertWithOnConflict("ingredients", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public List<Ingredient> getAllIngredients() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, name, has FROM ingredients ORDER BY name COLLATE NOCASE ASC", null);

        List<Ingredient> out = new ArrayList<>();
        try {
            while (c.moveToNext()) {
                Ingredient i = new Ingredient();
                i.id = c.getLong(0);
                i.name = c.getString(1);
                i.has = c.getInt(2) == 1;
                out.add(i);
            }
        } finally {
            c.close();
        }
        return out;
    }
    public List<String> searchIngredientNames(String prefix, int limit) {
        String p = safeTrim(prefix);
        if (p.isEmpty()) return new ArrayList<>();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT name FROM ingredients " +
                "WHERE name LIKE ? " +
                "ORDER BY name COLLATE NOCASE ASC " +
                "LIMIT ?",
                new String[]{ p + "%", String.valueOf(limit) }
        );

        List<String> out = new ArrayList<>();
        try {
            while (c.moveToNext()) {
                out.add(c.getString(0));
            }
        } finally {
            c.close();
        }
        return out;
    }

    public void setIngredientHas(long id, boolean has) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("has", has ? 1 : 0);
        db.update("ingredients", cv, "id=?", new String[]{String.valueOf(id)});
    }

    public void deleteIngredient(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("ingredients", "id=?", new String[]{String.valueOf(id)});
    }

    public HashSet<String> getOwnedIngredientNames() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT name FROM ingredients WHERE has=1", null);

        HashSet<String> out = new HashSet<>();
        try {
            while (c.moveToNext()) {
                out.add(norm(c.getString(0)));
            }
        } finally {
            c.close();
        }
        return out;
    }

    public List<String> getOwnedIngredientDisplayNames() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT name FROM ingredients WHERE has=1 ORDER BY name COLLATE NOCASE ASC", null);

        List<String> out = new ArrayList<>();
        try {
            while (c.moveToNext()) out.add(c.getString(0));
        } finally {
            c.close();
        }
        return out;
    }

    // ---------- Recipes ----------
    public long upsertRecipeByName(Recipe recipe) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Long existingId = findRecipeIdByName(recipe.name);
        if (existingId != null) {
            ContentValues cv = new ContentValues();
            cv.put("instructions", recipe.instructions);
            cv.put("abv", recipe.abv);
            cv.put("sweet", recipe.sweet);
            cv.put("sour", recipe.sour);
            cv.put("favorite", recipe.favorite ? 1 : 0);
            cv.put("category", recipe.category == null ? "" : recipe.category);
            cv.put("category_detail", recipe.categoryDetail == null ? "" : recipe.categoryDetail);
            cv.put("glass", recipe.glass == null ? "" : recipe.glass);
            cv.put("rating", clamp0to10(recipe.rating));
            db.update("recipes", cv, "id=?", new String[]{String.valueOf(existingId)});
            return existingId;
        } else {
            ContentValues cv = new ContentValues();
            cv.put("name", recipe.name);
            cv.put("instructions", recipe.instructions);
            cv.put("abv", recipe.abv);
            cv.put("sweet", recipe.sweet);
            cv.put("sour", recipe.sour);
            cv.put("favorite", recipe.favorite ? 1 : 0);
            cv.put("category", recipe.category == null ? "" : recipe.category);
            cv.put("category_detail", recipe.categoryDetail == null ? "" : recipe.categoryDetail);
            cv.put("glass", recipe.glass == null ? "" : recipe.glass);
            cv.put("rating", clamp0to10(recipe.rating));
            return db.insert("recipes", null, cv);
        }
    }

    private Long findRecipeIdByName(String name) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id FROM recipes WHERE name=? LIMIT 1", new String[]{name});
        try {
            if (c.moveToFirst()) return c.getLong(0);
            return null;
        } finally {
            c.close();
        }
    }

    public void setFavorite(long recipeId, boolean favorite) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("favorite", favorite ? 1 : 0);
        db.update("recipes", cv, "id=?", new String[]{String.valueOf(recipeId)});
    }

    public Recipe getRecipe(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
            "SELECT id, name, category, category_detail, glass, instructions, abv, sweet, sour, favorite, rating " +
            "FROM recipes WHERE id=? LIMIT 1",
            new String[]{String.valueOf(id)}
        );
        try {
            if (!c.moveToFirst()) return null;
            Recipe r = new Recipe();
            r.id = c.getLong(0);
            r.name = c.getString(1);
            r.category = c.getString(2);
            r.categoryDetail = c.getString(3);
            r.glass = c.getString(4);
            r.instructions = c.getString(5);
            r.abv = c.getInt(6);
            r.sweet = c.getInt(7);
            r.sour = c.getInt(8);
            r.favorite = c.getInt(9) == 1;
            r.rating = c.getInt(10);
            return r;
        } finally {
            c.close();
        }
    }

    public List<RecipeIngredient> getRecipeIngredients(long recipeId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("""
            SELECT id, recipe_id, ingredient_name, amount, optional
            FROM recipe_ingredients
            WHERE recipe_id=?
            ORDER BY optional ASC, id ASC
        """, new String[]{String.valueOf(recipeId)});

        List<RecipeIngredient> out = new ArrayList<>();
        try {
            while (c.moveToNext()) {
                RecipeIngredient ri = new RecipeIngredient();
                ri.id = c.getLong(0);
                ri.recipeId = c.getLong(1);
                ri.ingredientName = c.getString(2);
                ri.amount = c.getString(3);
                ri.optional = c.getInt(4) == 1;
                out.add(ri);
            }
        } finally {
            c.close();
        }
        return out;
    }

    public void replaceRecipeIngredients(long recipeId, List<RecipeIngredient> ingredients) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("recipe_ingredients", "recipe_id=?", new String[]{String.valueOf(recipeId)});

        for (RecipeIngredient ri : ingredients) {
            ContentValues cv = new ContentValues();
            cv.put("recipe_id", recipeId);
            cv.put("ingredient_name", safeTrim(ri.ingredientName));
            cv.put("amount", ri.amount == null ? "" : ri.amount);
            cv.put("optional", ri.optional ? 1 : 0);
            db.insert("recipe_ingredients", null, cv);
            addIngredientIfNotExists(ri.ingredientName);
        }
    }

    public void deleteRecipe(long recipeId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("recipe_ingredients", "recipe_id=?", new String[]{String.valueOf(recipeId)});
        db.delete("recent_views", "recipe_id=?", new String[]{String.valueOf(recipeId)});
        db.delete("recipes", "id=?", new String[]{String.valueOf(recipeId)});
    }

    public void updateRecipeById(Recipe recipe) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put("name", recipe.name);
        cv.put("category", recipe.category == null ? "" : recipe.category);
        cv.put("category_detail", recipe.categoryDetail == null ? "" : recipe.categoryDetail);
        cv.put("glass", recipe.glass == null ? "" : recipe.glass);

        cv.put("instructions", recipe.instructions);
        cv.put("abv", recipe.abv);
        cv.put("sweet", recipe.sweet);
        cv.put("sour", recipe.sour);
        cv.put("favorite", recipe.favorite ? 1 : 0);

        db.update("recipes", cv, "id=?", new String[]{String.valueOf(recipe.id)});
    }

    // ---------- Recent ----------
    public void markViewed(long recipeId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("recipe_id", recipeId);
        cv.put("viewed_at", System.currentTimeMillis());
        db.insertWithOnConflict("recent_views", null, cv, SQLiteDatabase.CONFLICT_REPLACE);

        // trim 10
        db.execSQL("""
          DELETE FROM recent_views
          WHERE recipe_id NOT IN (
            SELECT recipe_id FROM recent_views ORDER BY viewed_at DESC LIMIT 10
          )
        """);
    }

    public List<Long> getRecentIds(int limit) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT recipe_id FROM recent_views ORDER BY viewed_at DESC LIMIT " + limit, null);

        List<Long> out = new ArrayList<>();
        try {
            while (c.moveToNext()) out.add(c.getLong(0));
        } finally {
            c.close();
        }
        return out;
    }

    // ---------- Home list building ----------
    public List<Recipe> getCraftableRecipes() {
        // 1) owned 재료
        HashSet<String> owned = getOwnedIngredientNames();

        // 2) recipeId -> required ingredient names
        HashMap<Long, List<String>> requiredMap = new HashMap<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("""
            SELECT recipe_id, ingredient_name
            FROM recipe_ingredients
            WHERE optional=0
        """, null);

        try {
            while (c.moveToNext()) {
                long rid = c.getLong(0);
                String nm = norm(c.getString(1));
                requiredMap.computeIfAbsent(rid, k -> new ArrayList<>()).add(nm);
            }
        } finally {
            c.close();
        }

        // 3) 모든 recipes에서 craftable만 필터
        Cursor rc = db.rawQuery("SELECT id, name, category, category_detail, glass, instructions, abv, sweet, sour, favorite, rating FROM recipes", null);
        List<Recipe> out = new ArrayList<>();
        try {
            while (rc.moveToNext()) {
                long rid = rc.getLong(0);

                List<String> req = requiredMap.get(rid);
                if (req == null) {
                    req = new ArrayList<>();
                }

                boolean ok = true;
                for (String rname : req) {
                    if (!owned.contains(rname)) { ok = false; break; }
                }
                if (!ok) continue;

                Recipe r = new Recipe();
                r.id = rid;
                r.name = rc.getString(1);
                r.category = rc.getString(2);
                r.categoryDetail = rc.getString(3);
                r.glass = rc.getString(4);
                r.instructions = rc.getString(5);
                r.abv = rc.getInt(6);
                r.sweet = rc.getInt(7);
                r.sour = rc.getInt(8);
                r.favorite = rc.getInt(9) == 1;
                r.rating = rc.getInt(10);
                out.add(r);
            }
        } finally {
            rc.close();
        }
        return out;
    }

    public List<Recipe> getCraftableRecipesBySelectedNames(HashSet<String> selectedNormalizedNames) {
        HashSet<String> selected = (selectedNormalizedNames == null) ? new HashSet<>() : selectedNormalizedNames;

        // recipeId -> required ingredient names
        HashMap<Long, List<String>> requiredMap = new HashMap<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("""
            SELECT recipe_id, ingredient_name
            FROM recipe_ingredients
            WHERE optional=0
        """, null);

        try {
            while (c.moveToNext()) {
                long rid = c.getLong(0);
                String nm = norm(c.getString(1));
                requiredMap.computeIfAbsent(rid, k -> new ArrayList<>()).add(nm);
            }
        } finally {
            c.close();
        }

        // 모든 recipes에서 "필수재료 ⊆ 선택집합"만 필터
        Cursor rc = db.rawQuery("SELECT id, name, instructions, abv, sweet, sour, favorite FROM recipes", null);
        List<Recipe> out = new ArrayList<>();
        try {
            while (rc.moveToNext()) {
                long rid = rc.getLong(0);

                List<String> req = requiredMap.get(rid);
                if (req == null) req = new ArrayList<>();

                boolean ok = true;
                for (String rname : req) {
                    if (!selected.contains(rname)) { ok = false; break; }
                }
                if (!ok) continue;

                Recipe r = new Recipe();
                r.id = rid;
                r.name = rc.getString(1);
                r.instructions = rc.getString(2);
                r.abv = rc.getInt(3);
                r.sweet = rc.getInt(4);
                r.sour = rc.getInt(5);
                r.favorite = rc.getInt(6) == 1;
                out.add(r);
            }
        } finally {
            rc.close();
        }

        return out;
    }

    public HashSet<Long> searchRecipeIds(String query) {
        String q = norm(query);
        HashSet<Long> out = new HashSet<>();
        if (q.isEmpty()) return out;

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String like = "%" + q + "%";

        // 1) 이름 + 카테고리(category/category_detail)
        Cursor c1 = db.rawQuery(
                "SELECT id FROM recipes " +
                "WHERE LOWER(name) LIKE ? " +
                "   OR LOWER(category) LIKE ? " +
                "   OR LOWER(category_detail) LIKE ?",
                new String[]{ like, like, like }
        );

        try {
            while (c1.moveToNext()) out.add(c1.getLong(0));
        } finally {
            c1.close();
        }

        // 2) 재료(recipe_ingredients.ingredient_name)
        Cursor c2 = db.rawQuery(
                "SELECT DISTINCT recipe_id FROM recipe_ingredients " +
                "WHERE LOWER(ingredient_name) LIKE ?",
                new String[]{ like }
        );

        try {
            while (c2.moveToNext()) out.add(c2.getLong(0));
        } finally {
            c2.close();
        }

        return out;
    }

    // ---------- utils ----------
    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }
    private static String norm(String s) {
        if (s == null) return "";
        // 일반 공백 + 특수공백(NBSP)까지 정리
        String t = s.replace('\u00A0', ' ').trim();
        // 중간에 공백이 여러개면 1개로 (선택이지만 추천)
        t = t.replaceAll("\\s+", " ");
        return t.toLowerCase(java.util.Locale.ROOT);
    }
    public List<Recipe> getAllRecipes() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, name, category, category_detail, glass, instructions, abv, sweet, sour, favorite, rating FROM recipes ORDER BY name COLLATE NOCASE ASC", null);

        List<Recipe> out = new ArrayList<>();
        try {
            while (c.moveToNext()) {
                Recipe r = new Recipe();
                r.id = c.getLong(0);
                r.name = c.getString(1);
                r.category = c.getString(2);
                r.categoryDetail = c.getString(3);
                r.glass = c.getString(4);
                r.instructions = c.getString(5);
                r.abv = c.getInt(6);
                r.sweet = c.getInt(7);
                r.sour = c.getInt(8);
                r.favorite = c.getInt(9) == 1;
                r.rating = c.getInt(10);
                out.add(r);
            }
        } finally {
            c.close();
        }
        return out;
    }
    public static class IngredientLine {
        public String name;
        public String amount;
        public boolean optional;
    }

    public List<IngredientLine> getIngredientsOfRecipe(long recipeId) {

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor c = db.rawQuery("""
        SELECT ingredient_name, amount, optional
        FROM recipe_ingredients
        WHERE recipe_id=?
        ORDER BY optional ASC
    """, new String[]{String.valueOf(recipeId)});

        List<IngredientLine> out = new ArrayList<>();

        try {
            while (c.moveToNext()) {
                IngredientLine line = new IngredientLine();
                line.name = c.getString(0);
                line.amount = c.getString(1);
                line.optional = c.getInt(2) == 1;
                out.add(line);
            }
        } finally {
            c.close();
        }

        return out;
    }

    public boolean existsRecipeNameExceptId(String name, long exceptId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT id FROM recipes WHERE name=? AND id<>? LIMIT 1",
                new String[]{name, String.valueOf(exceptId)}
        );
        try {
            return c.moveToFirst();
        } finally {
            c.close();
        }
    }

    public boolean existsRecipeName(String name) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id FROM recipes WHERE name=? LIMIT 1", new String[]{name});
        try {
            return c.moveToFirst();
        } finally {
            c.close();
        }
    }

    public void setRating(long recipeId, int rating) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("rating", clamp0to10(rating));
        db.update("recipes", cv, "id=?", new String[]{ String.valueOf(recipeId) });
    }

    public Long getRandomRecipeId() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id FROM recipes ORDER BY RANDOM() LIMIT 1", null);

        try {
            if(c.moveToFirst()) return c.getLong(0);
            return null;
        } finally {
            c.close();
        }
    }

    public boolean recipeNameExists(String name) {
        return findRecipeIdByName(name) != null;
    }

    public static class IngredientSuggest {
        public String name;
        public int count;
        public ArrayList<String> samples = new ArrayList<>();

        public IngredientSuggest(String name) {
            this.name = name;
        }
    }

    public void setIngredientHas(String name, boolean has) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("has", has ? 1 : 0);
        db.update("ingredients", cv, "name=?", new String[]{ name });
    }

    public List<IngredientSuggest> getNextBuySuggestions(int samplePerIngredient) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // 1) 보유 재료 집합
        HashSet<String> owned = new HashSet<>();
        Cursor oc = db.rawQuery("SELECT name FROM ingredients WHERE has=1", null);
        try {
            while (oc.moveToNext()) owned.add(norm(oc.getString(0)));
        } finally {
            oc.close();
        }

        // 2) recipe_id -> recipe_name
        HashMap<Long, String> recipeName = new HashMap<>();
        Cursor rc = db.rawQuery("SELECT id, name FROM recipes", null);
        try {
            while (rc.moveToNext()) recipeName.put(rc.getLong(0), rc.getString(1));
        } finally {
            rc.close();
        }

        // 3) recipe_id -> required ingredient list (optional=0)
        HashMap<Long, ArrayList<String>> required = new HashMap<>();
        Cursor ic = db.rawQuery(
                "SELECT recipe_id, ingredient_name FROM recipe_ingredients WHERE optional=0",
                null
        );
        try {
            while (ic.moveToNext()) {
                long rid = ic.getLong(0);
                String ing = norm(ic.getString(1));
                required.computeIfAbsent(rid, k -> new ArrayList<>()).add(ing);
            }
        } finally {
            ic.close();
        }

        // 4) 부족 필수 재료가 '딱 1개'인 레시피만 카운트
        //    missing == 1이면 그 missing ingredient를 +1, 샘플 레시피명 추가
        HashMap<String, IngredientSuggest> map = new HashMap<>();

        for (Map.Entry<Long, String> e : recipeName.entrySet()) {
            long rid = e.getKey();
            String rname = e.getValue();

            ArrayList<String> req = required.get(rid);
            if (req == null || req.isEmpty()) continue; // 필수재료가 없다면 추천 계산에서 제외

            ArrayList<String> missing = new ArrayList<>();
            for (String ing : req) {
                if (!owned.contains(ing)) missing.add(ing);
                if (missing.size() > 1) break; // 2개 이상 부족하면 조기 종료
            }

            if (missing.size() == 1) {
                String miss = missing.get(0);
                IngredientSuggest s = map.computeIfAbsent(miss, IngredientSuggest::new);
                s.count++;

                if (s.samples.size() < samplePerIngredient) {
                    s.samples.add(rname);
                }
            }
        }

        // 5) 정렬: count desc, name asc
        ArrayList<IngredientSuggest> out = new ArrayList<>(map.values());
        out.sort((a, b) -> {
            if (b.count != a.count) return Integer.compare(b.count, a.count);
            return a.name.compareToIgnoreCase(b.name);
        });

        // 6) 표시용 이름을 “원래 ingredients.name”으로 맞추고 싶다면(선택)
        // 지금은 norm된 소문자 기반이므로, 실제 표기명 유지가 필요하면 아래처럼 매핑을 만들면 됨.
        // (우선은 간단히 norm 그대로 출력해도 동작은 함)

        return out;
    }
}