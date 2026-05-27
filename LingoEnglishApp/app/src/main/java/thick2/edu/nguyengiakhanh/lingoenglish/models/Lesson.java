package thick2.edu.nguyengiakhanh.lingoenglish.models;

import java.util.List;

public class Lesson {
    private String topicId;
    private String title;
    private String audioFileName;
    private String transcript;
    private List<Question> questions;

    // Constructor rỗng bắt buộc phải có cho Firebase
    public Lesson() {}

    public String getTopicId() { return topicId; }
    public String getTitle() { return title; }
    public String getAudioFileName() { return audioFileName; }
    public String getTranscript() { return transcript; }
    public List<Question> getQuestions() { return questions; }
}