package com.jio.jiotalkie.util;

import android.annotation.SuppressLint;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.os.Build;
import android.util.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.concurrent.TimeUnit;


public class DateUtils {
    private static  final String TAG ="DateUtils";
    private static final String TODAY = "Today";
    private static final String YESTERDAY = "Yesterday";
   public static String dateFormatDateDivider = "d MMMM yyyy";
   public static String dateFormatServer = "yyyy-MM-dd HH:mm:ss";

    public static  String getStringDateFromLong(long receivedTime, String format) {
        SimpleDateFormat sdf = null;
        String formattedDate = new String();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            sdf = new SimpleDateFormat(format);
            formattedDate= sdf.format(new Date(receivedTime));
        }

        return formattedDate;
    }

    public static long getLongFromStringDate(String durationFrom) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormatServer);
        try {
            Date date = dateFormat.parse(durationFrom);
            return date.getTime();

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static String getLastSeenDateFormat(long receivedTime) {
        // Last seen date format example : 2024-06-27T16:07:24Z
        String date = getStringDateFromLong(receivedTime,dateFormatServer);
        // date = 2024-06-27 16:07:24
        String ld = date.concat("Z");
        // ld = 2024-06-27 16:07:24Z
        return ld.replace(" ", "T"); // 2024-06-27T16:07:24Z
    }

    public static  String CompareDate(String dateToCompareStr) {
        Calendar cal = null;
        long yesterdayTime = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -1); // Subtract one day
            yesterdayTime = cal.getTimeInMillis();
        }

        String yestrdayDate = getStringDateFromLong(yesterdayTime, dateFormatDateDivider);
        long todaysTime = System.currentTimeMillis();
        String todayDate = getStringDateFromLong(todaysTime, dateFormatDateDivider);
        if (dateToCompareStr.equals(todayDate)) {
            return TODAY;
        } else if (dateToCompareStr.equals(yestrdayDate)) {
            return YESTERDAY;
        }
        return getFormattedDate(dateToCompareStr);
    }

    public static String getFormattedDate(String inputDate) {
        Date date = new Date();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy");
            try {
                date = sdf.parse(inputDate);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        String formattedDate = addDaySuffix(date);
        return formattedDate;
    }

    public static String addDaySuffix(Date date) {
        SimpleDateFormat dayFormat = null;
        int day = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            dayFormat = new SimpleDateFormat("d");
            day = Integer.parseInt(dayFormat.format(date));
        }
        String suffix = "";
        if (day >= 11 && day <= 13) {
            suffix = "th";
        } else {
            switch (day % 10) {
                case 1:
                    suffix = "st";
                    break;
                case 2:
                    suffix = "nd";
                    break;
                case 3:
                    suffix = "rd";
                    break;
                default:
                    suffix = "th";
                    break;
            }
        }
        String finalDate = new String();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM");
            finalDate = day + suffix + " " + monthYearFormat.format(date);
        }
        return finalDate;
    }
    public static String covertTimeToText(String dataDate) {

        String time = null;
        String suffix = "ago";

        try {
            SimpleDateFormat dateFormat = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            }
            Date receivedTime = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                receivedTime = dateFormat.parse(dataDate);
            }

            Date currentTime = new Date();

            long dateDifference = currentTime.getTime() - receivedTime.getTime();

            long second = TimeUnit.MILLISECONDS.toSeconds(dateDifference);
            long minute = TimeUnit.MILLISECONDS.toMinutes(dateDifference);
            long hour = TimeUnit.MILLISECONDS.toHours(dateDifference);
            long day = TimeUnit.MILLISECONDS.toDays(dateDifference);

            if (second < 60) {
                time = "Just now";
            } else if (minute < 60) {
                time = minute + (minute == 1 ? " minute " : " minutes ") + suffix;
            } else if (hour < 24) {
                time = hour + (hour == 1 ? " hour " : " hours ") + suffix;
            } else if (day < 7) {
                time = day + (day == 1 ? " day " : " days ") + suffix;
            }

        } catch (ParseException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }

        return time;
    }

    public static String dateFormat(String receivedTime) {
        @SuppressLint("SimpleDateFormat")
        java.text.SimpleDateFormat simpleDateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        try {
            Date date = simpleDateFormat.parse(receivedTime);
            return simpleDateFormat.format(date);
        } catch (ParseException e) {
            Log.e(TAG, "Date format " + e.getMessage());
        }
        return receivedTime;
    }
}
