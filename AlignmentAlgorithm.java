public class AlignmentAlgorithm {
    private final SubstitutionMatrix substitutionMatrix;
    private final double gapOpenPenalty;
    private final double gapExtendPenalty;

    public AlignmentAlgorithm(SubstitutionMatrix matrix, double gapOpen, double gapExtend) {
        this.substitutionMatrix = matrix;
        this.gapOpenPenalty = gapOpen;
        this.gapExtendPenalty = gapExtend;
    }

    public class GotohMatrices {
        double[][] M;
        double[][] E;
        double[][] F;
        int[][] backtrack;
        
        GotohMatrices(int m, int n) {
            M = new double[m + 1][n + 1];
            E = new double[m + 1][n + 1];
            F = new double[m + 1][n + 1];
            backtrack = new int[m + 1][n + 1];
            
            // Initialize with negative infinity
            for (int i = 0; i <= m; i++) {
                for (int j = 0; j <= n; j++) {
                    E[i][j] = Double.NEGATIVE_INFINITY;
                    F[i][j] = Double.NEGATIVE_INFINITY;
                }
            }
        }
    }

    public class AlignmentResult {
        public final String seq1Aligned;
        public final String seq2Aligned;
        public final double score;
        public final double[][] dpMatrix;
        public final int[][] backtrackMatrix;
        public final int alignmentLength;
        public final int numMatches;
        public final int numPositives;

        public AlignmentResult(String seq1Aligned, String seq2Aligned, double score,
                             double[][] dpMatrix, int[][] backtrackMatrix) {
            this.seq1Aligned = seq1Aligned;
            this.seq2Aligned = seq2Aligned;
            this.score = score;
            this.dpMatrix = dpMatrix;
            this.backtrackMatrix = backtrackMatrix;
            
            this.alignmentLength = seq1Aligned.length();
            int matches = 0;
            int positives = 0;
            for (int i = 0; i < alignmentLength; i++) {
                char c1 = seq1Aligned.charAt(i);
                char c2 = seq2Aligned.charAt(i);
                if (c1 != '-' && c2 != '-') {
                    if (c1 == c2) matches++;
                    if (substitutionMatrix.getScore(c1, c2) > 0) positives++;
                }
            }
            this.numMatches = matches;
            this.numPositives = positives;
        }
    }

    public AlignmentResult globalAlignment(String seq1, String seq2) {
        int m = seq1.length();
        int n = seq2.length();
        GotohMatrices matrices = new GotohMatrices(m, n);

        matrices.M[0][0] = 0;
        for (int i = 1; i <= m; i++) {
            matrices.M[i][0] = gapOpenPenalty + i * gapExtendPenalty;
            matrices.F[i][0] = matrices.M[i][0];
            matrices.E[i][0] = Double.NEGATIVE_INFINITY;
            matrices.backtrack[i][0] = 1;  // Up
        }
        for (int j = 1; j <= n; j++) {
            matrices.M[0][j] = gapOpenPenalty + j * gapExtendPenalty;
            matrices.E[0][j] = matrices.M[0][j];
            matrices.F[0][j] = Double.NEGATIVE_INFINITY;
            matrices.backtrack[0][j] = 2;  // Left
        }

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                double openE = matrices.M[i][j-1] + gapOpenPenalty + gapExtendPenalty;
                double extendE = matrices.E[i][j-1] + gapExtendPenalty;
                matrices.E[i][j] = Math.max(openE, extendE);


                double openF = matrices.M[i-1][j] + gapOpenPenalty + gapExtendPenalty;
                double extendF = matrices.F[i-1][j] + gapExtendPenalty;
                matrices.F[i][j] = Math.max(openF, extendF);


                double match = matrices.M[i-1][j-1] + substitutionMatrix.getScore(seq1.charAt(i-1), seq2.charAt(j-1));

                double maxScore = Double.NEGATIVE_INFINITY;
                int backtrack = -1;

                if (matrices.F[i][j] >= maxScore) {
                    maxScore = matrices.F[i][j];
                    backtrack = 1;
                }

                if (matrices.E[i][j] >= maxScore) {
                    maxScore = matrices.E[i][j];
                    backtrack = 2;
                }

                if (match > maxScore) {
                    maxScore = match;
                    backtrack = 0;
                }

                matrices.M[i][j] = maxScore;
                matrices.backtrack[i][j] = backtrack;
            }
        }

        return backtrack(matrices, seq1, seq2, m, n, matrices.M[m][n]);
    }

    public AlignmentResult localAlignment(String seq1, String seq2) {
        int m = seq1.length();
        int n = seq2.length();
        GotohMatrices matrices = new GotohMatrices(m, n);
        for (int i = 0; i <= m; i++) {
            matrices.M[i][0] = 0;
            matrices.E[i][0] = Double.NEGATIVE_INFINITY;
            matrices.F[i][0] = Double.NEGATIVE_INFINITY;
        }
        for (int j = 0; j <= n; j++) {
            matrices.M[0][j] = 0;
            matrices.E[0][j] = Double.NEGATIVE_INFINITY;
            matrices.F[0][j] = Double.NEGATIVE_INFINITY;
        }

        double maxScore = 0;
        int maxI = 0, maxJ = 0;

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                matrices.E[i][j] = Math.max(
                    matrices.M[i][j-1] + gapOpenPenalty + gapExtendPenalty,
                    matrices.E[i][j-1] + gapExtendPenalty
                );

                matrices.F[i][j] = Math.max(
                    matrices.M[i-1][j] + gapOpenPenalty + gapExtendPenalty,
                    matrices.F[i-1][j] + gapExtendPenalty
                );

                double match = matrices.M[i-1][j-1] + substitutionMatrix.getScore(seq1.charAt(i-1), seq2.charAt(j-1));
                matrices.M[i][j] = Math.max(0, Math.max(Math.max(match, matrices.E[i][j]), matrices.F[i][j]));

                if (matrices.M[i][j] > maxScore) {
                    maxScore = matrices.M[i][j];
                    maxI = i;
                    maxJ = j;
                }

                if (matrices.M[i][j] == 0) matrices.backtrack[i][j] = 3;
                else if (matrices.M[i][j] == match) matrices.backtrack[i][j] = 0;
                else if (matrices.M[i][j] == matrices.F[i][j]) matrices.backtrack[i][j] = 1;
                else matrices.backtrack[i][j] = 2;
            }
        }

        return backtrackLocal(matrices, seq1, seq2, maxI, maxJ, maxScore);
    }

    public AlignmentResult freeShiftAlignment(String seq1, String seq2) {
        int m = seq1.length();
        int n = seq2.length();
        GotohMatrices matrices = new GotohMatrices(m, n);

        for (int i = 0; i <= m; i++) {
            matrices.M[i][0] = 0;
            matrices.E[i][0] = Double.NEGATIVE_INFINITY;
            matrices.F[i][0] = Double.NEGATIVE_INFINITY;
        }
        for (int j = 0; j <= n; j++) {
            matrices.M[0][j] = 0;
            matrices.E[0][j] = Double.NEGATIVE_INFINITY;
            matrices.F[0][j] = Double.NEGATIVE_INFINITY;
        }
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                double openE = matrices.M[i][j-1] + gapOpenPenalty + gapExtendPenalty;
                double extendE = matrices.E[i][j-1] + gapExtendPenalty;
                matrices.E[i][j] = Math.max(openE, extendE);


                double openF = matrices.M[i-1][j] + gapOpenPenalty + gapExtendPenalty;
                double extendF = matrices.F[i-1][j] + gapExtendPenalty;
                matrices.F[i][j] = Math.max(openF, extendF);

                double match = matrices.M[i-1][j-1] + substitutionMatrix.getScore(seq1.charAt(i-1), seq2.charAt(j-1));

                double maxScore = Double.NEGATIVE_INFINITY;
                int backtrack = -1;

                if (matrices.F[i][j] >= maxScore) {
                    maxScore = matrices.F[i][j];
                    backtrack = 1;
                }

                if (matrices.E[i][j] >= maxScore) {
                    maxScore = matrices.E[i][j];
                    backtrack = 2;
                }

                if (match > maxScore) {
                    maxScore = match;
                    backtrack = 0;
                }

                matrices.M[i][j] = maxScore;
                matrices.backtrack[i][j] = backtrack;
            }
        }

        double maxScore = Double.NEGATIVE_INFINITY;
        int maxI = m, maxJ = n;


        for (int j = 0; j <= n; j++) {
            if (matrices.M[m][j] > maxScore) {
                maxScore = matrices.M[m][j];
                maxI = m;
                maxJ = j;
            }
        }

        for (int i = 0; i <= m; i++) {
            if (matrices.M[i][n] > maxScore) {
                maxScore = matrices.M[i][n];
                maxI = i;
                maxJ = n;
            }
        }

        return backtrackFreeshift(matrices, seq1, seq2, maxI, maxJ, maxScore);
    }

    private AlignmentResult backtrack(GotohMatrices matrices, String seq1, String seq2, 
                                    int i, int j, double score) {
        StringBuilder align1 = new StringBuilder();
        StringBuilder align2 = new StringBuilder();
        String currentMatrix = "M";
        
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0) {
                if (currentMatrix.equals("M")) {
                    if (Math.abs(matrices.M[i][j] - matrices.F[i][j]) < 0.0001) {
                        currentMatrix = "F";
                    } else if (Math.abs(matrices.M[i][j] - matrices.E[i][j]) < 0.0001) {
                        currentMatrix = "E";
                    } else {
                        align1.insert(0, seq1.charAt(i-1));
                        align2.insert(0, seq2.charAt(j-1));
                        i--; j--;
                        continue;
                    }
                }
                
                if (currentMatrix.equals("F")) {
                    double gapOpenScore = matrices.M[i-1][j] + gapOpenPenalty + gapExtendPenalty;
                    double gapExtendScore = matrices.F[i-1][j] + gapExtendPenalty;
                    
                    align1.insert(0, seq1.charAt(i-1));
                    align2.insert(0, '-');
                    i--;
                    
                    if (Math.abs(matrices.F[i+1][j] - gapOpenScore) < 0.0001) {
                        currentMatrix = "M";
                    }
                    continue;
                }
                
                if (currentMatrix.equals("E")) {
                    double gapOpenScore = matrices.M[i][j-1] + gapOpenPenalty + gapExtendPenalty;
                    double gapExtendScore = matrices.E[i][j-1] + gapExtendPenalty;
                    
                    align1.insert(0, '-');
                    align2.insert(0, seq2.charAt(j-1));
                    j--;
                    
                    if (Math.abs(matrices.E[i][j+1] - gapOpenScore) < 0.0001) {
                        currentMatrix = "M";
                    }
                    continue;
                }
            }

            if (i > 0) {
                align1.insert(0, seq1.charAt(i-1));
                align2.insert(0, '-');
                i--;
            } else if (j > 0) {
                align1.insert(0, '-');
                align2.insert(0, seq2.charAt(j-1));
                j--;
            }
        }

        return new AlignmentResult(align1.toString(), align2.toString(), score, 
                                 matrices.M, matrices.backtrack);
    }

    private AlignmentResult backtrackLocal(GotohMatrices matrices, String seq1, String seq2,
                                         int maxI, int maxJ, double score) {
        StringBuilder align1 = new StringBuilder();
        StringBuilder align2 = new StringBuilder();
        int i = maxI, j = maxJ;
        String currentMatrix = "M";

        // First, build the local alignment
        while (i > 0 && j > 0) {
            if (currentMatrix.equals("M")) {
                if (Math.abs(matrices.M[i][j] - matrices.F[i][j]) < 0.0001) {
                    currentMatrix = "F";
                } else if (Math.abs(matrices.M[i][j] - matrices.E[i][j]) < 0.0001) {
                    currentMatrix = "E";
                } else if (matrices.M[i][j] == 0) {
                    break;
                } else {
                    align1.insert(0, seq1.charAt(i-1));
                    align2.insert(0, seq2.charAt(j-1));
                    i--; j--;
                    continue;
                }
            }
            
            if (currentMatrix.equals("F")) {
                double gapOpenScore = matrices.M[i-1][j] + gapOpenPenalty + gapExtendPenalty;
                double gapExtendScore = matrices.F[i-1][j] + gapExtendPenalty;
                
                align1.insert(0, seq1.charAt(i-1));
                align2.insert(0, '-');
                i--;
                
                if (Math.abs(matrices.F[i+1][j] - gapOpenScore) < 0.0001) {
                    currentMatrix = "M";
                }
                continue;
            }
            
            if (currentMatrix.equals("E")) {
                double gapOpenScore = matrices.M[i][j-1] + gapOpenPenalty + gapExtendPenalty;
                double gapExtendScore = matrices.E[i][j-1] + gapExtendPenalty;
                
                align1.insert(0, '-');
                align2.insert(0, seq2.charAt(j-1));
                j--;
                
                if (Math.abs(matrices.E[i][j+1] - gapOpenScore) < 0.0001) {
                    currentMatrix = "M";
                }
                continue;
            }
        }

        // Add the complete sequences with gaps
        StringBuilder completeAlign1 = new StringBuilder();
        StringBuilder completeAlign2 = new StringBuilder();

        // Add leading part of seq1
        if (i > 0) {
            completeAlign1.append(seq1.substring(0, i));
            completeAlign2.append("-".repeat(i));
        }

        // Add the local alignment
        completeAlign1.append(align1);
        completeAlign2.append(align2);

        // Add trailing part of seq1
        if (maxI < seq1.length()) {
            completeAlign1.append(seq1.substring(maxI));
            completeAlign2.append("-".repeat(seq1.length() - maxI));
        }

        // Add leading part of seq2
        if (j > 0) {
            completeAlign1.insert(0, "-".repeat(j));
            completeAlign2.insert(0, seq2.substring(0, j));
        }

        // Add trailing part of seq2
        if (maxJ < seq2.length()) {
            completeAlign1.append("-".repeat(seq2.length() - maxJ));
            completeAlign2.append(seq2.substring(maxJ));
        }

        // Ensure both sequences have the same length
        int maxLength = Math.max(completeAlign1.length(), completeAlign2.length());
        while (completeAlign1.length() < maxLength) {
            completeAlign1.append("-");
        }
        while (completeAlign2.length() < maxLength) {
            completeAlign2.append("-");
        }

        return new AlignmentResult(completeAlign1.toString(), completeAlign2.toString(), score,
                                 matrices.M, matrices.backtrack);
    }

    private AlignmentResult backtrackFreeshift(GotohMatrices matrices, String seq1, String seq2,
                                             int maxI, int maxJ, double score) {
        StringBuilder align1 = new StringBuilder();
        StringBuilder align2 = new StringBuilder();
        int i = maxI, j = maxJ;
        String currentMatrix = "M";

        if (j < seq2.length()) {
            align1.append("-".repeat(seq2.length() - j));
            align2.append(seq2.substring(j));
        } else if (i < seq1.length()) {
            align1.append(seq1.substring(i));
            align2.append("-".repeat(seq1.length() - i));
        }

        while (i > 0 && j > 0) {
            if (currentMatrix.equals("M")) {
                if (Math.abs(matrices.M[i][j] - matrices.F[i][j]) < 0.0001) {
                    currentMatrix = "F";
                } else if (Math.abs(matrices.M[i][j] - matrices.E[i][j]) < 0.0001) {
                    currentMatrix = "E";
                } else if (i==0 || j==0) {
                    break;
                } else {
                    align1.insert(0, seq1.charAt(i-1));
                    align2.insert(0, seq2.charAt(j-1));
                    i--; j--;
                    continue;
                }
            }
            
            if (currentMatrix.equals("F")) {
                double gapOpenScore = matrices.M[i-1][j] + gapOpenPenalty + gapExtendPenalty;
                double gapExtendScore = matrices.F[i-1][j] + gapExtendPenalty;
                
                align1.insert(0, seq1.charAt(i-1));
                align2.insert(0, '-');
                i--;
                
                if (Math.abs(matrices.F[i+1][j] - gapOpenScore) < 0.0001) {
                    currentMatrix = "M";
                }
                continue;
            }
            
            if (currentMatrix.equals("E")) {
                double gapOpenScore = matrices.M[i][j-1] + gapOpenPenalty + gapExtendPenalty;
                double gapExtendScore = matrices.E[i][j-1] + gapExtendPenalty;
                
                align1.insert(0, '-');
                align2.insert(0, seq2.charAt(j-1));
                j--;
                
                if (Math.abs(matrices.E[i][j+1] - gapOpenScore) < 0.0001) {
                    currentMatrix = "M";
                }
                continue;
            }
        }

        if (i > 0) {
            align1.insert(0, seq1.substring(0, i));
            align2.insert(0, "-".repeat(i));
        } else if (j > 0) {
            align1.insert(0, "-".repeat(j));
            align2.insert(0, seq2.substring(0, j));
        }

        return new AlignmentResult(align1.toString(), align2.toString(), score,
                                 matrices.M, matrices.backtrack);
    }

    public double getGapOpenPenalty() {
        return gapOpenPenalty;
    }

    public double getGapExtendPenalty() {
        return gapExtendPenalty;
    }

    public double getSubstitutionScore(char aa1, char aa2) {
        return substitutionMatrix.getScore(aa1, aa2);
    }

    private String padWithGaps(String seq, int targetLength, boolean padEnd) {
        if (seq.length() >= targetLength) return seq;
        String gaps = "-".repeat(targetLength - seq.length());
        return padEnd ? seq + gaps : gaps + seq;
    }

    public double checkScore(String seq1Aligned, String seq2Aligned) {
        if (seq1Aligned.length() != seq2Aligned.length()) {
            throw new IllegalArgumentException("Aligned sequences must have the same length");
        }

        double score = 0;
        boolean inGap = false;
        
        int start = 0;
        int end = seq1Aligned.length() - 1;

        while (start < seq1Aligned.length() && 
               (seq1Aligned.charAt(start) == '-' || seq2Aligned.charAt(start) == '-')) {
            start++;
        }

        while (end >= 0 && 
               (seq1Aligned.charAt(end) == '-' || seq2Aligned.charAt(end) == '-')) {
            end--;
        }

        for (int i = start; i <= end; i++) {
            char c1 = seq1Aligned.charAt(i);
            char c2 = seq2Aligned.charAt(i);
            
            if (c1 == '-' || c2 == '-') {
                if (!inGap) {
                    score += gapOpenPenalty + gapExtendPenalty;
                    inGap = true;
                } else {
                    score += gapExtendPenalty;
                }
            } else {
                score += substitutionMatrix.getScore(c1, c2);
                inGap = false;
            }
        }
        
        return score;
    }

    public GotohMatrices getAlignmentMatrices(String seq1, String seq2) {
        int m = seq1.length();
        int n = seq2.length();
        GotohMatrices matrices = new GotohMatrices(m, n);

        for (int i = 0; i <= m; i++) {
            matrices.M[i][0] = 0;
            matrices.E[i][0] = Double.NEGATIVE_INFINITY;
            matrices.F[i][0] = Double.NEGATIVE_INFINITY;
        }
        for (int j = 0; j <= n; j++) {
            matrices.M[0][j] = 0;
            matrices.E[0][j] = Double.NEGATIVE_INFINITY;
            matrices.F[0][j] = Double.NEGATIVE_INFINITY;
        }

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                double openE = matrices.M[i][j-1] + gapOpenPenalty + gapExtendPenalty;
                double extendE = matrices.E[i][j-1] + gapExtendPenalty;
                matrices.E[i][j] = Math.max(openE, extendE);

                double openF = matrices.M[i-1][j] + gapOpenPenalty + gapExtendPenalty;
                double extendF = matrices.F[i-1][j] + gapExtendPenalty;
                matrices.F[i][j] = Math.max(openF, extendF);

                double match = matrices.M[i-1][j-1] + substitutionMatrix.getScore(seq1.charAt(i-1), seq2.charAt(j-1));

                matrices.M[i][j] =  Math.max(match, Math.max(matrices.E[i][j], matrices.F[i][j]));
            }
        }
        
        return matrices;
    }
} 