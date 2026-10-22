# Releasing ExpiryWatch

How release APKs are signed and published. For everyday debug builds, see
[Building](README.md#building) in the README.

## Signing

Release builds are minified with R8. To sign them with your own key, create `keystore.properties`
in the project root (it's git-ignored):

```properties
storeFile=path/to/release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

Without it, release builds are signed with the debug key.

To sign the release APKs built by CI and the release workflow, add these repository secrets
(Settings → Secrets and variables → Actions):

| Secret | Value |
|--------|-------|
| `RELEASE_KEYSTORE_BASE64` | The keystore file, base64-encoded on a single line (`base64 -w 0 release.jks`) |
| `RELEASE_KEYSTORE_PASSWORD` | The keystore password |
| `RELEASE_KEY_ALIAS` | The key's alias |
| `RELEASE_KEY_PASSWORD` | Optional: the key's password, if it differs from the keystore's |

## Publishing a release

First move the changes under **Unreleased** in [CHANGELOG.md](CHANGELOG.md) to a section for the
new version, with the date.

The release workflow publishes a [GitHub release](https://github.com/abhi-k9/ExpiryWatch/releases)
with signed APKs of both flavors and their SHA-256 checksums. Start it either way:

- In the Actions tab, open **Release**, choose **Run workflow** and enter the version, such as
  `1.2.0`. The workflow tags the branch's latest commit as `v1.2.0`.
- Or push a tag: `git tag v1.2.0 && git push origin v1.2.0`.

The version sets the app's version name (`1.2.0`) and version code (`10200`), so each part must be
between 0 and 99. Local builds are version `1.0.0` unless you pass
`-Pexpirywatch.versionName=1.2.0`. The workflow refuses to publish without the signing secrets.
