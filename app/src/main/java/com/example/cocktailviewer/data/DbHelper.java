package com.example.cocktailviewer.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "cocktail.db";
    public static final int DB_VERSION = 1;

    public DbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("""
            CREATE TABLE ingredients (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              name TEXT UNIQUE NOT NULL,
              has INTEGER NOT NULL DEFAULT 0
            )
        """);

        db.execSQL("""
            CREATE TABLE recipes (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              name TEXT UNIQUE NOT NULL,
              instructions TEXT NOT NULL,
              abv INTEGER NOT NULL,
              sweet INTEGER NOT NULL,
              sour INTEGER NOT NULL,
              favorite INTEGER NOT NULL DEFAULT 0
            )
        """);

        db.execSQL("""
            CREATE TABLE recipe_ingredients (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              recipe_id INTEGER NOT NULL,
              ingredient_name TEXT NOT NULL,
              amount TEXT NOT NULL,
              optional INTEGER NOT NULL DEFAULT 0,
              FOREIGN KEY(recipe_id) REFERENCES recipes(id) ON DELETE CASCADE
            )
        """);
        db.execSQL("CREATE INDEX idx_ri_recipe ON recipe_ingredients(recipe_id)");
        db.execSQL("CREATE INDEX idx_ri_name ON recipe_ingredients(ingredient_name)");

        db.execSQL("""
            CREATE TABLE recent_views (
              recipe_id INTEGER PRIMARY KEY,
              viewed_at INTEGER NOT NULL
            )
        """);
        db.execSQL("CREATE INDEX idx_recent_viewed_at ON recent_views(viewed_at)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // v1 고정. 버전 올리면 여기서 마이그레이션.
        db.execSQL("DROP TABLE IF EXISTS recent_views");
        db.execSQL("DROP TABLE IF EXISTS recipe_ingredients");
        db.execSQL("DROP TABLE IF EXISTS recipes");
        db.execSQL("DROP TABLE IF EXISTS ingredients");
        onCreate(db);
    }
}