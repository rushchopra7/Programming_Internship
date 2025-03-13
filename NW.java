import java.util.*;

public class NW {
    private final String seq1;
    private final String seq2;
    private final double gapOpenPenalty;
    private final double gapExtendPenalty;
    private final AlignmentAlgorithm parentAlgorithm;

    public NW(String seq1, String seq2, AlignmentAlgorithm parent) {
        this.seq1 = seq1;
        this.seq2 = seq2;
        this.parentAlgorithm = parent;
        this.gapOpenPenalty = parent.getGapOpenPenalty();
        this.gapExtendPenalty = parent.getGapExtendPenalty();
    }

    public AlignmentAlgorithm.AlignmentResult computeAlignment() {
        int m = seq1.length();
        int n = seq2.length();
        double[][] dp = new double[m + 1][n + 1];
        int[][] backtrack = new int[m + 1][n + 1];

        // Initialize first row and column with gap opening + extension
        for (int i = 0; i <= m; i++) {
            dp[i][0] = gapOpenPenalty + i * gapExtendPenalty;
            backtrack[i][0] = 1;  // Up
        }
        for (int j = 0; j <= n; j++) {
            dp[0][j] = gapOpenPenalty + j * gapExtendPenalty;
            backtrack[0][j] = 2;  // Left
        }

        // Fill the matrix
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                double match = dp[i - 1][j - 1] + parentAlgorithm.getSubstitutionScore(seq1.charAt(i - 1), seq2.charAt(j - 1));
                double delete = dp[i - 1][j] + gapExtendPenalty;
                double insert = dp[i][j - 1] + gapExtendPenalty;

                // Find the maximum score and set backtrack pointer
                double maxScore = Double.NEGATIVE_INFINITY;
                int backtrackDir = -1;

                // Check vertical gap first (up)
                if (delete >= maxScore) {
                    maxScore = delete;
                    backtrackDir = 1;  // Up
                }

                // Check horizontal gap (left)
                if (insert >= maxScore) {
                    maxScore = insert;
                    backtrackDir = 2;  // Left
                }

                // Only use match/mismatch if it's strictly better than gaps
                if (match > maxScore) {
                    maxScore = match;
                    backtrackDir = 0;  // Diagonal
                }

                dp[i][j] = maxScore;
                backtrack[i][j] = backtrackDir;
            }
        }

        // Backtrack to get the alignment
        StringBuilder align1 = new StringBuilder();
        StringBuilder align2 = new StringBuilder();
        int i = m, j = n;

        while (i > 0 || j > 0) {
            if (i > 0 && j > 0) {
                if (backtrack[i][j] == 0) {
                    align1.insert(0, seq1.charAt(i - 1));
                    align2.insert(0, seq2.charAt(j - 1));
                    i--;
                    j--;
                } else if (backtrack[i][j] == 1) {
                    align1.insert(0, seq1.charAt(i - 1));
                    align2.insert(0, '-');
                    i--;
                } else {
                    align1.insert(0, '-');
                    align2.insert(0, seq2.charAt(j - 1));
                    j--;
                }
            } else if (i > 0) {
                align1.insert(0, seq1.charAt(i - 1));
                align2.insert(0, '-');
                i--;
            } else {
                align1.insert(0, '-');
                align2.insert(0, seq2.charAt(j - 1));
                j--;
            }
        }

        return parentAlgorithm.new AlignmentResult(
                align1.toString(),
                align2.toString(),
                dp[m][n],
                dp,
                backtrack
        );
    }
}
