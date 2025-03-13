#!/usr/bin/python3

import cgi
import cgitb
import subprocess
import html

cgitb.enable()

print("Content-Type: text/html\r\n\r\n")  # Ensure headers are sent first

HTML_TEMPLATE = """
<!DOCTYPE html>
<html lang="de">
<head>
    <meta charset="UTF-8">
    <title>SwissProt AC Search</title>
    <style>
        body {{
            font-family: Arial, sans-serif;
            background-color: #f4f4f4;
            display: flex;
            justify-content: center;
            align-items: center;
            height: 100vh;
            margin: 0;
        }}
        .container {{
            background: white;
            padding: 20px;
            border-radius: 10px;
            box-shadow: 0px 4px 8px rgba(0, 0, 0, 0.1);
            width: 400px;
            text-align: center;
        }}
        h1 {{
            font-size: 20px;
            color: #333;
            margin-bottom: 15px;
        }}
        input {{
            width: 80%;
            padding: 8px;
            margin: 10px 0;
            border: 1px solid #ccc;
            border-radius: 5px;
            font-size: 16px;
        }}
        button {{
            background-color: #007bff;
            color: white;
            padding: 10px 15px;
            border: none;
            border-radius: 5px;
            font-size: 16px;
            cursor: pointer;
        }}
        button:hover {{
            background-color: #0056b3;
        }}
        .result, .error {{
            margin-top: 15px;
            padding: 10px;
            border-radius: 5px;
            font-size: 14px;
            text-align: left;
            word-wrap: break-word;
            white-space: pre-wrap;
        }}
        .result {{
            background-color: #e6f7e6;
            color: #008000;
            border: 1px solid #008000;
        }}
        .error {{
            background-color: #f8d7da;
            color: #721c24;
            border: 1px solid #f5c6cb;
        }}
    </style>
</head>
<body>
    <div class="container">
        <h1>Enter the SwissProt AC Number</h1>
        <form method="GET">
            <input type="text" name="ac" placeholder="e.g., P12345" required/>
            <button type="submit">Search</button>
        </form>

        {result_block}
        {error_block}
    </div>
</body>
</html>
"""

form = cgi.FieldStorage()
ac_number = form.getvalue("ac")
result_block = error_block = ""

if ac_number:
    try:
        # Sanitize and execute the subprocess safely
        sanitized_ac = html.escape(ac_number)
        output = subprocess.check_output(
            ["/usr/bin/python3", "/home/c/chopra/public_html/acsearch.py", "--ac", sanitized_ac],
            stderr=subprocess.STDOUT,
            timeout=10,
            universal_newlines=True
        )
        result_block = f"<div class='result'><strong>Result:</strong><br>{html.escape(output)}</div>"

    except subprocess.CalledProcessError as e:
        error_block = f"<div class='error'><strong>Error:</strong> Command failed with error: {html.escape(str(e.output))}</div>"
    except subprocess.TimeoutExpired:
        error_block = "<div class='error'><strong>Error:</strong> Request timed out. Please try again.</div>"
    except Exception as e:
        error_block = f"<div class='error'><strong>Error:</strong> {html.escape(str(e))}</div>"

print(HTML_TEMPLATE.format(
    result_block=result_block,
    error_block=error_block
))
