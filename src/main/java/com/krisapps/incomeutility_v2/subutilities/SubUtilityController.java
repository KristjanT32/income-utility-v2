package com.krisapps.incomeutility_v2.subutilities;

public abstract class SubUtilityController {

    public abstract void onStartup(SubUtility utility);

    public abstract void onShutdown();

    public abstract void onPromptCommand(String command, String[] args);
}
