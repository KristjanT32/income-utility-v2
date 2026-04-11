package com.krisapps.incomeutility_v2.subutilities.money_flow;

import com.krisapps.incomeutility_v2.subutilities.SubUtility;
import com.krisapps.incomeutility_v2.subutilities.SubUtilityType;
import com.krisapps.incomeutility_v2.types.fiscal.Account;

public class MoneyFlowUtility extends SubUtility {
    public MoneyFlowUtility() {
        super(SubUtilityType.MONEY_IN_MONEY_OUT, "layouts/windows/money-flow.fxml", "money_in_money_out_96.png", new MoneyFlowUtilityController(), 860, 510, true);
    }
}
