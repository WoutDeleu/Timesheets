package be.axxes.timesheets.service;

import be.axxes.timesheets.model.TimeEntry;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ReportService reportService;

    public ExportService(ReportService reportService) {
        this.reportService = reportService;
    }

    public String exportToCsv(LocalDate start, LocalDate end, Long projectId) throws IOException {
        var entries = reportService.getTimeEntriesForExport(start, end, projectId);

        var writer = new StringWriter();
        var format = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                .setHeader("Datum", "Project", "Uren", "Begintijd", "Eindtijd", "Factureerbaar", "Locatie", "Notities")
                .setDelimiter(';')
                .build();

        try (var printer = new CSVPrinter(writer, format)) {
            for (var entry : entries) {
                printer.printRecord(
                        entry.getEntryDate().format(DATE_FMT),
                        entry.getProject().getName(),
                        entry.getHoursWorked().toPlainString().replace(".", ","),
                        entry.getStartTime() != null ? entry.getStartTime().toString() : "",
                        entry.getEndTime() != null ? entry.getEndTime().toString() : "",
                        entry.getProject().isBillable() ? "Ja" : "Nee",
                        entry.getWorkLocation().getDisplayName(),
                        entry.getNotes() != null ? entry.getNotes() : ""
                );
            }
        }

        return writer.toString();
    }

    public String generatePdfHtml(LocalDate start, LocalDate end, Long projectId) {
        var entries = reportService.getTimeEntriesForExport(start, end, projectId);

        var sb = new StringBuilder();
        sb.append("""
                <html>
                <head>
                <style>
                    body { font-family: Arial, sans-serif; font-size: 11px; margin: 20px; }
                    h1 { font-size: 18px; color: #333; }
                    h2 { font-size: 14px; color: #666; margin-top: 20px; }
                    table { width: 100%%; border-collapse: collapse; margin-top: 10px; }
                    th, td { border: 1px solid #ddd; padding: 6px 8px; text-align: left; }
                    th { background-color: #f5f5f5; font-weight: bold; }
                    .text-right { text-align: right; }
                    .total-row { background-color: #f0f0f0; font-weight: bold; }
                </style>
                </head>
                <body>
                """);

        sb.append("<h1>Urenrapport</h1>");
        sb.append("<p>Periode: ").append(start.format(DATE_FMT))
          .append(" - ").append(end.format(DATE_FMT)).append("</p>");

        if (!entries.isEmpty()) {
            sb.append("""
                    <table>
                    <thead>
                    <tr>
                        <th>Datum</th>
                        <th>Project</th>
                        <th class="text-right">Uren</th>
                        <th>Begintijd</th>
                        <th>Eindtijd</th>
                        <th>Locatie</th>
                        <th>Notities</th>
                    </tr>
                    </thead>
                    <tbody>
                    """);

            var totalHours = java.math.BigDecimal.ZERO;
            for (var entry : entries) {
                totalHours = totalHours.add(entry.getHoursWorked());
                sb.append("<tr>");
                sb.append("<td>").append(entry.getEntryDate().format(DATE_FMT)).append("</td>");
                sb.append("<td>").append(escapeHtml(entry.getProject().getName())).append("</td>");
                sb.append("<td class=\"text-right\">").append(entry.getHoursWorked().toPlainString().replace(".", ",")).append("</td>");
                sb.append("<td>").append(entry.getStartTime() != null ? entry.getStartTime().toString() : "").append("</td>");
                sb.append("<td>").append(entry.getEndTime() != null ? entry.getEndTime().toString() : "").append("</td>");
                sb.append("<td>").append(entry.getWorkLocation().getDisplayName()).append("</td>");
                sb.append("<td>").append(entry.getNotes() != null ? escapeHtml(entry.getNotes()) : "").append("</td>");
                sb.append("</tr>\n");
            }

            sb.append("<tr class=\"total-row\">");
            sb.append("<td colspan=\"2\">Totaal</td>");
            sb.append("<td class=\"text-right\">").append(totalHours.toPlainString().replace(".", ",")).append("</td>");
            sb.append("<td colspan=\"4\"></td>");
            sb.append("</tr>");
            sb.append("</tbody></table>");
        } else {
            sb.append("<p>Geen uren gevonden in deze periode.</p>");
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
