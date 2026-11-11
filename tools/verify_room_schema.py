import sqlite3

db_path = r'I:\projects\paper_trade\app\src\main\assets\stock_master.db'
conn = sqlite3.connect(db_path)
cur = conn.cursor()

def get_table_info(table_name):
    cur.execute(f"PRAGMA table_info('{table_name}')")
    cols = cur.fetchall()
    # (cid, name, type, notnull, dflt_value, pk)
    return {c[1]: {'type': c[2], 'notnull': bool(c[3]), 'pk': c[5], 'dflt_value': c[4]} for c in cols}

def get_foreign_keys(table_name):
    cur.execute(f"PRAGMA foreign_key_list('{table_name}')")
    return cur.fetchall()

def get_indices(table_name):
    cur.execute(f"PRAGMA index_list('{table_name}')")
    indices = cur.fetchall()
    result = {}
    for idx in indices:
        name = idx[1]
        unique = bool(idx[2])
        cur.execute(f"PRAGMA index_info('{name}')")
        cols = [c[2] for c in cur.fetchall()]
        result[name] = {'unique': unique, 'columns': cols}
    return result

for table in ['portfolio', 'stock_master', 'trade', 'portfolio_snapshot']:
    print(f"=== Table: {table} ===")
    cols = get_table_info(table)
    print("Columns:", cols)
    fks = get_foreign_keys(table)
    print("Foreign keys:", fks)
    indices = get_indices(table)
    print("Indices:", indices)
    print()

conn.close()
