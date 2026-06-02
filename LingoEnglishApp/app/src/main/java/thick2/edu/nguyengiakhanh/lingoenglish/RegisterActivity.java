package thick2.edu.nguyengiakhanh.lingoenglish;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtName, edtEmailReg, edtPasswordReg;
    private Button btnRegister;
    private TextView tvGoToLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        edtName = findViewById(R.id.edtName);
        edtEmailReg = findViewById(R.id.edtEmailReg);
        edtPasswordReg = findViewById(R.id.edtPasswordReg);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        btnRegister.setOnClickListener(v -> registerUser());

        tvGoToLogin.setOnClickListener(v -> {
            finish(); // Đóng trang Đăng ký để quay lại trang Login
        });
    }

    private void registerUser() {
        String name = edtName.getText().toString().trim();
        String email = edtEmailReg.getText().toString().trim();
        String password = edtPasswordReg.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            edtName.setError("Pls enter display name");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            edtEmailReg.setError("Pls enter email");
            return;
        }
        if (password.length() < 6) {
            edtPasswordReg.setError("The password must be at least 6 characters");
            return;
        }

        Toast.makeText(this, "Loading...", Toast.LENGTH_SHORT).show();

        // 1. Tạo tài khoản trên Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // 2. Nếu thành công, lưu Tên hiển thị vào Database Firestore (Collection: users)
                            saveUserToFirestore(user.getUid(), name, email);
                        }
                    } else {
                        Toast.makeText(RegisterActivity.this, "Failed to register! Try again later.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToFirestore(String uid, String name, String email) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("display_name", name);
        userData.put("email", email);
        userData.put("created_at", System.currentTimeMillis());

        db.collection("users").document(uid)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "Successfully registered!", Toast.LENGTH_SHORT).show();
                    // Chuyển thẳng sang trang chủ MainActivity
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    // Lệnh này xóa hết các trang Login/Register cũ để bấm nút Back không bị quay lại
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firebase", "Error saving user data to Firestore", e);
                    Toast.makeText(RegisterActivity.this, "Error saving data", Toast.LENGTH_SHORT).show();
                });
    }
}