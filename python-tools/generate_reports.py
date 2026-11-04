#!/usr/bin/env python3
"""
generate_reports.py
--------------------
Reads the VSVPS Connect data files (CSV) and produces a set of plain-text
and CSV summary reports -- the kind of thing a school office regularly
needs (fee defaulters, class strength, attendance summary, library status)
without having to click through the web UI page by page.

Usage:
    python3 generate_reports.py
    python3 generate_reports.py --data-dir /path/to/vsvps-java/data --out-dir ./reports

Only the Python standard library is used (csv, collections, pathlib, datetime).
"""

import argparse
import csv
import sys
from collections import defaultdict
from datetime import date
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from vsvps_data import data_dir, read_table, require_data_dir


def money(n: float) -> str:
    return f"Rs. {n:,.0f}"


def write_csv_report(path: Path, header: list[str], rows: list[list]):
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(header)
        w.writerows(rows)


def fee_defaulters_report(ddir: Path, out_dir: Path):
    fees = read_table("fees", ddir)
    due = [f for f in fees if f.get("status") == "Due"]
    due.sort(key=lambda f: f.get("dueDate", ""))
    total = sum(float(f.get("amount") or 0) for f in due)

    print(f"\n== Fee Defaulters ==")
    print(f"{len(due)} due record(s), total outstanding: {money(total)}")
    for f in due[:15]:
        print(f"  - {f.get('studentName'):<22} Class {f.get('className'):<4} {f.get('head'):<16} {money(float(f.get('amount') or 0)):>12}  due {f.get('dueDate')}")
    if len(due) > 15:
        print(f"  ... and {len(due) - 15} more (see CSV)")

    write_csv_report(
        out_dir / "fee_defaulters.csv",
        ["studentName", "className", "head", "term", "amount", "dueDate"],
        [[f.get("studentName"), f.get("className"), f.get("head"), f.get("term"), f.get("amount"), f.get("dueDate")] for f in due],
    )


def class_strength_report(ddir: Path, out_dir: Path):
    students = read_table("students", ddir)
    counts = defaultdict(int)
    for s in students:
        key = f"{s.get('className')}-{s.get('section')}"
        counts[key] += 1

    print(f"\n== Class Strength ==")
    print(f"{len(students)} total student(s) across {len(counts)} section(s)")
    for cls in sorted(counts):
        print(f"  - Class {cls:<6} {counts[cls]} student(s)")

    write_csv_report(
        out_dir / "class_strength.csv",
        ["classSection", "studentCount"],
        [[cls, counts[cls]] for cls in sorted(counts)],
    )


def attendance_summary_report(ddir: Path, out_dir: Path):
    attendance = read_table("attendance", ddir)
    students = read_table("students", ddir)
    by_student = defaultdict(lambda: {"present": 0, "absent": 0})
    for a in attendance:
        key = a.get("studentId")
        if a.get("status") == "Present":
            by_student[key]["present"] += 1
        else:
            by_student[key]["absent"] += 1

    name_of = {s["id"]: s["name"] for s in students}
    rows = []
    print(f"\n== Attendance Summary (below 75%) ==")
    flagged = 0
    for sid, counts in by_student.items():
        total = counts["present"] + counts["absent"]
        pct = (counts["present"] / total * 100) if total else 0
        rows.append([name_of.get(sid, sid), counts["present"], counts["absent"], f"{pct:.1f}"])
        if total and pct < 75:
            flagged += 1
            print(f"  - {name_of.get(sid, sid):<22} {pct:.1f}% present ({counts['present']}/{total} days)")
    if flagged == 0:
        print("  No students below 75% attendance (or no attendance recorded yet).")

    write_csv_report(
        out_dir / "attendance_summary.csv",
        ["studentName", "daysPresent", "daysAbsent", "attendancePct"],
        rows,
    )


def library_status_report(ddir: Path, out_dir: Path):
    books = read_table("books", ddir)
    issues = read_table("library_issues", ddir)
    outstanding = [i for i in issues if i.get("status") == "Issued"]
    today = date.today().isoformat()
    overdue = [i for i in outstanding if i.get("dueDate", "9999-99-99") < today]

    print(f"\n== Library Status ==")
    print(f"{len(books)} title(s) in catalog, {len(outstanding)} currently issued, {len(overdue)} overdue")
    for i in overdue:
        print(f"  OVERDUE: {i.get('bookTitle')} -> {i.get('studentName')} (was due {i.get('dueDate')})")

    write_csv_report(
        out_dir / "library_overdue.csv",
        ["bookTitle", "studentName", "issueDate", "dueDate"],
        [[i.get("bookTitle"), i.get("studentName"), i.get("issueDate"), i.get("dueDate")] for i in overdue],
    )


def ledger_summary_report(ddir: Path, out_dir: Path):
    ledger = read_table("ledger", ddir)
    income = sum(float(l.get("amount") or 0) for l in ledger if l.get("type") == "Income")
    expense = sum(float(l.get("amount") or 0) for l in ledger if l.get("type") == "Expense")
    print(f"\n== Ledger Summary ==")
    print(f"  Income:  {money(income)}")
    print(f"  Expense: {money(expense)}")
    print(f"  Net:     {money(income - expense)}")


def main():
    parser = argparse.ArgumentParser(description="Generate school office reports from VSVPS Connect data")
    parser.add_argument("--data-dir", default=None, help="Path to the VSVPS Connect data/ directory")
    parser.add_argument("--out-dir", default="./reports", help="Directory to write CSV reports into (default: ./reports)")
    args = parser.parse_args()

    ddir = data_dir(args.data_dir)
    require_data_dir(ddir)
    out_dir = Path(args.out_dir)

    print(f"Reading data from: {ddir}")
    print(f"Writing CSV reports to: {out_dir.resolve()}")

    fee_defaulters_report(ddir, out_dir)
    class_strength_report(ddir, out_dir)
    attendance_summary_report(ddir, out_dir)
    library_status_report(ddir, out_dir)
    ledger_summary_report(ddir, out_dir)

    print(f"\nAll reports written to {out_dir.resolve()}")


if __name__ == "__main__":
    main()
