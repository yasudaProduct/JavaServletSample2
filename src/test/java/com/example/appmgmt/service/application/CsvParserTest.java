package com.example.appmgmt.service.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class CsvParserTest {

    @Test
    void parsesQuotedFieldsWithCommasAndEscapedQuotes() {
        List<List<String>> r = CsvParser.parse("a,b,c\r\n1,\"x, y\",\"he said \"\"hi\"\"\"\n2,,\n");
        assertEquals(3, r.size());
        assertEquals(List.of("a", "b", "c"), r.get(0));
        assertEquals(List.of("1", "x, y", "he said \"hi\""), r.get(1));
        assertEquals(List.of("2", "", ""), r.get(2));
    }

    @Test
    void parsesNewlineInsideQuotes() {
        List<List<String>> r = CsvParser.parse("a,\"line1\nline2\"\n");
        assertEquals(1, r.size());
        assertEquals("line1\nline2", r.get(0).get(1));
    }
}
