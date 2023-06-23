package defpackage;

import org.mozilla.classfile.ByteCode;
import org.mozilla.classfile.ClassFileWriter;

public class StrUtils
{

    /**
     * 字符串加密
     */
    public static String e(String data)
    {
        byte[] b = data.getBytes();
        for (int i = 0; i < b.length; i++)
        {
            b[i] = (byte) (b[i] + 1);
        }
        StringBuilder hex = new StringBuilder();
        for (int i2 = 0; i2 < b.length; i2++)
        {
            hex.append(String.format("%02X", Byte.valueOf(b[i2])));
        }
        return hex.toString();
    }

    /**
     * 字符串解密
     */
    public static String d(String data)
    {
        int l = data.length() / 2;
        byte[] b = new byte[l];
        for (int i = 0; i < l; i++)
        {
            b[i] = Integer.valueOf(data.substring(i * 2, (i * 2) + 2), 16).byteValue();
        }
        for (int i2 = 0; i2 < b.length; i2++)
        {
            b[i2] = (byte) (b[i2] - 1);
        }
        return new String(b);
    }

    /**
     * 加密，把一个字符串在原有的基础上 加一个数值
     */
    public static String e2(String data)
    {

        //把字符串转为字节数组
        byte[] b = data.getBytes();
        //遍历
        for (int i = 0; i < b.length; i++)
        {
            b[i] += 3;//在原有的基础上 加一个数值
        }
        return new String(b);
    }

    /**
     * 解密：把一个加密后的字符串在原有基础上 减一个数
     *
     * @param data 加密后的字符串
     * @return 返回解密后的新字符串
     */
    public static String d2(String data)
    {
        //把字符串转为字节数组
        byte[] b = data.getBytes();
        //遍历
        for (int i = 0; i < b.length; i++)
        {
            b[i] -= 3;//在原有的基础上 减一个数
        }
        return new String(b);
    }

    /**
     * 字符串加密
     */
    public static void strE(ClassFileWriter cfw, String content)
    {
        //System.out.println(content);
        if (content.isEmpty())
        {
            //为空 则 不调用 解密..
            cfw.addPush(content);
        }
        else
        {
            cfw.addPush(StrUtils.e(content));
            cfw.addInvoke(ByteCode.INVOKESTATIC, "defpackage.StrUtils", "d", "(Ljava/lang/String;)" +
                    "Ljava/lang/String;");
        }
    }
}