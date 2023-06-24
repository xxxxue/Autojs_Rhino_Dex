package org.mozilla.mycode;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.CharsetUtil;
import com.itranswarp.compiler.JavaStringCompiler;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.optimizer.ClassCompiler;
import org.apache.commons.cli.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

class Main {

    static final String JAVA_SOURCE_CODE = "" +
            "package defpackage;" +
            "import org.mozilla.classfile.ByteCode;" +
            "import org.mozilla.classfile.ClassFileWriter;" +
            "public class StrUtils{" +
            "		public static String d(String data)" +
            "		{" +
            "			int l = data.length() / 2;" +
            "			byte[] b = new byte[l];" +
            "			for (int i = 0; i < l; i++)" +
            "			{" +
            "				b[i] = Integer.valueOf(data.substring(i * 2, (i * 2) + 2), 16).byteValue();" +
            "			}" +
            "			for (int i2 = 0; i2 < b.length; i2++)" +
            "			{" +
            "				b[i2] = (byte) (b[i2] - 1);" +
            "			}" +
            "			return new String(b);" +
            "		}" +
            "	}	";

    // 工作的目录
    public static String _outDirPath = "";

    public static void main(String[] args) throws Exception {

        // 创建 Options 对象
        Options options = new Options();
        options.addOption("f", "file", true, "输入文件");
        options.addOption("o", "output", true, "输出的目录");

        // 创建 CommandLineParser 对象
        CommandLineParser parser = new DefaultParser();

        try {
            // 解析命令行参数
            CommandLine cmd = parser.parse(options, args);

            // 获取解析后的参数值
            String inputFile = cmd.getOptionValue("f");
            String outputDir = cmd.getOptionValue("o");

            // 打印参数值
            System.out.println("输入文件：" + inputFile);
            System.out.println("输出文件夹：" + outputDir);
            // 输出目录 再创建一个唯一的文件夹 (使用当前时间精确到秒)
            _outDirPath = outputDir + "\\" + getTimeString() + "\\";
            System.out.println("最终的输出地址:" + _outDirPath);

            // 将 当前 jar 程序中的 dx.jar 释放到 outputDir 目录
            byte[] fileData = ResourceUtil.readBytes("dx-29.0.3.jar");
            FileUtil.writeBytes(fileData, _outDirPath + "dx-29.0.3.jar");

            toClassFile(FileUtil.readUtf8String(inputFile));

        } catch (ParseException e) {
            // 解析失败，打印错误消息
            System.err.println("命令行参数解析失败: " + e.getMessage());
        }

//        String filePath = "E:\\Work\\AutojsProject\\autox-super-kit\\out\\main.js";
    }

    /**
     * 获取时间 字符串
     *
     * @return
     */

    public static String getTimeString() {
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss");
        return formatter.format(date);
    }

    //这里注意传参是 js 文本数据，不是 js 路径
    static void toClassFile(String script) throws Exception {
        //创建 Rhino 编译环境 相关参数..
        CompilerEnvirons compilerEnv = new CompilerEnvirons();
        compilerEnv.setGeneratingSource(false); //编译后,不添加 js 源码
        compilerEnv.setLanguageVersion(Context.VERSION_ES6); //设置 支持es6
        compilerEnv.setOptimizationLevel(9); //  优化等级改为 9 级.    如果有问题 就改成0
        ClassCompiler compiler = new ClassCompiler(compilerEnv);

        // compileToClassFiles 的第4个参数比较重要，它表明了js转成.class的类路径，影响到  在 autojs 调用的方法
        // 不填写包名 则 默认在 defpackage 中

        // auto js调用例子 (如果autojs闪退.就是抛出了异常.请使用开源的autojs 代码, 使用 AdnroidStudio 进行调试,查看异常)
        //	   var localDexPath= "/sdcard/辅助/aaa.dex"
        //     runtime.loadDex(localDexPath);
        //     new Packages["aaa"]()();
        //
        Object[] compiled = compiler.compileToClassFiles(
                script,
                null,
                1,
                "aaa");

        for (int j = 0; j != compiled.length; j += 2) {
            //String className = (String) compiled[j];

            JavaStringCompiler compiler2 = new JavaStringCompiler();

            // 字符串 转为 java  class 文件
            Map<String, byte[]> results = compiler2.compile("StrUtils.java", JAVA_SOURCE_CODE);

            Console.log(results);

            //解密工具类的 数据
            byte[] utilsBytes = results.get("defpackage.StrUtils");

            String utilsClassPath = _outDirPath + "class\\defpackage\\StrUtils.class";
            File utilsFile = new File(utilsClassPath);
            utilsFile.getParentFile().mkdirs(); //创建文件夹

            try (FileOutputStream fos = new FileOutputStream(utilsFile)) {
                fos.write(utilsBytes);
            } catch (FileNotFoundException e) {
                System.out.println("utils 文件未找到！");
                e.printStackTrace();
            } catch (IOException e) {
                System.out.println("utils 文件保存失败！");
                e.printStackTrace();
            }

            //------------
            String classPath = _outDirPath + "class\\aaa.class";

            //js 转为 class
            byte[] bytes = (byte[]) compiled[(j + 1)];
            File file = new File(classPath);
            file.getParentFile().mkdirs(); //创建文件夹

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(bytes);
            } catch (FileNotFoundException e) {
                System.out.println("文件未找到！");
                e.printStackTrace();
            } catch (IOException e) {
                System.out.println("文件保存失败！");
                e.printStackTrace();
            }

            //将两个 class 打包为 一个jar
            cmdExec("jar cvf demo.jar -C class .");

            System.out.println(utilsFile.exists());
            System.out.println("开始转dex,js代码越多,耗时越长,请耐心等待");
            //将 jar 转为 dex
            cmdExec("java -jar dx-29.0.3.jar --dex " +
                    "--output=aaa.dex " +
                    "demo.jar");
            System.out.println();
            System.out.println();
            System.out.println("===================================");
            System.out.println("js 转 dex 结束");

        }
        System.out.println("编译成功！dex 文件保存位置: " + _outDirPath);
        System.out.println("===================================");
    }

    /**
     * 执行cmd命令
     *
     * @param cmd
     */
    public static void cmdExec(String cmd) {

        Runtime run = Runtime.getRuntime();
        try {
            Process p = run.exec(cmd, new String[]{}, new File(_outDirPath)); // 设置目录
            InputStream ins = p.getInputStream();
            InputStream ers = p.getErrorStream();
            new Thread(new inputStreamThread(ins)).start();
            p.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static class inputStreamThread implements Runnable {
        private InputStream ins = null;
        private BufferedReader bfr = null;

        public inputStreamThread(InputStream ins) {
            this.ins = ins;
            this.bfr = new BufferedReader(new InputStreamReader(ins));
        }

        @Override
        public void run() {
            String line = null;
            byte[] b = new byte[100];
            int num = 0;
            try {
                while ((num = ins.read(b)) != -1) {
                    System.out.println(new String(b, CharsetUtil.CHARSET_GBK));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}