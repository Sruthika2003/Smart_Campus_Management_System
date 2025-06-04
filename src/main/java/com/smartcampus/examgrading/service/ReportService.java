package com.smartcampus.examgrading.service;

import com.smartcampus.examgrading.model.Course;
import com.smartcampus.examgrading.model.Enrollment;
import com.smartcampus.examgrading.model.AttendanceReport;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.time.Month;

@Service
public class ReportService {

    public byte[] generateCourseReport(Course course, List<Enrollment> enrollments) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Add title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph title = new Paragraph("Course Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));

            // Course details
            document.add(new Paragraph("Course Details:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
            document.add(new Paragraph("Code: " + course.getCourseCode()));
            document.add(new Paragraph("Name: " + course.getCourseName()));
            document.add(new Paragraph("Description: " + course.getContent()));
            document.add(new Paragraph("Capacity: " + course.getCapacity()));
            document.add(new Paragraph("\n"));

            // Enrollment statistics
            int activeEnrollments = (int) enrollments.stream()
                    .filter(Enrollment::isActive)
                    .count();

            document.add(new Paragraph("Enrollment Statistics:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
            document.add(new Paragraph("Total Enrollments: " + enrollments.size()));
            document.add(new Paragraph("Active Enrollments: " + activeEnrollments));
            document.add(new Paragraph("Available Seats: " + course.getAvailableSeats()));

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF report", e);
        }
    }
    
    public byte[] generateAttendanceReportPdf(List<AttendanceReport> reports, Integer month, Integer year) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate()); // Landscape orientation

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Add title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            String period = Month.of(month).toString() + " " + year;
            Paragraph title = new Paragraph("Attendance Report - " + period, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));

            // Create table
            PdfPTable table = new PdfPTable(8); // 8 columns
            table.setWidthPercentage(100);
            
            // Set column widths
            float[] columnWidths = {3f, 2.5f, 2f, 1.5f, 1.5f, 1.5f, 1.5f, 2f};
            table.setWidths(columnWidths);
            
            // Add table headers
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            String[] headers = {"Student Name", "Course", "Total Classes", 
                              "Present", "Absent", "Late", "Excused", "Attendance %"};
            
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(5);
                cell.setBackgroundColor(new java.awt.Color(220, 220, 220));
                table.addCell(cell);
            }
            
            // Add data rows
            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Font lowAttendanceFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, java.awt.Color.RED);
            
            for (AttendanceReport report : reports) {
                // Student name
                table.addCell(new Phrase(
                    report.getStudent().getFirstName() + " " + report.getStudent().getLastName(), 
                    dataFont));
                
                // Course
                table.addCell(new Phrase(report.getCourse().getCourseName(), dataFont));
                
                // Total Classes
                table.addCell(new Phrase(report.getTotalClasses().toString(), dataFont));
                
                // Present
                table.addCell(new Phrase(report.getPresentCount().toString(), dataFont));
                
                // Absent
                table.addCell(new Phrase(report.getAbsentCount().toString(), dataFont));
                
                // Late
                table.addCell(new Phrase(report.getLateCount().toString(), dataFont));
                
                // Excused
                table.addCell(new Phrase(report.getExcusedCount().toString(), dataFont));
                
                // Attendance percentage
                Font percentageFont = report.getAttendancePercentage().compareTo(new java.math.BigDecimal("75.00")) < 0 
                    ? lowAttendanceFont : dataFont;
                
                table.addCell(new Phrase(report.getAttendancePercentage() + "%", percentageFont));
            }
            
            document.add(table);
            
            // Add footer with date
            document.add(new Paragraph("\n"));
            Paragraph footer = new Paragraph("Generated on: " + java.time.LocalDate.now().toString(), 
                FontFactory.getFont(FontFactory.HELVETICA, 8, Font.ITALIC));
            footer.setAlignment(Element.ALIGN_RIGHT);
            document.add(footer);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generating attendance PDF report", e);
        }
    }
}