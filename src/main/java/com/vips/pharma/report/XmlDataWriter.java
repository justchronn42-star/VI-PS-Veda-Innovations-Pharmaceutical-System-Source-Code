package com.vips.pharma.report;

import com.vips.pharma.model.CartItem;
import com.vips.pharma.model.Medicine;
import com.vips.pharma.model.Receipt;

// Use only JDK built-in java.xml module — no xerces on classpath.
// All javax.xml.* and org.w3c.dom.* classes below come exclusively
// from the java.xml module that ships with Java 9+.
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ════════════════════════════════════════════════════════════════════
 *  XML DATA WRITER
 *  Pipeline step: Java objects  →  XML file  →  JasperReports
 *
 *  Each method:
 *   1. Receives Java model objects (already fetched from the DB by a DAO)
 *   2. Builds a DOM tree that matches the XPath selectors in the .jrxml
 *   3. Writes a temporary XML file to the reports/data/ directory
 *   4. Returns the absolute path so ReportGenerator can pass it to
 *      JRXmlDataSource.
 *
 *  Why no xerces dependency?
 *  Java 17 bundles a complete javax.xml / org.w3c.dom implementation
 *  inside the java.xml module. Adding xercesImpl on the classpath
 *  duplicates those packages and causes the Eclipse / javac
 *  "split package" compiler error. The JDK parser is used directly.
 *
 *  XML root element is always <data>.  Row elements match the XPath
 *  selectExpression declared in each .jrxml template.
 *
 *  Example receipt XML:
 *  <pre>
 *  &lt;?xml version="1.0" encoding="UTF-8"?&gt;
 *  &lt;data&gt;
 *    &lt;receipt&gt;
 *      &lt;receiptId&gt;42&lt;/receiptId&gt;
 *      &lt;dateTime&gt;2025-06-01 14:22:00&lt;/dateTime&gt;
 *      &lt;processedBy&gt;cashier1&lt;/processedBy&gt;
 *      &lt;grandTotal&gt;350.00&lt;/grandTotal&gt;
 *      &lt;items&gt;
 *        &lt;item&gt;
 *          &lt;medName&gt;AMOXICILLIN 500MG&lt;/medName&gt;
 *          &lt;quantity&gt;2&lt;/quantity&gt;
 *          &lt;unitPrice&gt;75.00&lt;/unitPrice&gt;
 *          &lt;total&gt;150.00&lt;/total&gt;
 *        &lt;/item&gt;
 *      &lt;/items&gt;
 *    &lt;/receipt&gt;
 *  &lt;/data&gt;
 *  </pre>
 * ════════════════════════════════════════════════════════════════════
 */
public class XmlDataWriter {

    private static final String DATA_DIR = "reports/data/";
    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    static {
        new File(DATA_DIR).mkdirs();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Receipt  (multi-item — called from POS on checkout)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Serialises a completed sale to XML.
     *
     * @param receipt   saved receipt (must have a valid id, dateTime,
     *                  processedBy, and totalAmount)
     * @param cartItems the individual line items sold
     * @return absolute path of the written XML file
     */
    public static String writeReceiptXml(Receipt receipt, List<CartItem> cartItems)
            throws Exception {

        Document doc = newDocument();
        Element root = doc.createElement("data");
        doc.appendChild(root);

        // <receipt> header node
        Element receiptEl = doc.createElement("receipt");
        root.appendChild(receiptEl);

        appendText(doc, receiptEl, "receiptId",
                String.valueOf(receipt.getId()));
        appendText(doc, receiptEl, "dateTime",
                receipt.getDateTime() != null
                        ? receipt.getDateTime()
                        : LocalDateTime.now().format(
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        appendText(doc, receiptEl, "processedBy",
                receipt.getProcessedBy() != null ? receipt.getProcessedBy() : "SYSTEM");
        appendText(doc, receiptEl, "grandTotal",
                String.format("%.2f", receipt.getTotalAmount()));

        // <items> -> repeated <item> nodes
        Element itemsEl = doc.createElement("items");
        receiptEl.appendChild(itemsEl);

        for (CartItem ci : cartItems) {
            Element item = doc.createElement("item");
            appendText(doc, item, "medName",            ci.getMedName());
            appendText(doc, item, "quantity",           String.valueOf(ci.getQuantity()));
            appendText(doc, item, "unitPriceFormatted", "₱" + String.format("%.2f", ci.getUnitPrice()));
            appendText(doc, item, "totalFormatted",     "₱" + String.format("%.2f", ci.getTotal()));
            itemsEl.appendChild(item);
        }

        String path = DATA_DIR + "receipt_" + STAMP.format(LocalDateTime.now()) + ".xml";
        writeXml(doc, path);
        return path;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Receipt reprint  (summary row only, from Sales History screen)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Serialises a summary receipt for reprinting.
     * No line-item breakdown — uses the single row stored in the receipts table.
     */
    public static String writeReceiptReprintXml(Receipt receipt) throws Exception {
        Document doc = newDocument();
        Element root = doc.createElement("data");
        doc.appendChild(root);

        Element receiptEl = doc.createElement("receipt");
        root.appendChild(receiptEl);

        appendText(doc, receiptEl, "receiptId",
                String.valueOf(receipt.getId()));
        appendText(doc, receiptEl, "dateTime",
                receipt.getDateTime() != null ? receipt.getDateTime() : "-");
        appendText(doc, receiptEl, "processedBy",
                receipt.getProcessedBy() != null ? receipt.getProcessedBy() : "SYSTEM");
        appendText(doc, receiptEl, "items",
                receipt.getMedName() != null ? receipt.getMedName() : "-");
        appendText(doc, receiptEl, "grandTotal",
                String.format("%.2f", receipt.getTotalAmount()));

        String path = DATA_DIR + "receipt_reprint_" + STAMP.format(LocalDateTime.now()) + ".xml";
        writeXml(doc, path);
        return path;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Stock / Inventory Report
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Serialises the full inventory list to XML.
     *
     * @param medicines list fetched from InventoryDAO
     * @return absolute path of the written XML file
     */
    public static String writeStockXml(List<Medicine> medicines) throws Exception {
        Document doc = newDocument();
        Element root = doc.createElement("data");
        doc.appendChild(root);

        // <meta> — report-level metadata read as parameters in the .jrxml
        Element meta = doc.createElement("meta");
        root.appendChild(meta);
        appendText(doc, meta, "reportDate",
                LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("MMMM dd, yyyy HH:mm")));
        appendText(doc, meta, "totalItems", String.valueOf(medicines.size()));
        long lowCount = medicines.stream().filter(m -> m.getStock() < 20).count();
        appendText(doc, meta, "lowStockCount", String.valueOf(lowCount));

        // One <medicine> element per inventory row
        for (Medicine m : medicines) {
            Element med = doc.createElement("medicine");
            appendText(doc, med, "id",             String.valueOf(m.getId()));
            appendText(doc, med, "name",           m.getName());
            appendText(doc, med, "stock",          String.valueOf(m.getStock()));
            appendText(doc, med, "priceFormatted", "₱" + String.format("%.2f", m.getPrice()));
            // "true"/"false" flag used by the conditional style in stock_report.jrxml
            appendText(doc, med, "lowStock", m.getStock() < 20 ? "true" : "false");
            root.appendChild(med);
        }

        String path = DATA_DIR + "stock_" + STAMP.format(LocalDateTime.now()) + ".xml";
        writeXml(doc, path);
        return path;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Private helpers
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Creates a new DOM Document using the JDK's built-in parser.
     * Explicitly uses the JDK factory class name to avoid any chance of
     * a third-party parser being picked up from the classpath.
     */
    private static Document newDocument() throws Exception {
        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance(
                        "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl",
                        XmlDataWriter.class.getClassLoader());
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.newDocument();
    }

    private static void appendText(Document doc, Element parent,
                                   String tag, String value) {
        Element el = doc.createElement(tag);
        el.setTextContent(value != null ? value : "");
        parent.appendChild(el);
    }

    /**
     * Writes the DOM to a UTF-8 XML file.
     * Explicitly uses the JDK's built-in TransformerFactory to avoid
     * any stray xalan/xslt jar being picked up instead.
     */
    private static void writeXml(Document doc, String path) throws Exception {
        TransformerFactory factory =
                TransformerFactory.newInstance(
                        "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl",
                        XmlDataWriter.class.getClassLoader());
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT,   "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.VERSION,  "1.0");
        transformer.setOutputProperty(
                "{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(new DOMSource(doc), new StreamResult(new File(path)));
    }
}
