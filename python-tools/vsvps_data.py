"""
vsvps_data.py
-------------
Small shared helper for reading/writing the same CSV data files that the
Java VSVPS Connect server uses (in the `data/` directory). Uses only the
Python standard library (csv, pathlib) -- no pip install required.

Because the Java app and these scripts agree on the same CSV format
(RFC4180-style quoting), either side can read and write the data safely.
Run these tools while the Java server is stopped to avoid a write race,
or treat them as read-mostly reporting tools while it's running.
"""

import csv
import sys
from pathlib import Path


def data_dir(explicit: str | None = None) -> Path:
    """Resolve the data/ directory. Defaults to ../data relative to this script,
    which matches the layout of the shipped project (python-tools/ and data/
    are siblings under the project root)."""
    if explicit:
        return Path(explicit)
    here = Path(__file__).resolve().parent
    candidate = here.parent / "data"
    return candidate


def read_table(name: str, ddir: Path) -> list[dict]:
    path = ddir / f"{name}.csv"
    if not path.exists():
        return []
    with open(path, newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def write_table(name: str, rows: list[dict], columns: list[str], ddir: Path) -> None:
    path = ddir / f"{name}.csv"
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=columns)
        writer.writeheader()
        for row in rows:
            writer.writerow({c: row.get(c, "") for c in columns})


def require_data_dir(ddir: Path) -> None:
    if not ddir.exists():
        print(f"Data directory not found: {ddir}")
        print("Start the Java server at least once (it seeds demo data on first run),")
        print("or pass --data-dir pointing at your VSVPS Connect data/ folder.")
        sys.exit(1)
