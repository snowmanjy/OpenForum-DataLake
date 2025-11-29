package com.openforum.datalake.dto;

import java.time.LocalDate;

public record DailyActiveUsersPoint(LocalDate date, long count) {
    public DailyActiveUsersPoint(java.sql.Date date, Long count) {
        this(date.toLocalDate(), count);
    }
}
