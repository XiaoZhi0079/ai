package com.example.ai.Tool;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DateTimeTools {

    @Tool(description = "获取系统时间 精确到毫秒")
    String getCurrentDateTime() {
        log.debug("获取系统时间类被调用");
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
    }

    @Tool(name = "alarm", description = "Set a user alarm for the given time")
    void setAlarm(String time) {
        LocalDateTime alarmTime = LocalDateTime.parse(time, DateTimeFormatter.ISO_DATE_TIME);
        log.info("闹钟将会在 {} 响起来", alarmTime);
    }
}