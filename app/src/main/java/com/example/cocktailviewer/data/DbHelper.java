package com.example.cocktailviewer.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "cocktail.db";
    public static final int DB_VERSION = 2;

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

              category TEXT NOT NULL DEFAULT '',
              category_detail TEXT NOT NULL DEFAULT '',
              glass TEXT NOT NULL DEFAULT '',

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
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE recipes ADD COLUMN category TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE recipes ADD COLUMN category_detail TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE recipes ADD COLUMN glass TEXT NOT NULL DEFAULT ''");
        }
    }
}