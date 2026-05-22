package com.david.astrodrill.network;

/**
 * Tiny JSON-writing helpers shared by the reflection-free serializers in this
 * package (BackendClient, SaveStateSerializer). libGDX's JsonReader handles the
 * read side; the write side is done by appending to a StringBuilder, which
 * needs this escape() so user strings can't break the JSON.
 */
final class JsonUtil {

    private JsonUtil() {}

    /** JSON string escaping: ", \, control chars, and the standard short escapes. */
    static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) {
                        sb.append("\\u");
                        String hex = Integer.toHexString(c);
                        for (int p = hex.length(); p < 4; p++) sb.append('0');
                        sb.append(hex);
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
