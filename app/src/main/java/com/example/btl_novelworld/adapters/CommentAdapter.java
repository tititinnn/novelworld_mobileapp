package com.example.btl_novelworld.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btl_novelworld.R;
import com.example.btl_novelworld.models.Comment;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> commentList;

    public CommentAdapter(List<Comment> commentList) {
        this.commentList = commentList;
    }

    // Hàm cập nhật dữ liệu mới khi có bình luận mới
    public void setData(List<Comment> newList) {
        this.commentList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Nạp layout item_comment đã tạo ở bước trước
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);

        if (comment == null) return;

        holder.txtUserName.setText(comment.getUserName() != null ? comment.getUserName() : "Người dùng ẩn danh");
        holder.txtContent.setText(comment.getContent());

        // Định dạng ngày giờ hiển thị: ví dụ 15:30 18/04/2024
        if (comment.getTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
            String dateStr = sdf.format(comment.getTimestamp().toDate());
            holder.txtTime.setText(dateStr);
        } else {
            holder.txtTime.setText("Vừa xong");
        }
    }

    @Override
    public int getItemCount() {
        return commentList != null ? commentList.size() : 0;
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView txtUserName, txtContent, txtTime;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            txtUserName = itemView.findViewById(R.id.txt_comment_user);
            txtContent = itemView.findViewById(R.id.txt_comment_content);
            txtTime = itemView.findViewById(R.id.txt_comment_time);
        }
    }
}