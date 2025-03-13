

CREATE TABLE IF NOT EXISTS Source  (
    source_id INT AUTO_INCREMENT PRIMARY KEY,
    source_name VARCHAR(255) NOT NULL,
    database_name VARCHAR(255) NOT NULL
);


CREATE TABLE IF NOT EXISTS Organism (
    organism_id INT NOT NULL AUTO_INCREMENT,
	organism_name VARCHAR(255) NOT NULL,
    PRIMARY KEY(organism_id)
    
);


CREATE TABLE IF NOT EXISTS Sequences (
    sequence_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    fasta_id varchar(250) default null,
    sequence longtext NOT NULL,
    source_id varchar(250) not null,
    organism_id INT not null,
    sequence_length INT GENERATED ALWAYS AS (CHAR_LENGTH(sequence)) STORED,
    FOREIGN KEY (source_id) REFERENCES Source(source_id) ON DELETE SET NULL,
    FOREIGN KEY (organism_id) REFERENCES Organism(organism_id) ON DELETE SET NULL
);


CREATE TABLE IF NOT EXISTS Keyword (
    keyword_id INT not null AUTO_INCREMENT PRIMARY KEY,
    keyword VARCHAR(255) UNIQUE NOT NULL,
    sequence_id int not null
);



CREATE TABLE IF NOT EXISTS SecondaryStructure (
    structure_id INT not null AUTO_INCREMENT PRIMARY KEY,
    sequence_id INT,
    structure TEXT NOT NULL,
    protein_name varchar(250),
    prediction_method VARCHAR(255),
    FOREIGN KEY (sequence_id) REFERENCES Sequence(sequence_id) ON DELETE CASCADE
);


CREATE TABLE IF NOT EXISTS SequenceEvolution (
     id int not null auto_increment primary key,
     sequence_id int, 
     common_ancestor varchar(250),
     evolutionary_group varchar(250),
     date_created TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- When the entry was added
     FOREIGN KEY (sequence_id) REFERENCES Sequences(sequence_id) ON DELETE CASCADE
);

CREATE TABLE DB (
    database_id INT not null AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,  
    description TEXT,  
    url VARCHAR(500),  
    release_version VARCHAR(50),  
    last_updated DATE 
    
);

CREATE TABLE Sequence_Database (
    sequence_id INT,
    database_id INT,
    database_accession VARCHAR(255) NOT NULL, -- Unique ID of the sequence in this database (e.g., UniProt ID: P12345)
    PRIMARY KEY (sequence_id, database_id),  
    FOREIGN KEY (sequence_id) REFERENCES Sequence(sequence_id) ON DELETE CASCADE,  
    FOREIGN KEY (database_id) REFERENCES DB(database_id) ON DELETE CASCADE  
);









  
