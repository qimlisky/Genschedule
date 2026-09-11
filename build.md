
### 1. 先拉源码
```
git clone https://v4.gh-proxy.org/https://github.com/qimlisky/GenSchedule.git
cd GenSchedule
git clone --branch v0.9.3 --depth 1 https://gh-proxy.com/https://github.com/compose-miuix-ui/miuix.git miuix
```
进入miuix目录
```
cd D:\GenSchedule\miuix
```
```
git log --oneline -1
```
得到类似输出
`c36fab7 (grafted, HEAD, tag: v0.9.3) build: bump version to 0.9.3`
### 2. 安装补丁
```
git apply ..\patches\miuix-0.9.3-sleepdown.patch
git apply  ..\patches\miuix-cascading-popup-surface.patch
git apply  ..\patches\miuix-scaffold-underlay.patch
git status
```
如果没有输出，就安装成功
### 3.处理环境路径以及换源
修改 `/gradle/wrapper/gradle-wrapper.properties` 文件,加速下载，把原本的`gradle-9.6.1-bin.zip`文件链接修改成
```
#/gradle/wrapper/gradle-wrapper.properties
distributionUrl=https\://mirrors.cloud.tencent.com/gradle/gradle-9.6.1-bin.zip
```

写到`/gradle.properties`路径自行匹配，java路径自行选择（java21貌似可以）
```
#/gradle.properties
sleepdown.miuixSourcePath=D:/GenSchedule/miuix
#org.gradle.java.home=C:/to/your/java/path  #可选
```
给 `miuix`文件夹里面也建一个 `local.properties`文件，写上内容  
sdk一般在用户文件夹里面。懒可以先构建"app"一遍,在GenSchedule下面找`local.properties`,把这个文件拖入`miuix`文件夹。
```
#/GenSchedule/miuix/local.properties
sdk.dir=C:/Users/username/AppData/Local/Android/Sdk
```

### 最后`sync project with Gradle file`
### 可能出错的可能的解决办法
对于`miuix-cascading-popup-surface.patch`文件，输出`error: corrupt patch at line 142`，就执行下面命令
使用powershell
```powershell
cd D:\GenSchedule

# 1. 确认补丁存在
#Test-Path .\patches\miuix-0.9.3-sleepdown.patch
Test-Path .\patches\miuix-cascading-popup-surface.patch

# 2. 转成无 BOM 的 UTF-8 + LF
$content1 = (Get-Content .\patches\miuix-0.9.3-sleepdown.patch -Raw) -replace "`r`n", "`n"
[System.IO.File]::WriteAllText(
    "D:\schedule\patches\miuix-0.9.3-sleepdown-lf.patch",
    $content1,
    [System.Text.UTF8Encoding]::new($false)
)

$content2 = (Get-Content .\patches\miuix-cascading-popup-surface.patch -Raw) -replace "`r`n", "`n"
[System.IO.File]::WriteAllText(
    "D:\schedule\patches\miuix-cascading-popup-surface-lf.patch",
    $content2,
    [System.Text.UTF8Encoding]::new($false)
)

# 3. 确认 -lf 文件已生成
Test-Path .\patches\miuix-0.9.3-sleepdown-lf.patch
Test-Path .\patches\miuix-cascading-popup-surface-lf.patch

# 4. 应用第一个补丁，选安装，哪个出错安装哪个
#cd D:\GenSchedule\miuix
#git apply ..\patches\miuix-0.9.3-sleepdown-lf.patch

# 5. 应用第二个补丁
git apply --ignore-whitespace -C1 ..\patches\miuix-cascading-popup-surface-lf.patch

# 6. 验证
git diff --stat HEAD
```
