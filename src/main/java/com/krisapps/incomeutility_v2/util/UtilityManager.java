package com.krisapps.incomeutility_v2.util;

import com.krisapps.incomeutility_v2.exceptions.NotInitializedException;
import com.krisapps.incomeutility_v2.subutilities.SubUtility;
import com.krisapps.incomeutility_v2.subutilities.SubUtilityType;
import com.krisapps.incomeutility_v2.subutilities.breakdown.BreakdownController;
import com.krisapps.incomeutility_v2.subutilities.breakdown.BreakdownUtility;
import com.krisapps.incomeutility_v2.subutilities.money_flow.MoneyFlowUtility;
import com.krisapps.incomeutility_v2.subutilities.pricer.PricerUtility;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;
import java.util.logging.Level;

public class UtilityManager {
    private static final HashMap<String, SubUtility> activeUtilities = new HashMap<>();
    public static UtilityManager instance;

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

    private static void log(String msg, Level level) {
        DataManager.log("[Process Registry] " + msg, level);
    }

    private String generateId(SubUtilityType type) {
        long count = activeUtilities.values().stream().filter(util -> util.getType() == type).count();
        return String.format("%s_%d-%d", type.name().toLowerCase(), count, (int) (Math.random() * 1000));
    }

    private boolean checkAlreadyOpen(SubUtilityType type) {
        return activeUtilities.values().stream().anyMatch(util -> util.getType() == type);
    }

    private void startUtility(SubUtilityType subutility) {
        String processId = getProcessIdFor(subutility);

        SubUtility utility = null;
        switch (subutility) {
            case PRICER -> {
                utility = new PricerUtility();
            }
            case BREAKDOWN -> {
                utility = new BreakdownUtility();
            }
            case MONEY_IN_MONEY_OUT -> {
                utility = new MoneyFlowUtility();
            }

            default -> throw new IllegalArgumentException("Invalid subutility '" + subutility + "'");
        }

        utility.setProcessId(processId);
        utility.setOnCloseCallback(this::unregister);
        try {
            register(utility, processId);
        } catch (IOException e) {
            log(String.format("Failed to start %s: ", utility.getName()) + e.getMessage(), Level.SEVERE);
            e.printStackTrace();
        }
    }

    public boolean hasOpenUtilities() {
        return !activeUtilities.isEmpty();
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
            for (SubUtility util : activeUtilities.values()) {
                util.stop();
            }
        } else {
            for (SubUtility util : activeUtilities.values()) {
                if (util.getType() == type) {
                    util.stop();
                }
            }
        }
    }

    public void focusAll(SubUtilityType type) {
        if (type == SubUtilityType.ALL) {
            for (SubUtility util : activeUtilities.values()) {
                util.focusWindow();
            }
        } else {
            for (SubUtility util : activeUtilities.values()) {
                if (util.getType() == type) {
                    util.focusWindow();
                }
            }
        }
    }

    public void stopUtility(String processId) {
        Optional<SubUtility> util = Optional.ofNullable(activeUtilities.get(processId));
        util.ifPresentOrElse(SubUtility::stop, () -> {
            log(String.format("Attempted to stop non-existent utility with process id: %s", processId), Level.WARNING);
        });
    }

    private String getProcessIdFor(SubUtilityType processType) {
        String processId = generateId(processType);
        while (activeUtilities.containsKey(processId)) {
            processId = generateId(processType);
        }
        return processId;
    }

    private void register(SubUtility process, String processId) throws IOException {
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

}