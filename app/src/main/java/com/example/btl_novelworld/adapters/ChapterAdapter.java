package com.example.btl_novelworld.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btl_novelworld.R;
import com.example.btl_novelworld.ReaderActivity;
import com.example.btl_novelworld.models.Chapter;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.ViewHolder> {

    private final Context context;
    private final String bookId;
    private final List<Chapter> chapters = new ArrayList<>();
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public ChapterAdapter(Context context, String bookId) {
        this.context = context;
        this.bookId = bookId;
    }

    public void submitList(List<Chapter> list) {
        chapters.clear();
        if (list != null) {
            chapters.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chapter, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Chapter chapter = chapters.get(position);

        holder.txtChapterTitle.setText(
                "Chương " + chapter.getChapterNumber() + ": " + safe(chapter.getChapterTitle())
        );

        if (chapter.getPublishedDate() != null) {
            holder.txtChapterDate.setText(
                    dateFormat.format(chapter.getPublishedDate().toDate())
            );
        } else {
            holder.txtChapterDate.setText("");
        }

        holder.itemView.setOnClickListener(v -> {
            FirebaseFirestore.getInstance().collection("Books").document(bookId)
                    .update("viewsCount", FieldValue.increment(1));
            Intent intent = new Intent(context, ReaderActivity.class);
            intent.putExtra("bookId", bookId);
            intent.putExtra("chapterId", chapter.getChapterId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return chapters.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtChapterTitle;
        TextView txtChapterDate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtChapterTitle = itemView.findViewById(R.id.txt_chapter_title);
            txtChapterDate = itemView.findViewById(R.id.txt_chapter_date);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}