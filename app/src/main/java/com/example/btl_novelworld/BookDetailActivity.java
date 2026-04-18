package com.example.btl_novelworld;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.example.btl_novelworld.models.Book;
import com.example.btl_novelworld.models.Chapter;
import com.example.btl_novelworld.models.Comment; // ĐÃ SỬA: Import đúng model của bạn
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.google.android.material.bottomsheet.BottomSheetDialog;

// XÓA BỎ import org.w3c.dom.Comment; vì gây xung đột

public class BookDetailActivity extends AppCompatActivity {

    private ImageView imgBgBlur, imgBookCover, btnBack, imgFavoriteIcon, btnExpandSynopsis;
    private TextView txtBookTitle, txtAuthor, txtGenre, txtViewCount, txtFavCount, txtRating,
            txtSynopsis, txtChapterCount, txtStoryStatus, txtCommentTitle;
    private Button btnStartReading, btnViewAllChapters;
    private CardView btnOpenComments;
    private BottomSheetDialog commentDialog;
    private RecyclerView rvRecentChapters;
    private LinearLayout btnFavorite, btnRate;

    private FirebaseFirestore db;
    private String bookId;
    private ChapterAdapter chapterAdapter;
    private boolean isFavorite = false;
    private boolean isSynopsisExpanded = false;

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

        initViews();
        setupActions();
        setupRecyclerView();
        loadBookDetail();
        loadRecentChapters();
        checkFavoriteStatus();
        loadCommentCount();
    }
    private void loadCommentCount() {
        db.collection("Books").document(bookId).collection("Comments")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    // Cập nhật số lượng hiển thị trên CardView TRƯỚC khi nhấn vào
                    txtCommentTitle.setText("Bình luận (" + value.size() + ")");
                });
    }
    private void initViews() {
        imgBgBlur = findViewById(R.id.img_bg_blur);
        imgBookCover = findViewById(R.id.img_book_cover);
        btnBack = findViewById(R.id.btn_back);
        imgFavoriteIcon = findViewById(R.id.img_favorite_icon);
        btnExpandSynopsis = findViewById(R.id.btn_expand_synopsis);
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
        btnOpenComments.setOnClickListener(v -> showCommentSheet());
        btnRate.setOnClickListener(v ->
                Toast.makeText(this, "Cảm ơn bạn đã quan tâm đến truyện!", Toast.LENGTH_SHORT).show());
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
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Truy vấn lấy thông tin người dùng từ Firestore để có tên chính xác nhất
        db.collection("Users").document(uid).get().addOnSuccessListener(userDoc -> {
            String username = "Người dùng";
            if (userDoc.exists()) {
                // Giả sử trong Firestore bạn đặt field tên là "name" hoặc "username"
                username = userDoc.getString("username");
                if (username == null) username = userDoc.getString("name");
            }

            // Nếu vẫn null thì mới lấy từ Auth Profile
            if (TextUtils.isEmpty(username)) {
                username = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            }

            // Cuối cùng nếu vẫn không có thì mới để mặc định
            if (TextUtils.isEmpty(username)) username = "Người dùng ẩn danh";

            final String finalUsername = username;

            // Tiến hành lưu Comment với finalUsername
            java.util.Map<String, Object> commentData = new java.util.HashMap<>();
            commentData.put("uid", uid);
            commentData.put("userName", finalUsername);
            commentData.put("content", content);
            commentData.put("bookId", bookId);
            commentData.put("timestamp", com.google.firebase.Timestamp.now());

            // Lưu vào Books và Users (như cũ)
            db.collection("Books").document(bookId).collection("Comments").add(commentData);
            db.collection("Users").document(uid).collection("Comments").add(commentData)
                    .addOnSuccessListener(ref -> {
                        edt.setText("");
                        Toast.makeText(this, "Đã gửi bình luận!", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void setupRecyclerView() {
        chapterAdapter = new ChapterAdapter(this, bookId);
        rvRecentChapters.setLayoutManager(new LinearLayoutManager(this));
        rvRecentChapters.setNestedScrollingEnabled(false);
        rvRecentChapters.setAdapter(chapterAdapter);
    }

    private void loadBookDetail() {
        db.collection("Books").document(bookId).get().addOnSuccessListener(snapshot -> {
            Book book = snapshot.toObject(Book.class);
            if (book == null) return;

            txtBookTitle.setText(book.getTitle());
            txtAuthor.setText("Tác giả: " + safe(book.getAuthor()));
            fetchCategoryNames(book.getCategories());
            txtViewCount.setText(formatCount(book.getViewsCount()));
            txtFavCount.setText(formatCount(book.getLikesCount()));
            txtRating.setText(String.format(Locale.getDefault(), "%.1f", book.getRating()));
            txtSynopsis.setText(safe(book.getDescription()));
            txtChapterCount.setText("Số chương: " + book.getTotalChapters());
            txtStoryStatus.setText("Tình trạng: " + safe(book.getStatus()));

            if (book.getCoverUrl() != null && !book.getCoverUrl().isEmpty()) {
                Glide.with(this).load(book.getCoverUrl()).into(imgBookCover);
                Glide.with(this).load(book.getCoverUrl()).centerCrop().into(imgBgBlur);
            }
        });
    }

    private void fetchCategoryNames(List<String> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            txtGenre.setText("Thể loại: Khác");
            return;
        }
        db.collection("Categories")
                .whereIn(FieldPath.documentId(), categoryIds)
                .get()
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
                .orderBy("chapterNumber", Query.Direction.DESCENDING)
                .limit(3)
                .get()
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
                .whereEqualTo("bookId", bookId)
                .whereEqualTo("type", "favorite")
                .get()
                .addOnSuccessListener(snapshots -> {
                    isFavorite = !snapshots.isEmpty();
                    updateFavoriteIcon();
                });
    }

    private void toggleFavorite() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để lưu truyện!", Toast.LENGTH_SHORT).show();
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
        db.collection("Books").document(bookId).collection("Chapters")
                .orderBy("chapterNumber", Query.Direction.ASCENDING)
                .limit(1).get().addOnSuccessListener(snapshots -> {
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

    public static class FavoritePayload {
        public String bookId;
        public String type;
        public com.google.firebase.Timestamp timestamp;
        public FavoritePayload(String bookId, String type) {
            this.bookId = bookId;
            this.type = type;
            this.timestamp = com.google.firebase.Timestamp.now();
        }
    }
}