#!/usr/bin/env python3
import argparse

parser = argparse.ArgumentParser()
parser.add_argument('--sequence', nargs='+', type=str, required=True)
parser.add_argument('--genome', type=str, required=True)
args = parser.parse_args()


#modifying the function now with [] instead ''
def read_genome(file):
    genome = [] #empty list
    with open(file, 'r') as f:
        #lines = f.readlines()
        for l in f:
            if not l.startswith('>'):
                genome.append(l.strip())
    return ''.join(genome)

seqs = args.sequence
gen = read_genome(args.genome)

def count_seqs(geno,sequ):

    len_genome = len(geno)
    counts = {}
    for s in sequ:
        pointer = 0
        for i in range(len_genome - len(s) + 1):  # not exceeding the length of genome
            if geno[i:i + len(s)] == s:
                pointer += 1
        counts[s] = pointer
    return counts

for sequence, count in count_seqs(gen,seqs).items():
    print(f"{sequence}: {count}")


def cal_relative_probability():
    total_length = len(gen)
    counts = {'A': gen.count('A'), 'C': gen.count('C'),
              'G': gen.count('G'), 'T': gen.count('T')}

    probs = {nuc: (count / total_length) * 100 for nuc, count in counts.items()}

    print("A: {:.2f}%  C: {:.2f}%  G: {:.2f}%  T: {:.2f}%".format(
        probs["A"], probs["C"], probs["G"], probs["T"]
    ))

def cal_incidence():
    expected = {'A': 0.1695,
        'C': 0.1695,
        'G': 0.3228,
        'T': 0.1694
    }

    for s in seqs:
        e = 1
        for base in s:
            e *= expected.get(base,0.0)
        print (f"{s}: {e * len(gen):.2f}")


cal_relative_probability()
cal_incidence()

















