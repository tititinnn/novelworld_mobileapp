package com.example.btl_novelworld.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.btl_novelworld.models.LocalBook;
import com.example.btl_novelworld.models.LocalChapter;

@Database(entities = {LocalBook.class, LocalChapter.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract OfflineDao offlineDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "novel_world_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}