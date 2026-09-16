# Releasing

## One-time setup (only for whoever signs releases)

Create a keystore and a `keystore.properties` beside it in the repository root:

```properties
storeFile=catslist-release.jks
storePassword=…
keyAlias=catslist
keyPassword=…
```

Both files are gitignored and must stay that way. Without them the build still
works — `assembleRelease` just produces an unsigned APK, which is what CI and a
fresh clone get. Only a machine holding the key can produce a signed build.

CI can supply the same four values as `CATSLIST_STOREFILE`,
`CATSLIST_STOREPASSWORD`, `CATSLIST_KEYALIAS` and `CATSLIST_KEYPASSWORD`.

## Cutting a release

1. **Bump the version** in `app/build.gradle.kts` — only `versionMajor`,
   `versionMinor`, `versionPatch`. `versionCode` and `versionName` are derived
   from them ([ADR-0021](docs/DECISIONS.md#adr-0021)), so there is no second
   number to remember.
2. **Merge to `dev`** as usual, with CI green.
3. **Merge `dev` into `master`** via a pull request, so the release commit is on
   `master` and has passed the same gate.
4. **Build and check the signature:**

   ```bash
   ./gradlew assembleRelease
   apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
   ```

   An `app-release-unsigned.apk` means the keystore was not picked up.
5. **Tag `master`** and push the tag:

   ```bash
   git tag -a v2.0.0 -m "v2.0.0"
   git push origin v2.0.0
   ```
6. **Create the GitHub release** from the tag and attach the signed APK.

## Known limitations

- **R8 is off** (`isMinifyEnabled = false`), so the APK is neither shrunk nor
  obfuscated. Deliberate for now: keep rules that are never exercised are worse
  than none, so R8 gets turned on and verified as its own change.
- Nothing publishes to Play. The GitHub release is the distribution point.
- Neither the release build nor the instrumented tests run in CI
  ([ADR-0018](docs/DECISIONS.md#adr-0018)).
