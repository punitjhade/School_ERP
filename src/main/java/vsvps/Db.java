package vsvps;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Central registry of all tables ("the database"), plus demo seed data. */
public class Db {

    public static final String SCHOOL_NAME = "Vidhya Sanskar Valley Public School";
    public static final String SCHOOL_ADDRESS = "Badora, Betul, Madhya Pradesh";
    public static final String SCHOOL_AFFIL = "CBSE Affiliation No. 1031102";
    public static final String SCHOOL_MANAGED_BY = "Vidhya Abhinav Shiksha Samiti";

    /** Demonstration hashing — non-cryptographic by design; a production build
     *  should use a real KDF (BCrypt / PBKDF2) with a salt + many rounds. */
    static String hashForDemo(String p) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < p.length(); i++) b.append((char) (p.charAt(i) % 94 + 33));
        return b.toString();
    }

    public final Map<String, Table> tables = new LinkedHashMap<>();
    private final AtomicInteger counter = new AtomicInteger(1000);

    // ---- Reference data pools used to generate realistic bulk demo data ----
    private static final String[] MALE_FIRST = {"Aarav","Vihaan","Aditya","Vivaan","Arjun","Reyansh","Kabir","Ayaan","Krishna","Ishaan","Rohan","Karan","Dev","Yash","Aryan","Rudra","Shaurya","Aarush","Dhruv","Om","Advait","Arnav","Veer","Sai","Kartik","Nikhil","Raghav","Siddharth","Tanish","Vedant"};
    private static final String[] FEMALE_FIRST = {"Diya","Ananya","Ishita","Anaya","Myra","Aadhya","Kiara","Saanvi","Riya","Navya","Pari","Meera","Tara","Aarohi","Ira","Anvi","Prisha","Sara","Kavya","Avni","Nisha","Priya","Divya","Sneha","Pooja","Neha","Simran","Jiya","Khushi","Anika"};
    private static final String[] LAST_NAMES = {"Kumar","Sharma","Patel","Singh","Verma","Rao","Gupta","Reddy","Mehta","Nair","Iyer","Joshi","Chauhan","Malhotra","Kapoor","Bansal","Agarwal","Yadav","Chowdhury","Pillai","Desai","Shah","Trivedi","Kulkarni","Bhatt","Menon","Saxena","Tiwari","Mishra","Pandey"};
    private static final String[] STREETS = {"MG Road","Station Road","Civil Lines","Nehru Nagar","Gandhi Chowk","College Road","Shastri Nagar","Model Town","Ashok Vihar","Ram Nagar","Subhash Marg","Indira Colony"};
    private static final String[] BLOOD_GROUPS = {"A+","A-","B+","B-","AB+","AB-","O+","O-"};
    private static final String[] RELIGIONS = {"Hindu","Hindu","Hindu","Muslim","Christian","Sikh"};
    private static final String[] CATEGORIES = {"General","OBC","General","SC","OBC","ST"};
    private static final String[] MOTHER_TONGUES = {"Hindi","Hindi","English","Marathi","Gujarati"};
    private static final String[] FATHER_OCCUPATIONS = {"Business","Engineer","Doctor","Farmer","Govt. Employee","Shop Owner","Bank Employee","Accountant"};
    private static final String[] MOTHER_OCCUPATIONS = {"Homemaker","Teacher","Nurse","Business","Govt. Employee","Homemaker","Homemaker"};
    private static final String[] PREVIOUS_SCHOOLS = {"Sunrise Public School","Green Valley School","St. Xavier's School","City Montessori","Little Angels School","Bright Future Academy"};

    private static final String[] CLASS_NAMES_ALL = {"Nursery","KG1","KG2","1","2","3","4","5","6","7","8","9","10","11","12"};
    private static final int[] CLASS_AGES =           {3,       4,    5,    6,  7,  8,  9,  10, 11, 12, 13, 14, 15, 16, 17};

    private static final String[] TEACH_SUBJECTS = {"English","Hindi","Mathematics","Science","Social Studies","Sanskrit","Computer Science","Physical Education","Art & Craft","Music","Physics","Chemistry","Biology","Commerce","Economics","Political Science","Environmental Science"};
    private static final String[] NONTEACH_DESIGNATIONS = {"Vice Principal","Office Superintendent","Accountant","Librarian","Lab Assistant","Receptionist","Peon","Security Guard","Bus Driver","Sports Coach","School Counselor","School Nurse","IT Support Staff","Gardener","Housekeeping Staff","Store Keeper","Admissions Coordinator"};

    public Db(Path dataDir) {
        reg("users", "id,loginId,passwordHash,role,name,email,studentId,teacherId,wardIds", dataDir);
        reg("students", "id,name,admissionNo,className,section,academicYear,dob,gender,bloodGroup,nationality,religion,category,motherTongue,aadharNo,identificationMarks,fatherName,fatherOccupation,fatherPhone,motherName,motherOccupation,motherPhone,address,city,state,pincode,studentEmail,previousSchool,parentPhone,house,stream", dataDir);
        reg("teachers", "id,name,empCode,designation,subject,phone,email,qualification,joiningDate,staffType,canMarkAttendance,canEnterMarks,canManageFees", dataDir);
        reg("classes", "id,className,sections,classTeacherId", dataDir);
        reg("subjects", "id,name,code,className,teacher", dataDir);
        reg("houses", "id,name,captain,points", dataDir);
        reg("admissions", "id,name,dob,className,parentPhone,address,status,notes", dataDir);
        reg("attendance", "id,studentId,studentName,className,section,date,status", dataDir);
        reg("timetables", "id,className,day,periods", dataDir);
        reg("homework", "id,className,subject,title,description,dueDate", dataDir);
        reg("homework_submissions", "id,homeworkId,studentId,submittedAt", dataDir);
        reg("exams", "id,title,className,subjects", dataDir);
        reg("exam_marks", "id,examId,studentId,marks", dataDir);
        reg("grade_history", "id,studentId,term,percentage,grade,result", dataDir);
        reg("fees", "id,studentId,studentName,className,head,term,amount,dueDate,status,paidDate", dataDir);
        reg("ledger", "id,type,category,amount,date,note", dataDir);
        reg("leave_apps", "id,appliedBy,appliedByName,role,fromDate,toDate,reason,status", dataDir);
        reg("notices", "id,title,category,date,body", dataDir);
        reg("certificates", "id,type,reason,requestedBy,requestedByName,status,issueDate", dataDir);
        reg("books", "id,title,author,isbn,copiesTotal,copiesAvailable", dataDir);
        reg("library_issues", "id,bookId,bookTitle,studentId,studentName,issueDate,dueDate,status,returnDate", dataDir);
        reg("gallery", "id,caption,album,imageUrl", dataDir);
        reg("events", "id,title,club,date,venue", dataDir);
        reg("event_regs", "id,eventId,userId", dataDir);
        reg("reminders", "id,ownerId,title,date,note", dataDir);
        reg("tickets", "id,subject,category,description,raisedBy,raisedByName,status", dataDir);
        reg("feedback", "id,category,text,submittedBy,submittedByName,date,status", dataDir);
        reg("hostel", "id,block,room,warden,bedType", dataDir);

        for (Table t : tables.values()) t.load();
        if (t("users").all().isEmpty()) seed();
    }

    private void reg(String name, String cols, Path dataDir) {
        tables.put(name, new Table(name, Arrays.asList(cols.split(",")), dataDir));
    }

    public Table t(String name) {
        Table tbl = tables.get(name);
        if (tbl == null) throw new IllegalArgumentException("No such table: " + name);
        return tbl;
    }

    public String newId(String prefix) {
        return prefix + "_" + counter.incrementAndGet() + "_" + Long.toString(System.nanoTime() % 100000, 36);
    }

    public static String today() {
        return java.time.LocalDate.now().toString();
    }

    public static String todayPlus(int days) {
        return java.time.LocalDate.now().plusDays(days).toString();
    }

    private LinkedHashMap<String, String> row(Table tbl, String... kv) {
        LinkedHashMap<String, String> r = tbl.newRow();
        for (int i = 0; i < kv.length; i += 2) r.put(kv[i], kv[i + 1]);
        return r;
    }

    /** Populate demo data on very first run. */
    private void seed() {
        // ---- Students: 15 classes (Nursery through 12), 10 students each = 150 students ----
        Table students = t("students");
        int admissionCounter = 2301;
        int phoneCounter = 500001;
        int studentIdCounter = 2; // "s1" is reserved for the demo Aarav Kumar record (Class 6), never auto-assigned
        String secondClass6StudentId = null; // captured for cross-referencing in fees/exam seed data below
        String secondClass6StudentName = null;
        for (int ci = 0; ci < CLASS_NAMES_ALL.length; ci++) {
            String className = CLASS_NAMES_ALL[ci];
            int age = CLASS_AGES[ci];
            int dobYear = 2026 - age;
            for (int j = 0; j < 10; j++) {
                int globalIdx = ci * 10 + j;
                boolean isMale = j % 2 == 0;
                String firstName = isMale ? MALE_FIRST[globalIdx % MALE_FIRST.length] : FEMALE_FIRST[globalIdx % FEMALE_FIRST.length];
                String lastName = LAST_NAMES[globalIdx % LAST_NAMES.length];
                String name = firstName + " " + lastName;
                String id;
                boolean isDemoAarav = (ci == 8 && j == 0);
                if (isDemoAarav) { id = "s1"; name = "Aarav Kumar"; }
                else { id = "s" + studentIdCounter; studentIdCounter++; }
                if (ci == 8 && j == 1) { secondClass6StudentId = id; secondClass6StudentName = name; }
                String section = j < 5 ? "A" : "B";
                int month = (globalIdx % 12) + 1;
                int day = (globalIdx % 27) + 1;
                String dob = String.format("%04d-%02d-%02d", dobYear, month, day);
                String house = new String[]{"Red", "Blue", "Green", "Yellow"}[(ci + j) % 4];
                String stream = (className.equals("11") || className.equals("12")) ? new String[]{"Science", "Commerce", "Arts"}[j % 3] : "";
                String fatherFirst = MALE_FIRST[(globalIdx + 7) % MALE_FIRST.length];
                String motherFirst = FEMALE_FIRST[(globalIdx + 11) % FEMALE_FIRST.length];
                String fatherPhone = "98765" + String.format("%05d", (phoneCounter + globalIdx * 2) % 100000);
                String motherPhone = "98766" + String.format("%05d", (phoneCounter + globalIdx * 2 + 1) % 100000);
                String admissionNo = "VS" + (admissionCounter + globalIdx);
                String previousSchool = (className.equals("Nursery") || className.equals("KG1") || className.equals("KG2") || className.equals("1"))
                        ? "" : PREVIOUS_SCHOOLS[globalIdx % PREVIOUS_SCHOOLS.length];

                students.add(row(students, "id", id, "name", name, "admissionNo", admissionNo, "className", className, "section", section,
                        "academicYear", "2026-27", "dob", dob, "gender", isMale ? "Male" : "Female",
                        "bloodGroup", BLOOD_GROUPS[globalIdx % BLOOD_GROUPS.length], "nationality", "Indian",
                        "religion", RELIGIONS[globalIdx % RELIGIONS.length], "category", CATEGORIES[globalIdx % CATEGORIES.length],
                        "motherTongue", MOTHER_TONGUES[globalIdx % MOTHER_TONGUES.length],
                        "aadharNo", String.format("2026%04d%04d", ci, j), "identificationMarks", globalIdx % 2 == 0 ? "None" : "Small scar on left hand",
                        "fatherName", "Mr. " + fatherFirst + " " + lastName, "fatherOccupation", FATHER_OCCUPATIONS[globalIdx % FATHER_OCCUPATIONS.length], "fatherPhone", fatherPhone,
                        "motherName", "Mrs. " + motherFirst + " " + lastName, "motherOccupation", MOTHER_OCCUPATIONS[globalIdx % MOTHER_OCCUPATIONS.length], "motherPhone", motherPhone,
                        "address", "House No. " + (100 + globalIdx) + ", " + STREETS[globalIdx % STREETS.length], "city", "Betul", "state", "Madhya Pradesh", "pincode", "460001",
                        "studentEmail", "", "previousSchool", previousSchool, "parentPhone", fatherPhone, "house", house, "stream", stream));
            }
        }

        // ---- Staff: 50 total (teaching + non-teaching) ----
        Table teachers = t("teachers");
        teachers.add(row(teachers, "id", "t1", "name", "Mrs. Sunita Sharma", "empCode", "EMP001", "designation", "PGT English", "subject", "English", "phone", "9876511001", "email", "sunita@vsvps.edu", "qualification", "M.A. English, B.Ed", "joiningDate", "2018-06-01", "staffType", "Teaching", "canMarkAttendance", "true", "canEnterMarks", "true", "canManageFees", "false"));
        teachers.add(row(teachers, "id", "t2", "name", "Mr. Ramesh Iyer", "empCode", "EMP002", "designation", "TGT Mathematics", "subject", "Mathematics", "phone", "9876511002", "email", "ramesh@vsvps.edu", "qualification", "M.Sc. Mathematics, B.Ed", "joiningDate", "2019-06-01", "staffType", "Teaching", "canMarkAttendance", "true", "canEnterMarks", "true", "canManageFees", "false"));
        teachers.add(row(teachers, "id", "t3", "name", "Ms. Pooja Nair", "empCode", "EMP003", "designation", "TGT Science", "subject", "Science", "phone", "9876511003", "email", "pooja@vsvps.edu", "qualification", "M.Sc. Chemistry, B.Ed", "joiningDate", "2020-06-01", "staffType", "Teaching", "canMarkAttendance", "true", "canEnterMarks", "true", "canManageFees", "false"));

        int teachingCount = 29; // + t1-t3 => 32 teaching, remaining 18 non-teaching = 50 total
        for (int k = 4; k <= 50; k++) {
            int idx = k - 4;
            boolean male = idx % 2 == 0;
            String first = male ? MALE_FIRST[(idx + 15) % MALE_FIRST.length] : FEMALE_FIRST[(idx + 15) % FEMALE_FIRST.length];
            String last = LAST_NAMES[(idx + 5) % LAST_NAMES.length];
            String name = (male ? "Mr. " : "Ms. ") + first + " " + last;
            String empCode = String.format("EMP%03d", k);
            String phone = "98765" + String.format("%05d", 12000 + k);
            String email = (first + "." + last + k).toLowerCase() + "@vsvps.edu";
            String joiningDate = (2014 + (idx % 11)) + "-06-01";
            boolean isTeaching = idx < teachingCount;
            String designation, subject, staffType, qualification;
            boolean canMark, canMarks, canFees;
            if (isTeaching) {
                subject = TEACH_SUBJECTS[idx % TEACH_SUBJECTS.length];
                String level = idx % 3 == 0 ? "PRT " : idx % 3 == 1 ? "TGT " : "PGT ";
                designation = level + subject;
                staffType = "Teaching";
                qualification = "M.A./M.Sc. " + subject + ", B.Ed";
                canMark = true; canMarks = true; canFees = false;
            } else {
                designation = NONTEACH_DESIGNATIONS[(idx - teachingCount) % NONTEACH_DESIGNATIONS.length];
                subject = "";
                staffType = "Non-Teaching";
                qualification = designation.contains("Vice Principal") ? "M.Ed, 15+ yrs experience" : "Graduate";
                canMark = false; canMarks = false;
                canFees = designation.equals("Accountant") || designation.equals("Office Superintendent");
            }
            teachers.add(row(teachers, "id", "t" + k, "name", name, "empCode", empCode, "designation", designation, "subject", subject,
                    "phone", phone, "email", email, "qualification", qualification, "joiningDate", joiningDate, "staffType", staffType,
                    "canMarkAttendance", String.valueOf(canMark), "canEnterMarks", String.valueOf(canMarks), "canManageFees", String.valueOf(canFees)));
        }

        // ---- Classes: Nursery through 12, each assigned a class teacher ----
        Table classes = t("classes");
        for (int ci = 0; ci < CLASS_NAMES_ALL.length; ci++) {
            classes.add(row(classes, "id", "cl" + (ci + 1), "className", CLASS_NAMES_ALL[ci], "sections", "A,B", "classTeacherId", "t" + (ci + 1)));
        }



        // Users (logins) — modern accounts store passwordHash (simple KDF
        // demonstration only; production should use BCrypt/PBKDF2 with salt + rounds).
        // The special gmail demo account ALSO carries a plaintext "password" column
        // (as documented in README) so operators editing the CSV by hand can still
        // authenticate directly, while standard @gmail accounts take the new OTP path
        // automatically when the "send me an OTP" checkbox is checked. Other demo
        // accounts keep their hash forms.
        Table users = t("users");
        users.add(row(users, "id", "u1", "loginId", "admin1", "password", "admin123", "role", "admin", "name", "Admin User", "email", "admin@gmail.com"));
        users.add(row(users, "id", "u2", "loginId", "teacher1", "passwordHash", hashForDemo("teach123"), "role", "teacher", "name", "Mrs. Sunita Sharma", "email", "teacher1@gmail.com", "teacherId", "t1"));
        users.add(row(users, "id", "u3", "loginId", "accountant1", "passwordHash", hashForDemo("acc123"), "role", "accountant", "name", "Mr. Rakesh Verma", "email", "accountant1@gmail.com"));
        users.add(row(users, "id", "u4", "loginId", "parent1", "passwordHash", hashForDemo("parent123"), "role", "parent", "name", "Mr. Anil Kumar", "email", "parent1@gmail.com", "wardIds", "s1"));
        users.add(row(users, "id", "u5", "loginId", "student1", "passwordHash", hashForDemo("stud123"), "role", "student", "name", "Aarav Kumar", "email", "student1@gmail.com", "studentId", "s1"));

        // Subjects / Houses
        Table subjects = t("subjects");
        subjects.add(row(subjects, "id", "sub1", "name", "English", "code", "ENG", "className", "6", "teacher", "Mrs. Sunita Sharma"));
        subjects.add(row(subjects, "id", "sub2", "name", "Mathematics", "code", "MAT", "className", "6", "teacher", "Mr. Ramesh Iyer"));
        subjects.add(row(subjects, "id", "sub3", "name", "Science", "code", "SCI", "className", "9", "teacher", "Ms. Pooja Nair"));

        Table houses = t("houses");
        houses.add(row(houses, "id", "h1", "name", "Red", "captain", "Kabir Singh", "points", "120"));
        houses.add(row(houses, "id", "h2", "name", "Blue", "captain", "Diya Patel", "points", "95"));
        houses.add(row(houses, "id", "h3", "name", "Green", "captain", "Ishaan Gupta", "points", "110"));
        houses.add(row(houses, "id", "h4", "name", "Yellow", "captain", "Anaya Verma", "points", "88"));

        // Admissions
        Table admissions = t("admissions");
        admissions.add(row(admissions, "id", "ad1", "name", "Reyansh Joshi", "dob", "2014-01-10", "className", "5", "parentPhone", "9876522001", "address", "Betul, MP", "status", "New", "notes", "Enquired via phone"));
        admissions.add(row(admissions, "id", "ad2", "name", "Myra Shah", "dob", "2013-09-05", "className", "6", "parentPhone", "9876522002", "address", "Badora, MP", "status", "Followed Up", "notes", "Campus visit scheduled"));

        // Timetable (Mon-Sat rows per class) -- generated for all 15 classes, tiered by level
        Table tt = t("timetables");
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        String[] earlyYearsSubjects = {"Rhymes","Drawing","Play Time","Story Time","Music","Alphabet","Numbers","Dance"};
        String[] primarySubjects = {"English","Maths","EVS","Hindi","Art","PE","Computer","Library"};
        String[] middleSubjects = {"English","Maths","Science","SST","Hindi","Computer","Art","PE","Library","Sanskrit"};
        String[] seniorSubjects = {"English","Maths","Physics","Chemistry","Biology","SST","Computer","PE","Economics","Commerce"};
        for (int ci = 0; ci < CLASS_NAMES_ALL.length; ci++) {
            String className = CLASS_NAMES_ALL[ci];
            String[] pool = ci <= 2 ? earlyYearsSubjects : ci <= 7 ? primarySubjects : ci <= 10 ? middleSubjects : seniorSubjects;
            for (int di = 0; di < days.length; di++) {
                StringBuilder periods = new StringBuilder();
                int periodsPerDay = di == 5 ? 4 : 6; // Saturday shorter
                for (int p = 0; p < periodsPerDay; p++) {
                    if (p > 0) periods.append(", ");
                    if (di == 5 && p == periodsPerDay - 1) periods.append("Assembly");
                    else periods.append(pool[(ci + di + p) % pool.length]);
                }
                tt.add(row(tt, "id", newId("tt"), "className", className, "day", days[di], "periods", periods.toString()));
            }
        }

        // Homework
        Table hw = t("homework");
        hw.add(row(hw, "id", "hw1", "className", "6", "subject", "English", "title", "Essay Writing", "description", "Write a 200-word essay on 'My Favourite Season'.", "dueDate", todayPlus(5)));
        hw.add(row(hw, "id", "hw2", "className", "9", "subject", "Science", "title", "Lab Record", "description", "Complete diagrams for Chapter 3 experiments.", "dueDate", todayPlus(3)));

        // Exam + marks
        Table exams = t("exams");
        exams.add(row(exams, "id", "ex1", "title", "Mid-Term Examination", "className", "6", "subjects", "English:100;Mathematics:100;Science:100"));
        Table marks = t("exam_marks");
        marks.add(row(marks, "id", newId("mk"), "examId", "ex1", "studentId", "s1", "marks", "English:78;Mathematics:88;Science:82"));
        marks.add(row(marks, "id", newId("mk"), "examId", "ex1", "studentId", secondClass6StudentId, "marks", "English:91;Mathematics:76;Science:85"));

        Table gh = t("grade_history");
        gh.add(row(gh, "id", newId("gh"), "studentId", "s1", "term", "Term 1 (Prev. Year)", "percentage", "81.2", "grade", "A2", "result", "Pass"));

        // Fees
        Table fees = t("fees");
        fees.add(row(fees, "id", newId("fee"), "studentId", "s1", "studentName", "Aarav Kumar", "className", "6", "head", "Tuition Fee", "term", "Q1", "amount", "8000", "dueDate", todayPlus(-10), "status", "Due", "paidDate", ""));
        fees.add(row(fees, "id", newId("fee"), "studentId", secondClass6StudentId, "studentName", secondClass6StudentName, "className", "6", "head", "Tuition Fee", "term", "Q1", "amount", "8000", "dueDate", todayPlus(-10), "status", "Paid", "paidDate", todayPlus(-8)));

        // Ledger
        Table ledger = t("ledger");
        ledger.add(row(ledger, "id", newId("led"), "type", "Income", "category", "Tuition Fee", "amount", "8000", "date", todayPlus(-8), "note", secondClass6StudentName + " - Q1 tuition"));
        ledger.add(row(ledger, "id", newId("led"), "type", "Expense", "category", "Stationery", "amount", "3500", "date", todayPlus(-5), "note", "Office stationery purchase"));

        // Leave
        Table leave = t("leave_apps");
        leave.add(row(leave, "id", newId("lv"), "appliedBy", "u5", "appliedByName", "Aarav Kumar", "role", "student", "fromDate", todayPlus(2), "toDate", todayPlus(3), "reason", "Family function", "status", "Pending"));

        // Notices
        Table notices = t("notices");
        notices.add(row(notices, "id", newId("nt"), "title", "Annual Sports Day", "category", "Event", "date", todayPlus(7), "body", "Annual Sports Day will be held on the school ground. All students must report by 8 AM in sports uniform."));
        notices.add(row(notices, "id", newId("nt"), "title", "Mid-Term Exam Datesheet Released", "category", "Academic", "date", todayPlus(-2), "body", "The datesheet for Mid-Term Examinations has been uploaded. Please check with your class teacher."));

        // Library
        Table books = t("books");
        books.add(row(books, "id", "bk1", "title", "NCERT Mathematics VI", "author", "NCERT", "isbn", "978-8174507421", "copiesTotal", "10", "copiesAvailable", "8"));
        books.add(row(books, "id", "bk2", "title", "Wings of Fire", "author", "A.P.J. Abdul Kalam", "isbn", "978-8173711466", "copiesTotal", "5", "copiesAvailable", "5"));
        Table issues = t("library_issues");
        issues.add(row(issues, "id", newId("li"), "bookId", "bk1", "bookTitle", "NCERT Mathematics VI", "studentId", "s1", "studentName", "Aarav Kumar", "issueDate", todayPlus(-10), "dueDate", todayPlus(4), "status", "Issued", "returnDate", ""));

        // Events
        Table events = t("events");
        events.add(row(events, "id", newId("ev"), "title", "Science Exhibition", "club", "Science Club", "date", todayPlus(14), "venue", "School Auditorium"));
        events.add(row(events, "id", newId("ev"), "title", "Inter-House Debate", "club", "Literary Club", "date", todayPlus(20), "venue", "Room 12"));

        // Tickets
        Table tickets = t("tickets");
        tickets.add(row(tickets, "id", newId("tk"), "subject", "Login issue", "category", "Technical", "description", "Unable to view timetable page.", "raisedBy", "u5", "raisedByName", "Aarav Kumar", "status", "Open"));

        // Hostel
        Table hostel = t("hostel");
        hostel.add(row(hostel, "id", "hostel1", "block", "Block C", "room", "204", "warden", "Mr. Devendra Chouhan", "bedType", "Double Sharing"));
    }
}
