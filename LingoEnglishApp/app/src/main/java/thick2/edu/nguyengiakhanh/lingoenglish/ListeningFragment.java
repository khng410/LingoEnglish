package thick2.edu.nguyengiakhanh.lingoenglish;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
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
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.internal.LinkedTreeMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import thick2.edu.nguyengiakhanh.lingoenglish.api.ApiClient;
import thick2.edu.nguyengiakhanh.lingoenglish.api.GeminiApiService;
import thick2.edu.nguyengiakhanh.lingoenglish.models.Lesson;
import thick2.edu.nguyengiakhanh.lingoenglish.models.Question;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

        // Ánh xạ các View của SeekBar
        seekBarAudio = view.findViewById(R.id.seekBarAudio);
        tvCurrentTime = view.findViewById(R.id.tvCurrentTime);
        tvTotalTime = view.findViewById(R.id.tvTotalTime);

        // Ánh xạ các nút lựa chọn câu hỏi trắc nghiệm
        tvQuestionText = view.findViewById(R.id.tvQuestionText);
        rbOptionA = view.findViewById(R.id.rbOptionA);
        rbOptionB = view.findViewById(R.id.rbOptionB);
        rbOptionC = view.findViewById(R.id.rbOptionC);
        rbOptionD = view.findViewById(R.id.rbOptionD);

        // Khởi tạo Firestore Database
        db = FirebaseFirestore.getInstance();

        // Nhận dữ liệu ID chủ đề từ màn hình Chọn Kỹ Năng truyền sang (dùng Bundle)
        String topicId = "topic_education"; // Mặc định để test nếu Bundle rỗng
        if (getArguments() != null && getArguments().containsKey("TOPIC_ID")) {
            topicId = getArguments().getString("TOPIC_ID");
        }

        // Gọi Firestore lấy đúng bài học tương ứng với chủ đề người dùng đã chọn
        loadLessonFromFirestore(topicId);

        // Xử lý sự kiện quay lại trang trước
        btnBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        // Xử lý sự kiện bật/tắt phụ đề bài đọc
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

        // Xử lý phát / tạm ngưng Audio bằng MediaPlayer cục bộ
        btnPlayPause.setOnClickListener(v -> {
            if (currentLesson == null) {
                Toast.makeText(getContext(), "Đang tải dữ liệu, vui lòng đợi...", Toast.LENGTH_SHORT).show();
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

                            // Tạo tính năng Clickable cho từng từ trong phụ đề (Tap-to-Translate)
                            setupTapToTranslate(currentLesson.getTranscript());

                            // Load danh sách câu hỏi trắc nghiệm
                            if (currentLesson.getQuestions() != null && !currentLesson.getQuestions().isEmpty()) {
                                questionList = currentLesson.getQuestions();
                                currentQuestionIndex = 0;
                                score = 0;
                                displayQuestion(currentQuestionIndex);
                            }

                            Toast.makeText(getContext(), "Tải bài học thành công!", Toast.LENGTH_SHORT).show();
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
            tvQuestionText.setText("Câu " + (index + 1) + ": " + question.getQuestionText());
        }
        if (rbOptionA != null) rbOptionA.setText(question.getOptionA());
        if (rbOptionB != null) rbOptionB.setText(question.getOptionB());
        if (rbOptionC != null) rbOptionC.setText(question.getOptionC());
        if (rbOptionD != null) rbOptionD.setText(question.getOptionD());

        // Reset lại lựa chọn cũ
        radioGroupAnswers.clearCheck();

        // Nếu là câu cuối cùng, đổi chữ nút bấm thành "Nộp bài"
        if (index == questionList.size() - 1) {
            btnSubmit.setText("Nộp bài & Hoàn thành");
        } else {
            btnSubmit.setText("Câu tiếp theo");
        }
    }

    // Kiểm tra câu trả lời của học sinh và chuyển câu
    private void checkAndSubmitAnswer() {
        if (questionList.isEmpty() || currentQuestionIndex >= questionList.size()) return;

        int selectedId = radioGroupAnswers.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(getContext(), "Vui lòng chọn đáp án!", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(getContext(), "Chính xác!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Chưa chính xác! Đáp án đúng là: " + correctAnswer, Toast.LENGTH_SHORT).show();
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

    // Hiển thị hộp thoại kết quả học tập
    private void showResultDialog() {
        BottomSheetDialog resultDialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.bottom_sheet_skills, null); // Có thể tái sử dụng hoặc tạo layout mới
        resultDialog.setContentView(dialogView);

        TextView tvTitle = dialogView.findViewById(R.id.tvTitleSheet);
        if (tvTitle != null) {
            tvTitle.setText("Kết Quả Làm Bài");
        }

        TextView tvDesc = dialogView.findViewById(R.id.tvDescSheet);
        if (tvDesc != null) {
            tvDesc.setText("Bạn đã hoàn thành chính xác " + score + "/" + questionList.size() + " câu hỏi bài nghe.");
        }

        Button btnClose = dialogView.findViewById(R.id.btnListen); // Mượn ID tạm thời để đóng
        if (btnClose != null) {
            btnClose.setText("Đóng & Quay lại");
            btnClose.setOnClickListener(v -> {
                resultDialog.dismiss();
                Navigation.findNavController(getView()).popBackStack();
            });
        }

        // Ẩn bớt các nút thừa trong Bottom Sheet mẫu
        View btnSpeak = dialogView.findViewById(R.id.btnSpeak);
        if (btnSpeak != null) btnSpeak.setVisibility(View.GONE);

        resultDialog.show();

        // Lưu điểm số lên Firestore (Lưu trữ lịch sử)
        saveScoreToFirestore();
    }

    // Lưu điểm của người dùng lên Cloud Firestore
    private void saveScoreToFirestore() {
        Map<String, Object> scoreData = new HashMap<>();
        scoreData.put("topicId", currentLesson.getTopicId());
        scoreData.put("lessonTitle", currentLesson.getTitle());
        scoreData.put("correctAnswers", score);
        scoreData.put("totalQuestions", questionList.size());
        scoreData.put("timestamp", System.currentTimeMillis());

        // Lưu vào Collection "user_scores"
        db.collection("user_scores")
                .add(scoreData)
                .addOnSuccessListener(documentReference -> Log.d("Firestore", "Lưu điểm thành công"))
                .addOnFailureListener(e -> Log.e("Firestore", "Lưu điểm thất bại", e));
    }

    // CHỨC NĂNG XỊN: Tách từ vựng trong Transcript thành các liên kết có thể nhấp chuột để dịch thuật
    private void setupTapToTranslate(String text) {
        if (text == null || text.isEmpty()) return;

        SpannableString spannableString = new SpannableString(text);
        String[] words = text.split("\\s+");
        int searchStart = 0;

        for (String word : words) {
            // Loại bỏ các ký tự dấu câu để tra cứu từ vựng chuẩn xác
            final String cleanWord = word.replaceAll("[^a-zA-Z]", "");
            if (cleanWord.isEmpty()) continue;

            int wordIndex = text.indexOf(word, searchStart);
            if (wordIndex != -1) {
                spannableString.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                        translateWordWithGemini(cleanWord);
                    }
                }, wordIndex, wordIndex + word.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                searchStart = wordIndex + word.length();
            }
        }

        tvTranscript.setText(spannableString);
        tvTranscript.setMovementMethod(LinkMovementMethod.getInstance());
    }

    // Gọi Gemini API trực tiếp để giải nghĩa từ vựng học sinh vừa chạm vào
    private void translateWordWithGemini(String word) {
        Toast.makeText(getContext(), "Đang tra từ '" + word + "' bằng Gemini AI...", Toast.LENGTH_SHORT).show();

        GeminiApiService apiService = ApiClient.getClient().create(GeminiApiService.class);

        // Khởi dựng JSON Request đúng chuẩn cấu trúc của Google Gemini API v1beta
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", "You are an English teacher dictionary helper. For the word '" + word + "', provide: " +
                "1. Vietnamese translation " +
                "2. IPA pronunciation " +
                "3. One short simple example sentence. " +
                "Format clearly, keep it short and encouraging for Vietnamese highschoolers.");

        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(textPart);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", parts);

        List<Map<String, Object>> contents = new ArrayList<>();
        contents.add(content);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", contents);

        // Gọi API bất đồng bộ
        apiService.getGeminiResponse(ApiClient.GEMINI_API_KEY, requestBody)
                .enqueue(new Callback<Object>() {
                    @Override
                    public void onResponse(Call<Object> call, Response<Object> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                // Bóc tách phản hồi JSON lồng ghép từ API của Google
                                LinkedTreeMap<?, ?> bodyMap = (LinkedTreeMap<?, ?>) response.body();
                                ArrayList<?> candidates = (ArrayList<?>) bodyMap.get("candidates");
                                LinkedTreeMap<?, ?> firstCandidate = (LinkedTreeMap<?, ?>) candidates.get(0);
                                LinkedTreeMap<?, ?> content = (LinkedTreeMap<?, ?>) firstCandidate.get("content");
                                ArrayList<?> partsList = (ArrayList<?>) content.get("parts");
                                LinkedTreeMap<?, ?> firstPart = (LinkedTreeMap<?, ?>) partsList.get(0);
                                String responseText = (String) firstPart.get("text");

                                // Hiển thị nghĩa của từ lên một Bottom Sheet Dialog cực kỳ hiện đại
                                showTranslationBottomSheet(word, responseText);

                            } catch (Exception e) {
                                Log.e("GeminiError", "Lỗi phân tích JSON", e);
                                Toast.makeText(getContext(), "Không phân tích được nghĩa!", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getContext(), "Lỗi từ Gemini API!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Object> call, Throwable t) {
                        Toast.makeText(getContext(), "Lỗi mạng, kiểm tra kết nối!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Hiển thị kết quả dịch nghĩa từ vựng từ Gemini lên Bottom Sheet
    private void showTranslationBottomSheet(String word, String explanation) {
        BottomSheetDialog translationSheet = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_skills, null);
        translationSheet.setContentView(sheetView);

        TextView tvTitle = sheetView.findViewById(R.id.tvTitleSheet);
        if (tvTitle != null) {
            tvTitle.setText("Từ vựng: " + word);
        }

        TextView tvDesc = sheetView.findViewById(R.id.tvDescSheet);
        if (tvDesc != null) {
            tvDesc.setText(explanation);
        }

        // Cập nhật lại các nút điều hướng của Bottom Sheet mẫu thành nút Đóng
        Button btnClose = sheetView.findViewById(R.id.btnListen);
        if (btnClose != null) {
            btnClose.setText("Đã hiểu");
            btnClose.setOnClickListener(v -> translationSheet.dismiss());
        }

        View btnSpeak = sheetView.findViewById(R.id.btnSpeak);
        if (btnSpeak != null) btnSpeak.setVisibility(View.GONE);

        translationSheet.show();
    }

    // Hàm hỗ trợ định dạng mili-giây thành chuỗi phút:giây (00:00)
    private String formatTime(int ms) {
        int seconds = (ms / 1000) % 60;
        int minutes = (ms / 1000) / 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (handler != null && updateSeekBarRunnable != null) {
            handler.removeCallbacks(updateSeekBarRunnable); // Xóa bộ nhớ chạy ngầm khi thoát trang
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}