package com.placeboard.map;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

final class Hosts {
    private Hosts() {
    }

    /**
     * exact 목록과 일치하거나 suffix 목록의 하위 도메인인 경우 true.
     */
    static boolean matches(URI uri, Set<String> exact, Set<String> suffixes) {
        if (uri.getHost() == null) return false;
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        return exact.contains(host) || suffixes.stream().anyMatch(suffix -> host.endsWith("." + suffix));
    }

    static URI parseHttps(String url) {
        try {
            URI uri = new URI(url);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null ? uri : null;
        } catch (Exception exception) {
            return null;
        }
    }
}
