import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


SCRIPT = Path(__file__).resolve().parents[1] / "release.sh"


class ReleaseTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="release fixture ")
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        for folder in ("scripts", "app", "docs/releases", "sdk/build-tools/36.0.0", "bin"):
            (self.root / folder).mkdir(parents=True)
        shutil.copy(SCRIPT, self.root / "scripts/release.sh")
        (self.root / "app/build.gradle.kts").write_text('versionName = "0.3.0"\n')
        (self.root / "keystore.properties").write_text("fake-test-configuration\n")
        (self.root / "docs/releases/0.3.0.md").write_text("Release notes\n")
        (self.root / "scripts/check-locales.py").write_text("print('checked')\n")
        self.executable("gradlew", """#!/bin/sh
mkdir -p app/build/outputs/apk/release
printf 'signed fixture' > app/build/outputs/apk/release/app-release.apk
""")
        self.executable("sdk/build-tools/36.0.0/apksigner", """#!/bin/sh
[ "${SCENARIO:-}" != unsigned ]
""")
        fake = """#!/usr/bin/env python3
import os, sys
from pathlib import Path
name=Path(sys.argv[0]).name
args=sys.argv[1:]
with open('calls.log','a') as stream: stream.write(name+' '+ ' '.join(args)+'\\n')
scenario=os.environ.get('SCENARIO','')
if name=='git':
    if args[:2]==['ls-files','--error-unmatch']: sys.exit(1)
    if args[:2]==['status','--porcelain'] and scenario=='dirty': print(' M file')
    if args[:1]==['show-ref']: sys.exit(1)
elif args[:2]==['repo','view']: print('owner/repo')
elif args[:2]==['release','download']:
    folder=Path(args[args.index('--dir')+1])
    filename=args[args.index('--pattern')+1]
    data=b'wrong bytes' if scenario=='tampered' else b'signed fixture'
    (folder/filename).write_bytes(data)
elif args[:2]==['release','view']: print('https://github.com/owner/repo/releases/tag/v0.3.0')
"""
        self.executable("bin/git", fake)
        self.executable("bin/gh", fake)
        self.environment = os.environ.copy()
        self.environment["PATH"] = str(self.root / "bin") + os.pathsep + os.environ["PATH"]
        self.environment["ANDROID_HOME"] = str(self.root / "sdk")

    def executable(self, path, text):
        file = self.root / path
        file.write_text(text)
        file.chmod(0o755)

    def run_release(self, *args, scenario=""):
        environment = dict(self.environment, SCENARIO=scenario)
        return subprocess.run(["bash", str(self.root / "scripts/release.sh"), *args],
                              cwd=self.root, env=environment, capture_output=True, text=True)

    def calls(self):
        file = self.root / "calls.log"
        return file.read_text() if file.exists() else ""

    def test_prepare_builds_and_checks_checksum_without_publishing(self):
        result = self.run_release("0.3.0")
        self.assertEqual(result.returncode, 0, result.stderr)
        subprocess.run(["sha256sum", "-c", "whyiamhere-0.3.0.apk.sha256"],
                       cwd=self.root / "dist", check=True, capture_output=True)
        self.assertNotIn("gh release", self.calls())

    def test_invalid_or_mismatched_versions_and_modes_are_rejected(self):
        for args in [("../evil",), ("0.4.0",), ("0.3.0", "--wrong")]:
            with self.subTest(args=args):
                self.assertNotEqual(self.run_release(*args).returncode, 0)
        self.assertFalse((self.root / "dist").exists())

    def test_unsigned_apk_cannot_be_prepared(self):
        self.assertNotEqual(self.run_release("0.3.0", scenario="unsigned").returncode, 0)
        self.assertNotIn("gh release", self.calls())

    def test_dirty_tree_cannot_publish(self):
        self.assertNotEqual(self.run_release("0.3.0", "--publish", scenario="dirty").returncode, 0)
        self.assertNotIn("git tag", self.calls())

    def test_mismatched_uploaded_bytes_leave_release_draft(self):
        self.assertNotEqual(self.run_release("0.3.0", "--publish", scenario="tampered").returncode, 0)
        self.assertIn("gh release create", self.calls())
        self.assertNotIn("gh release edit", self.calls())

    def test_publish_verifies_before_making_release_public(self):
        result = self.run_release("0.3.0", "--publish")
        self.assertEqual(result.returncode, 0, result.stderr)
        calls = self.calls()
        self.assertIn("push --atomic origin HEAD refs/tags/v0.3.0", calls)
        self.assertLess(calls.index("gh release download"), calls.index("gh release edit"))
        self.assertIn("--draft=false", calls)


if __name__ == "__main__":
    unittest.main()
