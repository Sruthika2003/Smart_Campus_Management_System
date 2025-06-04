package com.smartcampus.examgrading.view.faculty;

import com.smartcampus.examgrading.model.Exam;
import com.smartcampus.examgrading.model.ExamPaper;
import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.service.ExamService;
import com.smartcampus.examgrading.service.SessionService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.VaadinSession;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Route("exam-papers")
public class ExamPaperView extends VerticalLayout implements BeforeEnterObserver {

    private final ExamService examService;
    private final SessionService sessionService;

    private Select<Exam> examSelect = new Select<>();
    private MemoryBuffer buffer = new MemoryBuffer();
    private Upload upload = new Upload(buffer);
    private Grid<ExamPaper> paperGrid = new Grid<>(ExamPaper.class, false);

    private User currentUser;
    private Exam selectedExam;

    private String determineContentType(String fileName) {
        String lowerCaseFileName = fileName.toLowerCase();
        if (lowerCaseFileName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lowerCaseFileName.endsWith(".doc")) {
            return "application/msword";
        } else if (lowerCaseFileName.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        return "application/octet-stream"; // Default binary type
    }

    public ExamPaperView(ExamService examService, SessionService sessionService) {
        this.examService = examService;
        this.sessionService = sessionService;

        // For demo purposes, we'll set a mock faculty user
        // In a real application, this would come from a login process
        this.currentUser = sessionService.getCurrentUser();

        // Check if user is logged in and is faculty
        if (currentUser == null || !sessionService.isFaculty()) {
            add(new H2("Access Denied"),
                    new Paragraph("Only faculty members can access this page."));
            return;
        }

        // Set up the UI components
        add(new H2("Exam Paper Upload"));

        // Exam selector
        examSelect.setLabel("Select Exam");
        examSelect.setItemLabelGenerator(exam -> exam.getExamName() + " (" + exam.getExamType() + ")");
        examSelect.addValueChangeListener(e -> {
            selectedExam = e.getValue();
            updatePaperGrid();
        });

        // Configure upload component
        upload.setMaxFiles(1);
        upload.setAcceptedFileTypes("application/pdf", ".pdf", "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                ".doc", ".docx");
        upload.setMaxFileSize(10 * 1024 * 1024); // 10MB limit

        Button uploadButton = new Button("Upload Paper");
        upload.setUploadButton(uploadButton);

        upload.addSucceededListener(event -> {
            try {
                if (selectedExam != null) {
                    examService.uploadExamPaper(
                            selectedExam.getId(),
                            currentUser.getUserId(),
                            buffer.getInputStream(),
                            event.getFileName());
                    Notification.show("Exam paper uploaded successfully!", 3000, Notification.Position.BOTTOM_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    updatePaperGrid();
                }
            } catch (Exception e) {
                Notification.show("Upload failed: " + e.getMessage(), 3000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        upload.addFailedListener(event -> {
            Notification.show("Upload failed: " + event.getReason(), 3000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        });

        // Configure grid for displaying uploaded papers
        configureGrid();

        // Add components to layout
        add(examSelect, new HorizontalLayout(upload), paperGrid);

        // Initialize data
        updateExamSelect();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // This would normally check if user is logged in and redirect if not
        if (currentUser == null) {
            // Redirect to login page
            // event.forwardTo(LoginView.class);
        }
    }

    private void updateExamSelect() {
        List<Exam> exams = examService.getAllExams();
        examSelect.setItems(exams);
        if (!exams.isEmpty()) {
            examSelect.setValue(exams.get(0));
        }
    }

    private void configureGrid() {
        paperGrid.addColumn(ExamPaper::getFileName).setHeader("File Name").setAutoWidth(true);
        paperGrid.addColumn(paper -> paper.getUploadDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .setHeader("Upload Date").setAutoWidth(true);

        // Add download button
        paperGrid.addComponentColumn(paper -> {
            Button downloadBtn = new Button("Download", new Icon(VaadinIcon.DOWNLOAD));
            downloadBtn.addClickListener(e -> {
                try {
                    byte[] fileData = Files.readAllBytes(Paths.get(paper.getFilePath()));
                    String contentType = determineContentType(paper.getFileName());

                    StreamResource resource = new StreamResource(paper.getFileName(),
                            () -> new ByteArrayInputStream(fileData));
                    resource.setContentType(contentType);

                    // Create anchor with download attribute
                    Anchor anchor = new Anchor(resource, "");
                    anchor.getElement().setAttribute("download", true);
                    add(anchor);

                    // Click and remove
                    anchor.getElement().executeJs("this.click(); this.remove();");
                } catch (IOException ex) {
                    Notification.show("Failed to download file: " + ex.getMessage(),
                            3000, Notification.Position.BOTTOM_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            return downloadBtn;
        }).setHeader("Download").setWidth("120px").setFlexGrow(0);

        // Add delete button (only for papers uploaded by current user)
        paperGrid.addComponentColumn(paper -> {
            Button deleteBtn = new Button(new Icon(VaadinIcon.TRASH));
            deleteBtn.setEnabled(paper.getUploadedBy().equals(currentUser.getUserId()));

            deleteBtn.addClickListener(e -> {
                try {
                    examService.deleteExamPaper(paper.getId(), currentUser.getUserId());
                    updatePaperGrid();
                    Notification.show("Exam paper deleted successfully!",
                            3000, Notification.Position.BOTTOM_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } catch (Exception ex) {
                    Notification.show("Failed to delete: " + ex.getMessage(),
                            3000, Notification.Position.BOTTOM_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            return deleteBtn;
        }).setHeader("Delete").setWidth("100px").setFlexGrow(0);
    }

    private void updatePaperGrid() {
        if (selectedExam != null) {
            List<ExamPaper> papers = examService.getExamPapersByExamId(selectedExam.getId());
            paperGrid.setItems(papers);
        } else {
            paperGrid.setItems();
        }
    }
}