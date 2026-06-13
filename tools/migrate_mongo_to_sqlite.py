"""Einmalige Migration der lokalen woolbattle-Mongo nach SQLite (woolbattle.db).

Serialisiert die Dokumente als Canonical Extended JSON (json_util), damit die
BSON-Zahlentypen erhalten bleiben ($numberLong/$numberInt/$numberDouble). Das Plugin
liest mit JsonMode.EXTENDED zurueck; nur so bleibt z. B. Long (Map-Chunk-Koordinaten)
ein Long statt zu Integer zu werden.
"""
import sqlite3
import sys
from bson.json_util import CANONICAL_JSON_OPTIONS
from bson.json_util import dumps as bson_dumps
from pymongo import MongoClient

COLLECTIONS = ["playerPerks", "playerInventories", "playerAchievements",
               "playerStats", "map", "blockBreaking"]

src = MongoClient("mongodb://localhost:27017", serverSelectionTimeoutMS=3000)["woolbattle"]
con = sqlite3.connect("woolbattle.db")
cur = con.cursor()
cur.execute("PRAGMA journal_mode=WAL")
for c in COLLECTIONS:
    cur.execute(f'CREATE TABLE IF NOT EXISTS "{c}" (_id TEXT PRIMARY KEY, data TEXT NOT NULL)')

existing = set(src.list_collection_names())
for c in COLLECTIONS:
    if c not in existing:
        continue
    for doc in src[c].find():
        _id = str(doc["_id"])
        doc["_id"] = _id
        cur.execute(f'INSERT OR REPLACE INTO "{c}" (_id, data) VALUES (?, ?)',
                    (_id, bson_dumps(doc, json_options=CANONICAL_JSON_OPTIONS)))
con.commit()

print("=== migration counts (mongo -> sqlite) ===")
ok = True
for c in COLLECTIONS:
    mongo_n = src[c].count_documents({}) if c in existing else 0
    sqlite_n = cur.execute(f'SELECT COUNT(*) FROM "{c}"').fetchone()[0]
    flag = "OK" if mongo_n == sqlite_n else "MISMATCH"
    if flag != "OK":
        ok = False
    print(f"{c}: mongo={mongo_n} sqlite={sqlite_n} [{flag}]")
con.close()
sys.exit(0 if ok else 1)
