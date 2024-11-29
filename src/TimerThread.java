
public class TimerThread extends Thread {
    private int timeLeft; // Waktu dalam detik
    private boolean running = true;
    private TimerListener listener;

    public TimerThread(int time, TimerListener listener) {
        this.timeLeft = time;
        this.listener = listener;
    }

    public void stopTimer() {
        running = false;
    }

    @Override
    public void run() {
        while (timeLeft > 0 && running) {
            try {
                Thread.sleep(1000); // Tunggu 1 detik
                timeLeft--;
                System.out.println("Time left: " + timeLeft + " seconds");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (timeLeft == 0 && listener != null) {
            listener.onTimeUp(); // Panggil metode dari interface
        }
    }
}
