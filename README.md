# 魔改 Rhino 1772   

# QQ: 1659809758



## 必要的 环境

1. IDEA
2. Java
3. Gradle
4. Android 的 dx.jar

## autojs  js脚本 转 dex

## 修改内容

 去除源码字符串

 加密所有字符串

---

加密入口类

> [mycode/main.java](https://github.com/xxxxue/Autojs_Rhino_Dex_Self/blob/master/src/org/mozilla/mycode/main.java)



js 转 class 具体实现类. 调用 自定义加密类 .实现 字符串的加密

> [optimizer/Codegen.java](https://github.com/xxxxue/Autojs_Rhino_Dex_Self/blob/master/src/org/mozilla/javascript/optimizer/Codegen.java)



 自定义的  加密 类

> [defpackage/StrUtils.java](https://github.com/xxxxue/Autojs_Rhino_Dex_Self/blob/master/src/defpackage/StrUtils.java)



Rhino 命令行 类

> [jsc/Main.java](https://github.com/xxxxue/Autojs_Rhino_Dex_Self/blob/master/toolsrc/org/mozilla/javascript/tools/jsc/Main.java)



现在的版本只支持  执行 mycode/main.java 来 转 dex . 

有兴趣的大佬可以 自己实现一下 jar命令行 转 dex

## 支持作者

如果这个开源项目可以帮助到你,  你也可以请作者吃一包辣条。

![pay](img.assets/pay.png)

## autojs 热更新dex 例子

## 小提示:

转dex之前,先到 js 混淆网站里把  js中的 所有的变量 和 方法名 全部重命名

将混淆后的js 再转 dex ,更加安全..

## 缓存问题

按 返回键的 方式  无法完全 杀掉 后台. app 依然会使用 dex 的缓存.

***正确的操作方法是*** 
点击设备 Home 键 旁边的 任务键,  
将 app 手动 关闭,(杀掉后台)
再打开app  即可 更新到最新的版本



实在不行就

重启下手机设备 或者 清除 该 app 的 所有数据.  



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





