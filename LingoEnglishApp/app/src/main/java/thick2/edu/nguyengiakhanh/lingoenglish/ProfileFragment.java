package thick2.edu.nguyengiakhanh.lingoenglish;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private TextView tvProfileName, tvProfileEmail, tvProfileXp;
    private MaterialButton btnLogout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Ánh xạ UI
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        btnLogout = view.findViewById(R.id.btnLogout);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Tải thông tin người dùng
        loadUserProfile();

        // Xử lý sự kiện Đăng xuất
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(getContext(), "Logged out", Toast.LENGTH_SHORT).show();

            // Chuyển về màn hình Đăng nhập và xóa sạch lịch sử để không ấn Back lại được
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return view;
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            tvProfileEmail.setText(currentUser.getEmail());

            // Truy vấn lấy Tên và XP từ bảng "users" trên Firestore
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("display_name");


                            if (name != null) tvProfileName.setText(name);

                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Profile", "Lỗi tải thông tin user", e);
                        Toast.makeText(getContext(), "Lỗi tải thông tin!", Toast.LENGTH_SHORT).show();
                    });
        } else {
            tvProfileName.setText("Guest");
            tvProfileEmail.setText("Pls log in");
        }
    }
}