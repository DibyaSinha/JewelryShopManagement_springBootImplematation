package org.example.util;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.example.model.Bill;
import org.example.model.BillItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

public class InvoiceUtil {
    private static final Logger logger = LoggerFactory.getLogger(InvoiceUtil.class);
    private static final String DIR = "C:/JewelryInvoices/";
    private static final DecimalFormat df = new DecimalFormat("0.00");

    public static byte[] generatePdf(Bill bill) {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Font Styles
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font titleFont1 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            // Title
            Paragraph title1 = new Paragraph("CHANDI FASHION - INVOICE", titleFont);
            Paragraph title2 = new Paragraph("BLESSINGS", titleFont1);

            title1.setAlignment(Element.ALIGN_CENTER);
            title1.setSpacingAfter(5);
            document.add(title1);
            title2.setAlignment(Element.ALIGN_CENTER);
            title2.setSpacingAfter(20);
            document.add(title2);

            // Bill Info
            document.add(new Paragraph("Bill ID : " + bill.getBillId(), headerFont));
            document.add(new Paragraph("Date    : " + bill.getBillDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")), normalFont));
            if (bill.getCustomerMobile() != null && !bill.getCustomerMobile().isEmpty()) {
                document.add(new Paragraph("Customer: " + bill.getCustomerMobile(), normalFont));
            }
            document.add(new Paragraph(" ", normalFont)); // Spacer

            // Table
            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3, 2, 2, 2, 2, 1, 3});

            // Table Headers
            addTableHeader(table, "Item");
            addTableHeader(table, "Metal");
            addTableHeader(table, "Wt(g)");
            addTableHeader(table, "Rate/g");
            addTableHeader(table, "Making");
            addTableHeader(table, "Qty");
            addTableHeader(table, "Amount");

            // Table Rows
            for (BillItem i : bill.getItems()) {
                table.addCell(new Phrase(i.getJewelry().getName(), normalFont));
                table.addCell(new Phrase(i.getJewelry().getType(), normalFont));
                table.addCell(new Phrase(df.format(i.getJewelry().getWeight()), normalFont));
                table.addCell(new Phrase(df.format(i.getRateUsed()), normalFont));
                table.addCell(new Phrase(df.format(i.getMakingCharge()), normalFont));
                table.addCell(new Phrase(String.valueOf(i.getQuantity()), normalFont));
                table.addCell(new Phrase(df.format(i.totalBeforeGST()), normalFont));
            }

            document.add(table);

            // Totals
            Paragraph totals = new Paragraph();
            totals.setSpacingBefore(20);
            totals.setAlignment(Element.ALIGN_RIGHT);
            totals.add(new Chunk("Subtotal: " + df.format(bill.getTotalAmount()) + "\n", normalFont));
            if (bill.getDiscountAmount() > 0) {
                totals.add(new Chunk("Discount: -" + df.format(bill.getDiscountAmount()) + "\n", normalFont));
            }
            totals.add(new Chunk("GST (3%): " + df.format(bill.getGstAmount()) + "\n", normalFont));
            totals.add(new Chunk("GRAND TOTAL: " + df.format(bill.getGrandTotal()), headerFont));
            document.add(totals);

            document.close();

            byte[] pdfBytes = baos.toByteArray();

            // Save locally
            saveLocally(bill.getBillId(), pdfBytes);

            return pdfBytes;

        } catch (Exception e) {
            logger.error("Error generating PDF for bill ID: {}", bill.getBillId(), e);
            return null;
        }
    }

    private static void addTableHeader(PdfPTable table, String title) {
        PdfPCell header = new PdfPCell();
        header.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
        header.setBorderWidth(1);
        header.setPhrase(new Phrase(title, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        header.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(header);
    }

    private static void saveLocally(long billId, byte[] data) {
        try {
            new File(DIR).mkdirs();
            String filePath = DIR + "invoice_" + billId + ".pdf";
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(data);
            }
            logger.info("PDF Invoice saved locally at: {}", filePath);
        } catch (Exception e) {
            logger.error("Failed to save PDF locally", e);
        }
    }
}
