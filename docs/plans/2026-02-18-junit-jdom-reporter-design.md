# junit-jdom-reporter Design

Little helper that creates JUnit compatible XML using JDOM.

## Overview

A standalone, lightweight Java library that produces JUnit/Surefire-compatible XML and text reports using JDOM. No JUnit runtime dependency. Minimal dependencies. Thread-safe.

## API

Two public classes in `com.walnutgeek.junitreporter`:

### TestSuiteReporter

Represents a `<testsuite>`. Constructed with a suite name.

**State:**
- `name` (String, required at construction)
- `properties` (thread-safe map of key-value pairs)
- `testCases` (ConcurrentHashMap<String, TestCaseReporter>)
- `timestamp` (set at construction, ISO 8601)

**Methods:**
- `testCase(String name)` — returns existing or lazily creates new TestCaseReporter
- `addProperty(String name, String value)`
- `toDocument()` — builds JDOM `Document` representing the Surefire XML
- `writeXml(File outputDir)` — writes `TEST-{name}.xml`
- `writeText(File outputDir)` — writes `{name}.txt`

### TestCaseReporter

Represents a `<testcase>`. Created lazily via `TestSuiteReporter.testCase(name)`.

**State:**
- `name`, `className` (derived from suite name)
- Lists of entries: stdout, stderr, failures, errors
- `time` (double, default 0, set explicitly by user)

**Methods:**
- `addStdout(String text)`
- `addStderr(String text)`
- `addFailure(String type, String message, String stackTrace)`
- `addError(String type, String message, String stackTrace)`
- `setTime(double seconds)`

### Thread Safety

- `TestSuiteReporter` uses `ConcurrentHashMap.computeIfAbsent` for lazy test case creation
- `TestCaseReporter` uses `synchronized` on its entry lists
- Properties map is thread-safe

## XML Output Format

Standard Surefire-style JUnit XML:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="com.example.MyTest" tests="3" failures="1" errors="1"
           skipped="0" time="0.123" timestamp="2026-02-18T18:00:00">
  <properties>
    <property name="env" value="ci"/>
  </properties>
  <testcase name="testAdd" classname="com.example.MyTest" time="0.045">
    <failure type="AssertionError" message="expected 3 got 4">stack trace here</failure>
    <system-out>computing...</system-out>
  </testcase>
  <testcase name="testDivide" classname="com.example.MyTest" time="0.030">
    <error type="ArithmeticException" message="/ by zero">stack trace here</error>
  </testcase>
  <testcase name="testSubtract" classname="com.example.MyTest" time="0.010"/>
</testsuite>
```

Computed at write time:
- `tests` — count of test cases
- `failures` — count of test cases with at least one failure
- `errors` — count of test cases with at least one error
- `skipped` — always 0
- `time` — sum of all test case times (user-provided via `setTime`)

## Text Output Format

Surefire-style plain text, file named `{name}.txt`:

```
-------------------------------------------------------------------------------
Test set: com.example.MyTest
-------------------------------------------------------------------------------
Tests run: 3, Failures: 1, Errors: 1, Skipped: 0, Time elapsed: 0.123 s

testAdd  Time elapsed: 0.045 s  <<< FAILURE!
AssertionError: expected 3 got 4
stack trace here

testDivide  Time elapsed: 0.030 s  <<< ERROR!
ArithmeticException: / by zero
stack trace here
```

Only failed/errored test cases listed in body. Passing tests counted in summary only.

## Project Structure

```
junit-jdom-reporter/
├── pom.xml
├── LICENSE
└── src/
    ├── main/java/com/walnutgeek/junitreporter/
    │   ├── TestSuiteReporter.java
    │   └── TestCaseReporter.java
    └── test/java/com/walnutgeek/junitreporter/
        └── TestSuiteReporterTest.java
```

## Dependencies

- **Compile:** `org.jdom:jdom2:2.0.5`
- **Test:** `junit:junit:4.13.2` (scope test)

## Build

- Maven, Java 8 source/target
- `groupId`: `com.walnutgeek`
- `artifactId`: `junit-jdom-reporter`
