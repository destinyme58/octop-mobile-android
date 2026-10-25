package com.octop.mobile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NavigationRouteTest {
    @Test
    public void mapsTopLevelRoutesToTabs() {
        assertEquals(NavigationRoute.Tab.CHAT, NavigationRoute.tabFor("/chat/agent/thread"));
        assertEquals(NavigationRoute.Tab.EXPERTS, NavigationRoute.tabFor("/experts"));
        assertEquals(NavigationRoute.Tab.TASKS, NavigationRoute.tabFor("/tasks?id=1"));
        assertEquals(NavigationRoute.Tab.MORE, NavigationRoute.tabFor("/personalization"));
    }

    @Test
    public void hidesNativeNavigationOnAuthenticationPages() {
        assertTrue(NavigationRoute.hidesPrimaryNavigation("/login"));
        assertTrue(NavigationRoute.hidesPrimaryNavigation("/setup/"));
        assertTrue(NavigationRoute.hidesPrimaryNavigation("/invite/token"));
        assertFalse(NavigationRoute.hidesPrimaryNavigation("/chat"));
    }
}

