package webby.commons.cookie;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import io.netty.handler.codec.http.HttpConstants;

/**
 * Модификация io.netty.handler.codec.http.CookieEncoderUtil для работы с куками version 0.
 */
final class CookieEncoderUtilV0 {

    static final ThreadLocal<StringBuilder> buffer = new ThreadLocal<StringBuilder>() {
        @Override
        public StringBuilder get() {
            StringBuilder buf = super.get();
            buf.setLength(0);
            return buf;
        }

        @Override
        protected StringBuilder initialValue() {
            return new StringBuilder(512);
        }
    };

    static String stripTrailingSeparator(StringBuilder buf) {
        if (buf.length() > 0) {
            buf.setLength(buf.length() - 2);
        }
        return buf.toString();
    }

    static void add(StringBuilder sb, String name, String val) {
        if (val == null) {
            addUnquoted(sb, name, "");
            return;
        }

        try {
            for (int i = 0; i < val.length(); i ++) {
                char c = val.charAt(i);
                switch (c) {
                case '\t': case ' ': case '"': case '(':  case ')': case ',':
                case '/':  case ':': case ';': case '<':  case '=': case '>':
                case '?':  case '@': case '[': case '\\': case ']':
                case '{':  case '}':
                        addUnquoted(sb, name, URLEncoder.encode(val, "utf-8"));
                        return;
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        addUnquoted(sb, name, val);
    }

    static void addUnquoted(StringBuilder sb, String name, String val) {
        sb.append(name);
        sb.append((char) HttpConstants.EQUALS);
        sb.append(val);
        sb.append((char) HttpConstants.SEMICOLON);
        sb.append((char) HttpConstants.SP);
    }

    static void addQuoted(StringBuilder sb, String name, String val) {
        if (val == null) {
            val = "";
        }

        sb.append(name);
        sb.append((char) HttpConstants.EQUALS);
        sb.append((char) HttpConstants.DOUBLE_QUOTE);
        sb.append(val.replace("\\", "\\\\").replace("\"", "\\\""));
        sb.append((char) HttpConstants.DOUBLE_QUOTE);
        sb.append((char) HttpConstants.SEMICOLON);
        sb.append((char) HttpConstants.SP);
    }

    static void add(StringBuilder sb, String name, long val) {
        sb.append(name);
        sb.append((char) HttpConstants.EQUALS);
        sb.append(val);
        sb.append((char) HttpConstants.SEMICOLON);
        sb.append((char) HttpConstants.SP);
    }

    private CookieEncoderUtilV0() {
        // Unused
    }
}
