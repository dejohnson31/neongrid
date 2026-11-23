#!/usr/bin/env python3
"""Check app/src/main/assets/black_cinema.json against TMDB (title + release year).

Usage:  TMDB_KEY=<your v3 key> python3 tools/verify_catalog.py
The key is read from the environment only; never commit it.
Prints every entry where TMDB has no movie with the same title within +-1 year, with the closest hits.
"""
import json, os, sys, time, urllib.parse, urllib.request

key = os.environ.get("TMDB_KEY")
if not key:
    sys.exit("Set TMDB_KEY in the environment.")

path = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "black_cinema.json")
doc = json.load(open(path, encoding="utf-8"))
bad = 0
for m in doc["movies"]:
    q = urllib.parse.urlencode({"api_key": key, "query": m["t"], "year": m["y"]})
    with urllib.request.urlopen("https://api.themoviedb.org/3/search/movie?" + q) as r:
        res = json.load(r)["results"]
    ok = any((x.get("release_date") or "0000")[:4].isdigit()
             and abs(int(x["release_date"][:4]) - m["y"]) <= 1
             and x["title"].lower() == m["t"].lower() for x in res)
    if not ok:
        bad += 1
        print(f'CHECK: {m["t"]} ({m["y"]}) ->', [(x["title"], (x.get("release_date") or "")[:4]) for x in res[:3]])
    time.sleep(0.05)
print(f"{len(doc['movies'])} entries, {bad} to check")
