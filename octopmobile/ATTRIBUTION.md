# Attribution and upstream notice

This build is based on the open-source project [huangcheng/OctopMobile](https://github.com/huangcheng/OctopMobile), fixed at upstream tag `v1.0.0` (tag object `cea2deed87fcbd392190c0b72e640cdb887eec0a`; source commit `524f3564ff192c86c73778d33ea58b4de87b0e8e`).

Thank you to the author, [@huangcheng](https://github.com/huangcheng), for designing and publishing this excellent Octop mobile client. The interface, interaction model and original implementation are the author's work. This repository keeps the upstream copyright and license notice.

## Changes made by the Destiny build

- Changed the Android package to `com.destinyme.octopmobile` so it can coexist with the author's build.
- Removed the upstream Expo/EAS owner and project binding; releases are built locally with an independent signing key.
- Added a native Expo config plugin that reliably enables Android cleartext traffic for self-hosted LAN servers. The existing one-time HTTP warning remains in place.
- Prevented an expected HTTP 401 on the login screen from remounting the page and clearing the username/password fields.
- Updated the version to `1.1.0` and added release/build documentation.

No upstream code should be treated as an official TencentCloud or Octop product. This repository is an independent, unofficial derivative.

## License note

Upstream's README calls the project Apache-2.0, while the `LICENSE` file shipped at `v1.0.0` is the Expo MIT template. The upstream `LICENSE` file is preserved verbatim in this directory. This notice records the discrepancy rather than asserting a license that upstream did not consistently provide.
