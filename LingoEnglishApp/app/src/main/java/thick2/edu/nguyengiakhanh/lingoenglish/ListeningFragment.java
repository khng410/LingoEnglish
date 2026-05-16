package thick2.edu.nguyengiakhanh.lingoenglish;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Locale;

public class ListeningFragment extends Fragment {

    private boolean isTranscriptVisible = false;
    private MediaPlayer mediaPlayer;
    private final Handler handler = new Handler();
    private SeekBar seekBarAudio;
    private TextView tvCurrentTime, tvTotalTime, tvLessonTitle;
    private FloatingActionButton btnPlayPause;
    private String selectedTopic = "Education";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_listening, container, false);

        // Ánh xạ các view
        ImageView btnBack = view.findViewById(R.id.btnBack);
        MaterialButton btnShowTranscript = view.findViewById(R.id.btnShowTranscript);
        TextView tvTranscript = view.findViewById(R.id.tvTranscript);
        CardView cardThumbnail = view.findViewById(R.id.cardThumbnail);
        btnPlayPause = view.findViewById(R.id.btnPlayPause);
        tvLessonTitle = view.findViewById(R.id.tvLessonTitle);
        seekBarAudio = view.findViewById(R.id.seekBarAudio);
        tvCurrentTime = view.findViewById(R.id.tvCurrentTime);
        tvTotalTime = view.findViewById(R.id.tvTotalTime);
        ImageView btnRewind = view.findViewById(R.id.btnRewind);
        ImageView btnForward = view.findViewById(R.id.btnForward);

        // Nhận dữ liệu topic
        if (getArguments() != null) {
            selectedTopic = getArguments().getString("SELECTED_TOPIC", "Education");
        }
        tvLessonTitle.setText(String.format("Chủ đề: %s", selectedTopic));

        // Nút Quay lại
        btnBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        // Nút Phụ đề
        btnShowTranscript.setOnClickListener(v -> {
            isTranscriptVisible = !isTranscriptVisible;
            tvTranscript.setVisibility(isTranscriptVisible ? View.VISIBLE : View.GONE);
            cardThumbnail.setVisibility(isTranscriptVisible ? View.GONE : View.VISIBLE);
            btnShowTranscript.setText(isTranscriptVisible ? "Ẩn phụ đề" : "Xem phụ đề");
        });

        // Khởi tạo Player
        setupMediaPlayer();

        // Điều khiển Play/Pause
        btnPlayPause.setOnClickListener(v -> togglePlayPause());

        // Tua lại 10s
        btnRewind.setOnClickListener(v -> {
            if (mediaPlayer != null) {
                int newPos = Math.max(mediaPlayer.getCurrentPosition() - 10000, 0);
                mediaPlayer.seekTo(newPos);
            }
        });

        // Tua nhanh 10s
        btnForward.setOnClickListener(v -> {
            if (mediaPlayer != null) {
                int newPos = Math.min(mediaPlayer.getCurrentPosition() + 10000, mediaPlayer.getDuration());
                mediaPlayer.seekTo(newPos);
            }
        });

        // Kéo thanh trượt
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

        // Nộp bài
        Button btnSubmit = view.findViewById(R.id.btnSubmitQuiz);
        RadioGroup radioGroupAnswers = view.findViewById(R.id.radioGroupAnswers);
        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                if (radioGroupAnswers != null && radioGroupAnswers.getCheckedRadioButtonId() == -1) {
                    Toast.makeText(getContext(), "Vui lòng chọn một đáp án!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Nộp bài thành công!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        return view;
    }

    private void setupMediaPlayer() {
        int audioResId;
        
        switch (selectedTopic) {
            case "Technology":
                audioResId = R.raw.techconversation;
                break;
            case "Travel":
                audioResId = R.raw.travelconversation;
                break;
            case "Jobs":
                audioResId = R.raw.jobconversation;
                break;
            case "Education":
            default:
                audioResId = R.raw.education;
                break;
        }

        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
            mediaPlayer = MediaPlayer.create(requireContext(), audioResId);
            if (mediaPlayer != null) {
                seekBarAudio.setMax(mediaPlayer.getDuration());
                tvTotalTime.setText(formatTime(mediaPlayer.getDuration()));
                
                mediaPlayer.setOnCompletionListener(mp -> {
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                    seekBarAudio.setProgress(0);
                    tvCurrentTime.setText("00:00");
                    handler.removeCallbacks(updateSeekBarRunnable);
                });
            } else {
                Toast.makeText(getContext(), "Không thể tải file âm thanh cho " + selectedTopic, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Lỗi khởi tạo âm thanh", Toast.LENGTH_SHORT).show();
        }
    }

    private void togglePlayPause() {
        if (mediaPlayer == null) return;

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
            handler.removeCallbacks(updateSeekBarRunnable);
        } else {
            mediaPlayer.start();
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            handler.post(updateSeekBarRunnable);
        }
    }

    private final Runnable updateSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                int currentPos = mediaPlayer.getCurrentPosition();
                seekBarAudio.setProgress(currentPos);
                tvCurrentTime.setText(formatTime(currentPos));
                handler.postDelayed(this, 1000);
            }
        }
    };

    private String formatTime(int milliseconds) {
        int minutes = (milliseconds / 1000) / 60;
        int seconds = (milliseconds / 1000) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
        }
        handler.removeCallbacks(updateSeekBarRunnable);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacks(updateSeekBarRunnable);
    }
}
