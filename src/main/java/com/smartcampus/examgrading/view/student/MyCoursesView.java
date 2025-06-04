package com.smartcampus.examgrading.view.student;

import com.smartcampus.examgrading.model.Course;
import com.smartcampus.examgrading.model.CourseMaterial;
import com.smartcampus.examgrading.model.Enrollment;
import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.security.SecurityService;
import com.smartcampus.examgrading.service.CourseService;
import com.smartcampus.examgrading.service.CourseMaterialService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Route(value = "student/my-courses", layout = StudentView.class)
@PageTitle("My Courses | Course Management System")
public class MyCoursesView extends VerticalLayout {
    private static final Logger logger = LoggerFactory.getLogger(MyCoursesView.class);
    private final Grid<Enrollment> grid = new Grid<>(Enrollment.class);
    private final CourseService courseService;
    private final SecurityService securityService;
    private final CourseMaterialService courseMaterialService;

    public MyCoursesView(CourseService courseService, SecurityService securityService,
            CourseMaterialService courseMaterialService) {
        this.courseService = courseService;
        this.securityService = securityService;
        this.courseMaterialService = courseMaterialService;

        try {
            if (!securityService.isLoggedIn() || !securityService.hasRole(User.Role.STUDENT)) {
                getUI().ifPresent(ui -> ui.navigate(""));
                return;
            }

            setSizeFull();
            setSpacing(true);
            setPadding(true);

            add(new H2("My Courses"));
            add(new Paragraph("View your enrolled courses, drop courses, or view course content"));

            configureGrid();
            updateGrid();

            add(grid);
        } catch (Exception e) {
            logger.error("Error initializing MyCoursesView", e);
            showNotification("Error initializing view: " + e.getMessage(),
                    NotificationVariant.LUMO_ERROR);
        }
    }

    private void configureGrid() {
        try {
            grid.setSizeFull();
            grid.setColumns();

            grid.addColumn(enrollment -> {
                try {
                    return enrollment.getCourse().getCourseCode();
                } catch (Exception e) {
                    logger.error("Error getting course code", e);
                    return "Error";
                }
            })
                    .setHeader("Course Code")
                    .setSortable(true);

            grid.addColumn(enrollment -> {
                try {
                    return enrollment.getCourse().getCourseName();
                } catch (Exception e) {
                    logger.error("Error getting course name", e);
                    return "Error";
                }
            })
                    .setHeader("Title")
                    .setSortable(true);

            grid.addColumn(enrollment -> {
                try {
                    return enrollment.getCourse().getCreditHours();
                } catch (Exception e) {
                    logger.error("Error getting credit hours", e);
                    return 0;
                }
            })
                    .setHeader("Credits")
                    .setSortable(true);

            grid.addColumn(enrollment -> {
                try {
                    Course course = enrollment.getCourse();
                    if (course != null && course.getFaculty() != null) {
                        return course.getFaculty().getFirstName() + " " +
                                course.getFaculty().getLastName();
                    }
                    return "Not Assigned";
                } catch (Exception e) {
                    logger.error("Error getting faculty name", e);
                    return "Error";
                }
            })
                    .setHeader("Faculty")
                    .setSortable(true);

            grid.addColumn(enrollment -> {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
                    return enrollment.getEnrollmentDate().format(formatter);
                } catch (Exception e) {
                    logger.error("Error formatting enrollment date", e);
                    return "Error";
                }
            })
                    .setHeader("Enrollment Date")
                    .setSortable(true);

            grid.addComponentColumn(enrollment -> {
                HorizontalLayout layout = new HorizontalLayout();

                Button viewButton = new Button("View Content", e -> viewCourseContent(enrollment.getCourse()));
                Button dropButton = new Button("Drop Course", e -> dropCourse(enrollment));

                layout.add(viewButton, dropButton);
                return layout;
            }).setHeader("Actions");

            grid.getColumns().forEach(col -> col.setAutoWidth(true));
        } catch (Exception e) {
            logger.error("Error configuring grid", e);
            showNotification("Error configuring course list: " + e.getMessage(),
                    NotificationVariant.LUMO_ERROR);
        }
    }

    private void updateGrid() {
        try {
            List<Enrollment> enrollments = courseService.getStudentEnrollments(securityService.getCurrentUser());
            logger.info("Found {} enrolled courses", enrollments.size());
            grid.setItems(enrollments);
        } catch (Exception e) {
            logger.error("Error loading enrollments", e);
            showNotification("Error loading your courses: " + e.getMessage(),
                    NotificationVariant.LUMO_ERROR);
            grid.setItems(Collections.emptyList());
        }
    }

    private void viewCourseContent(Course course) {
        try {
            // Fetch course with materials
            Course courseWithMaterials = courseService.getCourseWithMaterials(course.getCourseId());

            Dialog dialog = new Dialog();
            dialog.setWidth("800px");

            VerticalLayout content = new VerticalLayout();
            content.add(new H3(courseWithMaterials.getCourseName()));
            content.add(new Paragraph(courseWithMaterials.getCourseDescription()));

            if (courseWithMaterials.getMaterials() != null && !courseWithMaterials.getMaterials().isEmpty()) {
                Grid<CourseMaterial> materialsGrid = new Grid<>(CourseMaterial.class);
                materialsGrid.setColumns(); // Clear default columns

                // Add custom columns
                materialsGrid.addColumn(CourseMaterial::getTitle).setHeader("Title");
                materialsGrid.addColumn(CourseMaterial::getDescription).setHeader("Description");
                materialsGrid.addColumn(material -> {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    return material.getUploadDate().format(formatter);
                }).setHeader("Upload Date");

                // Add download button column
                materialsGrid.addComponentColumn(material -> {
                    Button downloadButton = new Button("Download");
                    downloadButton.addClickListener(e -> {
                        try {
                            String fileName = material.getFileName() != null ? material.getFileName()
                                    : material.getTitle() + ".pdf";

                            StreamResource resource = new StreamResource(fileName,
                                    () -> new ByteArrayInputStream(
                                            courseMaterialService.getFileData(material.getMaterialId())));

                            Anchor downloadLink = new Anchor(resource, "");
                            downloadLink.getElement().setAttribute("download", true);
                            downloadLink.setId("download-link-" + material.getMaterialId());
                            add(downloadLink);

                            UI.getCurrent().getPage().executeJs(
                                    "document.getElementById('download-link-" + material.getMaterialId()
                                            + "').click(); " +
                                            "document.getElementById('download-link-" + material.getMaterialId()
                                            + "').remove();");
                        } catch (Exception ex) {
                            showNotification("Error downloading file: " + ex.getMessage(),
                                    NotificationVariant.LUMO_ERROR);
                        }
                    });
                    return downloadButton;
                }).setHeader("Action");

                materialsGrid.setItems(courseWithMaterials.getMaterials());
                content.add(new H4("Course Materials"), materialsGrid);
            } else {
                content.add(new Paragraph("No materials available yet."));
            }

            Button closeButton = new Button("Close", e -> dialog.close());
            content.add(closeButton);

            dialog.add(content);
            dialog.open();
        } catch (Exception e) {
            logger.error("Error viewing course content", e);
            showNotification("Error viewing course content: " + e.getMessage(),
                    NotificationVariant.LUMO_ERROR);
        }
    }

    private void dropCourse(Enrollment enrollment) {
        try {
            courseService.dropCourse(securityService.getCurrentUser(), enrollment.getCourse());
            showNotification("Course dropped successfully", NotificationVariant.LUMO_SUCCESS);
            updateGrid();
        } catch (Exception e) {
            logger.error("Error dropping course", e);
            showNotification("Error dropping course: " + e.getMessage(),
                    NotificationVariant.LUMO_ERROR);
        }
    }

    private void showNotification(String message, NotificationVariant variant) {
        getUI().ifPresent(ui -> ui.access(() -> {
            Notification notification = Notification.show(message, 3000, Notification.Position.MIDDLE);
            notification.addThemeVariants(variant);
        }));
    }
}