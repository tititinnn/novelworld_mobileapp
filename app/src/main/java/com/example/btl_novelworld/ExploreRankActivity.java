package com.example.btl_novelworld;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.btl_novelworld.adapters.BookAdapter;
import com.example.btl_novelworld.models.Book;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class ExploreRankActivity extends AppCompatActivity {

    private RecyclerView rvMonthRanking, rvWeekRanking;
    private ImageView btnBack;
    private EditText edtSearch;
    private LinearLayout navHome, navExplore, navLibrary, navProfile;

    private BookAdapter monthAdapter, weekAdapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore_rank);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerViews();
        setupNavigation();

        fetchRankings();
    }

    private void initViews() {
        rvMonthRanking = findViewById(R.id.rvMonthRanking);
        rvWeekRanking = findViewById(R.id.rvWeekRanking);
        btnBack = findViewById(R.id.btnBack);
        edtSearch = findViewById(R.id.edtSearch);

        navHome = findViewById(R.id.navHome);
        navExplore = findViewById(R.id.navExplore);
        navLibrary = findViewById(R.id.navLibrary);
        navProfile = findViewById(R.id.navProfile);
    }

    private void setupRecyclerViews() {
        // BXH Tháng
        rvMonthRanking.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        monthAdapter = new BookAdapter(this);
        monthAdapter.setRanking(true); // Kích hoạt hiển thị số rank
        rvMonthRanking.setAdapter(monthAdapter);

        // BXH Tuần
        rvWeekRanking.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        weekAdapter = new BookAdapter(this);
        weekAdapter.setRanking(true); // Kích hoạt hiển thị số rank
        rvWeekRanking.setAdapter(weekAdapter);
    }

    private void fetchRankings() {
// Lấy BXH Tháng: Sắp xếp theo viewsMonth GIẢM DẦN (Nhiều view nhất lên đầu)
        db.collection("Books")
                .orderBy("viewsMonth", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Book> monthBooks = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots) {
                        Book book = doc.toObject(Book.class);
                        if (book != null) {
                            book.setBookId(doc.getId());
                            monthBooks.add(book);
                        }
                    }
                    monthAdapter.submitList(monthBooks);
                });

        // Lấy BXH Tuần: Sắp xếp theo viewsWeek GIẢM DẦN
        db.collection("Books")
                .orderBy("viewsWeek", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Book> weekBooks = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots) {
                        Book book = doc.toObject(Book.class);
                        if (book != null) {
                            book.setBookId(doc.getId());
                            weekBooks.add(book);
                        }
                    }
                    weekAdapter.submitList(weekBooks);
                });
    }

    private void setupNavigation() {
        btnBack.setOnClickListener(v -> finish());

        // Bottom Nav logic
        navHome.setOnClickListener(v -> navigateTo(HomeActivity.class));
        navLibrary.setOnClickListener(v -> navigateTo(LibraryActivity.class));
        navProfile.setOnClickListener(v -> navigateTo(ProfileActivity.class));

        // Nút Tìm kiếm (Nếu bạn muốn nhấn icon search để thực hiện hành động)
        findViewById(R.id.btnBookshelf).setOnClickListener(v -> {
            // Có thể mở bộ lọc hoặc thực hiện tìm kiếm từ edtSearch
        });
    }

    private void navigateTo(Class<?> targetActivity) {
        Intent intent = new Intent(this, targetActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }
}