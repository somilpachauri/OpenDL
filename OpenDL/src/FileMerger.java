import java.io.*;
import java.nio.file.Files;

public class FileMerger {
    public static void merge(String path, String fileName, int threadCount) throws IOException {
        File finalFile = new File(path, fileName);

        try (FileOutputStream out = new FileOutputStream(finalFile)) {
            for (int i = 0; i < threadCount; i++) {
                File partFile = new File(path, "part_" + i + ".tmp");
                if (partFile.exists()) {
                    Files.copy(partFile.toPath(), out);
                    partFile.delete();
                }
            }
        }
        System.out.println("Merge complete: " + finalFile.getAbsolutePath());
    }
}