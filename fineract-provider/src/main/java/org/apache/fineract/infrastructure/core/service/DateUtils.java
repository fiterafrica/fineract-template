/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.infrastructure.core.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenant;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.filters.FilterElement;
import org.apache.fineract.organisation.teller.util.DateRange;

public final class DateUtils {

    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    public static final String DEFAULT_DATETIME_FORMAT = DEFAULT_DATE_FORMAT + " HH:mm:ss";
    public static final DateTimeFormatter DEFAULT_DATETIME_FORMATTER = new DateTimeFormatterBuilder().appendPattern(DEFAULT_DATETIME_FORMAT)
            .toFormatter();
    public static final DateTimeFormatter DEFAULT_DATE_FORMATER = new DateTimeFormatterBuilder().appendPattern(DEFAULT_DATE_FORMAT)
            .toFormatter();

    private DateUtils() {

    }

    public static ZoneId getSystemZoneId() {
        return ZoneId.systemDefault();
    }

    public static ZoneId getDateTimeZoneOfTenant() {
        final FineractPlatformTenant tenant = ThreadLocalContextUtil.getTenant();
        return ZoneId.of(tenant.getTimezoneId());
    }

    public static TimeZone getTimeZoneOfTenant() {
        final FineractPlatformTenant tenant = ThreadLocalContextUtil.getTenant();
        TimeZone zone = null;
        if (tenant != null) {
            zone = TimeZone.getTimeZone(tenant.getTimezoneId());
        }
        return zone;
    }

    public static LocalDate getLocalDateOfTenant() {
        final ZoneId zone = getDateTimeZoneOfTenant();
        return LocalDate.now(zone);
    }

    public static LocalDateTime getLocalDateTimeOfTenant() {
        final ZoneId zone = getDateTimeZoneOfTenant();
        return LocalDateTime.now(zone).truncatedTo(ChronoUnit.SECONDS);
    }

    public static OffsetDateTime getOffsetDateTimeOfTenant() {
        final ZoneId zone = getDateTimeZoneOfTenant();
        return OffsetDateTime.now(zone).truncatedTo(ChronoUnit.SECONDS);
    }

    public static LocalDateTime getLocalDateTimeOfSystem() {
        return LocalDateTime.now(ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS);
    }

    public static LocalDate parseLocalDate(final String stringDate, final String pattern, final ZoneId zoneId) {
        try {
            DateTimeFormatter dateStringFormat;
            if (zoneId != null) {
                dateStringFormat = DateTimeFormatter.ofPattern(pattern).withZone(zoneId);
            } else {
                dateStringFormat = DateTimeFormatter.ofPattern(pattern);
            }
            final ZonedDateTime dateTime = ZonedDateTime.parse(stringDate, dateStringFormat);
            return dateTime.toLocalDate();
        } catch (final IllegalArgumentException e) {
            final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
            final ApiParameterError error = ApiParameterError.parameterError("validation.msg.invalid.date.pattern",
                    "The parameter `date` (value: " + stringDate + ") is invalid w.r.t. pattern `" + pattern + "`", "date", stringDate,
                    pattern);
            dataValidationErrors.add(error);
            throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                    dataValidationErrors, e);
        }
    }

    public static LocalDate parseLocalDate(final String stringDate, final String pattern) {
        return parseLocalDate(stringDate, pattern, getDateTimeZoneOfTenant());
    }

    public static boolean isDateInTheFuture(final LocalDate localDate) {
        return localDate.isAfter(getLocalDateOfTenant());
    }

    public static LocalDate getBusinessLocalDate() {
        return ThreadLocalContextUtil.getBusinessDate();
    }

    public static DateTimeFormatter getDefaultFormatter() {
        return DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT);
    }

    public static Date asDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    }

    public static boolean isSameLocalDate(LocalDate firstDate, LocalDate secondDate) {
        return org.apache.commons.lang3.time.DateUtils.isSameDay(asDate(firstDate), asDate(secondDate));
    }

    public static DateRange getDateRange(LocalDate today, FilterElement filterElement, String days) {
        switch (filterElement) {
            case ON:
            case TODAY:
                return DateRange.of(today, today);
            case THIS_WEEK:
                LocalDate startOfWeek = today
                        .with(TemporalAdjusters.previousOrSame(LocalDateTime.now(ZoneId.systemDefault()).getDayOfWeek()))
                        .with(LocalTime.MIN);
                LocalDate endOfWeek = startOfWeek.plusDays(6).with(LocalTime.MAX);
                return DateRange.of(startOfWeek, endOfWeek);
            case LAST_DAYS:
                LocalDate endOfDays = today;
                LocalDate startOfDay = endOfDays.minusDays(Integer.parseInt(days));
                return DateRange.of(startOfDay, endOfDays);
            case THIS_MONTH:
                LocalDate startOfMonth = today.withDayOfMonth(1).with(LocalTime.MIN);
                LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1).with(LocalTime.MAX);
                return DateRange.of(startOfMonth, endOfMonth);
            case THIS_YEAR:
                LocalDate startOfYear = today.withDayOfYear(1).with(LocalTime.MIN);
                LocalDate endOfYear = startOfYear.plusYears(1).minusDays(1).with(LocalTime.MAX);
                return DateRange.of(startOfYear, endOfYear);
            default:
                return null;
        }
    }

    public static LocalDate convertToLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static LocalDateTime convertToLocalDateTime(Date date) {
        return date.toInstant().atZone(getDateTimeZoneOfTenant()).toLocalDateTime();
    }
}
