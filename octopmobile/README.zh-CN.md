# Octop Mobile - Destiny 构建版

简体中文 | [English](./README.md)

这是基于 [huangcheng/OctopMobile](https://github.com/huangcheng/OctopMobile) `v1.0.0`（源码提交 `524f3564ff192c86c73778d33ea58b4de87b0e8e`）构建的非官方 Android 衍生版本。原项目是一款完成度很高的 [Octop](https://github.com/TencentCloud/Octop) 自托管服务移动客户端。

> **致谢：** 原应用的界面、设计与实现由 [@huangcheng](https://github.com/huangcheng) 完成。感谢作者公开了这套完整、优秀的移动客户端。本项目与原作者、腾讯云及 Octop 官方均无隶属或背书关系。

## 本版本的改动

- 修复 Android Release Manifest，确保局域网 `http://` 服务可以正常连接。
- 保留原作者的一次性明文 HTTP 安全提醒，仍推荐使用 `https://`。
- 使用独立包名 `com.destinyme.octopmobile`，可与作者原版同时安装。
- 移除上游 EAS owner/project 绑定，使用本项目独立签名在本地构建。
- 修复登录返回 HTTP 401 时页面重新挂载、输入框被清空且错误提示消失的问题。
- 版本为 `1.1.0`，源码固定在上游标签 `v1.0.0`（源码提交 `524f3564ff192c86c73778d33ea58b4de87b0e8e`）。

完整的来源、修改和许可证说明见 [ATTRIBUTION.md](./ATTRIBUTION.md)。上游原始 README 保存在 [UPSTREAM_README.zh-CN.md](./UPSTREAM_README.zh-CN.md)。

## 功能

- 输入自己的 HTTP/HTTPS Octop 服务器地址并登录。
- HTTP/WebSocket 流式对话、断线重连和历史消息。
- 专家、知识库、定时任务、主题、多语言与设置。
- 同源 WebView 控制台，并传递现有登录会话。
- Access Token 使用 `expo-secure-store` 保存；不会绕过 TLS 证书校验。

## 安全说明

明文 HTTP 不会加密局域网中的密码、Token 和聊天内容。只要不是完全可信的局域网，都应给 Octop 配置 HTTPS。应用允许 HTTP，是因为自托管场景常运行在家庭或办公局域网，并会在首次请求前显示风险提醒。

## 构建

环境要求：Node.js 20+、JDK 17+、Android SDK Platform 36、Build Tools 36。

```bash
npm ci
npx expo prebuild --platform android --clean
cd android
./gradlew assembleRelease
```

生成的 Manifest 必须包含：

```xml
<application android:usesCleartextTraffic="true" ...>
```

正式签名请使用长期保管的独立 release keystore，不要将密钥或密码提交到 Git。

## 许可证

本目录保留了上游原始 `LICENSE`。上游 README 标注 Apache-2.0，但发布的 `LICENSE` 文件实际为 Expo MIT 模板，详见 [ATTRIBUTION.md](./ATTRIBUTION.md)。重新分发时请保留上游声明。
