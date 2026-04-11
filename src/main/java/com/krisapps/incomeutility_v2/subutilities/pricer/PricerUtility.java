package com.krisapps.incomeutility_v2.subutilities.pricer;

import com.krisapps.incomeutility_v2.subutilities.SubUtility;
import com.krisapps.incomeutility_v2.subutilities.SubUtilityController;
import com.krisapps.incomeutility_v2.subutilities.SubUtilityType;
import org.jetbrains.annotations.Nullable;

public class PricerUtility extends SubUtility {
    public PricerUtility() {
        super(SubUtilityType.PRICER, "layouts/windows/pricer.fxml", "pricer_96.png", new PricerController(), 860, 510, true);
    }
}
