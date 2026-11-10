package org.communityapp.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a SQL script on statement terminators, ignoring comments and quoted text.
 */
public final class SqlScript {

    private SqlScript() {
    }

    public static List<String> statements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean lineComment = false;
        boolean blockComment = false;
        char quote = 0;

        for (int i = 0; i < script.length(); i++) {
            char character = script.charAt(i);
            char next = i + 1 < script.length() ? script.charAt(i + 1) : 0;

            if (lineComment) {
                if (character == '\n') {
                    lineComment = false;
                    current.append(character);
                }
                continue;
            }
            if (blockComment) {
                if (character == '*' && next == '/') {
                    blockComment = false;
                    i++;
                } else if (character == '\n') {
                    current.append(character);
                }
                continue;
            }
            if (quote != 0) {
                current.append(character);
                if (character == '\\' && next != 0) {
                    current.append(next);
                    i++;
                } else if (character == quote) {
                    quote = 0;
                }
                continue;
            }
            if (character == '-' && next == '-') {
                lineComment = true;
                i++;
            } else if (character == '/' && next == '*') {
                blockComment = true;
                i++;
            } else if (character == '\'' || character == '"' || character == '`') {
                quote = character;
                current.append(character);
            } else if (character == ';') {
                add(statements, current);
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        add(statements, current);
        return statements;
    }

    private static void add(List<String> statements, StringBuilder sql) {
        String statement = sql.toString().trim();
        if (!statement.isEmpty()) {
            statements.add(statement);
        }
    }
}
