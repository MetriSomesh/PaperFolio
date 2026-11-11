import os
import csv
import io
import sqlite3
import urllib.request
import datetime

DB_OUTPUT_PATH = r"I:\projects\paper_trade\app\src\main\assets\stock_master.db"

def fetch_nse_stocks():
    print("Fetching NSE equity list...")
    url = "https://nsearchives.nseindia.com/content/equities/EQUITY_L.csv"
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
    }
    req = urllib.request.Request(url, headers=headers)
    stocks = []
    try:
        with urllib.request.urlopen(req, timeout=15) as response:
            content = response.read().decode('utf-8')
            reader = csv.DictReader(io.StringIO(content))
            for row in reader:
                sym = row.get("SYMBOL", "").strip()
                name = row.get("NAME OF COMPANY", "").strip()
                series = row.get(" SERIES", "").strip() or row.get("SERIES", "").strip()
                if sym and series == "EQ":
                    stocks.append({
                        "yahooSymbol": f"{sym}.NS",
                        "tradingSymbol": sym,
                        "companyName": name,
                        "exchange": "NSE",
                        "sector": None,
                        "isCustom": 0
                    })
        print(f"Fetched {len(stocks)} NSE stocks.")
    except Exception as e:
        print(f"Error fetching NSE stocks: {e}")
    return stocks

def fetch_nse_etfs():
    print("Fetching NSE ETF list...")
    url = "https://nsearchives.nseindia.com/content/equities/eq_etfseclist.csv"
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
    }
    req = urllib.request.Request(url, headers=headers)
    etfs = []
    try:
        with urllib.request.urlopen(req, timeout=15) as response:
            content = response.read().decode('utf-8')
            reader = csv.DictReader(io.StringIO(content))
            for row in reader:
                sym = row.get("Symbol", "").strip() or row.get("SYMBOL", "").strip()
                name = row.get("Security Name", "").strip() or row.get("NAME OF COMPANY", "").strip() or sym
                underlying = row.get("Underlying", "").strip() or "ETF"
                if sym:
                    etfs.append({
                        "yahooSymbol": f"{sym}.NS",
                        "tradingSymbol": sym,
                        "companyName": name,
                        "exchange": "NSE",
                        "sector": f"ETF ({underlying})",
                        "isCustom": 0
                    })
        print(f"Fetched {len(etfs)} NSE ETFs.")
    except Exception as e:
        print(f"Error fetching NSE ETFs: {e}")
    return etfs

def fetch_bse_stocks():
    print("Fetching BSE Bhavcopy...")
    stocks = []
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Accept": "*/*"
    }
    today = datetime.date.today()
    found = False
    for i in range(1, 10):
        d = today - datetime.timedelta(days=i)
        if d.weekday() >= 5:
            continue
        date_str = d.strftime("%Y%m%d")
        url = f"https://www.bseindia.com/download/BhavCopy/Equity/BhavCopy_BSE_CM_0_0_0_{date_str}_F_0000.CSV"
        try:
            req = urllib.request.Request(url, headers=headers)
            with urllib.request.urlopen(req, timeout=10) as response:
                content = response.read().decode('utf-8', errors='ignore')
                reader = csv.DictReader(io.StringIO(content))
                for row in reader:
                    scrip_cd = row.get("FinInstrmId", "").strip() or row.get("SC_CODE", "").strip()
                    scrip_name = row.get("TckrSymb", "").strip() or row.get("SC_NAME", "").strip()
                    full_name = row.get("FinInstrmNm", "").strip() or scrip_name
                    sec_type = row.get("SctySrs", "").strip() or row.get("SC_TYPE", "").strip()
                    if scrip_cd and sec_type in ("A", "B", "T", "X", "XT", "Z"):
                        stocks.append({
                            "yahooSymbol": f"{scrip_cd}.BO",
                            "tradingSymbol": scrip_name,
                            "companyName": full_name,
                            "exchange": "BSE",
                            "sector": None,
                            "isCustom": 0
                        })
                found = True
                break
        except Exception:
            continue
    print(f"Fetched {len(stocks)} BSE stocks.")
    return stocks

def build_database():
    nse_stocks = fetch_nse_stocks()
    nse_etfs = fetch_nse_etfs()
    bse_stocks = fetch_bse_stocks()
    
    all_stocks = {}
    for s in nse_stocks:
        all_stocks[s["yahooSymbol"]] = s
    for s in nse_etfs:
        all_stocks[s["yahooSymbol"]] = s
    for s in bse_stocks:
        all_stocks[s["yahooSymbol"]] = s
        
    print(f"Total unique stocks across NSE and BSE: {len(all_stocks)}")
    
    os.makedirs(os.path.dirname(DB_OUTPUT_PATH), exist_ok=True)
    if os.path.exists(DB_OUTPUT_PATH):
        os.remove(DB_OUTPUT_PATH)
        
    conn = sqlite3.connect(DB_OUTPUT_PATH)
    cursor = conn.cursor()
    
    # 1. stock_master table
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS stock_master (
        yahooSymbol TEXT PRIMARY KEY NOT NULL,
        tradingSymbol TEXT NOT NULL,
        companyName TEXT NOT NULL,
        exchange TEXT NOT NULL,
        sector TEXT,
        isCustom INTEGER NOT NULL
    );
    """)
    
    # Matching Room indices exactly
    cursor.execute("CREATE INDEX IF NOT EXISTS index_stock_master_tradingSymbol ON stock_master(tradingSymbol);")
    cursor.execute("CREATE INDEX IF NOT EXISTS index_stock_master_exchange ON stock_master(exchange);")
    cursor.execute("CREATE INDEX IF NOT EXISTS index_stock_master_companyName ON stock_master(companyName);")
    
    # 2. portfolio table
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS portfolio (
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        name TEXT NOT NULL,
        createdAt INTEGER NOT NULL,
        description TEXT NOT NULL,
        colorTag TEXT NOT NULL,
        initialBalance REAL NOT NULL,
        cashBalance REAL NOT NULL
    );
    """)
    
    # 3. trade table
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS trade (
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        portfolioId INTEGER NOT NULL,
        yahooSymbol TEXT NOT NULL,
        type TEXT NOT NULL,
        quantity INTEGER NOT NULL,
        price REAL NOT NULL,
        timestamp INTEGER NOT NULL,
        realizedPnl REAL,
        notes TEXT,
        FOREIGN KEY(portfolioId) REFERENCES portfolio(id) ON UPDATE NO ACTION ON DELETE CASCADE
    );
    """)
    cursor.execute("CREATE INDEX IF NOT EXISTS index_trade_portfolioId ON trade (portfolioId);")
    cursor.execute("CREATE INDEX IF NOT EXISTS index_trade_yahooSymbol ON trade (yahooSymbol);")
    cursor.execute("CREATE INDEX IF NOT EXISTS index_trade_timestamp ON trade (timestamp);")
    
    # 4. portfolio_snapshot table
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS portfolio_snapshot (
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        portfolioId INTEGER NOT NULL,
        date TEXT NOT NULL,
        totalValue REAL NOT NULL,
        investedAmount REAL NOT NULL,
        cashBalance REAL NOT NULL,
        timestamp INTEGER NOT NULL,
        FOREIGN KEY(portfolioId) REFERENCES portfolio(id) ON UPDATE NO ACTION ON DELETE CASCADE
    );
    """)
    cursor.execute("CREATE INDEX IF NOT EXISTS index_portfolio_snapshot_portfolioId ON portfolio_snapshot (portfolioId);")
    cursor.execute("CREATE INDEX IF NOT EXISTS index_portfolio_snapshot_date ON portfolio_snapshot (date);")
    cursor.execute("CREATE UNIQUE INDEX IF NOT EXISTS index_portfolio_snapshot_portfolioId_date ON portfolio_snapshot (portfolioId, date);")
    
    # 5. room_master_table
    cursor.execute("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT);")
    cursor.execute("INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, '7ad03fac311821d000e28a8342e3b062');")
    
    # 6. Full Text Search virtual table for lightning search
    cursor.execute("""
    CREATE VIRTUAL TABLE IF NOT EXISTS stock_master_fts USING fts4(
        yahooSymbol,
        tradingSymbol,
        companyName
    );
    """)
    
    rows = []
    fts_rows = []
    for s in all_stocks.values():
        rows.append((s["yahooSymbol"], s["tradingSymbol"], s["companyName"], s["exchange"], s["sector"], s["isCustom"]))
        fts_rows.append((s["yahooSymbol"], s["tradingSymbol"], s["companyName"]))
        
    cursor.executemany("""
    INSERT INTO stock_master (yahooSymbol, tradingSymbol, companyName, exchange, sector, isCustom)
    VALUES (?, ?, ?, ?, ?, ?)
    """, rows)
    
    cursor.executemany("""
    INSERT INTO stock_master_fts (yahooSymbol, tradingSymbol, companyName)
    VALUES (?, ?, ?)
    """, fts_rows)
    
    conn.commit()
    conn.close()
    
    print(f"Database successfully generated at: {DB_OUTPUT_PATH}")
    print(f"File size: {os.path.getsize(DB_OUTPUT_PATH) / 1024:.2f} KB")

if __name__ == "__main__":
    build_database()
