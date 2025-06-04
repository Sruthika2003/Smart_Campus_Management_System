package com.smartcampus.examgrading.service;

import com.itextpdf.html2pdf.HtmlConverter;
import com.smartcampus.examgrading.model.Payment;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class ReceiptService {

    public byte[] generateReceiptPdf(Payment payment) {
        String html = generateReceiptHtml(payment);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(html, outputStream);
        return outputStream.toByteArray();
    }

    private String generateReceiptHtml(Payment payment) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; margin: 40px; }
                    .header { text-align: center; margin-bottom: 30px; }
                    .title { font-size: 24px; font-weight: bold; margin-bottom: 10px; }
                    .subtitle { font-size: 18px; color: #666; }
                    .section { margin: 20px 0; }
                    .section-title { font-size: 16px; font-weight: bold; margin-bottom: 10px; }
                    .row { display: flex; margin: 5px 0; }
                    .label { width: 200px; font-weight: bold; }
                    .value { flex: 1; }
                    .footer { margin-top: 40px; text-align: center; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="header">
                    <div class="title">SMART CAMPUS</div>
                    <div class="subtitle">Payment Receipt</div>
                </div>
                
                <div class="section">
                    <div class="section-title">Fee Details</div>
                    <div class="row">
                        <div class="label">Fee Type:</div>
                        <div class="value">%s</div>
                    </div>
                    <div class="row">
                        <div class="label">Description:</div>
                        <div class="value">%s</div>
                    </div>
                    <div class="row">
                        <div class="label">Amount:</div>
                        <div class="value">₹%s</div>
                    </div>
                    <div class="row">
                        <div class="label">Semester:</div>
                        <div class="value">%s</div>
                    </div>
                    <div class="row">
                        <div class="label">Academic Year:</div>
                        <div class="value">%s</div>
                    </div>
                </div>
                
                <div class="section">
                    <div class="section-title">Payment Information</div>
                    <div class="row">
                        <div class="label">Payment Date:</div>
                        <div class="value">%s</div>
                    </div>
                    <div class="row">
                        <div class="label">Payment Method:</div>
                        <div class="value">%s</div>
                    </div>
                    <div class="row">
                        <div class="label">Transaction Reference:</div>
                        <div class="value">%s</div>
                    </div>
                    <div class="row">
                        <div class="label">Receipt Number:</div>
                        <div class="value">%s</div>
                    </div>
                </div>
                
                <div class="footer">
                    <p>This is a computer-generated receipt. No signature required.</p>
                    <p>Thank you for your payment!</p>
                </div>
            </body>
            </html>
            """.formatted(
                payment.getStudentFee().getFeeType().getFeeName(),
                payment.getStudentFee().getFeeType().getDescription(),
                payment.getAmount(),
                payment.getStudentFee().getSemester(),
                payment.getStudentFee().getAcademicYear(),
                payment.getPaymentDate().toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                payment.getPaymentMethod(),
                payment.getTransactionReference(),
                payment.getReceiptNumber()
            );
    }
} 