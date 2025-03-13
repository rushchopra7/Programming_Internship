import java.io.FileInputStream;
import java.io.InputStreamReader;

public class AlignmentMetrics {
    private static class Alignment {
        String header;
        double originalScore;
        String templateId;
        String targetId;
        String templateSeq;
        String targetSeq;
        String refTemplateSeq;
        String refTargetSeq;

        Alignment(String header, double score, String templateId, String targetId,
                 String templateSeq, String targetSeq, String refTemplateSeq, String refTargetSeq) {
            this.header = header;
            this.originalScore = score;
            this.templateId = templateId;
            this.targetId = targetId;
            this.templateSeq = templateSeq;
            this.targetSeq = targetSeq;
            this.refTemplateSeq = refTemplateSeq;
            this.refTargetSeq = refTargetSeq;
        }
    }

    private static class Metrics {
        double sensitivity;
        double specificity;
        double coverage;
        double meanShiftError;
        double inverseMeanShiftError;

        Metrics(double sensitivity, double specificity, double coverage, 
               double meanShiftError, double inverseMeanShiftError) {
            this.sensitivity = sensitivity;
            this.specificity = specificity;
            this.coverage = coverage;
            this.meanShiftError = meanShiftError;
            this.inverseMeanShiftError = inverseMeanShiftError;
        }
    }

    private static Alignment[] loadAlignments(String filename) throws Exception {
        // First count number of alignments
        int alignmentCount = 0;
        FileInputStream fis = new FileInputStream(filename);
        InputStreamReader reader = new InputStreamReader(fis);
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = reader.read()) != -1) {
            if (c == '>') alignmentCount++;
        }
        reader.close();
        fis.close();

        // Create array to hold alignments
        Alignment[] alignments = new Alignment[alignmentCount];
        int currentAlignment = 0;

        // Read file again to load alignments
        fis = new FileInputStream(filename);
        reader = new InputStreamReader(fis);
        String header = null;
        double score = 0.0;
        String templateId = null, targetId = null;
        String templateSeq = null, targetSeq = null;
        String refTemplateSeq = null, refTargetSeq = null;
        int lineCount = 0;
        sb = new StringBuilder();

        while ((c = reader.read()) != -1) {
            char ch = (char)c;
            if (ch == '\n' || ch == '\r') {
                String line = sb.toString().trim();
                sb = new StringBuilder();
                
                if (line.isEmpty()) continue;

                if (line.startsWith(">")) {
                    if (header != null) {
                        alignments[currentAlignment++] = new Alignment(header, score, templateId, targetId,
                                                                     templateSeq, targetSeq, refTemplateSeq, refTargetSeq);
                    }
                    String[] parts = line.substring(1).split("\\s+");
                    header = parts[0];
                    score = Double.parseDouble(parts[1]);
                    lineCount = 0;
                } else if (line.contains(":")) {
                    String[] parts = line.split(":");
                    String id = parts[0].trim();
                    String seq = parts[1].trim();
                    
                    switch (lineCount) {
                        case 0: // Template sequence
                            templateId = id;
                            templateSeq = seq;
                            break;
                        case 1: // Target sequence
                            targetId = id;
                            targetSeq = seq;
                            break;
                        case 2: // Reference template sequence
                            refTemplateSeq = seq;
                            break;
                        case 3: // Reference target sequence
                            refTargetSeq = seq;
                            break;
                    }
                    lineCount++;
                }
            } else {
                sb.append(ch);
            }
        }
        
        // Add last alignment
        if (header != null) {
            alignments[currentAlignment] = new Alignment(header, score, templateId, targetId,
                                                       templateSeq, targetSeq, refTemplateSeq, refTargetSeq);
        }

        reader.close();
        fis.close();
        return alignments;
    }

    private static Metrics calculateMetrics(String predSeq1, String predSeq2, 
                                         String refSeq1, String refSeq2) {
        int tp = 0;
        int fp = 0;
        int fn = 0;
        double totalShift = 0;
        double totalInverseShift = 0;

        // Convert sequences to arrays of indices (ignoring gaps)
        int[] pred1Indices = new int[predSeq1.length() + 1];
        int[] pred2Indices = new int[predSeq2.length() + 1];
        int[] ref1Indices = new int[refSeq1.length() + 1];
        int[] ref2Indices = new int[refSeq2.length() + 1];
        
        int pred1Count = 0, pred2Count = 0;
        for (int i = 0; i < predSeq1.length(); i++) {
            if (predSeq1.charAt(i) != '-') pred1Indices[pred1Count++] = i;
            if (predSeq2.charAt(i) != '-') pred2Indices[pred2Count++] = i;
        }
        
        int ref1Count = 0, ref2Count = 0;
        for (int i = 0; i < refSeq1.length(); i++) {
            if (refSeq1.charAt(i) != '-') ref1Indices[ref1Count++] = i;
            if (refSeq2.charAt(i) != '-') ref2Indices[ref2Count++] = i;
        }

        // Create alignment maps for quick lookup
        boolean[][] predAligned = new boolean[pred1Count][pred2Count];
        boolean[][] refAligned = new boolean[ref1Count][ref2Count];
        char[] TargetNoGaps = new char[refSeq2.length()];
        char[] TemplateNoGaps = new char[refSeq2.length()];
        int refCount = 0;
        for (int i = 0; i < refSeq2.length(); i++) {
            if (refSeq2.charAt(i) != '-') {
                TargetNoGaps[refCount++] = refSeq2.charAt(i);
            }
        }
        refCount = 0;
        for (int i = 0; i < refSeq1.length(); i++) {
            if (refSeq1.charAt(i) != '-') {
                TemplateNoGaps[refCount++] = refSeq1.charAt(i);
            }
        }
        

        for (int i = 0; i < pred1Count; i++) {
            for (int j = 0; j < pred2Count; j++) {
                if (pred1Indices[i] == pred2Indices[j]) {
                    predAligned[i][j] = true;
                }
            }
        }
        
        // Mark aligned positions in reference
        for (int i = 0; i < ref1Count; i++) {
            for (int j = 0; j < ref2Count; j++) {
                if (ref1Indices[i] == ref2Indices[j]) {
                    refAligned[i][j] = true;
                }
            }
        }

        // Count metrics and calculate shifts
        for (int i = 0; i < pred1Count; i++) {
            for (int j = 0; j < pred2Count; j++) {
                if (predAligned[i][j]) {
                    if (i < ref1Count && j < ref2Count && refAligned[i][j]) {
                        tp++;
                        // Calculate shift for template sequence (predSeq1 and refSeq1)
                        int templateShift = Math.abs(pred1Indices[i] - ref1Indices[i]);
                        totalShift += templateShift;
                        // Calculate shift for target sequence (predSeq2 and refSeq2)
                        int targetShift = Math.abs(pred2Indices[j] - ref2Indices[j]);
                        totalInverseShift += targetShift;
                    } else {
                        fp++;
                    }
                }
            }
        }

        // Count false negatives
        for (int i = 0; i < ref1Count; i++) {
            for (int j = 0; j < ref2Count; j++) {
                if (refAligned[i][j]) {
                    if (i >= pred1Count || j >= pred2Count || !predAligned[i][j]) {
                        fn++;
                    }
                }
            }
        }
        
        int coverageCount = 0;
        int[] refAli = new int[TargetNoGaps.length];
        int k = 0;
        for (int i = 0; i < refSeq2.length(); i++) {
             if (refSeq2.charAt(i) != '-'){
                 if(refSeq1.charAt(i) != '-'){
                     refAli[i-k] = 1;
                 }
             } else {
                 k++;
             }
        }
        int[] predAli = new int[TargetNoGaps.length];
        int countPredAli = 0;
        k = 0;
        for (int i = 0; i < predSeq2.length(); i++) {
            if (predSeq2.charAt(i) != '-'){
                if(predSeq1.charAt(i) != '-'){
                    predAli[i-k] = 1;
                }
            } else {
                k++;
            }
        }
        for (int i = 0; i < predAli.length; i++) {
            if (predAli[i] == 1) {
                countPredAli++;
            }
        }
        for (int i = 0; i < predAli.length; i++) {
            if (predAli[i] == 1 && refAli[i]== 1) {
                coverageCount++;
            }
        }
        int[] refAliChar = new int[TargetNoGaps.length];
        int[] predAliChar = new int[TargetNoGaps.length];
        for (int i = 0; i < TargetNoGaps.length; i++) {
            refAliChar[i] = -1;
            predAliChar[i] = -1;
        }
        k = 0;
        for (int i = 0; i < refSeq2.length(); i++) {
            if (refSeq2.charAt(i) != '-') {
                if (refSeq1.charAt(i) != '-') {
                    int pos = 0;
                    for (int j = 0; j < i; j++) {
                        if (refSeq1.charAt(j) != '-') pos++;
                    }
                    refAliChar[k] = pos;
                }
                k++;
            }
        }
        k = 0;
        for (int i = 0; i < predSeq2.length(); i++) {
            if (predSeq2.charAt(i) != '-') {
                if (predSeq1.charAt(i) != '-') {
                    int pos = 0;
                    for (int j = 0; j < i; j++) {
                        if (predSeq1.charAt(j) != '-') pos++;
                    }
                    predAliChar[k] = pos;
                }
                k++;
            }
        }
        int[] shifts = new int[TargetNoGaps.length];
        int countShift = 0;
        for (int i = 0; i < TargetNoGaps.length; i++) {
            if (refAliChar[i] != -1 && predAliChar[i] != -1) {
                shifts[countShift] = Math.abs(refAliChar[i] - predAliChar[i]);
                countShift++;
            }
        }
        totalShift = 0;
        for (int i = 0; i < countShift; i++) {
            totalShift += shifts[i];
        }
        double meanShiftError = countShift > 0 ? totalShift / countShift : 0;

        // Berechne inverse mean shift error (für Target-Sequenzen)
        int[] refTargetAliChar = new int[TargetNoGaps.length];
        int[] predTargetAliChar = new int[TargetNoGaps.length];
        for (int i = 0; i < TargetNoGaps.length; i++) {
            refTargetAliChar[i] = -1;
            predTargetAliChar[i] = -1;
        }

        k = 0;
        for (int i = 0; i < refSeq1.length(); i++) {
            if (refSeq1.charAt(i) != '-') {
                if (refSeq2.charAt(i) != '-') {
                    int pos = 0;
                    for (int j = 0; j < i; j++) {
                        if (refSeq2.charAt(j) != '-') pos++;
                    }
                    refTargetAliChar[k] = pos;
                }
                k++;
            }
        }

        k = 0;
        for (int i = 0; i < predSeq1.length(); i++) {
            if (predSeq1.charAt(i) != '-') {
                if (predSeq2.charAt(i) != '-') {
                    int pos = 0;
                    for (int j = 0; j < i; j++) {
                        if (predSeq2.charAt(j) != '-') pos++;
                    }
                    predTargetAliChar[k] = pos;
                }
                k++;
            }
        }

        int[] inverseShifts = new int[TargetNoGaps.length];
        int countInverseShift = 0;
        for (int i = 0; i < TargetNoGaps.length; i++) {
            if (refTargetAliChar[i] != -1 && predTargetAliChar[i] != -1) {
                inverseShifts[countInverseShift] = Math.abs(refTargetAliChar[i] - predTargetAliChar[i]);
                countInverseShift++;
            }
        }

        totalInverseShift = 0;
        for (int i = 0; i < countInverseShift; i++) {
            totalInverseShift += inverseShifts[i];
        }
        double inverseMeanShiftError = countInverseShift > 0 ? totalInverseShift / countInverseShift : 0;

        double sensitivity = (tp + fn) > 0 ? (double) tp / (tp + fn) : 0;
        double specificity = (tp + fp) > 0 ? (double) tp / (tp + fp) : 0;
        double coverage = pred2Count > 0 ? (double) coverageCount / countPredAli : 0;

        return new Metrics(sensitivity, specificity, coverage, meanShiftError, inverseMeanShiftError);
    }

    private static int findPosition(String refSeq, String predSeq, int predPos) {
        int refCount = 0;
        int predCount = 0;
        int maxLength = Math.max(refSeq.length(), predSeq.length());
        
        for (int i = 0; i < maxLength; i++) {
            char predChar = i < predSeq.length() ? predSeq.charAt(i) : '-';
            char refChar = i < refSeq.length() ? refSeq.charAt(i) : '-';
            
            if (predChar != '-') {
                if (predCount == predPos) {
                    return refCount;
                }
                predCount++;
            }
            if (refChar != '-') {
                refCount++;
            }
        }
        return -1;
    }

    private static int findTemplatePosition(String seq, int pos) {
        int seqCount = 0;
        int gaplessCount = 0;
        
        for (int i = 0; i < seq.length(); i++) {
            if (seqCount == pos) {
                return gaplessCount;
            }
            if (seq.charAt(i) != '-') {
                gaplessCount++;
            }
            seqCount++;
        }
        
        return seqCount == pos ? gaplessCount : -1;
    }

    public static void main(String[] args) {
        if (args.length != 2 || !args[0].equals("-f")) {
            System.err.println("Usage: java -jar validateAli.jar -f <alignment-pairs>");
            System.exit(1);
        }

        try {
            Alignment[] alignments = loadAlignments(args[1]);
            
            for (int i = 0; i < alignments.length; i++) {
                Alignment alignment = alignments[i];
                if (alignment == null) continue;
                
                Metrics metrics = calculateMetrics(
                    alignment.templateSeq, alignment.targetSeq,
                    alignment.refTemplateSeq, alignment.refTargetSeq
                );
                
                // Print header with scores
                System.out.printf(">%s %.4f %.4f %.4f %.4f %.4f %.4f%n",
                    alignment.header,
                    alignment.originalScore,
                    metrics.sensitivity,
                    metrics.specificity,
                    metrics.coverage,
                    metrics.meanShiftError,
                    metrics.inverseMeanShiftError
                );
                
                // Print alignments
                System.out.printf("%s: %s%n", alignment.templateId, alignment.templateSeq);
                System.out.printf("%s: %s%n", alignment.targetId, alignment.targetSeq);
                System.out.printf("%s: %s%n", alignment.templateId, alignment.refTemplateSeq);
                System.out.printf("%s: %s%n", alignment.targetId, alignment.refTargetSeq);
                System.out.println();
            }
            
        } catch (Exception e) {
            System.err.println("Error reading alignment file: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 