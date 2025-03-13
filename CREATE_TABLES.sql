CREATE TABLE IF NOT EXISTS Source (
    source_id INT AUTO_INCREMENT PRIMARY KEY,
    source_name VARCHAR(255) NOT NULL,
    url VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS Organism (
    organism_id INT AUTO_INCREMENT PRIMARY KEY,
    organism_name VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS Sequences (
    sequence_id INT AUTO_INCREMENT PRIMARY KEY,
    fasta_id VARCHAR(255),
    sequence LONGTEXT NOT NULL,
    source_id INT,
    swissprot_id VARCHAR(255),
    organism_id INT,
    structure_id INT,
    pdb_id VARCHAR(10),
    FOREIGN KEY (source_id) REFERENCES Source(source_id) ON DELETE SET NULL,
    FOREIGN KEY (organism_id) REFERENCES Organism(organism_id) ON DELETE SET NULL,
    FOREIGN KEY (structure_id) REFERENCES SecondaryStructure(structure_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS Keyword (
    keyword_id INT AUTO_INCREMENT PRIMARY KEY,
    keyword VARCHAR(255) UNIQUE NOT NULL,
    sequence_id INT NOT NULL,
    FOREIGN KEY (sequence_id) REFERENCES Sequences(sequence_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS SecondaryStructure (
    structure_id INT AUTO_INCREMENT PRIMARY KEY,
    sequence_id INT NOT NULL,
    structure TEXT NOT NULL,
    protein_name VARCHAR(255),
    prediction_method VARCHAR(255),
    FOREIGN KEY (sequence_id) REFERENCES Sequences(sequence_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS Homestrad (
    homestrad_id INT AUTO_INCREMENT PRIMARY KEY,
    family VARCHAR(255),
    classes LONGTEXT,
    pdb_id VARCHAR(10)
);

CREATE TABLE IF NOT EXISTS Seq_Homestrad (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sequence_id INT NOT NULL,
    homestrad_id INT NOT NULL,
    seq_alignment LONGTEXT,
    FOREIGN KEY (sequence_id) REFERENCES Sequences(sequence_id) ON DELETE CASCADE,
    FOREIGN KEY (homestrad_id) REFERENCES Homestrad(homestrad_id) ON DELETE CASCADE
);
