import argparse
import os
import re


def sanitize_genome(genome):
    """Sanitize the genome to contain only valid nucleotides A, C, G, T."""
    return ''.join([nucleotide for nucleotide in genome if nucleotide in 'ACGT'])


def read_genome(file_path):
    """Read and sanitize genome from the file."""
    with open(file_path, 'r') as file:
        genome = []
        for line in file:
            if not line.startswith('>'):  # Skip header lines in FASTA format
                genome.append(line.strip().upper())
        return sanitize_genome(''.join(genome))


def count_overlapping_seqs(genome, sequence):
    """Count overlapping occurrences of the sequence in the genome."""
    return len(re.findall(f'(?={sequence})', genome))


def calculate_nucleotide_composition(genome):
    """Calculate the relative frequencies of nucleotides in the genome."""
    total_length = len(genome)
    composition = {nucleotide: genome.count(nucleotide) for nucleotide in 'ACGT'}
    composition_percentage = {nuc: (count / total_length) * 100 for nuc, count in composition.items()}
    return composition_percentage


def calculate_expected_counts(genome, sequences, composition):
    """Calculate the expected number of incidences based on nucleotide composition."""
    total_length = len(genome)
    expected_counts = {}
    for seq in sequences:
        # Uniform expectation: p = 1/4 for each nucleotide
        p_uniform = (0.25) ** len(seq) * (total_length - len(seq) + 1)

        # Expected based on composition: using the relative frequencies
        p_composition = 1
        for nucleotide in seq:
            p_composition *= composition.get(nucleotide, 0) / 100  # relative frequency
        p_composition *= (total_length - len(seq) + 1)

        expected_counts[seq] = (p_uniform, p_composition)
    return expected_counts


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--sequence', nargs='+', type=str, required=True, help="List of sequences to search for")
    parser.add_argument('--genome', nargs='+', type=str, required=True, help="List of genome file paths")
    parser.add_argument('--output', type=str, required=True, help="Output file path")
    args = parser.parse_args()

    sequences = args.sequence
    genome_files = args.genome

    with open(args.output, 'w') as f:
        for genome_file in genome_files:
            genome = read_genome(genome_file)
            composition = calculate_nucleotide_composition(genome)
            expected_counts = calculate_expected_counts(genome, sequences, composition)
            counts = {seq: count_overlapping_seqs(genome, seq) for seq in sequences}

            organism_name = os.path.splitext(os.path.basename(genome_file))[0]
            f.write(f"{organism_name}:\n")

            # Write nucleotide composition
            f.write("A: {:.2f}% C: {:.2f}% G: {:.2f}% T: {:.2f}%\n".format(
                composition.get('A', 0), composition.get('C', 0), composition.get('G', 0), composition.get('T', 0)
            ))

            # Write expected and actual counts for each sequence
            for sequence in sequences:
                exp_uniform, exp_composition = expected_counts.get(sequence, (0, 0))
                actual_count = counts.get(sequence, 0)
                fold_change = actual_count / exp_composition if exp_composition != 0 else float('inf')

                # Format: sequence, expected uniform, expected composition, actual count, fold change
                f.write(f"{sequence}:\t{exp_uniform:.2f}\t{exp_composition:.2f}\t{actual_count}\t{fold_change:.2f}\n")
            f.write("\n")  # Separate outputs for different organisms


if __name__ == "__main__":
    main()
