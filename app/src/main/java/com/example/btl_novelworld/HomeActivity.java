package com.example.btl_novelworld;

import android.content.Context; // Thêm import này
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager; // Thêm import này
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

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
    private TextView txtMonthRankMore;

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

        txtMonthRankMore = findViewById(R.id.txtMonthRankMore);
    }

    private void setupRecyclerViews() {
        monthAdapter = new BookAdapter(this);
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

        edtSearch.setOnEditorActionListener((v, actionId, event) -> {
            // Bắt sự kiện Search, Done, hoặc Enter
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {

                String query = edtSearch.getText().toString().trim();
                if (!TextUtils.isEmpty(query)) {
                    // Chuyển sang màn hình tìm kiếm và gửi từ khóa đi
                    Intent intent = new Intent(HomeActivity.this, SearchActivity.class);
                    intent.putExtra("QUERY", query);
                    startActivity(intent);

                    // Đóng bàn phím ảo ngay sau khi bấm tìm kiếm
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(edtSearch.getWindowToken(), 0);

                    // (Tuỳ chọn) Xóa chữ đã gõ ở Home sau khi chuyển đi
                    // edtSearch.setText("");
                }
                return true; // Đã xử lý sự kiện
            }
            return false;
        });
    }

    private void loadMonthBooks() {
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

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Book book = doc.toObject(Book.class);
                        if (book != null) {
                            book.setBookId(doc.getId());
                            bookList.add(book);

                            if (book.getCategories() != null) {
                                allCategoryIds.addAll(book.getCategories());
                            }
                        }
                    }

                    if (bookList.isEmpty()) return;

                    fetchCategoryNamesAndDisplay(bookList, allCategoryIds);
                });
    }

    private void fetchCategoryNamesAndDisplay(List<Book> bookList, List<String> categoryIds) {
        if (categoryIds.isEmpty()) {
            recommendAdapter.submitList(bookList);
            return;
        }

        db.collection("Categories")
                .whereIn(FieldPath.documentId(), categoryIds)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    Map<String, String> categoryMap = new HashMap<>();
                    for (DocumentSnapshot doc : querySnapshots) {
                        categoryMap.put(doc.getId(), doc.getString("name"));
                    }

                    for (Book book : bookList) {
                        List<String> ids = book.getCategories();
                        if (ids != null && !ids.isEmpty()) {
                            List<String> names = new ArrayList<>();
                            for (String id : ids) {
                                if (categoryMap.containsKey(id)) {
                                    names.add(categoryMap.get(id));
                                }
                            }
                            book.setCategoryNamesDisplay(TextUtils.join(", ", names));
                        } else {
                            book.setCategoryNamesDisplay("Chưa phân loại");
                        }
                    }

                    recommendAdapter.submitList(bookList);
                });
    }
}