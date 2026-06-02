package thick2.edu.nguyengiakhanh.lingoenglish;

import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.firebase.firestore.FirebaseFirestore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import thick2.edu.nguyengiakhanh.lingoenglish.models.Lesson;
import thick2.edu.nguyengiakhanh.lingoenglish.models.Question;

public class ReadingFragment extends Fragment {

    private TextView tvReadingContent, tvLessonTitle, tvQuestionText;
    private ImageView btnZoomIn, btnZoomOut, btnBack;

    // UI Trắc nghiệm
    private RadioGroup radioGroupAnswers;
    private RadioButton rbOptionA, rbOptionB, rbOptionC, rbOptionD;
    private Button btnSubmit;

    // Biến lưu kích thước chữ hiện tại (Mặc định 18sp)
    private float currentTextSize = 18f;
    private final float MAX_TEXT_SIZE = 30f;
    private final float MIN_TEXT_SIZE = 14f;

    // Từ điển Mini Hardcode
    private Map<String, String> miniDictionary;

    // Firebase & Logic Trắc nghiệm
    private FirebaseFirestore db;
    private Lesson currentLesson;
    private int currentQuestionIndex = 0;
    private int score = 0;
    private List<Question> questionList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reading, container, false);

        // Ánh xạ UI
        tvReadingContent = view.findViewById(R.id.tvReadingContent);
        tvLessonTitle = view.findViewById(R.id.tvLessonTitle);
        btnZoomIn = view.findViewById(R.id.btnZoomIn);
        btnZoomOut = view.findViewById(R.id.btnZoomOut);
        btnBack = view.findViewById(R.id.btnBack);

        tvQuestionText = view.findViewById(R.id.tvQuestionText);
        radioGroupAnswers = view.findViewById(R.id.radioGroupAnswers);
        rbOptionA = view.findViewById(R.id.rbA);
        rbOptionB = view.findViewById(R.id.rbB);
        rbOptionC = view.findViewById(R.id.rbC);
        rbOptionD = view.findViewById(R.id.rbD);
        btnSubmit = view.findViewById(R.id.btnSubmitQuiz);

        // Khởi tạo Firestore Database
        db = FirebaseFirestore.getInstance();

        // Nhận dữ liệu ID chủ đề từ màn hình Chọn Kỹ Năng truyền sang
        String topicId = "topic_education"; // Mặc định để test
        if (getArguments() != null && getArguments().containsKey("TOPIC_ID")) {
            topicId = getArguments().getString("TOPIC_ID");
        }

        btnBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        // 1. XỬ LÝ CHỨC NĂNG PHÓNG TO / THU NHỎ
        btnZoomIn.setOnClickListener(v -> {
            if (currentTextSize < MAX_TEXT_SIZE) {
                currentTextSize += 2f;
                tvReadingContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentTextSize);
            } else {
                Toast.makeText(getContext(), "Max size!", Toast.LENGTH_SHORT).show();
            }
        });

        btnZoomOut.setOnClickListener(v -> {
            if (currentTextSize > MIN_TEXT_SIZE) {
                currentTextSize -= 2f;
                tvReadingContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentTextSize);
            } else {
                Toast.makeText(getContext(), "Min size!", Toast.LENGTH_SHORT).show();
            }
        });

        // 2. KHỞI TẠO TỪ ĐIỂN MINI VÀ LOAD DỮ LIỆU TỪ FIREBASE
        setupMiniDictionary();
        loadLessonFromFirestore(topicId);

        // 3. XỬ LÝ NÚT NỘP BÀI TRẮC NGHIỆM
        btnSubmit.setOnClickListener(v -> {
            checkAndSubmitAnswer();
        });

        return view;
    }

    // Nạp sẵn một số từ vựng khó và nghĩa của chúng
    private void setupMiniDictionary() {
        miniDictionary = new HashMap<>();
        miniDictionary.put("algorithm", "Thuật toán");
        miniDictionary.put("automation", "Tự động hóa");
        miniDictionary.put("privacy", "Quyền riêng tư");
        miniDictionary.put("sustainable", "Bền vững");
        miniDictionary.put("footprint", "Dấu chân (khí thải)");
        miniDictionary.put("preserve", "Bảo tồn");
        miniDictionary.put("flexibility", "Sự linh hoạt");
        miniDictionary.put("adaptability", "Khả năng thích nghi");
        miniDictionary.put("blended", "Pha trộn, kết hợp (VD: Blended learning = Học tập kết hợp)");
        miniDictionary.put("transformed", "Biến đổi, thay đổi hoàn toàn");
        miniDictionary.put("traditional", "Truyền thống");
        miniDictionary.put("enhance", "Cải thiện, nâng cao");
        miniDictionary.put("inequality", "Sự bất bình đẳng");
        miniDictionary.put("scarce", "Khan hiếm, ít ỏi");
        miniDictionary.put("accessible", "Có thể truy cập, tiếp cận được");
        miniDictionary.put("motivation", "Động lực");
    }

    // Hàm gọi Firestore để lấy dữ liệu bài đọc (Sử dụng chung Class Lesson)
    private void loadLessonFromFirestore(String documentId) {
        // Lưu ý: Đổi tên Collection thành reading_lessons trên Firebase
        db.collection("reading_lessons").document(documentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentLesson = documentSnapshot.toObject(Lesson.class);

                        if (currentLesson != null) {
                            tvLessonTitle.setText(currentLesson.getTitle());

                            // Sử dụng trường transcript trong Model Lesson để chứa văn bản bài Đọc
                            String articleContent = currentLesson.getTranscript();
                            if (articleContent != null) {
                                applyDictionaryToText(articleContent);
                            }

                            // Load danh sách câu hỏi trắc nghiệm
                            if (currentLesson.getQuestions() != null && !currentLesson.getQuestions().isEmpty()) {
                                questionList = currentLesson.getQuestions();
                                currentQuestionIndex = 0;
                                score = 0;
                                displayQuestion(currentQuestionIndex);
                            }

                            Toast.makeText(getContext(), "Loading success!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Fail to load data!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseError", "Fail to load data on Firebase", e);
                    Toast.makeText(getContext(), "Connection error", Toast.LENGTH_SHORT).show();
                });
    }

    // Hàm duyệt đoạn văn, tìm từ khó, bôi xanh và gắn sự kiện Click
    private void applyDictionaryToText(String text) {
        SpannableString spannableString = new SpannableString(text);

        for (Map.Entry<String, String> entry : miniDictionary.entrySet()) {
            String wordToFind = entry.getKey();
            String meaning = entry.getValue();

            String lowerText = text.toLowerCase();
            int startIndex = lowerText.indexOf(wordToFind.toLowerCase());

            // Tìm và bôi xanh tất cả các từ trùng khớp trong bài
            while (startIndex >= 0) {
                int endIndex = startIndex + wordToFind.length();

                ClickableSpan clickableSpan = new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                        Toast.makeText(getContext(), wordToFind.toUpperCase() + ": " + meaning, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                        super.updateDrawState(ds);
                        ds.setColor(Color.parseColor("#29B6F6")); // Xanh dương
                        ds.setUnderlineText(true);
                        ds.setFakeBoldText(true);
                    }
                };

                spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                startIndex = lowerText.indexOf(wordToFind.toLowerCase(), endIndex);
            }
        }

        tvReadingContent.setMovementMethod(LinkMovementMethod.getInstance());
        tvReadingContent.setText(spannableString);
    }

    // =================================================================
    // LOGIC TRẮC NGHIỆM (Tương tự phần Listening)
    // =================================================================

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

        radioGroupAnswers.clearCheck();

        if (index == questionList.size() - 1) {
            btnSubmit.setText("Submit");
        } else {
            btnSubmit.setText("Next");
        }
    }

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
        String correctAnswer = currentQuestion.getCorrectAnswer();
        String correctAnswerText = "";

        if (correctAnswer.equalsIgnoreCase("A")) correctAnswerText = currentQuestion.getOptionA();
        else if (correctAnswer.equalsIgnoreCase("B"))
            correctAnswerText = currentQuestion.getOptionB();
        else if (correctAnswer.equalsIgnoreCase("C"))
            correctAnswerText = currentQuestion.getOptionC();
        else if (correctAnswer.equalsIgnoreCase("D"))
            correctAnswerText = currentQuestion.getOptionD();

        if (selectedAnswerText.equals(correctAnswerText)) {
            score++;
        }

        if (currentQuestionIndex < questionList.size() - 1) {
            currentQuestionIndex++;
            displayQuestion(currentQuestionIndex);
        } else {
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

        int finalScore = score;
        int finalTotal = questionList.size();
        String skillNameDB = "Reading"; // Lưu lên db là Reading
        String skillNameUI = "Reading"; // Hiện lên giao diện Corgi

        if (isComboMode) {
            // NẾU LÀ COMBO: Cộng dồn điểm bài Nghe được truyền sang
            int listeningScore = bundle.getInt("COMBO_SCORE_LISTENING", 0);
            int listeningTotal = bundle.getInt("COMBO_TOTAL_LISTENING", 0);

            finalScore = score + listeningScore;
            finalTotal = questionList.size() + listeningTotal;

            skillNameDB = "Combo";
            skillNameUI = "Combo Listening and Reading";
        }

        // LƯU ĐIỂM LÊN FIRESTORE 1 LẦN DUY NHẤT TẠI ĐÂY
        saveScoreToFirestore(skillNameDB, finalScore, finalTotal);
        // Chuyển dữ liệu lên màn hình Result Corgi
        bundle.putInt("SCORE", finalScore);
        bundle.putInt("TOTAL_QUESTIONS", finalTotal);
        bundle.putString("SKILL_NAME", skillNameUI);

        if (getView() != null) {
            Navigation.findNavController(getView()).navigate(R.id.resultFragment, bundle);
        }
    }

    private void saveScoreToFirestore(String skillName, int finalScore, int totalQuestions) {
        // Lấy thông tin user đang đăng nhập
        com.google.firebase.auth.FirebaseAuth mAuth = com.google.firebase.auth.FirebaseAuth.getInstance();
        com.google.firebase.auth.FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            String uid = currentUser.getUid();

            // Truy vấn lấy tên người dùng từ bảng 'users' trước khi lưu điểm
            db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
                String userName = documentSnapshot.getString("display_name");
                if (userName == null) userName = "Guest";

                Map<String, Object> scoreData = new HashMap<>();
                scoreData.put("userId", uid);              // Đã thêm UID
                scoreData.put("userName", userName);        // Đã thêm Tên người dùng
                scoreData.put("topicId", currentLesson.getTopicId());
                scoreData.put("lessonTitle", currentLesson.getTitle());
                scoreData.put("skillName", skillName); // Sẽ là "Reading" hoặc "Combo"
                scoreData.put("correctAnswers", finalScore);
                scoreData.put("totalQuestions", totalQuestions);
                scoreData.put("timestamp", System.currentTimeMillis());

                db.collection("user_scores")
                        .add(scoreData)
                        .addOnSuccessListener(documentReference -> Log.d("Firestore", "Saved!" + skillName + " thành công"))
                        .addOnFailureListener(e -> Log.e("Firestore", "Failed to save", e));
            });
        }
    }
}