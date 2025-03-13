import java.io.*;

public class Test {
    public static void main(String[] args) {
        try {

            SubstitutionMatrix matrix = new SubstitutionMatrix("C:/Users/rusha/PycharmProjects/pythonProject4/PBlock/Alignment/Propra/src/dayhoff.mat", false);

            AlignmentAlgorithm algorithm = new AlignmentAlgorithm(matrix, -11.0, -5.0);

            //String seq1 = "ADAAPTVSIFPPSSEQLTSGGASVVCFLNNFYPKDINVKWKIDGSERQNGVLNSWTDQDSKDSTYSMSSTLTLTKDEYERHNGYTCEATHKTSTSPIVKS";
            //String seq2 = "VPTPTNVTIESYNMNPIVYWEYQIMPQVPVFTVEVKNYGVKNSEWIDACINISHHYCNISDHVGDPSNSLWVRVKARVGQKESAYAKSEEFAVCRD";

            String seq1 = "RGHRFTKENVRILESWFAKNIENPYLDTKGLENLMKNTSLSRIQIKNWVSNRRRKEK";
            String seq2 = "RKFSADEDYTLAIAVKKQFYRDLFQIDPDTGRSLIRTQSRRGPIAREFFKHFAEEHAAHTENAWRDRFRKFLLAYGIDDYISYYEAEEPMKN";

            System.out.println("Testing Freeshift Alignment with Dayhoff matrix:");
            System.out.println("Sequence 1: " + seq1);
            System.out.println("Sequence 2: " + seq2);
            System.out.println();

            AlignmentAlgorithm.AlignmentResult result = algorithm.freeShiftAlignment(seq1, seq2);

            System.out.println("M Matrix:");
            printMatrix(result.dpMatrix);
            System.out.println();

            System.out.println("E Matrix:");
            printMatrix(algorithm.getAlignmentMatrices(seq1, seq2).E);
            System.out.println();

            System.out.println("F Matrix:");
            printMatrix(algorithm.getAlignmentMatrices(seq1, seq2).F);
            System.out.println();

            System.out.println("Backtrack Matrix:");
            printBacktrackMatrix(result.backtrackMatrix);
            System.out.println();

            // Print alignment
            System.out.println("Alignment:");
            System.out.println(result.seq1Aligned);
            System.out.println(result.seq2Aligned);
            System.out.println();

            System.out.println("Score: " + result.score);
            System.out.println("Alignment Length: " + result.alignmentLength);
            System.out.println("Matches: " + result.numMatches);
            System.out.println("Positives: " + result.numPositives);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printMatrix(double[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                if (matrix[i][j] == Double.NEGATIVE_INFINITY) {
                    System.out.printf("%8s", "-inf");
                } else {
                    System.out.printf("%8.2f", matrix[i][j]);
                }
            }
            System.out.println();
        }
    }

    private static void printBacktrackMatrix(int[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                System.out.printf("%2d", matrix[i][j]);
            }
            System.out.println();
        }
    }
}

