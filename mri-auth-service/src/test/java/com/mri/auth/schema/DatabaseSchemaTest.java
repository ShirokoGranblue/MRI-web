package com.mri.auth.schema;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseSchemaTest {
    private static final Pattern TABLE_PATTERN = Pattern.compile(
            "CREATE TABLE IF NOT EXISTS\\s+(\\w+)\\s*\\((.*?)\\);",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern KEY_PATTERN = Pattern.compile(
            "(?:UNIQUE\\s+)?KEY\\s+(\\w+)\\s*\\(([^)]+)\\)",
            Pattern.CASE_INSENSITIVE);

    @Test
    void tableIndexesReferenceColumnsDeclaredInTheSameTable() throws IOException {
        String schema = Files.readString(schemaPath());
        Matcher tables = TABLE_PATTERN.matcher(schema);
        List<String> missingColumns = new ArrayList<>();

        while (tables.find()) {
            String tableName = tables.group(1);
            String body = tables.group(2);
            Set<String> columns = declaredColumns(body);
            Matcher keys = KEY_PATTERN.matcher(body);
            while (keys.find()) {
                String keyName = keys.group(1);
                for (String column : keyColumns(keys.group(2))) {
                    if (!columns.contains(column)) {
                        missingColumns.add(tableName + "." + keyName + " -> " + column);
                    }
                }
            }
        }

        assertThat(missingColumns).isEmpty();
    }

    @Test
    void schemaSupportsPatientAccountBindingAndUtf8SafeSystemNames() throws IOException {
        String schema = Files.readString(schemaPath());

        assertThat(schema).contains("account_username VARCHAR(64)");
        assertThat(schema).contains("UNIQUE KEY uk_patient_account_username (account_username)");
        assertThat(schema).contains("'PATIENT', CONVERT(0xE682A3E88085 USING utf8mb4)");
        assertThat(schema).contains("CONVERT(0xE7B3BBE7BB9FE7AEA1E79086E59198 USING utf8mb4)");
    }

    @Test
    void existingDatabaseMigrationIsIdempotent() throws IOException {
        Path migration = schemaPath().resolveSibling("02-patient-account-migration.sql");

        assertThat(Files.readString(migration))
                .contains("information_schema.COLUMNS")
                .contains("information_schema.STATISTICS")
                .contains("'PATIENT', CONVERT(0xE682A3E88085 USING utf8mb4)")
                .contains("UPDATE sys_role")
                .contains("UPDATE sys_user")
                .contains("CONVERT(0xE7B3BBE7BB9FE7AEA1E79086E59198 USING utf8mb4)");
    }

    private static Path schemaPath() {
        Path rootRelative = Path.of("docker", "mysql", "init", "01-schema.sql");
        if (Files.exists(rootRelative)) {
            return rootRelative;
        }
        return Path.of("..", "docker", "mysql", "init", "01-schema.sql");
    }

    private static Set<String> declaredColumns(String tableBody) {
        Set<String> columns = new HashSet<>();
        for (String rawLine : tableBody.split("\\R")) {
            String line = rawLine.strip();
            if (line.isEmpty()) {
                continue;
            }
            String firstToken = line.split("\\s+", 2)[0].replace("`", "").replace(",", "");
            String normalized = firstToken.toUpperCase(Locale.ROOT);
            if (!Set.of("PRIMARY", "UNIQUE", "KEY", "CONSTRAINT").contains(normalized)) {
                columns.add(firstToken);
            }
        }
        return columns;
    }

    private static List<String> keyColumns(String keyBody) {
        List<String> columns = new ArrayList<>();
        for (String column : keyBody.split(",")) {
            columns.add(column.strip().replace("`", ""));
        }
        return columns;
    }
}
