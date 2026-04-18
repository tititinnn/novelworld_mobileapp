package com.example.btl_novelworld;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btl_novelworld.adapters.ChapterAdapter;
import com.example.btl_novelworld.models.Book;
import com.example.btl_novelworld.models.Chapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ChapterListActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView txtBookTitle;
    private RecyclerView rvAllChapters;

    private FirebaseFirestore db;
    private ChapterAdapter chapterAdapter;
    private String bookId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter_list);

        db = FirebaseFirestore.getInstance();
        bookId = getIntent().getStringExtra("bookId");

        if (bookId == null || bookId.isEmpty()) {
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        setupActions();
        loadBookInfo();
        loadAllChapters();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        txtBookTitle = findViewById(R.id.txtBookTitle);
        rvAllChapters = findViewById(R.id.rvAllChapters);
    }

    private void setupRecyclerView() {
        chapterAdapter = new ChapterAdapter(this, bookId);
        rvAllChapters.setLayoutManager(new LinearLayoutManager(this));
        rvAllChapters.setAdapter(chapterAdapter);
    }

    private void setupActions() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadBookInfo() {
        db.collection("Books")
                .document(bookId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    Book book = snapshot.toObject(Book.class);
                    if (book != null) {
                        txtBookTitle.setText(book.getTitle());
                    }
                });
    }

    private void loadAllChapters() {
        db.collection("Books")
                .document(bookId)
                .collection("Chapters")
                .orderBy("chapterNumber", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Chapter> list = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Chapter chapter = doc.toObject(Chapter.class);
                        if (chapter != null) {
                            chapter.setChapterId(doc.getId());
                            list.add(chapter);
                        }
                    }
                    chapterAdapter.submitList(list);
                });
    }

}