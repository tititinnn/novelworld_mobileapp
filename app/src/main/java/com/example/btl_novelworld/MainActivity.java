package com.example.btl_novelworld;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private TextView tvUserEmail;
    private Button btnLogout;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        tvUserEmail = findViewById(R.id.tvUserEmail);
        btnLogout = findViewById(R.id.btnLogout);

        // Lấy thông tin user hiện tại
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            tvUserEmail.setText("Tài khoản: " + user.getEmail());
        } else {
            // Nếu chưa đăng nhập mà lọt vào đây thì đá về trang Login
            backToLogin();
        }

        // Xử lý Đăng xuất
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut(); // Xóa phiên đăng nhập trên Firebase
            backToLogin();
        });
    }

    private void backToLogin() {
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }
}