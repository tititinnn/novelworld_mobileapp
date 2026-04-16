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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecommendBookAdapter extends RecyclerView.Adapter<RecommendBookAdapter.ViewHolder> {

    private final Context context;
    private final List<Book> books = new ArrayList<>();

    public RecommendBookAdapter(Context context) {
        this.context = context;
    }

    public void submitList(List<Book> list) {
        books.clear();
        if (list != null) {
            books.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_home_recommend_book, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Book book = books.get(position);

        holder.txtBookTitle.setText(book.getTitle());
        holder.txtViewCount.setText(formatCount(book.getViewsCount()));
        holder.txtAuthor.setText("Tác giả: " + safe(book.getAuthor()));
        holder.txtGenre.setText("Thể loại: " + formatCategories(book.getCategories()));
        holder.txtChapterCount.setText("Số chương: " + book.getTotalChapters());

        if (book.getCoverUrl() != null && !book.getCoverUrl().isEmpty()) {
            Glide.with(context)
                    .load(book.getCoverUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.imgBookCover);
        } else {
            holder.imgBookCover.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, BookDetailActivity.class);
            intent.putExtra("bookId", book.getBookId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgBookCover;
        TextView txtBookTitle, txtViewCount, txtAuthor, txtGenre, txtChapterCount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBookCover = itemView.findViewById(R.id.img_book_cover);
            txtBookTitle = itemView.findViewById(R.id.txt_book_title);
            txtViewCount = itemView.findViewById(R.id.txt_view_count);
            txtAuthor = itemView.findViewById(R.id.txt_author);
            txtGenre = itemView.findViewById(R.id.txt_genre);
            txtChapterCount = itemView.findViewById(R.id.txt_chapter_count);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String formatCategories(List<String> categories) {
        if (categories == null || categories.isEmpty()) return "";
        return String.join(", ", categories);
    }

    private String formatCount(long count) {
        if (count >= 1_000_000) {
            return String.format(Locale.getDefault(), "👁 %.1fM", count / 1_000_000f);
        } else if (count >= 1_000) {
            return String.format(Locale.getDefault(), "👁 %.1fK", count / 1_000f);
        }
        return "👁 " + count;
    }
}