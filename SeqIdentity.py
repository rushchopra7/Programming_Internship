import pandas as pd
import matplotlib.pyplot as plt


# Function to compute sequence identity
def compute_identity(seq1, seq2):
    """Compute the sequence identity as percentage of identical positions"""
    matches = sum(a == b for a, b in zip(seq1, seq2) if a != '-' and b != '-')
    total_positions = sum(1 for a, b in zip(seq1, seq2) if a != '-' and b != '-')
    return (matches / total_positions) * 100 if total_positions > 0 else 0


# Load aligned sequences and IDs
pairwise_seqs_file = "C:/Users/rusha/PycharmProjects/pythonProject4/PBlock/pairwise_seqs2.txt"
pairwise_ids_file = "C:/Users/rusha/PycharmProjects/pythonProject4/PBlock/pairwise_ids2.txt"
unaligned_seqs_file = "C:/Users/rusha/PycharmProjects/pythonProject4/PBlock/unaligned_seqs2.txt"

# Read the pairwise sequences into a dictionary
pairwise_seqs = {}
with open(pairwise_seqs_file, "r") as f:
    for line in f:
        parts = line.strip().split(":")
        if len(parts) == 2:
            pairwise_seqs[parts[0]] = parts[1]

# Read the unaligned sequences into a dictionary
unaligned_seqs = {}
with open(unaligned_seqs_file, "r") as f:
    for line in f:
        parts = line.strip().split(":")
        if len(parts) == 2:
            unaligned_seqs[parts[0]] = parts[1]

# Load sequence pairs and compute sequence identity
sequence_identity = []
sequence_pairs = []

with open(pairwise_ids_file, "r") as f:
    for line in f:
        id1, id2 = line.strip().split()
        if id1 in pairwise_seqs and id2 in pairwise_seqs:
            seq1 = pairwise_seqs[id1]
            seq2 = pairwise_seqs[id2]
            identity = compute_identity(seq1, seq2)
            sequence_identity.append(identity)
            sequence_pairs.append((id1, id2))

# Convert to DataFrame
df_identity = pd.DataFrame(sequence_pairs, columns=["ID1", "ID2"])
df_identity["Sequence Identity (%)"] = sequence_identity

# Define the threshold for classification (e.g., 70%)
threshold = 70

# Classify pairs into closely related and distantly related based on the threshold
closely_related = df_identity[df_identity["Sequence Identity (%)"] > threshold]
distantly_related = df_identity[df_identity["Sequence Identity (%)"] <= threshold]

# Save sequence identity results to CSV
df_identity.to_csv("C:/Users/rusha/PycharmProjects/pythonProject4/PBlock/sequence_identity.csv", index=False)
print("Sequence identity calculated and saved as 'sequence_identity.csv'!")


# Create output files for closely related and distantly related pairs

def save_pairs_to_files(related_pairs, label):
    seq_file = f"C:/Users/rusha/PycharmProjects/pythonProject4/PBlock/{label}_aligned_seqs.txt"
    id_file = f"C:/Users/rusha/PycharmProjects/pythonProject4/PBlock/{label}_ids.txt"
    unaligned_file = f"C:/Users/rusha/PycharmProjects/pythonProject4/PBlock/{label}_unaligned_seqs.txt"

    aligned_seqs = []
    ids = []
    unaligned_seqs_out = []

    for _, row in related_pairs.iterrows():
        id1, id2 = row["ID1"], row["ID2"]
        aligned_seqs.append(f"{id1}:{pairwise_seqs[id1]}")
        aligned_seqs.append(f"{id2}:{pairwise_seqs[id2]}")
        ids.append(f"{id1} {id2}")
        unaligned_seqs_out.append(f"{id1}:{unaligned_seqs[id1]}")
        unaligned_seqs_out.append(f"{id2}:{unaligned_seqs[id2]}")

    # Write the aligned sequences file
    with open(seq_file, "w") as f:
        f.write("\n".join(aligned_seqs))

    # Write the IDs file
    with open(id_file, "w") as f:
        f.write("\n".join(ids))

    # Write the unaligned sequences file
    with open(unaligned_file, "w") as f:
        f.write("\n".join(unaligned_seqs_out))


# Save files for closely related and distantly related pairs
save_pairs_to_files(closely_related, "closely_related")
save_pairs_to_files(distantly_related, "distantly_related")

# Plot the distribution of sequence identity
plt.figure(figsize=(10, 6))

# Plot histogram for closely related pairs (greater than the threshold)
plt.hist(closely_related["Sequence Identity (%)"], bins=20, alpha=0.6, label="Closely Related", color='g')

# Plot histogram for distantly related pairs (less than or equal to the threshold)
plt.hist(distantly_related["Sequence Identity (%)"], bins=20, alpha=0.6, label="Distantly Related", color='r')

# Add vertical line for cutoff
plt.axvline(x=threshold, color='black', linestyle='--', label=f"Threshold = {threshold}%")

# Add labels and title
plt.xlabel("Sequence Identity (%)")
plt.ylabel("Frequency")
plt.title("Distribution of Sequence Identity for Closely and Distantly Related Pairs")
plt.legend()

# Show plot
plt.grid(True)
plt.show()
