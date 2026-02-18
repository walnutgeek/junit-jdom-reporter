# junit-jdom-reporter Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a standalone Java library that produces JUnit/Surefire-compatible XML and text reports using JDOM 2.0.5.

**Architecture:** Two classes — `TestSuiteReporter` (holds properties and a ConcurrentHashMap of test cases, writes XML/text files) and `TestCaseReporter` (accumulates stdout/stderr/failure/error entries, thread-safe via synchronized). No JUnit runtime dependency.

**Tech Stack:** Java 8, Maven, JDOM 2.0.5, JUnit 4.13.2 (test scope only)

**Design doc:** `docs/plans/2026-02-18-junit-jdom-reporter-design.md`

---

### Task 1: Maven Project Setup

**Files:**
- Create: `pom.xml`

**Step 1: Create pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.walnutgeek</groupId>
    <artifactId>junit-jdom-reporter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <name>junit-jdom-reporter</name>
    <description>Little helper that creates JUnit compatible XML using JDOM</description>
    <url>https://github.com/walnutgeek/junit-jdom-reporter</url>

    <licenses>
        <license>
            <name>Apache License, Version 2.0</name>
            <url>https://www.apache.org/licenses/LICENSE-2.0</url>
        </license>
    </licenses>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.jdom</groupId>
            <artifactId>jdom2</artifactId>
            <version>2.0.5</version>
        </dependency>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.13.2</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

**Step 2: Verify Maven resolves dependencies**

Run: `mvn dependency:resolve -q`
Expected: SUCCESS, no errors

**Step 3: Commit**

```bash
git add pom.xml
git commit -m "Add Maven pom.xml with JDOM and JUnit dependencies"
```

---

### Task 2: TestCaseReporter — Failing Tests

**Files:**
- Create: `src/test/java/com/walnutgeek/junitreporter/TestCaseReporterTest.java`
- Create: `src/main/java/com/walnutgeek/junitreporter/TestCaseReporter.java` (empty stub so tests compile)

**Step 1: Create directory structure**

```bash
mkdir -p src/main/java/com/walnutgeek/junitreporter
mkdir -p src/test/java/com/walnutgeek/junitreporter
```

**Step 2: Write TestCaseReporter stub**

Create `src/main/java/com/walnutgeek/junitreporter/TestCaseReporter.java` with just enough to compile:

```java
package com.walnutgeek.junitreporter;

public class TestCaseReporter {
}
```

**Step 3: Write failing tests**

Create `src/test/java/com/walnutgeek/junitreporter/TestCaseReporterTest.java`:

```java
package com.walnutgeek.junitreporter;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestCaseReporterTest {

    @Test
    public void testNameAndClassName() {
        TestCaseReporter tc = new TestCaseReporter("testAdd", "com.example.MyTest");
        assertEquals("testAdd", tc.getName());
        assertEquals("com.example.MyTest", tc.getClassName());
    }

    @Test
    public void testDefaultTimeIsZero() {
        TestCaseReporter tc = new TestCaseReporter("testAdd", "com.example.MyTest");
        assertEquals(0.0, tc.getTime(), 0.001);
    }

    @Test
    public void testSetTime() {
        TestCaseReporter tc = new TestCaseReporter("testAdd", "com.example.MyTest");
        tc.setTime(1.234);
        assertEquals(1.234, tc.getTime(), 0.001);
    }

    @Test
    public void testAddStdout() {
        TestCaseReporter tc = new TestCaseReporter("testAdd", "com.example.MyTest");
        tc.addStdout("line 1\n");
        tc.addStdout("line 2\n");
        assertEquals("line 1\nline 2\n", tc.getStdout());
    }

    @Test
    public void testAddStderr() {
        TestCaseReporter tc = new TestCaseReporter("testAdd", "com.example.MyTest");
        tc.addStderr("err 1\n");
        tc.addStderr("err 2\n");
        assertEquals("err 1\nerr 2\n", tc.getStderr());
    }

    @Test
    public void testEmptyStdoutStderr() {
        TestCaseReporter tc = new TestCaseReporter("testAdd", "com.example.MyTest");
        assertEquals("", tc.getStdout());
        assertEquals("", tc.getStderr());
    }

    @Test
    public void testAddFailure() {
        TestCaseReporter tc = new TestCaseReporter("testAdd", "com.example.MyTest");
        tc.addFailure("AssertionError", "expected 3 got 4", "stack trace");
        assertEquals(1, tc.getFailures().size());
        assertEquals("AssertionError", tc.getFailures().get(0).getType());
        assertEquals("expected 3 got 4", tc.getFailures().get(0).getMessage());
        assertEquals("stack trace", tc.getFailures().get(0).getBody());
    }

    @Test
    public void testAddError() {
        TestCaseReporter tc = new TestCaseReporter("testAdd", "com.example.MyTest");
        tc.addError("ArithmeticException", "/ by zero", "stack trace");
        assertEquals(1, tc.getErrors().size());
        assertEquals("ArithmeticException", tc.getErrors().get(0).getType());
        assertEquals("/ by zero", tc.getErrors().get(0).getMessage());
        assertEquals("stack trace", tc.getErrors().get(0).getBody());
    }

    @Test
    public void testMultipleFailuresAndErrors() {
        TestCaseReporter tc = new TestCaseReporter("testAdd", "com.example.MyTest");
        tc.addFailure("AssertionError", "msg1", "trace1");
        tc.addFailure("AssertionError", "msg2", "trace2");
        tc.addError("RuntimeException", "msg3", "trace3");
        assertEquals(2, tc.getFailures().size());
        assertEquals(1, tc.getErrors().size());
    }

    @Test
    public void testHasFailureAndHasError() {
        TestCaseReporter tc = new TestCaseReporter("testAdd", "com.example.MyTest");
        assertFalse(tc.hasFailure());
        assertFalse(tc.hasError());
        tc.addFailure("AssertionError", "msg", "trace");
        assertTrue(tc.hasFailure());
        assertFalse(tc.hasError());
        tc.addError("RuntimeException", "msg", "trace");
        assertTrue(tc.hasError());
    }
}
```

**Step 4: Run tests to verify they fail**

Run: `mvn test -pl . -Dtest=TestCaseReporterTest -q`
Expected: COMPILATION FAILURE (constructor and methods don't exist yet)

**Step 5: Commit**

```bash
git add src/
git commit -m "Add failing tests for TestCaseReporter"
```

---

### Task 3: TestCaseReporter — Implementation

**Files:**
- Modify: `src/main/java/com/walnutgeek/junitreporter/TestCaseReporter.java`

**Step 1: Implement TestCaseReporter**

Replace `src/main/java/com/walnutgeek/junitreporter/TestCaseReporter.java` with full implementation:

```java
package com.walnutgeek.junitreporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestCaseReporter {

    public static class Entry {
        private final String type;
        private final String message;
        private final String body;

        public Entry(String type, String message, String body) {
            this.type = type;
            this.message = message;
            this.body = body;
        }

        public String getType() { return type; }
        public String getMessage() { return message; }
        public String getBody() { return body; }
    }

    private final String name;
    private final String className;
    private double time;
    private final StringBuilder stdout = new StringBuilder();
    private final StringBuilder stderr = new StringBuilder();
    private final List<Entry> failures = new ArrayList<>();
    private final List<Entry> errors = new ArrayList<>();

    public TestCaseReporter(String name, String className) {
        this.name = name;
        this.className = className;
    }

    public String getName() { return name; }
    public String getClassName() { return className; }
    public double getTime() { return time; }

    public synchronized void setTime(double seconds) {
        this.time = seconds;
    }

    public synchronized void addStdout(String text) {
        stdout.append(text);
    }

    public synchronized void addStderr(String text) {
        stderr.append(text);
    }

    public synchronized String getStdout() {
        return stdout.toString();
    }

    public synchronized String getStderr() {
        return stderr.toString();
    }

    public synchronized void addFailure(String type, String message, String stackTrace) {
        failures.add(new Entry(type, message, stackTrace));
    }

    public synchronized void addError(String type, String message, String stackTrace) {
        errors.add(new Entry(type, message, stackTrace));
    }

    public synchronized List<Entry> getFailures() {
        return Collections.unmodifiableList(new ArrayList<>(failures));
    }

    public synchronized List<Entry> getErrors() {
        return Collections.unmodifiableList(new ArrayList<>(errors));
    }

    public synchronized boolean hasFailure() {
        return !failures.isEmpty();
    }

    public synchronized boolean hasError() {
        return !errors.isEmpty();
    }
}
```

**Step 2: Run tests to verify they pass**

Run: `mvn test -Dtest=TestCaseReporterTest -q`
Expected: All 10 tests PASS

**Step 3: Commit**

```bash
git add src/main/java/com/walnutgeek/junitreporter/TestCaseReporter.java
git commit -m "Implement TestCaseReporter with thread-safe entry accumulation"
```

---

### Task 4: TestSuiteReporter — Failing Tests (Core + XML)

**Files:**
- Create: `src/test/java/com/walnutgeek/junitreporter/TestSuiteReporterTest.java`
- Create: `src/main/java/com/walnutgeek/junitreporter/TestSuiteReporter.java` (empty stub)

**Step 1: Write TestSuiteReporter stub**

Create `src/main/java/com/walnutgeek/junitreporter/TestSuiteReporter.java`:

```java
package com.walnutgeek.junitreporter;

public class TestSuiteReporter {
}
```

**Step 2: Write failing tests**

Create `src/test/java/com/walnutgeek/junitreporter/TestSuiteReporterTest.java`:

```java
package com.walnutgeek.junitreporter;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.*;

public class TestSuiteReporterTest {

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    @Test
    public void testSuiteName() {
        TestSuiteReporter suite = new TestSuiteReporter("com.example.MyTest");
        assertEquals("com.example.MyTest", suite.getName());
    }

    @Test
    public void testLazyTestCaseCreation() {
        TestSuiteReporter suite = new TestSuiteReporter("com.example.MyTest");
        TestCaseReporter tc1 = suite.testCase("testAdd");
        TestCaseReporter tc2 = suite.testCase("testAdd");
        assertSame(tc1, tc2);
        assertEquals("testAdd", tc1.getName());
        assertEquals("com.example.MyTest", tc1.getClassName());
    }

    @Test
    public void testMultipleTestCases() {
        TestSuiteReporter suite = new TestSuiteReporter("com.example.MyTest");
        suite.testCase("testAdd");
        suite.testCase("testSubtract");
        suite.testCase("testMultiply");
        assertEquals(3, suite.getTestCount());
    }

    @Test
    public void testProperties() {
        TestSuiteReporter suite = new TestSuiteReporter("com.example.MyTest");
        suite.addProperty("env", "ci");
        suite.addProperty("os", "linux");
        Document doc = suite.toDocument();
        Element root = doc.getRootElement();
        Element props = root.getChild("properties");
        assertNotNull(props);
        List<Element> propList = props.getChildren("property");
        assertEquals(2, propList.size());
    }

    @Test
    public void testToDocumentEmpty() {
        TestSuiteReporter suite = new TestSuiteReporter("com.example.MyTest");
        Document doc = suite.toDocument();
        Element root = doc.getRootElement();
        assertEquals("testsuite", root.getName());
        assertEquals("com.example.MyTest", root.getAttributeValue("name"));
        assertEquals("0", root.getAttributeValue("tests"));
        assertEquals("0", root.getAttributeValue("failures"));
        assertEquals("0", root.getAttributeValue("errors"));
        assertEquals("0", root.getAttributeValue("skipped"));
    }

    @Test
    public void testToDocumentWithTestCases() {
        TestSuiteReporter suite = new TestSuiteReporter("com.example.MyTest");
        suite.testCase("testPass").setTime(0.01);
        TestCaseReporter tc = suite.testCase("testFail");
        tc.setTime(0.045);
        tc.addFailure("AssertionError", "expected 3 got 4", "stack trace");
        tc.addStdout("computing...\n");
        TestCaseReporter tc2 = suite.testCase("testError");
        tc2.setTime(0.03);
        tc2.addError("ArithmeticException", "/ by zero", "error trace");

        Document doc = suite.toDocument();
        Element root = doc.getRootElement();
        assertEquals("3", root.getAttributeValue("tests"));
        assertEquals("1", root.getAttributeValue("failures"));
        assertEquals("1", root.getAttributeValue("errors"));

        List<Element> testcases = root.getChildren("testcase");
        assertEquals(3, testcases.size());
    }

    @Test
    public void testToDocumentFailureElement() {
        TestSuiteReporter suite = new TestSuiteReporter("com.example.MyTest");
        TestCaseReporter tc = suite.testCase("testFail");
        tc.addFailure("AssertionError", "msg", "trace");

        Document doc = suite.toDocument();
        Element testcase = doc.getRootElement().getChildren("testcase").get(0);
        Element failure = testcase.getChild("failure");
        assertNotNull(failure);
        assertEquals("AssertionError", failure.getAttributeValue("type"));
        assertEquals("msg", failure.getAttributeValue("message"));
        assertEquals("trace", failure.getText());
    }

    @Test
    public void testToDocumentSystemOut() {
        TestSuiteReporter suite = new TestSuiteReporter("com.example.MyTest");
        suite.testCase("testOut").addStdout("hello\n");

        Document doc = suite.toDocument();
        Element testcase = doc.getRootElement().getChildren("testcase").get(0);
        Element sysout = testcase.getChild("system-out");
        assertNotNull(sysout);
        assertEquals("hello\n", sysout.getText());
    }

    @Test
    public void testToDocumentNoSystemOutWhenEmpty() {
        TestSuiteReporter suite = new TestSuiteReporter("com.example.MyTest");
        suite.testCase("testClean");

        Document doc = suite.toDocument();
        Element testcase = doc.getRootElement().getChildren("testcase").get(0);
        assertNull(testcase.getChild("system-out"));
        assertNull(testcase.getChild("system-err"));
    }

    @Test
    public void testWriteXml() throws IOException {
        TestSuiteReporter suite = new TestSuiteReporter("com.example.MyTest");
        suite.testCase("testPass").setTime(0.01);

        File dir = tempDir.getRoot();
        suite.writeXml(dir);

        File xmlFile = new File(dir, "TEST-com.example.MyTest.xml");
        assertTrue(xmlFile.exists());
        String content = new String(Files.readAllBytes(xmlFile.toPath()), "UTF-8");
        assertTrue(content.contains("<?xml"));
        assertTrue(content.contains("<testsuite"));
        assertTrue(content.contains("name=\"com.example.MyTest\""));
    }

    @Test
    public void testWriteText() throws IOException {
        TestSuiteReporter suite = new TestSuiteReporter("com.example.MyTest");
        suite.testCase("testPass").setTime(0.01);
        TestCaseReporter tc = suite.testCase("testFail");
        tc.setTime(0.045);
        tc.addFailure("AssertionError", "expected 3 got 4", "stack trace here");

        File dir = tempDir.getRoot();
        suite.writeText(dir);

        File txtFile = new File(dir, "com.example.MyTest.txt");
        assertTrue(txtFile.exists());
        String content = new String(Files.readAllBytes(txtFile.toPath()), "UTF-8");
        assertTrue(content.contains("Test set: com.example.MyTest"));
        assertTrue(content.contains("Tests run: 2"));
        assertTrue(content.contains("Failures: 1"));
        assertTrue(content.contains("<<< FAILURE!"));
        assertTrue(content.contains("AssertionError: expected 3 got 4"));
        assertTrue(content.contains("stack trace here"));
    }

    @Test
    public void testWriteTextPassingOnly() throws IOException {
        TestSuiteReporter suite = new TestSuiteReporter("com.example.MyTest");
        suite.testCase("testPass").setTime(0.01);

        File dir = tempDir.getRoot();
        suite.writeText(dir);

        String content = new String(Files.readAllBytes(
            new File(dir, "com.example.MyTest.txt").toPath()), "UTF-8");
        assertTrue(content.contains("Tests run: 1"));
        assertTrue(content.contains("Failures: 0"));
        assertFalse(content.contains("<<<"));
    }

    @Test
    public void testThreadSafeLazyCreation() throws InterruptedException {
        TestSuiteReporter suite = new TestSuiteReporter("com.example.MyTest");
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        TestCaseReporter[] results = new TestCaseReporter[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            threads[i] = new Thread(() -> {
                results[idx] = suite.testCase("sharedTest");
            });
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        for (int i = 1; i < threadCount; i++) {
            assertSame("All threads should get the same TestCaseReporter", results[0], results[i]);
        }
    }

    @Test
    public void testTimeIsSumOfTestCases() {
        TestSuiteReporter suite = new TestSuiteReporter("com.example.MyTest");
        suite.testCase("test1").setTime(1.0);
        suite.testCase("test2").setTime(2.5);

        Document doc = suite.toDocument();
        String timeStr = doc.getRootElement().getAttributeValue("time");
        assertEquals(3.5, Double.parseDouble(timeStr), 0.001);
    }
}
```

**Step 3: Run tests to verify they fail**

Run: `mvn test -Dtest=TestSuiteReporterTest -q`
Expected: COMPILATION FAILURE

**Step 4: Commit**

```bash
git add src/
git commit -m "Add failing tests for TestSuiteReporter"
```

---

### Task 5: TestSuiteReporter — Implementation

**Files:**
- Modify: `src/main/java/com/walnutgeek/junitreporter/TestSuiteReporter.java`

**Step 1: Implement TestSuiteReporter**

Replace `src/main/java/com/walnutgeek/junitreporter/TestSuiteReporter.java`:

```java
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
```

**Key implementation notes:**
- `ConcurrentLinkedQueue<String> testCaseOrder` preserves insertion order for deterministic output (ConcurrentHashMap doesn't guarantee order)
- `computeIfAbsent` ensures atomic lazy creation
- `formatTime` uses `Locale.US` to avoid locale-dependent decimal separators

**Step 2: Run all tests**

Run: `mvn test -q`
Expected: All tests PASS (both TestCaseReporterTest and TestSuiteReporterTest)

**Step 3: Commit**

```bash
git add src/main/java/com/walnutgeek/junitreporter/TestSuiteReporter.java
git commit -m "Implement TestSuiteReporter with XML and text output"
```

---

### Task 6: Final Verification

**Step 1: Run full build**

Run: `mvn clean verify -q`
Expected: BUILD SUCCESS

**Step 2: Inspect generated test artifacts**

Run: `ls target/surefire-reports/`
Expected: Should see the XML/TXT files from Maven's own test run of our test classes.

**Step 3: Commit any remaining changes (if any)**

Only if there are uncommitted changes from the verification step.
