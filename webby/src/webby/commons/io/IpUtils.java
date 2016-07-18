package webby.commons.io;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class IpUtils {

    public static String intToIp(int i) {
        return ((i >> 24) & 0xFF) + "." +
             ((i >> 16) & 0xFF) + "." +
             ((i >> 8) & 0xFF) + "." +
             (i & 0xFF);
    }

    public static byte[] intToAddress(int i) {
        return new byte[]{
             (byte) ((i >> 24) & 0xFF),
             (byte) ((i >> 16) & 0xFF),
             (byte) ((i >> 8) & 0xFF),
             (byte) (i & 0xFF)};
    }

    public static InetAddress intToInetAddress(int i) throws UnknownHostException {
        return InetAddress.getByAddress(intToAddress(i));
    }

    public static int ipToInt(String ip) {
        if (ip == null) return 0;
        char[] chars = ip.toCharArray();
        int i1, i2, i3;
        for (i1 = 0; i1 < chars.length && chars[i1] != '.'; i1++) ;
        for (i2 = i1 + 1; i2 < chars.length && chars[i2] != '.'; i2++) ;
        for (i3 = i2 + 1; i3 < chars.length && chars[i3] != '.'; i3++) ;
        return (Integer.parseInt(ip.substring(0, i1), 10) << 24) |
             (Integer.parseInt(ip.substring(i1 + 1, i2), 10) << 16) |
             (Integer.parseInt(ip.substring(i2 + 1, i3), 10) << 8) |
             Integer.parseInt(ip.substring(i3 + 1, chars.length), 10);
    }
}
