import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class DownloadWorker implements Runnable {
    private String fileUrl;
    private long startByte;
    private long endByte;
    private File partFile;
    private AtomicLong globalDownloaded;
    private AtomicLong myProgress;
    private AtomicBoolean isCancelled;

    public DownloadWorker(String url, long start, long end, String saveDir, int partNum, AtomicLong globalDownloaded, AtomicLong myProgress, AtomicBoolean isCancelled) {
        this.fileUrl = url;
        this.startByte = start;
        this.endByte = end;
        this.partFile = new File(saveDir, "part_" + partNum + ".tmp");
        this.globalDownloaded = globalDownloaded;
        this.myProgress = myProgress;
        this.isCancelled = isCancelled;
    }

    @Override
    public void run() {
        try {
            long existingSize = partFile.exists() ? partFile.length() : 0;
            long currentStart = startByte + existingSize;

            globalDownloaded.addAndGet(existingSize);
            myProgress.addAndGet(existingSize);

            if (endByte != -1 && currentStart >= endByte) {
                return;
            }

            URL url = new URL(fileUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            conn.setRequestProperty("Accept-Encoding", "identity");

            if (endByte != -1) {
                conn.setRequestProperty("Range", "bytes=" + currentStart + "-" + endByte);
            } else if (currentStart > 0) {
                conn.setRequestProperty("Range", "bytes=" + currentStart + "-");
            }
            conn.connect();

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_PARTIAL || responseCode == HttpURLConnection.HTTP_OK) {
                try (InputStream in = conn.getInputStream();
                     RandomAccessFile raf = new RandomAccessFile(partFile, "rw")) {

                    raf.seek(existingSize);
                    byte[] buffer = new byte[8192];
                    int bytesRead;

                    while ((bytesRead = in.read(buffer)) != -1) {
                        if (isCancelled.get()) {
                            System.out.println("Worker stopped for Pause.");
                            break;
                        }

                        raf.write(buffer, 0, bytesRead);
                        globalDownloaded.addAndGet(bytesRead);
                        myProgress.addAndGet(bytesRead);
                    }
                }
            }
        } catch (IOException e) {
            if (!isCancelled.get()) {
                System.err.println("Error in worker thread: " + e.getMessage());
            }
        }
    }
}