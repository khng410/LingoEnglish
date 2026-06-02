package thick2.edu.nguyengiakhanh.lingoenglish;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LeaderboardFragment extends Fragment {

    private TabLayout tabLayoutSkills;
    private RecyclerView recyclerViewLeaderboard;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private LeaderboardAdapter adapter;
    private List<ScoreRecord> scoreList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_leaderboard, container, false);

        tabLayoutSkills = view.findViewById(R.id.tabLayoutSkills);
        recyclerViewLeaderboard = view.findViewById(R.id.recyclerViewLeaderboard);
        progressBar = view.findViewById(R.id.progressBar);

        db = FirebaseFirestore.getInstance();

        // Cài đặt RecyclerView
        recyclerViewLeaderboard.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new LeaderboardAdapter(scoreList);
        recyclerViewLeaderboard.setAdapter(adapter);

        // Mặc định load Tab đầu tiên là Nghe (Listening)
        fetchLeaderboardData("Listening");

        // Lắng nghe sự kiện người dùng bấm chuyển Tab
        tabLayoutSkills.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: // Tab Nghe
                        fetchLeaderboardData("Listening");
                        break;
                    case 1: // Tab Đọc
                        fetchLeaderboardData("Reading");
                        break;
                    case 2: // Tab Combo
                        fetchLeaderboardData("Combo");
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        return view;
    }

    // Hàm truy vấn Firebase theo tên Kỹ năng được chọn
    private void fetchLeaderboardData(String skillFilter) {
        progressBar.setVisibility(View.VISIBLE);
        scoreList.clear();
        adapter.notifyDataSetChanged();

        // Tìm trong Collection user_scores những bản ghi có skillName khớp với Tab
        db.collection("user_scores")
                .whereEqualTo("skillName", skillFilter)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        ScoreRecord record = new ScoreRecord();
                        // Firebase trả về kiểu số dưới dạng Long/Double nên cần convert
                        Long correct = doc.getLong("correctAnswers");
                        Long total = doc.getLong("totalQuestions");

                        record.correctAnswers = (correct != null) ? correct.intValue() : 0;
                        record.totalQuestions = (total != null) ? total.intValue() : 0;
                        record.lessonTitle = doc.getString("lessonTitle");

                        // ĐÃ SỬA DÒNG NÀY: Lấy tên người dùng từ Firebase (nếu null thì để ẩn danh)
                        String fetchedName = doc.getString("userName");
                        record.userName = (fetchedName != null && !fetchedName.isEmpty()) ? fetchedName : "Guest";

                        scoreList.add(record);
                    }

                    // Sắp xếp danh sách điểm từ Cao xuống Thấp bằng Code Java (Tránh lỗi Index Firestore)
                    Collections.sort(scoreList, (a, b) -> {
                        // Tính % đúng (vì Combo là 30 câu, Single là 15 câu nên so sánh tỷ lệ % là công bằng nhất)
                        float percentA = (float) a.correctAnswers / Math.max(a.totalQuestions, 1);
                        float percentB = (float) b.correctAnswers / Math.max(b.totalQuestions, 1);
                        return Float.compare(percentB, percentA);
                    });

                    progressBar.setVisibility(View.GONE);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e("Firebase", "Lỗi tải bảng xếp hạng", e);
                    Toast.makeText(getContext(), "Không tải được bảng xếp hạng", Toast.LENGTH_SHORT).show();
                });
    }

    // ==========================================
    // CLASS MODEL: Chứa dữ liệu của 1 dòng
    // ==========================================
    public static class ScoreRecord {
        public String userName;
        public String lessonTitle;
        public int correctAnswers;
        public int totalQuestions;
    }

    // ==========================================
    // CLASS ADAPTER: Đổ dữ liệu vào RecyclerView
    // ==========================================
    private class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

        private List<ScoreRecord> records;

        public LeaderboardAdapter(List<ScoreRecord> records) {
            this.records = records;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leaderboard, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ScoreRecord record = records.get(position);

            // Gán vị trí Top
            holder.tvRank.setText(String.valueOf(position + 1));

            // Tô màu Top 1, 2, 3 cho nổi bật
            if (position == 0) {
                holder.tvRank.setTextColor(Color.parseColor("#FFD700")); // Vàng
            } else if (position == 1) {
                holder.tvRank.setTextColor(Color.parseColor("#C0C0C0")); // Bạc
            } else if (position == 2) {
                holder.tvRank.setTextColor(Color.parseColor("#CD7F32")); // Đồng
            } else {
                holder.tvRank.setTextColor(Color.parseColor("#757575")); // Xám
            }

            holder.tvUserName.setText(record.userName);
            holder.tvScore.setText(record.correctAnswers + "/" + record.totalQuestions);
        }

        @Override
        public int getItemCount() {
            return records.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvRank, tvUserName, tvScore;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvRank = itemView.findViewById(R.id.tvRank);
                tvUserName = itemView.findViewById(R.id.tvUserName);
                tvScore = itemView.findViewById(R.id.tvScore);
            }
        }
    }
}