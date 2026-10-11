# F-Droid submission

Status: submitted on 2026-10-11 as
[fdroiddata !52121](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/52121).
The merge request is open; this project is not yet listed or approved by F-Droid.

The fork pipeline failed before creating any jobs (no YAML error was reported).
The merge request asks F-Droid maintainers to trigger upstream CI. Official
buildserver validation is still pending. The recipe now targets v0.3.1
(versionCode 4); three English screenshots from a real phone are included.

## Submission procedure (already completed)

Do not create a duplicate request; use the existing merge request above.

1. Register at https://gitlab.com/users/sign_up and verify your email.
2. Fork https://gitlab.com/fdroid/fdroiddata into your GitLab account.
3. On a new branch, add `dev.cena.whyiamhere.yml` from this directory as
   `metadata/dev.cena.whyiamhere.yml` in that fork.
4. Open a merge request targeting `fdroid/fdroiddata`, with the title
   `New App: Why am I here? (dev.cena.whyiamhere)` and the body below.
5. Check the CI results and respond to reviewer requests. Publication depends
   on review and successful builds on F-Droid infrastructure.

The current recipe targets v0.3.1. Descriptions, translations,
changelog and a 512×512 PNG rendered from the app's vector icon are in
`fastlane/metadata/android/`. Three English screenshots supplied by the maintainer are stored as
`en-US/images/phoneScreenshots/1.jpg`, `2.jpg` and `3.jpg`: settings, the YouTube
goal form, and the floating widget with English buttons. They are also displayed
in the GitHub READMEs and the F-Droid merge request.

## Merge request body

New application: **Why am I here?**, a small offline tool for intentional app use.

- Source: https://github.com/hlophlopgaming/whyiamhereAndroid
- License: MIT
- Application ID: `dev.cena.whyiamhere`
- Release: https://github.com/hlophlopgaming/whyiamhereAndroid/releases/tag/v0.3.1
- Version: 0.3.1 / versionCode 4
- Minimum Android version: 8.0 (API 26)
- Build: Gradle 9.3.1, AGP 9.1.1, OpenJDK 17, Android SDK Platform 37.0,
  Build Tools 36.0.0
- Languages: English, Spanish and Russian
- No internet permission, accounts, advertisements, analytics or proprietary SDKs
- The author requests inclusion and intends to maintain the application.

An AccessibilityService detects the active app's package, displays a goal and
timer overlay, and performs the Home action when a session expires. It does not
read screen text or capture screenshots. This is a voluntary reminder; the
service can be disabled and returning home does not stop background audio.

The recipe requests reproducible builds with the existing upstream signing key.
No private key is needed or provided. An independent unsigned build of commit
`3e1934f4945866319f814ad377e6476f9e598342` passed `apksigcopier compare --unsigned`
against the published release APK, using the same local toolchain. Official
F-Droid CI/buildserver verification is still needed.

Local validation on 2026-10-11:

- `fdroid rewritemeta` and `fdroid lint` passed with fdroidserver 2.4.5 and
  the current fdroiddata category definitions.
- `fdroid scanner --exit-code` passed on the release APK.
- Release build from a separate checkout without signing credentials passed.
- `apksigcopier compare --unsigned` passed with apksigcopier 1.1.1.
- Store descriptions in three languages and the icon are available upstream.

The complete official buildserver pipeline has not been run. The app-language
fix was manually verified on a Samsung Galaxy S20+ running Android 13. The
regression test was compiled but has not been executed on the phone. Please advise if any packaging changes are needed.

## Reproduce the local APK comparison

Use a separate clone; do not copy `keystore.properties` into it. A Git checkout
is required: AGP includes the Git revision in `META-INF/version-control-info.textproto`,
so building a source archive without `.git` produces different APK metadata.

```sh
git clone https://github.com/hlophlopgaming/whyiamhereAndroid.git why-fdroid-check
cd why-fdroid-check
git checkout v0.3.1
# Set JAVA_HOME to OpenJDK 17 and ANDROID_HOME to your Android SDK.
./gradlew assembleRelease --console=plain
curl -fL https://github.com/hlophlopgaming/whyiamhereAndroid/releases/download/v0.3.1/whyiamhere-0.3.1.apk -o upstream.apk
# Install apksigcopier and put apksigner and java on PATH.
apksigcopier compare --unsigned upstream.apk app/build/outputs/apk/release/app-release-unsigned.apk
```

Expected signing certificate SHA-256:
`17ef695c03f3419115129e1c341e94d1cb43f78091f02c19202bac2cf54466e9`.

The metadata enables update checks on version tags and matches upstream APK
URLs. Future releases must retain the permanent signing key and remain
reproducible; otherwise F-Droid cannot publish those APKs with this recipe.

References:
- https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/
- https://f-droid.org/docs/Reproducible_Builds/
