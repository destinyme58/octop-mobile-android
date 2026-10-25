const { withAndroidManifest } = require("@expo/config-plugins");

/**
 * Expo SDK 57 does not consistently project `android.usesCleartextTraffic`
 * into release manifests. Force the attribute so LAN HTTP remains usable while
 * the app itself continues to show its one-time cleartext warning.
 */
module.exports = function withCleartextTraffic(config) {
  return withAndroidManifest(config, (config) => {
    const application = config.modResults.manifest.application?.[0];
    if (!application) {
      throw new Error("Android application node is missing during prebuild");
    }
    application.$ = application.$ ?? {};
    application.$["android:usesCleartextTraffic"] = "true";
    return config;
  });
};
