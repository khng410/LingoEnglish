package thick2.edu.nguyengiakhanh.lingoenglish;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private TextView tvProfileName, tvProfileEmail;
    private MaterialButton btnLogout, btnChangePassword;
    private FrameLayout frameAvatar;
    private LottieAnimationView lottieDefaultAvatar;
    private CardView cardRealAvatar;
    private ImageView imgRealAvatar;

    // Thêm biến chứa Lịch sử
    private LinearLayout layoutHistoryContainer;
    private TextView tvHistoryLoading;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Bộ lắng nghe kết quả khi người dùng chọn ảnh từ Gallery
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    uploadAndSaveBase64Avatar(imageUri);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        frameAvatar = view.findViewById(R.id.frameAvatar);
        lottieDefaultAvatar = view.findViewById(R.id.lottieDefaultAvatar);
        cardRealAvatar = view.findViewById(R.id.cardRealAvatar);
        imgRealAvatar = view.findViewById(R.id.imgRealAvatar);

        // Ánh xạ Lịch sử
        layoutHistoryContainer = view.findViewById(R.id.layoutHistoryContainer);
        tvHistoryLoading = view.findViewById(R.id.tvHistoryLoading);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadUserProfile();

        // Xử lý nút Đăng xuất
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(getContext(), "Logged out", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // Xử lý nút Đổi Mật Khẩu
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        // Xử lý sự kiện bấm vào khung Avatar để chọn ảnh
        frameAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });

        return view;
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            tvProfileEmail.setText(currentUser.getEmail());

            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("display_name");
                            String base64Avatar = documentSnapshot.getString("avatar_base64");

                            if (name != null) tvProfileName.setText(name);

                            if (base64Avatar != null && !base64Avatar.isEmpty()) {
                                displayBase64Avatar(base64Avatar);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Profile", "Error loading user profile", e);
                        Toast.makeText(getContext(), "Error loading data", Toast.LENGTH_SHORT).show();
                    });

            // Gọi hàm tải lịch sử
            loadRecentHistory(uid);
        }
    }

    // ----------------------------------------------------
    // CHỨC NĂNG MỚI: TẢI 3 LẦN THI GẦN NHẤT
    // ----------------------------------------------------
    private void loadRecentHistory(String uid) {
        // Lấy toàn bộ điểm của user này (Không dùng orderBy để tránh lỗi Missing Index)
        db.collection("user_scores")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    layoutHistoryContainer.removeView(tvHistoryLoading); // Ẩn chữ loading

                    if (queryDocumentSnapshots.isEmpty()) {
                        TextView tvEmpty = new TextView(getContext());
                        tvEmpty.setText("No data yet, pls take a test!");
                        layoutHistoryContainer.addView(tvEmpty);
                        return;
                    }

                    // Chuyển kết quả thành List để sắp xếp bằng Java
                    List<DocumentSnapshot> docs = new ArrayList<>(queryDocumentSnapshots.getDocuments());

                    // Sắp xếp giảm dần theo thời gian (sử dụng hàm an toàn để tránh Crash)
                    Collections.sort(docs, (d1, d2) -> {
                        long t1 = getSafeTimestamp(d1);
                        long t2 = getSafeTimestamp(d2);
                        return Long.compare(t2, t1); // Đảo t2 lên trước t1 để giảm dần
                    });

                    // Chỉ lấy tối đa 3 phần tử
                    int limit = Math.min(docs.size(), 3);
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault());

                    for (int i = 0; i < limit; i++) {
                        DocumentSnapshot doc = docs.get(i);

                        String title = doc.getString("lessonTitle");
                        String skill = doc.getString("skillName");
                        Long correct = doc.getLong("correctAnswers");
                        Long total = doc.getLong("totalQuestions");

                        // Lấy thời gian an toàn
                        long timestamp = getSafeTimestamp(doc);

                        String timeString = (timestamp != 0L) ? sdf.format(new Date(timestamp)) : "Don't know";
                        String scoreString = (correct != null && total != null) ? correct + "/" + total : "0/0";

                        // Nạp giao diện item_recent_history vào
                        View historyItemView = LayoutInflater.from(getContext()).inflate(R.layout.item_recent_history, layoutHistoryContainer, false);

                        TextView tvTitle = historyItemView.findViewById(R.id.tvHistoryTitle);
                        TextView tvTimeSkill = historyItemView.findViewById(R.id.tvHistoryTimeSkill);
                        TextView tvScore = historyItemView.findViewById(R.id.tvHistoryScore);

                        tvTitle.setText(title);
                        tvTimeSkill.setText(timeString + " • " + skill);
                        tvScore.setText(scoreString);

                        layoutHistoryContainer.addView(historyItemView);
                    }
                })
                .addOnFailureListener(e -> {
                    tvHistoryLoading.setText("No data yet");
                });
    }

    // Hàm hỗ trợ "Cứu hộ": Ép kiểu an toàn mọi định dạng dữ liệu trên Firebase thành Number
    private long getSafeTimestamp(DocumentSnapshot doc) {
        Object tsObj = doc.get("timestamp");
        if (tsObj instanceof Number) {
            return ((Number) tsObj).longValue();
        } else if (tsObj instanceof String) {
            try {
                return Long.parseLong((String) tsObj);
            } catch (NumberFormatException e) {
                return 0L; // Bỏ qua nếu chuỗi bị lỗi
            }
        } else if (tsObj instanceof com.google.firebase.Timestamp) {
            return ((com.google.firebase.Timestamp) tsObj).toDate().getTime();
        }
        return 0L;
    }

    // ----------------------------------------------------
    // CHỨC NĂNG 1: ĐỔI MẬT KHẨU BẰNG DIALOG
    // ----------------------------------------------------
    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Change password");

        final EditText inputPass = new EditText(requireContext());
        inputPass.setHint("Change password at least 6 characters");
        inputPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 20, 60, 0);
        layout.addView(inputPass);
        builder.setView(layout);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newPassword = inputPass.getText().toString().trim();
            if (newPassword.length() >= 6) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    Toast.makeText(getContext(), "Updating...", Toast.LENGTH_SHORT).show();
                    user.updatePassword(newPassword).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Done!", Toast.LENGTH_LONG).show();
                        } else {
                            // Lỗi bảo mật: Nếu user đăng nhập quá lâu, Firebase sẽ từ chối đổi pass trực tiếp
                            Toast.makeText(getContext(), "Error", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } else {
                Toast.makeText(getContext(), "Too short!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // ----------------------------------------------------
    // CHỨC NĂNG 2: XỬ LÝ ẢNH BASE64 (KHÔNG DÙNG STORAGE)
    // ----------------------------------------------------
    private void uploadAndSaveBase64Avatar(Uri imageUri) {
        try {
            // Lấy ảnh từ điện thoại
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);

            // QUAN TRỌNG: Nén kích thước ảnh xuống 300x300 để chuỗi Base64 không bị quá tải giới hạn của Firestore
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, true);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // Nén chất lượng ảnh xuống 50%
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            byte[] imageBytes = baos.toByteArray();

            // Chuyển mảng byte thành chuỗi Text
            String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            // Lưu chuỗi Text này lên Firestore
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                Toast.makeText(getContext(), "Loading...", Toast.LENGTH_SHORT).show();
                db.collection("users").document(user.getUid())
                        .update("avatar_base64", base64Image)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Done", Toast.LENGTH_SHORT).show();
                            displayBase64Avatar(base64Image); // Hiển thị ngay lên màn hình
                        })
                        .addOnFailureListener(e -> Toast.makeText(getContext(), "Error!", Toast.LENGTH_SHORT).show());
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error", Toast.LENGTH_SHORT).show();
        }
    }

    // Hàm Dịch chuỗi Text ngược lại thành Hình ảnh để hiển thị
    private void displayBase64Avatar(String base64Image) {
        try {
            byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

            imgRealAvatar.setImageBitmap(decodedByte);

            // Ẩn hoạt ảnh Corgi, hiện ảnh thật lên
            lottieDefaultAvatar.setVisibility(View.GONE);
            cardRealAvatar.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}