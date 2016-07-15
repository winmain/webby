package webby.commons.cookie;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;

import static webby.commons.cookie.CookieEncoderUtilV0.*;

/**
 * Модификация энкодера io.netty.handler.codec.http.ServerCookieEncoder для cookie version 0. Мы не
 * можем генерировать куки версии 1, потому что ie9 не поддерживает их. А стандартный
 * ServerCookieEncoder не умеет правильно генерировать куки версии 0.
 */
public final class CookieEncoderV0 {

    /**
     * Encodes the specified cookie into an HTTP header value.
     */
    public static String encode(String name, String value) {
        return encode(new DefaultCookie(name, value));
    }

    public static String encode(Cookie cookie) {
        if (cookie == null) {
            throw new NullPointerException("cookie");
        }

        StringBuilder buf = buffer.get();

        add(buf, cookie.name(), cookie.value());

        if (cookie.maxAge() != Long.MIN_VALUE) {
            addUnquoted(buf, CookieHeaderNames.EXPIRES,
                 HttpHeaderDateFormat.get().format(
                      new Date(System.currentTimeMillis() +
                           cookie.maxAge() * 1000L)));
        }

        if (cookie.path() != null) {
            addUnquoted(buf, CookieHeaderNames.PATH, cookie.path());
        }

        if (cookie.domain() != null) {
            addUnquoted(buf, CookieHeaderNames.DOMAIN, cookie.domain());
        }
        if (cookie.isSecure()) {
            buf.append(CookieHeaderNames.SECURE);
            buf.append((char) HttpConstants.SEMICOLON);
            buf.append((char) HttpConstants.SP);
        }
        if (cookie.isHttpOnly()) {
            buf.append(CookieHeaderNames.HTTPONLY);
            buf.append((char) HttpConstants.SEMICOLON);
            buf.append((char) HttpConstants.SP);
        }

        return stripTrailingSeparator(buf);
    }

    public static List<String> encode(Cookie... cookies) {
        if (cookies == null) {
            throw new NullPointerException("cookies");
        }

        List<String> encoded = new ArrayList<String>(cookies.length);
        for (Cookie c : cookies) {
            if (c == null) {
                break;
            }
            encoded.add(encode(c));
        }
        return encoded;
    }

    public static List<String> encode(Collection<Cookie> cookies) {
        if (cookies == null) {
            throw new NullPointerException("cookies");
        }

        List<String> encoded = new ArrayList<String>(cookies.size());
        for (Cookie c : cookies) {
            if (c == null) {
                break;
            }
            encoded.add(encode(c));
        }
        return encoded;
    }

    public static List<String> encode(Iterable<Cookie> cookies) {
        if (cookies == null) {
            throw new NullPointerException("cookies");
        }

        List<String> encoded = new ArrayList<String>();
        for (Cookie c : cookies) {
            if (c == null) {
                break;
            }
            encoded.add(encode(c));
        }
        return encoded;
    }

    private CookieEncoderV0() {
        // Unused
    }
}
