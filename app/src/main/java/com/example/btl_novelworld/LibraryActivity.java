package com.example.btl_novelworld;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btl_novelworld.adapters.BookAdapter;
import com.example.btl_novelworld.adapters.LibraryBookAdapter; // ĐÃ THÊM IMPORT NÀY
import com.example.btl_novelworld.models.Book;
import com.example.btl_novelworld.models.LibraryItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class LibraryActivity extends AppCompatActivity {

    // 1. Khai báo các thành phần Giao diện (UI)
    private ImageView btnBack;
    private TextView tabHistory, tabFavorite, tabSaved;
    private RecyclerView rvLibraryBooks, rvSuggestBooks;
    private LinearLayout navHome, navExplore, navLibrary, navProfile;

    // 2. Biến logic và Firebase
    private String currentTab = "history"; // Tab mặc định khi mở lên là Lịch sử
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // 3. Khai báo Adapter
    private BookAdapter suggestAdapter; // Dùng chung adapter với Trang chủ cho phần Đề xuất
    private LibraryBookAdapter libraryAdapter; // ĐÃ MỞ KHÓA KHAI BÁO ADAPTER TỦ SÁCH

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        // Khởi tạo kết nối Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Gọi các hàm setup
        initViews();
        setupRecyclerViews();
        setupTabs();
        setupBottomNav();

        // Tải dữ liệu lần đầu khi vừa mở trang
        updateTabUI(tabHistory, tabFavorite, tabSaved);
        loadBooksForTab(currentTab);
        loadSuggestBooks();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);

        tabHistory = findViewById(R.id.tabHistory);
        tabFavorite = findViewById(R.id.tabFavorite);
        tabSaved = findViewById(R.id.tabSaved);

        rvLibraryBooks = findViewById(R.id.rvLibraryBooks);
        rvSuggestBooks = findViewById(R.id.rvSuggestBooks);

        navHome = findViewById(R.id.navHome);
        navExplore = findViewById(R.id.navExplore);
        navLibrary = findViewById(R.id.navLibrary);
        navProfile = findViewById(R.id.navProfile);

        // Xử lý nút Back (Quay lại trang trước đó)
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerViews() {
        // --- 1. Setup cho danh sách truyện Đề Xuất (Trượt ngang) ---
        suggestAdapter = new BookAdapter(this);
        rvSuggestBooks.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvSuggestBooks.setAdapter(suggestAdapter);

        // --- 2. Setup cho danh sách Tủ Sách chính (Trượt dọc) ---
        rvLibraryBooks.setLayoutManager(new LinearLayoutManager(this));

        // ĐÃ MỞ KHÓA: Khởi tạo và gắn Adapter cho Tủ Sách
        libraryAdapter = new LibraryBookAdapter(this);
        rvLibraryBooks.setAdapter(libraryAdapter);
    }

    // --- LOGIC CHUYỂN ĐỔI GIỮA 3 TAB ---
    private void setupTabs() {
        tabHistory.setOnClickListener(v -> {
            if (!currentTab.equals("history")) {
                currentTab = "history";
                updateTabUI(tabHistory, tabFavorite, tabSaved);
                loadBooksForTab(currentTab); // Tải lại dữ liệu tương ứng
            }
        });

        tabFavorite.setOnClickListener(v -> {
            if (!currentTab.equals("favorite")) {
                currentTab = "favorite";
                updateTabUI(tabFavorite, tabHistory, tabSaved);
                loadBooksForTab(currentTab);
            }
        });

        tabSaved.setOnClickListener(v -> {
            if (!currentTab.equals("saved")) {
                currentTab = "saved";
                updateTabUI(tabSaved, tabHistory, tabFavorite);
                loadBooksForTab(currentTab);
            }
        });
    }

    // Hàm đổi màu chữ để user biết mình đang ở Tab nào
    private void updateTabUI(TextView activeTab, TextView inactive1, TextView inactive2) {
        activeTab.setTextColor(Color.parseColor("#1E88FF")); // Màu xanh dương (Đang chọn)
        inactive1.setTextColor(Color.parseColor("#222222")); // Màu xám đen (Không chọn)
        inactive2.setTextColor(Color.parseColor("#222222"));
    }

    // --- LOGIC MENU ĐIỀU HƯỚNG BÊN DƯỚI (BOTTOM NAVIGATION) ---
    private void setupBottomNav() {
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(LibraryActivity.this, HomeActivity.class);
            // Cờ này giúp xóa các Activity cũ trùng lặp trên ngăn xếp (Back Stack)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(0, 0); // Tắt animation chuyển cảnh để mượt như app thật
            finish();
        });

        navExplore.setOnClickListener(v -> {
            Intent intent = new Intent(LibraryActivity.this, ExploreActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        });

        navProfile.setOnClickListener(v -> {
            Intent intent = new Intent(LibraryActivity.this, ProfileActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        });
    }

    // --- LOGIC FIREBASE: LẤY DỮ LIỆU TỦ SÁCH THEO TAB ---
    private void loadBooksForTab(String tabType) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String userId = user.getUid();

        // và vào đúng collection "Library" để lọc
        db.collection("Users").document(userId).collection("Library")
                .whereEqualTo("type", tabType) // Lọc đúng loại: history, favorite hoặc saved
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<LibraryItem> list = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        LibraryItem item = doc.toObject(LibraryItem.class);
                        if (item != null) {
                            list.add(item);
                        }
                    }
                    if (libraryAdapter != null) {
                        libraryAdapter.submitList(list);
                    }
                });
    }

    // --- LOGIC FIREBASE: TẢI TRUYỆN ĐỀ XUẤT ---
    private void loadSuggestBooks() {
        // Lấy 5 truyện có views cao nhất trên hệ thống để làm đề xuất
        db.collection("Books")
                .orderBy("viewsCount", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Book> list = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Book book = doc.toObject(Book.class);
                        if (book != null) {
                            book.setBookId(doc.getId());
                            list.add(book);
                        }
                    }
                    if (suggestAdapter != null) {
                        suggestAdapter.submitList(list);
                    }
                });
    }
}