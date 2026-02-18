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
