import java.io.*;
import java.sql.*;
import java.util.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class Homstrad_Sequences {

    public static void main(String[] args) {
        // Example usage
        generatingTestSet("mysql2-ext.bio.ifi.lmu.de", "bioprakt2", "3306", "$1$xyWsttEl$sAmFI1NOY5sVGpgmzf1ga1", "bioprakt2");
    }

    public static void generatingTestSet(String host, String user, String port, String password, String database) {
        String dbUrl = "jdbc:mysql://" + host + ":" + port + "/" + database;
        String pairwiseIdsFile = "pairwise_ids.txt";
        String pairwiseSequencesFile = "pairwise_sequences.txt";

        try (Connection conn = DriverManager.getConnection(dbUrl, user, password)) {
            System.out.println("Connected to database.");

            // Query to get sequences grouped by family
            String query = "SELECT family, id, sequence FROM homstrad ORDER BY family, id";
            try (PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {

                // Store sequences by family
                Map<String, List<SequenceEntry>> familySequences = new HashMap<>();

                while (rs.next()) {
                    String family = rs.getString("family");
                    String id = rs.getString("id");
                    String sequence = rs.getString("sequence");

                    familySequences.putIfAbsent(family, new ArrayList<>());
                    familySequences.get(family).add(new SequenceEntry(id, sequence));
                }

                // Generate pairwise alignments and write them to files
                writePairwiseData(familySequences, pairwiseIdsFile, pairwiseSequencesFile);
                System.out.println("Pairwise test set created successfully!");

            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Method to generate and write pairwise data
    private static void writePairwiseData(Map<String, List<SequenceEntry>> familySequences,
                                          String pairwiseIdsFile, String pairwiseSequencesFile) {
        try (BufferedWriter idWriter = new BufferedWriter(new FileWriter(pairwiseIdsFile));
             BufferedWriter seqWriter = new BufferedWriter(new FileWriter(pairwiseSequencesFile))) {

            for (String family : familySequences.keySet()) {
                List<SequenceEntry> sequences = familySequences.get(family);

                // Generate all pairwise combinations
                for (int i = 0; i < sequences.size(); i++) {
                    for (int j = i + 1; j < sequences.size(); j++) {
                        SequenceEntry seq1 = sequences.get(i);
                        SequenceEntry seq2 = sequences.get(j);

                        // Write to ID file
                        idWriter.write(family + ": " + seq1.id + "," + seq2.id + "\n");

                        // Write to sequence file
                        seqWriter.write(family + ": " + seq1.id + " -> " + seq1.sequence + "\n");
                        seqWriter.write(family + ": " + seq2.id + " -> " + seq2.sequence + "\n\n");
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Error writing pairwise test set files.");
            e.printStackTrace();
        }
    }

    // Helper class to store sequence data
    static class SequenceEntry {
        String id;
        String sequence;

        SequenceEntry(String id, String sequence) {
            this.id = id;
            this.sequence = sequence;
        }
    }
}
