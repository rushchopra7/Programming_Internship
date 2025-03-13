import mysql.connector
import os

def connect_to_db():
    return mysql.connector.connect(
        host="mysql2-ext.bio.ifi.lmu.de",
        user="bioprakt2",
        port=3306,
        password="$1$xyWsttEl$sAmFI1NOY5sVGpgmzf1ga1",
        database="bioprakt2"

    )

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

    for pdb_id, family, aligned_seq, raw_seq in rows:
        if family not in families:
            families[family] = []
        families[family].append(pdb_id)
        aligned_seqs[pdb_id] = aligned_seq.replace(" ", "")  # Clean aligned sequence
        raw_seqs[pdb_id] = raw_seq.replace("-", "")         # Remove gaps for raw

    # Generate files
    pairwise_ids = []
    pairwise_aligned = []
    pairwise_unaligned = []

    for family, pdb_ids in families.items():
        # Generate all unique pairs
        for i in range(len(pdb_ids)):
            for j in range(i + 1, len(pdb_ids)):
                id1 = pdb_ids[i]
                id2 = pdb_ids[j]

                # For pairwise_ids.txt
                pairwise_ids.append(f"{id1} {id2}")

                # For pairwise_seqs.txt (aligned, split into two lines)
                pairwise_aligned.append(f"{id1}:{aligned_seqs[id1]}")
                pairwise_aligned.append(f"{id2}:{aligned_seqs[id2]}")

                # For unaligned_seqs.txt (raw, split into two lines)
                pairwise_unaligned.append(f"{id1}:{raw_seqs[id1]}")
                pairwise_unaligned.append(f"{id2}:{raw_seqs[id2]}")

    # Write files
    output_dir = "pairwise_output"
    os.makedirs(output_dir, exist_ok=True)

    with open(os.path.join(output_dir, "pairwise_ids.txt"), "w") as f:
        f.write("\n".join(pairwise_ids))

    with open(os.path.join(output_dir, "pairwise_seqs.txt"), "w") as f:
        f.write("\n".join(pairwise_aligned))

    with open(os.path.join(output_dir, "unaligned_seqs.txt"), "w") as f:
        f.write("\n".join(pairwise_unaligned))

    cursor.close()
    connection.close()
    print("Files generated successfully.")

fetch_pairwise_data()