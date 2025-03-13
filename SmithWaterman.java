public class SmithWaterman {
    private final String seq1;
    private final String seq2;
    private final double gapOpenPenalty;
    private final double gapExtendPenalty;
    private final AlignmentAlgorithm parentAlgorithm;

    public SmithWaterman(String seq1, String seq2, AlignmentAlgorithm parent) {
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

        for (int i = 0; i <= m; i++) {
            dp[i][0] = 0;
            backtrack[i][0] = 3;
        }
        for (int j = 0; j <= n; j++) {
            dp[0][j] = 0;
            backtrack[0][j] = 3;
        }

        double maxScore = 0;
        int maxI = 0, maxJ = 0;

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                double match = dp[i - 1][j - 1] + parentAlgorithm.getSubstitutionScore(seq1.charAt(i - 1), seq2.charAt(j - 1));
                double delete = dp[i - 1][j] + gapExtendPenalty;
                double insert = dp[i][j - 1] + gapExtendPenalty;

                double currentScore = 0;
                int backtrackDir = 3;

                if (delete >= currentScore) {
                    currentScore = delete;
                    backtrackDir = 1;
                }

                if (insert >= currentScore) {
                    currentScore = insert;
                    backtrackDir = 2;
                }

                if (match > currentScore) {
                    currentScore = match;
                    backtrackDir = 0;
                }

                dp[i][j] = currentScore;
                backtrack[i][j] = backtrackDir;

                if (currentScore > maxScore) {
                    maxScore = currentScore;
                    maxI = i;
                    maxJ = j;
                }
            }
        }


        StringBuilder align1 = new StringBuilder();
        StringBuilder align2 = new StringBuilder();
        int i = maxI, j = maxJ;

        while (i > 0 && j > 0 && backtrack[i][j] != 3) {
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
        }

        StringBuilder completeAlign1 = new StringBuilder();
        StringBuilder completeAlign2 = new StringBuilder();

        if (i > 0) {
            completeAlign1.append(seq1.substring(0, i));
            completeAlign2.append("-".repeat(i));
        }

        completeAlign1.append(align1);
        completeAlign2.append(align2);

        if (maxI < seq1.length()) {
            completeAlign1.append(seq1.substring(maxI));
            completeAlign2.append("-".repeat(seq1.length() - maxI));
        }

        if (j > 0) {
            completeAlign1.insert(0, "-".repeat(j));
            completeAlign2.insert(0, seq2.substring(0, j));
        }

        if (maxJ < seq2.length()) {
            completeAlign1.append("-".repeat(seq2.length() - maxJ));
            completeAlign2.append(seq2.substring(maxJ));
        }

        int maxLength = Math.max(completeAlign1.length(), completeAlign2.length());
        while (completeAlign1.length() < maxLength) {
            completeAlign1.append("-");
        }
        while (completeAlign2.length() < maxLength) {
            completeAlign2.append("-");
        }

        return parentAlgorithm.new AlignmentResult(
            completeAlign1.toString(), 
            completeAlign2.toString(), 
            maxScore,
            dp,
            backtrack
        );
    }

    public AlignmentAlgorithm.AlignmentResult computeFreeshiftAlignment() {
        int m = seq1.length();
        int n = seq2.length();
        double[][] dp = new double[m + 1][n + 1];
        int[][] backtrack = new int[m + 1][n + 1];

        for (int i = 0; i <= m; i++) {
            dp[i][0] = 0;
        }
        for (int j = 0; j <= n; j++) {
            dp[0][j] = 0;
        }

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                double match = dp[i - 1][j - 1] + parentAlgorithm.getSubstitutionScore(seq1.charAt(i - 1), seq2.charAt(j - 1));
                double delete = dp[i - 1][j] + gapExtendPenalty;
                double insert = dp[i][j - 1] + gapExtendPenalty;

                double maxScore = match;
                int backtrackDir = 0;

                if (delete > maxScore) {
                    maxScore = delete;
                    backtrackDir = 1;
                }

                if (insert > maxScore) {
                    maxScore = insert;
                    backtrackDir = 2;
                }

                dp[i][j] = maxScore;
                backtrack[i][j] = backtrackDir;
            }
        }

        double maxScore = Double.NEGATIVE_INFINITY;
        int maxI = m, maxJ = n;

        for (int j = 0; j <= n; j++) {
            if (dp[m][j] > maxScore) {
                maxScore = dp[m][j];
                maxI = m;
                maxJ = j;
            }
        }

        for (int i = 0; i <= m; i++) {
            if (dp[i][n] > maxScore) {
                maxScore = dp[i][n];
                maxI = i;
                maxJ = n;
            }
        }

        StringBuilder align1 = new StringBuilder();
        StringBuilder align2 = new StringBuilder();
        int i = maxI, j = maxJ;

        if (j < n) {
            align1.append("-".repeat(n - j));
            align2.append(seq2.substring(j));
        } else if (i < m) {
            align1.append(seq1.substring(i));
            align2.append("-".repeat(m - i));
        }

        while (i > 0 || j > 0) {
            if (i > 0 && j > 0) {
                if (backtrack[i][j] == 0) {
                    align1.insert(0, seq1.charAt(i - 1));
                    align2.insert(0, seq2.charAt(j - 1));
                    i--; j--;
                } else if (backtrack[i][j] == 1) {
                    align1.insert(0, seq1.charAt(i - 1));
                    align2.insert(0, '-');
                    i--;
                } else if (backtrack[i][j] == 2) {
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
            maxScore,
            dp,
            backtrack
        );
    }
}