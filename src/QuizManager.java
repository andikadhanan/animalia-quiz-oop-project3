
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.Reader;
import java.util.List;

public class QuizManager {
    private List<Question> questions;
    private int currentIndex = 0;

    public QuizManager(String filePath) {
        loadQuestions(filePath);
    }

    private void loadQuestions(String filePath) {
        try (Reader reader = new FileReader(filePath)) {
            Gson gson = new Gson();
            questions = gson.fromJson(reader, new TypeToken<List<Question>>(){}.getType());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Question getNextQuestion() {
        if (currentIndex < questions.size()) {
            return questions.get(currentIndex++);
        }
        return null;
    }

    public boolean hasNextQuestion() {
        return currentIndex < questions.size();
    }
}