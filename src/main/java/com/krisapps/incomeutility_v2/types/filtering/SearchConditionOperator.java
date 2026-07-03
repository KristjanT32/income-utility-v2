package com.krisapps.incomeutility_v2.types.filtering;

import com.krisapps.incomeutility_v2.types.SearchMode;

public final class SearchConditionOperator extends SearchConditionElement {

    SearchMode mode;

    public SearchConditionOperator(SearchMode mode) {
        this.mode = mode;
    }

    @Override
    public String toSQL() {
        return switch (mode) {
            case AND -> "AND";
            case OR -> "OR";
        };
    }
}
