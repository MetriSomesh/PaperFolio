import sqlite3

db_path = r'I:\projects\paper_trade\app\src\main\assets\stock_master.db'
conn = sqlite3.connect(db_path)
cur = conn.cursor()

# 1. Drop old non-matching indices
cur.execute('DROP INDEX IF EXISTS idx_stock_trading_sym;')
cur.execute('DROP INDEX IF EXISTS idx_stock_exchange;')
cur.execute('DROP INDEX IF EXISTS idx_stock_company;')

# 2. Create indices for stock_master exactly as Room expects
cur.execute('CREATE INDEX IF NOT EXISTS index_stock_master_tradingSymbol ON stock_master (tradingSymbol);')
cur.execute('CREATE INDEX IF NOT EXISTS index_stock_master_exchange ON stock_master (exchange);')
cur.execute('CREATE INDEX IF NOT EXISTS index_stock_master_companyName ON stock_master (companyName);')

# 3. Create portfolio table
cur.execute('''
CREATE TABLE IF NOT EXISTS portfolio (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name TEXT NOT NULL,
    createdAt INTEGER NOT NULL,
    description TEXT NOT NULL,
    colorTag TEXT NOT NULL,
    initialBalance REAL NOT NULL,
    cashBalance REAL NOT NULL
);
''')

# 4. Create trade table and indices
cur.execute('''
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
''')
cur.execute('CREATE INDEX IF NOT EXISTS index_trade_portfolioId ON trade (portfolioId);')
cur.execute('CREATE INDEX IF NOT EXISTS index_trade_yahooSymbol ON trade (yahooSymbol);')
cur.execute('CREATE INDEX IF NOT EXISTS index_trade_timestamp ON trade (timestamp);')

# 5. Create portfolio_snapshot table and indices
cur.execute('''
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
''')
cur.execute('CREATE INDEX IF NOT EXISTS index_portfolio_snapshot_portfolioId ON portfolio_snapshot (portfolioId);')
cur.execute('CREATE INDEX IF NOT EXISTS index_portfolio_snapshot_date ON portfolio_snapshot (date);')
cur.execute('CREATE UNIQUE INDEX IF NOT EXISTS index_portfolio_snapshot_portfolioId_date ON portfolio_snapshot (portfolioId, date);')

# 6. The asset is now only a SEED source (see AppDatabase.kt) — it must
# NOT carry a room_master_table with a hand-copied identity hash. Room
# manages that table in the live database; a stale hash could make Room
# reject the database. Drop any leftover row and stamp the version.
cur.execute('DROP TABLE IF EXISTS room_master_table;')
cur.execute('PRAGMA user_version = 1;')

conn.commit()

# Verify all tables
cur.execute("SELECT name FROM sqlite_master WHERE type='table';")
print('Updated tables:', cur.fetchall())
cur.execute("SELECT name FROM sqlite_master WHERE type='index';")
print('Updated indices:', cur.fetchall())
cur.execute("SELECT * FROM room_master_table;")
print('Room master table:', cur.fetchall())
cur.execute("SELECT COUNT(*) FROM stock_master;")
print('Stock master count:', cur.fetchone()[0])
conn.close()
