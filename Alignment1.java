import java.io.*;
import java.util.*;

public class Alignment1 {
    // Default gap penalties
    private static final double DEFAULT_GAP_OPEN = -12.0;
    private static final double DEFAULT_GAP_EXTEND = -1.0;

    // Help text
    private static final String HELP_TEXT =
            "Usage: java -jar alignment.jar [options]\n" +
                    "Options:\n" +
                    "  --pairs FILE       Input file containing sequence pairs to align\n" +
                    "  --seqlib FILE      Sequence library file\n" +
                    "  -m FILE           Substitution matrix file\n" +
                    "  --go VALUE        Gap open penalty (default: -12.0)\n" +
                    "  --ge VALUE        Gap extend penalty (default: -1.0)\n" +
                    "  --mode MODE       Alignment mode: global, local, or freeshift\n" +
                    "  --nw              Use NW/SW algorithms instead of Gotoh (default: Gotoh)\n" +
                    "  --format FORMAT   Output format: score, ali, or html\n" +
                    "  --dpmatrices DIR  Output directory for DP matrices\n" +
                    "  --check          Check alignment scores\n" +
                    "  --debug          Enable debug output\n";

    // Alignment modes
    public enum Mode {
        LOCAL, GLOBAL, FREESHIFT
    }

    // Output formats
    public enum Format {
        SCORE, ALI, HTML
    }

    // Command line options
    private String pairsFile;
    private String seqLibFile;
    private String matrixFile;
    private double gapOpen = DEFAULT_GAP_OPEN;
    private double gapExtend = DEFAULT_GAP_EXTEND;
    private Mode mode = Mode.GLOBAL;
    private Format format = Format.SCORE;
    private String dpMatricesDir;
    private boolean checkScores = false;
    private boolean useNWSW = false;
    private boolean debug = false;

    // Core components
    private SubstitutionMatrix substitutionMatrix;
    private AlignmentAlgorithm algorithm;
    private Map<String, String> sequenceLibrary = new HashMap<>();
    private List<AlignmentPair> alignmentPairs = new ArrayList<>();

    public static void main(String[] args) {
        Alignment1 alignment = new Alignment1();
        try {
            alignment.parseArgs(args);
            alignment.run();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            if (alignment.debug) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }

    private void parseArgs(String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("No arguments provided");
        }

        String seq1 = null;
        String seq2 = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--seq1":
                    seq1 = args[++i];  // First sequence directly
                    break;
                case "--seq2":
                    seq2 = args[++i];  // Second sequence directly
                    break;
                case "-m":
                    matrixFile = args[++i];
                    break;
                case "--go":
                    gapOpen = Double.parseDouble(args[++i]);
                    break;
                case "--ge":
                    gapExtend = Double.parseDouble(args[++i]);
                    break;
                case "--mode":
                    mode = Mode.valueOf(args[++i].toUpperCase());
                    break;
                case "--format":
                    format = Format.HTML; // Always force HTML output
                    break;
                default:
                    throw new IllegalArgumentException("Unknown option: " + args[i]);
            }
        }

        if (seq1 == null || seq2 == null) {
            throw new IllegalArgumentException("Missing required arguments: --seq1 and --seq2 are required.");
        }
    }


    private boolean argsContains(String[] args, String arg) {
        for (String a : args) {
            if (a.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private void run() throws IOException {
        loadSequenceLibrary();
        loadSubstitutionMatrix();
        loadAlignmentPairs();
        algorithm = new AlignmentAlgorithm(substitutionMatrix, gapOpen, gapExtend);
        processAlignments();
    }

    private void loadSequenceLibrary() throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(seqLibFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    String id = parts[0].trim();
                    String sequence = parts[1].trim();
                    sequenceLibrary.put(id, sequence);
                    sequenceLibrary.put(id.toLowerCase(), sequence);
                }
            }
        }

        if (sequenceLibrary.isEmpty()) {
            throw new IOException("No sequences loaded from library file");
        }
    }

    private void loadSubstitutionMatrix() throws IOException {
        substitutionMatrix = new SubstitutionMatrix(matrixFile, debug);
    }

    private void loadAlignmentPairs() throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(pairsFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                String domain1 = null;
                String domain2 = null;

                for (int i = 0; i < parts.length - 1; i++) {
                    if (parts[i].matches("[\\w.]+") && parts[i+1].matches("[\\w.]+")) {
                        String testDomain1 = convertDomainId(parts[i]);
                        String testDomain2 = convertDomainId(parts[i+1]);

                        if (sequenceLibrary.containsKey(testDomain1) &&
                                sequenceLibrary.containsKey(testDomain2)) {
                            domain1 = testDomain1;
                            domain2 = testDomain2;
                            break;
                        }
                    }
                }

                if (domain1 != null && domain2 != null) {
                    alignmentPairs.add(new AlignmentPair(domain1, domain2));
                }
            }

            if (alignmentPairs.isEmpty()) {
                throw new IOException("No alignment pairs loaded from pairs file.");
            }
        }
    }

    private String convertDomainId(String domainId) {
        if (sequenceLibrary.containsKey(domainId)) {
            return domainId;
        }

        String lowercaseId = domainId.toLowerCase();
        if (sequenceLibrary.containsKey(lowercaseId)) {
            return lowercaseId;
        }

        domainId = domainId.replaceAll("^d|_$", "");
        String cleanId = domainId.replace(".", "");

        if (sequenceLibrary.containsKey(cleanId)) {
            return cleanId;
        }
        if (sequenceLibrary.containsKey(cleanId.toLowerCase())) {
            return cleanId.toLowerCase();
        }

        if (domainId.matches("\\d+[A-Z]\\d+")) {
            String base = domainId.replaceAll("[A-Z]\\d+$", "");
            String chain = domainId.replaceAll("^\\d+|\\d+$", "");
            String number = domainId.replaceAll("^\\d+[A-Z]", "");

            List<String> candidates = Arrays.asList(
                    base + chain + String.format("%02d", Integer.parseInt(number)),
                    base + number + chain,
                    base + chain + "A" + String.format("%02d", Integer.parseInt(number)),
                    base + chain.toLowerCase() + String.format("%02d", Integer.parseInt(number))
            );

            for (String candidate : candidates) {
                if (sequenceLibrary.containsKey(candidate)) {
                    return candidate;
                }
                if (sequenceLibrary.containsKey(candidate.toLowerCase())) {
                    return candidate.toLowerCase();
                }
            }
        }

        return domainId;
    }

    private void processAlignments() {
        // Create output files for both formats
        String detailedOutputFile = "alignment_results.txt";
        String simpleOutputFile = "simple_alignments.txt";

        try (PrintWriter detailedWriter = new PrintWriter(new FileWriter(detailedOutputFile));
             PrintWriter simpleWriter = new PrintWriter(new FileWriter(simpleOutputFile))) {

            for (AlignmentPair pair : alignmentPairs) {
                String seq1 = sequenceLibrary.get(pair.id1);
                String seq2 = sequenceLibrary.get(pair.id2);

                if (seq1 == null || seq2 == null) {
                    continue;
                }

                AlignmentAlgorithm.AlignmentResult result = computeAlignment(seq1, seq2);
                outputResult(pair, result, detailedWriter, simpleWriter);
            }
        } catch (IOException e) {
            System.err.println("Error writing to output files: " + e.getMessage());
        }
    }

    private AlignmentAlgorithm.AlignmentResult computeAlignment(String seq1, String seq2) {
        if (useNWSW) {
            switch (mode) {
                case GLOBAL:
                    return new NW(seq1, seq2, algorithm).computeAlignment();
                case LOCAL:
                    return new SmithWaterman(seq1, seq2, algorithm).computeAlignment();
                case FREESHIFT:
                    return new SmithWaterman(seq1, seq2, algorithm).computeFreeshiftAlignment();
                default:
                    throw new IllegalStateException("Unknown alignment mode: " + mode);
            }
        } else {
            switch (mode) {
                case GLOBAL:
                    return algorithm.globalAlignment(seq1, seq2);
                case LOCAL:
                    return algorithm.localAlignment(seq1, seq2);
                case FREESHIFT:
                    return algorithm.freeShiftAlignment(seq1, seq2);
                default:
                    throw new IllegalStateException("Unknown alignment mode: " + mode);
            }
        }
    }

    private void outputResult(AlignmentPair pair, AlignmentAlgorithm.AlignmentResult result,
                              PrintWriter detailedWriter, PrintWriter simpleWriter) {
        switch (format) {
            case SCORE:
                outputScores(pair, result, detailedWriter);
                break;
            case ALI:
                outputAlignment(pair, result, detailedWriter, simpleWriter);
                break;
            case HTML:
                outputHTML(pair, result);
                break;
        }
    }

    private void outputScores(AlignmentPair pair, AlignmentAlgorithm.AlignmentResult result, PrintWriter writer) {
        String output = String.format("%s %s %.4f%n", pair.id1, pair.id2, result.score);
        System.out.print(output);
        writer.print(output);
    }

    private void outputAlignment(AlignmentPair pair, AlignmentAlgorithm.AlignmentResult result,
                                 PrintWriter detailedWriter, PrintWriter simpleWriter) {
        // Detailed format output
        String[] detailedOutputs = {
                String.format(">%s %s %.3f%n", pair.id1, pair.id2, result.score),
                String.format("%s: %s%n", pair.id1, result.seq1Aligned),
                String.format("%s: %s%n", pair.id2, result.seq2Aligned),
                System.lineSeparator()
        };

        for (String output : detailedOutputs) {
            System.out.print(output);
            detailedWriter.print(output);
        }

        // Simple format output
        String[] simpleOutputs = {
                String.format("%s:%s%n", pair.id1, result.seq1Aligned),
                String.format("%s:%s%n", pair.id2, result.seq2Aligned)
        };

        for (String output : simpleOutputs) {
            simpleWriter.print(output);
        }
    }

    private void outputHTML(AlignmentPair pair, AlignmentAlgorithm.AlignmentResult result) {
        String htmlFile = "alignment_results.html";
        try (PrintWriter writer = new PrintWriter(new FileWriter(htmlFile, true))) {
            // Write HTML header only for the first alignment
            if (pair == alignmentPairs.get(0)) {
                writer.println("<!DOCTYPE html>");
                writer.println("<html><head><style>");
                writer.println("body { font-family: monospace; }");
                writer.println(".match { background-color: #90EE90; }");
                writer.println(".positive { background-color: #FFB6C1; }");
                writer.println(".mismatch { background-color: #FFFFFF; }");
                writer.println("</style></head><body>");
            }

            writer.printf("<h2>Alignment: %s vs %s</h2>%n", pair.id1, pair.id2);
            writer.printf("<p>Score: %.4f</p>%n", result.score);
            writer.printf("<p>Length: %d</p>%n", result.alignmentLength);
            writer.printf("<p>Matches: %d (%.1f%%)</p>%n",
                    result.numMatches,
                    100.0 * result.numMatches / result.alignmentLength);
            writer.printf("<p>Positives: %d (%.1f%%)</p>%n",
                    result.numPositives,
                    100.0 * result.numPositives / result.alignmentLength);

            writer.println("<pre>");
            outputAlignmentHTML(writer, result);
            writer.println("</pre>");

            if (dpMatricesDir != null) {
                writer.println("<h3>Dynamic Programming Matrix</h3>");
                writer.println("<pre>");
                outputDPMatrixHTML(writer, result);
                writer.println("</pre>");
            }

            // Write HTML footer only for the last alignment
            if (pair == alignmentPairs.get(alignmentPairs.size() - 1)) {
                writer.println("</body></html>");
            }

            // Also print the alignment to console
            outputAlignment(pair, result, new PrintWriter(System.out), new PrintWriter(System.out));
        } catch (IOException e) {
            System.err.println("Error writing HTML output: " + e.getMessage());
        }
    }

    private void outputAlignmentHTML(PrintWriter writer, AlignmentAlgorithm.AlignmentResult result) {
        int lineLength = 60;
        for (int i = 0; i < result.alignmentLength; i += lineLength) {
            int end = Math.min(i + lineLength, result.alignmentLength);

            writer.printf("%-10s ", result.seq1Aligned.substring(i, end));
            writer.println();

            writer.print("           ");
            for (int j = i; j < end; j++) {
                char c1 = result.seq1Aligned.charAt(j);
                char c2 = result.seq2Aligned.charAt(j);
                if (c1 == c2 && c1 != '-') writer.print("|");
                else if (c1 != '-' && c2 != '-' &&
                        substitutionMatrix.getScore(c1, c2) > 0) writer.print("+");
                else writer.print(" ");
            }
            writer.println();

            writer.printf("%-10s ", result.seq2Aligned.substring(i, end));
            writer.println();
            writer.println();
        }
    }

    private void outputDPMatrix(AlignmentPair pair, AlignmentAlgorithm.AlignmentResult result) {
        String filename = String.format("%s/%s_%s_matrix.txt", dpMatricesDir, pair.id1, pair.id2);
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            for (int i = 0; i < result.dpMatrix.length; i++) {
                for (int j = 0; j < result.dpMatrix[i].length; j++) {
                    writer.printf("%8.2f ", result.dpMatrix[i][j]);
                }
                writer.println();
            }
        } catch (IOException e) {
            System.err.println("Error writing DP matrix: " + e.getMessage());
        }
    }

    private void outputDPMatrixHTML(PrintWriter writer, AlignmentAlgorithm.AlignmentResult result) {
        writer.println("<table border='1' cellpadding='5'>");
        for (int i = 0; i < result.dpMatrix.length; i++) {
            writer.println("<tr>");
            for (int j = 0; j < result.dpMatrix[i].length; j++) {
                String bgColor = result.backtrackMatrix[i][j] == 0 ? "#90EE90" :
                        result.backtrackMatrix[i][j] == 1 ? "#FFB6C1" : "#FFFFFF";
                writer.printf("<td bgcolor='%s'>%.2f</td>", bgColor, result.dpMatrix[i][j]);
            }
            writer.println("</tr>");
        }
        writer.println("</table>");
    }

    private static class AlignmentPair {
        final String id1;
        final String id2;

        AlignmentPair(String id1, String id2) {
            this.id1 = id1;
            this.id2 = id2;
        }
    }
}




