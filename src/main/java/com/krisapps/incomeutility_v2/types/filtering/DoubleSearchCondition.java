package com.krisapps.incomeutility_v2.types.filtering;

import java.security.InvalidParameterException;

public class DoubleSearchCondition extends SearchCondition {
    public enum FilterMode {
        LESS_THAN("Less than (<)"),
        LESS_THAN_OR_EQUAL_TO("Less than or equal to (<=)"),
        EQUAL_TO("Equal to (=)"),
        BETWEEN("Between (both sides inclusive)"),
        GREATER_THAN("Greater than (>)"),
        GREATER_THAN_OR_EQUAL_TO("Greater than (>=)");
        private String displayName;

        FilterMode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    double amount1;
    double amount2;
    FilterMode mode;

    public DoubleSearchCondition(String filterColumn, FilterMode mode, double amount1) {
        if (mode.equals(FilterMode.BETWEEN)) {
            throw new InvalidParameterException("Cannot create new search condition for filter type BETWEEN with one parameter!");
        }
        this.filterColumn = filterColumn;
        this.amount1 = amount1;
        this.mode = mode;
    }

    public DoubleSearchCondition(String filterColumn, FilterMode mode, double amount1, double amount2) {
        this.filterColumn = filterColumn;
        this.amount1 = amount1;
        this.amount2 = amount2;
        this.mode = mode;
    }

    @Override
    public String toSQL() {
        return switch (mode) {
            case LESS_THAN -> "%column% < %amount%"
                    .replace("%column%", filterColumn)
                    .replace("%amount%", String.valueOf(amount1));
            case LESS_THAN_OR_EQUAL_TO -> "%column% <= %amount%"
                    .replace("%column%", filterColumn)
                    .replace("%amount%", String.valueOf(amount1));
            case EQUAL_TO -> "%column% = %amount%"
                    .replace("%column%", filterColumn)
                    .replace("%amount%", String.valueOf(amount1));
            case BETWEEN -> "%column% BETWEEN %bound1% AND %bound2%"
                    .replace("%column%", filterColumn)
                    .replace("%bound1%", String.valueOf(amount1))
                    .replace("%bound2%", String.valueOf(amount2));
            case GREATER_THAN -> "%column% > %amount%"
                    .replace("%column%", filterColumn)
                    .replace("%amount%", String.valueOf(amount1));
            case GREATER_THAN_OR_EQUAL_TO -> "%column% >= %amount%"
                    .replace("%column%", filterColumn)
                    .replace("%amount%", String.valueOf(amount1));
        };
    }
}
