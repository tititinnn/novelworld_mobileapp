package com.example.btl_novelworld;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
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

        // Nhận từ khóa từ HomeActivity truyền sang
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

    private void setupActions() {
        // Nút quay lại
        btnBack.setOnClickListener(v -> finish());

        // Lắng nghe sự kiện tìm kiếm trên bàn phím
        edtSearchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {

                String newQuery = edtSearchInput.getText().toString().trim();
                if (!TextUtils.isEmpty(newQuery)) {
                    searchBooks(newQuery);
                    hideKeyboard(); // Đóng bàn phím khi bắt đầu tìm
                }
                return true;
            }
            return false;
        });
    }

    private void searchBooks(String searchText) {
        txtStatus.setVisibility(View.VISIBLE);
        txtStatus.setText("Đang tìm kiếm...");
        searchAdapter.submitList(new ArrayList<>()); // Xóa kết quả cũ trên màn hình

        // Truy vấn Firestore (Tìm theo tên sách bắt đầu bằng từ khóa)
        db.collection("Books")
                .orderBy("title")
                .startAt(searchText)
                .endAt(searchText + "\uf8ff")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Book> searchResults = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Book book = doc.toObject(Book.class);
                        if (book != null) {
                            book.setBookId(doc.getId());
                            searchResults.add(book);
                        }
                    }

                    if (searchResults.isEmpty()) {
                        txtStatus.setText("Không tìm thấy truyện nào có tên bắt đầu bằng:\n\"" + searchText + "\"");
                        txtStatus.setVisibility(View.VISIBLE);
                    } else {
                        txtStatus.setVisibility(View.GONE);
                        searchAdapter.submitList(searchResults);
                    }
                })
                .addOnFailureListener(e -> {
                    txtStatus.setText("Lỗi khi tìm kiếm. Vui lòng kiểm tra kết nối mạng!");
                    txtStatus.setVisibility(View.VISIBLE);
                });
    }

    // Hàm hỗ trợ ẩn bàn phím ảo
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}