package com.octop.mobile;

import java.util.Locale;

public final class NavigationRoute {
    public enum Tab {
        CHAT,
        EXPERTS,
        TASKS,
        MORE
    }

    private NavigationRoute() {
    }

    public static boolean hidesPrimaryNavigation(String path) {
        String normalized = normalizePath(path);
        return normalized.equals("/login")
                || normalized.startsWith("/login/")
                || normalized.equals("/setup")
                || normalized.startsWith("/setup/")
                || normalized.equals("/invite")
                || normalized.startsWith("/invite/");
    }

    public static Tab tabFor(String path) {
        String normalized = normalizePath(path);
        if (normalized.equals("/chat") || normalized.startsWith("/chat/")) {
            return Tab.CHAT;
        }
        if (normalized.equals("/experts") || normalized.startsWith("/experts/")) {
            return Tab.EXPERTS;
        }
        if (normalized.equals("/tasks") || normalized.startsWith("/tasks/")) {
            return Tab.TASKS;
        }
        return Tab.MORE;
    }

    public static boolean isHome(String path) {
        return tabFor(path) == Tab.CHAT && normalizePath(path).equals("/chat");
    }

    public static String normalizePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "/";
        }
        String value = path.trim().toLowerCase(Locale.ROOT);
        int query = value.indexOf('?');
        if (query != -1) {
            value = value.substring(0, query);
        }
        int fragment = value.indexOf('#');
        if (fragment != -1) {
            value = value.substring(0, fragment);
        }
        if (value.length() > 1 && value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value.startsWith("/") ? value : "/" + value;
    }
}

