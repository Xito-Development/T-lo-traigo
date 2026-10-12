#!/usr/bin/env python3
"""Scraper ético de precios PÚBLICOS -> Supabase (tabla products).

Salvaguardas legales/técnicas (no constituyen asesoramiento jurídico):
  * Respeta robots.txt y un retardo mínimo entre peticiones (por defecto 6 s).
  * Se identifica con un User-Agent con contacto.
  * Solo extrae datos fácticos (nombre, marca, precio, EAN) del marcado schema.org/Product (JSON-LD).
  * No descarga ni rehospeda imágenes salvo allow_images=true (derechos de autor).
  * Exige terms_reviewed=true por fuente: revisar antes las condiciones de uso del sitio.
  * Volumen limitado (MAX_URLS por fuente) para no realizar extracción sustancial de la base de datos
    (derecho sui generis, art. 133 TRLPI / Directiva 96/9/CE).
Uso: python scrape.py sources.json [--dry-run]
"""
import hashlib, json, os, sys, time, urllib.parse, urllib.robotparser
import requests
from bs4 import BeautifulSoup

UA = os.getenv("SCRAPER_UA", "TLoTraigoBot/0.1 (+mailto:contacto@unocerobits.es)")
MAX_URLS = int(os.getenv("SCRAPER_MAX_URLS", "300"))
_robots: dict[str, urllib.robotparser.RobotFileParser] = {}


def allowed(url: str) -> bool:
    base = "{0.scheme}://{0.netloc}".format(urllib.parse.urlparse(url))
    rp = _robots.get(base)
    if rp is None:
        rp = urllib.robotparser.RobotFileParser(base + "/robots.txt")
        try:
            rp.read()
        except Exception:
            return False  # sin robots legible -> no rastrear
        _robots[base] = rp
    return rp.can_fetch(UA, url)


def _walk(node):
    if isinstance(node, list):
        for n in node:
            yield from _walk(n)
    elif isinstance(node, dict):
        yield node
        for k in ("@graph", "itemListElement", "item"):
            if k in node:
                yield from _walk(node[k])


def extract(html: str):
    soup = BeautifulSoup(html, "html.parser")
    for tag in soup.find_all("script", type="application/ld+json"):
        try:
            data = json.loads(tag.string or "")
        except json.JSONDecodeError:
            continue
        for n in _walk(data):
            t = n.get("@type")
            if t == "Product" or (isinstance(t, list) and "Product" in t):
                offers = n.get("offers") or {}
                if isinstance(offers, list):
                    offers = offers[0] if offers else {}
                price = offers.get("price") or offers.get("lowPrice")
                if price is None or offers.get("priceCurrency", "EUR") != "EUR":
                    continue
                brand = n.get("brand")
                img = n.get("image")
                return {
                    "name": str(n.get("name", "")).strip()[:200],
                    "brand": (brand.get("name") if isinstance(brand, dict) else brand) or None,
                    "price": round(float(str(price).replace(",", ".")), 2),
                    "ean": n.get("gtin13") or n.get("gtin") or n.get("gtin8") or n.get("sku"),
                    "image": img[0] if isinstance(img, list) and img else img,
                }
    return None


def upsert(rows, url, key):
    r = requests.post(
        f"{url}/rest/v1/products?on_conflict=supermarket_id,ean",
        headers={"apikey": key, "Authorization": f"Bearer {key}", "Content-Type": "application/json",
                 "Prefer": "resolution=merge-duplicates,return=minimal"},
        data=json.dumps(rows), timeout=30)
    r.raise_for_status()


def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    dry = "--dry-run" in sys.argv
    sources = json.load(open(sys.argv[1], encoding="utf-8"))
    sb_url, sb_key = os.getenv("SUPABASE_URL"), os.getenv("SUPABASE_SERVICE_ROLE_KEY")
    if not dry and not (sb_url and sb_key):
        sys.exit("Faltan SUPABASE_URL / SUPABASE_SERVICE_ROLE_KEY")
    s = requests.Session(); s.headers["User-Agent"] = UA
    for src in sources:
        if not src.get("terms_reviewed"):
            print(f"[omitida] {src.get('name')}: marca terms_reviewed=true tras revisar sus condiciones de uso")
            continue
        delay = max(float(src.get("delay_s", 6)), 3.0)
        rows = []
        for u in src["urls"][:MAX_URLS]:
            if not allowed(u):
                print(f"[robots] no permitido: {u}"); continue
            try:
                resp = s.get(u, timeout=20)
                resp.raise_for_status()
                p = extract(resp.text)
            except Exception as e:
                print(f"[error] {u}: {e}"); p = None
            if p and p["name"]:
                rows.append({
                    "supermarket_id": src["supermarket_id"], "category_id": src.get("default_category_id"),
                    "name": p["name"], "brand": p["brand"], "price": p["price"], "unit": "ud",
                    "ean": str(p["ean"]) if p["ean"] else "u:" + hashlib.sha1(u.encode()).hexdigest()[:16],
                    "image_url": p["image"] if src.get("allow_images") else None,
                    "source": "scraping", "source_url": u, "active": True,
                })
                print(f"[ok] {p['name']} {p['price']} €")
            time.sleep(delay)
        if rows and not dry:
            for i in range(0, len(rows), 100):
                upsert(rows[i:i + 100], sb_url, sb_key)
        print(f"{src.get('name')}: {len(rows)} productos {'(dry-run)' if dry else 'guardados'}")


if __name__ == "__main__":
    main()
