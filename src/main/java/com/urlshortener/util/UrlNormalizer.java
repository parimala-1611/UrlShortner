package com.urlshortener.util;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class UrlNormalizer {

    private static final Pattern HAS_SCHEME = Pattern.compile("(?i)^[a-z][a-z0-9+.-]*://.*");

    private static final Set<String> RECOGNIZED_NON_SLASH_SCHEMES =
            Set.of("mailto", "javascript", "tel", "sms", "data");

    private static final Pattern DOMAIN_PATTERN =
            Pattern.compile("^(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,63}$");

    private static final Pattern IPV4_PATTERN =
            Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");

    private static final Set<String> BLOCKED_HOST_EXTENSIONS = Set.of(
            "exe", "dll", "bat", "cmd", "msi", "sh", "bin", "apk", "dmg", "iso", "jar", "class",
            "zip", "rar", "7z", "tar", "gz", "bz2",
            "doc", "docx", "xls", "xlsx", "ppt", "pptx", "pdf", "txt", "csv", "rtf",
            "png", "jpg", "jpeg", "gif", "bmp", "svg", "webp", "ico",
            "mp3", "mp4", "avi", "mov", "wav", "mkv", "flac",
            "json", "xml", "yaml", "yml", "sql",
            "py", "js", "ts", "java", "cpp", "c", "go", "rb", "php"
    );

    public String normalize(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalArgumentException("URL must not be blank");
        }

        String trimmed = rawUrl.trim();
        String withScheme = hasExplicitScheme(trimmed) ? trimmed : "https://" + trimmed;

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

        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new IllegalArgumentException("Only http/https URLs are supported: " + rawUrl);
        }
        validateHost(host, rawUrl);

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

    private boolean hasExplicitScheme(String trimmed) {
        if (HAS_SCHEME.matcher(trimmed).matches()) {
            return true;
        }
        int colonIdx = trimmed.indexOf(':');
        if (colonIdx <= 0) {
            return false;
        }
        String prefix = trimmed.substring(0, colonIdx).toLowerCase(Locale.ROOT);
        return RECOGNIZED_NON_SLASH_SCHEMES.contains(prefix);
    }

    private void validateHost(String host, String rawUrl) {
        if (host.equals("localhost") || isValidIpv4(host)) {
            return;
        }
        if (!DOMAIN_PATTERN.matcher(host).matches()) {
            throw new IllegalArgumentException("Invalid URL host: " + rawUrl);
        }
        String tld = host.substring(host.lastIndexOf('.') + 1);
        if (BLOCKED_HOST_EXTENSIONS.contains(tld)) {
            throw new IllegalArgumentException(
                    "URL host looks like a file, not a domain: " + rawUrl);
        }
    }

    private boolean isValidIpv4(String host) {
        var matcher = IPV4_PATTERN.matcher(host);
        if (!matcher.matches()) {
            return false;
        }
        for (int i = 1; i <= 4; i++) {
            if (Integer.parseInt(matcher.group(i)) > 255) {
                return false;
            }
        }
        return true;
    }
}
