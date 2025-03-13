#!/usr/bin/env python3

import argparse
import requests  # Correct usage of requests module
import sys



def fasta_main(ac_number):
    uniprot_url = f"https://rest.uniprot.org/uniprotkb/{ac_number}.fasta"
    try:
        response = requests.get(uniprot_url)  # Corrected request method
        response.raise_for_status()  # Raise an error for HTTP errors (like 404)
        print(response.text)  # Print FASTA sequence
    except requests.exceptions.HTTPError as http_err:
        print(f"HTTP error: {http_err} - The AC-number '{ac_number}' is not  valid.")



if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='AC number from SwissProt DB')
    parser.add_argument("--ac", type=str,required=True,help="SwissProt AC-Nummer ")
    args = parser.parse_args().ac

    fasta_main(args)






