#!/usr/bin/env python3

print("Content-Type: text/html")
print()

print("""
<!DOCTYPE html>
<html lang="de">
<head>
    <meta charset="UTF-8">
    <link rel="stylesheet" type="text/css" href="/~musienko/styles.css">
</head>
<body>

    <div class="sidenav">
        <button class="dropdown-btn">Solution </button>
        <div class="dropdown-container">
            <a href="/~chopra/acsearchCGI.py">Swissprot AC Search</a>
            <a href="/~gryb/keywordsearchCGI.py">Swissprot Keyword Search</a>
            <a href="/~gryb/psscan.cgi">Prosite Pattern Scan</a>
            <a href="/~musienko/genome_length_cgi.py">Genome Report</a>
            <a href="/~musienko/dna2rna_cgi.py">DNA to RNA</a>
            <a href="/~chopra/alignment_cgi.py">Alignment</a>
            
        </div>
    </div>

    <div class="main-content">
        <h1>Welcome to Group 2</h1>
        <h2>Our members are:</h2>
        <ul>
            <li>Gryb Anastasiya</li>
            <li>Rushali Chopra</li>
            <li>Nhat Linh Tran</li>
            <li>Katerina Musienko</li>
        </ul>
        <h2>Our supervisors are:</h2>
        <p>Herr Heun</p>
        <p>Armin Hadziahmetovic</p>
    </div>

    <script>
        document.querySelector(".dropdown-btn").addEventListener("click", function() {
            this.classList.toggle("active");
            var dropdownContent = this.nextElementSibling;
            if (dropdownContent.style.display === "block") {
                dropdownContent.style.display = "none";
            } else {
                dropdownContent.style.display = "block";
            }
        });
    </script>

</body>
</html>
""")