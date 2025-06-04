package com.smartcampus.examgrading.view.faculty;

import com.smartcampus.examgrading.model.Course;
import com.smartcampus.examgrading.model.Enrollment;
import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.model.CourseMaterial;
import com.smartcampus.examgrading.security.SecurityService;
import com.smartcampus.examgrading.service.CourseService;
import com.smartcampus.examgrading.service.ReportService;
import com.smartcampus.examgrading.service.CourseMaterialService;
import com.smartcampus.examgrading.service.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.Collectors;

@Route(value = "faculty/courses", layout = FacultyView.class)
@PageTitle("My Courses | Faculty")
public class CoursesView extends VerticalLayout {
    private final Grid<Course> grid = new Grid<>(Course.class);
    private final CourseService courseService;
    private final SecurityService securityService;
    private final ReportService reportService;
    private final CourseMaterialService courseMaterialService;
    private final UserService userService;

    public CoursesView(CourseService courseService, SecurityService securityService,
            ReportService reportService, CourseMaterialService courseMaterialService,
            UserService userService) {
        this.courseService = courseService;
        this.securityService = securityService;
        this.reportService = reportService;
        this.courseMaterialService = courseMaterialService;
        this.userService = userService;

        if (!securityService.isLoggedIn()) {
            getUI().ifPresent(ui -> ui.navigate(""));
            return;
        }

        setSizeFull();
        setSpacing(true);
        setPadding(true);

        H2 title = new H2("My Courses");
        add(title);

        Button addCourseButton = new Button("Add Course", e -> openCourseForm(new Course()));
        add(addCourseButton);

        configureGrid();
        updateGrid();

        add(grid);
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.removeAllColumns();

        grid.addColumn(Course::getCourseCode).setHeader("Course Code").setAutoWidth(true);
        grid.addColumn(Course::getCourseName).setHeader("Course Name").setAutoWidth(true);
        grid.addColumn(Course::getContent).setHeader("Description").setAutoWidth(true);
        grid.addColumn(Course::getCreditHours).setHeader("Credit Hours").setAutoWidth(true);
        grid.addColumn(Course::getCapacity).setHeader("Capacity").setAutoWidth(true);

        grid.addComponentColumn(course -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setWidth("420px");
            actions.setSpacing(true);
            actions.setPadding(false);
            actions.setJustifyContentMode(JustifyContentMode.START);

            Button viewStudentsButton = new Button("View Students", e -> viewStudents(course));
            viewStudentsButton.getStyle().set("margin-right", "5px");
            
            Button editButton = new Button("Edit", e -> openCourseForm(course));
            editButton.getStyle().set("margin-right", "5px");
            
            Button manageMaterialsButton = new Button("Manage Materials", e -> manageMaterials(course));
            manageMaterialsButton.getStyle().set("margin-right", "5px");
            
            Button generateReportButton = new Button("Generate Report", e -> generateReport(course));

            actions.add(viewStudentsButton, editButton, manageMaterialsButton, generateReportButton);
            return actions;
        }).setHeader("Actions").setWidth("430px").setFlexGrow(0);
    }

    private void updateGrid() {
        User faculty = securityService.getCurrentUser();
        if (faculty == null) {
            getUI().ifPresent(ui -> ui.navigate(""));
            return;
        }
        List<Course> courses = courseService.getCoursesByFaculty(faculty);
        grid.setItems(courses);
    }

    private void openCourseForm(Course course) {
        User faculty = securityService.getCurrentUser();
        if (faculty == null) {
            Notification.show("Error: Please log in again", 3000, Notification.Position.TOP_CENTER);
            getUI().ifPresent(ui -> ui.navigate(""));
            return;
        }

        boolean isNewCourse = course.getId() == null;
        Dialog dialog = new Dialog();
        dialog.setWidth("800px");
        dialog.setHeight("90%");
        dialog.setModal(true);
        dialog.setDraggable(false);

        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setSizeFull();
        mainLayout.setSpacing(false);

        H3 title = new H3(isNewCourse ? "Add New Course" : "Edit Course");
        title.getStyle().set("margin", "0 0 1em 0");

        // Create a scrollable container for the form
        VerticalLayout formContainer = new VerticalLayout();
        formContainer.setWidthFull();
        formContainer.getStyle().set("overflow-y", "auto");
        formContainer.setSpacing(true);
        formContainer.setPadding(true);

        FormLayout form = new FormLayout();
        form.setWidthFull();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2));

        TextField courseCodeField = new TextField("Course Code");
        courseCodeField.setRequired(true);
        courseCodeField.setWidthFull();

        TextField courseNameField = new TextField("Course Name");
        courseNameField.setRequired(true);
        courseNameField.setWidthFull();

        TextArea courseDescriptionField = new TextArea("Course Description");
        courseDescriptionField.setRequired(true);
        courseDescriptionField.setWidthFull();

        IntegerField creditHoursField = new IntegerField("Credit Hours");
        creditHoursField.setRequired(true);
        creditHoursField.setMin(1);
        creditHoursField.setMax(6);
        creditHoursField.setValue(3);
        creditHoursField.setWidthFull();

        IntegerField capacityField = new IntegerField("Capacity");
        capacityField.setRequired(true);
        capacityField.setMin(1);
        capacityField.setValue(30);
        capacityField.setWidthFull();

        TextArea contentField = new TextArea("Course Content");
        contentField.setWidthFull();

        form.add(courseCodeField, courseNameField);
        form.add(courseDescriptionField);
        form.add(creditHoursField, capacityField);
        form.add(contentField);

        formContainer.add(form);

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setWidthFull();
        buttons.setJustifyContentMode(JustifyContentMode.END);

        Binder<Course> binder = new Binder<>(Course.class);
        binder.bind(courseCodeField, Course::getCourseCode, Course::setCourseCode);
        binder.bind(courseNameField, Course::getCourseName, Course::setCourseName);
        binder.bind(courseDescriptionField, Course::getContent, Course::setContent);
        binder.bind(creditHoursField, Course::getCreditHours, Course::setCreditHours);
        binder.bind(capacityField, Course::getCapacity, Course::setCapacity);
        binder.bind(contentField, Course::getContent, Course::setContent);

        Button saveButton = new Button("Save", e -> {
            if (binder.validate().isOk()) {
                try {
                    if (courseCodeField.isEmpty() || courseNameField.isEmpty() ||
                            courseDescriptionField.isEmpty() || creditHoursField.isEmpty() ||
                            capacityField.isEmpty()) {
                        Notification.show("Please fill in all required fields", 3000, Notification.Position.TOP_CENTER);
                        return;
                    }
                    binder.writeBean(course);

                    // Get current faculty user
                    User currentFaculty = securityService.getCurrentUser();
                    if (currentFaculty == null) {
                        Notification.show("Error: No faculty user found", 3000, Notification.Position.TOP_CENTER);
                        return;
                    }

                    // Set faculty only for new courses
                    if (course.getId() == null) {
                        course.setFaculty(currentFaculty);
                    }

                    // Ensure courseName is set to avoid null value in title column
                    if (course.getCourseName() == null || course.getCourseName().isEmpty()) {
                        course.setCourseName(courseNameField.getValue());
                    }

                    courseService.saveCourse(course);
                    dialog.close();
                    Notification.show("Course saved successfully", 3000, Notification.Position.TOP_CENTER);
                    updateGrid();
                } catch (Exception ex) {
                    String errorMessage = "Error saving course: " + ex.getMessage();
                    System.err.println(errorMessage);
                    ex.printStackTrace();
                    Notification.show(errorMessage, 3000, Notification.Position.TOP_CENTER);
                }
            }
        });

        Button cancelButton = new Button("Cancel", e -> dialog.close());

        buttons.add(saveButton, cancelButton);
        buttons.setSpacing(true);

        mainLayout.add(title);
        mainLayout.add(formContainer);
        mainLayout.add(buttons);

        mainLayout.setFlexGrow(1, formContainer);

        dialog.add(mainLayout);

        if (!isNewCourse) {
            binder.readBean(course);
        }

        dialog.open();
    }

    private void viewStudents(Course course) {
        Dialog dialog = new Dialog();
        dialog.setWidth("800px");

        VerticalLayout content = new VerticalLayout();
        content.add(new H3("Students Enrolled in " + course.getCourseName()));

        Grid<Enrollment> studentsGrid = new Grid<>(Enrollment.class);
        studentsGrid.setItems(courseService.getEnrollmentsForCourse(course));

        studentsGrid.addColumn(enrollment -> enrollment.getStudent().getFirstName() + " " +
                enrollment.getStudent().getLastName())
                .setHeader("Student Name");
        studentsGrid.addColumn(enrollment -> enrollment.getStudent().getEmail())
                .setHeader("Email");
        studentsGrid.addColumn(Enrollment::getEnrollmentDate)
                .setHeader("Enrollment Date");

        content.add(studentsGrid);
        
        // Add "Add Student" button
        Button addStudentButton = new Button("Add Student", e -> {
            openAddStudentDialog(course, studentsGrid);
        });
        
        HorizontalLayout buttonRow = new HorizontalLayout();
        Button closeButton = new Button("Close", e -> dialog.close());
        buttonRow.add(addStudentButton, closeButton);
        buttonRow.setSpacing(true);
        content.add(buttonRow);

        dialog.add(content);
        dialog.open();
    }
    
    private void openAddStudentDialog(Course course, Grid<Enrollment> studentsGrid) {
        Dialog dialog = new Dialog();
        dialog.setWidth("500px");
        dialog.setHeaderTitle("Add Student to " + course.getCourseName());
        
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(true);
        
        // Get all students with STUDENT role
        List<User> allStudents = userService.getUsersByRole(User.Role.STUDENT);
        
        // Get already enrolled students to exclude them
        List<Enrollment> existingEnrollments = courseService.getEnrollmentsForCourse(course);
        List<Long> enrolledStudentIds = existingEnrollments.stream()
            .map(e -> e.getStudent().getUserId())
            .collect(Collectors.toList());
        
        // Filter out already enrolled students
        List<User> availableStudents = allStudents.stream()
            .filter(s -> !enrolledStudentIds.contains(s.getUserId()))
            .collect(Collectors.toList());
        
        if (availableStudents.isEmpty()) {
            content.add(new Paragraph("All available students are already enrolled in this course."));
            Button closeButton = new Button("Close", e -> dialog.close());
            content.add(closeButton);
            dialog.add(content);
            dialog.open();
            return;
        }
        
        // Student select dropdown
        ComboBox<User> studentSelect = new ComboBox<>("Select Student");
        studentSelect.setItems(availableStudents);
        studentSelect.setItemLabelGenerator(user -> user.getFirstName() + " " + user.getLastName() + " (" + user.getEmail() + ")");
        studentSelect.setWidthFull();
        studentSelect.setRequired(true);
        
        content.add(studentSelect);
        
        HorizontalLayout buttons = new HorizontalLayout();
        Button addButton = new Button("Add Student", e -> {
            User selectedStudent = studentSelect.getValue();
            if (selectedStudent == null) {
                Notification.show("Please select a student", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            
            try {
                // Enroll the student
                Enrollment enrollment = courseService.enrollStudentInCourse(selectedStudent, course);
                
                // Refresh the grid
                studentsGrid.setItems(courseService.getEnrollmentsForCourse(course));
                
                // Show success notification
                Notification.show("Student successfully enrolled", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
                // Close the dialog
                dialog.close();
            } catch (Exception ex) {
                Notification.show("Error enrolling student: " + ex.getMessage(), 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        buttons.add(addButton, cancelButton);
        buttons.setSpacing(true);
        
        content.add(buttons);
        dialog.add(content);
        dialog.open();
    }

    private void manageMaterials(Course course) {
        Dialog dialog = new Dialog();
        dialog.setWidth("800px");
        dialog.setHeight("600px");

        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setSpacing(true);
        content.setPadding(true);

        H3 title = new H3("Course Materials - " + course.getCourseName());
        content.add(title);

        // Upload form
        FormLayout uploadForm = new FormLayout();
        TextField titleField = new TextField("Title");
        titleField.setRequired(true);
        titleField.setWidthFull();

        TextArea descriptionField = new TextArea("Description");
        descriptionField.setWidthFull();

        Upload upload = new Upload();
        upload.setAcceptedFileTypes(".pdf", ".doc", ".docx", ".txt", ".ppt", ".pptx");
        MemoryBuffer buffer = new MemoryBuffer();
        upload.setReceiver(buffer);

        uploadForm.add(titleField, descriptionField, upload);
        content.add(uploadForm);

        // Materials grid
        Grid<CourseMaterial> materialsGrid = new Grid<>(CourseMaterial.class);
        materialsGrid.setHeight("300px");
        materialsGrid.setColumns("title", "description", "uploadDate", "fileName");

        // Add action column for download and delete
        materialsGrid.addComponentColumn(material -> {
            HorizontalLayout actions = new HorizontalLayout();

            Button downloadButton = new Button("Download", e -> {
                try {
                    byte[] fileData = courseMaterialService.getFileData(material.getMaterialId());
                    if (fileData == null) {
                        Notification.show("No file data available for this material",
                                3000, Notification.Position.TOP_CENTER)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                        return;
                    }

                    String fileName = material.getFileName() != null ? material.getFileName()
                            : material.getTitle() + ".pdf";

                    StreamResource resource = new StreamResource(fileName,
                            () -> new ByteArrayInputStream(fileData));

                    Anchor downloadLink = new Anchor(resource, "");
                    downloadLink.getElement().setAttribute("download", true);
                    downloadLink.setId("download-link-" + material.getMaterialId());
                    add(downloadLink);

                    UI.getCurrent().getPage().executeJs(
                            "document.getElementById('download-link-" + material.getMaterialId() + "').click(); " +
                                    "document.getElementById('download-link-" + material.getMaterialId()
                                    + "').remove();");
                } catch (Exception ex) {
                    Notification.show("Error downloading file: " + ex.getMessage(),
                            3000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });

            Button deleteButton = new Button("Delete", e -> {
                try {
                    courseMaterialService.deleteMaterial(material.getMaterialId());
                    updateMaterialsGrid(materialsGrid, course);
                    Notification.show("Material deleted successfully",
                            3000, Notification.Position.TOP_CENTER);
                } catch (Exception ex) {
                    Notification.show("Error deleting material: " + ex.getMessage(),
                            3000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });

            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
            actions.add(downloadButton, deleteButton);
            return actions;
        }).setHeader("Actions");

        // Handle file upload
        upload.addSucceededListener(event -> {
            try {
                String materialTitle = titleField.getValue();
                if (materialTitle.isEmpty()) {
                    Notification.show("Please enter a title for the material",
                            3000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }

                // Create new material
                CourseMaterial material = new CourseMaterial();
                material.setTitle(materialTitle);
                material.setDescription(descriptionField.getValue());
                material.setCourse(course);
                material.setFileName(event.getFileName());
                material.setUploadedBy(securityService.getCurrentUser());

                // Read file data
                InputStream inputStream = buffer.getInputStream();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                inputStream.transferTo(outputStream);
                byte[] fileData = outputStream.toByteArray();

                // Save material and file data
                courseMaterialService.saveMaterialWithFile(material, fileData);

                // Clear form
                titleField.clear();
                descriptionField.clear();
                upload.clearFileList();

                // Update grid
                updateMaterialsGrid(materialsGrid, course);

                Notification.show("Material uploaded successfully",
                        3000, Notification.Position.TOP_CENTER);

            } catch (Exception e) {
                Notification.show("Error uploading file: " + e.getMessage(),
                        3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        content.add(materialsGrid);
        updateMaterialsGrid(materialsGrid, course);

        Button closeButton = new Button("Close", e -> dialog.close());
        content.add(closeButton);

        dialog.add(content);
        dialog.open();
    }

    private void updateMaterialsGrid(Grid<CourseMaterial> grid, Course course) {
        List<CourseMaterial> materials = courseMaterialService.getMaterialsByCourse(course);
        grid.setItems(materials);
    }

    private void generateReport(Course course) {
        try {
            // Get fresh course data
            Course freshCourse = courseService.getCourseById(course.getId())
                    .orElseThrow(() -> new RuntimeException("Course not found"));

            // Get enrollments
            List<Enrollment> enrollments = courseService.getEnrollmentsForCourse(freshCourse);

            // Generate PDF
            byte[] pdfContent = reportService.generateCourseReport(freshCourse, enrollments);

            // Create download resource
            String fileName = freshCourse.getCourseCode() + "_report.pdf";
            StreamResource resource = new StreamResource(fileName,
                    () -> new ByteArrayInputStream(pdfContent));

            // Create download link
            Anchor downloadLink = new Anchor(resource, "");
            downloadLink.getElement().setAttribute("download", true);
            downloadLink.setId("download-link");
            add(downloadLink);

            // Trigger download
            UI.getCurrent().getPage().executeJs(
                    "document.getElementById('download-link').click(); " +
                            "document.getElementById('download-link').remove();");

            Notification.show("Report generated successfully!",
                    3000, Notification.Position.TOP_CENTER);

        } catch (Exception e) {
            String error = "Error generating report: " + e.getMessage();
            System.err.println(error);
            e.printStackTrace();
            Notification.show(error, 3000, Notification.Position.TOP_CENTER);
        }
    }
}