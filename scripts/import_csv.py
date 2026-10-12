#!/usr/bin/env python3
"""Carga manual de catálogo desde CSV (separador ';', UTF-8).
Columnas: name;brand;unit;price;category;ean;image_url   (category = nombre exacto de la categoría)
Uso: SUPABASE_URL=... SUPABASE_SERVICE_ROLE_KEY=... python import_csv.py catalogo.csv 1
"""
import csv, json, os, sys, requests

url, key = os.environ["SUPABASE_URL"], os.environ["SUPABASE_SERVICE_ROLE_KEY"]
H = {"apikey": key, "Authorization": f"Bearer {key}", "Content-Type": "application/json"}
path, market = sys.argv[1], int(sys.argv[2])
cats = {c["name"]: c["id"] for c in requests.get(f"{url}/rest/v1/categories?select=id,name", headers=H, timeout=20).json()}
rows = []
with open(path, encoding="utf-8") as f:
    for r in csv.DictReader(f, delimiter=";"):
        rows.append({"supermarket_id": market, "category_id": cats.get(r["category"].strip()), "name": r["name"].strip(),
                     "brand": r.get("brand") or None, "unit": r.get("unit") or "ud",
                     "price": round(float(r["price"].replace(",", ".")), 2), "ean": r["ean"].strip(),
                     "image_url": r.get("image_url") or None, "source": "csv", "active": True})
for i in range(0, len(rows), 200):
    requests.post(f"{url}/rest/v1/products?on_conflict=supermarket_id,ean", data=json.dumps(rows[i:i + 200]), timeout=30,
                  headers={**H, "Prefer": "resolution=merge-duplicates,return=minimal"}).raise_for_status()
print(f"{len(rows)} productos importados")
