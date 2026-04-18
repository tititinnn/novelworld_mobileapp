package com.example.btl_novelworld;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btl_novelworld.adapters.RecommendBookAdapter;
import com.example.btl_novelworld.models.Book;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private ImageView btnBack;
    private EditText edtSearchInput;
    private TextView txtStatus;
    private RecyclerView rvSearchResults;

    private RecommendBookAdapter searchAdapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        db = FirebaseFirestore.getInstance();
        initViews();
        setupRecyclerView();
        setupActions();

        String query = getIntent().getStringExtra("QUERY");
        if (query != null && !query.isEmpty()) {
            edtSearchInput.setText(query);
            searchBooks(query);
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        edtSearchInput = findViewById(R.id.edtSearchInput);
        txtStatus = findViewById(R.id.txtStatus);
        rvSearchResults = findViewById(R.id.rvSearchResults);
    }

    private void setupRecyclerView() {
        searchAdapter = new RecommendBookAdapter(this);
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        rvSearchResults.setAdapter(searchAdapter);
    }
    @SuppressLint("ClickableViewAccessibility")
    private void setupActions() {
        btnBack.setOnClickListener(v -> finish());

        edtSearchInput.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (edtSearchInput.getRight() - edtSearchInput.getCompoundDrawables()[2].getBounds().width() - edtSearchInput.getPaddingEnd())) {
                    String newQuery = edtSearchInput.getText().toString().trim();
                    if (!TextUtils.isEmpty(newQuery)) {
                        searchBooks(newQuery); // Gọi hàm search đã sửa ở bước trước
                        hideKeyboard();
                    }
                    return true;
                }
            }
            return false;
        });
    }

    private void searchBooks(String searchText) {
        // Chuẩn hóa từ khóa tìm kiếm về chữ thường
        final String finalSearchText = searchText.toLowerCase().trim();

        txtStatus.setVisibility(View.VISIBLE);
        txtStatus.setText("Đang tìm kiếm...");
        searchAdapter.submitList(new ArrayList<>());

        // Lấy toàn bộ hoặc một lượng lớn sách để lọc (Giải pháp tối ưu cho BTL ít dữ liệu)
        db.collection("Books")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Book> searchResults = new ArrayList<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Book book = doc.toObject(Book.class);
                        if (book != null) {
                            book.setBookId(doc.getId());

                            // Lấy tên và tác giả, chuyển về chữ thường để so sánh
                            String title = (book.getTitle() != null) ? book.getTitle().toLowerCase() : "";
                            String author = (book.getAuthor() != null) ? book.getAuthor().toLowerCase() : "";

                            // Kiểm tra nếu từ khóa xuất hiện trong Tên HOẶC Tác giả
                            if (title.contains(finalSearchText) || author.contains(finalSearchText)) {
                                searchResults.add(book);
                            }
                        }
                    }

                    if (searchResults.isEmpty()) {
                        txtStatus.setText("Không tìm thấy kết quả cho: \"" + searchText + "\"");
                        txtStatus.setVisibility(View.VISIBLE);
                    } else {
                        txtStatus.setVisibility(View.GONE);
                        searchAdapter.submitList(searchResults);
                    }
                })
                .addOnFailureListener(e -> {
                    txtStatus.setText("Lỗi kết nối. Vui lòng thử lại!");
                    txtStatus.setVisibility(View.VISIBLE);
                });
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}