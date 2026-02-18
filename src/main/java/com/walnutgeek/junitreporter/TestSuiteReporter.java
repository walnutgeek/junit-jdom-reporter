package com.walnutgeek.junitreporter;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TestSuiteReporter {

    private final String name;
    private final String timestamp;
    private final ConcurrentHashMap<String, TestCaseReporter> testCases = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<String> testCaseOrder = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<String, String> properties = new ConcurrentHashMap<>();

    public TestSuiteReporter(String name) {
        this.name = name;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        this.timestamp = sdf.format(new Date());
    }

    public String getName() {
        return name;
    }

    public TestCaseReporter testCase(String testName) {
        return testCases.computeIfAbsent(testName, k -> {
            testCaseOrder.add(k);
            return new TestCaseReporter(k, name);
        });
    }

    public void addProperty(String key, String value) {
        properties.put(key, value);
    }

    public int getTestCount() {
        return testCases.size();
    }

    public Document toDocument() {
        Element root = new Element("testsuite");
        List<TestCaseReporter> orderedCases = getOrderedTestCases();

        int failures = 0;
        int errors = 0;
        double totalTime = 0;

        for (TestCaseReporter tc : orderedCases) {
            if (tc.hasFailure()) failures++;
            if (tc.hasError()) errors++;
            totalTime += tc.getTime();
        }

        root.setAttribute("name", name);
        root.setAttribute("tests", String.valueOf(orderedCases.size()));
        root.setAttribute("failures", String.valueOf(failures));
        root.setAttribute("errors", String.valueOf(errors));
        root.setAttribute("skipped", "0");
        root.setAttribute("time", formatTime(totalTime));
        root.setAttribute("timestamp", timestamp);

        if (!properties.isEmpty()) {
            Element propsEl = new Element("properties");
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                Element prop = new Element("property");
                prop.setAttribute("name", entry.getKey());
                prop.setAttribute("value", entry.getValue());
                propsEl.addContent(prop);
            }
            root.addContent(propsEl);
        }

        for (TestCaseReporter tc : orderedCases) {
            Element tcEl = new Element("testcase");
            tcEl.setAttribute("name", tc.getName());
            tcEl.setAttribute("classname", tc.getClassName());
            tcEl.setAttribute("time", formatTime(tc.getTime()));

            for (TestCaseReporter.Entry f : tc.getFailures()) {
                Element fel = new Element("failure");
                fel.setAttribute("type", f.getType());
                fel.setAttribute("message", f.getMessage());
                fel.setText(f.getBody());
                tcEl.addContent(fel);
            }

            for (TestCaseReporter.Entry e : tc.getErrors()) {
                Element eel = new Element("error");
                eel.setAttribute("type", e.getType());
                eel.setAttribute("message", e.getMessage());
                eel.setText(e.getBody());
                tcEl.addContent(eel);
            }

            String stdout = tc.getStdout();
            if (!stdout.isEmpty()) {
                Element sysout = new Element("system-out");
                sysout.setText(stdout);
                tcEl.addContent(sysout);
            }

            String stderr = tc.getStderr();
            if (!stderr.isEmpty()) {
                Element syserr = new Element("system-err");
                syserr.setText(stderr);
                tcEl.addContent(syserr);
            }

            root.addContent(tcEl);
        }

        return new Document(root);
    }

    public void writeXml(File outputDir) throws IOException {
        File file = new File(outputDir, "TEST-" + name + ".xml");
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
            outputter.output(toDocument(), writer);
        }
    }

    public void writeText(File outputDir) throws IOException {
        List<TestCaseReporter> orderedCases = getOrderedTestCases();

        int failures = 0;
        int errors = 0;
        double totalTime = 0;

        for (TestCaseReporter tc : orderedCases) {
            if (tc.hasFailure()) failures++;
            if (tc.hasError()) errors++;
            totalTime += tc.getTime();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("-------------------------------------------------------------------------------\n");
        sb.append("Test set: ").append(name).append("\n");
        sb.append("-------------------------------------------------------------------------------\n");
        sb.append(String.format("Tests run: %d, Failures: %d, Errors: %d, Skipped: 0, Time elapsed: %s s\n",
                orderedCases.size(), failures, errors, formatTime(totalTime)));

        for (TestCaseReporter tc : orderedCases) {
            if (tc.hasFailure()) {
                sb.append("\n");
                sb.append(tc.getName()).append("  Time elapsed: ").append(formatTime(tc.getTime())).append(" s  <<< FAILURE!\n");
                for (TestCaseReporter.Entry f : tc.getFailures()) {
                    sb.append(f.getType()).append(": ").append(f.getMessage()).append("\n");
                    sb.append(f.getBody()).append("\n");
                }
            }
            if (tc.hasError()) {
                sb.append("\n");
                sb.append(tc.getName()).append("  Time elapsed: ").append(formatTime(tc.getTime())).append(" s  <<< ERROR!\n");
                for (TestCaseReporter.Entry e : tc.getErrors()) {
                    sb.append(e.getType()).append(": ").append(e.getMessage()).append("\n");
                    sb.append(e.getBody()).append("\n");
                }
            }
        }

        File file = new File(outputDir, name + ".txt");
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
            writer.write(sb.toString());
        }
    }

    private List<TestCaseReporter> getOrderedTestCases() {
        List<TestCaseReporter> ordered = new ArrayList<>();
        for (String testName : testCaseOrder) {
            TestCaseReporter tc = testCases.get(testName);
            if (tc != null) {
                ordered.add(tc);
            }
        }
        return ordered;
    }

    private static String formatTime(double seconds) {
        return String.format(Locale.US, "%.3f", seconds);
    }
}
