# F-Droid submission

The existing [fdroiddata !52121](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/52121)
requests inclusion. Do not create a duplicate request. Publication requires
successful official CI and maintainer review; the app is not yet listed.

The recipe targets only [0.3.3](https://github.com/hlophlopgaming/whyiamhereAndroid/releases/tag/v0.3.3)
(versionCode 6), commit `4f970573e82aef5db9dd21907fce3aa771852f0d`.
It retains the permanent upstream signing key and uses reproducible builds.
Store descriptions, changelogs in English, Spanish and Russian, the icon,
and three real-device English screenshots are in `fastlane/metadata/android/`.

## Pipeline fixes

[The 0.3.1 pipeline](https://gitlab.com/fdroid/fdroiddata/-/pipelines/2938736065)
confirmed a reproducible APK but failed four jobs:

- `check apk`: encrypted dependency metadata in the APK signing block.
  Release 0.3.3 disables it with `dependenciesInfo { includeInApk = false }`.
- `schema validation`: corrected `AutoUpdateMode` to `Version`.
- `fdroid rewritemeta`: regenerated canonical YAML, using ruamel.yaml 0.18.10
  to match CI.
- `checkupdates`: replaced old builds with the latest release and set the
  current version and full commit hash accordingly.

## Local validation on 2026-10-12

- 14 JVM tests, 6 release-script tests, translations and release lint passed.
- Device tests compiled; they were not executed for this packaging change.
- Current fdroidserver master APK scanner passed with refreshed signatures.
- `fdroid lint`, `fdroid rewritemeta` and the current fdroiddata JSON schema passed.
- A separate Git clone built the release commit without signing credentials.
  `apksigcopier compare --unsigned` passed against the signed release APK.
- The release script verified the uploaded APK byte for byte before publication.

These local results do not replace official CI validation of 0.3.3.
The recipe and MR description were updated to 0.3.3. The new
[fork pipeline](https://gitlab.com/hlophlopgaming/fdroiddata/-/pipelines/2939576242)
failed without creating any jobs; its API reports `yaml_errors: null`.
The submitting account has no project or group access to `fdroid/fdroiddata`.
A F-Droid maintainer must trigger the pipeline in the parent project, as for
the previous upstream run. No paid CI or account-verification changes were made.

## Reproduce the APK comparison

Use a Git clone without `keystore.properties`: AGP records the source revision
in the APK, so an archive without `.git` is not equivalent.

```sh
git clone https://github.com/hlophlopgaming/whyiamhereAndroid.git why-fdroid-check
cd why-fdroid-check
git checkout 4f970573e82aef5db9dd21907fce3aa771852f0d
# Set JAVA_HOME and ANDROID_HOME; put java and apksigner on PATH.
./gradlew assembleRelease --console=plain
curl -fL https://github.com/hlophlopgaming/whyiamhereAndroid/releases/download/v0.3.3/whyiamhere-0.3.3.apk -o upstream.apk
apksigcopier compare --unsigned upstream.apk app/build/outputs/apk/release/app-release-unsigned.apk
```

Expected signing certificate SHA-256:
`17ef695c03f3419115129e1c341e94d1cb43f78091f02c19202bac2cf54466e9`.

The metadata enables checks on release tags and versioned APK URLs. Future
releases must retain the permanent signing key and remain reproducible.
