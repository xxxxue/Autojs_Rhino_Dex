# 魔改 Rhino 1.7.14

将 js 转为 dex, 移除 js 源码字段, 加密所有字符串, 防止被轻易破解

## 环境

JDK 8

## 使用方法

在 release 中下载 jar (或者自己用源码编译)

到 jar 文件的所在目录执行命令

### 参数

必填项

`-f` 想要转换的 js 文件

`-o` 输出的目录

可选项

`-l` Rhino 优化等级 (例子: -l 9) ( 传参 -1 ~ 9 默认 0 )

`-s` 禁用字符串加密 (程序默认会加密字符串, 命令中添加 `-s` 则禁用加密字符串) 

```bash
java -jar .\rhino-Rhino1_7_14_Release-1.7.14.jar -f E:\autox-super-kit\out\main.js -o E:\autox-super-kit\out\dist\
```

最终会生成一个 `aaa.dex` 文件.

调用 dex : 
```javascript
"ui";
// autojs 加载 dex
runtime.loadDex("/sdcard/xxx辅助/aaa.dex");
// 运行
new Packages["aaa"]()();
```

更多代码 请看底部的 热更新小例子

## 源码指南 

打包 jar

```
IDEA 右侧的 gradle - Tasks - build - jar
```

修改 main 入口

```
build.gradle 中 修改 Main-Class

jar 
    manifest
        attributes
            Main-Class
```

StrUtils.class  ( 使用 javac 编译 )

```bash
javac .\StrUtils.java
```

## 注意事项

### 自己的软件加载 dex, 运行后界面乱码闪退的 bug

是系统中 JDK 默认编码的问题, 需要指定为 `UTF-8`

下面是 `两种情况` 下的解决方案

#### 执行 jar 

执行 jar 的情况, 在 `系统环境变量` 中添加

```
JAVA_TOOL_OPTIONS
-Dfile.encoding=UTF-8
```

如图:

![image](https://github.com/xxxxue/Autojs_Rhino_Dex/assets/32764266/cd46ec18-1477-48dc-8d1f-9300929d5650)

### IDEA 运行项目

IDEA中顶部菜单 -- 帮助 -- 编辑自定义 VM选项 -- 内容最底下加入下面的代码.

重启 IDEA 再次运行 Main 方法, 可以看到控制台日志可以显示中文了, app 也正常了.

```
-Dfile.encoding=UTF-8
```

如图:

![image](https://user-images.githubusercontent.com/32764266/169649429-c9a6d195-0fa4-4b0c-9fb6-aca66aa4ef92.png)

## autojs 热更新 dex 例子

### 小提示:

转 dex 之前, 先到 js 混淆网站里把 js 中的 所有的变量 和 方法名 全部重命名,

将混淆后的 js 再转 dex, 更加安全

[xxxxue/autox-super-kit](https://github.com/xxxxue/autox-super-kit) 

实现了 混淆 和 变量/方法名 全部重命名

### 缓存问题

按 返回键 的方式无法完全清掉后台, app 依然会使用 dex 的缓存

***正确的操作方法是***

点击设备的任务键, 将 app 手动关闭, 再打开即可更新到最新的版本

```javascript
"ui";

var DexName = "aaa.dex";
var DexVersionName = "DexVersion.js";
//本地文件
var LocalDirPath = "/sdcard/xxx辅助/";
var LocalDexPath = LocalDirPath + DexName;
var LocalVersionFilePath = LocalDirPath + DexVersionName;

//网络文件
var RemoteHost = "http://自己的地址/";
var RemoteDexFilePath = RemoteHost + DexName;
var RemoteVersionFilePath = RemoteHost + DexVersionName;


var Header = {
  headers: {
    "User-Agent":
      "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.25 Safari/537.36 Core/1.70.3754.400 QQBrowser/10.5.4034.400 ",
  },
};
/**
 * 开始运行
 */
function Run() {
  try {
    var checkState = false;

    //更新
    threads
      .start(function () {
        checkState = CheckVersion();
      })
      .join();

    if (checkState) {
      //加载dex并运行
      runtime.loadDex(LocalDexPath);
      new Packages["aaa"]()();
    }
  } catch (error) {
    toast("检查更新状态失败\n" + error);
    console.warn("Run Error: " + error);
  }
}

/**
 * 检查版本
 */
function CheckVersion() {
  var res = true;
  try {
    if (!files.exists(LocalVersionFilePath)) {
      console.log("创建版本文件");
      files.createWithDirs(LocalVersionFilePath);
      /** 默认值 */
      files.write(LocalVersionFilePath, "0.0.0");
    }

    var localVersion = files.read(LocalVersionFilePath);
    var remoteVersion = http.get(RemoteVersionFilePath,Header).body.string();

    if (localVersion != remoteVersion || !files.exists(LocalDexPath)) {
      console.warn("本地版本: " + localVersion);
      console.warn("远程版本: " + remoteVersion);
      if (DownloadDex()) {
        files.write(LocalVersionFilePath, remoteVersion);
      } else {
        //res = false;
      }
    } else {
      toast("最新版,无需更新");
    }
  } catch (error) {
    console.warn("CheckVersion Error: " + error);
    toast("检查版本发生异常\n" + error);
    //OpenLog();
  }
  return res;
}

/**
 * 下载Dex
 */
function DownloadDex() {
  var res = false;
  try {
    console.warn("dex开始更新");
    var res = http.get(RemoteDexFilePath,Header);
    if (Http200(res)) {
      files.writeBytes(LocalDexPath, res.body.bytes());
      if (files.exists(LocalDexPath)) {
        console.warn("dex更新成功");
        toast("更新成功");
        res = true;
      }
    } else {
      console.warn("DownloadDex 下载失败:  " + res);
      toast("DownloadDex 下载失败:  " + res);
      OpenLog();
      threads.shutDownAll();
      sleep(99999);
    }   
  } catch (error) {
    console.warn("DownloadDex Error: " + error);
    toast("下载新的dex 异常.\n" + error);
   // OpenLog();
  }

  return res;
}

/**
 * 判断是否 不是 空
 * @param {any}} content 内容
 */
function IsNotNullOrEmpty(content) {
  return content != null && content != undefined && Trim(content).length > 0;
}

/**
 * http200验证
 * @param {object} content http返回的json
 */
function Http200(content) {
  return (
    IsNotNullOrEmpty(content) &&
    (content.statusCode == 200 || content.statusCode == "200")
  );
}

/**
 * 去除左右空格
 * @param {string} content
 */
function Trim(content) {
  return (content + "").replace(/(^\s*)|(\s*$)/g, "");
}

function OpenLog() {
  ui.run(function () {
    // app.startActivity("console");
   
  });
}

Run();
```