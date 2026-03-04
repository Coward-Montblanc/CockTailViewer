package com.example.cocktailviewer.data;

import android.content.Context;
import android.net.Uri;

import com.example.cocktailviewer.model.Recipe;
import com.example.cocktailviewer.model.RecipeIngredient;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class YamlIO {

    // ===== Import =====
    // YAML 구조:
    // <칵테일명>:
    //   ingredients: [{name: "...", amount: "..."}, ...]
    //   optionalIngredients: [{name: "...", amount: "..."}, ...]
    //   recipe: "멀티라인 문자열"
    //   abv: 15
    //   sweet: 3
    //   sour: 7
    //

    public static int importFromUri(Context context, RecipeRepository repo, Uri uri) throws IOException {
        String yamlText = readAllText(context, uri);

        Yaml yaml = new Yaml();
        Object rootObj = yaml.load(yamlText);
        if (!(rootObj instanceof Map)) {
            return 0;
        }

        @SuppressWarnings("unchecked")
        Map<Object, Object> root = (Map<Object, Object>) rootObj;

        int imported = 0;

        for (Map.Entry<Object, Object> entry : root.entrySet()) {
            String cocktailName = entry.getKey() == null ? "" : entry.getKey().toString().trim();
            if (cocktailName.isEmpty()) continue;

            if (!(entry.getValue() instanceof Map)) continue;

            @SuppressWarnings("unchecked")
            Map<Object, Object> node = (Map<Object, Object>) entry.getValue();

            Recipe r = new Recipe();
            r.name = cocktailName;
            r.abv = asInt(node.get("abv"), 0);
            r.sweet = clamp(asInt(node.get("sweet"), 0), 0, 10);
            r.sour = clamp(asInt(node.get("sour"), 0), 0, 10);
            r.category = asString(node.get("category"));
            r.categoryDetail = asString(node.get("category_detail"));
            r.glass = asString(node.get("glass"));
            r.favorite = false;

            String instructions = readRecipeField(node.get("recipe"));
            r.instructions = instructions == null ? "" : instructions;

            long recipeId = repo.upsertRecipeByName(r);

            List<RecipeIngredient> all = new ArrayList<>();
            all.addAll(readIngredientsList(recipeId, node.get("ingredients"), false));
            all.addAll(readIngredientsList(recipeId, node.get("optionalIngredients"), true));

            repo.replaceRecipeIngredients(recipeId, all);

            imported++;
        }

        return imported;
    }

    public static ImportResult importFromTextSkipDuplicates(RecipeRepository repo, String yamlText) {
        String text = (yamlText == null) ? "" : yamlText.trim();
        if (text.isEmpty()) throw new IllegalArgumentException("YAML 입력이 비어있습니다.");

        Yaml yaml = new Yaml();
        Object rootObj = yaml.load(text);
        if (!(rootObj instanceof Map)) {
            throw new IllegalArgumentException("최상위 YAML 형식이 올바르지 않습니다.");
        }

        @SuppressWarnings("unchecked")
        Map<Object, Object> root = (Map<Object, Object>) rootObj;

        int ok = 0, dup = 0, fail = 0;

        for (Map.Entry<Object, Object> entry : root.entrySet()) {
            try {
                String cocktailName = entry.getKey() == null ? "" : entry.getKey().toString().trim();
                if (cocktailName.isEmpty()) { fail++; continue; }

                // ★ 중복 정책: 동일 이름이면 스킵
                if (repo.recipeNameExists(cocktailName)) {
                    dup++;
                    continue;
                }

                if (!(entry.getValue() instanceof Map)) { fail++; continue; }

                @SuppressWarnings("unchecked")
                Map<Object, Object> node = (Map<Object, Object>) entry.getValue();

                Recipe r = new Recipe();
                r.name = cocktailName;
                r.abv = asInt(node.get("abv"), 0);
                r.sweet = clamp(asInt(node.get("sweet"), 0), 0, 10);
                r.sour = clamp(asInt(node.get("sour"), 0), 0, 10);
                r.category = asString(node.get("category"));
                r.categoryDetail = asString(node.get("category_detail"));
                r.glass = asString(node.get("glass"));
                r.favorite = false;

                String instructions = readRecipeField(node.get("recipe"));
                r.instructions = instructions == null ? "" : instructions;

                long recipeId = repo.upsertRecipeByName(r);

                List<RecipeIngredient> all = new ArrayList<>();
                all.addAll(readIngredientsList(recipeId, node.get("ingredients"), false));
                all.addAll(readIngredientsList(recipeId, node.get("optionalIngredients"), true));
                repo.replaceRecipeIngredients(recipeId, all);

                ok++;
            } catch (Exception e) {
                fail++;
            }
        }

        return new ImportResult(ok, dup, fail);
    }

    private static List<RecipeIngredient> readIngredientsList(long recipeId, Object obj, boolean optional) {
        List<RecipeIngredient> out = new ArrayList<>();
        if (!(obj instanceof List)) return out;

        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) obj;

        for (Object item : list) {
            if (!(item instanceof Map)) continue;

            @SuppressWarnings("unchecked")
            Map<Object, Object> m = (Map<Object, Object>) item;

            String name = asString(m.get("name"));
            String amount = asString(m.get("amount"));

            if (name.trim().isEmpty()) continue;

            RecipeIngredient ri = new RecipeIngredient();
            ri.recipeId = recipeId;
            ri.ingredientName = name.trim();
            ri.amount = amount;
            ri.optional = optional;
            out.add(ri);
        }
        return out;
    }

    private static String readRecipeField(Object obj) {
        if (obj == null) return "";
        if (obj instanceof String) return (String) obj;

        // (추측 대응) recipe: ["줄1", "줄2"] 같은 형태도 허용
        if (obj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> lines = (List<Object>) obj;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lines.size(); i++) {
                if (i > 0) sb.append("\n");
                sb.append(lines.get(i) == null ? "" : lines.get(i).toString());
            }
            return sb.toString();
        }
        return obj.toString();
    }

    public static class ImportResult {
        public int success;
        public int duplicate;
        public int failed;

        public ImportResult(int success, int duplicate, int failed) {
            this.success = success;
            this.duplicate = duplicate;
            this.failed = failed;
        }
    }

    // ===== Export =====
    public static void exportToUri(Context context, RecipeRepository repo, Uri uri) throws IOException {
        List<Recipe> recipes = repo.getAllRecipes();

        // LinkedHashMap으로 순서 유지
        Map<String, Object> root = new LinkedHashMap<>();

        for (Recipe r : recipes) {
            Map<String, Object> node = new LinkedHashMap<>();

            // ingredients / optionalIngredients 만들기
            List<RecipeIngredient> ing = repo.getRecipeIngredients(r.id);

            List<Map<String, Object>> required = new ArrayList<>();
            List<Map<String, Object>> optional = new ArrayList<>();

            for (RecipeIngredient ri : ing) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", ri.ingredientName);
                m.put("amount", ri.amount == null ? "" : ri.amount);
                if (ri.optional) optional.add(m);
                else required.add(m);
            }

            node.put("ingredients", required);
            node.put("optionalIngredients", optional);

            // recipe 멀티라인을 LITERAL로 덤프하면 보기 좋음
            node.put("recipe", r.instructions == null ? "" : r.instructions);

            node.put("abv", r.abv);
            node.put("sweet", r.sweet);
            node.put("sour", r.sour);

            root.put(r.name, node);
        }

        DumperOptions opt = new DumperOptions();
        opt.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        opt.setPrettyFlow(true);
        opt.setIndent(2);
        opt.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);

        // 멀티라인 문자열을 | 형태로 덤프하고 싶으면 LITERAL 강제는 값 타입별로 제어가 까다로운데,
        // SnakeYAML 기본도 줄바꿈이 있으면 종종 block으로 떨어짐. (환경에 따라 다름)
        Yaml yaml = new Yaml(opt);

        String output = yaml.dump(root);
        writeAllText(context, uri, output);
    }

    // ===== IO utils =====
    private static String readAllText(Context context, Uri uri) throws IOException {
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) throw new FileNotFoundException("InputStream is null for uri=" + uri);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) >= 0) bos.write(buf, 0, n);
            try {
                return bos.toString("UTF-8");
            } catch (UnsupportedEncodingException e) {
                return bos.toString(); // fallback
            }

        }
    }

    private static void writeAllText(Context context, Uri uri, String text) throws IOException {
        try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
            if (os == null) throw new FileNotFoundException("OutputStream is null for uri=" + uri);
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            os.write(bytes);
            os.flush();
        }
    }

    private static String asString(Object o) {
        return o == null ? "" : o.toString();
    }

    private static int asInt(Object o, int def) {
        try {
            if (o == null) return def;
            if (o instanceof Number) return ((Number) o).intValue();
            String s = o.toString().trim();
            if (s.isEmpty()) return def;
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}