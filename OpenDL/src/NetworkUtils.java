import java.net.HttpURLConnection;
import java.net.URL;

public class NetworkUtils {
    public static class FileInfo {
        public String finalUrl;
        public long size;
        public boolean supportsMultiThreading;
        public String suggestedName;
    }

    public static FileInfo getFileInfo(String targetUrl) throws Exception {
        FileInfo info = new FileInfo();
        info.finalUrl = targetUrl;
        int redirects = 0;
        HttpURLConnection conn = null;

        while (redirects < 10) {
            URL url = new URL(info.finalUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Accept-Encoding", "identity");

            int status = conn.getResponseCode();
            if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                info.finalUrl = conn.getHeaderField("Location");
                redirects++;
            } else {
                break;
            }
        }

        info.size = conn.getContentLengthLong();
        info.supportsMultiThreading = (info.size > 0 && conn.getHeaderField("Accept-Ranges") != null && conn.getHeaderField("Accept-Ranges").equals("bytes"));

        String disposition = conn.getHeaderField("Content-Disposition");
        String fileName = "";

        if (disposition != null && disposition.contains("filename=")) {
            int index = disposition.indexOf("filename=") + 9;
            fileName = disposition.substring(index).split(";")[0];
            fileName = fileName.replace("\"", "").replace("'", "");
        } else {
            String path = new URL(info.finalUrl).getPath();
            fileName = path.substring(path.lastIndexOf("/") + 1);
        }

        if (fileName.contains("?")) {
            fileName = fileName.substring(0, fileName.indexOf("?"));
        }

        if (!fileName.contains(".")) {
            String contentType = conn.getContentType();
            if (contentType != null) {
                contentType = contentType.toLowerCase();
                if (contentType.contains("video/mp4")) fileName += ".mp4";
                else if (contentType.contains("video/webm")) fileName += ".webm";
                else if (contentType.contains("audio/mpeg")) fileName += ".mp3";
                else if (contentType.contains("application/pdf")) fileName += ".pdf";
                else if (contentType.contains("application/zip")) fileName += ".zip";
                else if (contentType.contains("image/jpeg")) fileName += ".jpg";
                else if (contentType.contains("image/png")) fileName += ".png";
                else fileName += ".dat"; // Last resort
            } else {
                fileName += ".dat";
            }
        }

        fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");

        if (fileName.isEmpty() || fileName.equals(".dat")) {
            fileName = "downloaded_file.dat";
        }

        info.suggestedName = fileName;
        return info;
    }
}