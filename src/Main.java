import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Main implements TimerListener {
    private JFrame frame;
    private QuizManager quizManager;
    private TimerThread timerThread;
    private int score = 0;
    private String username;
    private Connection connection;

    public Main() {
        initDatabase();
        initGUI();
    }

    private void initDatabase() {
        try {
            // Konfigurasi database
            String url = "jdbc:mysql://localhost:3306/quiz_app";
            String user = "root"; // Ganti dengan user database Anda
            String password = ""; // Ganti dengan password database Anda

            connection = DriverManager.getConnection(url, user, password);
            System.out.println("Connected to database!");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed to connect to database!", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void initGUI() {
        frame = new JFrame("Quiz App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLayout(new BorderLayout());

        // Panel input nama pengguna
        JPanel startPanel = new JPanel();
        startPanel.setLayout(new BorderLayout());
        JLabel nameLabel = new JLabel("Enter your name:", SwingConstants.CENTER);
        JTextField nameField = new JTextField();
        JButton startButton = new JButton("Start Quiz");

        startButton.addActionListener(e -> {
            username = nameField.getText().trim();
            if (username.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Name cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            saveOrUpdateUser();
            selectFile();
        });

        startPanel.add(nameLabel, BorderLayout.NORTH);
        startPanel.add(nameField, BorderLayout.CENTER);
        startPanel.add(startButton, BorderLayout.SOUTH);

        frame.add(startPanel);
        frame.setVisible(true);
    }

    private void saveOrUpdateUser() {
        try {
            // Masukkan atau perbarui pengguna di database
            String query = "INSERT INTO leaderboard (username, score) VALUES (?, ?) ON DUPLICATE KEY UPDATE username = username";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setInt(2, 0);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void selectFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Quiz JSON File");
        int result = fileChooser.showOpenDialog(frame);

        if (result == JFileChooser.APPROVE_OPTION) {
            String filePath = fileChooser.getSelectedFile().getAbsolutePath();
            loadQuiz(filePath);
        } else {
            JOptionPane.showMessageDialog(frame, "No file selected. Please select a valid JSON file.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadQuiz(String filePath) {
        try {
            quizManager = new QuizManager(filePath);
            if (!quizManager.hasNextQuestion()) {
                JOptionPane.showMessageDialog(frame, "The file is empty or invalid.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            showNextQuestion();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Error loading the quiz file. Please check the file format.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showNextQuestion() {
        if (!quizManager.hasNextQuestion()) {
            updateScoreInDatabase();
            showResult();
            return;
        }

        Question question = quizManager.getNextQuestion();

        JPanel questionPanel = new JPanel(new BorderLayout());
        JLabel questionLabel = new JLabel(question.getQuestion());
        questionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        questionPanel.add(questionLabel, BorderLayout.NORTH);

        JPanel optionsPanel = new JPanel(new GridLayout(2, 2));
        ButtonGroup buttonGroup = new ButtonGroup();

        for (String option : question.getOptions()) {
            JRadioButton optionButton = new JRadioButton(option);
            buttonGroup.add(optionButton);
            optionsPanel.add(optionButton);

            optionButton.addActionListener(e -> {
                if (option.equals(question.getAnswer())) {
                    score += 10;
                }
                timerThread.stopTimer();
                showNextQuestion();
            });
        }

        questionPanel.add(optionsPanel, BorderLayout.CENTER);

        frame.getContentPane().removeAll();
        frame.add(questionPanel, BorderLayout.CENTER);

        // Timer
        if (timerThread != null) {
            timerThread.stopTimer();
        }
        timerThread = new TimerThread(15, this); // 15 detik untuk setiap soal
        timerThread.start();

        frame.revalidate();
        frame.repaint();
    }

    private void updateScoreInDatabase() {
        try {
            String query = "UPDATE leaderboard SET score = ? WHERE username = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, score);
            stmt.setString(2, username);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showResult() {
        frame.getContentPane().removeAll();
        JLabel resultLabel = new JLabel("Quiz Complete! Your score: " + score, SwingConstants.CENTER);
        JButton leaderboardButton = new JButton("View Leaderboard");

        leaderboardButton.addActionListener(e -> showLeaderboard());

        frame.add(resultLabel, BorderLayout.CENTER);
        frame.add(leaderboardButton, BorderLayout.SOUTH);

        frame.revalidate();
        frame.repaint();
    }

    private void showLeaderboard() {
        frame.getContentPane().removeAll();

        JPanel leaderboardPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Leaderboard", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));

        JTextArea leaderboardArea = new JTextArea();
        leaderboardArea.setEditable(false);

        String query = "SELECT username, score FROM leaderboard ORDER BY score DESC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                String user = rs.getString("username");
                int userScore = rs.getInt("score");
                leaderboardArea.append(user + " - " + userScore + " points\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        JScrollPane scrollPane = new JScrollPane(leaderboardArea);
        leaderboardPanel.add(titleLabel, BorderLayout.NORTH);
        leaderboardPanel.add(scrollPane, BorderLayout.CENTER);

        JButton backButton = new JButton("Back to Main");
        backButton.addActionListener(e -> frame.dispose());

        leaderboardPanel.add(backButton, BorderLayout.SOUTH);

        frame.add(leaderboardPanel);
        frame.revalidate();
        frame.repaint();
    }

    @Override
    public void onTimeUp() {
        SwingUtilities.invokeLater(this::showNextQuestion);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}