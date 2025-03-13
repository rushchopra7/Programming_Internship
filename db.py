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
