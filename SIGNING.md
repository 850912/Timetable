# Release signing

The repository intentionally contains **no release keystore and no signing password**.
Production APKs keep the existing/original signing identity by supplying the same JKS only at build time.

## Local build

Keep the original JKS outside the repository and set these environment variables before building:

- `TIMETABLE_RELEASE_STORE_FILE` — absolute or project-relative path to the original JKS
- `TIMETABLE_RELEASE_STORE_PASSWORD` — keystore password
- `TIMETABLE_RELEASE_KEY_ALIAS` — key alias
- `TIMETABLE_RELEASE_KEY_PASSWORD` — key password

Then run:

```bash
./gradlew :mobile:assembleRelease :wear:assembleRelease
```

Release tasks intentionally fail if any signing value is missing. There is no debug-signing fallback for a release build.

## GitHub Actions

Add these **Repository secrets** under **Settings → Secrets and variables → Actions**:

- `TIMETABLE_RELEASE_JKS_BASE64`
- `TIMETABLE_RELEASE_STORE_PASSWORD`
- `TIMETABLE_RELEASE_KEY_ALIAS`
- `TIMETABLE_RELEASE_KEY_PASSWORD`

Encode the original JKS on the phone/computer without changing the file itself. Example on Linux/Termux:

```bash
base64 -w 0 timetable-release.jks > timetable-release.jks.base64
```

Copy the content of `timetable-release.jks.base64` into the `TIMETABLE_RELEASE_JKS_BASE64` secret. Base64 is only a transport encoding; secrecy comes from GitHub Actions Secrets.

The workflow `.github/workflows/release.yml` decodes the JKS into `$RUNNER_TEMP`, verifies the alias with `keytool`, builds signed mobile + Wear release APKs, uploads the APK artifacts, and removes the temporary JKS.

## Important

- Never commit `*.jks`, `*.keystore`, `keystore.properties`, passwords, or Base64 keystore content.
- The phone and Wear APKs should continue using the same release certificate used by previous production versions.
- If the old JKS/password was ever committed to a public or untrusted repository, removing it from the current tree does **not** erase Git history. Treat that key as potentially exposed and assess whether key rotation is possible for your distribution channel.
