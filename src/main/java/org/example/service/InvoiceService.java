package org.example.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.example.entity.Bill;
import org.example.entity.BillItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.net.URL;

@Service
public class InvoiceService {
    private static final Logger logger = LoggerFactory.getLogger(InvoiceService.class);

    public byte[] generatePdf(Bill bill) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 54, 36);
            PdfWriter writer = PdfWriter.getInstance(document, baos);

            // Add Page Event for Border and Watermark
            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter writer, Document document) {
                    PdfContentByte canvas = writer.getDirectContentUnder();
                    
                    // 1. Golden Border
                    java.awt.Color goldColor = java.awt.Color.decode("#D4AF37");
                    canvas.saveState();
                    canvas.setColorStroke(goldColor);
                    canvas.setLineWidth(3f);
                    canvas.rectangle(
                            20, // x
                            20, // y
                            document.getPageSize().getWidth() - 40, // width
                            document.getPageSize().getHeight() - 40 // height
                    );
                    canvas.stroke();
                    canvas.restoreState();

                    // 2. Watermark (55% opacity)
                    try {
                        // Load watermark from static/images/CF_LOGO.png
                        ClassPathResource resource = new ClassPathResource("static/images/CF_LOGO.png");

                        Image watermark = Image.getInstance(
                                resource.getInputStream().readAllBytes()
                        );

                        // Center and scale watermark
                        watermark.scaleToFit(400, 400);

                        float x = (document.getPageSize().getWidth() - watermark.getScaledWidth()) / 2;
                        float y = (document.getPageSize().getHeight() - watermark.getScaledHeight()) / 2;

                        watermark.setAbsolutePosition(x, y);

                        PdfGState gs = new PdfGState();
                        gs.setFillOpacity(0.30f);

                        canvas.saveState();
                        canvas.setGState(gs);
                        canvas.addImage(watermark);
                        canvas.restoreState();

                    } catch (Exception e) {
                        logger.warn("Could not load watermark image", e);
                    }
                }
            });

            document.open();

            // 3. Logo at the top
            try {
                ClassPathResource resource = new ClassPathResource("static/images/logo.png");

                Image logo = Image.getInstance(
                        resource.getInputStream().readAllBytes()
                );

                logo.scaleToFit(250, 250);
                logo.setAlignment(Element.ALIGN_CENTER);

                document.add(logo);

            } catch (Exception e) {
                logger.warn("Could not load logo image", e);
            }

            //Front
            BaseFont bf = BaseFont.createFont(
                    "static/images/edwardianscriptitc.ttf",
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED
            );

            Font titleFont = new Font(
                    bf,
                    24,
                    Font.NORMAL,
                    java.awt.Color.decode("#D4AF37")
            );

            Paragraph title = new Paragraph("Blessings", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10f);

            document.add(title);


//            document.add(new Paragraph("Jewelry Shop Management System"));
            document.add(new Paragraph("Bill ID: " + bill.getId()));
            document.add(new Paragraph("Date: " + bill.getBillDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))));
            document.add(new Paragraph("Seller ID: " + bill.getSeller().getId()));

            if (bill.getCustomer() != null) {
                document.add(new Paragraph("Customer: " + bill.getCustomer().getName() + " (" + bill.getCustomer().getMobileNumber() + ")"));
            }
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setSpacingBefore(15f);

            String[] headers = {"Item", "Qty", "Rate", "Making", "Total"};
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(java.awt.Color.decode("#f4f4f4"));
                cell.setPadding(8f);
                table.addCell(cell);
            }

            for (BillItem item : bill.getItems()) {
                PdfPCell nameCell = new PdfPCell(new Phrase(item.getJewelry().getName()));
                nameCell.setPadding(5f);
                table.addCell(nameCell);
                
                PdfPCell qtyCell = new PdfPCell(new Phrase(String.valueOf(item.getQuantity())));
                qtyCell.setPadding(5f);
                table.addCell(qtyCell);
                
                PdfPCell rateCell = new PdfPCell(new Phrase("Rs." + item.getRateAtTime()));
                rateCell.setPadding(5f);
                table.addCell(rateCell);
                
                PdfPCell makingCell = new PdfPCell(new Phrase("Rs." + item.getMakingCharge()));
                makingCell.setPadding(5f);
                table.addCell(makingCell);
                
                PdfPCell totalCell = new PdfPCell(new Phrase("Rs." + item.getTotalAmount()));
                totalCell.setPadding(5f);
                table.addCell(totalCell);
            }
            document.add(table);

            document.add(new Paragraph(" "));
            
            Font totalsFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            
            Paragraph subtotal = new Paragraph("Subtotal: Rs." + String.format("%.2f", bill.getTotalAmount()), totalsFont);
            subtotal.setAlignment(Element.ALIGN_RIGHT);
            document.add(subtotal);
            
            if (bill.getDiscountAmount() > 0) {
                Paragraph discount = new Paragraph("Discount: -Rs." + String.format("%.2f", bill.getDiscountAmount()), totalsFont);
                discount.setAlignment(Element.ALIGN_RIGHT);
                document.add(discount);
            }
            
            Paragraph gst = new Paragraph("GST (3%): Rs." + String.format("%.2f", bill.getGstAmount()), totalsFont);
            gst.setAlignment(Element.ALIGN_RIGHT);
            document.add(gst);
            
            Paragraph grandTotal = new Paragraph("Grand Total: Rs." + String.format("%.2f", bill.getGrandTotal()), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, java.awt.Color.decode("#D4AF37")));
            grandTotal.setAlignment(Element.ALIGN_RIGHT);
            document.add(grandTotal);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            logger.error("Error generating PDF for Bill ID: {}", bill.getId(), e);
            return null;
        }
    }
}