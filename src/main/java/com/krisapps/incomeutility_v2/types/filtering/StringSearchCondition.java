package com.krisapps.incomeutility_v2.types.filtering;

public class StringSearchCondition extends SearchCondition {
    public enum FilterMode {
        IS("Is exactly"),
        CONTAINS("Contains substring"),
        STARTS_WITH("Starts with substring"),
        ENDS_WITH("Ends with substring");
        private final String description;

        FilterMode(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }


    String filterString;
    FilterMode mode;

    public StringSearchCondition(String filterColumn, FilterMode mode, String filterString) {
        this.filterColumn = filterColumn;
        this.filterString = filterString;
        this.mode = mode;
    }

    @Override
    public String toSQL() {
        return switch (mode) {
            case IS -> "%column% = \"%substring%\""
                    .replace("%column%", filterColumn)
                    .replace("%substring%", filterString);
            case CONTAINS -> "%column% LIKE \"%%substring%%\""
                    .replace("%column%", filterColumn)
                    .replace("%substring%", filterString);
            case STARTS_WITH -> "%column% LIKE \"%substring%%\""
                    .replace("%column%", filterColumn)
                    .replace("%substring%", filterString);
            case ENDS_WITH -> "%column% LIKE \"%%substring%\""
                    .replace("%column%", filterColumn)
                    .replace("%substring%", filterString);
        };
    }
}
