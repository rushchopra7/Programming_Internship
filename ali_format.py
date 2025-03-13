#!/usr/bin/env python3

def extract_score_and_ids(line, next_line):
    parts = line.strip().split()
    score = 0.0
    if len(parts) >= 3:
        try:
            score = float(parts[2])
        except ValueError:
            pass

    if len(parts) >= 2:
        seq1_id = parts[0][1:]
        seq2_id = parts[1]
        return seq1_id, seq2_id, score
    
    return "", "", 0.0

def read_alignments(file_path):
    alignments = {}
    with open(file_path, 'r') as f:
        current_header = None
        current_seqs = []
        
        for line in f:
            line = line.strip()
            if not line:
                continue
                
            if line.startswith('>'):
                if current_header and len(current_seqs) == 2:
                    seq1_id, seq2_id, score = extract_score_and_ids(current_header, "")
                    key = f"{seq1_id}_{seq2_id}"
                    if key not in alignments:
                        alignments[key] = {'score': score, 'alignments': []}
                    alignments[key]['alignments'].append(current_seqs)

                current_header = line
                current_seqs = []
            elif ':' in line:
                current_seqs.append(line)

        if current_header and len(current_seqs) == 2:
            seq1_id, seq2_id, score = extract_score_and_ids(current_header, "")
            key = f"{seq1_id}_{seq2_id}"
            if key not in alignments:
                alignments[key] = {'score': score, 'alignments': []}
            alignments[key]['alignments'].append(current_seqs)
    
    return alignments

def pad_sequences(seq1, seq2):
    id1, seq1_content = seq1.split(': ', 1)
    id2, seq2_content = seq2.split(': ', 1)

    seq1_content = seq1_content.strip()
    seq2_content = seq2_content.strip()

    max_len = max(len(seq1_content), len(seq2_content))

    seq1_padded = seq1_content.ljust(max_len, '-')
    seq2_padded = seq2_content.ljust(max_len, '-')

    return f"{id1}: {seq1_padded.rstrip()}", f"{id2}: {seq2_padded.rstrip()}"

def combine_alignments(file1, file2):
    try:
        alignments1 = read_alignments(file1)
        alignments2 = read_alignments(file2)

        with open('alignment_com.txt', 'w') as out:
            for key in alignments1:
                scores = []
                if key in alignments2:
                    scores.append(alignments2[key]['score'])

                header = f">{key}"
                if scores:
                    header += " " + " ".join(f"{score:.4f}" for score in scores)
                out.write(header + "\n")

                all_sequences = []

                if alignments1[key]['alignments']:
                    for seqs in alignments1[key]['alignments']:
                        all_sequences.extend(seqs)

                if key in alignments2 and alignments2[key]['alignments']:
                    for seqs in alignments2[key]['alignments']:
                        all_sequences.extend(seqs)

                max_len = 0
                for seq in all_sequences:
                    _, seq_content = seq.split(': ', 1)
                    seq_content = seq_content.strip()
                    max_len = max(max_len, len(seq_content))

                for seq in all_sequences:
                    seq_id, seq_content = seq.split(': ', 1)
                    seq_content = seq_content.strip()
                    padded_seq = seq_content.ljust(max_len, '-')
                    out.write(f"{seq_id}: {padded_seq}\n")
                
                out.write('\n')

    except FileNotFoundError as e:
        print(f"Error: Could not find file {e.filename}")
    except Exception as e:
        print(f"Error processing files: {str(e)}")

def main():
    import sys

    if len(sys.argv) != 3:
        print("Usage: python ali_format.py <file1> <file2>")
        sys.exit(1)

    file1 = sys.argv[1]
    file2 = sys.argv[2]

    combine_alignments(file1, file2)

if __name__ == "__main__":
    main()