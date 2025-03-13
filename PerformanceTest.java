import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class PerformanceTest {
    private static final int NUM_ITERATIONS = 5;
    private static final int WARMUP_ITERATIONS = 3;
    private static final String[] SEQUENCE_LENGTHS = {"5","6", "7", "8", "9","10","11", "12", "13","14","15"};
    private static final String NUCLEOTIDES = "ACGT";
    private static final Random random = new Random();

    public static String generateRandomSequence(int length) {
        StringBuilder sequence = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sequence.append(NUCLEOTIDES.charAt(random.nextInt(NUCLEOTIDES.length())));
        }
        return sequence.toString();
    }

    public static void main(String[] args) {
        try (FileWriter csvWriter = new FileWriter("performance_results.csv")) {
            // Write CSV header
            csvWriter.write("Sequence Length,Recursive Time (ms),DP Time (ms)\n");

            System.out.println("Starting performance test...");
            System.out.println("Testing sequences of lengths: " + String.join(", ", SEQUENCE_LENGTHS));
            System.out.println("Number of iterations per length: " + NUM_ITERATIONS);
            System.out.println("Warmup iterations: " + WARMUP_ITERATIONS);
            System.out.println("\nResults:");
            System.out.println("----------------------------------------");
            System.out.printf("%-15s %-20s %-20s%n", "Length", "Recursive (ms)", "DP (ms)");
            System.out.println("----------------------------------------");

            for (String lengthStr : SEQUENCE_LENGTHS) {
                int length = Integer.parseInt(lengthStr);
                long recursiveTime = 0;
                long dpTime = 0;

                System.out.println("Testing length " + length + "...");

                // Warm up JVM (reduced iterations)
                System.out.println("Warming up...");
                for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                    String seq1 = generateRandomSequence(length);
                    String seq2 = generateRandomSequence(length);
                    NeedlemanWunsch.Recursion(seq1, seq2);
                    NwDp.computeAlignment(seq1, seq2);
                }

                // Actual measurements
                System.out.println("Running measurements...");
                for (int i = 0; i < NUM_ITERATIONS; i++) {
                    String seq1 = generateRandomSequence(length);
                    String seq2 = generateRandomSequence(length);

                    // Measure recursive implementation
                    long startTime = System.nanoTime();
                    NeedlemanWunsch.Recursion(seq1, seq2);
                    recursiveTime += System.nanoTime() - startTime;

                    // Measure DP implementation
                    startTime = System.nanoTime();
                    NwDp.computeAlignment(seq1, seq2);
                    dpTime += System.nanoTime() - startTime;
                }

                // Convert to milliseconds and average
                double avgRecursiveTime = (recursiveTime / 1_000_000.0) / NUM_ITERATIONS;
                double avgDpTime = (dpTime / 1_000_000.0) / NUM_ITERATIONS;

                // Write results to CSV
                csvWriter.write(String.format("%d,%.3f,%.3f%n", length, avgRecursiveTime, avgDpTime));

                // Print results in a formatted table
                System.out.printf("%-15d %-20.3f %-20.3f%n", length, avgRecursiveTime, avgDpTime);
            }

            System.out.println("----------------------------------------");
            System.out.println("\nResults have been saved to performance_results.csv");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 