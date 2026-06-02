package thick2.edu.nguyengiakhanh.lingoenglish;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import thick2.edu.nguyengiakhanh.lingoenglish.models.Lesson;
import thick2.edu.nguyengiakhanh.lingoenglish.models.Question;

public class ListeningFragment extends Fragment {

    private boolean isTranscriptVisible = false;
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;

    // Khai báo Handler và SeekBar để cập nhật thời gian
    private Handler handler = new Handler();
    private Runnable updateSeekBarRunnable;
    private SeekBar seekBarAudio;
    private TextView tvCurrentTime, tvTotalTime;

    // Khai báo Firestore & Model
    private FirebaseFirestore db;
    private Lesson currentLesson;

    // Các biến quản lý bài tập trắc nghiệm
    private int currentQuestionIndex = 0;
    private int score = 0;
    private List<Question> questionList = new ArrayList<>();

    // Các View UI thành phần
    private TextView tvLessonTitle, tvTranscript, tvQuestionText;
    private RadioGroup radioGroupAnswers;
    private RadioButton rbOptionA, rbOptionB, rbOptionC, rbOptionD;
    private Button btnSubmit;
    private MaterialButton btnShowTranscript;
    private CardView cardThumbnail;
    private FloatingActionButton btnPlayPause;
    private ImageView imgThumbnail; // Thêm biến cho hình thu nhỏ

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_listening, container, false);

        // Ánh xạ các UI View từ XML
        ImageView btnBack = view.findViewById(R.id.btnBack);
        btnShowTranscript = view.findViewById(R.id.btnShowTranscript);
        tvTranscript = view.findViewById(R.id.tvTranscript);
        tvLessonTitle = view.findViewById(R.id.tvLessonTitle);
        cardThumbnail = view.findViewById(R.id.cardThumbnail);
        btnPlayPause = view.findViewById(R.id.btnPlayPause);
        btnSubmit = view.findViewById(R.id.btnSubmitQuiz);
        radioGroupAnswers = view.findViewById(R.id.radioGroupAnswers);

        // Ánh xạ 2 nút tua và hình ảnh
        ImageView btnRewind = view.findViewById(R.id.btnRewind);
        ImageView btnForward = view.findViewById(R.id.btnForward);
        imgThumbnail = view.findViewById(R.id.imgThumbnail);

        // Ánh xạ các View của SeekBar
        seekBarAudio = view.findViewById(R.id.seekBarAudio);
        tvCurrentTime = view.findViewById(R.id.tvCurrentTime);
        tvTotalTime = view.findViewById(R.id.tvTotalTime);

        // Ánh xạ các nút lựa chọn câu hỏi trắc nghiệm
        tvQuestionText = view.findViewById(R.id.tvQuestionText);
        rbOptionA = view.findViewById(R.id.rbA);
        rbOptionB = view.findViewById(R.id.rbB);
        rbOptionC = view.findViewById(R.id.rbC);
        rbOptionD = view.findViewById(R.id.rbD);

        // Khởi tạo Firestore Database
        db = FirebaseFirestore.getInstance();

        // Nhận dữ liệu ID chủ đề từ màn hình Chọn Kỹ Năng truyền sang (dùng Bundle)
        String topicId = "topic_education"; // Mặc định để test nếu Bundle rỗng
        if (getArguments() != null && getArguments().containsKey("TOPIC_ID")) {
            topicId = getArguments().getString("TOPIC_ID");
        }

        // ==========================================
        // TỰ ĐỘNG THAY ĐỔI HÌNH ẢNH THEO CHỦ ĐỀ
        // ==========================================
        if (topicId.equals("topic_tech")) {
            imgThumbnail.setImageResource(R.drawable.tech_img); // Bạn có thể thay "ic_tech" bằng file ảnh tĩnh nếu muốn
        } else if (topicId.equals("topic_travel")) {
            imgThumbnail.setImageResource(R.drawable.travel_img);
        } else if (topicId.equals("topic_jobs")) {
            imgThumbnail.setImageResource(R.drawable.jobs_img);
        } else {
            imgThumbnail.setImageResource(R.drawable.education_img);
        }

        // Gọi Firestore lấy đúng bài học tương ứng với chủ đề người dùng đã chọn
        loadLessonFromFirestore(topicId);

        // Xử lý sự kiện quay lại trang trước
        btnBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        // Xử lý nút tua lùi 10 giây
        btnRewind.setOnClickListener(v -> {
            if (mediaPlayer != null) {
                int currentPosition = mediaPlayer.getCurrentPosition();
                // Lùi 10000ms (10s), Math.max đảm bảo không bị lùi quá số 0 (tránh lỗi app)
                int seekPosition = Math.max(currentPosition - 10000, 0);
                mediaPlayer.seekTo(seekPosition);
                seekBarAudio.setProgress(seekPosition);
                tvCurrentTime.setText(formatTime(seekPosition));
            }
        });

        // Xử lý nút tua tới 10 giây
        btnForward.setOnClickListener(v -> {
            if (mediaPlayer != null) {
                int currentPosition = mediaPlayer.getCurrentPosition();
                int duration = mediaPlayer.getDuration();
                // Tới 10000ms (10s), Math.min đảm bảo không bị vượt quá độ dài bài nghe
                int seekPosition = Math.min(currentPosition + 10000, duration);
                mediaPlayer.seekTo(seekPosition);
                seekBarAudio.setProgress(seekPosition);
                tvCurrentTime.setText(formatTime(seekPosition));
            }
        });

        // Xử lý sự kiện bật/tắt phụ đề bài đọc
        btnShowTranscript.setOnClickListener(v -> {
            if (isTranscriptVisible) {
                tvTranscript.setVisibility(View.GONE);
                cardThumbnail.setVisibility(View.VISIBLE);
                btnShowTranscript.setText("Show");
                isTranscriptVisible = false;
            } else {
                tvTranscript.setVisibility(View.VISIBLE);
                cardThumbnail.setVisibility(View.GONE);
                btnShowTranscript.setText("Hide");
                isTranscriptVisible = true;
            }
        });

        // Xử lý phát / tạm ngưng Audio bằng MediaPlayer cục bộ
        btnPlayPause.setOnClickListener(v -> {
            if (currentLesson == null) {
                Toast.makeText(getContext(), "Loading...", Toast.LENGTH_SHORT).show();
                return;
            }

            String fileName = currentLesson.getAudioFileName();
            int resID = getResources().getIdentifier(fileName, "raw", requireContext().getPackageName());

            if (resID != 0) {
                if (mediaPlayer == null) {
                    mediaPlayer = MediaPlayer.create(getContext(), resID);

                    // Cài đặt tổng thời gian cho SeekBar
                    seekBarAudio.setMax(mediaPlayer.getDuration());
                    tvTotalTime.setText(formatTime(mediaPlayer.getDuration()));

                    // ==========================================
                    // SỰ KIỆN KHI AUDIO HÁT XONG (RESET VỀ 0)
                    // ==========================================
                    mediaPlayer.setOnCompletionListener(mp -> {
                        mp.seekTo(0); // Đưa bài nhạc về giây thứ 0
                        seekBarAudio.setProgress(0); // Kéo thanh tua về 0
                        tvCurrentTime.setText(formatTime(0)); // Đổi text thời gian về 00:00
                        btnPlayPause.setImageResource(android.R.drawable.ic_media_play); // Đổi icon thành nút Play
                        isPlaying = false;
                        handler.removeCallbacks(updateSeekBarRunnable); // Dừng handler chạy ngầm
                    });

                    // Lắng nghe sự kiện người dùng cầm tay kéo thả thanh tua nhạc
                    seekBarAudio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            if (fromUser && mediaPlayer != null) {
                                mediaPlayer.seekTo(progress);
                                tvCurrentTime.setText(formatTime(progress));
                            }
                        }
                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {}
                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {}
                    });

                    // Định nghĩa tiến trình chạy ngầm cập nhật thanh kéo mỗi 1 giây
                    updateSeekBarRunnable = new Runnable() {
                        @Override
                        public void run() {
                            if (mediaPlayer != null && isPlaying) {
                                seekBarAudio.setProgress(mediaPlayer.getCurrentPosition());
                                tvCurrentTime.setText(formatTime(mediaPlayer.getCurrentPosition()));
                            }
                            handler.postDelayed(this, 1000);
                        }
                    };
                }

                if (!isPlaying) {
                    mediaPlayer.start();
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                    isPlaying = true;
                    handler.postDelayed(updateSeekBarRunnable, 0); // Kích hoạt cho thanh trượt chạy
                } else {
                    mediaPlayer.pause();
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                    isPlaying = false;
                    handler.removeCallbacks(updateSeekBarRunnable); // Dừng thanh trượt để đỡ tốn pin
                }
            } else {
                Toast.makeText(getContext(), "Không tìm thấy file audio: " + fileName, Toast.LENGTH_SHORT).show();
            }
        });

        // Xử lý sự kiện bấm nút Nộp bài / Câu tiếp theo
        btnSubmit.setOnClickListener(v -> {
            checkAndSubmitAnswer();
        });

        return view;
    }

    // Hàm gọi Firestore để lấy dữ liệu bài nghe
    private void loadLessonFromFirestore(String documentId) {
        db.collection("listening_lessons").document(documentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentLesson = documentSnapshot.toObject(Lesson.class);

                        if (currentLesson != null) {
                            tvLessonTitle.setText(currentLesson.getTitle());

                            // Chỉ hiển thị phụ đề tĩnh, đã gỡ bỏ hoàn toàn tính năng AI
                            tvTranscript.setText(currentLesson.getTranscript());

                            if (currentLesson.getQuestions() != null && !currentLesson.getQuestions().isEmpty()) {
                                questionList = currentLesson.getQuestions();
                                // XÁO TRỘN CÂU HỎI TRƯỚC KHI HIỂN THỊ
                                Collections.shuffle(questionList);
                                currentQuestionIndex = 0;
                                score = 0;
                                displayQuestion(currentQuestionIndex);
                            }

                            Toast.makeText(getContext(), "Done!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Không tìm thấy bài học!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseError", "Lỗi tải dữ liệu Firestore", e);
                    Toast.makeText(getContext(), "Lỗi kết nối mạng!", Toast.LENGTH_SHORT).show();
                });
    }

    // Thiết lập hiển thị câu hỏi trắc nghiệm hiện tại lên giao diện
    private void displayQuestion(int index) {
        if (index >= questionList.size()) return;

        Question question = questionList.get(index);

        if (tvQuestionText != null) {
            tvQuestionText.setText("Question " + (index + 1) + ": " + question.getQuestionText());
        }
        if (rbOptionA != null) rbOptionA.setText(question.getOptionA());
        if (rbOptionB != null) rbOptionB.setText(question.getOptionB());
        if (rbOptionC != null) rbOptionC.setText(question.getOptionC());
        if (rbOptionD != null) rbOptionD.setText(question.getOptionD());

        // Reset lại lựa chọn cũ
        radioGroupAnswers.clearCheck();

        // Nếu là câu cuối cùng, đổi chữ nút bấm thành "Nộp bài"
        if (index == questionList.size() - 1) {
            btnSubmit.setText("Submit");
        } else {
            btnSubmit.setText("Next");
        }
    }

    // Kiểm tra câu trả lời của học sinh và chuyển câu
    private void checkAndSubmitAnswer() {
        if (questionList.isEmpty() || currentQuestionIndex >= questionList.size()) return;

        int selectedId = radioGroupAnswers.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(getContext(), "Pls choose answer!", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton selectedRadioButton = getView().findViewById(selectedId);
        String selectedAnswerText = selectedRadioButton.getText().toString();

        Question currentQuestion = questionList.get(currentQuestionIndex);
        String correctAnswer = currentQuestion.getCorrectAnswer(); // Ví dụ: "C"
        String correctAnswerText = "";

        // Ánh xạ ký tự đáp án đúng sang nội dung text tương ứng để đối chiếu
        if (correctAnswer.equalsIgnoreCase("A")) correctAnswerText = currentQuestion.getOptionA();
        else if (correctAnswer.equalsIgnoreCase("B")) correctAnswerText = currentQuestion.getOptionB();
        else if (correctAnswer.equalsIgnoreCase("C")) correctAnswerText = currentQuestion.getOptionC();
        else if (correctAnswer.equalsIgnoreCase("D")) correctAnswerText = currentQuestion.getOptionD();

        // Kiểm tra xem đáp án có chính xác không
        if (selectedAnswerText.equals(correctAnswerText)) {
            score++;
            Toast.makeText(getContext(), "Correct!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Incorrect! Correct answer is: " + correctAnswer, Toast.LENGTH_SHORT).show();
        }

        // Chuyển sang câu tiếp theo hoặc kết thúc bài kiểm tra
        if (currentQuestionIndex < questionList.size() - 1) {
            currentQuestionIndex++;
            displayQuestion(currentQuestionIndex);
        } else {
            // Hoàn thành toàn bộ câu hỏi của bài nghe
            showResultDialog();
        }
    }

    // Hiển thị hộp thoại kết quả học tập sử dụng AlertDialog mặc định

    // ĐÃ FIX LOGIC COMBO & FIRESTORE
    private void showResultDialog() {
        Bundle bundle = new Bundle();
        if (getArguments() != null) {
            bundle.putAll(getArguments());
        }

        boolean isComboMode = bundle.getBoolean("IS_COMBO_MODE", false);

        if (isComboMode) {
            // NẾU LÀ COMBO: KHÔNG LƯU VÀO FIRESTORE LÚC NÀY
            // Chỉ đóng gói điểm Nghe và truyền thẳng sang Bài Đọc
            Toast.makeText(getContext(), "Đã xong bài Nghe! Đang chuyển sang bài Đọc...", Toast.LENGTH_LONG).show();
            bundle.putInt("COMBO_SCORE_LISTENING", score);
            bundle.putInt("COMBO_TOTAL_LISTENING", questionList.size());

            if (getView() != null) {
                Navigation.findNavController(getView()).navigate(R.id.readingFragment, bundle);
            }
        } else {
            // NẾU CHỈ LÀM BÀI NGHE LẺ: Lưu Firestore với chữ "Listening"
            saveScoreToFirestore("Listening", score, questionList.size());

            bundle.putInt("SCORE", score);
            bundle.putInt("TOTAL_QUESTIONS", questionList.size());
            bundle.putString("SKILL_NAME", "Nghe (Listening)");

            if (getView() != null) {
                Navigation.findNavController(getView()).navigate(R.id.resultFragment, bundle);
            }
        }
    }

    // Hàm lưu điểm đã được thiết kế lại để nhận các tham số linh hoạt
    private void saveScoreToFirestore(String skillName, int finalScore, int totalQuestions) {
        Instant instant = Instant.ofEpochMilli(System.currentTimeMillis());
        Map<String, Object> scoreData = new HashMap<>();
        scoreData.put("topicId", currentLesson.getTopicId());
        scoreData.put("lessonTitle", currentLesson.getTitle());
        scoreData.put("skillName", skillName); // Lưu tên kỹ năng là Listening
        scoreData.put("correctAnswers", finalScore);
        scoreData.put("totalQuestions", totalQuestions);
        scoreData.put("timestamp", instant);

        db.collection("user_scores")
                .add(scoreData)
                .addOnSuccessListener(documentReference -> Log.d("Firestore", "Lưu điểm Listening thành công"))
                .addOnFailureListener(e -> Log.e("Firestore", "Lưu điểm thất bại", e));
    }

    private String formatTime(int ms) {
        int seconds = (ms / 1000) % 60;
        int minutes = (ms / 1000) / 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (handler != null && updateSeekBarRunnable != null) {
            handler.removeCallbacks(updateSeekBarRunnable);
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}