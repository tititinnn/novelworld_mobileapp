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

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {

    private final Context context;
    private final List<Book> books = new ArrayList<>();

    public BookAdapter(Context context) {
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
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_book, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book book = books.get(position);

        holder.txtBookTitle.setText(book.getTitle());
        holder.txtViewCount.setText(formatCount(book.getViewsCount()));

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

    static class BookViewHolder extends RecyclerView.ViewHolder {
        ImageView imgBookCover;
        TextView txtBookTitle;
        TextView txtViewCount;

        BookViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBookCover = itemView.findViewById(R.id.img_book_cover);
            txtBookTitle = itemView.findViewById(R.id.txt_book_title);
            txtViewCount = itemView.findViewById(R.id.txt_view_count);
        }
    }

    private String formatCount(long count) {
        if (count >= 1_000_000) {
            return String.format(Locale.getDefault(), "◉ %.1fM", count / 1_000_000f);
        } else if (count >= 1_000) {
            return String.format(Locale.getDefault(), "◉ %.1fK", count / 1_000f);
        }
        return "◉ " + count;
    }
}