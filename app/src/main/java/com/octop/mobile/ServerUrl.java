package com.octop.mobile;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public final class ServerUrl {
    private ServerUrl() {
    }

    public static String normalize(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Server URL is empty");
        }

        String candidate = input.trim();
        if (candidate.isEmpty()) {
            throw new IllegalArgumentException("Server URL is empty");
        }
        if (candidate.startsWith("//")) {
            candidate = "http:" + candidate;
        } else if (!candidate.contains("://")) {
            candidate = "http://" + candidate;
        }

        final URI uri;
        try {
            uri = new URI(candidate);
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Invalid server URL", exception);
        }

        String scheme = uri.getScheme() == null
                ? ""
                : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new IllegalArgumentException("Only HTTP and HTTPS are supported");
        }
        if (uri.getUserInfo() != null || uri.getHost() == null) {
            throw new IllegalArgumentException("Server URL must contain a host");
        }
        if (uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("Server URL cannot contain a query or fragment");
        }
        if (uri.getPath() != null && !uri.getPath().isEmpty() && !uri.getPath().equals("/")) {
            throw new IllegalArgumentException("Server URL cannot contain a path");
        }

        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (host.contains(":") && !host.startsWith("[")) {
            host = "[" + host + "]";
        }
        int port = uri.getPort();
        return scheme + "://" + host + (port == -1 ? "" : ":" + port);
    }

    public static String pathOf(String url) {
        if (url == null || url.isEmpty()) {
            return "/";
        }
        try {
            String path = new URI(url).getPath();
            if (path == null || path.isEmpty()) {
                return "/";
            }
            if (path.length() > 1 && path.endsWith("/")) {
                return path.substring(0, path.length() - 1);
            }
            return path;
        } catch (URISyntaxException exception) {
            int query = url.indexOf('?');
            String withoutQuery = query == -1 ? url : url.substring(0, query);
            int scheme = withoutQuery.indexOf("://");
            if (scheme != -1) {
                int pathStart = withoutQuery.indexOf('/', scheme + 3);
                return pathStart == -1 ? "/" : withoutQuery.substring(pathStart);
            }
            return withoutQuery.startsWith("/") ? withoutQuery : "/";
        }
    }

    public static String hostLabel(String normalizedUrl) {
        try {
            URI uri = new URI(normalizedUrl);
            String host = uri.getHost();
            if (host == null) {
                return normalizedUrl;
            }
            return host + (uri.getPort() == -1 ? "" : ":" + uri.getPort());
        } catch (URISyntaxException exception) {
            return normalizedUrl;
        }
    }
}

