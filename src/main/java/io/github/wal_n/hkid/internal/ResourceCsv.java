package io.github.wal_n.hkid.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Minimal UTF-8 CSV resource reader for the project's bundled reference data.
 *
 * <p>This is an internal API. It intentionally supports the subset used by the
 * project data files: quoted fields, escaped quotes, comments, and one record
 * per line.</p>
 */
public final class ResourceCsv {
    private ResourceCsv() {
        throw new AssertionError("ResourceCsv cannot be instantiated");
    }

    public static List<Row> readRows(String resourceName, String... expectedHeader) {
        if (resourceName == null || resourceName.trim().isEmpty()) {
            throw new IllegalArgumentException("CSV resource name cannot be empty");
        }

        InputStream inputStream = ResourceCsv.class.getClassLoader().getResourceAsStream(resourceName);
        if (inputStream == null) {
            throw new IllegalStateException("CSV resource not found: " + resourceName);
        }

        List<String> header = Arrays.asList(expectedHeader);
        List<Row> rows = new ArrayList<>();
        boolean headerRead = false;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                List<String> fields = parseLine(line, resourceName, lineNumber);
                if (!headerRead) {
                    if (!fields.equals(header)) {
                        throw new IllegalStateException(
                                "Unexpected CSV header in " + resourceName + ": " + fields);
                    }
                    headerRead = true;
                    continue;
                }

                if (fields.size() != header.size()) {
                    throw new IllegalStateException(
                            "Expected " + header.size() + " fields in " + resourceName
                                    + " at line " + lineNumber + ", found " + fields.size());
                }
                rows.add(new Row(lineNumber, fields));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load CSV resource: " + resourceName, e);
        }

        if (!headerRead) {
            throw new IllegalStateException("CSV resource has no header: " + resourceName);
        }
        return Collections.unmodifiableList(rows);
    }

    private static List<String> parseLine(String line, String resourceName, int lineNumber) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            if (current == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ',' && !quoted) {
                fields.add(field.toString().trim());
                field.setLength(0);
            } else {
                field.append(current);
            }
        }

        if (quoted) {
            throw new IllegalStateException(
                    "Unclosed quoted field in " + resourceName + " at line " + lineNumber);
        }
        fields.add(field.toString().trim());
        return fields;
    }

    public static final class Row {
        private final int lineNumber;
        private final List<String> fields;

        private Row(int lineNumber, List<String> fields) {
            this.lineNumber = lineNumber;
            this.fields = Collections.unmodifiableList(fields);
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public List<String> getFields() {
            return fields;
        }
    }
}
