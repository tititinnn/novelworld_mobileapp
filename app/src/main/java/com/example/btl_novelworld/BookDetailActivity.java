package com.example.btl_novelworld;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.btl_novelworld.adapters.ChapterAdapter;
import com.example.btl_novelworld.adapters.CommentAdapter;
import com.example.btl_novelworld.database.AppDatabase;
import com.example.btl_novelworld.models.Book;
import com.example.btl_novelworld.models.Chapter;
import com.example.btl_novelworld.models.Comment;
import com.example.btl_novelworld.models.LocalBook;
import com.example.btl_novelworld.models.LocalChapter;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BookDetailActivity extends AppCompatActivity {

    // --- VIEW BIẾN ---
    private ImageView imgBgBlur, imgBookCover, btnBack, imgFavoriteIcon, btnExpandSynopsis, btnDownload;
    private TextView txtBookTitle, txtAuthor, txtGenre, txtViewCount, txtFavCount, txtRating,
            txtSynopsis, txtChapterCount, txtStoryStatus, txtCommentTitle;
    private Button btnStartReading, btnViewAllChapters;
    private CardView btnOpenComments;
    private BottomSheetDialog commentDialog;
    private RecyclerView rvRecentChapters;
    private LinearLayout btnFavorite, btnRate;

    // --- DATA BIẾN ---
    private FirebaseFirestore db;
    private String bookId;
    private ChapterAdapter chapterAdapter;
    private boolean isFavorite = false;
    private boolean isSynopsisExpanded = false;

    // --- OFFLINE BIẾN ---
    private Book currentBook;
    private AppDatabase appDb;
    private ExecutorService executorService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_detail);

        db = FirebaseFirestore.getInstance();
        bookId = getIntent().getStringExtra("bookId");

        if (bookId == null || bookId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy dữ liệu sách!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        appDb = AppDatabase.getInstance(this);
        executorService = Executors.newSingleThreadExecutor();

        initViews();
        setupActions();
        setupRecyclerView();
        loadBookDetail();
        loadRecentChapters();
        checkFavoriteStatus();
        loadCommentCount();
    }

    private void initViews() {
        imgBgBlur = findViewById(R.id.img_bg_blur);
        imgBookCover = findViewById(R.id.img_book_cover);
        btnBack = findViewById(R.id.btn_back);
        imgFavoriteIcon = findViewById(R.id.img_favorite_icon);
        btnExpandSynopsis = findViewById(R.id.btn_expand_synopsis);
        btnDownload = findViewById(R.id.btn_download);

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
        btnOpenComments = findViewById(R.id.btn_open_comments);
        rvRecentChapters = findViewById(R.id.rv_recent_chapters);
        btnFavorite = findViewById(R.id.btn_favorite);
        btnRate = findViewById(R.id.btn_rate);
    }

    private void setupActions() {
        btnBack.setOnClickListener(v -> finish());
        btnStartReading.setOnClickListener(v -> openFirstChapter());
        btnViewAllChapters.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChapterListActivity.class);
            intent.putExtra("bookId", bookId);
            startActivity(intent);
        });
        btnExpandSynopsis.setOnClickListener(v -> toggleSynopsis());
        btnFavorite.setOnClickListener(v -> toggleFavorite());

        // SỬA: Chỉ giữ 1 Listener để mở Comment Sheet
        btnOpenComments.setOnClickListener(v -> showCommentSheet());

        if (btnDownload != null) {
            btnDownload.setOnClickListener(v -> {
                if (currentBook != null) {
                    downloadBookOffline(currentBook);
                } else {
                    Toast.makeText(this, "Đang tải dữ liệu, vui lòng đợi!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        btnRate.setOnClickListener(v -> showRatingDialog());
    }

    private void loadCommentCount() {
        db.collection("Books").document(bookId).collection("Comments")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    txtCommentTitle.setText("Bình luận (" + value.size() + ")");
                });
    }

    private void showCommentSheet() {
        commentDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.item_comment_sheet, null);

        RecyclerView rvComments = view.findViewById(R.id.rv_comments);
        EditText edtComment = view.findViewById(R.id.edt_comment);
        ImageView btnSend = view.findViewById(R.id.btn_send_comment);

        // Khởi tạo list và adapter bằng model Comment đúng
        List<Comment> sheetCommentList = new ArrayList<>();
        CommentAdapter sheetAdapter = new CommentAdapter(sheetCommentList);

        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(sheetAdapter);

        // Lắng nghe dữ liệu
        db.collection("Books").document(bookId).collection("Comments")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        sheetCommentList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Comment c = doc.toObject(Comment.class);
                            if (c != null) sheetCommentList.add(c);
                        }
                        sheetAdapter.notifyDataSetChanged();
                        txtCommentTitle.setText("Bình luận (" + value.size() + ")");
                    }
                });

        btnSend.setOnClickListener(v -> {
            String content = edtComment.getText().toString().trim();
            if (!TextUtils.isEmpty(content)) {
                postComment(content, edtComment);
            }
        });

        commentDialog.setContentView(view);
        commentDialog.show();
    }

    private void postComment(String content, EditText edt) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("Users").document(uid).get().addOnSuccessListener(userDoc -> {
            String username = userDoc.getString("username");
            if (username == null) username = userDoc.getString("name");
            if (TextUtils.isEmpty(username)) username = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            if (TextUtils.isEmpty(username)) username = "Người dùng ẩn danh";

            Map<String, Object> commentData = new HashMap<>();
            commentData.put("uid", uid);
            commentData.put("userName", username);
            commentData.put("content", content);
            commentData.put("bookId", bookId);
            commentData.put("timestamp", FieldValue.serverTimestamp());

            db.collection("Books").document(bookId).collection("Comments").add(commentData);
            db.collection("Users").document(uid).collection("Comments").add(commentData)
                    .addOnSuccessListener(ref -> {
                        edt.setText("");
                        Toast.makeText(this, "Đã gửi bình luận!", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void loadBookDetail() {
        db.collection("Books").document(bookId).get().addOnSuccessListener(snapshot -> {
            currentBook = snapshot.toObject(Book.class);
            if (currentBook == null) return;
            currentBook.setBookId(snapshot.getId());

            txtBookTitle.setText(currentBook.getTitle());
            txtAuthor.setText("Tác giả: " + safe(currentBook.getAuthor()));
            fetchCategoryNames(currentBook.getCategories());
            txtViewCount.setText(formatCount(currentBook.getViewsCount()));
            txtFavCount.setText(formatCount(currentBook.getLikesCount()));
            txtRating.setText(String.format(Locale.getDefault(), "%.1f", currentBook.getRating()));
            txtSynopsis.setText(safe(currentBook.getDescription()));
            txtChapterCount.setText("Số chương: " + currentBook.getTotalChapters());
            txtStoryStatus.setText("Tình trạng: " + safe(currentBook.getStatus()));

            if (currentBook.getCoverUrl() != null && !currentBook.getCoverUrl().isEmpty()) {
                Glide.with(this).load(currentBook.getCoverUrl()).into(imgBookCover);
                Glide.with(this).load(currentBook.getCoverUrl()).centerCrop().into(imgBgBlur);
            }
        });
    }

    private void downloadBookOffline(Book book) {
        executorService.execute(() -> {
            LocalBook existingBook = appDb.offlineDao().getBookById(book.getBookId());
            if (existingBook != null) {
                runOnUiThread(() -> Toast.makeText(this, "Truyện đã có trong tủ sách!", Toast.LENGTH_SHORT).show());
                return;
            }

            runOnUiThread(() -> Toast.makeText(this, "Đang bắt đầu tải truyện...", Toast.LENGTH_SHORT).show());

            db.collection("Books").document(bookId).collection("Chapters")
                    .orderBy("chapterNumber", Query.Direction.ASCENDING).get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<LocalChapter> localChapters = new ArrayList<>();
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Chapter chapter = doc.toObject(Chapter.class);
                            if (chapter != null) {
                                localChapters.add(new LocalChapter(doc.getId(), bookId,
                                        chapter.getChapterNumber(), chapter.getChapterTitle(), chapter.getContent()));
                            }
                        }

                        if (localChapters.isEmpty()) return;

                        executorService.execute(() -> {
                            LocalBook localBook = new LocalBook(book.getBookId(), book.getTitle(),
                                    book.getAuthor(), book.getCoverUrl(),
                                    txtGenre.getText().toString().replace("Thể loại: ", ""),
                                    localChapters.size());
                            appDb.offlineDao().insertBook(localBook);
                            appDb.offlineDao().insertChapters(localChapters);
                            runOnUiThread(() -> Toast.makeText(this, "Đã tải xong: " + book.getTitle(), Toast.LENGTH_LONG).show());
                        });
                    });
        });
    }

    private void setupRecyclerView() {
        chapterAdapter = new ChapterAdapter(this, bookId);
        rvRecentChapters.setLayoutManager(new LinearLayoutManager(this));
        rvRecentChapters.setNestedScrollingEnabled(false);
        rvRecentChapters.setAdapter(chapterAdapter);
    }

    private void incrementViewCount() {
        if (bookId == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("viewsCount", FieldValue.increment(1));
        updates.put("viewsWeek", FieldValue.increment(1));
        updates.put("viewsMonth", FieldValue.increment(1));
        db.collection("Books").document(bookId).update(updates);
    }

    private void fetchCategoryNames(List<String> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            txtGenre.setText("Thể loại: Khác");
            return;
        }
        db.collection("Categories").whereIn(FieldPath.documentId(), categoryIds).get()
                .addOnSuccessListener(querySnapshots -> {
                    List<String> names = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshots) {
                        String name = doc.getString("name");
                        if (name != null) names.add(name);
                    }
                    txtGenre.setText("Thể loại: " + TextUtils.join(", ", names));
                });
    }

    private void loadRecentChapters() {
        db.collection("Books").document(bookId).collection("Chapters")
                .orderBy("chapterNumber", Query.Direction.DESCENDING).limit(3).get()
                .addOnSuccessListener(snapshots -> {
                    List<Chapter> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots) {
                        Chapter ch = doc.toObject(Chapter.class);
                        if (ch != null) {
                            ch.setChapterId(doc.getId());
                            list.add(ch);
                        }
                    }
                    chapterAdapter.submitList(list);
                });
    }

    private void toggleSynopsis() {
        if (isSynopsisExpanded) {
            txtSynopsis.setMaxLines(6);
            txtSynopsis.setEllipsize(TextUtils.TruncateAt.END);
            btnExpandSynopsis.setImageResource(R.drawable.ic_arrow_down_gray);
        } else {
            txtSynopsis.setMaxLines(Integer.MAX_VALUE);
            txtSynopsis.setEllipsize(null);
            btnExpandSynopsis.setImageResource(R.drawable.ic_arrow_up_gray);
        }
        isSynopsisExpanded = !isSynopsisExpanded;
    }

    private void checkFavoriteStatus() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("Users").document(uid).collection("Library")
                .whereEqualTo("bookId", bookId).whereEqualTo("type", "favorite").get()
                .addOnSuccessListener(snapshots -> {
                    isFavorite = !snapshots.isEmpty();
                    updateFavoriteIcon();
                });
    }

    private void toggleFavorite() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (isFavorite) {
            db.collection("Users").document(uid).collection("Library")
                    .whereEqualTo("bookId", bookId).get()
                    .addOnSuccessListener(snapshots -> {
                        for (DocumentSnapshot doc : snapshots) doc.getReference().delete();
                        isFavorite = false;
                        updateFavoriteIcon();
                        Toast.makeText(this, "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();
                    });
        } else {
            FavoritePayload payload = new FavoritePayload(bookId, "favorite");
            db.collection("Users").document(uid).collection("Library").add(payload)
                    .addOnSuccessListener(ref -> {
                        isFavorite = true;
                        updateFavoriteIcon();
                        Toast.makeText(this, "Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateFavoriteIcon() {
        imgFavoriteIcon.setImageResource(isFavorite ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
    }

    private void openFirstChapter() {
        incrementViewCount();
        db.collection("Books").document(bookId).collection("Chapters")
                .orderBy("chapterNumber", Query.Direction.ASCENDING).limit(1).get()
                .addOnSuccessListener(snapshots -> {
                    if (!snapshots.isEmpty()) {
                        String chId = snapshots.getDocuments().get(0).getId();
                        Intent intent = new Intent(this, ReaderActivity.class);
                        intent.putExtra("bookId", bookId);
                        intent.putExtra("chapterId", chId);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Truyện chưa có chương nào!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String safe(String value) {
        return (value == null || value.isEmpty()) ? "Đang cập nhật" : value;
    }

    private String formatCount(long count) {
        if (count >= 1000000) return String.format(Locale.getDefault(), "%.1fM", count / 1000000f);
        if (count >= 1000) return String.format(Locale.getDefault(), "%.1fK", count / 1000f);
        return String.valueOf(count);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) executorService.shutdown();
    }

    // --- INNER CLASS ĐÃ SỬA LỖI ---
    public static class FavoritePayload {
        public String bookId;
        public String type;
        public Timestamp timestamp;

        public FavoritePayload(String bookId, String type) {
            this.bookId = bookId;
            this.type = type;
            this.timestamp = Timestamp.now();
        }
    }

    // --- CÁC HÀM XỬ LÝ TÍNH NĂNG ĐÁNH GIÁ (RATING) ---

    private void showRatingDialog() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để đánh giá truyện!", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // 1. Khởi tạo giao diện Dialog bằng Java (Không cần file XML)
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 20);
        layout.setGravity(android.view.Gravity.CENTER);

        // Tạo thanh Đánh giá sao (RatingBar)
        android.widget.RatingBar ratingBar = new android.widget.RatingBar(this, null, android.R.attr.ratingBarStyleIndicator);
        ratingBar.setIsIndicator(false); // Cho phép người dùng bấm vào để chọn
        ratingBar.setNumStars(5);
        ratingBar.setStepSize(1.0f); // Chỉ cho phép đánh giá số chẵn (1, 2, 3, 4, 5 sao)

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        ratingBar.setLayoutParams(params);
        layout.addView(ratingBar);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Đánh giá truyện")
                .setView(layout)
                .setPositiveButton("Gửi đánh giá", null) // Để null để tự kiểm soát sự kiện click
                .setNegativeButton("Hủy", (d, w) -> d.dismiss())
                .create();

        // 2. Truy vấn xem người này đã đánh giá truyện này bao giờ chưa
        // Nếu có rồi thì hiển thị lại số sao cũ họ đã chọn
        db.collection("Books").document(bookId)
                .collection("Ratings").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.getDouble("rating") != null) {
                        float oldRating = documentSnapshot.getDouble("rating").floatValue();
                        ratingBar.setRating(oldRating);
                    }
                });

        // 3. Xử lý khi bấm nút "Gửi đánh giá"
        dialog.setOnShowListener(dialogInterface -> {
            Button btnSubmit = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
            btnSubmit.setOnClickListener(v -> {
                float myRating = ratingBar.getRating();
                if (myRating == 0) {
                    Toast.makeText(this, "Vui lòng chọn ít nhất 1 sao!", Toast.LENGTH_SHORT).show();
                    return;
                }
                submitRating(uid, myRating);
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void submitRating(String uid, float rating) {
        // Lưu đánh giá của user vào sub-collection "Ratings"
        Map<String, Object> rateData = new HashMap<>();
        rateData.put("rating", rating);
        rateData.put("timestamp", FieldValue.serverTimestamp());

        db.collection("Books").document(bookId)
                .collection("Ratings").document(uid)
                .set(rateData) // Dùng set() để lấp đè lên nếu uid này đã từng đánh giá
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cảm ơn bạn đã đánh giá!", Toast.LENGTH_SHORT).show();
                    // Đánh giá xong thì tính toán lại điểm trung bình cho cuốn truyện
                    calculateAverageRating();
                });
    }

    private void calculateAverageRating() {
        // Kéo toàn bộ đánh giá của cuốn truyện này về để tính trung bình cộng
        db.collection("Books").document(bookId).collection("Ratings")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double totalScore = 0;
                    int count = queryDocumentSnapshots.size();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        if (doc.getDouble("rating") != null) {
                            totalScore += doc.getDouble("rating");
                        }
                    }

                    // Tính điểm trung bình (tránh lỗi chia cho 0)
                    double average = count > 0 ? (totalScore / count) : 0.0;

                    // Cập nhật điểm trung bình mới lên Document của Book
                    db.collection("Books").document(bookId)
                            .update("rating", average)
                            .addOnSuccessListener(aVoid -> {
                                // Cập nhật lại giao diện ngay lập tức cho người dùng thấy
                                txtRating.setText(String.format(Locale.getDefault(), "%.1f", average));
                            });
                });
    }
}