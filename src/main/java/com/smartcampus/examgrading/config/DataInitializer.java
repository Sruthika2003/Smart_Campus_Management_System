package com.smartcampus.examgrading.config;

import com.smartcampus.examgrading.model.*;
import com.smartcampus.examgrading.repository.CourseRepository;
import com.smartcampus.examgrading.repository.EnrollmentRepository;
import com.smartcampus.examgrading.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final Random random = new Random();

    @PostConstruct
    @Transactional
    public void init() {
        if (userRepository.count() > 0)
            return;

        LocalDateTime now = LocalDateTime.now();

        // Create and save users with encoded passwords and proper timestamps
        User admin = createUser("admin", "Admin", "User", "admin@example.com", User.Role.ADMIN);

        List<User> faculty = List.of(
                createUser("prof1", "John", "Doe", "john.doe@example.com", User.Role.FACULTY),
                createUser("prof2", "Jane", "Smith", "jane.smith@example.com", User.Role.FACULTY),
                createUser("prof3", "Robert", "Johnson", "robert.johnson@example.com", User.Role.FACULTY),
                createUser("prof4", "Michelle", "Williams", "michelle.williams@example.com", User.Role.FACULTY)
        );

        List<User> students = List.of(
                createUser("student1", "Alice", "Johnson", "alice@example.com", User.Role.STUDENT),
                createUser("student2", "Bob", "Brown", "bob@example.com", User.Role.STUDENT),
                createUser("student3", "Charlie", "Davis", "charlie@example.com", User.Role.STUDENT),
                createUser("student4", "Diana", "Edwards", "diana@example.com", User.Role.STUDENT),
                createUser("student5", "Edward", "Franklin", "edward@example.com", User.Role.STUDENT),
                createUser("student6", "Fiona", "Garcia", "fiona@example.com", User.Role.STUDENT),
                createUser("student7", "George", "Hernandez", "george@example.com", User.Role.STUDENT),
                createUser("student8", "Hannah", "Ingram", "hannah@example.com", User.Role.STUDENT)
        );

        userRepository.save(admin);
        userRepository.saveAll(faculty);
        userRepository.saveAll(students);

        // Create and save courses with detailed descriptions
        Course course1 = createCourse("CS101", "Introduction to Programming", 
            "Learn the fundamentals of programming using Java. Topics include variables, control structures, functions, and basic object-oriented programming concepts.", 
            30, 3, faculty.get(0));
        Course course2 = createCourse("CS201", "Data Structures", 
            "Advanced programming concepts focusing on data structures and algorithms. Topics include arrays, linked lists, trees, graphs, and algorithm analysis.", 
            25, 4, faculty.get(0));
        Course course3 = createCourse("CS301", "Database Systems", 
            "Introduction to database design and management. Covers ER diagrams, SQL, normalization, and transaction management.", 
            20, 3, faculty.get(1));
        Course course4 = createCourse("MATH101", "Calculus I", 
            "Introduction to differential and integral calculus. Topics include limits, derivatives, and basic integration techniques.", 
            35, 4, faculty.get(1));
        Course course5 = createCourse("CS401", "Artificial Intelligence", 
            "Fundamentals of AI including search algorithms, knowledge representation, machine learning, and neural networks.", 
            25, 4, faculty.get(2));
        Course course6 = createCourse("CS501", "Web Development", 
            "Modern web development using HTML5, CSS3, JavaScript, and popular frameworks. Covers both frontend and backend development.", 
            30, 3, faculty.get(3));

        // Add schedules to each course
        addSchedule(course1, DayOfWeek.MONDAY, "09:00", "10:30", "Room 101");
        addSchedule(course1, DayOfWeek.WEDNESDAY, "09:00", "10:30", "Room 101");

        addSchedule(course2, DayOfWeek.TUESDAY, "11:00", "12:30", "Room 102");
        addSchedule(course2, DayOfWeek.THURSDAY, "11:00", "12:30", "Room 102");

        addSchedule(course3, DayOfWeek.MONDAY, "14:00", "15:30", "Room 201");
        addSchedule(course3, DayOfWeek.WEDNESDAY, "14:00", "15:30", "Room 201");

        addSchedule(course4, DayOfWeek.TUESDAY, "09:00", "10:30", "Room 301");
        addSchedule(course4, DayOfWeek.THURSDAY, "09:00", "10:30", "Room 301");

        addSchedule(course5, DayOfWeek.MONDAY, "11:00", "12:30", "Room 401");
        addSchedule(course5, DayOfWeek.FRIDAY, "11:00", "12:30", "Room 401");

        addSchedule(course6, DayOfWeek.WEDNESDAY, "15:00", "16:30", "Room 501");
        addSchedule(course6, DayOfWeek.FRIDAY, "15:00", "16:30", "Room 501");

        // Save all courses
        courseRepository.saveAll(List.of(course1, course2, course3, course4, course5, course6));
    }

    private User createUser(String username, String firstName, String lastName, String email, User.Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("password")); // In production, use a proper password policy
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setRole(role);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    private Course createCourse(String code, String title, String description, int capacity, int credits,
            User instructor) {
        Course course = new Course();
        course.setCourseCode(code);
        course.setCourseName(title);
        course.setContent(description);
        course.setCapacity(capacity);
        course.setCreditHours(credits);
        course.setFaculty(instructor);
        return course;
    }

    private void addSchedule(Course course, DayOfWeek day, String startTimeStr, String endTimeStr, String location) {
        Timetable schedule = new Timetable();
        schedule.setDayOfWeek(day);
        schedule.setStartTime(LocalTime.parse(startTimeStr));
        schedule.setEndTime(LocalTime.parse(endTimeStr));
        schedule.setLocation(location);
        schedule.setCourse(course);
        course.getSchedules().add(schedule);
    }
}
