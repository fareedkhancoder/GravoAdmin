package com.gravo.gravoadmin;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DraftDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertDraft(Product product);

    @Query("SELECT * FROM drafts WHERE draftId = :id LIMIT 1")
    Product getDraftById(int id);

    @Delete
    void deleteDraft(Product product);

    @Query("SELECT * FROM drafts ORDER BY draftId DESC")
    List<Product> getAllDrafts();

    // Optional: Get count
    @Query("SELECT COUNT(*) FROM drafts")
    int getCount();
}