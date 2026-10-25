package com.octop.mobile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class ServerUrlTest {
    @Test
    public void normalizeAddsHttpSchemeAndRemovesTrailingSlash() {
        assertEquals(
                "http://192.168.32.65:8088",
                ServerUrl.normalize("192.168.32.65:8088/")
        );
    }

    @Test
    public void normalizePreservesHttpsPort() {
        assertEquals(
                "https://octop.example.com:8443",
                ServerUrl.normalize("HTTPS://Octop.Example.com:8443")
        );
    }

    @Test
    public void normalizeRejectsPathsAndUnsupportedSchemes() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ServerUrl.normalize("http://localhost:8088/octop")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ServerUrl.normalize("ftp://localhost")
        );
    }

    @Test
    public void pathOfHandlesQueryAndTrailingSlash() {
        assertEquals(
                "/chat/agent-1",
                ServerUrl.pathOf("http://localhost:8088/chat/agent-1/?tab=recent")
        );
        assertEquals("/", ServerUrl.pathOf("http://localhost:8088"));
    }
}

