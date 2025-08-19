package studentmanagementsystem;

import java.util.*;
import java.io.*;
import java.util.stream.Collectors;

public class StudentManagementSystem {

    static class Student {
        private final String id;
        private String name;
        private int age;
        private String email;
        private double gpa;
        private final List<String> courses;

        public Student(String id, String name, int age, String email, double gpa) {
            this.id = id;
            this.name = name;
            this.age = age;
            this.email = email;
            this.gpa = gpa;
            this.courses = new ArrayList<>();
        }

        // Getters and setters
        public String getId() { return id; }
        public String getName() { return name; }
        public int getAge() { return age; }
        public String getEmail() { return email; }
        public double getGpa() { return gpa; }
        public List<String> getCourses() { return courses; }

        public void setName(String name) { this.name = name; }
        public void setAge(int age) { this.age = age; }
        public void setEmail(String email) { this.email = email; }
        public void setGpa(double gpa) { this.gpa = gpa; }

        public void addCourse(String course) {
            if (!courses.contains(course)) {
                courses.add(course);
            }
        }

        public void removeCourse(String course) {
            courses.remove(course);
        }

        public String toCSV() {
            return String.join(",", id, name, String.valueOf(age), email,
                    String.valueOf(gpa), String.join(";", courses));
        }

        public static Student fromCSV(String csvLine) {
            String[] parts = csvLine.split(",");
            if (parts.length >= 5) {
                Student student = new Student(parts[0], parts[1],
                        Integer.parseInt(parts[2]),
                        parts[3],
                        Double.parseDouble(parts[4]));
                if (parts.length > 5 && !parts[5].isEmpty()) {
                    String[] courses = parts[5].split(";");
                    for (String course : courses) {
                        student.addCourse(course);
                    }
                }
                return student;
            }
            return null;
        }

        @Override
        public String toString() {
            return String.format(
                    "| %-6s | %-15s | %-3d | %-25s | %-4.2f | %s |",
                    id, name, age, email, gpa, courses.isEmpty() ? "-" : String.join(";", courses)
            );
        }
    }

    private final Map<String, Student> students;
    private final Scanner scanner;
    private final String DATA_FILE = "students.csv";

    public StudentManagementSystem() {
        students = new HashMap<>();
        scanner = new Scanner(System.in);
        loadStudentsFromFile();
    }

    public void start() {
        System.out.println("\n===================================");
        System.out.println("     🎓 Student Management System  ");
        System.out.println("===================================");

        while (true) {
            displayMenu();
            int choice = getIntInput("Enter your choice: ");

            switch (choice) {
                case 1 -> addStudent();
                case 2 -> viewAllStudents();
                case 3 -> searchStudent();
                case 4 -> updateStudent();
                case 5 -> deleteStudent();
                case 6 -> addCourseToStudent();
                case 7 -> generateReports();
                case 8 -> {
                    saveStudentsToFile();
                    System.out.println("\n✅ Data saved. Goodbye!");
                    return;
                }
                default -> System.out.println("⚠️ Invalid choice. Try again.");
            }
        }
    }

    private void displayMenu() {
        System.out.println("\n----------- MENU -----------");
        System.out.println("1️⃣  Add Student");
        System.out.println("2️⃣  View All Students");
        System.out.println("3️⃣  Search Student");
        System.out.println("4️⃣  Update Student");
        System.out.println("5️⃣  Delete Student");
        System.out.println("6️⃣  Add Course to Student");
        System.out.println("7️⃣  Generate Reports");
        System.out.println("8️⃣  Exit");
        System.out.println("----------------------------");
    }

    private void addStudent() {
        System.out.println("\n--- Add Student ---");

        String id = getStringInput("Enter Student ID: ");
        if (students.containsKey(id)) {
            System.out.println("⚠️ Student with ID " + id + " already exists.");
            return;
        }

        String name = getStringInput("Enter Name: ");
        int age = getIntInput("Enter Age: ");
        String email = getStringInput("Enter Email: ");
        double gpa = getDoubleInput();

        Student student = new Student(id, name, age, email, gpa);
        students.put(id, student);

        System.out.println("\n✅ Student added successfully!");
        printStudentHeader();
        System.out.println(student);
        printStudentFooter();
    }

    private void viewAllStudents() {
        System.out.println("\n--- All Students ---");

        if (students.isEmpty()) {
            System.out.println("⚠️ No students found.");
            return;
        }

        printStudentHeader();
        students.values().stream()
                .sorted(Comparator.comparing(Student::getName))
                .forEach(System.out::println);
        printStudentFooter();
    }

    private void searchStudent() {
        System.out.println("\n--- Search Student ---");
        String id = getStringInput("Enter Student ID: ");

        Student student = students.get(id);
        if (student != null) {
            System.out.println("\n✅ Student found:");
            printStudentHeader();
            System.out.println(student);
            printStudentFooter();
        } else {
            System.out.println("⚠️ Student not found.");
        }
    }

    private void updateStudent() {
        System.out.println("\n--- Update Student ---");
        String id = getStringInput("Enter Student ID: ");

        Student student = students.get(id);
        if (student == null) {
            System.out.println("⚠️ Student not found.");
            return;
        }

        System.out.println("\nCurrent details:");
        printStudentHeader();
        System.out.println(student);
        printStudentFooter();

        System.out.println("Enter new details (press Enter to keep current):");

        String name = getStringInput("Name [" + student.getName() + "]: ");
        if (!name.isEmpty()) {
            student.setName(name);
        }

        String ageStr = getStringInput("Age [" + student.getAge() + "]: ");
        if (!ageStr.isEmpty()) {
            try {
                student.setAge(Integer.parseInt(ageStr));
            } catch (NumberFormatException e) {
                System.out.println("⚠️ Invalid age format.");
            }
        }

        String email = getStringInput("Email [" + student.getEmail() + "]: ");
        if (!email.isEmpty()) {
            student.setEmail(email);
        }

        String gpaStr = getStringInput("GPA [" + student.getGpa() + "]: ");
        if (!gpaStr.isEmpty()) {
            try {
                student.setGpa(Double.parseDouble(gpaStr));
            } catch (NumberFormatException e) {
                System.out.println("⚠️ Invalid GPA format.");
            }
        }

        System.out.println("\n✅ Student updated:");
        printStudentHeader();
        System.out.println(student);
        printStudentFooter();
    }

    private void deleteStudent() {
        System.out.println("\n--- Delete Student ---");
        String id = getStringInput("Enter Student ID: ");

        Student removed = students.remove(id);
        if (removed != null) {
            System.out.println("\n🗑️ Student deleted:");
            printStudentHeader();
            System.out.println(removed);
            printStudentFooter();
        } else {
            System.out.println("⚠️ Student not found.");
        }
    }

    private void addCourseToStudent() {
        System.out.println("\n--- Add Course to Student ---");
        String id = getStringInput("Enter Student ID: ");

        Student student = students.get(id);
        if (student == null) {
            System.out.println("⚠️ Student not found.");
            return;
        }

        String course = getStringInput("Enter Course Name: ");
        student.addCourse(course);

        System.out.println("\n✅ Course added. Updated student:");
        printStudentHeader();
        System.out.println(student);
        printStudentFooter();
    }

    private void generateReports() {
        System.out.println("\n--- Reports ---");
        System.out.println("1. Students by GPA (High → Low)");
        System.out.println("2. Students by Age");
        System.out.println("3. Course Enrollment Report");

        int choice = getIntInput("Choose report: ");

        switch (choice) {
            case 1 -> {
                System.out.println("\n📊 Students by GPA:");
                students.values().stream()
                        .sorted(Comparator.comparing(Student::getGpa).reversed())
                        .forEach(s -> System.out.printf(" %-15s | GPA: %.2f%n", s.getName(), s.getGpa()));
            }
            case 2 -> {
                System.out.println("\n📊 Students by Age:");
                students.values().stream()
                        .sorted(Comparator.comparing(Student::getAge))
                        .forEach(s -> System.out.printf(" %-15s | Age: %d%n", s.getName(), s.getAge()));
            }
            case 3 -> {
                System.out.println("\n📊 Course Enrollment:");
                Map<String, Long> courseCount = students.values().stream()
                        .flatMap(s -> s.getCourses().stream())
                        .collect(Collectors.groupingBy(course -> course, Collectors.counting()));

                if (courseCount.isEmpty()) {
                    System.out.println("⚠️ No courses found.");
                } else {
                    courseCount.entrySet().stream()
                            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                            .forEach(entry -> System.out.printf(" %-20s : %d students%n", entry.getKey(), entry.getValue()));
                }
            }
            default -> System.out.println("⚠️ Invalid choice.");
        }
    }

    // === Utility methods ===

    private void loadStudentsFromFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader(DATA_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Student student = Student.fromCSV(line);
                if (student != null) {
                    students.put(student.getId(), student);
                }
            }
            System.out.println("📂 Loaded " + students.size() + " students from file.");
        } catch (FileNotFoundException e) {
            System.out.println("ℹ️ No existing data file found. Starting fresh.");
        } catch (IOException e) {
            System.out.println("⚠️ Error loading data: " + e.getMessage());
        }
    }

    private void saveStudentsToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(DATA_FILE))) {
            for (Student student : students.values()) {
                writer.println(student.toCSV());
            }
            System.out.println("💾 Saved " + students.size() + " students to file.");
        } catch (IOException e) {
            System.out.println("⚠️ Error saving data: " + e.getMessage());
        }
    }

    private String getStringInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private int getIntInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("⚠️ Invalid number. Try again.");
            }
        }
    }

    private double getDoubleInput() {
        while (true) {
            try {
                System.out.print("Enter GPA: ");
                return Double.parseDouble(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("⚠️ Invalid number. Try again.");
            }
        }
    }

    private void printStudentHeader() {
        System.out.println("--------------------------------------------------------------------------------------------");
        System.out.printf("| %-6s | %-15s | %-3s | %-25s | %-4s | %-20s |%n",
                "ID", "Name", "Age", "Email", "GPA", "Courses");
        System.out.println("--------------------------------------------------------------------------------------------");
    }

    private void printStudentFooter() {
        System.out.println("--------------------------------------------------------------------------------------------");
    }

    public static void main(String[] args) {
        new StudentManagementSystem().start();
    }
}
