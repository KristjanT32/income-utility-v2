package com.krisapps.incomeutility_v2.util;

import com.krisapps.incomeutility_v2.exceptions.NotInitializedException;
import com.krisapps.incomeutility_v2.subutilities.SubUtility;
import com.krisapps.incomeutility_v2.subutilities.SubUtilityType;
import com.krisapps.incomeutility_v2.subutilities.breakdown.BreakdownUtility;
import com.krisapps.incomeutility_v2.subutilities.money_flow.MoneyFlowUtility;
import com.krisapps.incomeutility_v2.subutilities.pantry.PantryUtility;
import com.krisapps.incomeutility_v2.subutilities.pricer.PricerUtility;
import com.krisapps.incomeutility_v2.subutilities.settings.SettingsUtility;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;
import java.util.logging.Level;

public class UtilityManager {
    private static final HashMap<String, SubUtility> activeUtilities = new HashMap<>();
    private static UtilityManager instance;
    private final Logging log = Logging.getInstance();

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

    /**
     * Generates a process ID for a sub-utility of the supplied type.
     *
     * @param type The type of sub-utility to generate the process ID for.
     * @return A process ID for the supplied utility type.
     */
    private String generateId(SubUtilityType type) {
        long count = activeUtilities.values().stream().filter(util -> util.getType() == type).count();
        return String.format("%s_%d-%d", type.name().toLowerCase(), count, (int) (Math.random() * 1000));
    }

    private boolean checkAlreadyOpen(SubUtilityType type) {
        return activeUtilities.values().stream().anyMatch(util -> util.getType() == type);
    }

    /**
     * Starts an instance of the sub-utility of the specified type.
     * <br>
     * The only disallowed type is {@link SubUtilityType#ALL}, as this method is only meant to launch one instance at a time.
     *
     * @param subutility The type of sub-utility to launch.
     */
    private void startUtility(SubUtilityType subutility) {
        String processId = getProcessIdFor(subutility);

        SubUtility utility;
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
            case SETTINGS -> {
                utility = new SettingsUtility();
            }
            case PANTRY -> {
                utility = new PantryUtility();
            }

            default -> throw new IllegalArgumentException("Invalid subutility '" + subutility + "'");
        }

        utility.setProcessId(processId);
        utility.setOnCloseCallback(this::unregister);
        try {
            register(utility, processId);
        } catch (IOException e) {
            log.log(String.format("Failed to start %s: ", utility.getName()) + e.getMessage(), "Process Registry", Level.SEVERE);
            e.printStackTrace();
        }
    }

    public boolean hasOpenUtilities() {
        return !activeUtilities.isEmpty();
    }

    /**
     * Launches the sub-utility of the supplied type.
     *
     * @param utility The type of sub-utility to launch.
     */
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

    /**
     * Terminates all instances of sub-utilities of the supplied type.
     *
     * @param type The type of sub-utilities to stop.
     */
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

    /**
     * Focuses all instances of sub-utilities of the supplied type.
     *
     * @param type The type of sub-utilities to focus.
     */
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

    /**
     * Stops the specified sub-utility process.
     *
     * @param processId The process ID for the sub-utility.
     */
    public void stopUtility(String processId) {
        Optional<SubUtility> util = Optional.ofNullable(activeUtilities.get(processId));
        util.ifPresentOrElse(SubUtility::stop, () -> {
            log.warning(String.format("Attempted to stop non-existent utility with process id: %s", processId), "Process Registry");
        });
    }

    /**
     * Generates a unique process ID for a sub-utility of the supplied type, ensuring it's unique.
     * @param processType The type of sub-utility to generate the process ID for.
     * @return A unique process ID.
     */
    private String getProcessIdFor(SubUtilityType processType) {
        String processId = generateId(processType);
        while (activeUtilities.containsKey(processId)) {
            processId = generateId(processType);
        }
        return processId;
    }

    /**
     * Starts and registers a new active sub-utility process.
     * @param process The utility whose process to register.
     * @param processId The process ID for the sub-utility.
     * @throws IOException If the utility cannot be started for some reason.
     */
    private void register(SubUtility process, String processId) throws IOException {
        try {
            activeUtilities.put(
                    processId, process.start(processId)
            );
            log.debug(String.format("Registered new active process '%s' (id: %s)", process.getName(), processId), "Process Registry");
        } catch (IOException e) {
            log.logStackTrace(e);
        }
    }

    /**
     * Unregisters a process from the active process registry.
     * <br>
     * Only use this method after stopping the utility, as you cannot stop a utility process without its ID.
     * @param processId The process ID of the process to unregister.
     */
    private void unregister(String processId) {
        if (activeUtilities.containsKey(processId)) {

            SubUtility util = activeUtilities.get(processId);
            log.debug(String.format("Unregistering active process for '%s' (%s)%n", util.getName(), processId), "Process Registry");

            activeUtilities.remove(processId);
        } else {
            log.warning(String.format("Failed to unregister '%s' - process not found in registry.", processId), "Process Registry");
        }
    }

}