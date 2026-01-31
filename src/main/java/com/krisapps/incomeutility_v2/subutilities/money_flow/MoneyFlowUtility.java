package com.krisapps.incomeutility_v2.subutilities.money_flow;

import com.krisapps.incomeutility_v2.subutilities.SubUtility;
import com.krisapps.incomeutility_v2.subutilities.SubUtilityType;

public class MoneyFlowUtility extends SubUtility {

    public MoneyFlowUtility() {
        super(SubUtilityType.MONEY_IN_MONEY_OUT, "layouts/windows/money-flow.fxml", new MoneyFlowUtilityController(), 860, 510, true);
    }

    @Override
    public void initialize() {

    }
}
