#!/usr/bin/env python3
"""
backup_data.py
---------------
Zips up the VSVPS Connect data/ directory (all CSV tables) into a timestamped
backup archive, or restores one. Handy to run as a nightly cron job / Task
Scheduler entry so the school always has a recent snapshot of student,
fee, and academic records outside of the live app.

Usage:
    python3 backup_data.py backup
    python3 backup_data.py backup --data-dir /path/to/vsvps-java/data --out-dir ./backups
    python3 backup_data.py restore ./backups/vsvps_backup_2026-08-04_120000.zip
"""

import argparse
import shutil
import sys
import zipfile
from datetime import datetime
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from vsvps_data import data_dir, require_data_dir


def backup(ddir: Path, out_dir: Path):
    require_data_dir(ddir)
    out_dir.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now().strftime("%Y-%m-%d_%H%M%S")
    archive_path = out_dir / f"vsvps_backup_{stamp}.zip"
    with zipfile.ZipFile(archive_path, "w", zipfile.ZIP_DEFLATED) as zf:
        for csv_file in sorted(ddir.glob("*.csv")):
            zf.write(csv_file, arcname=csv_file.name)
    print(f"Backed up {len(list(ddir.glob('*.csv')))} table(s) to {archive_path}")


def restore(archive: Path, ddir: Path):
    if not archive.exists():
        print(f"Archive not found: {archive}")
        sys.exit(1)
    ddir.mkdir(parents=True, exist_ok=True)
    confirm = input(f"This will overwrite CSV files in {ddir} with the contents of {archive.name}. Continue? [y/N] ")
    if confirm.strip().lower() != "y":
        print("Cancelled.")
        return
    with zipfile.ZipFile(archive, "r") as zf:
        zf.extractall(ddir)
    print(f"Restored data into {ddir}")
    print("Restart the Java app to pick up the restored data.")


def main():
    parser = argparse.ArgumentParser(description="Backup or restore VSVPS Connect data")
    parser.add_argument("action", choices=["backup", "restore"])
    parser.add_argument("archive", nargs="?", help="Path to a backup .zip (required for restore)")
    parser.add_argument("--data-dir", default=None, help="Path to the VSVPS Connect data/ directory")
    parser.add_argument("--out-dir", default="./backups", help="Directory to write backups into (default: ./backups)")
    args = parser.parse_args()

    ddir = data_dir(args.data_dir)

    if args.action == "backup":
        backup(ddir, Path(args.out_dir))
    else:
        if not args.archive:
            print("Please provide the path to a backup .zip to restore, e.g.:")
            print("  python3 backup_data.py restore ./backups/vsvps_backup_2026-08-04_120000.zip")
            sys.exit(1)
        restore(Path(args.archive), ddir)


if __name__ == "__main__":
    main()
