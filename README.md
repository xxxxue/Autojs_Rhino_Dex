# 环境

JDK 8

# 执行 jar

到 jar 文件的所在目录执行命令

`-f` 是想要转换的 js 文件

`-o` 是输出的目录

```bash
java -jar .\rhino-Rhino1_7_14_Release-1.7.14.jar -f E:\Work\AutojsProject\autox-super-kit\out\main.js -o E:\Work\AutojsProject\autox-super-kit\out\dist\
```

# 打包 jar
```
IDEA 右侧的 gradle - Tasks - build - jar
```

# 修改 main 入口
```
build.gradle 中 修改 Main-Class

jar 
    manifest
        attributes
            Main-Class
```