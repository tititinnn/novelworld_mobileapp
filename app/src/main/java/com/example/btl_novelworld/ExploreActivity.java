package com.example.btl_novelworld;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btl_novelworld.adapters.BookAdapter;
import com.example.btl_novelworld.adapters.CategoryAdapter;
import com.example.btl_novelworld.models.Book;
import com.example.btl_novelworld.models.Category;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ExploreActivity extends AppCompatActivity {

    private RecyclerView rvCategories, rvBooks;
    private EditText edtSearch;
    private ImageView btnBack;
    private LinearLayout navHome, navExplore, navLibrary, navProfile;

    private FrameLayout btnLeaderboard;
    private FirebaseFirestore db;

    private CategoryAdapter categoryAdapter;
    private BookAdapter bookAdapter;

    private List<Book> allBooksOfCategory = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore);

        db = FirebaseFirestore.getInstance();
        initViews();
        setupNavigation(); // Thiết lập nút Back và Bottom Nav
        loadCategories();
    }

    private void initViews() {
        rvCategories = findViewById(R.id.rvCategories);
        rvBooks = findViewById(R.id.rvBooks);
        edtSearch = findViewById(R.id.edtSearch);
        btnBack = findViewById(R.id.btnBack);
        btnLeaderboard = findViewById(R.id.btnLeaderboard);

        // Khởi tạo các nút điều hướng
        navHome = findViewById(R.id.navHome);
        navExplore = findViewById(R.id.navExplore);
        navLibrary = findViewById(R.id.navLibrary);
        navProfile = findViewById(R.id.navProfile);

        // Cột trái: Danh mục
        rvCategories.setLayoutManager(new LinearLayoutManager(this));
        categoryAdapter = new CategoryAdapter(category -> {
            edtSearch.setText("");
            loadBooksByCategory(category.getCategoryId());
        });
        rvCategories.setAdapter(categoryAdapter);

        // Cột phải: Sách
        rvBooks.setLayoutManager(new GridLayoutManager(this, 3));
        bookAdapter = new BookAdapter(this);
        rvBooks.setAdapter(bookAdapter);

        // Xử lý tìm kiếm
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterBooks(s.toString());
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupNavigation() {
        // Xử lý nút Back
        btnBack.setOnClickListener(v -> finish());

        // Xu ly nut BXH
        btnLeaderboard.setOnClickListener(v -> {
                    Intent intent = new Intent(ExploreActivity.this, ExploreRankActivity.class);
                    startActivity(intent);
        });

        // Xử lý Bottom Navigation
        navHome.setOnClickListener(v -> {
            navigateTo(HomeActivity.class); // Giả sử Home của bạn là MainActivity
        });

        navExplore.setOnClickListener(v -> {
            // Đang ở trang Khám phá rồi nên không cần làm gì hoặc chỉ cần scroll lên đầu
            rvBooks.smoothScrollToPosition(0);
        });

        navLibrary.setOnClickListener(v -> {
            navigateTo(LibraryActivity.class);
        });

        navProfile.setOnClickListener(v -> {
            navigateTo(ProfileActivity.class);
        });
    }

    // Hàm bổ trợ để chuyển trang mượt mà
    private void navigateTo(Class<?> targetActivity) {
        Intent intent = new Intent(ExploreActivity.this, targetActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        overridePendingTransition(0, 0); // Xóa hiệu ứng chuyển trang để cảm giác như tab
        if (targetActivity != ExploreActivity.class) {
            finish();
        }
    }

    private void loadCategories() {
        db.collection("Categories")
                .orderBy("order", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Category> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots) {
                        Category cat = doc.toObject(Category.class);
                        if (cat != null) {
                            cat.setCategoryId(doc.getId());
                            list.add(cat);
                        }
                    }
                    categoryAdapter.submitList(list);

                    if (!list.isEmpty()) {
                        loadBooksByCategory(list.get(0).getCategoryId());
                    }
                });
    }

    private void loadBooksByCategory(String categoryId) {
        db.collection("Books")
                .whereArrayContains("categories", categoryId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    allBooksOfCategory.clear();
                    for (DocumentSnapshot doc : snapshots) {
                        Book book = doc.toObject(Book.class);
                        if (book != null) {
                            book.setBookId(doc.getId());
                            allBooksOfCategory.add(book);
                        }
                    }
                    bookAdapter.submitList(allBooksOfCategory);
                });
    }

    private void filterBooks(String query) {
        if (query.isEmpty()) {
            bookAdapter.submitList(allBooksOfCategory);
            return;
        }
        List<Book> filtered = new ArrayList<>();
        for (Book b : allBooksOfCategory) {
            if (b.getTitle() != null && b.getTitle().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(b);
            }
        }
        bookAdapter.submitList(filtered);
    }
}