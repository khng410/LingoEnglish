package thick2.edu.nguyengiakhanh.lingoenglish;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

public class ResultFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_result, container, false);

        TextView tvScoreFraction = view.findViewById(R.id.tvScoreFraction);
        TextView tvScoreFeedback = view.findViewById(R.id.tvScoreFeedback);
        TextView tvXpEarned = view.findViewById(R.id.tvXpEarned);
        Button btnFinish = view.findViewById(R.id.btnFinish);

        // Nhận dữ liệu điểm số từ màn hình trước gửi sang
        if (getArguments() != null) {
            int score = getArguments().getInt("SCORE", 0);
            int total = getArguments().getInt("TOTAL_QUESTIONS", 1);

            // Tính toán tỷ lệ phần trăm đúng
            float percentage = (float) score / total;

            // Hiển thị điểm số (Ví dụ: 8/15)
            tvScoreFraction.setText(score + "/" + total);

            // Logic tính XP thưởng và lời nhận xét dựa trên điểm số
            int xp = 0;
            if (percentage == 1.0f) {
                tvScoreFeedback.setText("Hoàn hảo! Bạn không sai câu nào.");
                xp = 50;
            } else if (percentage >= 0.8f) {
                tvScoreFeedback.setText("Rất tốt! Chỉ một chút nữa là hoàn hảo.");
                xp = 40;
            } else if (percentage >= 0.5f) {
                tvScoreFeedback.setText("Khá tốt, nhưng bạn vẫn cần ôn tập thêm nhé.");
                xp = 20;
            } else {
                tvScoreFeedback.setText("Đừng buồn, hãy nghe lại nhiều lần để quen tai hơn!");
                xp = 10;
            }

            tvXpEarned.setText("+ " + xp + " XP");
        }

        // Xử lý nút Tiếp tục (Quay về Trang chủ)
        btnFinish.setOnClickListener(v -> {
            // Lệnh popBackStack này có tham số đặc biệt để xóa sạch lịch sử các trang trước đó,
            // đưa người dùng về thẳng HomeFragment mà không bị lùi lại màn hình làm bài kiểm tra nữa.
            Navigation.findNavController(v).popBackStack(R.id.navigation_home, false);
        });

        return view;
    }
}