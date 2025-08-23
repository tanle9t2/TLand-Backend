package com.tanle.tland.payment_service.utils;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateUtils {
    protected static final SimpleDateFormat ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    protected static final SimpleDateFormat VNPAY_DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
    public static final Calendar VN_CALENDAR = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));

    public static Date parseISO(String date) {
        try {
            return ISO_DATE_FORMAT.parse(date);
        } catch (Exception e) {
            return null;
        }
    }

    public static String convertDateEmail(Long timestamp) {

        LocalDateTime dateTime = Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();


        DateTimeFormatter monthYearFormatter = DateTimeFormatter.ofPattern("MMM, yyyy");

        int day = dateTime.getDayOfMonth();
        String dayWithSuffix = day + getDaySuffix(day);
        String formatted = dateTime.format(DateTimeFormatter.ofPattern("MMM")) + ", " + dayWithSuffix + " " + dateTime.getYear();

        return formatted;
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
        return LocalDate.parse(date);
    }

    public static String getVnTime() {
        return VNPAY_DATE_FORMAT.format(VN_CALENDAR.getTime());
    }

    public static String formatVnTime(Calendar calendar) {
        return VNPAY_DATE_FORMAT.format(calendar.getTime());
    }
}
