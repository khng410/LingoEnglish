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
            tvSelectedTopic.setText("Chủ đề: " + topicName);
        }

        btnBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        // Truyền thẳng 'view' vào hàm showBottomSheet để NavController có thể sử dụng
        cardSingleSkill.setOnClickListener(v -> {
            showBottomSheet(v);
        });

        cardComboSkill.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Sẽ chuyển sang Màn hình Intro", Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    // Cập nhật hàm: Nhận thêm tham số View parentView để tìm NavController
    private void showBottomSheet(View parentView) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_skills, null);
        bottomSheetDialog.setContentView(sheetView);

        // Khởi tạo NavController từ View của Fragment hiện tại
        NavController navController = Navigation.findNavController(parentView);

        sheetView.findViewById(R.id.btnListen).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            // Chuyển sang màn hình Nghe kèm theo tên Chủ đề
            Bundle bundle = new Bundle();
            bundle.putString("SELECTED_TOPIC", topicName);
            navController.navigate(R.id.listeningFragment, bundle);
        });

        sheetView.findViewById(R.id.btnRead).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            // Tạm thời để Toast cho phần Đọc vì ta chưa tạo ReadingFragment
            Toast.makeText(getContext(), "Đang chuẩn bị giao diện Đọc...", Toast.LENGTH_SHORT).show();
        });

        bottomSheetDialog.show();
    }
}