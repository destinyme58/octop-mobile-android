# Octop Mobile - Destiny Build

[English](./README.md) | [简体中文](./README.zh-CN.md)

An independent, unofficial Android build of [huangcheng/OctopMobile](https://github.com/huangcheng/OctopMobile), a polished Expo/React Native companion for self-hosted [Octop](https://github.com/TencentCloud/Octop). This derivative is based on upstream tag `v1.0.0` (source commit `524f3564ff192c86c73778d33ea58b4de87b0e8e`).

> **Credit:** The original application, design and implementation were created by [@huangcheng](https://github.com/huangcheng). Thank you for publishing such a complete mobile client. This repository is not affiliated with or endorsed by the author, TencentCloud or the Octop project.

## What this build changes

- Fixed the Android release manifest so local `http://` servers work reliably.
- Keeps the original one-time cleartext-HTTP warning; `https://` remains strongly recommended.
- Uses the independent package id `com.destinyme.octopmobile`, so it can coexist with the author's APK.
- Removes the upstream EAS owner/project binding and builds locally with a separate signing key.
- Prevents a failed login (HTTP 401) from clearing the form and hiding the error.
- Version: `1.1.0`, based on upstream `v1.0.0` (source commit `524f3564ff192c86c73778d33ea58b4de87b0e8e`).

See [ATTRIBUTION.md](./ATTRIBUTION.md) for the complete provenance and modification record. The unmodified upstream READMEs are preserved as [UPSTREAM_README.md](./UPSTREAM_README.md) and [UPSTREAM_README.zh-CN.md](./UPSTREAM_README.zh-CN.md).

## Features

- Login with a user-supplied HTTP or HTTPS Octop server URL.
- Streaming chats over HTTP/WebSocket with reconnect and thread history.
- Experts, knowledge bases, scheduled jobs, themes, localization and settings.
- Same-origin WebView console with the existing session handed off safely.
- Access tokens stored with `expo-secure-store`; TLS verification is never bypassed.

## Security

Plain HTTP does not encrypt passwords, access tokens or chat content on the local network. Use HTTPS for any network you do not fully trust. The app accepts HTTP only because self-hosted Octop is often used on a private LAN and shows a warning before the first HTTP request.

## Build

Requirements: Node.js 20+, JDK 17+, Android SDK Platform 36 and Build Tools 36.

```bash
npm ci
npx expo prebuild --platform android --clean
cd android
./gradlew assembleRelease
```

The generated manifest must contain:

```xml
<application android:usesCleartextTraffic="true" ...>
```

Sign the release APK with your own long-lived release keystore. Never commit the keystore or its passwords.

## License

The upstream `LICENSE` is preserved verbatim. Upstream's README describes the project as Apache-2.0 while the distributed `LICENSE` is the Expo MIT template; see [ATTRIBUTION.md](./ATTRIBUTION.md). Retain the upstream notice when redistributing this derivative.
