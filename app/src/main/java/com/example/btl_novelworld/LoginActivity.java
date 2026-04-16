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

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegisterNow, tvForgotPassword;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        mAuth = FirebaseAuth.getInstance();

        // 1. Xử lý nút Đăng nhập bằng Email/Password
        btnLogin.setOnClickListener(v -> loginUser());

        // 2. Chuyển sang màn hình Đăng ký
        tvRegisterNow.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            // Không nên finish() ở đây để người dùng có thể nhấn Back quay lại trang Login
        });

        // 3. Xử lý nút Quên mật khẩu
        tvForgotPassword.setOnClickListener(v -> {
            // Sau này bạn có thể dùng mAuth.sendPasswordResetEmail(email) ở đây
            Toast.makeText(LoginActivity.this, "Chức năng khôi phục mật khẩu đang cập nhật", Toast.LENGTH_SHORT).show();
        });
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegisterNow = findViewById(R.id.tvRegisterNow);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // Đã xóa bỏ phần ánh xạ btnGoogle
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validate đầu vào
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

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Vui lòng nhập mật khẩu");
            etPassword.requestFocus();
            return;
        }

        // Hiệu ứng loading cho nút bấm để tăng trải nghiệm người dùng
        btnLogin.setEnabled(false);
        btnLogin.setText("Đang đăng nhập...");

        // Gửi request lên Firebase Authentication
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();

                            // Chuyển hướng sang màn hình Trang chủ (MainActivity)
                            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                            startActivity(intent);
                            finish(); // Đã vào trang chủ thì nên đóng trang Login lại
                        }
                    } else {
                        // Thất bại (Sai mật khẩu hoặc email chưa đăng ký)
                        Toast.makeText(LoginActivity.this, "Sai email hoặc mật khẩu!", Toast.LENGTH_LONG).show();
                        resetButton();
                    }
                });
    }

    private void resetButton() {
        btnLogin.setEnabled(true);
        btnLogin.setText("Đăng nhập");
    }

    // Tự động vào app nếu đã đăng nhập từ trước (Lưu phiên làm việc)
    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Nếu muốn tự động đăng nhập, hãy bỏ comment các dòng dưới sau khi có MainActivity
            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        }
    }
}