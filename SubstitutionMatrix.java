import java.io.*;
import java.util.*;

public class SubstitutionMatrix {
    private final double[][] matrix;
    private final Map<Character, Integer> aminoAcidIndices;
    private final List<Character> aminoAcids;
    private final boolean debug;

    public SubstitutionMatrix(String filename, boolean debug) throws IOException {
        this.debug = debug;
        aminoAcidIndices = new HashMap<>();
        aminoAcids = new ArrayList<>();
        matrix = loadMatrix(filename);
    }

    private double[][] loadMatrix(String filename) throws IOException {
        List<String> lines = new ArrayList<>();
        String rowIndex = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                if (line.startsWith("ROWINDEX")) {
                    rowIndex = line.substring("ROWINDEX".length()).trim();
                    continue;
                }

                if (line.startsWith("MATRIX")) {
                    String matrixLine = line.substring("MATRIX".length()).trim();
                    lines.add(matrixLine);
                }
            }
        }

        if (rowIndex == null) {
            throw new IOException("No ROWINDEX found in matrix file");
        }

        for (int i = 0; i < rowIndex.length(); i++) {
            char aa = rowIndex.charAt(i);
            aminoAcidIndices.put(aa, i);
            aminoAcids.add(aa);
        }

        int size = aminoAcids.size();
        double[][] result = new double[size][size];

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            List<String> numericValues = Arrays.asList(line.split("\\s+"));

            for (int j = 0; j < i + 1; j++) {
                try {
                    String value = numericValues.get(j);
                    if (value.endsWith(".")) {
                        value = value.substring(0, value.length() - 1);
                    }
                    result[i][j] = Double.parseDouble(value);
                    result[j][i] = result[i][j];
                } catch (NumberFormatException e) {
                    System.err.println("Error parsing value at position [" + i + "][" + j + "]: '" + numericValues.get(j) + "'");
                    throw new IOException("Invalid number format in matrix at row " + (i + 1) + ", column " + (j + 1));
                } catch (IndexOutOfBoundsException e) {
                    throw new IOException("Matrix row " + (i + 1) + " has insufficient values");
                }
            }
        }

        return result;
    }

    public double getScore(char aa1, char aa2) {
        Integer index1 = aminoAcidIndices.get(aa1);
        Integer index2 = aminoAcidIndices.get(aa2);

        if (index1 == null || index2 == null) {
            throw new IllegalArgumentException("Unknown amino acid: " +
                    (index1 == null ? aa1 : aa2));
        }

        return matrix[index1][index2];
    }

    public Set<Character> getAminoAcids() {
        return new HashSet<>(aminoAcids);
    }

    public boolean isValidAminoAcid(char aa) {
        return aminoAcidIndices.containsKey(aa);
    }
}
