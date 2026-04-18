package com.example.btl_novelworld.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.btl_novelworld.models.LocalBook;
import com.example.btl_novelworld.models.LocalChapter;

import java.util.List;

@Dao
public interface OfflineDao {

    // --- Lưu và lấy danh sách truyện ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertBook(LocalBook book);

    @Query("SELECT * FROM offline_books")
    List<LocalBook> getAllOfflineBooks();

    @Query("SELECT * FROM offline_books WHERE bookId = :bookId LIMIT 1")
    LocalBook getOfflineBookById(String bookId);

    // --- Lưu và lấy các chương truyện ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertChapters(List<LocalChapter> chapters);

    @Query("SELECT * FROM offline_chapters WHERE bookId = :bookId ORDER BY chapterNumber ASC")
    List<LocalChapter> getChaptersByBookId(String bookId);

    @Query("SELECT * FROM offline_books WHERE bookId = :id LIMIT 1") LocalBook getBookById(String id);

    @Query("SELECT * FROM offline_chapters WHERE chapterId = :chapterId LIMIT 1")
    LocalChapter getChapterById(String chapterId);

    @Query("SELECT * FROM offline_chapters WHERE bookId = :bookId AND chapterNumber > :currentNumber ORDER BY chapterNumber ASC LIMIT 1")
    LocalChapter getNextChapter(String bookId, long currentNumber);

    @Query("SELECT * FROM offline_chapters WHERE bookId = :bookId AND chapterNumber < :currentNumber ORDER BY chapterNumber DESC LIMIT 1")
    LocalChapter getPreviousChapter(String bookId, long currentNumber);
}