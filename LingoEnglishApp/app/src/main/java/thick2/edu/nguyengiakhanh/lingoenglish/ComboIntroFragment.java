package thick2.edu.nguyengiakhanh.lingoenglish;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;

public class ComboIntroFragment extends Fragment {

    private String topicId = "topic_education";
    private String topicName = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_combo_intro, container, false);
        ImageView btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());
        MaterialButton btnStartCombo = view.findViewById(R.id.btnStartCombo);
        TextView tvComboTitle = view.findViewById(R.id.tvComboTitle);
        TextView tvDescription = view.findViewById(R.id.tvDescription);

        // Nhận dữ liệu Chủ đề từ ModeSelection gửi sang
        if (getArguments() != null) {
            topicId = getArguments().getString("TOPIC_ID", "topic_education");
            topicName = getArguments().getString("SELECTED_TOPIC", "");

            // Cập nhật giao diện một chút cho đúng với 2 kỹ năng
            tvComboTitle.setText("Combo 2 skills");
            tvDescription.setText("Welcome to the 2 skills challenge include Listening and Reading 🌟\n\n TOPIC:" + topicName + "\n\nIn this mode, you will take two consecutive sections. Your score will be added up at the end of the test.\n\n🎧 The first skill will be Listening. Make sure you're in a quiet space and turn up the volume.\n\nReady? GO!");
        }


        // Xử lý sự kiện bấm BẮT ĐẦU
        btnStartCombo.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("TOPIC_ID", topicId);
            bundle.putString("SELECTED_TOPIC", topicName);
            // Quan trọng: Bật cờ Combo Mode lên True
            bundle.putBoolean("IS_COMBO_MODE", true);

            // Chuyển sang Bài Nghe đầu tiên
            Navigation.findNavController(v).navigate(R.id.listeningFragment, bundle);
        });

        return view;
    }
}