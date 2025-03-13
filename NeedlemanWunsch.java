import java.util.List;
import java.util.stream.IntStream;

import java.util.*;

public class NeedlemanWunsch {

    // scoring scheme
    final static int match = 3;
    final static int mismatch = -2;
    final static int gap = -4;

    // sequences to be analyzed
    public String sequence1;
    public String sequence2;

    public NeedlemanWunsch(String sequence1, String sequence2) {
        this.sequence1 = sequence1;
        this.sequence2 = sequence2;
    }

    // Base case function
    public static int base_case(String s, String p) {
        if (s.isEmpty()) return p.length() * gap;
        else if (p.isEmpty()) return s.length() * gap;
        return 0; // both empty
    }

    // Recursive function for calculating optimal alignment score
    public static int Recursion(String s, String p) {
        int score_base = base_case(s, p);
        if (score_base != 0) {
            return score_base; // if it's a base case, return the calculated score
        } else {
            // Only call substring if the length of the strings is greater than 0
            if (s.length() > 0 && p.length() > 0) {
                String sstripP = s.substring(0, s.length() - 1);
                String pstripQ = p.substring(0, p.length() - 1);

                // Recursively calculate three possible cases: insertion, deletion, and comparison
                int insertion = Recursion(sstripP, p) + gap;
                int deletion = Recursion(s, pstripQ) + gap;
                int comparison = Recursion(sstripP, pstripQ) +
                        scoringFunction(s.charAt(s.length() - 1), p.charAt(p.length() - 1));

                return Math.max(Math.max(insertion, deletion), comparison);
            }
        }
        return 0; // Return 0 for empty strings
    }


    // Function to calculate dynamic programming matrix
    public static int[][] DPMatrix(String p, String q) {
        int[][] dp = new int[p.length() + 1][q.length() + 1];

        initializeBorders(dp, p.length(), q.length());
        fillMatrix(dp, p, q);

        return dp;
    }

    // Initialize borders with gap penalties
    private static void initializeBorders(int[][] dp, int m, int n) {
        for (int i = 0; i <= m; i++) dp[i][0] = i * gap;
        for (int j = 0; j <= n; j++) dp[0][j] = j * gap;
    }

    // Fill the matrix using dynamic programming
    private static void fillMatrix(int[][] dp, String p, String q) {
        for (int i = 1; i <= p.length(); i++) {
            for (int j = 1; j <= q.length(); j++) {
                dp[i][j] = calculateCell(dp, p, q, i, j);
            }
        }
    }

    // Calculate the value for each cell in the matrix
    private static int calculateCell(int[][] dp, String p, String q, int i, int j) {
        int diagonal = dp[i - 1][j - 1] + scoringFunction(p.charAt(i - 1), q.charAt(j - 1));
        int up = dp[i - 1][j] + gap;
        int left = dp[i][j - 1] + gap;
        return Math.max(Math.max(diagonal, up), left);
    }

    // Scoring function
    public static int scoringFunction(char x, char y) {
        return (x == y) ? match : mismatch;
    }

    // Printing the DP matrix
    static void printingMatrix(int[][] matrix, int rows, int cols) {
        System.out.println("\nDP Matrix:");
        for (int i = 0; i <= rows; i++) {
            for (int j = 0; j <= cols; j++) {
                System.out.print(String.format("%3d", matrix[i][j]) + " ");
            }
            System.out.println();
        }
    }

    // Backtrack function for generating the alignment
    static void backtrackAlignment(int i, int j, String p, String q, int[][] dp,
                                   String alignP, String alignQ, List<String> result) {
        if (i == 0 && j == 0) {
            result.add(new StringBuilder(alignP).reverse().toString());
            result.add(new StringBuilder(alignQ).reverse().toString());
            return;
        }

        // Check diagonal (match/mismatch) move
        if (i > 0 && j > 0 && dp[i][j] == dp[i - 1][j - 1] +
                scoringFunction(p.charAt(i - 1), q.charAt(j - 1))) {
            backtrackAlignment(i - 1, j - 1, p, q, dp,
                    alignP + p.charAt(i - 1), alignQ + q.charAt(j - 1), result);
        }
        // Check vertical move (gap in q)
        else if (i > 0 && dp[i][j] == dp[i - 1][j] + gap) {
            backtrackAlignment(i - 1, j, p, q, dp,
                    alignP + p.charAt(i - 1), alignQ + "-", result);
        }
        // Check horizontal move (gap in p)
        else {
            backtrackAlignment(i, j - 1, p, q, dp,
                    alignP + "-", alignQ + q.charAt(j - 1), result);
        }
    }

    // Main method to test the implementation
    public static void main(String[] args) {
        System.out.println("Needleman-Wunsch Algorithm");

        // Example sequences
        String p = "TATAAT";
        String q = "TTACGTAAGC";

        // Call recursive function for the alignment score
        System.out.println("Recursive alignment score: " + Recursion(p, q));

        // Generate DP matrix
        int[][] dpMatrix = DPMatrix(p, q);
        printingMatrix(dpMatrix, p.length(), q.length());

        // Backtrack to get the best alignment
        List<String> result = new ArrayList<>();
        backtrackAlignment(p.length(), q.length(), p, q, dpMatrix, "", "", result);

        System.out.println("Optimal Alignment:");
        System.out.println(result.get(0));
        System.out.println(result.get(1));
    }
}
