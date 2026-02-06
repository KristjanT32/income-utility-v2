package com.krisapps.incomeutility_v2;

import com.krisapps.incomeutility_v2.exceptions.NotInitializedException;
import com.krisapps.incomeutility_v2.subutilities.SubUtility;
import com.krisapps.incomeutility_v2.subutilities.SubUtilityType;
import com.krisapps.incomeutility_v2.subutilities.money_flow.MoneyFlowUtility;
import com.krisapps.incomeutility_v2.util.DataManager;
import com.krisapps.incomeutility_v2.util.PopupManager;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;
import java.util.logging.Level;

public class UtilityManager {
    public static UtilityManager instance;
    private static final HashMap<String, SubUtility> activeUtilities = new HashMap<>();

    private UtilityManager() {

    }

    public static UtilityManager create() {
        if (instance == null) {

            instance = new UtilityManager();
        }
        return instance;
    }

    public static UtilityManager getInstance() {
        if (instance == null) {
            throw new NotInitializedException("UtilityManager not initialized. You need to call UtilityManager#create before use.");
        }

        return instance;
    }

    private String generateId(SubUtilityType type) {
        long count = activeUtilities.values().stream().filter(util -> util.getType() == type).count();
        return String.format("%s_%d-%d", type.name().toLowerCase(), count, (int) (Math.random() * 1000));
    }

    private boolean checkAlreadyOpen(SubUtilityType type) {
        return activeUtilities.values().stream().anyMatch(util -> util.getType() == type);
    }

    private void startUtility(SubUtilityType subutility) {
        switch (subutility) {
            case PRICER -> {
                // TODO: Logic
            }
            case MONEY_IN_MONEY_OUT -> {
                MoneyFlowUtility utility = new MoneyFlowUtility();
                utility.setOnCloseCallback(this::unregister);
                try {
                    register(utility);
                } catch (IOException e) {
                    log(String.format("Failed to start %s: ", utility.getName()) + e.getMessage(), Level.SEVERE);
                    e.printStackTrace();
                }
            }
            default -> throw new IllegalArgumentException("Invalid subutility '" + subutility + "'");
        }
    }

    public void openUtility(SubUtilityType utility) {
        if (checkAlreadyOpen(utility)) {
            PopupManager.showConfirmation("Confirm open new instance",
                    "Another instance of " + utility.getDisplayName() + " has already been opened.\nAre you sure you wish to open another instance of this utility?",
                    new ButtonType("Yes, open", ButtonBar.ButtonData.APPLY),
                    new ButtonType("No, cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
            ).ifPresent(response -> {
                if (response.getButtonData() == ButtonBar.ButtonData.APPLY) {
                    startUtility(utility);
                }
            });
        } else {
            startUtility(utility);
        }
    }

    public void stopAll(SubUtilityType type) {
        if (type == SubUtilityType.ALL) {
            for (SubUtility util: activeUtilities.values()) {
                util.stop();
            }
        } else {
            for (SubUtility util: activeUtilities.values()) {
                if (util.getType() == type) {
                    util.stop();
                }
            }
        }
    }

    public void focusAll(SubUtilityType type) {
        if (type == SubUtilityType.ALL) {
            for (SubUtility util: activeUtilities.values()) {
                util.focusWindow();
            }
        } else {
            for (SubUtility util: activeUtilities.values()) {
                if (util.getType() == type) {
                    util.focusWindow();
                }
            }
        }
    }

    private void stopUtility(String processId) {
        Optional<SubUtility> util = Optional.ofNullable(activeUtilities.get(processId));
        util.ifPresentOrElse(SubUtility::stop, () -> {
            log(String.format("Attempted to stop non-existent utility with process id: %s", processId), Level.WARNING);
        });
    }

    private void register(SubUtility process) throws IOException {
        String processId = generateId(process.getType());
        while (activeUtilities.containsKey(processId)) {
            processId = generateId(process.getType());
        }

        activeUtilities.put(
                processId, process.start(processId)
        );

        log(String.format("Registered new active process '%s' (id: %s)", process.getName(), processId), Level.INFO);
    }

    private void unregister(String processId) {
        if (activeUtilities.containsKey(processId)) {

            SubUtility util = activeUtilities.get(processId);
            log(String.format("Unregistering active process for '%s' (%s)%n", util.getName(), processId), Level.INFO);

            activeUtilities.remove(processId);
        } else {
            log(String.format("Failed to unregister '%s' - process not found in registry.", processId), Level.WARNING);
        }
    }

    private static void log(String msg, Level level) {
        DataManager.log("[Process Registry] " + msg, level);
    }

}