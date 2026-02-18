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
    public void testToDocumentErrorElement() {
        TestSuiteReporter suite = new TestSuiteReporter("com.example.MyTest");
        TestCaseReporter tc = suite.testCase("testError");
        tc.addError("ArithmeticException", "/ by zero", "error trace");

        Document doc = suite.toDocument();
        Element testcase = doc.getRootElement().getChildren("testcase").get(0);
        Element error = testcase.getChild("error");
        assertNotNull(error);
        assertEquals("ArithmeticException", error.getAttributeValue("type"));
        assertEquals("/ by zero", error.getAttributeValue("message"));
        assertEquals("error trace", error.getText());
    }

    @Test
    public void testToDocumentSystemErr() {
        TestSuiteReporter suite = new TestSuiteReporter("com.example.MyTest");
        suite.testCase("testErr").addStderr("error output\n");

        Document doc = suite.toDocument();
        Element testcase = doc.getRootElement().getChildren("testcase").get(0);
        Element syserr = testcase.getChild("system-err");
        assertNotNull(syserr);
        assertEquals("error output\n", syserr.getText());
    }

    @Test
    public void testWriteTextWithError() throws IOException {
        TestSuiteReporter suite = new TestSuiteReporter("com.example.MyTest");
        suite.testCase("testPass").setTime(0.01);
        TestCaseReporter tc = suite.testCase("testError");
        tc.setTime(0.03);
        tc.addError("ArithmeticException", "/ by zero", "error trace");

        File dir = tempDir.getRoot();
        suite.writeText(dir);

        String content = new String(Files.readAllBytes(
            new File(dir, "com.example.MyTest.txt").toPath()), "UTF-8");
        assertTrue(content.contains("Errors: 1"));
        assertTrue(content.contains("<<< ERROR!"));
        assertTrue(content.contains("ArithmeticException: / by zero"));
        assertTrue(content.contains("error trace"));
    }

    @Test
    public void testInsertionOrder() {
        TestSuiteReporter suite = new TestSuiteReporter("com.example.MyTest");
        suite.testCase("charlie");
        suite.testCase("alpha");
        suite.testCase("bravo");

        Document doc = suite.toDocument();
        List<Element> testcases = doc.getRootElement().getChildren("testcase");
        assertEquals("charlie", testcases.get(0).getAttributeValue("name"));
        assertEquals("alpha", testcases.get(1).getAttributeValue("name"));
        assertEquals("bravo", testcases.get(2).getAttributeValue("name"));
    }

    @Test
    public void testWriteTextWithBothFailureAndError() throws IOException {
        TestSuiteReporter suite = new TestSuiteReporter("com.example.MyTest");
        TestCaseReporter tc = suite.testCase("testBoth");
        tc.setTime(0.05);
        tc.addFailure("AssertionError", "assertion msg", "fail trace");
        tc.addError("RuntimeException", "runtime msg", "error trace");

        File dir = tempDir.getRoot();
        suite.writeText(dir);

        String content = new String(Files.readAllBytes(
            new File(dir, "com.example.MyTest.txt").toPath()), "UTF-8");
        // Single block with FAILURE! marker (failure takes precedence)
        assertTrue(content.contains("testBoth  Time elapsed: 0.050 s  <<< FAILURE!"));
        // Both entries appear in the same block
        assertTrue(content.contains("AssertionError: assertion msg"));
        assertTrue(content.contains("fail trace"));
        assertTrue(content.contains("RuntimeException: runtime msg"));
        assertTrue(content.contains("error trace"));
        // Only one header line for this test case (not two)
        int firstIdx = content.indexOf("testBoth  Time elapsed:");
        int secondIdx = content.indexOf("testBoth  Time elapsed:", firstIdx + 1);
        assertEquals("Should have only one header line per test case", -1, secondIdx);
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
