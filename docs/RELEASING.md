# Releasing

The APK release is built locally and published to the existing GitHub repository.
GitHub CLI must be authenticated with permission to push and create releases.
The script works in Bash with Git, Python 3, `sha256sum`, JDK 17, and Android SDK
Platform 37.0 / Build Tools 36.0.0. Set `JAVA_HOME` and `ANDROID_HOME`.

## Signing

Keep one permanent signing key for every public version. Configure it using
`keystore.properties.example`; the real `keystore.properties` and keys are ignored
by Git. Do not paste passwords into release notes, commit them, or upload the key
as a release asset. Back up the key and passwords in a secure location: losing
them means losing the ability to update existing installations.

The maintainer's v0.3.0 key and private recovery configuration are stored in
`~/.local/share/whyiamhere/signing/`, outside this repository. Release builds use
that key, have debugging disabled, and enable R8 and resource shrinking.
The older v0.2.0 debug APK has a different signature and must be uninstalled
before the first permanent release is installed; its local data will be deleted.

## Prepare

1. Increase both `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Add `docs/releases/VERSION.md` with English, Spanish, and Russian notes.
3. Check translations and run the release-script tests:

```sh
python3 scripts/check-locales.py
python3 -m unittest discover -s scripts/tests -v
scripts/release.sh 0.3.0 --prepare
```

This checks translations, runs JVM tests and release lint, compiles device tests,
builds the signed release APK, verifies its signature, and writes these files:

```text
dist/whyiamhere-0.3.0.apk
dist/whyiamhere-0.3.0.apk.sha256
```

Check the APK on a phone using the checklist in the README. The script does not
launch an emulator or claim that compiled device tests were executed.

## Publish

Commit the intended source changes first. Use a clean working tree and a branch
that can be pushed to `origin`; publishing refuses existing local or remote tags.

```sh
scripts/release.sh 0.3.0 --publish
```

This repeats the build and checks, creates annotated tag `v0.3.0`, atomically
pushes the current branch and tag without forcing history, then creates a draft
release with the APK and checksum. It downloads the APK from GitHub and compares
it byte for byte before making the release public and marking it Latest.

If upload or verification fails, the release stays a draft. Inspect the error
and the draft before retrying; the script never overwrites an existing version.

To verify a downloaded APK locally, keep both files together and run:

```sh
sha256sum -c whyiamhere-0.3.0.apk.sha256
```
