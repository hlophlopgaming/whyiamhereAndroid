# F-Droid submission

The existing [fdroiddata !52121](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/52121)
requests inclusion. Do not create a duplicate request. Publication requires
successful official CI and maintainer review; the app is not yet listed.

The recipe targets only [0.3.4](https://github.com/hlophlopgaming/whyiamhereAndroid/releases/tag/v0.3.4)
(versionCode 7), commit `33e383245d6e1c65bad20731887d2a2bdfeee084`.
It retains the permanent upstream signing key and uses reproducible builds.
Store descriptions, changelogs in English, Spanish and Russian, the icon,
and three real-device English screenshots are in `fastlane/metadata/android/`.

## Status

The [official 0.3.3 pipeline](https://gitlab.com/fdroid/fdroiddata/-/pipelines/2939621668)
passed. Reviewer testing on Android 16 then found a gate-launch race: the
one-second fallback could return home while accessibility events still reported
the previous app. Version 0.3.4 makes the gate acknowledge its launch and cancel
the fallback directly; a launch that never completes has a five-second timeout.

The MR recipe was updated in commit
`09f19d10c5ec6700da0d802dd6f547ab5667ca0a`.
The new [fork pipeline](https://gitlab.com/hlophlopgaming/fdroiddata/-/pipelines/2953140021)
failed without creating any jobs; its API reports `yaml_errors: null`.
A F-Droid maintainer must trigger CI in the parent project for this new commit.
Local checks do not replace official CI validation of 0.3.4.

## Local validation on 2026-10-16

- 14 JVM tests, 6 release-script tests and 52 translated strings checked.
- Release lint passed with zero errors and ten warnings; the signed APK built.
- All five Android tests passed on Android 16 (API 36) and Android 17 (API 37).
  Chrome opened the gate in five out of five attempts on each debug build.
- A temporary six-second accessibility-event delay reproduced the erroneous
  return home with launch acknowledgement disabled and passed with it enabled
  on both Android versions. All temporary source changes were removed.
- The signed 0.3.4 APK updated over signed 0.3.3 on Android 16 and passed five
  Chrome gate openings with no fallback warning or crash in logcat.
- Separate per-app timers and silent mode were not checked in this round.
- Current fdroidserver master APK scanner passed with refreshed signatures.
- `fdroid lint` and canonical `fdroid rewritemeta` passed using ruamel.yaml
  0.18.10, matching the existing CI formatting.
- A separate Git clone built the release commit without signing credentials.
  `apksigcopier compare --unsigned` passed against the signed release APK.
- The release script downloaded the uploaded APK and verified it byte for byte
  before publication. The checksum and permanent signing certificate match.

## Reproduce the APK comparison

Use a Git clone without `keystore.properties`: AGP records the source revision
in the APK, so an archive without `.git` is not equivalent.

```sh
git clone https://github.com/hlophlopgaming/whyiamhereAndroid.git why-fdroid-check
cd why-fdroid-check
git checkout 33e383245d6e1c65bad20731887d2a2bdfeee084
# Set JAVA_HOME and ANDROID_HOME; put java and apksigner on PATH.
./gradlew assembleRelease --console=plain
curl -fL https://github.com/hlophlopgaming/whyiamhereAndroid/releases/download/v0.3.4/whyiamhere-0.3.4.apk -o upstream.apk
apksigcopier compare --unsigned upstream.apk app/build/outputs/apk/release/app-release-unsigned.apk
```

Expected signing certificate SHA-256:
`17ef695c03f3419115129e1c341e94d1cb43f78091f02c19202bac2cf54466e9`.

The metadata enables checks on release tags and versioned APK URLs. Future
releases must retain the permanent signing key and remain reproducible.
