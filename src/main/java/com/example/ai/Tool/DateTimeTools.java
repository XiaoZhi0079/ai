package com.example.ai.Tool;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;



@Component
public class DateTimeTools {

    private static final DateTimeFormatter FORMATTER_TO_SECOND = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");





    @Tool(description = "获取系统时间 精确到毫秒")
    String getCurrentDateTime() {
        System.out.println("获取系统时间类被调用");
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
    }




    @Tool(name = "alarm", description = "Set a user alarm for the given time")
    void setAlarm(String time) {
        LocalDateTime alarmTime = LocalDateTime.parse(time, DateTimeFormatter.ISO_DATE_TIME);
        System.out.println("闹钟将会在 " + alarmTime+"响起来");
    }

}