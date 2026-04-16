package com.example.btl_novelworld;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.btl_novelworld.models.Book;
import com.example.btl_novelworld.models.Chapter;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import android.widget.FrameLayout;

public class ReaderActivity extends AppCompatActivity {

    private TextView txtBookTitle, txtCurrentChapter, tvChapterContent, btnPrevChapter, btnNextChapter;
    private ImageView btnBack, btnTableOfContents;
    private FrameLayout btnScrollTop;
    private ScrollView scrollViewContent;

    private FirebaseFirestore db;
    private String bookId;
    private String chapterId;
    private Chapter currentChapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter_detail);

        db = FirebaseFirestore.getInstance();
        bookId = getIntent().getStringExtra("bookId");
        chapterId = getIntent().getStringExtra("chapterId");

        if (bookId == null || chapterId == null) {
            finish();
            return;
        }

        initViews();
        setupActions();
        loadBookInfo();
        loadCurrentChapter();
    }

    private void initViews() {
        txtBookTitle = findViewById(R.id.txt_book_title);
        txtCurrentChapter = findViewById(R.id.txt_current_chapter);
        tvChapterContent = findViewById(R.id.tvChapterContent);
        btnPrevChapter = findViewById(R.id.btnPrevChapter);
        btnNextChapter = findViewById(R.id.btnNextChapter);
        btnBack = findViewById(R.id.btnBack);
        btnTableOfContents = findViewById(R.id.btnTableOfContents);
        btnScrollTop = findViewById(R.id.btnScrollTop);
        scrollViewContent = findViewById(R.id.scrollViewContent);
    }

    private void setupActions() {
        btnBack.setOnClickListener(v -> finish());

        btnPrevChapter.setOnClickListener(v -> loadAdjacentChapter(false));
        btnNextChapter.setOnClickListener(v -> loadAdjacentChapter(true));

        btnTableOfContents.setOnClickListener(v -> openChapterList());

        btnScrollTop.setOnClickListener(v -> scrollToTop());
    }

    private void openChapterList() {
        Intent intent = new Intent(this, ChapterListActivity.class);
        intent.putExtra("bookId", bookId);
        startActivity(intent);
    }

    private void scrollToTop() {
        if (scrollViewContent != null) {
            scrollViewContent.post(() -> scrollViewContent.smoothScrollTo(0, 0));
        }
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

    private void loadCurrentChapter() {
        db.collection("Books")
                .document(bookId)
                .collection("Chapters")
                .document(chapterId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    Chapter chapter = snapshot.toObject(Chapter.class);
                    if (chapter == null) return;

                    chapter.setChapterId(snapshot.getId());
                    currentChapter = chapter;

                    txtCurrentChapter.setText("Chương " + chapter.getChapterNumber());
                    tvChapterContent.setText(chapter.getContent());

                    saveReadingHistory(chapter.getChapterId());
                    scrollToTop();
                });
    }

    private void loadAdjacentChapter(boolean next) {
        if (currentChapter == null) return;

        long currentNumber = currentChapter.getChapterNumber();

        Query query;
        if (next) {
            query = db.collection("Books")
                    .document(bookId)
                    .collection("Chapters")
                    .orderBy("chapterNumber", Query.Direction.ASCENDING)
                    .startAfter(currentNumber)
                    .limit(1);
        } else {
            query = db.collection("Books")
                    .document(bookId)
                    .collection("Chapters")
                    .orderBy("chapterNumber", Query.Direction.DESCENDING)
                    .startAfter(currentNumber)
                    .limit(1);
        }

        query.get().addOnSuccessListener(result -> {
            if (result.isEmpty()) {
                Toast.makeText(this, next ? "Đây là chương cuối" : "Đây là chương đầu", Toast.LENGTH_SHORT).show();
                return;
            }

            DocumentSnapshot doc = result.getDocuments().get(0);
            chapterId = doc.getId();
            loadCurrentChapter();
        });
    }

    private void saveReadingHistory(String lastReadChapterId) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        HistoryLibraryPayload payload = new HistoryLibraryPayload(bookId, "history", lastReadChapterId);

        db.collection("Users")
                .document(uid)
                .collection("Library")
                .whereEqualTo("bookId", bookId)
                .whereEqualTo("type", "history")
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        queryDocumentSnapshots.getDocuments().get(0).getReference().set(payload);
                    } else {
                        db.collection("Users")
                                .document(uid)
                                .collection("Library")
                                .add(payload);
                    }
                });
    }

    public static class HistoryLibraryPayload {
        public String bookId;
        public String type;
        public String lastReadChapterId;
        public Timestamp timestamp;

        public HistoryLibraryPayload() {
        }

        public HistoryLibraryPayload(String bookId, String type, String lastReadChapterId) {
            this.bookId = bookId;
            this.type = type;
            this.lastReadChapterId = lastReadChapterId;
            this.timestamp = Timestamp.now();
        }
    }
}