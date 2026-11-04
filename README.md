# VSVPS Connect — School ERP (Java + HTML + CSS + Python)

A School Management ERP for **Vidhya Sanskar Valley Public School**, Badora,
Betul, Madhya Pradesh, built using **only Java, HTML, CSS, and Python**:

- **Java** — a real backend web server (using the JDK's built-in
  `com.sun.net.httpserver.HttpServer`), with **zero external dependencies**.
  No Maven, no Gradle, no Spring, no application server — just a JVM.
- **HTML + CSS** — every page is rendered server-side by Java as plain HTML.
  **No JavaScript is used anywhere in this app** — all interactivity (forms,
  navigation, filters) is done with standard HTML forms and links, the way
  the web worked before client-side scripting.
- **Python** — companion command-line tools (`python-tools/`) for bulk
  student import, office reports, and backups, operating on the same CSV
  data files the Java app uses. Standard library only — no `pip install`
  needed.

---

## 1. How data is stored

There's no external database to install. Each "table" (students, fees,
attendance, etc.) is a plain CSV file under `data/`, read into memory on
startup and rewritten whenever something changes. This means:

- Zero setup — just run the app.
- The data files are plain text you can open in Excel/Sheets, back up, or
  process directly, including with the Python scripts.
- On first run, `data/` doesn't exist yet — the app creates it and fills it
  with realistic demo records automatically.

---

## 2. Run it in VS Code

**Requirements:** a JDK (Java 17 or newer). Nothing else.

**Option A — VS Code "Extension Pack for Java"**
1. Open this folder (`vsvps-java/`) in VS Code.
2. Install the **"Extension Pack for Java"** (by Microsoft) if you don't have it.
3. Open `src/main/java/vsvps/Main.java` and click **Run** above the
   `main` method — or press `F5` (a launch config is already set up in
   `.vscode/launch.json`).
4. Visit **http://localhost:8080** in your browser.

**Option B — plain terminal (Mac/Linux)**
```bash
cd vsvps-java
./run.sh 8080
```

**Option B — plain terminal (Windows)**
```bat
cd vsvps-java
run.bat 8080
```

**Option C — manual compile/run (any OS)**
```bash
cd vsvps-java
javac -d bin $(find src -name "*.java")     # Windows: see run.bat for the equivalent
java -cp bin vsvps.Main 8080
```

Then open **http://localhost:8080**.

---

## 3. Demo login credentials

Shown on the login screen itself, and listed here:

| Role       | Login ID     | Password   |
|------------|--------------|------------|
| Admin      | `admin1`     | `admin123` |
| Teacher    | `teacher1`   | `teach123` |
| Accountant | `accountant1`| `acc123`   |
| Parent     | `parent1`    | `parent123`|
| Student    | `student1`   | `stud123`  |

> **How to fill the form:** the box is labeled "Email address" but you type the **Login ID** into it (e.g. `admin1`, `teacher1`) — not an email like `admin@gmail.com`. Type the Login ID, type the matching Password, then click **Log In**.

The app is pre-seeded with a full demo dataset: 15 classes (Nursery through
Class 12, each with a real assigned class teacher), 150 students (10 per
class, every field filled in — parents, address, blood group, etc.), and 50
staff members (32 teaching + 18 non-teaching, covering roles like Vice
Principal, Accountant, Librarian, Lab Assistant, and more), plus sample fees,
notices, an exam with marks, library books, and events on first run.

---

## 4. Project structure

```
vsvps-java/
├── run.sh / run.bat         # one-command compile + run
├── .vscode/                 # VS Code launch config (F5 to run)
├── src/main/java/vsvps/
│   ├── Main.java             # HTTP server bootstrap + request router
│   ├── Db.java                 # table registry + demo seed data
│   ├── Table.java, Csv.java     # the CSV-backed "database engine"
│   ├── Session.java, SessionStore.java   # cookie-based login sessions
│   ├── Req.java, Res.java, Static.java   # request/response/static-file helpers
│   ├── Html.java                # page shell, nav, form/table builders
│   ├── Auth.java                # login / logout
│   ├── Core.java                # home dashboard, profile, manage logins
│   ├── Academics.java            # students, teachers, classes, subjects,
│   │                               faculty, admissions, attendance,
│   │                               timetable, homework, exams & marks
│   ├── Finance.java              # fees, ledger
│   └── Services.java             # leave, notices, certificates, library,
│                                    gallery, events, reminders, tickets,
│                                    feedback, hostel, ID cards
├── webroot/css/style.css     # the full design system (served statically)
├── data/                     # CSV "database" (created & seeded on first run)
└── python-tools/
    ├── vsvps_data.py          # shared CSV read/write helpers
    ├── bulk_import_students.py  # import students from any external CSV
    ├── generate_reports.py      # fee defaulters, class strength, attendance,
    │                              library overdue, and ledger reports
    └── backup_data.py           # zip up / restore the data/ directory
```

To rebrand for a different school, edit the constants at the top of
`Db.java` (`SCHOOL_NAME`, `SCHOOL_ADDRESS`, etc).

---

## 5. Using the Python tools

No `pip install` is required — everything uses the Python standard library.

```bash
cd python-tools

# Bulk-import students from any spreadsheet export
python3 bulk_import_students.py path/to/students.csv --data-dir ../data --dry-run
python3 bulk_import_students.py path/to/students.csv --data-dir ../data

# Generate office reports (fee defaulters, class strength, attendance,
# library overdue items, ledger summary) as CSV files + a console summary
python3 generate_reports.py --data-dir ../data --out-dir ./reports

# Back up (or restore) the data/ directory
python3 backup_data.py backup --data-dir ../data --out-dir ./backups
python3 backup_data.py restore ./backups/vsvps_backup_2026-08-04_120000.zip --data-dir ../data
```

**Tip:** stop the Java server before running a script that *writes* data
(`bulk_import_students.py`, `backup_data.py restore`), so a save from the
web UI doesn't race with the script. `generate_reports.py` only reads, so
it's safe to run anytime.

---

## 6. Deploying to any server

Since this is a single self-contained Java program, deployment is simple:

1. Compile once: `javac -d bin $(find src -name "*.java")`
2. Copy `bin/`, `webroot/`, and (optionally, to keep existing data) `data/`
   to the server.
3. Run: `java -cp bin vsvps.Main 8080` (or whatever port you want; put it
   behind Nginx/Apache as a reverse proxy for HTTPS in production, or run
   it directly if that's not needed).
4. Optionally package a single runnable jar:
   ```bash
   cd bin && jar cfe ../vsvps-connect.jar vsvps.Main . && cd ..
   java -jar vsvps-connect.jar 8080
   ```
   (Remember to also copy `webroot/` next to the jar — it's read from disk
   at startup, not bundled into the jar.)

Any host that can run a JVM works — a VPS, an on-prem server, a Docker
container (`FROM eclipse-temurin:21-jre`, copy `bin/` + `webroot/`, `CMD
["java","-cp","bin","vsvps.Main","8080"]`), etc.

---

## 7. Feature list

- **Auth & Roles:** Admin, Teacher, Accountant, Parent, Student — single
  login form, role-aware sidebar & dashboard, cookie-based sessions.
- **Students:** full CRUD, CSV export (download), CSV import (paste CSV
  text into a form field), bulk class promotion.
- **Teachers, Classes, Sections, Houses, Subjects:** full CRUD.
- **Admissions:** enquiry tracking with one-click "Convert to Student."
- **Attendance:** roster-based marking with upsert (re-marking the same
  date updates instead of duplicating), attendance % for students/parents.
- **Timetable:** admin-editable weekly grid per class, read-only for others.
- **Homework:** assign per class/subject; students track submission status.
- **Exams & Marks:** create exams, enter marks in a grid, auto-computed
  report cards with a CBSE-style letter grade and live class rank.
- **Fees:** bulk-assign a fee head to a whole class, defaulters/paid/day-book
  filters, demo "Pay Now" flow, printable receipts.
- **Ledger:** income/expense tracking with live totals.
- **Leave Applications:** apply, approve/reject.
- **Notices:** post & browse (server-rendered list; see note below on push
  notifications).
- **ID Cards:** printable card (see note below on the QR code limitation).
- **Certificates:** request → issue → printable certificate.
- **Library:** book catalog, issue/return with auto-tracked copy counts.
- **Gallery:** add photos by image URL (see note below on file upload).
- **Events:** browse & self-register.
- **Reminders:** private per-user notes.
- **Support Tickets & Feedback:** raise/track/resolve.
- **Hostel Info:** read-only info card (admin-editable).
- **Manage Logins:** admin creates/removes any login, links to student/
  teacher/ward records.
- **Profile:** view account info, log out.

### Honest limitations (given the "no JavaScript, no external libraries" constraint)

- **No scannable QR code on ID cards.** Generating a real QR/barcode image
  needs either JavaScript or an external image library; neither is used
  here, so the ID card shows the unique identifier as printed text instead.
- **Gallery uses image URLs, not file upload.** True binary file upload
  (multipart forms) was left out to keep the app 100%-dependency-free and
  robust; instead, admins/teachers paste a link to an already-hosted image.
- **No live/pop-up notifications.** Since there's no JavaScript, notices
  simply appear in the Notices list next time a page loads — there's no
  push/toast alert while someone has another page open.
- **Printing uses your browser's own Print command** (Ctrl+P / ⌘+P), since
  there's no `window.print()` without JavaScript. Every printable page
  (receipts, certificates, report cards, ID cards) has print-specific CSS
  so it comes out clean, and says so on-page.
- **A live payment gateway and SMS integration are out of scope** (as in
  the original spec) — Fees includes a clearly-labeled **demo** "Pay Now"
  flow instead of a real transaction.
- **Single-server data.** All data lives in CSV files on whatever machine
  runs the Java process — this isn't a distributed/clustered setup. For a
  single school running one instance, that's normal and fine; back it up
  with `python-tools/backup_data.py`.

---

## 8. Reliability notes

- Every page render is wrapped in a try/catch at the router level — a
  broken record or missing reference shows a friendly "Something went
  wrong" page with technical detail and a link back to Home, never a blank
  screen.
- Role checks (`Util.requireAdmin`, `requireStaff`, `requireFinance`) guard
  every write-capable route; violating them shows a clear 403 "Access
  Restricted" page rather than crashing or silently allowing it.
- Attendance and exam marks are upserts (matched on student+date or
  student+exam), so resubmitting never creates duplicate rows.
- Library issue/return keeps `copiesAvailable` in sync automatically and
  never lets it go negative or above `copiesTotal`.
- To reset the app back to fresh seed data at any time, stop the server and
  delete the `data/` folder — it will be recreated and reseeded on next
  startup.
