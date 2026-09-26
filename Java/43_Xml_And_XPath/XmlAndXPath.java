import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * 43 - XML part two: build a document in memory, serialise it, then query it
 * with XPath instead of walking the DOM by hand.
 *
 * Compile and run:
 *   javac XmlAndXPath.java
 *   java XmlAndXPath
 */
public class XmlAndXPath {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static void addTest(Document document, Element suite, String id, String status, double seconds) {
        Element test = document.createElement("test");
        test.setAttribute("id", id);
        test.setAttribute("status", status);
        test.setAttribute("seconds", String.valueOf(seconds));

        Element name = document.createElement("name");
        name.setTextContent("case " + id);
        test.appendChild(name);
        suite.appendChild(test);
    }

    static Document buildReport() throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = builder.newDocument();

        Element suite = document.createElement("suite");
        suite.setAttribute("name", "smoke");
        suite.setAttribute("browser", "chromium");
        document.appendChild(suite);

        addTest(document, suite, "TC-01", "pass", 1.25);
        addTest(document, suite, "TC-02", "fail", 4.75);
        addTest(document, suite, "TC-03", "pass", 0.5);
        return document;
    }

    static String toXml(Document document) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString();
    }

    /** Evaluate an XPath expression that yields a string, number or boolean. */
    static String queryString(Document document, String expression) throws Exception {
        XPath xpath = XPathFactory.newInstance().newXPath();
        return xpath.evaluate(expression, document);
    }

    /** Evaluate an XPath expression that yields a node set, as their text. */
    static List<String> queryNodes(Document document, String expression) throws Exception {
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList nodes = (NodeList) xpath.evaluate(expression, document, XPathConstants.NODESET);
        List<String> texts = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            texts.add(nodes.item(i).getTextContent());
        }
        return texts;
    }

    public static void main(String[] args) throws Exception {
        Document report = buildReport();
        String xml = toXml(report);
        System.out.println(xml.strip());

        // ---- counts and scalar values ---------------------------------------
        check(queryString(report, "count(/suite/test)").equals("3"), "three tests in the suite");
        check(queryString(report, "count(/suite/test[@status='pass'])").equals("2"), "two passed");
        check(queryString(report, "count(/suite/test[@status='fail'])").equals("1"), "one failed");
        check(queryString(report, "string(/suite/@name)").equals("smoke"), "the suite name");
        check(queryString(report, "string(/suite/@browser)").equals("chromium"), "the browser attribute");
        check(queryString(report, "sum(/suite/test/@seconds)").equals("6.5"), "total seconds");
        check(queryString(report, "boolean(/suite/test[@status='fail'])").equals("true"),
                "a boolean expression");
        check(queryString(report, "boolean(/suite/test[@status='skipped'])").equals("false"),
                "no skipped tests");

        // ---- node selections -------------------------------------------------
        check(queryNodes(report, "/suite/test[@status='fail']/name").equals(List.of("case TC-02")),
                "the failing case by name");
        check(queryNodes(report, "//name").size() == 3, "every name element");
        check(queryNodes(report, "/suite/test[2]/@id").equals(List.of("TC-02")),
                "XPath positions start at 1, not 0");
        check(queryNodes(report, "/suite/test[@seconds > 1]/@id").equals(List.of("TC-01", "TC-02")),
                "a numeric comparison");
        System.out.println("failing case: " + queryNodes(report, "/suite/test[@status='fail']/name"));

        // ---- round trip: parse the text back and query that ------------------
        Document reparsed = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        check(queryString(reparsed, "count(/suite/test)").equals("3"), "the serialised XML reparses");
        check(queryString(reparsed, "sum(/suite/test/@seconds)").equals("6.5"), "same total after reparsing");

        // ---- a bad expression is an error, not a silent empty result ---------
        try {
            queryString(report, "count(/suite/test");
            throw new AssertionError("a malformed XPath should fail");
        } catch (XPathExpressionException expected) {
            System.out.println("bad xpath   : " + expected.getMessage());
        }
        System.out.println("All checks passed.");
    }
}
