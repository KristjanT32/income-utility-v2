package com.krisapps.incomeutility_v2.types.filtering;

import java.security.InvalidParameterException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class DateTimeSearchCondition extends SearchCondition {

    public enum FilterMode {
        /**
         * Everything within a range (both sides inclusive)
         */
        IS_BETWEEN("All transactions between (inclusive)"),

        /**
         * Everything before the supplied date (inclusive)
         */
        IS_BEFORE("All transactions before (inclusive)"),

        /**
         * Everything after the supplied date (inclusive)
         */
        IS_AFTER("All transactions after (inclusive)"),

        /**
         * Everything on the supplied date
         */
        IS("All transactions on");
        private final String displayName;

        FilterMode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    FilterMode mode;
    LocalDateTime dateTime1;
    LocalDateTime dateTime2;

    public DateTimeSearchCondition(String column, FilterMode mode, LocalDateTime dateTime1) {
        if (mode.equals(FilterMode.IS_BETWEEN)) {
            throw new InvalidParameterException("Cannot create date search condition for filter mode RANGE with one parameter!");
        }

        this.filterColumn = column;
        this.mode = mode;
        this.dateTime1 = dateTime1;
    }

    public DateTimeSearchCondition(String column, FilterMode mode, LocalDateTime dateTime1, LocalDateTime dateTime2) {
        this.filterColumn = column;
        this.mode = mode;
        this.dateTime1 = dateTime1;
        this.dateTime2 = dateTime2;
    }

    @Override
    public String toSQL() {
        return switch (mode) {
            case IS_BETWEEN -> "%filter% BETWEEN \"%date1%\" AND \"%date2%\""
                    .replace("%filter%", filterColumn)
                    .replace("%date1%", Timestamp.from(dateTime1.atZone(ZoneId.systemDefault()).toInstant()).toString())
                    .replace("%date2%", Timestamp.from(dateTime2.atZone(ZoneId.systemDefault()).toInstant()).toString());
            case IS_BEFORE -> "%filter% <= \"%date1%\""
                    .replace("%filter%", filterColumn)
                    .replace("%date1%", Timestamp.from(dateTime1.atZone(ZoneId.systemDefault()).toInstant()).toString());
            case IS_AFTER -> "%filter% >= \"%date1%\""
                    .replace("%filter%", filterColumn)
                    .replace("%date1%", Timestamp.from(dateTime1.atZone(ZoneId.systemDefault()).toInstant()).toString());
            case IS -> "DATE(%filter%) = DATE(\"%date1%\")"
                    .replace("%filter%", filterColumn)
                    .replace("%date1%", Timestamp.from(dateTime1.atZone(ZoneId.systemDefault()).toInstant()).toString());
        };
    }
}
