import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Semaphore;

public class FileSimilarity {
    private static Semaphore mutex = new Semaphore(1);

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java Sum filepath1 filepath2 filepathN");
            System.exit(1);
        }

        Thread[] threads = new Thread[args.length];
        int index = 0;
        // // Create a map to store the fingerprint for each file
        Map<String, List<Long>> fileFingerprints = new HashMap<>();

        // Calculate the fingerprint for each file
        for (String path : args) {
            Thread th = new Thread(() -> {
                try{
                    List<Long> fingerprint = fileSum(path);
                    mutex.acquire();
                    fileFingerprints.put(path, fingerprint);
                } catch(Exception e){
                    System.err.println("Error: " + e);
                } finally{
                    mutex.release();
                }
                 

            });
            th.start();
            threads[index] = th;
            index++;
        
        }

        for(Thread th: threads){
            th.join();
        }

        // Compare each pair of files
        for (int i = 0; i < args.length; i++) {
            for (int j = i + 1; j < args.length; j++) {
                String file1 = args[i];
                String file2 = args[j];
                List<Long> fingerprint1 = fileFingerprints.get(file1);
                List<Long> fingerprint2 = fileFingerprints.get(file2);
                Thread th = new Thread(() -> {
                    float similarityScore = similarity(fingerprint1, fingerprint2);
                    System.out.println("Similarity between " + file1 + " and " + file2 + ": " + (similarityScore * 100) + "%");
                });
                th.start();
                
            }
        }
    }

    private static List<Long> fileSum(String filePath) throws IOException {
        File file = new File(filePath);
        List<Long> chunks = new ArrayList<>();
        try (FileInputStream inputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[100];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                long sum = sum(buffer, bytesRead);
                chunks.add(sum);
            }
        }
        return chunks;
    }

    private static long sum(byte[] buffer, int length) {
        long sum = 0;
        for (int i = 0; i < length; i++) {
            sum += Byte.toUnsignedInt(buffer[i]);
        }
        return sum;
    }

    private static float similarity(List<Long> base, List<Long> target) {
        int counter = 0;
        List<Long> targetCopy = new ArrayList<>(target);

        for (Long value : base) {
            if (targetCopy.contains(value)) {
                counter++;
                targetCopy.remove(value);
            }
        }

        return (float) counter / base.size();
    }
}

