#!/usr/bin/env python3
import cgi
import cgitb
import subprocess
import os
import tempfile
import json
import mysql.connector
from pathlib import Path

# Enable CGI error reporting
cgitb.enable()

# HTML template with embedded CSS and JavaScript
HTML_TEMPLATE = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Sequence Alignment Tool</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 20px;
            background-color: #f5f5f5;
        }
        .container {
            max-width: 800px;
            margin: 0 auto;
            background-color: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        .form-group {
            margin-bottom: 20px;
        }
        label {
            display: block;
            margin-bottom: 8px;
            font-weight: bold;
            color: #333;
        }
        select, input[type="text"], input[type="number"], textarea {
            width: 100%;
            padding: 8px;
            border: 1px solid #ddd;
            border-radius: 4px;
            box-sizing: border-box;
            margin-bottom: 10px;
        }
        textarea {
            height: 100px;
            font-family: monospace;
        }
        .input-section {
            margin-top: 10px;
            display: none;
        }
        .input-section.active {
            display: block;
        }
        .database-input {
            display: grid;
            grid-template-columns: 1fr;
            gap: 10px;
        }
        button {
            background-color: #4CAF50;
            color: white;
            padding: 10px 20px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-size: 16px;
            width: 100%;
        }
        button:hover {
            background-color: #45a049;
        }
        .result {
            margin-top: 20px;
            padding: 15px;
            border: 1px solid #ddd;
            border-radius: 4px;
            background-color: #f9f9f9;
        }
        .alignment-text {
            font-family: monospace;
            white-space: pre;
            overflow-x: auto;
            background-color: #fff;
            padding: 10px;
            border: 1px solid #eee;
            border-radius: 4px;
        }
        .error {
            color: #d32f2f;
            background-color: #ffebee;
            padding: 10px;
            border-radius: 4px;
            margin: 10px 0;
        }
        .loading {
            text-align: center;
            padding: 20px;
            display: none;
        }
        .stats {
            margin-top: 15px;
            padding: 15px;
            background-color: white;
            border: 1px solid #ddd;
            border-radius: 4px;
        }
        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 15px;
            margin-top: 10px;
        }
        .stat-item {
            background-color: #f5f5f5;
            padding: 10px;
            border-radius: 4px;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .stat-item strong {
            color: #333;
        }
        .stat-item .score {
            font-weight: bold;
            color: #4CAF50;
        }
        .matrix-container {
            overflow-x: auto;
            margin: 20px 0;
        }
        .backtrack-matrix {
            border-collapse: collapse;
            font-family: monospace;
            font-size: 14px;
        }
        .backtrack-matrix th {
            padding: 5px;
            background-color: #f0f0f0;
            border: 1px solid #ddd;
        }
        .backtrack-matrix td {
            padding: 5px;
            border: 1px solid #ddd;
            text-align: center;
            min-width: 50px;
        }
        .backtrack-matrix .path {
            background-color: #90EE90;
        }
        .backtrack-matrix .header {
            background-color: #f0f0f0;
            font-weight: bold;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>Sequence Alignment Tool</h1>
        <form id="alignmentForm" enctype="multipart/form-data">
            <!-- First Sequence -->
            <div class="form-group">
                <label>First Sequence Source:</label>
                <select name="sequence1_source" class="sequence-source" data-target="sequence1">
                    <option value="paste">Paste Sequence</option>
                    <option value="database">Select from Database</option>
                    <option value="upload">Upload File</option>
                </select>

                <!-- Paste Input -->
                <div id="sequence1-paste" class="input-section">
                    <textarea name="sequence1" placeholder="Enter sequence"></textarea>
                </div>

                <!-- Database Input -->
                <div id="sequence1-database" class="input-section">
                    <select name="db_type1" class="database-select">
                        <option value="swissprot">SwissProt ID</option>
                        <option value="pdb">PDB ID</option>
                        <option value="bioprakt2">Bioprakt2 ID</option>
                    </select>
                    <input type="text" name="db_id1" placeholder="Enter database ID">
                </div>

                <!-- File Input -->
                <div id="sequence1-upload" class="input-section">
                    <input type="file" name="sequence1_file">
                </div>
            </div>

            <!-- Second Sequence -->
            <div class="form-group">
                <label>Second Sequence Source:</label>
                <select name="sequence2_source" class="sequence-source" data-target="sequence2">
                    <option value="paste">Paste Sequence</option>
                    <option value="database">Select from Database</option>
                    <option value="upload">Upload File</option>
                </select>

                <!-- Paste Input -->
                <div id="sequence2-paste" class="input-section">
                    <textarea name="sequence2" placeholder="Enter sequence"></textarea>
                </div>

                <!-- Database Input -->
                <div id="sequence2-database" class="input-section">
                    <select name="db_type2" class="database-select">
                        <option value="swissprot">SwissProt ID</option>
                        <option value="pdb">PDB ID</option>
                        <option value="bioprakt2">Bioprakt2 ID</option>
                    </select>
                    <input type="text" name="db_id2" placeholder="Enter database ID">
                </div>

                <!-- File Input -->
                <div id="sequence2-upload" class="input-section">
                    <input type="file" name="sequence2_file">
                </div>
            </div>

            <!-- Alignment Options -->
            <div class="form-group">
                <label for="algorithm">Algorithm:</label>
                <select name="algorithm" id="algorithm">
                    <option value="gotoh">Gotoh</option>
                    <option value="nw">Needleman-Wunsch</option>
                    <option value="sw">Smith-Waterman</option>
                </select>
            </div>

            <div class="form-group">
                <label for="mode">Mode:</label>
                <select name="mode" id="mode">
                    <option value="global">Global</option>
                    <option value="local">Local</option>
                    <option value="freeshift">Free Shift</option>
                </select>
            </div>

            <div class="form-group">
                <label for="matrix">Substitution Matrix:</label>
                <select name="matrix" id="matrix">
                    <option value="blosum62">BLOSUM62</option>
                    <option value="pam250">PAM250</option>
                    <option value="dayhoff">Dayhoff</option>
                    <option value="BlakeCohenMatrix">BlakeCohenMatrix</option>
                </select>
            </div>

            <div class="form-group">
                <label for="gap_open">Gap Open Penalty:</label>
                <input type="number" name="gap_open" id="gap_open" value="-12" step="0.1">
            </div>

            <div class="form-group">
                <label for="gap_extend">Gap Extend Penalty:</label>
                <input type="number" name="gap_extend" id="gap_extend" value="-1" step="0.1">
            </div>

            <button type="submit">Run Alignment</button>
        </form>

        <div id="loading" class="loading">
            Processing alignment...
        </div>

        <div id="result" class="result" style="display: none;">
            <h2>Results</h2>
            <div id="alignmentResult"></div>
            <div id="statsResult" class="stats"></div>
        </div>
    </div>

    <script>
        // Handle sequence source selection
        document.querySelectorAll('.sequence-source').forEach(select => {
            select.addEventListener('change', function() {
                const target = this.dataset.target;
                const value = this.value;

                // Hide all input sections for this sequence
                ['paste', 'database', 'upload'].forEach(type => {
                    const section = document.getElementById(`${target}-${type}`);
                    section.classList.remove('active');
                });

                // Show selected input section
                const selectedSection = document.getElementById(`${target}-${value}`);
                selectedSection.classList.add('active');
            });

            // Trigger change event to set initial state
            select.dispatchEvent(new Event('change'));
        });

        // Handle algorithm mode updates
        function updateModeOptions() {
            const algorithm = document.getElementById('algorithm').value;
            const modeSelect = document.getElementById('mode');
            const currentMode = modeSelect.value;

            modeSelect.innerHTML = '';

            if (algorithm === 'nw') {
                modeSelect.add(new Option('Global', 'global'));
                modeSelect.disabled = true;
            } else if (algorithm === 'sw') {
                modeSelect.add(new Option('Local', 'local'));
                modeSelect.add(new Option('Free Shift', 'freeshift'));
                modeSelect.value = currentMode === 'global' ? 'local' : currentMode;
                modeSelect.disabled = false;
            } else {
                modeSelect.add(new Option('Global', 'global'));
                modeSelect.add(new Option('Local', 'local'));
                modeSelect.add(new Option('Free Shift', 'freeshift'));
                modeSelect.value = currentMode;
                modeSelect.disabled = false;
            }
        }

        document.getElementById('algorithm').addEventListener('change', updateModeOptions);
        updateModeOptions();

        // Handle form submission
        document.getElementById('alignmentForm').addEventListener('submit', async function(e) {
            e.preventDefault();

            const loading = document.getElementById('loading');
            const result = document.getElementById('result');
            const alignmentResult = document.getElementById('alignmentResult');
            const statsResult = document.getElementById('statsResult');

            loading.style.display = 'block';
            result.style.display = 'none';

            try {
                const formData = new FormData(this);

                const response = await fetch(window.location.href, {
                    method: 'POST',
                    headers: {
                        'X-Requested-With': 'XMLHttpRequest'
                    },
                    body: formData
                });

                const data = await response.json();

                if (data.error) {
                    alignmentResult.innerHTML = `<div class="error">${data.error}</div>`;
                    statsResult.innerHTML = '';
                } else {
                    alignmentResult.innerHTML = `
                        <h3>Alignment</h3>
                        <div class="alignment-text">${data.alignment}</div>
                        <h3>Backtracking Matrix</h3>
                        ${data.matrix_visualization}
                        <h3>Debug Output</h3>
                        <pre style="background: #f5f5f5; padding: 10px; overflow: auto;">${data.raw_output}</pre>
                    `;

                    if (data.statistics) {
                        const stats = JSON.parse(data.statistics);
                        statsResult.innerHTML = `
                            <h3>Statistics</h3>
                            <div class="stats-grid">
                                <div class="stat-item">
                                    <strong>Alignment Score:</strong>
                                    <span class="score">${stats["Alignment Score"]}</span>
                                </div>
                                <div class="stat-item">
                                    <strong>Length:</strong>
                                    <span>${stats["Alignment Length"]}</span>
                                </div>
                                <div class="stat-item">
                                    <strong>Matches:</strong>
                                    <span>${stats["Matches"]}</span>
                                </div>
                                <div class="stat-item">
                                    <strong>Mismatches:</strong>
                                    <span>${stats["Mismatches"]}</span>
                                </div>
                                <div class="stat-item">
                                    <strong>Gaps:</strong>
                                    <span>${stats["Gaps"]}</span>
                                </div>
                                <div class="stat-item">
                                    <strong>Identity:</strong>
                                    <span>${stats["Identity"]}</span>
                                </div>
                            </div>
                        `;
                    }
                }
            } catch (error) {
                alignmentResult.innerHTML = `<div class="error">Error: ${error.message}</div>`;
                statsResult.innerHTML = '';
            } finally {
                loading.style.display = 'none';
                result.style.display = 'block';
            }
        });
    </script>
</body>
</html>
"""


def get_db_connection():
    """Create database connection"""
    try:
        conn = mysql.connector.connect(
            host="mysql2-ext.bio.ifi.lmu.de",
            user="bioprakt2",
            port=3306,
            password="$1$xyWsttEl$sAmFI1NOY5sVGpgmzf1ga1",
            database="bioprakt2"
        )
        return conn
    except mysql.connector.Error as e:
        raise Exception(f"Database connection error: {e}")


def calculate_alignment_metrics(seq1_aligned, seq2_aligned):
    """Calculate alignment metrics similar to the validation file"""
    if not seq1_aligned or not seq2_aligned:
        return {
            "sensitivity": 0.0,
            "specificity": 0.0,
            "coverage": 0.0,
            "mean_shift_error": 0.0,
            "inverse_mean_shift_error": 0.0
        }

    # Calculate aligned positions
    aligned_positions = []
    for i in range(len(seq1_aligned)):
        if seq1_aligned[i] != '-' and seq2_aligned[i] != '-':
            aligned_positions.append(i)

    # Calculate coverage
    total_length = max(len(seq1_aligned.replace('-', '')), len(seq2_aligned.replace('-', '')))
    aligned_length = len(aligned_positions)
    coverage = aligned_length / total_length if total_length > 0 else 0.0

    # Calculate mean shift error
    shifts = []
    for i in range(len(aligned_positions) - 1):
        shift = aligned_positions[i + 1] - aligned_positions[i]
        shifts.append(shift)

    mean_shift_error = sum(shifts) / len(shifts) if shifts else 0.0
    inverse_mean_shift_error = 1.0 / mean_shift_error if mean_shift_error > 0 else 0.0

    return {
        "sensitivity": coverage,  # Simplified approximation
        "specificity": coverage,  # Simplified approximation
        "coverage": coverage,
        "mean_shift_error": mean_shift_error,
        "inverse_mean_shift_error": inverse_mean_shift_error
    }


def create_backtrack_matrix_html(seq1, seq2, matrix_data):
    """Create HTML visualization of the backtracking matrix"""
    if not matrix_data:
        return "No matrix available."

    html = """
    <style>
        .matrix-container {
            overflow-x: auto;
            margin: 20px 0;
        }
        .backtrack-matrix {
            border-collapse: collapse;
            font-family: monospace;
            font-size: 14px;
        }
        .backtrack-matrix th {
            padding: 5px;
            background-color: #f0f0f0;
            border: 1px solid #ddd;
        }
        .backtrack-matrix td {
            padding: 5px;
            border: 1px solid #ddd;
            text-align: center;
            min-width: 50px;
        }
        .backtrack-matrix .path {
            background-color: #90EE90;
        }
        .backtrack-matrix .header {
            background-color: #f0f0f0;
            font-weight: bold;
        }
    </style>
    <div class="matrix-container">
        <table class="backtrack-matrix">
            <tr>
                <th></th>
                <th></th>
    """

    # Add sequence 2 as column headers
    for char in seq2:
        html += f"<th>{char}</th>"
    html += "</tr>"

    # Add matrix rows with sequence 1 as row headers
    for i, header in enumerate(['-'] + list(seq1)):
        html += "<tr>"
        if i == 0:
            html += '<th></th>'
        else:
            html += f'<th>{header}</th>'

        for j in range(len(matrix_data[i])):
            cell_value = matrix_data[i][j]
            cell_class = ' class="path"' if cell_value.get('in_path', False) else ''
            html += f'<td{cell_class}>{cell_value["score"]:.1f}</td>'

        html += "</tr>"

    html += """
        </table>
    </div>
    """
    return html


def create_colored_alignment_html(seq1_aligned, seq2_aligned):
    """Create an HTML snippet with colored alignment:
       - Blue for matches (non-gap characters that are equal)
       - Red for mismatches (non-gap characters that differ)
       - Gaps are left uncolored.
    """
    html = '<div style="font-family: monospace; white-space: pre;">'
    colored_line1 = ""
    colored_line2 = ""
    for a, b in zip(seq1_aligned, seq2_aligned):
        if a == b and a != '-':
            colored_line1 += f'<span style="color: blue;">{a}</span>'
            colored_line2 += f'<span style="color: blue;">{b}</span>'
        elif a != b and a != '-' and b != '-':
            colored_line1 += f'<span style="color: red;">{a}</span>'
            colored_line2 += f'<span style="color: red;">{b}</span>'
        else:
            # Leave gaps or cases with one gap uncolored
            colored_line1 += a
            colored_line2 += b
    html += colored_line1 + "\n" + colored_line2 + "</div>"
    return html


def run_alignment(seq1, seq2, algorithm, mode, matrix, gap_open, gap_extend):
    """Run sequence alignment"""
    try:
        # Create temporary files with headers (needed for the pairs file)
        with tempfile.NamedTemporaryFile(mode='w', delete=False) as seqlib_file:
            seqlib_file.write(f"1: {seq1}\n2: {seq2}\n")
            seqlib_path = seqlib_file.name

        with tempfile.NamedTemporaryFile(mode='w', delete=False) as pairs_file:
            pairs_file.write("1 2\n")
            pairs_path = pairs_file.name

        # Prepare alignment command
        cmd = [
            "java", "-jar", "/mnt/biocluster/praktikum/bioprakt/progprakt-02/Solution3/Alignment/alignment.jar",
            "--seqlib", seqlib_path,
            "--pairs", pairs_path,
            "-m", f"/mnt/biocluster/praktikum/bioprakt/Data/MATRICES/{matrix}.mat",
            "--go", str(gap_open),
            "--ge", str(gap_extend),
            "--mode", mode,
            "--format", "ali",
            "--dpmatrices", "true"
        ]

        if algorithm != "gotoh":
            cmd.append("--nw")

        # Run alignment with universal_newlines instead of text
        process = subprocess.Popen(cmd,
                                   stdout=subprocess.PIPE,
                                   stderr=subprocess.PIPE,
                                   universal_newlines=True)
        stdout, stderr = process.communicate()

        if process.returncode != 0:
            return {"error": f"Alignment failed: {stderr}"}

        # Debug output

        # Process results
        alignment_lines = stdout.split('\n')
        score = None
        alignment = []
        matrix_data = []
        current_section = None
        backtrack_data = []
        in_backtrack = False

        # Extract aligned sequences and matrix data
        for line in alignment_lines:
            if line.startswith('>'):
                try:
                    score = float(line.split()[-1])
                except (IndexError, ValueError):
                    pass
            elif line.startswith('Backtrack Matrix:'):
                in_backtrack = True
                continue
            elif in_backtrack and line.strip():
                if line.strip() and not line.startswith('sequence') and not line.startswith('structure'):
                    backtrack_data.append(line.strip().split())
            elif line.strip() and not line.startswith('structure') and not line.startswith('sequence'):
                if not in_backtrack:
                    alignment.append(line)

        if len(alignment) < 2:
            return {"error": "Invalid alignment output"}

        # Strip header info from the aligned sequences
        def strip_header(aln_line):
            if ':' in aln_line:
                return aln_line.split(':', 1)[1].strip()
            return aln_line.strip()

        # Get the aligned sequences
        aligned_seq1 = strip_header(alignment[0])
        aligned_seq2 = strip_header(alignment[1])

        # Calculate detailed statistics
        alignment_length = len(aligned_seq1)
        matches = sum(1 for a, b in zip(aligned_seq1, aligned_seq2) if a == b and a != '-')
        gaps = sum(1 for a, b in zip(aligned_seq1, aligned_seq2) if a == '-' or b == '-')
        mismatches = alignment_length - matches - gaps
        identity = (matches / alignment_length) * 100 if alignment_length > 0 else 0
        
        # Calculate additional metrics
        gap_openings = 0
        current_gap = False
        for a, b in zip(aligned_seq1, aligned_seq2):
            if a == '-' or b == '-':
                if not current_gap:
                    gap_openings += 1
                    current_gap = True
            else:
                current_gap = False

        similarity = 0
        for a, b in zip(aligned_seq1, aligned_seq2):
            if a != '-' and b != '-':
                if a == b:
                    similarity += 1
                # You could add more conditions here for similar amino acids

        similarity_percentage = (similarity / alignment_length) * 100 if alignment_length > 0 else 0

        stats = {
            "Alignment Score": f"{score:.2f}" if score is not None else "N/A",
            "Alignment Length": alignment_length,
            "Matches": matches,
            "Mismatches": mismatches,
            "Gaps": gaps,
            "Gap Openings": gap_openings,
            "Identity": f"{identity:.2f}%",
            "Similarity": f"{similarity_percentage:.2f}%",
            "Sequence 1 Length": len(seq1),
            "Sequence 2 Length": len(seq2),
            "Coverage": f"{(alignment_length / max(len(seq1), len(seq2))) * 100:.2f}%"
        }

        # Create backtrack matrix visualization if data is available
        matrix_html = ""
        if backtrack_data:
            matrix_html = f"""
            <div class="matrix-container">
                <table class="backtrack-matrix">
                    <tr>
                        <th></th>
                        <th></th>
                        {''.join(f'<th>{c}</th>' for c in seq2)}
                    </tr>
            """
            
            for i, row in enumerate(backtrack_data):
                matrix_html += "<tr>"
                if i == 0:
                    matrix_html += '<th></th>'
                else:
                    matrix_html += f'<th>{seq1[i-1]}</th>'
                
                for value in row:
                    matrix_html += f'<td>{value}</td>'
                matrix_html += "</tr>"
            
            matrix_html += "</table></div>"

        return {
            "alignment": "\n".join(alignment),
            "statistics": json.dumps(stats, indent=2),
            "matrix_visualization": matrix_html if backtrack_data else "Matrix visualization available soon"
        }

    except Exception as e:
        return {"error": f"Error during alignment: {str(e)}"}

    finally:
        # Cleanup temporary files
        try:
            os.unlink(seqlib_path)
            os.unlink(pairs_path)
        except Exception:
            pass


def get_sequence_from_database(db_type, db_id):
    """Retrieve sequence from database based on database type and ID"""
    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        if db_type == "swissprot":
            query = "SELECT sequence FROM Sequences WHERE swissprot_id = %s LIMIT 1"
        elif db_type == "pdb":
            query = "SELECT sequence FROM Sequence_test WHERE pdb_id = %s LIMIT 1"
        elif db_type == "bioprakt2":
            query = "SELECT sequence FROM Sequences WHERE sequence_id = %s LIMIT 1"
        else:
            raise ValueError(f"Invalid database type: {db_type}")

        cursor.execute(query, (db_id,))
        result = cursor.fetchone()

        if not result:
            raise Exception(f"No sequence found for ID {db_id} in {db_type}")

        return result[0]

    finally:
        if conn:
            conn.close()


def get_sequence_from_file(file_item):
    """Read sequence from uploaded file"""
    if not file_item or not file_item.file:
        raise ValueError("No file uploaded")

    content = file_item.file.read().decode('utf-8')
    # Remove any whitespace and newlines
    return ''.join(content.split())


def print_backtrack_matrix(backtrack, seq1, seq2):
    """Print the backtrack matrix with sequence headers"""
    if not backtrack:
        print("No backtrack matrix available")
        return

    # Print the column headers (sequence 2)
    print("     ", end="")
    for char in seq2:
        print(f"{char:>4}", end="")
    print()

    # Print each row with sequence 1 characters
    for i, row in enumerate(backtrack):
        if i == 0:
            print("  ", end="")
        else:
            print(f"{seq1[i - 1]} ", end="")

        # Print the matrix values
        for value in row:
            print(f"{value:>4}", end="")
        print()


def main():
    """Main CGI handler"""
    # Check if this is an AJAX request
    is_ajax = os.environ.get('HTTP_X_REQUESTED_WITH') == 'XMLHttpRequest'

    if is_ajax:
        print("Content-Type: application/json")
        print()

        form = cgi.FieldStorage()

        try:
            # Get sequences based on source
            seq1 = ""
            seq2 = ""

            # Handle first sequence
            seq1_source = form.getvalue("sequence1_source")
            if seq1_source == "database":
                db_type1 = form.getvalue("db_type1")
                db_id1 = form.getvalue("db_id1")
                seq1 = get_sequence_from_database(db_type1, db_id1)
            elif seq1_source == "paste":
                seq1 = form.getvalue("sequence1", "").strip()
            elif seq1_source == "upload" and "sequence1_file" in form:
                seq1 = get_sequence_from_file(form["sequence1_file"])

            # Handle second sequence
            seq2_source = form.getvalue("sequence2_source")
            if seq2_source == "database":
                db_type2 = form.getvalue("db_type2")
                db_id2 = form.getvalue("db_id2")
                seq2 = get_sequence_from_database(db_type2, db_id2)
            elif seq2_source == "paste":
                seq2 = form.getvalue("sequence2", "").strip()
            elif seq2_source == "upload" and "sequence2_file" in form:
                seq2 = get_sequence_from_file(form["sequence2_file"])

            if not seq1 or not seq2:
                print(json.dumps({"error": "Both sequences are required"}))
                return

            # Run alignment
            result = run_alignment(
                seq1,
                seq2,
                form.getvalue("algorithm", "gotoh"),
                form.getvalue("mode", "global"),
                form.getvalue("matrix", "blosum62"),
                float(form.getvalue("gap_open", -12)),
                float(form.getvalue("gap_extend", -1))
            )

            print(json.dumps(result))

        except Exception as e:
            print(json.dumps({"error": str(e)}))
    else:
        # Show HTML interface
        print("Content-Type: text/html")
        print()
        print(HTML_TEMPLATE)


if __name__ == "__main__":
    main()