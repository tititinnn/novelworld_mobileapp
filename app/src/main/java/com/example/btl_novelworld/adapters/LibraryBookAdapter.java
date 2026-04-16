package com.example.btl_novelworld.adapters;

import android.content.Context;
import android.content.Intent;
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
import com.example.btl_novelworld.models.Book;
import com.example.btl_novelworld.models.LibraryItem;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

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

        // --- 1. CLICK VÀO TRUYỆN -> SANG TRANG CHI TIẾT ---
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, BookDetailActivity.class);
            intent.putExtra("bookId", bId);
            context.startActivity(intent);
        });

        // --- 2. LẤY THÔNG TIN SÁCH (ẢNH + TÊN) ---
        FirebaseFirestore.getInstance().collection("Books").document(bId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Book detailBook = documentSnapshot.toObject(Book.class);
                    if (detailBook != null) {
                        holder.txtTitle.setText(detailBook.getTitle());
                        Glide.with(context).load(detailBook.getCoverUrl()).into(holder.imgCover);
                    }
                });

        // --- 3. LẤY SỐ CHƯƠNG ---
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
                                // Nếu muốn hiện cả tên chương (Váy đỏ 1) thì dùng dòng dưới:
                                // String title = chapterDoc.getString("chapterTitle");
                                // holder.txtChapterCount.setText("Chương vừa đọc: " + title);

                                holder.txtChapterCount.setText("Chương vừa đọc: " + cId);
                            }
                        }
                    });
        } else {
            holder.txtChapterCount.setText("Chưa bắt đầu đọc");
        }
    }

    @Override
    public int getItemCount() {
        return libraryItemList != null ? libraryItemList.size() : 0;
    }

    public static class LibraryViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover;
        TextView txtTitle, txtChapterCount, txtViewCount;

        public LibraryViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.img_book_cover);
            txtTitle = itemView.findViewById(R.id.txt_book_title);
            txtChapterCount = itemView.findViewById(R.id.txt_chapter_count);
            txtViewCount = itemView.findViewById(R.id.txt_view_count);
        }
    }
}