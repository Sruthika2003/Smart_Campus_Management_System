package com.smartcampus.examgrading.view.student;

import com.smartcampus.examgrading.model.Course;
import com.smartcampus.examgrading.model.Enrollment;
import com.smartcampus.examgrading.model.Schedule;
import com.smartcampus.examgrading.model.Timetable;
import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.security.SecurityService;
import com.smartcampus.examgrading.service.CourseService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Div;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Route(value = "student/timetable", layout = StudentView.class)
@PageTitle("Timetable | Course Management System")
public class TimetableView extends VerticalLayout {
    private static final Logger logger = LoggerFactory.getLogger(TimetableView.class);
    private final Grid<Map<String, String>> grid = new Grid<>();
    private final CourseService courseService;
    private final SecurityService securityService;
    private static final String[] TIME_SLOTS = {
            "09:00-10:00", "10:00-11:00", "11:00-12:00", "12:00-13:00",
            "13:00-14:00", "14:00-15:00", "15:00-16:00"
    };

    public TimetableView(CourseService courseService, SecurityService securityService) {
        this.courseService = courseService;
        this.securityService = securityService;

        try {
            if (!securityService.isLoggedIn() || !securityService.hasRole(User.Role.STUDENT)) {
                getUI().ifPresent(ui -> ui.navigate(""));
                return;
            }

            setSizeFull();
            setSpacing(true);
            setPadding(true);

            H2 heading = new H2("My Timetable");
            Paragraph description = new Paragraph(
                    "View your course schedule for the week (Indian Standard Time, 9:00 AM - 4:00 PM)");

            // Add refresh button
            Button refreshButton = new Button("Refresh Schedule", e -> {
                logger.info("Manual refresh requested by student: {}",
                        securityService.getCurrentUser().getUsername());

                // Force a complete refresh of the UI by navigating to the same page
                getUI().ifPresent(ui -> {
                    // First show notification
                    showNotification("Refreshing timetable data...", NotificationVariant.LUMO_SUCCESS);

                    // Then perform the refresh
                    try {
                        // Force clear any cached data
                        grid.setItems(Collections.emptyList());

                        // Update the grid with fresh data
                        updateGrid();

                        // Confirm success
                        showNotification("Timetable refreshed with the latest schedule data",
                                NotificationVariant.LUMO_SUCCESS);
                    } catch (Exception ex) {
                        logger.error("Error refreshing timetable: {}", ex.getMessage(), ex);
                        showNotification("Error refreshing timetable: " + ex.getMessage(),
                                NotificationVariant.LUMO_ERROR);
                    }
                });
            });

            // Add button to view all courses
            Button viewCoursesButton = new Button("View All Courses", e -> {
                showAllCoursesDialog();
            });

            HorizontalLayout actions = new HorizontalLayout(refreshButton, viewCoursesButton);
            actions.setSpacing(true);

            HorizontalLayout header = new HorizontalLayout(heading, actions);
            header.setWidthFull();
            header.setJustifyContentMode(JustifyContentMode.BETWEEN);
            header.setVerticalComponentAlignment(Alignment.CENTER, heading, actions);

            add(header);
            add(description);

            configureGrid();
            updateGrid();

            add(grid);
        } catch (Exception e) {
            logger.error("Error initializing TimetableView", e);
            showNotification("Error initializing view: " + e.getMessage(),
                    NotificationVariant.LUMO_ERROR);
        }
    }

    private void configureGrid() {
        try {
            grid.addThemeVariants(GridVariant.LUMO_NO_BORDER,
                    GridVariant.LUMO_ROW_STRIPES,
                    GridVariant.LUMO_WRAP_CELL_CONTENT);
            grid.setSizeFull();
            grid.setHeight("600px");

            grid.addColumn(data -> data.get("Time"))
                    .setHeader("Time (IST)")
                    .setWidth("150px")
                    .setFrozen(true)
                    .setSortable(false);

            for (DayOfWeek day : DayOfWeek.values()) {
                String dayName = day.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
                grid.addColumn(data -> data.get(dayName))
                        .setHeader(dayName)
                        .setAutoWidth(true)
                        .setSortable(false)
                        .setClassNameGenerator(item -> "timetable-cell");
            }

            // Add CSS for better display
            getElement().executeJs(
                    "const style = document.createElement('style');" +
                            "style.textContent = '.timetable-cell { white-space: pre-wrap; min-height: 70px; }'; " +
                            "document.head.appendChild(style);");

        } catch (Exception e) {
            logger.error("Error configuring timetable grid", e);
            showNotification("Error configuring timetable: " + e.getMessage(),
                    NotificationVariant.LUMO_ERROR);
        }
    }

    private void updateGrid() {
        try {
            logger.info("Starting timetable update for student: {}",
                    securityService.getCurrentUser().getUsername());

            // Force clear any cached data by emptying the grid first
            grid.setItems(Collections.emptyList());

            // Get fresh data from database with explicit loading of schedules
            List<Enrollment> enrollments = courseService.getStudentEnrollments(securityService.getCurrentUser());
            logger.info("Retrieved {} enrollments for student", enrollments.size());

            // List to track issues for reporting to the user
            List<String> issues = new ArrayList<>();

            // Track courses without schedules to inform the user
            List<String> coursesWithoutSchedules = new ArrayList<>();

            // Initialize grid items with time slots
            List<Map<String, String>> gridItems = new ArrayList<>();
            for (String timeSlot : TIME_SLOTS) {
                Map<String, String> row = new HashMap<>();
                row.put("Time", timeSlot);
                for (DayOfWeek day : DayOfWeek.values()) {
                    row.put(day.getDisplayName(TextStyle.FULL, Locale.ENGLISH), "");
                }
                gridItems.add(row);
            }

            logger.info("Initialized grid with {} time slots", TIME_SLOTS.length);

            // Track if we found any valid schedules
            boolean hasValidSchedules = false;

            // Process each enrollment
            for (Enrollment enrollment : enrollments) {
                try {
                    Course course = enrollment.getCourse();
                    if (course == null) {
                        issues.add("Found enrollment with missing course data");
                        continue;
                    }

                    List<Timetable> schedules = course.getSchedules();
                    int scheduleCount = schedules != null ? schedules.size() : 0;
                    logger.info("Processing course: {} with {} schedules", course.getCourseCode(), scheduleCount);

                    // Skip courses with no schedules instead of adding placeholder
                    if (schedules == null || schedules.isEmpty()) {
                        logger.info("Course {} has no schedules, skipping", course.getCourseCode());
                        coursesWithoutSchedules.add(course.getCourseCode() + " - " + course.getCourseName());
                        continue;
                    }

                    // Process each schedule for the course
                    for (Timetable schedule : schedules) {
                        try {
                            if (schedule == null) {
                                logger.warn("Null schedule found in course: {}", course.getCourseCode());
                                continue;
                            }

                            // Validate schedule data
                            if (schedule.getDayOfWeek() == null ||
                                    schedule.getStartTime() == null ||
                                    schedule.getEndTime() == null) {
                                logger.warn("Invalid schedule data for course {}: day={}, start={}, end={}",
                                        course.getCourseCode(),
                                        schedule.getDayOfWeek(),
                                        schedule.getStartTime(),
                                        schedule.getEndTime());
                                continue;
                            }

                            logger.info("Processing schedule: day={}, time={}-{}, location={}",
                                    schedule.getDayOfWeek(),
                                    schedule.getStartTime(),
                                    schedule.getEndTime(),
                                    schedule.getLocation());

                            // Get the time slot and day for this schedule
                            String timeSlot = findTimeSlot(schedule.getStartTime(), schedule.getEndTime());
                            String dayName = schedule.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);

                            logger.info("Mapped to time slot: {} on {}", timeSlot, dayName);

                            // Find the matching row for this time slot
                            Map<String, String> matchingRow = null;
                            for (Map<String, String> row : gridItems) {
                                if (row.get("Time").equals(timeSlot)) {
                                    matchingRow = row;
                                    break;
                                }
                            }

                            // If we couldn't find a matching row, add a new one
                            if (matchingRow == null) {
                                logger.info("Creating new row for time slot: {}", timeSlot);
                                matchingRow = new HashMap<>();
                                matchingRow.put("Time", timeSlot);
                                for (DayOfWeek day : DayOfWeek.values()) {
                                    matchingRow.put(day.getDisplayName(TextStyle.FULL, Locale.ENGLISH), "");
                                }
                                gridItems.add(matchingRow);
                                // Sort grid items by time
                                gridItems.sort(Comparator.comparing(row -> row.get("Time")));
                            }

                            // Add the course to the grid
                            String existingContent = matchingRow.get(dayName);
                            if (existingContent == null) {
                                existingContent = "";
                            }

                            String newContent = String.format("%s%n%s%n%s",
                                    course.getCourseCode(),
                                    course.getCourseName(),
                                    schedule.getLocation() != null ? schedule.getLocation() : "No location");

                            if (!existingContent.isEmpty()) {
                                newContent = existingContent + "\n---\n" + newContent;
                            }

                            matchingRow.put(dayName, newContent);
                            hasValidSchedules = true;

                            logger.info("Added course {} to timetable at {}, {}",
                                    course.getCourseCode(), dayName, timeSlot);
                        } catch (Exception scheduleEx) {
                            logger.error("Error processing schedule for course {}: {}",
                                    course.getCourseCode(), scheduleEx.getMessage(), scheduleEx);
                            issues.add("Error processing schedule for " + course.getCourseCode());
                        }
                    }
                } catch (Exception courseEx) {
                    logger.error("Error processing enrollment: {}", courseEx.getMessage(), courseEx);
                    issues.add("Error processing course: " + courseEx.getMessage());
                }
            }

            // Update the grid
            logger.info("Setting grid with {} items, hasValidSchedules={}", gridItems.size(), hasValidSchedules);
            grid.setItems(gridItems);

            // Show appropriate notifications
            if (!hasValidSchedules) {
                String message = "No scheduled courses found. Your enrolled courses don't have any assigned schedules yet.";
                if (!issues.isEmpty()) {
                    message += " Issues found: " + String.join(", ", issues);
                }
                showNotification(message, NotificationVariant.LUMO_PRIMARY);
            } else {
                if (!coursesWithoutSchedules.isEmpty()) {
                    String message = "The following courses have no schedules assigned yet: " +
                            String.join(", ", coursesWithoutSchedules);
                    showNotification(message, NotificationVariant.LUMO_PRIMARY);
                }

                if (!issues.isEmpty()) {
                    showNotification(
                            "Some issues occurred while loading your timetable. Please contact support if needed.",
                            NotificationVariant.LUMO_WARNING);
                }
            }

        } catch (Exception e) {
            logger.error("Error updating timetable: {}", e.getMessage(), e);
            showNotification("Error loading timetable: " + e.getMessage(),
                    NotificationVariant.LUMO_ERROR);

            // Create simple example timetable so the page doesn't appear empty
            createExampleTimetable();
        }
    }

    private void createExampleTimetable() {
        List<Map<String, String>> exampleItems = new ArrayList<>();

        for (String timeSlot : TIME_SLOTS) {
            Map<String, String> row = new HashMap<>();
            row.put("Time", timeSlot);
            for (DayOfWeek day : DayOfWeek.values()) {
                row.put(day.getDisplayName(TextStyle.FULL, Locale.ENGLISH), "");
            }
            exampleItems.add(row);
        }

        // Add example data to Monday
        if (!exampleItems.isEmpty()) {
            Map<String, String> mondayMorning = exampleItems.get(1);
            mondayMorning.put("Monday", "Example Course\nCourse Description\nRoom 101");

            // Add another example for Wednesday
            Map<String, String> wednesdayAfternoon = exampleItems.get(4);
            wednesdayAfternoon.put("Wednesday", "Another Course\nAnother Description\nRoom 202");

            // Add an example for Friday
            Map<String, String> fridayClass = exampleItems.get(3);
            fridayClass.put("Friday", "IST Class\nIndian Standard Time\nRoom 305");
        }

        grid.setItems(exampleItems);
    }

    private String findTimeSlot(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            logger.warn("Invalid time values: start={}, end={}", startTime, endTime);
            return null;
        }

        // Format the time with 24-hour format
        String timeString = String.format("%02d:%02d-%02d:%02d",
                startTime.getHour(), startTime.getMinute(),
                endTime.getHour(), endTime.getMinute());

        logger.debug("Looking for time slot matching: {}", timeString);

        // First try exact match
        for (String slot : TIME_SLOTS) {
            if (slot.equals(timeString)) {
                return slot;
            }
        }

        // If no exact match, try to find the closest hour slot
        // Round to the nearest hour for start time
        int startHour = startTime.getHour();
        if (startTime.getMinute() >= 30) {
            startHour += 1;
        }

        // Make sure the hour is within our time slot range (9am-4pm)
        startHour = Math.max(9, Math.min(15, startHour));

        // Create a one-hour slot starting at the rounded hour
        String formattedSlot = String.format("%02d:00-%02d:00",
                startHour, startHour + 1);

        // Check if this slot exists in our predefined slots
        for (String slot : TIME_SLOTS) {
            if (slot.equals(formattedSlot)) {
                return slot;
            }
        }

        logger.info("No exact match found for time slot {}. Using calculated slot: {}",
                timeString, formattedSlot);

        // If we still don't have a match, just use the first available slot
        if (startHour < 9 || startHour >= 16) {
            return TIME_SLOTS[0]; // Default to first slot if outside our range
        }

        return formattedSlot;
    }

    private void showNotification(String message, NotificationVariant variant) {
        getUI().ifPresent(ui -> ui.access(() -> {
            Notification notification = Notification.show(message, 3000, Notification.Position.MIDDLE);
            notification.addThemeVariants(variant);
        }));
    }

    private void showAllCoursesDialog() {
        try {
            List<Enrollment> enrollments = courseService.getStudentEnrollments(securityService.getCurrentUser());

            Dialog dialog = new Dialog();
            dialog.setWidth("600px");

            VerticalLayout content = new VerticalLayout();
            content.setPadding(true);
            content.setSpacing(true);

            H3 title = new H3("My Enrolled Courses");
            content.add(title);

            // Add course information
            Grid<Enrollment> coursesGrid = new Grid<>();
            coursesGrid.addColumn(enrollment -> enrollment.getCourse().getCourseCode())
                    .setHeader("Course Code")
                    .setAutoWidth(true);

            coursesGrid.addColumn(enrollment -> enrollment.getCourse().getCourseName())
                    .setHeader("Course Name")
                    .setAutoWidth(true);

            coursesGrid.addColumn(enrollment -> {
                Course course = enrollment.getCourse();
                List<Timetable> schedules = course.getSchedules();
                if (schedules == null || schedules.isEmpty()) {
                    return "No schedule";
                } else {
                    return schedules.size() + " scheduled time(s)";
                }
            })
                    .setHeader("Schedule Status")
                    .setAutoWidth(true);

            coursesGrid.setItems(enrollments);
            coursesGrid.setHeight("300px");

            // Description of what this means
            Div description = new Div();
            description.setText("Courses with schedules are displayed in the timetable grid. " +
                    "Courses without schedules will not appear until schedules are assigned by faculty. " +
                    "All times are shown in Indian Standard Time (IST) from 9:00 AM to 4:00 PM.");
            description.getStyle().set("margin-top", "10px");
            description.getStyle().set("color", "var(--lumo-secondary-text-color)");

            // Add a close button
            Button closeButton = new Button("Close", e -> dialog.close());

            content.add(coursesGrid, description, closeButton);
            dialog.add(content);

            dialog.open();
        } catch (Exception e) {
            logger.error("Error showing courses dialog", e);
            showNotification("Could not load course information: " + e.getMessage(),
                    NotificationVariant.LUMO_ERROR);
        }
    }
}