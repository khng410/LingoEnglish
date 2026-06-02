package thick2.edu.nguyengiakhanh.lingoenglish;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText edtEmail, edtPassword;
    private Button btnLogin;
    private TextView tvGoToRegister, tvForgotPassword;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        setContentView(R.layout.activity_login);

        // Khởi tạo Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // Nút Đăng Nhập
        btnLogin.setOnClickListener(v -> loginUser());

        // Nút chuyển sang trang Đăng Ký
        tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        // Nút Quên mật khẩu
        tvForgotPassword.setOnClickListener(v -> showResetPasswordDialog());
    }

    private void loginUser() {
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("Pls enter email");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            edtPassword.setError("Pls enter password");
            return;
        }

        Toast.makeText(this, "Loading...", Toast.LENGTH_SHORT).show();

        // Gọi Firebase xác thực
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Successfully logged in!", Toast.LENGTH_SHORT).show();
                        // Chuyển sang MainActivity
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish(); // Đóng Activity này
                    } else {
                        Toast.makeText(LoginActivity.this, "Wrong email or password!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Hiển thị Hộp thoại yêu cầu nhập Email để khôi phục mật khẩu
    private void showResetPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");
        builder.setMessage("Pls enter your email to receive reset link.");

        // Tạo ô nhập liệu (EditText) bằng code Java thay vì XML để tiết kiệm file
        final EditText inputEmail = new EditText(this);
        inputEmail.setHint("Enter your email");
        inputEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        // Canh lề cho ô nhập liệu đẹp hơn
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 20, 60, 0); // Trái, Trên, Phải, Dưới
        layout.addView(inputEmail);
        builder.setView(layout);

        // Nút Gửi
        builder.setPositiveButton("Send Email", (dialog, which) -> {
            String email = inputEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(LoginActivity.this, "Pls enter Email!", Toast.LENGTH_SHORT).show();
            } else {
                sendResetEmail(email);
            }
        });

        // Nút Hủy
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        // Hiển thị Hộp thoại
        builder.show();
    }

    // Gửi yêu cầu lên Firebase
    private void sendResetEmail(String email) {
        Toast.makeText(this, "Loading...", Toast.LENGTH_SHORT).show();

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Email was sent! Please check your inbox.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(LoginActivity.this, "Fail to send email!", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}