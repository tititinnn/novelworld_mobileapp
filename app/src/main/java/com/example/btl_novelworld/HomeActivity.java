package com.example.btl_novelworld;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView; // Thêm import này

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btl_novelworld.adapters.BookAdapter;
import com.example.btl_novelworld.adapters.RecommendBookAdapter;
import com.example.btl_novelworld.models.Book;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {

    private EditText edtSearch;
    private FrameLayout btnInfo;
    private RecyclerView rvMonthBooks;
    private RecyclerView rvRecommendBooks;
    private LinearLayout navHome, navExplore, navLibrary, navProfile;
    private TextView txtMonthRankMore; // Khai báo nút xem thêm BXH

    private FirebaseFirestore db;
    private BookAdapter monthAdapter;
    private RecommendBookAdapter recommendAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerViews();
        setupActions();
        loadMonthBooks();
        loadRecommendBooks();
    }

    private void initViews() {
        edtSearch = findViewById(R.id.edtSearch);
        btnInfo = findViewById(R.id.btnInfo);
        rvMonthBooks = findViewById(R.id.rvMonthBooks);
        rvRecommendBooks = findViewById(R.id.rvRecommendBooks);
        navHome = findViewById(R.id.navHome);
        navExplore = findViewById(R.id.navExplore);
        navLibrary = findViewById(R.id.navLibrary);
        navProfile = findViewById(R.id.navProfile);

        // Ánh xạ TextView BXH Hoàn chỉnh
        txtMonthRankMore = findViewById(R.id.txtMonthRankMore);
    }

    private void setupRecyclerViews() {
        monthAdapter = new BookAdapter(this);
        // KÍCH HOẠT HIỂN THỊ SỐ RANK (Dựa trên hàm setRanking đã thêm vào BookAdapter)
        monthAdapter.setRanking(true);

        recommendAdapter = new RecommendBookAdapter(this);

        rvMonthBooks.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );
        rvMonthBooks.setNestedScrollingEnabled(false);
        rvMonthBooks.setAdapter(monthAdapter);

        rvRecommendBooks.setLayoutManager(new LinearLayoutManager(this));
        rvRecommendBooks.setNestedScrollingEnabled(false);
        rvRecommendBooks.setAdapter(recommendAdapter);
    }

    private void setupActions() {
        // Sự kiện khi ấn vào "BXH Hoàn chỉnh ▶"
        txtMonthRankMore.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ExploreRankActivity.class);
            startActivity(intent);
        });

        // Điều hướng Bottom Nav
        navExplore.setOnClickListener(v -> {
            startActivity(new Intent(this, ExploreActivity.class));
            overridePendingTransition(0, 0);
        });

        navLibrary.setOnClickListener(v -> {
            startActivity(new Intent(this, LibraryActivity.class));
            overridePendingTransition(0, 0);
        });

        navProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            overridePendingTransition(0, 0);
        });
    }

    private void loadMonthBooks() {
        // Sắp xếp theo viewsCount giảm dần để lấy top tháng
        db.collection("Books")
                .orderBy("viewsCount", Query.Direction.DESCENDING)
                .limit(10)
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
                    monthAdapter.submitList(list);
                });
    }

    private void loadRecommendBooks() {
        db.collection("Books")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Book> bookList = new ArrayList<>();
                    List<String> allCategoryIds = new ArrayList<>();

                    // Bước 1: Thu thập danh sách sách và các ID thể loại cần tìm
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Book book = doc.toObject(Book.class);
                        if (book != null) {
                            book.setBookId(doc.getId());
                            bookList.add(book);

                            // Gom tất cả ID thể loại vào một danh sách để truy vấn 1 lần
                            if (book.getCategories() != null) {
                                allCategoryIds.addAll(book.getCategories());
                            }
                        }
                    }

                    if (bookList.isEmpty()) return;

                    // Bước 2: Truy vấn collection Categories để lấy tên tương ứng
                    fetchCategoryNamesAndDisplay(bookList, allCategoryIds);
                });
    }

    private void fetchCategoryNamesAndDisplay(List<Book> bookList, List<String> categoryIds) {
        if (categoryIds.isEmpty()) {
            recommendAdapter.submitList(bookList);
            return;
        }

        // Firestore chỉ cho phép whereIn tối đa 10-30 phần tử, nếu nhiều hơn bạn cần xử lý chia nhỏ
        db.collection("Categories")
                .whereIn(FieldPath.documentId(), categoryIds)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    // Tạo một Map để tra cứu nhanh: Key là ID, Value là Name
                    Map<String, String> categoryMap = new HashMap<>();
                    for (DocumentSnapshot doc : querySnapshots) {
                        categoryMap.put(doc.getId(), doc.getString("name"));
                    }

                    // Bước 3: Gán tên thể loại vào từng cuốn sách
                    for (Book book : bookList) {
                        List<String> ids = book.getCategories();
                        if (ids != null && !ids.isEmpty()) {
                            List<String> names = new ArrayList<>();
                            for (String id : ids) {
                                if (categoryMap.containsKey(id)) {
                                    names.add(categoryMap.get(id));
                                }
                            }
                            // Nối các tên lại: "Hành động, Tình cảm"
                            book.setCategoryNamesDisplay(TextUtils.join(", ", names));
                        } else {
                            book.setCategoryNamesDisplay("Chưa phân loại");
                        }
                    }

                    // Bước 4: Cập nhật lên Adapter
                    recommendAdapter.submitList(bookList);
                });
    }
}