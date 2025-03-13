import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Plot {
    private static final int MAX_SEQUENCE_LENGTH = 500;
    private static final int STEP_SIZE = 1000;
    private static final int ALIGNMENTS_PER_LENGTH = 10000;

    private static class MeasurementResult {
        List<Double> lengths = new ArrayList<>();
        List<Double> cpu_time_recursive = new ArrayList<>();
        List<Double> cpu_time_dp = new ArrayList<>();
    }

    private static String generateRandomSequence(int length) {
        Random rand = new Random();
        StringBuilder seq = new StringBuilder();
        String bases = "ACGT";
        for (int i = 0; i < length; i++) {
            seq.append(bases.charAt(rand.nextInt(bases.length())));
        }
        return seq.toString();
    }

    private static double measureCPUTimeDP(String seq1, String seq2, int iterations) {
        // Warm up JVM
        for (int i = 0; i < 1000; i++) {
            NwDp.computeAlignment(seq1, seq2);
        }

        // Measure CPU time
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            NwDp.computeAlignment(seq1, seq2);
        }
        long endTime = System.nanoTime();

        // Return average time per alignment in milliseconds
        return (endTime - startTime) / (iterations * 1_000_000.0);
    }

    private static double measureCPUTimeRecursive(String seq1, String seq2, int iterations) {
        // Warm up JVM
        for (int i = 0; i < 1000; i++) {
            NeedlemanWunsch.Recursion(seq1, seq2);
        }

        // Measure CPU time
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            NeedlemanWunsch.Recursion(seq1, seq2);
        }
        long endTime = System.nanoTime();

        // Return average time per alignment in milliseconds
        return (endTime - startTime) / (iterations * 1_000_000.0);
    }

    private static MeasurementResult collectMeasurements() {
        MeasurementResult result = new MeasurementResult();

        for (int length = STEP_SIZE; length <= MAX_SEQUENCE_LENGTH; length += STEP_SIZE) {
            System.out.printf("Processing length %d with %d alignments...%n", length, ALIGNMENTS_PER_LENGTH);

            // Generate test sequences
            String seq1 = generateRandomSequence(length);
            String seq2 = generateRandomSequence(length);

            // Measure CPU time for both implementations
            double cpuTimeDP = measureCPUTimeDP(seq1, seq2, ALIGNMENTS_PER_LENGTH);
            double cpuTimeRecursive = measureCPUTimeRecursive(seq1, seq2, ALIGNMENTS_PER_LENGTH);

            result.lengths.add((double) length);
            result.cpu_time_dp.add(cpuTimeDP);
            result.cpu_time_recursive.add(cpuTimeRecursive);
        }

        return result;
    }

    private static void saveToDataFile(MeasurementResult measurements, String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            // Write header
            writer.write("# Length CPU_Time_DP_ms CPU_Time_Recursive_ms\n");

            // Write data
            for (int i = 0; i < measurements.lengths.size(); i++) {
                writer.write(String.format("%.0f %.6f %.6f\n",
                        measurements.lengths.get(i),
                        measurements.cpu_time_dp.get(i),
                        measurements.cpu_time_recursive.get(i)));
            }

            System.out.println("Results saved to " + filename);

        } catch (IOException e) {
            System.err.println("Error saving results: " + e.getMessage());
        }
    }

    private static void createGnuplotScript(String dataFile) {
        try (FileWriter writer = new FileWriter("plot_script.gnu")) {
            writer.write("""
                set terminal pngcairo enhanced font 'Arial,12' size 1200,800
                set output 'cpu_time_comparison.png'
                
                set title 'CPU Time Comparison (Log Scale)' font 'Arial,14'
                set xlabel 'Sequence Length'
                set ylabel 'Average CPU Time per Alignment (ms)'
                set key left top
                set grid
                set logscale y
                
                # Style settings
                set style line 1 lc rgb '#0060ad' lt 1 lw 2 pt 7 ps 1.5   # blue
                set style line 2 lc rgb '#dd181f' lt 1 lw 2 pt 5 ps 1.5   # red
                set style line 3 lc rgb '#00ad00' lt 2 lw 2               # green
                set style line 4 lc rgb '#ad6000' lt 2 lw 2               # orange
                
                # Fit functions
                f(x) = a*x**2 + b*x + c                   # quadratic for DP
                g(x) = p*exp(q*x) + r                     # exponential for recursive
                
                # Fit the data
                fit f(x) '%s' using 1:2 via a,b,c
                fit g(x) '%s' using 1:3 via p,q,r
                
                # Plot the data and fitted curves
                plot '%s' using 1:2 title 'Dynamic Programming' with linespoints ls 1, \\
                     '%s' using 1:3 title 'Recursive' with linespoints ls 2, \\
                     f(x) title 'Quadratic Fit (DP)' with lines ls 3, \\
                     g(x) title 'Exponential Fit (Recursive)' with lines ls 4
                
                # Add complexity annotations
                set label 1 'Dynamic Programming: O(n²)\\nRecursive: O(2^n)' at graph 0.02,0.95 font 'Arial,10'
                """.formatted(dataFile, dataFile, dataFile, dataFile));

            System.out.println("Gnuplot script created");

        } catch (IOException e) {
            System.err.println("Error creating gnuplot script: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        System.out.println("Starting CPU time measurements...");
        System.out.println("Performing " + ALIGNMENTS_PER_LENGTH + " alignments per sequence length");
        System.out.println("JVM warm-up: 1000 iterations before each measurement");

        MeasurementResult measurements = collectMeasurements();
        String dataFile = "performance_data.txt";
        saveToDataFile(measurements, dataFile);
        createGnuplotScript(dataFile);

        System.out.println("Measurements complete. Run 'gnuplot plot_script.gnu' to create the plot.");
    }
} 