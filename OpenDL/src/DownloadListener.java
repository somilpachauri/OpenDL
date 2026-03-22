public interface DownloadListener {
    void onStart(long totalBytes, int actualThreads);
    void onProgressUpdate(long downloadedBytes, double speedMBps);
    void onThreadProgress(int threadId, int progressPercentage);
    void onComplete(String filePath);
    void onError(String errorMessage);
    void onPaused();
}