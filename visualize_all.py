#!/usr/bin/env python3

import argparse
import urllib.request

import numpy as np
import subprocess
import sys
import os




def download_get_pdb(id):
    downloads = []
    pdb_file = f"{id}.pdb"
    url = f"https://files.rcsb.org/download/{id}.pdb"
  #  url = f"https://files.rcsb.org/view/4D09{id}.pdb"
    try:
        with urllib.request.urlopen(url) as response, open(pdb_file, 'wb') as out_put:
            out_put.write(response.read())
        downloads.append(pdb_file)
    except urllib.error.HTTPError as e:
        print(f"HTTP Error {e.code}: {e.reason} for {pdb_id}")
    except urllib.error.URLError as e:
        print(f"URL Error: {e.reason} for {pdb_id}")
    except Exception as e:
        print(f"Something is wrong !! The file {pdb_id}: {e} couldn't be downloaded")
    return downloads

def extraction_data(pdb_file):
        a_positions = []
        c_alpha = []
        c_beta = []
        secondary_elements = 0
        total_residues = 0
        first_res = None
        last_res = None

        with open(pdb_file, 'r') as file:
            current_model = []  # To handle multiple models
            for line in file:
                record = line[:6].strip()
                if record == "MODEL":
                    # Start a new model
                    current_model = []
                elif record == "ATOM":
                    atom_type = line[12:16].strip()
                    res_number = int(line[22:26].strip())
                    coords = [float(line[30:38]), float(line[38:46]), float(line[46:54])]
                    current_model.append((atom_type, res_number, coords))
                elif record in ["HELIX", "SHEET"]:
                    # Process secondary structure
                    start_res = int(line[21:25].strip())
                    end_res = int(line[33:37].strip())
                    secondary_elements += (end_res - start_res + 1)
                elif record == "ENDMDL":
                    # Process current model
                    model_a = []
                    model_ca = []
                    model_cb = []
                    total = 0
                    for atom in current_model:
                        atom_type, res_num, coord = atom
                        model_a.append(coord)
                        if atom_type == 'CA':
                            model_ca.append(coord)
                            total += 1
                            if first_res is None:
                                first_res = res_num
                            last_res = res_num
                        elif atom_type == 'CB':
                            model_cb.append(coord)
                    # Append model data
                    a_positions.append(np.array(model_a))
                    c_alpha.append(np.array(model_ca))
                    c_beta.append(np.array(model_cb))
                    total_residues += total
            # Handle case with no MODEL records
            if not a_positions:
                # Process as single model
                model_a = []
                model_ca = []
                model_cb = []
                total = 0
                for atom in current_model:
                    atom_type, res_num, coord = atom
                    model_a.append(coord)
                    if atom_type == 'CA':
                        model_ca.append(coord)
                        total += 1
                        if first_res is None:
                            first_res = res_num
                        last_res = res_num
                    elif atom_type == 'CB':
                        model_cb.append(coord)
                a_positions.append(np.array(model_a))
                c_alpha.append(np.array(model_ca))
                c_beta.append(np.array(model_cb))
                total_residues += total

                # Calculate secondary structure ratio
                if total_residues == 0:
                    fractions_sec_str = 0.0
                else:
                    fractions_sec_str = secondary_elements / total_residues

        # Return a list of models, each containing their respective data
        return (
            np.array(a_positions),  # All atom positions
            np.array(c_alpha),  # All CA atoms
            np.array(c_beta),  # All CB atoms
            fractions_sec_str,  # Global secondary structure ratio
            first_res,  # First residue number
            last_res  # Last residue number
        )

def secondary_str_ratio(s,l):
    return np.round(s / l, 4)



def calculate_distances(pos1, pos2):
    return np.round(np.linalg.norm(np.array(pos1) - np.array(pos2)), 4)

def calculate_binding_boxes(coords):
    min_coords = np.min(coords, axis=0)
    max_coords = np.max(coords, axis=0)
    s = max_coords - min_coords
    volume = round(s[0] * s[1] *s[2], 4)
    return s, volume


parser = argparse.ArgumentParser(description="Visualize and analyze PDB structures.")
parser.add_argument("--id", nargs="+", required=True, help="List of PDB IDs (e.g., --id 1MBN 256B)")
parser.add_argument("--output", help="Optional output directory for saving images", default=None)
args = parser.parse_args()
pdb_ids = args.id
out_path = args.output

for pdb_id in pdb_ids:
    # Download PDB file
    print(f"Downloading PDB file for: {pdb_id}")  # Debugging step
    pdb_files = download_get_pdb(pdb_id)

    file = download_get_pdb(pdb_id)[0]
    info = extraction_data(file)

    for model in info:
        print(f"{pdb_id}\tAnteil AS in Sekundaerstruktur {secondary_str_ratio(model[0], model[1])}")
        print(f"{pdb_id}\t Abstand C_alpha {calculate_distances(model[2], model[4])}")
        print(f"{pdb_id}\t Abstand C_beta {calculate_distances(model[3], model[5])}")
        print(
            f"{pdb_id}\t X-Groesse {max(calculate_distances(model[2][0], model[4][0]), calculate_distances(model[3][0], model[5][0]))}")
        print(
            f"{pdb_id}\t Y-Groesse {max(calculate_distances(model[2][1], model[4][1]), calculate_distances(model[3][1], model[5][1]))}")
        print(
            f"{pdb_id}\t Z-Groesse {max(calculate_distances(model[2][2], model[4][2]), calculate_distances(model[3][2], model[5][2]))}")

        bounding_box, volume = calculate_binding_boxes(model[6])
        print(f"{pdb_id}\tVolumen\t{volume:.4f}")

#print(f"{pdb_id}\tVolumen\t{volume:.4f}")
#bounding_box, volume = calculate_binding_boxes(coordinates)
#command = "java -jar Jmol.jar  fileName  -J load=" + id + "; cartoons only; color cartoon structure; "
    #if out_path == '-':
        #command += "write png " + id + ".png"
   # else:
       # command += "write png " + out_path + '/' + id + ".png"

    #process = subprocess.run(command)
   # output, error = process.communicate()




