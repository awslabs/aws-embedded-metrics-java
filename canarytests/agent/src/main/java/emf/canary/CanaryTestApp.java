package emf.canary;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledExecutorService;

public class CanaryTestApp {
    public static void main(String[] args) {
        System.out.println("Canary starting...");
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(new ECSRunnable(), 5000, 1000, TimeUnit.MILLISECONDS);
    }
}
