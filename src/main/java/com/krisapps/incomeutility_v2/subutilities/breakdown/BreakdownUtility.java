package com.krisapps.incomeutility_v2.subutilities.breakdown;

import com.krisapps.incomeutility_v2.subutilities.SubUtility;
import com.krisapps.incomeutility_v2.subutilities.SubUtilityType;

public class BreakdownUtility extends SubUtility {
    public BreakdownUtility() {
        super(SubUtilityType.BREAKDOWN, "layouts/windows/breakdown.fxml", "fiscal_breakdown_96.png", new BreakdownController(), 1100, 670, true);
    }
}
