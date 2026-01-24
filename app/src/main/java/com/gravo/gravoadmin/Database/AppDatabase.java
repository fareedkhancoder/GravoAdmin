package com.gravo.gravoadmin.Database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.gravo.gravoadmin.Converters;
import com.gravo.gravoadmin.DraftDao;
import com.gravo.gravoadmin.Product;
@Database(entities = {Product.class}, version = 1)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract DraftDao draftDao();

    private static AppDatabase INSTANCE;

    public static AppDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "product_draft_db")
                    .allowMainThreadQueries() // Simplification for small apps; usually use Async
                    .build();
        }
        return INSTANCE;
    }
}