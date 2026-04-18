package com.tanle.tland.payment_service.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateUtils {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final DateTimeFormatter ISO_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter VNPAY_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static String getVnTime() {
        return ZonedDateTime.now(VN_ZONE).format(VNPAY_DATE_FORMAT);
    }

    public static String formatVnTime(ZonedDateTime dateTime) {
        return dateTime.withZoneSameInstant(VN_ZONE).format(VNPAY_DATE_FORMAT);
    }

    public static String convertDateEmail(Long timestamp) {
        LocalDateTime dateTime = Instant.ofEpochMilli(timestamp)
                .atZone(VN_ZONE)
                .toLocalDateTime();

        int day = dateTime.getDayOfMonth();
        String dayWithSuffix = day + getDaySuffix(day);

        return dateTime.format(DateTimeFormatter.ofPattern("MMM"))
                + ", " + dayWithSuffix + " " + dateTime.getYear();
    }

    private static String getDaySuffix(int day) {
        if (day >= 11 && day <= 13) return "th";
        return switch (day % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }

    public static long getDiffInDays(LocalDate date1, LocalDate date2) {
        return ChronoUnit.DAYS.between(date1, date2);
    }

    public static LocalDate parse(String date) {
        return LocalDate.parse(date, ISO_DATE_FORMAT);
    }
}