package com.example.btl_novelworld.adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.btl_novelworld.BookDetailActivity;
import com.example.btl_novelworld.R;
import com.example.btl_novelworld.database.AppDatabase;
import com.example.btl_novelworld.models.Book;
import com.example.btl_novelworld.models.LibraryItem;
import com.example.btl_novelworld.models.LocalBook;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class LibraryBookAdapter extends RecyclerView.Adapter<LibraryBookAdapter.LibraryViewHolder> {

    private Context context;
    private List<LibraryItem> libraryItemList;

    public LibraryBookAdapter(Context context) {
        this.context = context;
        this.libraryItemList = new ArrayList<>();
    }

    public void submitList(List<LibraryItem> list) {
        this.libraryItemList = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LibraryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_library_book, parent, false);
        return new LibraryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LibraryViewHolder holder, int position) {
        LibraryItem item = libraryItemList.get(position);
        String bId = item.getBookId();
        String cId = item.getLastReadChapterId();

        // Xử lý Click vào truyện
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, BookDetailActivity.class);
            intent.putExtra("bookId", bId);
            context.startActivity(intent);
        });

        // --- RẼ NHÁNH HIỂN THỊ GIAO DIỆN ---
        if ("offline".equals(item.getType())) {

            // 1. GIAO DIỆN TAB ĐÃ LƯU (OFFLINE)
            holder.txtAuthor.setVisibility(View.VISIBLE);
            holder.txtGenre.setVisibility(View.VISIBLE);
            holder.txtViewCount.setVisibility(View.GONE); // Xóa dòng view đi như bạn yêu cầu

            // Lấy dữ liệu trực tiếp từ Room Database (Không cần mạng)
            Executors.newSingleThreadExecutor().execute(() -> {
                // Giả sử OfflineDao của bạn có hàm getBookById, nếu chưa có bạn phải thêm vào nhé
                LocalBook localBook = AppDatabase.getInstance(context).offlineDao().getBookById(bId);

                if (localBook != null) {
                    // Đẩy dữ liệu lên giao diện (Bắt buộc dùng Handler vì đang ở luồng ngầm)
                    new Handler(Looper.getMainLooper()).post(() -> {
                        holder.txtTitle.setText(localBook.title);
                        holder.txtAuthor.setText("Tác giả: " + localBook.author);
                        holder.txtGenre.setText("Thể loại: " + localBook.categoryNames);
                        holder.txtChapterCount.setText("Số chương: " + localBook.totalChapters);

                        Glide.with(context).load(localBook.coverUrl).into(holder.imgCover);
                    });
                }
            });

        } else {

            // 2. GIAO DIỆN TAB LỊCH SỬ / YÊU THÍCH (ONLINE)
            holder.txtAuthor.setVisibility(View.GONE);
            holder.txtGenre.setVisibility(View.GONE);
            holder.txtViewCount.setVisibility(View.VISIBLE);

            // Tải thông tin từ Firebase
            FirebaseFirestore.getInstance().collection("Books").document(bId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        Book detailBook = documentSnapshot.toObject(Book.class);
                        if (detailBook != null) {
                            holder.txtTitle.setText(detailBook.getTitle());
                            Glide.with(context).load(detailBook.getCoverUrl()).into(holder.imgCover);
                        }
                    });

            // Tải số chương vừa đọc
            if (cId != null && !cId.isEmpty()) {
                FirebaseFirestore.getInstance()
                        .collection("Books").document(bId)
                        .collection("Chapters").document(cId)
                        .get()
                        .addOnSuccessListener(chapterDoc -> {
                            if (chapterDoc.exists()) {
                                Long chapterNum = chapterDoc.getLong("chapterNumber");
                                if (chapterNum != null) {
                                    holder.txtChapterCount.setText("Chương vừa đọc: Chương " + chapterNum);
                                } else {
                                    holder.txtChapterCount.setText("Chương vừa đọc: " + cId);
                                }
                            }
                        });
            } else {
                holder.txtChapterCount.setText("Chưa bắt đầu đọc");
            }
        }
    }

    @Override
    public int getItemCount() {
        return libraryItemList != null ? libraryItemList.size() : 0;
    }

    public static class LibraryViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover;
        TextView txtTitle, txtAuthor, txtGenre, txtChapterCount, txtViewCount;

        public LibraryViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.img_book_cover);
            txtTitle = itemView.findViewById(R.id.txt_book_title);

            // Ánh xạ thêm 2 TextView mới
            txtAuthor = itemView.findViewById(R.id.txt_author);
            txtGenre = itemView.findViewById(R.id.txt_genre);

            txtChapterCount = itemView.findViewById(R.id.txt_chapter_count);
            txtViewCount = itemView.findViewById(R.id.txt_view_count);
        }
    }
}