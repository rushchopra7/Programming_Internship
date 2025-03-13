import mysql.connector

mydb = mysql.connector.connect(
  host="localhost",
  user="bioprakt2",
  password="$1$xyWsttEl$sAmFI1NOY5sVGpgmzf1ga1",
  database = "bioprakt2",
  port = 3307
)

print(mydb)