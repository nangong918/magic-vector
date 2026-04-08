package com.openapi;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * @author 13225
 * @date 2025/10/15 14:43
 */
public class JsonTests {

    public static void main(String[] args) {
        String date1 = "-999999999-01-01T00:00:00";
        String date2 = "2000-09-18T00:00:00"; // 修正为正确的格式

        LocalDateTime localDateTime1 = parseToLocalDateTime(date1);
        LocalDateTime localDateTime2 = parseToLocalDateTime(date2);

        if (localDateTime1 != null && localDateTime2 != null) {
            compareDates(localDateTime1, localDateTime2);
        } else {
            System.out.println("One of the dates is invalid.");
        }
    }

    public static LocalDateTime parseToLocalDateTime(String dateStr) {
        try {
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            System.err.println("Date parsing error: " + e.getMessage());
            return null; // 或者根据需求返回一个默认值
        }
    }

    public static void compareDates(LocalDateTime date1, LocalDateTime date2) {
        if (date1.isBefore(date2)) {
            System.out.println("Date 1 is before Date 2");
        } else if (date1.isAfter(date2)) {
            System.out.println("Date 1 is after Date 2");
        } else {
            System.out.println("Both dates are equal");
        }
    }
}
