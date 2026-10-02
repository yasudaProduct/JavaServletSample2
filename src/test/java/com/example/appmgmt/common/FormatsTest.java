package com.example.appmgmt.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class FormatsTest {

    @Test
    void amountRatioIsTruncatedToFourDecimals() {
        assertEquals(new BigDecimal("1.3333"), Formats.amountRatio(new BigDecimal("4000000"), new BigDecimal("3000000")));
        assertEquals(new BigDecimal("1.0000"), Formats.amountRatio(new BigDecimal("1000000"), new BigDecimal("1000000")));
        assertNull(Formats.amountRatio(new BigDecimal("1000"), BigDecimal.ZERO));
    }

    @Test
    void parseDateAcceptsSlashAndHyphenAndRejectsInvalid() {
        assertEquals(LocalDate.of(2026, 10, 1), Formats.parseDate("2026/10/01"));
        assertEquals(LocalDate.of(2026, 10, 1), Formats.parseDate("2026-10-01"));
        assertNull(Formats.parseDate("2026/02/30"));
        assertNull(Formats.parseDate("abc"));
        assertNull(Formats.parseDate(""));
    }

    @Test
    void parseAmountAllowsCommas() {
        assertEquals(new BigDecimal("1200000"), Formats.parseAmount("1,200,000"));
        assertNull(Formats.parseAmount("12a"));
        assertNull(Formats.parseAmount("12345678901234"));
        assertEquals("1,200,000", Formats.amount(new BigDecimal("1200000")));
        assertEquals("1.83", Formats.ratio(new BigDecimal("1.8333")));
    }

    @Test
    void passwordHashRoundTrip() {
        String h = PasswordHasher.hash("password");
        org.junit.jupiter.api.Assertions.assertTrue(PasswordHasher.verify("password", h));
        org.junit.jupiter.api.Assertions.assertFalse(PasswordHasher.verify("Password", h));
        org.junit.jupiter.api.Assertions.assertTrue(PasswordHasher.verify("password", "pbkdf2$65536$YXBwbWdtdC1zYW1wbGUhIQ==$9QdzmfHknk0USbaZaU9d+1w9vp9muDxLzYcXIp9Vkg8="));
    }

    @Test
    void tokenHashIsHex64() {
        String t = TokenUtil.newToken();
        org.junit.jupiter.api.Assertions.assertTrue(TokenUtil.looksLikeToken(t));
        assertEquals(64, TokenUtil.sha256Hex(t).length());
    }
}
