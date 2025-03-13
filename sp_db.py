
#!/usr/bin/env python3


#read sequences from swissprot db  and then add these in db..
# also check for the case when the sequence is already existing in the db


import mysql
import argparse



#connection = mysql.connector.connect(host="127.0.0.1",port=3307,user="bioprakt2",
#password="$1$xyWsttEl$sAmFI1NOY5sVGpgmzf1ga1",database="bioprakt2")
#cursor = connection.cursor()

#parser = argparse.ArgumentParser()
#parser.add_argument("--input", type = argparse.FileType('r'), default= sys.stdin)
#args=parser.parse_args()




import mysql.connector
from mysql.connector import Error

def prep_file(input_file):
    """Parses a SwissProt-like file and returns entries as a list of dictionaries."""
    entries = []
    entry = {}
    parsing_sequence = False  # Flag to indicate sequence parsing

    for line in input_file:
        key = line[:2].strip()
        value = line[5:].strip()

        if key == "//":  # End of an entry
            if entry:  # Ensure entry is not empty
                entries.append(entry)
            entry = {}  # Reset for next entry
            parsing_sequence = False

        elif key == "SQ":  # Start of sequence section
            parsing_sequence = True
            entry["SQ"] = ""

        elif parsing_sequence:
            # Extract sequence parts, removing any non-alphabetic characters
            seq_part = ''.join([c for c in value if c.isalpha()])
            entry["SQ"] += seq_part

        else:
            if key in entry:
                entry[key] += " " + value
            else:
                entry[key] = value

    print(f"Prepared {len(entries)} entries.")  # Debugging statement
    return entries

def insert_DB(connection, entries):
    """Inserts parsed entries into the database."""
    try:
        with connection.cursor() as cursor:
            for entry in entries:
                sequence = entry.get("SQ", "").strip()
                if not sequence:
                    print(f"Warning: No sequence for entry {entry.get('AC', 'Unknown')}")
                    continue

                organism_name = entry.get("OS", "Unknown").split(';')[0].strip()[:255]
                source_name = "SwissProt"
                swissprot_id = entry.get("AC", "Unknown")[:255]
                keywords = [kw.strip()[:255] for kw in entry.get("KW", "").split(";") if kw.strip()]
                pdb_id = ""
                structure = entry.get("STRUCTURE", "").strip()[:255]
                family = entry.get("FAMILY", "").strip()[:255]
                classes = entry.get("CLASSES", "").strip()[:255]

                # Extract PDB ID from DR lines
                dr_lines = entry.get("DR", "")
                if isinstance(dr_lines, str):
                    dr_lines = dr_lines.split()
                for dr in dr_lines:
                    if 'PDB;' in dr:
                        parts = [p.strip() for p in dr.split(';')]
                        if len(parts) >= 2:
                            pdb_id = parts[1][:10]
                            break

                # Get or insert Source
                cursor.execute("SELECT source_id FROM Source WHERE source_name = %s", (source_name,))
                source_row = cursor.fetchone()
                if source_row:
                    source_id = source_row[0]
                else:
                    cursor.execute("INSERT INTO Source (source_name, url) VALUES (%s, %s)",
                                   (source_name, "https://www.uniprot.org"))
                    source_id = cursor.lastrowid
                connection.commit()  # Commit after inserting Source

                # Get or insert Organism
                cursor.execute("SELECT organism_id FROM Organism WHERE organism_name = %s", (organism_name,))
                organism_row = cursor.fetchone()
                if organism_row:
                    organism_id = organism_row[0]
                else:
                    cursor.execute("INSERT INTO Organism (organism_name) VALUES (%s)", (organism_name,))
                    organism_id = cursor.lastrowid
                connection.commit()  # Commit after inserting Organism

                # Process each FASTA ID in the AC line
                fasta_ids = entry.get("AC", "Unknown").split(";")
                for fasta_id in fasta_ids:
                    fasta_id = fasta_id.strip()
                    if not fasta_id:
                        continue  # Skip empty FASTA IDs

                    # Check for existing FASTA ID
                    cursor.execute("SELECT 1 FROM Sequences WHERE fasta_id = %s", (fasta_id,))
                    if cursor.fetchone():
                        print(f"Sequence {fasta_id} already exists. Skipping.")
                        continue

                    cursor.execute(''' 
                        INSERT INTO Sequences (fasta_id, sequence, source_id, swissprot_id, organism_id, pdb_id)
                        VALUES (%s, %s, %s, %s, %s, %s)
                    ''', (fasta_id, sequence, source_id, swissprot_id, organism_id, pdb_id))
                    sequence_id = cursor.lastrowid
                    connection.commit()  # Commit after inserting Sequence
                    print(f"Sequence {fasta_id} added.")

                    # Insert Keywords and link to Sequence only if sequence is inserted successfully
                    if sequence_id:
                        for keyword in keywords:
                            cursor.execute("SELECT keyword_id FROM Keyword WHERE keyword = %s", (keyword,))
                            keyword_row = cursor.fetchone()
                            if keyword_row:
                                keyword_id = keyword_row[0]
                            else:
                                # Insert into Keyword table if it doesn't exist
                                cursor.execute("INSERT INTO Keyword (keyword, sequence_id) VALUES (%s, %s)",
                                               (keyword, sequence_id))
                                keyword_id = cursor.lastrowid
                                connection.commit()  # Commit after inserting Keyword
                    # Insert SecondaryStructure if data exists
                    if structure:
                        cursor.execute(''' 
                            INSERT INTO SecondaryStructure (sequence_id, structure, pdb_id)
                            VALUES (%s, %s, %s)
                        ''', (sequence_id, structure, pdb_id))
                        connection.commit()  # Commit after inserting SecondaryStructure

                    # Insert Homestrad and link to Sequence
                    if family and classes:
                        cursor.execute(''' 
                            INSERT INTO Homestrad (family, classes, pdb_id)
                            VALUES (%s, %s, %s)
                        ''', (family, classes, pdb_id))
                        homestrad_id = cursor.lastrowid
                        cursor.execute(''' 
                            INSERT INTO Seq_Homestrad (sequence_id, homestrad_id)
                            VALUES (%s, %s)
                        ''', (sequence_id, homestrad_id))
                        connection.commit()  # Commit after inserting Homestrad

                # After processing all FASTA IDs, commit any remaining operations if needed.
                connection.commit()

    except Error as e:
        connection.rollback()
        print(f"Database Error: {e}")
        print(f"Failed entry: {entry}")



def main():

    parser = argparse.ArgumentParser(description="Process SwissProt file and insert data into MySQL")
    parser.add_argument("--input", type=argparse.FileType('r'), required=True, help="Path to the SwissProt file")
    args = parser.parse_args()

    # Establish database connection using mysql.connector
    connection = mysql.connector.connect(
        host="mysql2-ext.bio.ifi.lmu.de",
        port=3306,
        user="bioprakt2",
        password="$1$xyWsttEl$sAmFI1NOY5sVGpgmzf1ga1",
        database="bioprakt2",

    )

    # Process the SwissProt file and insert into database
    processed_file = prep_file(args.input)
    insert_DB(connection, processed_file)

    # Print the processed entries
    #for e in processed_file:
    #    for f in e:
    #        print(f, e[f])

    # Close connection
    connection.close()

if __name__ == "__main__":
    main()





















