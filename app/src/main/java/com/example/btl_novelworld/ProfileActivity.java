package com.example.btl_novelworld;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class ProfileActivity extends AppCompatActivity {

    // Khai báo View
    private ImageView btnBack, img_avatar;
    private ImageButton btnSetting;
    private TextView txt_username;
    private LinearLayout itemRecent, itemComment, itemUploadRequest, itemTranslate;
    private LinearLayout itemAbout, itemFeedback, itemLogout;
    private LinearLayout navHome, navExplore, navLibrary, navProfile;

    // Khai báo Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Khởi tạo Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 1. Ánh xạ toàn bộ view
        initViews();

        // 2. Lấy thông tin người dùng
        loadUserInfo();

        // 3. Cài đặt các sự kiện Click
        setupClickListeners();
    }

    private void initViews() {
        // Header
        btnBack = findViewById(R.id.btnBack);
        btnSetting = findViewById(R.id.btnSetting);

        // Info
        img_avatar = findViewById(R.id.img_avatar);
        txt_username = findViewById(R.id.txt_username);

        // Card 1
        itemRecent = findViewById(R.id.itemRecent);
        itemComment = findViewById(R.id.itemComment);
        itemUploadRequest = findViewById(R.id.itemUploadRequest);
        itemTranslate = findViewById(R.id.itemTranslate);

        // Card 2
        itemAbout = findViewById(R.id.itemAbout);
        itemFeedback = findViewById(R.id.itemFeedback);

        // Logout
        itemLogout = findViewById(R.id.itemLogout);

        // Bottom Nav
        navHome = findViewById(R.id.navHome);
        navExplore = findViewById(R.id.navExplore);
        navLibrary = findViewById(R.id.navLibrary);
        navProfile = findViewById(R.id.navProfile); // Nút hiện tại
    }

    private void loadUserInfo() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();

            // Hiển thị tên hiển thị hoặc email lên giao diện
            String displayName = currentUser.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                txt_username.setText(displayName);
            } else {
                txt_username.setText(currentUser.getEmail()); // Dùng email nếu chưa có tên
            }
        } else {
            txt_username.setText("Chưa đăng nhập");
        }
    }

    private void setupClickListeners() {
        // --- CÁC NÚT ĐIỀU HƯỚNG CƠ BẢN ---
        btnBack.setOnClickListener(v -> finish());

        btnSetting.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, SettingsActivity.class));
        });

        // --- CARD 1: HOẠT ĐỘNG ---
        itemRecent.setOnClickListener(v -> {
            if (currentUserId != null) {
                fetchRecentHistory();
                // TODO: Chuyển sang HistoryActivity
            } else {
                Toast.makeText(this, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show();
            }
        });

        itemComment.setOnClickListener(v -> {
            if (currentUserId != null) {
                fetchUserComments();
                // TODO: Chuyển sang MyCommentsActivity
            } else {
                Toast.makeText(this, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show();
            }
        });

        itemUploadRequest.setOnClickListener(v ->
                Toast.makeText(this, "Tính năng Gửi yêu cầu đăng tải đang phát triển", Toast.LENGTH_SHORT).show());

        itemTranslate.setOnClickListener(v ->
                Toast.makeText(this, "Tính năng Đóng góp bản dịch đang phát triển", Toast.LENGTH_SHORT).show());

        // --- CARD 2: THÔNG TIN ---
        itemAbout.setOnClickListener(v ->
                Toast.makeText(this, "NovelWorld v1.0 - Bản quyền thuộc về DHT Group", Toast.LENGTH_SHORT).show());

        itemFeedback.setOnClickListener(v ->
                Toast.makeText(this, "Tính năng Phản hồi ý kiến đang phát triển", Toast.LENGTH_SHORT).show());

        // --- ĐĂNG XUẤT ---
        itemLogout.setOnClickListener(v -> {
            if (currentUserId != null) {
                new AlertDialog.Builder(this)
                        .setTitle("Đăng xuất")
                        .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                        .setPositiveButton("Đăng xuất", (dialog, which) -> {
                            mAuth.signOut();
                            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            } else {
                Toast.makeText(this, "Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            }
        });

        // --- BOTTOM NAVIGATION ---
        navHome.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, HomeActivity.class));
            finish(); // Đóng Activity hiện tại để tránh đầy RAM
        });

        navExplore.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, ExploreActivity.class));
            finish();
        });

        navLibrary.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, LibraryActivity.class));
            finish();
        });

        // navProfile: Không xử lý vì đang ở chính màn hình này
    }

    // ================= CHỨC NĂNG FIREBASE =================

    private void fetchRecentHistory() {
        if (currentUserId == null) return;

        db.collection("Users").document(currentUserId).collection("Library")
                .whereEqualTo("type", "history")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int count = 0;
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String bookId = document.getString("bookId");
                            String lastReadChapterId = document.getString("lastReadChapterId");
                            Log.d("FirebaseHistory", "Truyện ID: " + bookId + " - Chương: " + lastReadChapterId);
                            count++;
                        }
                        Toast.makeText(this, "Đã tải " + count + " mục lịch sử!", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e("FirebaseHistory", "Lỗi tải lịch sử", task.getException());
                    }
                });
    }

    private void fetchUserComments() {
        if (currentUserId == null) return;

        db.collection("Users").document(currentUserId).collection("Comments")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int count = 0;
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String content = document.getString("content");
                            Log.d("Firebase", "Bình luận: " + content);
                            count++;
                        }
                        Toast.makeText(this, "Đã tải " + count + " bình luận!", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e("Firebase", "Lỗi tải bình luận", task.getException());
                    }
                });
    }
}