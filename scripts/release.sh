#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { printf '%s\n' "$*" >&2; exit 1; }
version="${1:-}"
mode="${2:---prepare}"
[[ $# -ge 1 && $# -le 2 && "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || fail 'Usage: scripts/release.sh VERSION [--prepare|--publish]'
[[ "$mode" == --prepare || "$mode" == --publish ]] || fail 'Unknown release mode.'
configured_version="$(sed -n 's/.*versionName = "\([^"]*\)".*/\1/p' app/build.gradle.kts)"
[[ "$version" == "$configured_version" ]] || fail 'VERSION must match versionName in app/build.gradle.kts.'
[[ -f keystore.properties ]] || fail 'Configure your permanent release key in keystore.properties first.'
[[ -n "${ANDROID_HOME:-}" ]] || fail 'Set ANDROID_HOME to your Android SDK.'
signer="$ANDROID_HOME/build-tools/36.0.0/apksigner"
[[ -x "$signer" ]] || fail 'Install Android Build Tools 36.0.0.'
notes="docs/releases/$version.md"
[[ -f "$notes" ]] || fail "Missing release notes: $notes"
if git ls-files --error-unmatch keystore.properties >/dev/null 2>&1; then
    fail 'Signing credentials must never be tracked by Git.'
fi

tag="v$version"
if [[ "$mode" == --publish ]]; then
    [[ -z "$(git status --porcelain)" ]] || fail 'Commit your changes before publishing.'
    gh auth status
    repository="$(gh repo view --json nameWithOwner --jq .nameWithOwner)"
    if git show-ref --verify --quiet "refs/tags/$tag"; then fail "Local tag already exists: $tag"; fi
    [[ -z "$(git ls-remote --tags origin "refs/tags/$tag")" ]] || fail "Remote tag already exists: $tag"
fi

python3 scripts/check-locales.py
./gradlew testDebugUnitTest lintRelease assembleRelease assembleDebugAndroidTest --console=plain
mkdir -p dist
apk="dist/whyiamhere-$version.apk"
cp app/build/outputs/apk/release/app-release.apk "$apk"
"$signer" verify "$apk"
checksum_file="dist/whyiamhere-$version.apk.sha256"
(cd dist && sha256sum "whyiamhere-$version.apk" > "whyiamhere-$version.apk.sha256")
printf 'Prepared %s\n' "$apk"

if [[ "$mode" == --publish ]]; then
    git tag -a "$tag" -m "Why am I here? $version"
    git -c credential.helper= -c 'credential.helper=!gh auth git-credential' push --atomic origin HEAD "refs/tags/$tag"
    gh release create "$tag" "$apk" "$checksum_file" --repo "$repository" --verify-tag --draft \
        --title "Why am I here? $version" --notes-file "$notes"
    verification_dir="$(mktemp -d)"
    trap 'rm -rf -- "$verification_dir"' EXIT
    gh release download "$tag" --repo "$repository" --pattern "whyiamhere-$version.apk" --dir "$verification_dir"
    cmp -- "$apk" "$verification_dir/whyiamhere-$version.apk"
    gh release edit "$tag" --repo "$repository" --draft=false --latest --verify-tag
    gh release view "$tag" --repo "$repository" --json url --jq .url
fi
