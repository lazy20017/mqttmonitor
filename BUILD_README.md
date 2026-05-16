# 构建说明

## 快速开始

### 方式一：双击运行
```
双击 build.bat
```

### 方式二：命令行

```powershell
# 调试版本
.\build.ps1

# 清理后重新编译
.\build.ps1 -Clean

# 发布版本
.\build.ps1 -Release

# 编译并安装到设备
.\build.ps1 -Install
```

或者使用批处理文件：
```cmd
build.bat                 # 调试版本
build.bat clean           # 清理
build.bat release         # 发布版本
build.bat install         # 编译并安装
```

## 输出

编译成功后，APK 文件位于：
- 调试版本：`app\build\outputs\apk\debug\app-debug.apk`
- 发布版本：`app\build\outputs\apk\release\app-release.apk`

## 需求

- Android SDK
- Gradle (项目已包含 gradlew)
- USB 调试模式已开启（用于安装）
