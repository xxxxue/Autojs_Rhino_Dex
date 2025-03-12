package org.mozilla.mycode;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.CharsetUtil;
import org.apache.commons.cli.*;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.optimizer.ClassCompiler;

import java.io.*;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Main {
    /**
     * 工作的目录
     */
    public static String _outDirPath = "";

    /**
     * Rhino 加密等级
     */
    public static Integer _level = 0;

    /**
     * 禁用加密
     */
    public static Boolean _isNoEncryptString = false;
    public static String _dxFileName = "dx-29.0.3.jar";
    public static String _strUtilsClassFileName = "StrUtils.class";

    public static void main(String[] args) throws Exception {

        Options options = new Options();
        options.addRequiredOption("f", "file", true, "输入文件");
        options.addRequiredOption("o", "output", true, "输出的目录");
        // -1 .. 9
        options.addOption("l", "level", true, "Rhino 优化等级");
        options.addOption("s", "string", false, "禁用字符串加密");

        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            // 获取解析后的参数值
            String inputFilePath = cmd.getOptionValue("f");
            String outputDirPath = cmd.getOptionValue("o");

            // 输出目录 再创建一个唯一的文件夹 (使用当前时间精确到秒)
            _outDirPath = Paths.get(outputDirPath, getTimeString()).toString();
            _level = Integer.valueOf(cmd.getOptionValue('l', "0"));
            _isNoEncryptString = cmd.hasOption('s');

            System.out.println("===================================");
            System.out.println("===================================");
            System.out.println("输入文件：" + inputFilePath);
            System.out.println("输出文件夹：" + outputDirPath);
            System.out.println("最终的输出地址:" + _outDirPath);
            System.out.println("Rhino 优化等级:" + _level);
            System.out.println("禁用字符串加密:" + _isNoEncryptString);
            System.out.println("===================================");
            System.out.println("===================================");

            // 将 当前 jar 中的 dx.jar 释放到 outputDir 目录
            FileUtil.writeBytes(ResourceUtil.readBytes(_dxFileName), Paths.get(_outDirPath, _dxFileName).toString());

            // 读取 代码文本
            String code = FileUtil.readUtf8String(inputFilePath);

            // 开始转 dex
            toDexFile(code);

        } catch (ParseException e) {
            System.err.println("命令行参数解析失败: " + e.getMessage());
        }
    }

    static void toDexFile(String code) {
        //创建 Rhino 编译环境 相关参数..
        CompilerEnvirons compilerEnv = new CompilerEnvirons();
        compilerEnv.setGeneratingSource(false); //编译后,不添加 js 源码
        compilerEnv.setLanguageVersion(Context.VERSION_ES6); //设置 支持es6
        compilerEnv.setOptimizationLevel(_level); //  优化等级 -1..9
        ClassCompiler compiler = new ClassCompiler(compilerEnv);

        // compileToClassFiles 的第4个参数比较重要，它表明了js转成.class的类路径，影响到  在 autojs 调用的方法
        // 不填写包名 则 默认在 defpackage 中

        // auto.js调用例子 (如果auto.js闪退.就是抛出了异常.可以使用 adb logcat 查看日志 或者 用开源的 auto.js 代码, 使用 AndroidStudio 进行调试,查看异常)
        //	   var localDexPath= "/sdcard/辅助/aaa.dex"
        //     runtime.loadDex(localDexPath);
        //     new Packages["aaa"]()();
        //
        Object[] compiled = compiler.compileToClassFiles(
                code,
                null,
                1,
                "aaa");

        for (int j = 0; j != compiled.length; j += 2) {

            // 生成 解密工具类 文件
            byte[] utilsBytes = ResourceUtil.readBytes(_strUtilsClassFileName);
            String utilsClassPath = Paths.get(_outDirPath, "class", "defpackage", "StrUtils.class").toString();
            FileUtil.writeBytes(utilsBytes, utilsClassPath);


            // 生成 自己的代码 文件
            String classPath = Paths.get(_outDirPath, "class", "aaa.class").toString();
            byte[] bytes = (byte[]) compiled[(j + 1)];
            FileUtil.writeBytes(bytes, classPath);

            // 将多个 class 合并为一个 jar
            String jarFileName = "code.jar";
            cmdExec("jar cvf " + jarFileName + " -C class .");
            System.out.println("开始转dex,js代码越多,耗时越长,请耐心等待");

            //将 jar 转为 dex
            cmdExec("java -jar " + _dxFileName + " --dex --output=aaa.dex " + jarFileName);

            System.out.println();
            System.out.println("===================================");
            System.out.println("js 转 dex 结束");

        }
        System.out.println("编译成功！dex 文件保存位置: " + _outDirPath);
        System.out.println("===================================");
    }

    /**
     * 执行cmd命令
     */
    public static void cmdExec(String cmd) {
        Runtime run = Runtime.getRuntime();
        try {
            Process p = run.exec(cmd, new String[]{}, new File(_outDirPath)); // 设置目录
            InputStream ins = p.getInputStream();
            InputStream ers = p.getErrorStream();
            new Thread(new inputStreamThread(ins)).start();
            p.waitFor();
        } catch (IOException | InterruptedException e) {
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

    /**
     * 获取时间 字符串
     */
    public static String getTimeString() {
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss");
        return formatter.format(date);
    }

}