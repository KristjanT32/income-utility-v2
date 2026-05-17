package com.krisapps.incomeutility_v2.subutilities.settings;

import com.krisapps.incomeutility_v2.subutilities.SubUtility;
import com.krisapps.incomeutility_v2.subutilities.SubUtilityType;

public class SettingsUtility extends SubUtility {
    public SettingsUtility() {
        super(SubUtilityType.SETTINGS, "layouts/windows/settings.fxml", "settings_96.png", new SettingsController(), 860, 510, true);
    }
}
