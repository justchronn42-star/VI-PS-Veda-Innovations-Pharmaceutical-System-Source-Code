package com.vips.pharma.report;

import com.vips.pharma.model.CartItem;
import com.vips.pharma.model.Medicine;
import com.vips.pharma.model.Receipt;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRXmlDataSource;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.*;

import java.io.File;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ════════════════════════════════════════════════════════════════════
 *  REPORT GENERATOR  —  V3  (XML-pipeline architecture)
 *
 *  Data flow for every report:
 *
 *    1. DAO fetches data from SQLite (already done by the caller)
 *    2. XmlDataWriter converts Java model objects → XML file on disk
 *    3. ReportGenerator loads the .jrxml template from the classpath
 *    4. JasperCompileManager compiles .jrxml → JasperReport (in-memory)
 *    5. JRXmlDataSource reads the XML file using XPath
 *    6. JasperFillManager fills the compiled report with the XML data
 *    7. JRPdfExporter writes the final PDF to reports/
 *
 *  No raw SQL or DAO calls exist in this class.
 *  No programmatic JasperDesign construction.
 *  All layout lives in the .jrxml files under resources/jrxml/.
 * ════════════════════════════════════════════════════════════════════
 */
public class ReportGenerator {

    private static final String OUTPUT_DIR = "reports/";
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /** Candidates checked in order; returns the first logo file found, or empty string. */
    private static String resolveLogoPath() {
        for (String name : new String[]{"logo.png", "logo.jpg", "logo.jpeg", "logo.gif"}) {
            File f = new File(name);
            if (f.exists() && f.isFile()) {
                return f.getAbsolutePath();
            }
        }
        return "";
    }

    static {
        new File(OUTPUT_DIR).mkdirs();
    }

    // ─────────────────────────────────────────────────────────────────
    //  PUBLIC API
    // ─────────────────────────────────────────────────────────────────

    /**
     * Generates the full POS receipt PDF.
     *
     * Pipeline:
     *   Java CartItem list → XmlDataWriter → receipt.xml
     *   receipt.jrxml + receipt.xml → JasperReports → receipt_<ts>.pdf
     *
     * @param receipt   the saved receipt (needs id, dateTime, processedBy, total)
     * @param cartItems the individual line items sold
     * @return absolute path to the generated PDF
     */
    public static String generateReceipt(Receipt receipt, List<CartItem> cartItems)
            throws Exception {

        // Step 1 – write XML from Java objects
        String xmlPath = XmlDataWriter.writeReceiptXml(receipt, cartItems);

        // Step 2 – parameters extracted from the XML header node
        Map<String, Object> params = new HashMap<>();
        params.put("LOGO_IMAGE",   resolveLogoPath());
        params.put("receiptId",   String.valueOf(receipt.getId()));
        params.put("dateTime",    receipt.getDateTime() != null
                ? receipt.getDateTime()
                : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        params.put("processedBy", receipt.getProcessedBy() != null
                ? receipt.getProcessedBy() : "SYSTEM");
        params.put("grandTotal",  "₱" + String.format("%.2f", receipt.getTotalAmount()));

        // Step 3 – XPath selects each <item> under <receipt><items>
        String outputPath = OUTPUT_DIR + "receipt_" + FMT.format(LocalDateTime.now()) + ".pdf";
        fillAndExport("jrxml/receipt.jrxml",
                      xmlPath,
                      "/data/receipt/items/item",
                      params,
                      outputPath);
        return outputPath;
    }

    /**
     * Generates a single-receipt reprint PDF (used from Sales History).
     *
     * Pipeline:
     *   Receipt object → XmlDataWriter → receipt_reprint.xml
     *   receipt_reprint.jrxml + xml → JasperReports → pdf
     */
    public static String generateReceiptReprint(Receipt receipt) throws Exception {

        // Step 1 – write XML
        String xmlPath = XmlDataWriter.writeReceiptReprintXml(receipt);

        // Step 2 – parameters
        Map<String, Object> params = new HashMap<>();
        params.put("LOGO_IMAGE",   resolveLogoPath());
        params.put("receiptId",   String.valueOf(receipt.getId()));
        params.put("dateTime",    receipt.getDateTime() != null
                ? receipt.getDateTime() : "—");
        params.put("processedBy", receipt.getProcessedBy() != null
                ? receipt.getProcessedBy() : "SYSTEM");
        params.put("items",       receipt.getMedName());
        params.put("grandTotal",  "₱" + String.format("%.2f", receipt.getTotalAmount()));

        // Step 3 – XPath: the reprint has only one <receipt> node (no iteration)
        String outputPath = OUTPUT_DIR + "receipt_reprint_" + FMT.format(LocalDateTime.now()) + ".pdf";
        fillAndExport("jrxml/receipt_reprint.jrxml",
                      xmlPath,
                      "/data/receipt",
                      params,
                      outputPath);
        return outputPath;
    }

    /**
     * Generates the full inventory stock report PDF.
     *
     * Pipeline:
     *   Medicine list → XmlDataWriter → stock_<ts>.xml
     *   stock_report.jrxml + xml → JasperReports → stock_report_<ts>.pdf
     */
    public static String generateStockReport(List<Medicine> medicines) throws Exception {

        // Step 1 – write XML
        String xmlPath = XmlDataWriter.writeStockXml(medicines);

        // Step 2 – read meta values for parameters (stored in the XML itself)
        //         We open the XML to extract <meta> values so the .jrxml
        //         can display them in the title band.
        Map<String, Object> params = new HashMap<>();
        params.put("LOGO_IMAGE",    resolveLogoPath());
        params.put("reportDate",    LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("MMMM dd, yyyy HH:mm")));
        params.put("totalItems",    String.valueOf(medicines.size()));
        long low = medicines.stream().filter(m -> m.getStock() < 20).count();
        params.put("lowStockCount", String.valueOf(low));

        // Step 3 – XPath: iterate every <medicine> under <data>
        String outputPath = OUTPUT_DIR + "stock_report_" + FMT.format(LocalDateTime.now()) + ".pdf";
        fillAndExport("jrxml/stock_report.jrxml",
                      xmlPath,
                      "/data/medicine",
                      params,
                      outputPath);
        return outputPath;
    }

    // ─────────────────────────────────────────────────────────────────
    //  PRIVATE PIPELINE CORE
    // ─────────────────────────────────────────────────────────────────

    /**
     * Compiles a .jrxml, wraps the XML data file in a JRXmlDataSource,
     * fills the report, and exports it to PDF.
     *
     * @param jrxmlClasspath  classpath resource path, e.g. "jrxml/receipt.jrxml"
     * @param xmlDataFilePath absolute path to the XML data file produced by XmlDataWriter
     * @param selectExpr      XPath expression selecting the repeating row node,
     *                        e.g. "/data/receipt/items/item"
     * @param params          parameter map injected into the report
     * @param outputPdfPath   absolute path for the generated PDF
     */
    private static void fillAndExport(String jrxmlClasspath,
                                       String xmlDataFilePath,
                                       String selectExpr,
                                       Map<String, Object> params,
                                       String outputPdfPath) throws Exception {

        // ── 1. Compile .jrxml → JasperReport ──────────────────────────────
        InputStream jrxmlStream = ReportGenerator.class
                .getClassLoader()
                .getResourceAsStream(jrxmlClasspath);
        if (jrxmlStream == null) {
            throw new IllegalStateException(
                    "JRXML template not found on classpath: " + jrxmlClasspath
                    + "\nMake sure src/main/resources/jrxml/ is included in the build.");
        }
        JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);

        // ── 2. Build JRXmlDataSource from the XML file ────────────────────
        //      JRXmlDataSource navigates the XML with the given XPath expr.
        JRXmlDataSource dataSource = new JRXmlDataSource(
                new File(xmlDataFilePath), selectExpr);

        // ── 3. Fill the report (merge template + data + parameters) ───────
        JasperPrint jasperPrint = JasperFillManager.fillReport(
                jasperReport, params, dataSource);

        // ── 4. Export to PDF ──────────────────────────────────────────────
        JRPdfExporter exporter = new JRPdfExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(
                new SimpleOutputStreamExporterOutput(outputPdfPath));
        exporter.exportReport();
    }
}
