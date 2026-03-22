import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class HistoryManager {
    private static final String HISTORY_FILE = "opendl_history.json";

    public static void addDownload(String fileName, String url, String filePath, long sizeBytes) {
        List<String> history = loadHistoryRaw();
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        String safeName = fileName.replace("\"", "\\\"");
        String safeUrl = url.replace("\"", "\\\"");
        String safePath = filePath.replace("\\", "\\\\").replace("\"", "\\\"");

        String newRecord = String.format(
                "{\"date\":\"%s\", \"name\":\"%s\", \"url\":\"%s\", \"path\":\"%s\", \"size\":%d}",
                date, safeName, safeUrl, safePath, sizeBytes
        );

        history.add(newRecord);
        saveHistoryRaw(history);
    }

    public static List<String[]> getHistoryList() {
        List<String> raw = loadHistoryRaw();
        List<String[]> parsed = new ArrayList<>();
        for (String line : raw) {
            try {
                String date = extractJsonValue(line, "date");
                String name = extractJsonValue(line, "name");
                String path = extractJsonValue(line, "path");
                String size = extractJsonValue(line, "size");
                parsed.add(new String[]{date, name, path, formatSize(Long.parseLong(size))});
            } catch(Exception e) {
            }
        }
        Collections.reverse(parsed);
        return parsed;
    }

    private static String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return "Unknown";
        start += search.length();

        int end;
        if (json.charAt(start) == '"') {
            start++;
            end = json.indexOf("\"", start);
        } else {
            end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
        }
        return json.substring(start, end).trim();
    }

    private static List<String> loadHistoryRaw() {
        List<String> lines = new ArrayList<>();
        File f = new File(HISTORY_FILE);
        if (!f.exists()) return lines;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String content = br.readLine();
            if (content != null && content.startsWith("[")) {
                String[] records = content.replace("[", "").replace("]", "").split("\\},\\{");
                for (String r : records) {
                    if (!r.startsWith("{")) r = "{" + r;
                    if (!r.endsWith("}")) r = r + "}";
                    if (r.length() > 5) lines.add(r);
                }
            }
        } catch (Exception e) {}
        return lines;
    }

    private static void saveHistoryRaw(List<String> history) {
        try (FileWriter fw = new FileWriter(HISTORY_FILE)) {
            fw.write("[");
            for (int i = 0; i < history.size(); i++) {
                fw.write(history.get(i));
                if (i < history.size() - 1) fw.write(",");
            }
            fw.write("]");
        } catch (Exception e) {}
    }
    public static void clearHistory() {
        File f = new File(HISTORY_FILE);
        if (f.exists()) {
            f.delete();
        }
    }
    private static String formatSize(long bytes) {
        if (bytes <= 0) return "Unknown";
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }
}