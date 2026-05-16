package thick2.edu.nguyengiakhanh.lingoenglish;

// BƯỚC 1: Tạo một Activity mới tên là SplashActivity và copy code này vào

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ẩn thanh tiêu đề trên cùng cho đẹp
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_splash);

        // Tạo delay 5 giây (5000 milliseconds) rồi chuyển sang MainActivity (Màn hình chính)
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // TODO: Sau này có Firebase, chỗ này sẽ check xem User đã đăng nhập chưa
                // Nếu chưa -> chuyển sang LoginActivity. Nếu rồi -> chuyển sang MainActivity.
                // Tạm thời giờ cứ chuyển thẳng sang MainActivity
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // Đóng SplashActivity để người dùng không bấm nút Back quay lại được
            }
        }, 5000);
    }
}
