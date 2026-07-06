package com.urlshortener.util;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class UrlNormalizer {

    private static final Pattern HAS_SCHEME = Pattern.compile("(?i)^[a-z][a-z0-9+.-]*://.*");

    public String normalize(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalArgumentException("URL must not be blank");
        }

        String trimmed = rawUrl.trim();
        String withScheme = HAS_SCHEME.matcher(trimmed).matches() ? trimmed : "https://" + trimmed;

        URI uri;
        try {
            uri = new URI(withScheme);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL: " + rawUrl, e);
        }

        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || host == null) {
            throw new IllegalArgumentException("Invalid URL: " + rawUrl);
        }
        scheme = scheme.toLowerCase(Locale.ROOT);
        host = host.toLowerCase(Locale.ROOT);

        int port = uri.getPort();
        boolean isDefaultPort = port == -1
                || ("http".equals(scheme) && port == 80)
                || ("https".equals(scheme) && port == 443);

        String path = uri.getRawPath();
        if (path == null || path.equals("/")) {
            path = "";
        } else if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        StringBuilder result = new StringBuilder();
        result.append(scheme).append("://").append(host);
        if (!isDefaultPort) {
            result.append(':').append(port);
        }
        result.append(path);

        String query = uri.getRawQuery();
        if (query != null && !query.isEmpty()) {
            result.append('?').append(query);
        }

        return result.toString();
    }
}
