# junit-jdom-reporter

[![CI](https://github.com/walnutgeek/junit-jdom-reporter/actions/workflows/ci.yml/badge.svg)](https://github.com/walnutgeek/junit-jdom-reporter/actions/workflows/ci.yml)

Little helper that creates JUnit compatible XML using JDOM.

A standalone Java library that produces JUnit/Surefire-compatible XML and plain-text report files. No JUnit runtime dependency -- just JDOM 2.0.5. Thread-safe.

## Usage

```java
TestSuiteReporter suite = new TestSuiteReporter("com.example.MyTest");
suite.addProperty("env", "ci");

// Test cases are created lazily by name
suite.testCase("testAdd").addStdout("computing...\n");
suite.testCase("testAdd").setTime(0.045);
suite.testCase("testAdd").addFailure("AssertionError", "expected 3 got 4", stackTrace);

suite.testCase("testDivide").setTime(0.030);
suite.testCase("testDivide").addError("ArithmeticException", "/ by zero", stackTrace);

suite.testCase("testSubtract").setTime(0.010);

// Write Surefire-style output files
File outputDir = new File("target/surefire-reports");
suite.writeXml(outputDir);   // TEST-com.example.MyTest.xml
suite.writeText(outputDir);  // com.example.MyTest.txt
```

Calling `testCase("name")` with the same name always returns the same `TestCaseReporter` instance, so you can add information to test cases incrementally from any thread.

### TestCaseReporter methods

```java
tc.setTime(double seconds)
tc.addStdout(String text)
tc.addStderr(String text)
tc.addFailure(String type, String message, String stackTrace)
tc.addError(String type, String message, String stackTrace)
```

### TestSuiteReporter methods

```java
suite.testCase(String name)              // get or create test case
suite.addProperty(String key, String value)
suite.toDocument()                       // JDOM Document
suite.writeXml(File outputDir)           // TEST-{name}.xml
suite.writeText(File outputDir)          // {name}.txt
```

## Output format

The XML output follows the standard Surefire/JUnit XML format understood by Jenkins, GitHub Actions, and other CI systems:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="com.example.MyTest" tests="3" failures="1" errors="1"
           skipped="0" time="0.085" timestamp="2026-02-18T18:00:00">
  <properties>
    <property name="env" value="ci" />
  </properties>
  <testcase name="testAdd" classname="com.example.MyTest" time="0.045">
    <failure type="AssertionError" message="expected 3 got 4">stack trace</failure>
    <system-out>computing...
</system-out>
  </testcase>
  <testcase name="testDivide" classname="com.example.MyTest" time="0.030">
    <error type="ArithmeticException" message="/ by zero">stack trace</error>
  </testcase>
  <testcase name="testSubtract" classname="com.example.MyTest" time="0.010" />
</testsuite>
```

## Maven dependency

```xml
<dependency>
    <groupId>com.walnutgeek</groupId>
    <artifactId>junit-jdom-reporter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Building

```bash
mvn clean verify
```

Requires Java 8+.

## License

Apache License 2.0
