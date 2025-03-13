#!/usr/bin/env python3

import argparse
import os



parser = argparse.ArgumentParser()
parser.add_argument('--input', type = str, help = '')
parser.add_argument('--output', type = str, help = '')
args = parser.parse_args()

# for running this code locally, please use the format encode.py --input encode.csv --output test
# test is the folder where all tables were created


def read_input(input_file):
    with open(input_file, 'r') as f:
        return [line.strip() for line in f]



in_lines = read_input(args.input)

# Task 1 : Anzahl der Experimente vom jeden Typ
# output should be exptypes.tsv file

Experiments = {} # dictionary for storing experiment as key and count as value

for l in in_lines[1:]:
    experiment_type = l.split(',')[0]
    if experiment_type not in Experiments:
        Experiments[experiment_type] = 1
    else:
        Experiments[experiment_type] = Experiments[experiment_type] + 1

#print(len(Experiments))
#print(Experiments)
# output file

out_experiments = ''
for exp in Experiments:
    out_experiments += exp + '\t' + str(Experiments[exp]) + '\n'

#print(Experiments)
#print(out_experiments)

#print("Row count before writing:", len(out_experiments.strip().split("\n")))

with open(os.path.join(args.output, "exptypes.tsv"), 'w') as output1:
    output1.write(out_experiments.strip())
# used os.path.join to avoid linux or windows classic problems, but for other two tasks it works anyways

# TASK 2 counting the number of CHIP-seq antibodies for evey cell line and also for the cell line without CHIP-seq

Celltypes = {}

for li in in_lines[1:]:
    c = li.split(',')
    cell = c[1]
    exp_factors = c[2]
    if cell not in Celltypes:
        if c[0] == "ChIP-seq":
            antibodies = (exp_factors.split(" ")[0]).split("=")[1]
            Celltypes[cell] = [antibodies]
        else:
            Celltypes[cell] = []
    else:
        if li.split(",")[0] == "ChIP-seq":
            antibody = (exp_factors.split(" ")[0]).split("=")[1]
            if antibody not in Celltypes[cell]:
                Celltypes[cell].append(antibody)

#print(Celltypes)

# output file
out_antibodies = ''
for anti in Celltypes:
    out_antibodies += anti + "\t" + str(len(Celltypes[anti])) + "\n"

#print(len(out_antibodies))

with open(args.output + "/" + "antibodies.tsv", 'w') as output2:
    output2.write(out_antibodies.strip())


# TASK 3 finding cell types in CHIP-seq and RNA-seq experiments for antibody H3K27me3

out_table = 'cell line' + '\t' + 'RNAseq Accession' + '\t' + 'ChIPseq Accession'

chip_seq = [x.strip() for x in in_lines if x.startswith('ChIP-seq')] #extracting all lines starting with chipseq
rna_seq = [x.strip() for x in in_lines if x.startswith('RNA-seq')]  # extracting all lines with rna seq


chip_cell = {}

for li in chip_seq:
    cell = li.split(",")[1]
    dcc = li.split(",")[9]
    if li.split(",")[2].split(" ")[0] == 'Antibody=H3K27me3':
        if cell not in chip_cell:
            if dcc.startswith('wgEncode'):
                chip_cell[cell] = [dcc]
        else:
            if dcc.startswith('wgEncode'):
                chip_cell[cell].append(dcc)

rna_cell = {}

for li in rna_seq:
    cell = li.split(",")[1]
    dcc = li.split(",")[9]
    if cell not in rna_cell:
        if dcc.startswith('wgEncode'):
            rna_cell[cell] = [dcc]
    else:
        if dcc.startswith('wgEncode'):
            rna_cell[cell].append(dcc)

#austeigende reihenfolge für DCC nummern

for chip in chip_cell:
    #print(chip)
    chip_cell[chip].sort()
#print(chip_cell)

for rna in rna_cell:
    rna_cell[rna].sort()

for rna in rna_cell:
    #print(rna)
    for chip in chip_cell:
        #print(chip)
        if rna == chip:
            out_table += '\n' + rna + '\t' + ','.join(rna_cell[rna]) + '\t' + ','.join(chip_cell[chip])



#print(chip_cell)
#print(rna_cell)

with open(args.output + '/' + 'chip_rna_seq.tsv', 'w') as output3:
    output3.write(out_table)
#print(out_table)







