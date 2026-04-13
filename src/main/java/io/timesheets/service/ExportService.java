package io.timesheets.service;

import io.timesheets.model.TimeEntry;
import io.timesheets.model.WorkLocation;
import io.timesheets.util.HourFormatter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ReportService reportService;

    public ExportService(ReportService reportService) {
        this.reportService = reportService;
    }

    public String exportToCsv(LocalDate start, LocalDate end, Long projectId,
                              boolean capHomeOvertime, boolean includeTimes) throws IOException {
        var entries = reportService.getTimeEntriesForExport(start, end, projectId);

        var headers = new ArrayList<>(List.of("Datum", "Project", "Uren", "Pauze"));
        if (includeTimes) {
            headers.add("Begintijd");
            headers.add("Eindtijd");
        }
        headers.addAll(List.of("Factureerbaar", "Locatie", "Notities"));

        var writer = new StringWriter();
        var format = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                .setHeader(headers.toArray(String[]::new))
                .setDelimiter(';')
                .build();

        try (var printer = new CSVPrinter(writer, format)) {
            for (var entry : entries) {
                var hours = getEffectiveHours(entry, capHomeOvertime);
                var record = new ArrayList<>();
                record.add(entry.getEntryDate().format(DATE_FMT));
                record.add(entry.getProject().getName());
                record.add(HourFormatter.format(hours));
                record.add(HourFormatter.format(entry.getBreakDuration()));
                if (includeTimes) {
                    record.add(entry.getStartTime() != null ? entry.getStartTime().toString() : "");
                    record.add(entry.getEndTime() != null ? entry.getEndTime().toString() : "");
                }
                record.add(entry.getProject().isBillable() ? "Ja" : "Nee");
                record.add(entry.getWorkLocation().getDisplayName());
                record.add(entry.getNotes() != null ? entry.getNotes() : "");
                printer.printRecord(record);
            }
        }

        return writer.toString();
    }

    public String generatePdfHtml(LocalDate start, LocalDate end, Long projectId,
                                   boolean capHomeOvertime, boolean includeTimes) {
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
            sb.append("<table><thead><tr>");
            sb.append("<th>Datum</th><th>Project</th><th class=\"text-right\">Uren</th><th>Pauze</th>");
            if (includeTimes) {
                sb.append("<th>Begintijd</th><th>Eindtijd</th>");
            }
            sb.append("<th>Locatie</th><th>Notities</th>");
            sb.append("</tr></thead><tbody>\n");

            var totalColSpan = includeTimes ? 4 : 2;
            var totalHours = BigDecimal.ZERO;
            for (var entry : entries) {
                var hours = getEffectiveHours(entry, capHomeOvertime);
                totalHours = totalHours.add(hours);
                sb.append("<tr>");
                sb.append("<td>").append(entry.getEntryDate().format(DATE_FMT)).append("</td>");
                sb.append("<td>").append(escapeHtml(entry.getProject().getName())).append("</td>");
                sb.append("<td class=\"text-right\">").append(HourFormatter.format(hours)).append("</td>");
                sb.append("<td>").append(HourFormatter.format(entry.getBreakDuration())).append("</td>");
                if (includeTimes) {
                    sb.append("<td>").append(entry.getStartTime() != null ? entry.getStartTime().toString() : "").append("</td>");
                    sb.append("<td>").append(entry.getEndTime() != null ? entry.getEndTime().toString() : "").append("</td>");
                }
                sb.append("<td>").append(entry.getWorkLocation().getDisplayName()).append("</td>");
                sb.append("<td>").append(entry.getNotes() != null ? escapeHtml(entry.getNotes()) : "").append("</td>");
                sb.append("</tr>\n");
            }

            sb.append("<tr class=\"total-row\">");
            sb.append("<td colspan=\"2\">Totaal</td>");
            sb.append("<td class=\"text-right\">").append(HourFormatter.format(totalHours)).append("</td>");
            sb.append("<td colspan=\"").append(totalColSpan + 3).append("\"></td>");
            sb.append("</tr>");
            sb.append("</tbody></table>");
        } else {
            sb.append("<p>Geen uren gevonden in deze periode.</p>");
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    private BigDecimal getEffectiveHours(TimeEntry entry, boolean capHomeOvertime) {
        // Use gross hours (net + break)
        var breakDuration = entry.getBreakDuration() != null ? entry.getBreakDuration() : BigDecimal.ZERO;
        var hours = entry.getHoursWorked().add(breakDuration);
        if (capHomeOvertime && entry.getWorkLocation() == WorkLocation.HOME) {
            var target = entry.getProject().getDailyHourTarget();
            var cap = target != null ? target : new BigDecimal("8");
            if (hours.compareTo(cap) > 0) {
                return cap;
            }
        }
        return hours;
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
