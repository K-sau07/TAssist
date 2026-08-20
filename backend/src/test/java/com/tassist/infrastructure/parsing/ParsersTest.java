package com.tassist.infrastructure.parsing;

import com.tassist.domain.port.out.DocumentParser.ParsedSegment;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Each parser extracts text from a generated sample of its type (Step 4 acceptance). */
class ParsersTest {

    @Test
    void txt() {
        var segs = new TxtParser().parse("hello world".getBytes(StandardCharsets.UTF_8));
        assertThat(segs).hasSize(1);
        assertThat(segs.get(0).text()).contains("hello world");
    }

    @Test
    void markdown() {
        var segs = new MarkdownParser().parse("# Title\n\nSome **bold** text.".getBytes(StandardCharsets.UTF_8));
        assertThat(segs.get(0).text()).contains("Title").contains("bold");
    }

    @Test
    void csv() {
        String csv = "name,age\nAlice,30\nBob,25\n";
        var segs = new CsvParser().parse(csv.getBytes(StandardCharsets.UTF_8));
        assertThat(segs).hasSize(2);
        assertThat(segs.get(0).text()).contains("name=Alice").contains("age=30");
        assertThat(segs.get(0).metadata()).containsEntry("type", "csv");
    }

    @Test
    void pdf() throws Exception {
        byte[] bytes;
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(72, 700);
                cs.showText("Hello PDF content");
                cs.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            bytes = out.toByteArray();
        }
        List<ParsedSegment> segs = new PdfParser().parse(bytes);
        assertThat(segs).isNotEmpty();
        assertThat(segs.get(0).text()).contains("Hello PDF content");
        assertThat(segs.get(0).metadata()).containsEntry("page", "1");
    }

    @Test
    void docx() throws Exception {
        byte[] bytes;
        try (XWPFDocument doc = new XWPFDocument()) {
            doc.createParagraph().createRun().setText("First paragraph.");
            doc.createParagraph().createRun().setText("Second paragraph.");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            bytes = out.toByteArray();
        }
        var segs = new DocxParser().parse(bytes);
        assertThat(segs).hasSize(2);
        assertThat(segs.get(0).text()).contains("First paragraph");
    }

    @Test
    void pptx() throws Exception {
        byte[] bytes;
        try (XMLSlideShow ppt = new XMLSlideShow()) {
            XSLFSlide slide = ppt.createSlide();
            XSLFTextBox tb = slide.createTextBox();
            tb.setText("Slide one content");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ppt.write(out);
            bytes = out.toByteArray();
        }
        var segs = new PptxParser().parse(bytes);
        assertThat(segs).isNotEmpty();
        assertThat(segs.get(0).text()).contains("Slide one content");
        assertThat(segs.get(0).metadata()).containsEntry("slide", "1");
    }

    @Test
    void xlsx() throws Exception {
        byte[] bytes;
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            var sheet = wb.createSheet("Data");
            var r0 = sheet.createRow(0);
            r0.createCell(0).setCellValue("name");
            r0.createCell(1).setCellValue("age");
            var r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("Alice");
            r1.createCell(1).setCellValue(30);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            bytes = out.toByteArray();
        }
        var segs = new XlsxParser().parse(bytes);
        assertThat(segs).isNotEmpty();
        assertThat(segs.get(0).text()).contains("name");
        assertThat(segs.stream().anyMatch(s -> s.text().contains("Alice"))).isTrue();
    }
}
