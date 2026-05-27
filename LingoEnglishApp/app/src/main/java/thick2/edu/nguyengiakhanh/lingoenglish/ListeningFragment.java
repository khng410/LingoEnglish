package thick2.edu.nguyengiakhanh.lingoenglish;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;

import thick2.edu.nguyengiakhanh.lingoenglish.models.Lesson;

public class ListeningFragment extends Fragment {

    private boolean isTranscriptVisible = false;
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;

    // Khai báo Firestore
    private FirebaseFirestore db;
    private Lesson currentLesson; // Lưu dữ liệu bài học hiện tại

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_listening, container, false);

        // Ánh xạ view
        ImageView btnBack = view.findViewById(R.id.btnBack);
        MaterialButton btnShowTranscript = view.findViewById(R.id.btnShowTranscript);
        TextView tvTranscript = view.findViewById(R.id.tvTranscript);
        TextView tvLessonTitle = view.findViewById(R.id.tvLessonTitle);
        CardView cardThumbnail = view.findViewById(R.id.cardThumbnail);
        FloatingActionButton btnPlayPause = view.findViewById(R.id.btnPlayPause);
        Button btnSubmit = view.findViewById(R.id.btnSubmitQuiz);
        RadioGroup radioGroupAnswers = view.findViewById(R.id.radioGroupAnswers);

        // Khởi tạo Firestore
        db = FirebaseFirestore.getInstance();

        // TODO: Mặc định lấy bài "topic_education" để test. Sau này ta sẽ truyền ID này từ màn hình trước sang.
        loadLessonFromFirestore("topic_education", tvLessonTitle, tvTranscript);

        btnBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        btnShowTranscript.setOnClickListener(v -> {
            if (isTranscriptVisible) {
                tvTranscript.setVisibility(View.GONE);
                cardThumbnail.setVisibility(View.VISIBLE);
                btnShowTranscript.setText("Xem phụ đề");
                isTranscriptVisible = false;
            } else {
                tvTranscript.setVisibility(View.VISIBLE);
                cardThumbnail.setVisibility(View.GONE);
                btnShowTranscript.setText("Ẩn phụ đề");
                isTranscriptVisible = true;
            }
        });

        // Xử lý nút Play Audio dựa trên dữ liệu lấy từ Firebase
        btnPlayPause.setOnClickListener(v -> {
            if (currentLesson == null) {
                Toast.makeText(getContext(), "Đang tải dữ liệu, vui lòng đợi...", Toast.LENGTH_SHORT).show();
                return;
            }

            // Lấy tên file audio từ Firestore (ví dụ: "education_1")
            String fileName = currentLesson.getAudioFileName();
            int resID = getResources().getIdentifier(fileName, "raw", requireContext().getPackageName());

            if (resID != 0) {
                if (mediaPlayer == null) {
                    mediaPlayer = MediaPlayer.create(getContext(), resID);
                }

                if (!isPlaying) {
                    mediaPlayer.start();
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                    isPlaying = true;
                } else {
                    mediaPlayer.pause();
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                    isPlaying = false;
                }
            } else {
                Toast.makeText(getContext(), "Không tìm thấy file audio: " + fileName, Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    // Hàm gọi Firebase để lấy dữ liệu
    private void loadLessonFromFirestore(String documentId, TextView tvTitle, TextView tvTranscript) {
        db.collection("listening_lessons").document(documentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Firebase tự động "đổ" dữ liệu vào Class Lesson cực kỳ tiện lợi
                        currentLesson = documentSnapshot.toObject(Lesson.class);

                        if (currentLesson != null) {
                            // Cập nhật giao diện
                            tvTitle.setText(currentLesson.getTitle());
                            tvTranscript.setText(currentLesson.getTranscript());
                            Toast.makeText(getContext(), "Tải dữ liệu thành công!", Toast.LENGTH_SHORT).show();

                            // TODO: Ở bước sau ta sẽ code tiếp phần hiển thị câu hỏi trắc nghiệm ra màn hình
                        }
                    } else {
                        Toast.makeText(getContext(), "Không tìm thấy bài học!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseError", "Lỗi tải dữ liệu", e);
                    Toast.makeText(getContext(), "Lỗi mạng!", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}