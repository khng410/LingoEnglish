package thick2.edu.nguyengiakhanh.lingoenglish;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class ModeSelectionFragment extends Fragment {

    private String topicName = "";
    private String topicId = "topic_education";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mode_selection, container, false);

        TextView tvSelectedTopic = view.findViewById(R.id.tvSelectedTopic);
        CardView cardSingleSkill = view.findViewById(R.id.cardSingleSkill);
        CardView cardComboSkill = view.findViewById(R.id.cardComboSkill);
        ImageView btnBack = view.findViewById(R.id.btnBack);

        if (getArguments() != null) {
            topicName = getArguments().getString("SELECTED_TOPIC");
            tvSelectedTopic.setText(topicName);
            topicId = getTopicIdFromName(topicName);
        }

        btnBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        cardSingleSkill.setOnClickListener(v -> {
            showBottomSheet(v);
        });

        // ĐÃ SỬA: Chuyển sang ComboIntroFragment kèm theo dữ liệu Chủ đề
        cardComboSkill.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("TOPIC_ID", topicId);
            bundle.putString("SELECTED_TOPIC", topicName);
            Navigation.findNavController(v).navigate(R.id.comboIntroFragment, bundle);
        });

        return view;
    }

    private void showBottomSheet(View parentView) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_skills, null);
        bottomSheetDialog.setContentView(sheetView);

        NavController navController = Navigation.findNavController(parentView);

        sheetView.findViewById(R.id.btnListen).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Bundle bundle = new Bundle();
            bundle.putString("TOPIC_ID", topicId);
            bundle.putString("SELECTED_TOPIC", topicName);
            // Điểm khác biệt: Đi lẻ thì không có cờ IS_COMBO_MODE (mặc định là false)
            navController.navigate(R.id.listeningFragment, bundle);
        });

        sheetView.findViewById(R.id.btnRead).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Bundle bundle = new Bundle();
            bundle.putString("TOPIC_ID", topicId);
            bundle.putString("SELECTED_TOPIC", topicName);
            navController.navigate(R.id.readingFragment, bundle);
        });

        bottomSheetDialog.show();
    }

    private String getTopicIdFromName(String name) {
        if (name == null) return "topic_education";
        String lowerName = name.toLowerCase();
        if (lowerName.contains("giáo dục") || lowerName.contains("education")) {
            return "topic_education";
        } else if (lowerName.contains("công việc") || lowerName.contains("job") || lowerName.contains("việc làm")) {
            return "topic_jobs";
        } else if (lowerName.contains("công nghệ") || lowerName.contains("tech")) {
            return "topic_tech";
        } else if (lowerName.contains("du lịch") || lowerName.contains("travel")) {
            return "topic_travel";
        }
        return "topic_education";
    }
}