package com.example.btl_novelworld;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    // Khai báo thêm etUsername
    private EditText etUsername, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvLoginNow;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Xử lý sự kiện click Đăng ký
        btnRegister.setOnClickListener(v -> registerUser());

        // Chuyển sang màn hình Đăng nhập
        tvLoginNow.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void initViews() {
        // Ánh xạ thêm etUsername từ XML mới
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLoginNow = findViewById(R.id.tvLoginNow);

        // Đã xóa bỏ btnGoogle
    }

    private void registerUser() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // 1. Validate Tên người dùng
        if (TextUtils.isEmpty(username)) {
            etUsername.setError("Vui lòng nhập tên hiển thị");
            etUsername.requestFocus();
            return;
        }

        // 2. Validate Email
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Vui lòng nhập Email");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Định dạng email không hợp lệ");
            etEmail.requestFocus();
            return;
        }

        // 3. Validate Mật khẩu
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etPassword.setError("Mật khẩu phải từ 6 ký tự trở lên");
            etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            etConfirmPassword.requestFocus();
            return;
        }

        // Hiệu ứng chờ cho nút bấm
        btnRegister.setEnabled(false);
        btnRegister.setText("Đang xử lý...");

        // 4. Đăng ký tài khoản trên Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Truyền thêm biến username vào hàm lưu Firestore
                            saveUserDataToFirestore(user.getUid(), email, username);
                        }
                    } else {
                        Toast.makeText(RegisterActivity.this, "Đăng ký thất bại: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        resetButton();
                    }
                });
    }

    private void saveUserDataToFirestore(String userId, String email, String username) {
        // Tạo Map lưu thông tin người dùng
        Map<String, Object> user = new HashMap<>();
        user.put("username", username); // Lưu cái tên người dùng đã chọn
        user.put("email", email);
        user.put("provider", "password");
        user.put("displayName", username); // Dùng làm tên hiển thị trong app
        user.put("avatarUrl", "");
        user.put("createdAt", System.currentTimeMillis());

        // Lưu vào Collection "users" với ID là userId từ Auth
        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                    // Đăng ký xong tự động quay về trang Login
                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this, "Lỗi lưu dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetButton();
                });
    }

    private void resetButton() {
        btnRegister.setEnabled(true);
        btnRegister.setText("Đăng ký");
    }
}