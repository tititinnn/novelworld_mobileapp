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
import com.example.btl_novelworld.adapters.LibraryBookAdapter;
import com.example.btl_novelworld.database.AppDatabase;
import com.example.btl_novelworld.models.Book;
import com.example.btl_novelworld.models.LibraryItem;
import com.example.btl_novelworld.models.LocalBook;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LibraryActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tabHistory, tabFavorite, tabSaved;
    private RecyclerView rvLibraryBooks, rvSuggestBooks;
    private LinearLayout navHome, navExplore, navLibrary, navProfile;

    private String currentTab = "history";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private BookAdapter suggestAdapter;
    private LibraryBookAdapter libraryAdapter;

    // Các biến phục vụ lấy dữ liệu Offline
    private AppDatabase appDb;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Khởi tạo Database và Luồng chạy ngầm
        appDb = AppDatabase.getInstance(this);
        executorService = Executors.newSingleThreadExecutor();

        initViews();
        setupRecyclerViews();
        setupTabs();
        setupBottomNav();

        // Load dữ liệu mặc định ban đầu
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

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerViews() {
        suggestAdapter = new BookAdapter(this);
        rvSuggestBooks.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvSuggestBooks.setAdapter(suggestAdapter);

        rvLibraryBooks.setLayoutManager(new LinearLayoutManager(this));
        libraryAdapter = new LibraryBookAdapter(this);
        rvLibraryBooks.setAdapter(libraryAdapter);
    }

    private void setupTabs() {
        tabHistory.setOnClickListener(v -> {
            if (!currentTab.equals("history")) {
                currentTab = "history";
                updateTabUI(tabHistory, tabFavorite, tabSaved);
                loadBooksForTab(currentTab);
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

    private void updateTabUI(TextView activeTab, TextView inactive1, TextView inactive2) {
        activeTab.setTextColor(Color.parseColor("#1E88FF"));
        inactive1.setTextColor(Color.parseColor("#222222"));
        inactive2.setTextColor(Color.parseColor("#222222"));
    }

    // --- LOGIC CHÍNH: PHÂN CHIA OFFLINE / ONLINE ---
    private void loadBooksForTab(String tabType) {

        // 1. Nếu là tab "Đã lưu" -> Lấy từ ROOM DATABASE (Offline)
        if ("saved".equals(tabType)) {
            executorService.execute(() -> {
                List<LocalBook> localBooks = appDb.offlineDao().getAllOfflineBooks();
                List<LibraryItem> offlineItems = new ArrayList<>();

                for (LocalBook lb : localBooks) {
                    // SỬ DỤNG LỚP ẨN DANH: Ghi đè hàm getBookId và getType
                    // Không chạm vào biến private, không cần biến title, author bên trong LibraryItem
                    LibraryItem item = new LibraryItem() {
                        @Override
                        public String getBookId() {
                            return lb.bookId;
                        }

                        @Override
                        public String getType() {
                            return "offline";
                        }
                    };

                    offlineItems.add(item);
                }

                // Đẩy dữ liệu lên giao diện
                runOnUiThread(() -> {
                    if (libraryAdapter != null) {
                        libraryAdapter.submitList(offlineItems);
                    }
                });
            });
            return;
        }

        // 2. Các tab khác (Lịch sử, Yêu thích) -> Lấy từ FIREBASE (Online)
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để xem tủ sách!", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("Users").document(user.getUid()).collection("Library")
                .whereEqualTo("type", tabType)
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

    private void loadSuggestBooks() {
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

    private void setupBottomNav() {
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        });

        navExplore.setOnClickListener(v -> {
            Intent intent = new Intent(this, ExploreActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        });

        navProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}