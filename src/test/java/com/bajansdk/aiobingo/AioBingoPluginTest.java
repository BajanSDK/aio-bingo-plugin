package com.bajansdk.aiobingo;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class AioBingoPluginTest {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(AioBingoPlugin.class);
        RuneLite.main(args);
    }
}
