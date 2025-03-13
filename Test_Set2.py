import mysql.connector
import os
import re
# 1.von jeder familie zwei sequenzen rausesucehn. eine alignierte , eine unalignierte , ein ids closely related
#2. eine seq aus einer eien aus andeerer un d...
def connect_to_db():
    return mysql.connector.connect(
        host="mysql2-ext.bio.ifi.lmu.de",
        user="bioprakt2",
        port=3306,
        password="$1$xyWsttEl$sAmFI1NOY5sVGpgmzf1ga1",
        database="bioprakt2"
    )

def is_valid_sequence(seq):
    """Check if sequence contains only valid amino acids and gaps"""
    valid_chars = set("ACDEFGHIKLMNPQRSTVWY-")
    return all(c in valid_chars for c in seq)

def fetch_pairwise_data():
    connection = connect_to_db()
    cursor = connection.cursor()

    # Fetch aligned and raw sequences
    query = """
        SELECT s.pdb_id, h.family, sh.seq_alignment, s.sequence
        FROM Seq_Homestrad sh
        JOIN Homestrad h ON sh.homestrad_id = h.homestrad_id
        JOIN Sequence_test s ON s.sequence_id = sh.sequence_id
        WHERE sh.seq_alignment IS NOT NULL;
    """
    cursor.execute(query)
    rows = cursor.fetchall()

    # Organize data
    families = {}
    aligned_seqs = {}
    raw_seqs = {}
    unique_sequences = set()  # Track unique sequences

    for pdb_id, family, aligned_seq, raw_seq in rows:
        # Clean and validate sequences
        aligned_seq = aligned_seq.replace(" ", "")
        raw_seq = raw_seq.replace("-", "")

        if not is_valid_sequence(aligned_seq) or not is_valid_sequence(raw_seq):
            continue

        if family not in families:
            families[family] = []
        families[family].append(pdb_id)
        
        # Only store if we haven't seen this sequence before
        if aligned_seq not in unique_sequences:
            aligned_seqs[pdb_id] = aligned_seq
            raw_seqs[pdb_id] = raw_seq
            unique_sequences.add(aligned_seq)

    # Generate files
    pairwise_ids = []
    pairwise_aligned = []
    pairwise_unaligned = []
    processed_pairs = set()  # Track processed pairs

    for family, pdb_ids in families.items():
        # Generate all unique pairs
        for i in range(len(pdb_ids)):
            for j in range(i + 1, len(pdb_ids)):
                id1 = pdb_ids[i]
                id2 = pdb_ids[j]

                # Skip if we don't have valid sequences for either ID
                if id1 not in aligned_seqs or id2 not in aligned_seqs:
                    continue

                # Create a sorted pair to avoid duplicates
                pair = tuple(sorted([id1, id2]))
                if pair in processed_pairs:
                    continue
                processed_pairs.add(pair)

                # For pairwise_ids.txt
                pairwise_ids.append(f"{id1} {id2}")

                # For pairwise_seqs.txt (aligned)
                pairwise_aligned.append(f"{id1}:{aligned_seqs[id1]}")
                pairwise_aligned.append(f"{id2}:{aligned_seqs[id2]}")

                # For unaligned_seqs.txt (raw)
                pairwise_unaligned.append(f"{id1}:{raw_seqs[id1]}")
                pairwise_unaligned.append(f"{id2}:{raw_seqs[id2]}")

    # Write files
    output_dir = "pairwise_output2"
    os.makedirs(output_dir, exist_ok=True)

    with open(os.path.join(output_dir, "pairwise_ids2.txt"), "w") as f:
        f.write("\n".join(pairwise_ids))

    with open(os.path.join(output_dir, "pairwise_seqs2.txt"), "w") as f:
        f.write("\n".join(pairwise_aligned))

    with open(os.path.join(output_dir, "unaligned_seqs2.txt"), "w") as f:
        f.write("\n".join(pairwise_unaligned))

    cursor.close()
    connection.close()
    print(f"Files generated successfully. Processed {len(processed_pairs)} unique pairs.")

if __name__ == "__main__":
    fetch_pairwise_data()