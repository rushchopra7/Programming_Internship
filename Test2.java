public class Test2 {
    public static void main(String[] args) {
        // Prediction alignment
        String predTemplate = "MRGMLPLFEPKGRVLLVDGHHLAYRTFHALKGLTTSRGEPVQAVYGFAKSLLKALKE-----DGDAVIVVFDAKAPSFRHEAYGGYKAGRAPTPEDFPRQLALIKELVDL--LGLARLEVPGYEADDVLASLAKKAEKEGYEVRILTADKDLYQLLSDRIHVLHPEGYLITPAW-LWEKYGLRP----DQWADYRALTGDESDNLPGVKGIGEKTARKLLEEWGSLEALLKNLDRLKPAIREKILAHMDDLK--LSWDLAKVRTDLP---LEVDFAKRREPDRERLRAFLERLEFGSLLHEF";
        String predTarget = "----------RRNLMIVDGTNLGFR-------------------FPFASSYVSTIQSLAKSYSARTTIVLGDKGKSVFRLEHLPEY---------AF---FEYLKDAFELCKTTFPTFTIRGVEADDMAAYIVKLIGHLYDHVWLISTDGDWDTLLTDKVSRF---SFTTRREYHLRDMYEHHNVDDVEQFISLKAIMGDLGDNIRGVEGIGAKRGYNIIREFGN---VLDIIDQLPLPGKQKYIQNLNASEELLFRNL--ILVDLPTYCVDAIAAVGQDVLDKFTKDILEIAE--------";

        // Reference alignment
        String refTemplate = "MRGMLPLFEPKGRVLLVDGHHLAYRTFHALKGLTTSRGEPVQAVYGFAKSLLKALKEDGDAVIVVFDAKAPSFRHEAYGGYKAGRAPTPEDFPRQLALIKELVDLLGLARLEVPGYEADDVLASLAKKAEKEGYEVRILTADKDLYQLLSDRIHVLHPEG-YLITPAWLWEKYGL-RPDQWADYRALTGDESDNLPGVKGIGEKTARKLLEEWG-SLEALL---KNLD-RLKPAIREKILAHMDDLKLSWDLAKVRT---DLPLEVDFAKRREPDRERLRAFLERL-EFGSLLHEF";
        String refTarget = "----------RRNLMIVDGTNLGFRFP--------------FASSYVSTIQSLAKSYSARTTIVLGDKG-KSVFRLEHLP--------EYAFFEYLKDAFELCKT-TFPTFTIRGVEADDMAAYIVKLIGHLYDHVWLISTDGDWDTLLTDKVSRFSFTTRREYHLRDMYEHHNVDDVEQFISLKAIMGDLGDNIRGVEG----IGAKRGYNIIREFGNVLDIIDQLPLPGKQKYIQNLNASEELLFRNLILVDLPTYCVDAIAAVG--------QDVLDKFTKDILEIAE-----";

        // Create temporary file with alignment
        try {
            java.io.PrintWriter writer = new java.io.PrintWriter("test_alignment.txt");
            writer.println(">5_3_exonuclease_0_1 119.1000");
            writer.println("1bgxt: " + predTemplate);
            writer.println("1xo1a: " + predTarget);
            writer.println("1bgxt: " + refTemplate);
            writer.println("1xo1a: " + refTarget);
            writer.println();
            writer.close();

            // Run AlignmentMetrics
            AlignmentMetrics.main(new String[]{"-f", "test_alignment.txt"});

            // Clean up
            new java.io.File("test_alignment.txt").delete();

        } catch (Exception e) {
            System.err.println("Error running test: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 