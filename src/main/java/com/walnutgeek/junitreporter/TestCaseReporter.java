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
    public synchronized double getTime() { return time; }

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
