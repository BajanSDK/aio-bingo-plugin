package com.bajansdk.aiobingo;

import org.junit.Test;

import static org.junit.Assert.assertSame;

public class AioBingoPluginPanelTest {
    @Test
    public void usesBoardOwnedScrollPaneInsteadOfRuneLiteWrapper() {
        AioBingoPluginPanel panel = new AioBingoPluginPanel();

        assertSame(panel, panel.getWrappedPanel());
    }
}
