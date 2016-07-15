package webby.commons.net;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class UrlEncoder {

    /**
     * Типичный urlencode. Пока использует стандартный URLEncoder.encode, но можно заменить его более производительной версией.
     */
    public static String encode(String str) throws UnsupportedEncodingException {
        return URLEncoder.encode(str, "utf-8");
    }

    /**
     * Urlencode, который кодирует только кириллические символы (точнее, все символы, начиная с кода 128).
     * От #encode отличается тем, что #encodeCyrillic не трогает ascii символы - пробелы, запятые, и т.д.
     */
    public static void encodeCyrillic(String str, StringBuilder sb) {
        int end = str.length();
        for (int i = 0; i < end; i++) {
            final char c = str.charAt(i);
            if (c < 0x80) {
                sb.append(c);
            } else if (c < 0x800) {
                writeEncodedChar((c >> 6) | 0xc0, sb);
                writeEncodedChar((c & 0x3f) | 0x80, sb);
            } else if (c < 0x10000) {
                writeEncodedChar((c >> 12) | 0xe0, sb);
                writeEncodedChar(((c >> 6) & 0x3f) | 0x80, sb);
                writeEncodedChar((c & 0x3f) | 0x80, sb);
            } else if (c < 0x200000) {
                writeEncodedChar((c >> 18) | 0xf0, sb);
                writeEncodedChar(((c >> 12) & 0x3f) | 0x80, sb);
                writeEncodedChar(((c >> 6) & 0x3f) | 0x80, sb);
                writeEncodedChar((c & 0x3f) | 0x80, sb);
            } else if (c < 0x4000000) {
                writeEncodedChar((c >> 24) | 0xf8, sb);
                writeEncodedChar(((c >> 18) & 0x3f) | 0x80, sb);
                writeEncodedChar(((c >> 12) & 0x3f) | 0x80, sb);
                writeEncodedChar(((c >> 6) & 0x3f) | 0x80, sb);
                writeEncodedChar((c & 0x3f) | 0x80, sb);
            } else {
                writeEncodedChar((c >> 30) | 0xfc, sb);
                writeEncodedChar(((c >> 24) & 0x3f) | 0x80, sb);
                writeEncodedChar(((c >> 18) & 0x3f) | 0x80, sb);
                writeEncodedChar(((c >> 12) & 0x3f) | 0x80, sb);
                writeEncodedChar(((c >> 6) & 0x3f) | 0x80, sb);
                writeEncodedChar((c & 0x3f) | 0x80, sb);
            }
        }
    }

    private static void writeEncodedChar(int code, StringBuilder sb) {
        sb.append('%');
        sb.append(Character.forDigit((code >> 4) & 0xF, 16));
        sb.append(Character.forDigit(code & 0xF, 16));
    }

    public static String encodeCyrillic(String str) {
        StringBuilder sb = new StringBuilder(str.length() * 6);
        encodeCyrillic(str, sb);
        return sb.toString();
    }
}
