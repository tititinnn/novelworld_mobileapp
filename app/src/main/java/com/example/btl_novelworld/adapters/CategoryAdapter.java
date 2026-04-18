package com.example.btl_novelworld.adapters;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.btl_novelworld.R;
import com.example.btl_novelworld.models.Category;
import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private final List<Category> categories = new ArrayList<>();
    private final OnCategoryClickListener listener;
    private int selectedPosition = 0;

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    public CategoryAdapter(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Category> list) {
        categories.clear();
        if (list != null) categories.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.txtGenre.setText(category.getName());

        // Hiệu ứng chọn: Chữ đậm và màu xanh khi được chọn
        if (selectedPosition == position) {
            holder.txtGenre.setTextColor(Color.parseColor("#2793FF"));
            holder.txtGenre.setTypeface(null, Typeface.BOLD);
        } else {
            holder.txtGenre.setTextColor(Color.parseColor("#666666"));
            holder.txtGenre.setTypeface(null, Typeface.NORMAL);
        }

        holder.itemView.setOnClickListener(v -> {
            int oldPos = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(oldPos);
            notifyItemChanged(selectedPosition);
            listener.onCategoryClick(category);
        });
    }

    @Override
    public int getItemCount() { return categories.size(); }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView txtGenre;
        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            txtGenre = itemView.findViewById(R.id.txt_genre);
        }
    }
}