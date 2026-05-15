package thick2.edu.nguyengiakhanh.lingoenglish;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Nạp giao diện từ file XML
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Ánh xạ các CardView (Nút bấm)
        CardView cardEducation = view.findViewById(R.id.cardEducation);
        CardView cardTech = view.findViewById(R.id.cardTech);
        CardView cardTravel = view.findViewById(R.id.cardTravel);
        CardView cardJob = view.findViewById(R.id.cardJob);

        // Xử lý sự kiện click cho nút Education
        cardEducation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Đóng gói dữ liệu (Chủ đề được chọn) để gửi sang màn hình tiếp theo
                Bundle bundle = new Bundle();
                bundle.putString("SELECTED_TOPIC", "Education");

                // Lệnh chuyển sang Màn hình ModeSelectionFragment (ID lấy từ file nav_graph)
                Navigation.findNavController(v).navigate(R.id.modeSelectionFragment, bundle);
            }
        });

        // Xử lý sự kiện click cho nút Tech
        cardTech.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putString("SELECTED_TOPIC", "Technology");
                Navigation.findNavController(v).navigate(R.id.modeSelectionFragment, bundle);
            }
        });

        // Làm tương tự cho 2 nút còn lại
        cardTravel.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("SELECTED_TOPIC", "Travel");
            Navigation.findNavController(v).navigate(R.id.modeSelectionFragment, bundle);
        });

        cardJob.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("SELECTED_TOPIC", "Jobs");
            Navigation.findNavController(v).navigate(R.id.modeSelectionFragment, bundle);
        });

        return view;
    }
}