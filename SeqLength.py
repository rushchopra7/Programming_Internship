import os
import pandas as pd

# Define cutoffs for sequence length
SHORT_SEQ_THRESHOLD = 70   # Adjust as needed
LONG_SEQ_THRESHOLD = 300   # Adjust as needed

# File paths
base_path = "C:/Users/rusha/PycharmProjects/pythonProject4/PBlock/"
pairwise_ids_file = os.path.join(base_path, "pairwise_ids2.txt")
pairwise_seqs_file = os.path.join(base_path, "pairwise_seqs2.txt")
unaligned_seqs_file = os.path.join(base_path, "unaligned_seqs2.txt")

# Read aligned sequences into a dictionary
pairwise_seqs = {}
with open(pairwise_seqs_file, "r") as f:
    for line in f:
        parts = line.strip().split(":")
        if len(parts) == 2:
            pairwise_seqs[parts[0]] = parts[1]

# Read unaligned sequences into a dictionary
unaligned_seqs = {}
with open(unaligned_seqs_file, "r") as f:
    for line in f:
        parts = line.strip().split(":")
        if len(parts) == 2:
            unaligned_seqs[parts[0]] = parts[1]

# Load sequence pairs
short_seq_ids = []
short_seq_aligned = []
short_seq_unaligned = []

long_seq_ids = []
long_seq_aligned = []
long_seq_unaligned = []

with open(pairwise_ids_file, "r") as f:
    for line in f:
        id1, id2 = line.strip().split()
        if id1 in pairwise_seqs and id2 in pairwise_seqs:
            seq1 = pairwise_seqs[id1]
            seq2 = pairwise_seqs[id2]

            # Determine the category based on sequence length
            len1 = len(seq1.replace("-", ""))
            len2 = len(seq2.replace("-", ""))

            if len1 <= SHORT_SEQ_THRESHOLD and len2 <= SHORT_SEQ_THRESHOLD:
                short_seq_ids.append(f"{id1} {id2}")
                short_seq_aligned.append(f"{id1}:{seq1}")
                short_seq_aligned.append(f"{id2}:{seq2}")
                if id1 in unaligned_seqs and id2 in unaligned_seqs:
                    short_seq_unaligned.append(f"{id1}:{unaligned_seqs[id1]}")
                    short_seq_unaligned.append(f"{id2}:{unaligned_seqs[id2]}")

            elif len1 >= LONG_SEQ_THRESHOLD and len2 >= LONG_SEQ_THRESHOLD:
                long_seq_ids.append(f"{id1} {id2}")
                long_seq_aligned.append(f"{id1}:{seq1}")
                long_seq_aligned.append(f"{id2}:{seq2}")
                if id1 in unaligned_seqs and id2 in unaligned_seqs:
                    long_seq_unaligned.append(f"{id1}:{unaligned_seqs[id1]}")
                    long_seq_unaligned.append(f"{id2}:{unaligned_seqs[id2]}")

# Ensure output directory exists
os.makedirs(base_path, exist_ok=True)

# Save output files
def save_file(filename, data):
    with open(os.path.join(base_path, filename), "w") as f:
        f.write("\n".join(data))

save_file("short_seq_ids.txt", short_seq_ids)
save_file("short_seq_aligned.txt", short_seq_aligned)
save_file("short_seq_unaligned.txt", short_seq_unaligned)

save_file("long_seq_ids.txt", long_seq_ids)
save_file("long_seq_aligned.txt", long_seq_aligned)
save_file("long_seq_unaligned.txt", long_seq_unaligned)

print("Files generated successfully:")
print(f"- {len(short_seq_ids)} short sequences")
print(f"- {len(long_seq_ids)} long sequences")
