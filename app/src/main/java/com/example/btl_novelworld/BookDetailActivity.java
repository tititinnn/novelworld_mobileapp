package com.example.btl_novelworld;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.btl_novelworld.adapters.ChapterAdapter;
import com.example.btl_novelworld.models.Book;
import com.example.btl_novelworld.models.Chapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BookDetailActivity extends AppCompatActivity {

    private ImageView imgBgBlur, imgBookCover, btnBack, imgFavoriteIcon;
    private TextView txtBookTitle, txtAuthor, txtGenre, txtViewCount, txtFavCount, txtRating,
            txtSynopsis, txtChapterCount, txtStoryStatus, txtCommentTitle;
    private Button btnStartReading, btnViewAllChapters;
    private RecyclerView rvRecentChapters;
    private LinearLayout btnFavorite, btnRate;

    private FirebaseFirestore db;
    private String bookId;
    private ChapterAdapter chapterAdapter;
    private boolean isFavorite = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_detail);

        db = FirebaseFirestore.getInstance();
        bookId = getIntent().getStringExtra("bookId");

        if (bookId == null || bookId.isEmpty()) {
            finish();
            return;
        }

        initViews();
        setupActions();
        setupRecyclerView();
        loadBookDetail();
        loadRecentChapters();
        checkFavoriteStatus();
    }

    private void initViews() {
        imgBgBlur = findViewById(R.id.img_bg_blur);
        imgBookCover = findViewById(R.id.img_book_cover);
        btnBack = findViewById(R.id.btn_back);
        imgFavoriteIcon = findViewById(R.id.img_favorite_icon);

        txtBookTitle = findViewById(R.id.txt_book_title);
        txtAuthor = findViewById(R.id.txt_author);
        txtGenre = findViewById(R.id.txt_genre);
        txtViewCount = findViewById(R.id.txt_view_count);
        txtFavCount = findViewById(R.id.txt_fav_count);
        txtRating = findViewById(R.id.txt_rating);
        txtSynopsis = findViewById(R.id.txt_synopsis);
        txtChapterCount = findViewById(R.id.txt_chapter_count);
        txtStoryStatus = findViewById(R.id.txt_story_status);
        txtCommentTitle = findViewById(R.id.txt_comment_title);

        btnStartReading = findViewById(R.id.btn_start_reading);
        btnViewAllChapters = findViewById(R.id.btn_view_all_chapters);
        rvRecentChapters = findViewById(R.id.rv_recent_chapters);
        btnFavorite = findViewById(R.id.btn_favorite);
        btnRate = findViewById(R.id.btn_rate);
        btnExpandSynopsis = findViewById(R.id.btn_expand_synopsis);
    }

    private void setupActions() {
        btnBack.setOnClickListener(v -> finish());

        btnStartReading.setOnClickListener(v -> openFirstChapter());

        btnViewAllChapters.setOnClickListener(v -> openAllChapters());

        btnExpandSynopsis.setOnClickListener(v -> toggleSynopsis());

        btnFavorite.setOnClickListener(v -> toggleFavorite());

        btnRate.setOnClickListener(v ->
                Toast.makeText(this, "Mở dialog đánh giá sau", Toast.LENGTH_SHORT).show());
    }

    private void openAllChapters() {
        Intent intent = new Intent(this, ChapterListActivity.class);
        intent.putExtra("bookId", bookId);
        startActivity(intent);
    }

    private void toggleSynopsis() {
        if (isSynopsisExpanded) {
            txtSynopsis.setMaxLines(6);
            txtSynopsis.setEllipsize(android.text.TextUtils.TruncateAt.END);
            btnExpandSynopsis.setImageResource(R.drawable.ic_arrow_down_gray);
            isSynopsisExpanded = false;
        } else {
            txtSynopsis.setMaxLines(Integer.MAX_VALUE);
            txtSynopsis.setEllipsize(null);
            btnExpandSynopsis.setImageResource(R.drawable.ic_arrow_up_gray);
            isSynopsisExpanded = true;
        }
    }
    private void setupRecyclerView() {
        chapterAdapter = new ChapterAdapter(this, bookId);
        rvRecentChapters.setLayoutManager(new LinearLayoutManager(this));
        rvRecentChapters.setNestedScrollingEnabled(false);
        rvRecentChapters.setAdapter(chapterAdapter);
    }

    private void loadBookDetail() {
        db.collection("Books")
                .document(bookId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    Book book = snapshot.toObject(Book.class);
                    if (book == null) return;

                    book.setBookId(snapshot.getId());

                    txtBookTitle.setText(book.getTitle());
                    txtAuthor.setText("Tác giả: " + safe(book.getAuthor()));
                    txtGenre.setText("Thể loại: " + formatCategories(book.getCategories()));
                    txtViewCount.setText(formatCount(book.getViewsCount()));
                    txtFavCount.setText(formatCount(book.getLikesCount()));
                    txtRating.setText(String.format(Locale.getDefault(), "%.1f", book.getRating()));
                    txtSynopsis.setText(safe(book.getDescription()));
                    txtChapterCount.setText("Số chương: " + book.getTotalChapters());
                    txtStoryStatus.setText("Tình trạng: " + safe(book.getStatus()));
                    txtCommentTitle.setText("Bình luận");

                    if (book.getCoverUrl() != null && !book.getCoverUrl().isEmpty()) {
                        Glide.with(this).load(book.getCoverUrl()).into(imgBookCover);
                        Glide.with(this).load(book.getCoverUrl()).into(imgBgBlur);
                    }
                });
    }

    private void loadRecentChapters() {
        db.collection("Books")
                .document(bookId)
                .collection("Chapters")
                .orderBy("chapterNumber", Query.Direction.DESCENDING)
                .limit(3)
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

    private void openFirstChapter() {
        db.collection("Books")
                .document(bookId)
                .collection("Chapters")
                .orderBy("chapterNumber", Query.Direction.ASCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String firstChapterId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        Intent intent = new Intent(this, ReaderActivity.class);
                        intent.putExtra("bookId", bookId);
                        intent.putExtra("chapterId", firstChapterId);
                        startActivity(intent);
                    }
                });
    }

    private void checkFavoriteStatus() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("Users")
                .document(uid)
                .collection("Library")
                .whereEqualTo("bookId", bookId)
                .whereEqualTo("type", "favorite")
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    isFavorite = !queryDocumentSnapshots.isEmpty();
                    updateFavoriteIcon();
                });
    }

    private void toggleFavorite() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Bạn cần đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("Users")
                .document(uid)
                .collection("Library")
                .whereEqualTo("bookId", bookId)
                .whereEqualTo("type", "favorite")
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        queryDocumentSnapshots.getDocuments().get(0).getReference()
                                .delete()
                                .addOnSuccessListener(unused -> {
                                    isFavorite = false;
                                    updateFavoriteIcon();
                                });
                    } else {
                        FavoriteLibraryPayload payload = new FavoriteLibraryPayload(bookId, "favorite", null);
                        db.collection("Users")
                                .document(uid)
                                .collection("Library")
                                .add(payload)
                                .addOnSuccessListener(documentReference -> {
                                    isFavorite = true;
                                    updateFavoriteIcon();
                                });
                    }
                });
    }

    private void updateFavoriteIcon() {
        imgFavoriteIcon.setImageResource(
                isFavorite
                        ? android.R.drawable.btn_star_big_on
                        : android.R.drawable.btn_star_big_off
        );
    }
    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String formatCategories(List<String> categories) {
        if (categories == null || categories.isEmpty()) return "";
        return String.join(", ", categories);
    }

    private String formatCount(long count) {
        if (count >= 1_000_000) return String.format(Locale.getDefault(), "%.1fM", count / 1_000_000f);
        if (count >= 1_000) return String.format(Locale.getDefault(), "%.1fK", count / 1_000f);
        return String.valueOf(count);
    }

    private ImageView btnExpandSynopsis;
    private boolean isSynopsisExpanded = false;

    public static class FavoriteLibraryPayload {
        public String bookId;
        public String type;
        public String lastReadChapterId;
        public com.google.firebase.Timestamp timestamp;

        public FavoriteLibraryPayload() {
        }

        public FavoriteLibraryPayload(String bookId, String type, String lastReadChapterId) {
            this.bookId = bookId;
            this.type = type;
            this.lastReadChapterId = lastReadChapterId;
            this.timestamp = com.google.firebase.Timestamp.now();
        }
    }
}