package com.krisapps.incomeutility_v2.subutilities.pantry;

import com.krisapps.incomeutility_v2.subutilities.SubUtility;
import com.krisapps.incomeutility_v2.subutilities.SubUtilityType;

public class PantryUtility extends SubUtility {
    public PantryUtility() {
        super(SubUtilityType.PANTRY, "layouts/windows/pantry.fxml", "pantry_96.png", new PantryController(), 1150, 820, true);
    }
}
