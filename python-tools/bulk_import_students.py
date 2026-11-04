#!/usr/bin/env python3
"""
bulk_import_students.py
------------------------
Bulk-imports students from any external CSV file directly into the VSVPS
Connect data store (data/students.csv), without needing to paste CSV text
into the web UI. Useful for onboarding a whole school's student list at once
from an existing spreadsheet export.

Usage:
    python3 bulk_import_students.py path/to/incoming_students.csv
    python3 bulk_import_students.py path/to/incoming_students.csv --data-dir /path/to/vsvps-java/data

The incoming CSV should have a header row with any of these columns
(missing columns are left blank): name, admissionNo, className, section,
dob, gender, parentPhone, house, stream

IMPORTANT: stop the Java server before running this script, so your import
doesn't race with a save happening from the web UI at the same instant.
"""

import argparse
import csv
import sys
import time
import uuid
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from vsvps_data import data_dir, read_table, write_table, require_data_dir

STUDENT_COLUMNS = ["id", "name", "admissionNo", "className", "section", "dob", "gender", "parentPhone", "house", "stream"]


def new_id() -> str:
    return "s_" + uuid.uuid4().hex[:10]


def main():
    parser = argparse.ArgumentParser(description="Bulk-import students into VSVPS Connect")
    parser.add_argument("csv_file", help="Path to the incoming CSV file")
    parser.add_argument("--data-dir", default=None, help="Path to the VSVPS Connect data/ directory")
    parser.add_argument("--dry-run", action="store_true", help="Show what would be imported without writing")
    args = parser.parse_args()

    ddir = data_dir(args.data_dir)
    require_data_dir(ddir)

    incoming_path = Path(args.csv_file)
    if not incoming_path.exists():
        print(f"File not found: {incoming_path}")
        sys.exit(1)

    with open(incoming_path, newline="", encoding="utf-8") as f:
        incoming_rows = list(csv.DictReader(f))

    if not incoming_rows:
        print("No rows found in the input CSV (or it's missing a header row).")
        sys.exit(1)

    existing = read_table("students", ddir)
    existing_admission_nos = {r.get("admissionNo", "").strip() for r in existing if r.get("admissionNo")}

    added = []
    skipped = []
    for row in incoming_rows:
        name = (row.get("name") or "").strip()
        if not name:
            skipped.append(row)
            continue
        admission_no = (row.get("admissionNo") or "").strip()
        if admission_no and admission_no in existing_admission_nos:
            skipped.append(row)
            continue
        new_row = {
            "id": new_id(),
            "name": name,
            "admissionNo": admission_no,
            "className": (row.get("className") or "").strip(),
            "section": (row.get("section") or "").strip(),
            "dob": (row.get("dob") or "").strip(),
            "gender": (row.get("gender") or "").strip(),
            "parentPhone": (row.get("parentPhone") or "").strip(),
            "house": (row.get("house") or "").strip(),
            "stream": (row.get("stream") or "").strip(),
        }
        added.append(new_row)
        if admission_no:
            existing_admission_nos.add(admission_no)

    print(f"Found {len(incoming_rows)} row(s) in {incoming_path.name}")
    print(f"  -> {len(added)} new student(s) to add")
    print(f"  -> {len(skipped)} row(s) skipped (blank name or duplicate admission number)")

    if args.dry_run:
        print("\n--dry-run set: no changes written.")
        for r in added[:10]:
            print(f"  would add: {r['name']} ({r['admissionNo'] or 'no admission no.'}) - Class {r['className']}-{r['section']}")
        if len(added) > 10:
            print(f"  ... and {len(added) - 10} more")
        return

    if not added:
        print("Nothing to import.")
        return

    all_rows = existing + added
    write_table("students", all_rows, STUDENT_COLUMNS, ddir)
    print(f"\nDone. {len(added)} student(s) added to {ddir / 'students.csv'}")
    print("Restart (or refresh) the Java app to see the new students.")


if __name__ == "__main__":
    main()
