package com.example.btl_novelworld;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends AppCompatActivity {

    // Khai báo View
    private ImageView btnBack, img_avatar;
    private TextView txt_username, txt_current_language;
    private LinearLayout itemChangePassword, itemChangeAvatar, itemDarkMode, itemNotification;
    private LinearLayout itemLanguage, itemDeleteAccount, itemSave;
    private Switch switchDarkMode, switchNotification;
    private LinearLayout navHome, navExplore, navLibrary, navProfile;

    // Firebase & SharedPreferences
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private SharedPreferences sharedPreferences;

    // Xử lý kết quả trả về khi chụp ảnh hoặc chọn ảnh
    private ActivityResultLauncher<Void> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        sharedPreferences = getSharedPreferences("AppSettings", Context.MODE_PRIVATE);

        initViews();
        loadUserInfo();
        loadSavedSettings();
        setupImagePickers();
        setupClickListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        img_avatar = findViewById(R.id.img_avatar);
        txt_username = findViewById(R.id.txt_username);
        itemChangePassword = findViewById(R.id.itemChangePassword);
        itemChangeAvatar = findViewById(R.id.itemChangeAvatar);
        itemDarkMode = findViewById(R.id.itemDarkMode);
        itemNotification = findViewById(R.id.itemNotification);
        itemLanguage = findViewById(R.id.itemLanguage);
        itemDeleteAccount = findViewById(R.id.itemDeleteAccount);
        itemSave = findViewById(R.id.itemSave);
        switchDarkMode = findViewById(R.id.switchDarkMode);
        switchNotification = findViewById(R.id.switchNotification);
        txt_current_language = findViewById(R.id.txt_current_language);
        navHome = findViewById(R.id.navHome);
        navExplore = findViewById(R.id.navExplore);
        navLibrary = findViewById(R.id.navLibrary);
        navProfile = findViewById(R.id.navProfile);
    }

    private void loadUserInfo() {
        if (currentUser != null) {
            String displayName = currentUser.getDisplayName();
            txt_username.setText((displayName != null && !displayName.isEmpty()) ? displayName : currentUser.getEmail());
        } else {
            txt_username.setText("Chưa đăng nhập");
        }
    }

    private void loadSavedSettings() {
        switchDarkMode.setChecked(sharedPreferences.getBoolean("DARK_MODE", false));
        switchNotification.setChecked(sharedPreferences.getBoolean("NOTIFICATIONS", true));
    }

    // --- CÀI ĐẶT BỘ CHỌN ẢNH ---
    private void setupImagePickers() {
        // Trình xử lý kết quả từ Camera
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicturePreview(),
                bitmap -> {
                    if (bitmap != null) {
                        img_avatar.setImageBitmap(bitmap);
                        Toast.makeText(this, "Đã cập nhật ảnh tạm thời!", Toast.LENGTH_SHORT).show();
                        // TODO: Upload bitmap lên Firebase Storage tại đây
                    }
                }
        );

        // Trình xử lý kết quả từ Thư viện (Gallery)
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        img_avatar.setImageURI(uri);
                        Toast.makeText(this, "Đã cập nhật ảnh tạm thời!", Toast.LENGTH_SHORT).show();
                        // TODO: Upload uri lên Firebase Storage tại đây
                    }
                }
        );
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        // --- ĐỔI MẬT KHẨU TRỰC TIẾP ---
        itemChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        // --- ĐỔI ẢNH ĐẠI DIỆN ---
        itemChangeAvatar.setOnClickListener(v -> showAvatarOptionsDialog());

        // --- CHẾ ĐỘ TỐI ---
        itemDarkMode.setOnClickListener(v -> switchDarkMode.setChecked(!switchDarkMode.isChecked()));
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("DARK_MODE", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });

        // --- THÔNG BÁO ---
        itemNotification.setOnClickListener(v -> switchNotification.setChecked(!switchNotification.isChecked()));
        switchNotification.setOnCheckedChangeListener((buttonView, isChecked) ->
                sharedPreferences.edit().putBoolean("NOTIFICATIONS", isChecked).apply());

        // --- XÓA TÀI KHOẢN ---
        itemDeleteAccount.setOnClickListener(v -> {
            if (currentUser != null) {
                new AlertDialog.Builder(this)
                        .setTitle("Cảnh báo")
                        .setMessage("Hành động này sẽ xóa vĩnh viễn tài khoản. Tiếp tục?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            currentUser.delete().addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(this, "Đã xóa tài khoản", Toast.LENGTH_SHORT).show();
                                    // Chuyển về màn hình đăng nhập...
                                    finish();
                                }
                            });
                        }).setNegativeButton("Hủy", null).show();
            }
        });

        itemSave.setOnClickListener(v -> {
            Toast.makeText(this, "Đã lưu!", Toast.LENGTH_SHORT).show();
            finish();
        });

        // XỬ LÝ SỰ KIỆN CHO BOTTOM NAVIGATION
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, HomeActivity.class);
            // Cờ này giúp xóa các Activity cũ trùng lặp trên ngăn xếp (Back Stack)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(0, 0); // Tắt animation chuyển cảnh để mượt như app thật
            finish(); // Đóng SettingsActivity
        });

        navExplore.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, ExploreActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        });

        navLibrary.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, LibraryActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        });

        navProfile.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, ProfileActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        });
    }

    // ================== LOGIC ĐỔI ẢNH ĐẠI DIỆN ==================
    private void showAvatarOptionsDialog() {
        String[] options = {"Chụp ảnh mới", "Chọn từ thư viện ảnh"};
        new AlertDialog.Builder(this)
                .setTitle("Cập nhật ảnh đại diện")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Mở Camera
                        cameraLauncher.launch(null);
                    } else {
                        // Mở Thư viện (chỉ lấy hình ảnh)
                        galleryLauncher.launch("image/*");
                    }
                })
                .show();
    }

    // ================== LOGIC ĐỔI MẬT KHẨU ==================
    private void showChangePasswordDialog() {
        if (currentUser == null || currentUser.getEmail() == null) return;

        // Tạo giao diện cho Dialog
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 20);

        EditText edtOldPass = new EditText(this);
        edtOldPass.setHint("Nhập mật khẩu hiện tại");
        edtOldPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(edtOldPass);

        // Khoảng cách giữa các ô nhập liệu cho đẹp
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 16, 0, 0);

        EditText edtNewPass = new EditText(this);
        edtNewPass.setHint("Nhập mật khẩu mới");
        edtNewPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        edtNewPass.setLayoutParams(params);
        layout.addView(edtNewPass);

        // Thêm trường Xác nhận mật khẩu mới
        EditText edtConfirmNewPass = new EditText(this);
        edtConfirmNewPass.setHint("Xác nhận mật khẩu mới");
        edtConfirmNewPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        edtConfirmNewPass.setLayoutParams(params);
        layout.addView(edtConfirmNewPass);

        new AlertDialog.Builder(this)
                .setTitle("Đổi mật khẩu")
                .setView(layout)
                .setPositiveButton("Cập nhật", (dialog, which) -> {
                    String oldPass = edtOldPass.getText().toString().trim();
                    String newPass = edtNewPass.getText().toString().trim();
                    String confirmNewPass = edtConfirmNewPass.getText().toString().trim();

                    // Kiểm tra rỗng
                    if (oldPass.isEmpty() || newPass.isEmpty() || confirmNewPass.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Kiểm tra độ dài mật khẩu mới
                    if (newPass.length() < 6) {
                        Toast.makeText(this, "Mật khẩu mới phải từ 6 ký tự", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Kiểm tra mật khẩu xác nhận có khớp không
                    if (!newPass.equals(confirmNewPass)) {
                        Toast.makeText(this, "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Nếu mọi thứ hợp lệ, tiến hành đổi mật khẩu
                    updatePasswordInFirebase(oldPass, newPass);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updatePasswordInFirebase(String oldPass, String newPass) {
        // 1. Xác thực lại người dùng bằng mật khẩu cũ
        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), oldPass);

        currentUser.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // 2. Nếu mật khẩu cũ đúng, tiến hành cập nhật mật khẩu mới
                currentUser.updatePassword(newPass).addOnCompleteListener(updateTask -> {
                    if (updateTask.isSuccessful()) {
                        Toast.makeText(this, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Lỗi: " + updateTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Toast.makeText(this, "Mật khẩu hiện tại không chính xác!", Toast.LENGTH_SHORT).show();
            }
        });
    }

}