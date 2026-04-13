package be.axxes.timesheets.controller;

import be.axxes.timesheets.service.ExportService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/export")
public class ExportController {

    private static final DateTimeFormatter FILE_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/csv")
    public ResponseEntity<byte[]> exportCsv(@RequestParam LocalDate start,
                                             @RequestParam LocalDate end,
                                             @RequestParam(required = false) Long projectId,
                                             @RequestParam(required = false, defaultValue = "false") boolean capHomeOvertime,
                                             @RequestParam(required = false, defaultValue = "false") boolean includeTimes) throws Exception {
        var csv = exportService.exportToCsv(start, end, projectId, capHomeOvertime, includeTimes);
        var filename = "timesheets_" + start.format(FILE_DATE_FMT) + "_" + end.format(FILE_DATE_FMT) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> exportPdf(@RequestParam LocalDate start,
                                             @RequestParam LocalDate end,
                                             @RequestParam(required = false) Long projectId,
                                             @RequestParam(required = false, defaultValue = "false") boolean capHomeOvertime,
                                             @RequestParam(required = false, defaultValue = "false") boolean includeTimes) throws Exception {
        var html = exportService.generatePdfHtml(start, end, projectId, capHomeOvertime, includeTimes);
        var filename = "timesheets_" + start.format(FILE_DATE_FMT) + "_" + end.format(FILE_DATE_FMT) + ".pdf";

        var outputStream = new ByteArrayOutputStream();
        var builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.withHtmlContent(html, null);
        builder.toStream(outputStream);
        builder.run();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(outputStream.toByteArray());
    }
}
