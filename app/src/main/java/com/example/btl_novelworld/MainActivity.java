package com.example.btl_novelworld;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            // Tùy chọn: Kiểm tra xem user đã đăng nhập Firebase từ trước chưa
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            Intent intent;

            if (currentUser != null) {
                // Đã đăng nhập -> Bỏ qua Login, vào thẳng Trang chủ
                intent = new Intent(MainActivity.this, HomeActivity.class);
            } else {
                // Chưa đăng nhập -> Vào trang Đăng nhập
                intent = new Intent(MainActivity.this, LoginActivity.class);
            }

            startActivity(intent);

            // Đóng MainActivity (màn hình loading) lại để khi ấn Back không bị quay ngược về đây
            finish();

        }, 2500); // 2500 mili-giây = 2.5 giây
    }
}