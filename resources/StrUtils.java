package defpackage;

public class StrUtils {
    public static String d(String data) {
        int l = data.length() / 2;
        byte[] b = new byte[l];
        for (int i = 0; i < l; i++) {
            b[i] = Integer.valueOf(data.substring(i * 2, (i * 2) + 2), 16).byteValue();
        }
        for (int i2 = 0; i2 < b.length; i2++) {
            b[i2] = (byte) (b[i2] - 1);
        }
        return new String(b);
    }
}