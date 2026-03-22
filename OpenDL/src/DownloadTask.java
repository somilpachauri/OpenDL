import javax.swing.Timer;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class DownloadTask implements Runnable {
    private String url;
    private String saveDir;
    private int requestedThreads;
    private DownloadListener listener;
    private AtomicBoolean isCancelled = new AtomicBoolean(false);

    public DownloadTask(String url, String saveDir, int requestedThreads, DownloadListener listener) {
        this.url = url;
        this.saveDir = saveDir;
        this.requestedThreads = requestedThreads;
        this.listener = listener;
    }

    public void pauseDownload() {
        isCancelled.set(true);
    }

    @Override
    public void run() {
        Timer progressTimer = null;
        try {
            NetworkUtils.FileInfo info = NetworkUtils.getFileInfo(url);
            int actualThreads = (info.size > 0) ? requestedThreads : 1;
            listener.onStart(info.size, actualThreads);

            String fileName = info.suggestedName;

            ExecutorService executor = Executors.newFixedThreadPool(actualThreads);
            AtomicLong totalDownloaded = new AtomicLong(0);
            AtomicLong[] threadProgresses = new AtomicLong[actualThreads];
            long[] threadExpectedSizes = new long[actualThreads];

            long[] lastDownloaded = {0};
            progressTimer = new Timer(500, e -> {
                long currentTotal = totalDownloaded.get();
                long bytesDiff = currentTotal - lastDownloaded[0];
                double speedMBps = (bytesDiff * 2) / (1024.0 * 1024.0);
                lastDownloaded[0] = currentTotal;

                listener.onProgressUpdate(currentTotal, speedMBps);

                for (int i = 0; i < actualThreads; i++) {
                    if (threadProgresses[i] != null) {
                        if (threadExpectedSizes[i] > 0) {
                            int tProg = (int) ((threadProgresses[i].get() * 100) / threadExpectedSizes[i]);
                            listener.onThreadProgress(i, Math.min(tProg, 100));
                        } else {
                            listener.onThreadProgress(i, -1);
                        }
                    }
                }
            });
            progressTimer.start();

            if (actualThreads == 1 && info.size <= 0) {
                threadExpectedSizes[0] = -1;
                threadProgresses[0] = new AtomicLong(0);
                executor.execute(new DownloadWorker(info.finalUrl, 0, -1, saveDir, 0, totalDownloaded, threadProgresses[0], isCancelled));
            } else {
                long partSize = info.size / actualThreads;
                for (int i = 0; i < actualThreads; i++) {
                    long start = i * partSize;
                    long end = (i == actualThreads - 1) ? info.size - 1 : (start + partSize) - 1;
                    threadExpectedSizes[i] = end - start + 1;
                    threadProgresses[i] = new AtomicLong(0);
                    executor.execute(new DownloadWorker(info.finalUrl, start, end, saveDir, i, totalDownloaded, threadProgresses[i], isCancelled));
                }
            }

            executor.shutdown();
            while (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                if (isCancelled.get()) {
                    executor.shutdownNow();
                    break;
                }
            }

            progressTimer.stop();

            if (isCancelled.get()) {
                listener.onPaused();
            } else {
                FileMerger.merge(saveDir, fileName, actualThreads);
                listener.onComplete(saveDir + File.separator + fileName);
            }

        } catch (Exception ex) {
            if (progressTimer != null) progressTimer.stop();
            listener.onError(ex.getMessage());
        }
    }
}