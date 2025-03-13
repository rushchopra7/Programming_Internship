public class NwDp {
    private static final int MATCH_SCORE = 3;
    private static final int MISMATCH_SCORE = -2;
    private static final int GAP_PENALTY = -4;
    
    public static class AlignmentResult {
        public final String alignment1;
        public final String alignment2;
        public final int score;
        
        public AlignmentResult(String alignment1, String alignment2, int score) {
            this.alignment1 = alignment1;
            this.alignment2 = alignment2;
            this.score = score;
        }
    }

    public static AlignmentResult computeAlignment(String seq1, String seq2) {
        int m = seq1.length();
        int n = seq2.length();

        int[][] dp = new int[m + 1][n + 1];
        char[][] backtrack = new char[m + 1][n + 1];

        for (int i = 0; i <= m; i++) {
            dp[i][0] = i * GAP_PENALTY;
            backtrack[i][0] = 'U';
        }
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j * GAP_PENALTY;
            backtrack[0][j] = 'L';
        }

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                int match = dp[i-1][j-1] + getScore(seq1.charAt(i-1), seq2.charAt(j-1));
                int delete = dp[i-1][j] + GAP_PENALTY;
                int insert = dp[i][j-1] + GAP_PENALTY;
                
                dp[i][j] = Math.max(Math.max(match, delete), insert);

                if (dp[i][j] == match) {
                    backtrack[i][j] = 'D';
                } else if (dp[i][j] == delete) {
                    backtrack[i][j] = 'U';
                } else {
                    backtrack[i][j] = 'L';
                }
            }
        }

        StringBuilder align1 = new StringBuilder();
        StringBuilder align2 = new StringBuilder();
        
        int i = m;
        int j = n;
        
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && backtrack[i][j] == 'D') {
                align1.insert(0, seq1.charAt(i-1));
                align2.insert(0, seq2.charAt(j-1));
                i--; j--;
            } else if (i > 0 && backtrack[i][j] == 'U') {
                align1.insert(0, seq1.charAt(i-1));
                align2.insert(0, '-');
                i--;
            } else {
                align1.insert(0, '-');
                align2.insert(0, seq2.charAt(j-1));
                j--;
            }
        }
        
        return new AlignmentResult(align1.toString(), align2.toString(), dp[m][n]);
    }

    private static int getScore(char a, char b) {
        return a == b ? MATCH_SCORE : MISMATCH_SCORE;
    }
    
    public static void main(String[] args) {
        String signal = "TATAAT";
        String sequence = "TTACGTAAGC";
        
        AlignmentResult result = computeAlignment(signal, sequence);
        
        System.out.println("Aligning -10 signal TATAAT with sequence TTACGTAAGC");
        System.out.println("Optimal alignment score: " + result.score);
        System.out.println("Alignment:");
        System.out.println(result.alignment1);
        System.out.println(result.alignment2);

        StringBuilder markers = new StringBuilder();
        for (int i = 0; i < result.alignment1.length(); i++) {
            if (result.alignment1.charAt(i) == '-' || result.alignment2.charAt(i) == '-') {
                markers.append(' ');
            } else {
                markers.append(result.alignment1.charAt(i) == result.alignment2.charAt(i) ? '|' : 'x');
            }
        }
        System.out.println(markers.toString());
    }
}
