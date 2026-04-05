# Release build (APK)

## Signing (recommended)
Create `keystore.properties` in project root (do not commit):

```
M9_KEYSTORE_PATH=/absolute/path/to/keystore.jks
M9_KEYSTORE_PASS=your_store_password
M9_KEY_ALIAS=your_key_alias
M9_KEY_ALIAS_PASS=your_key_password
```

You can also provide these values via environment variables with the same names.

## Build
In Android Studio:
1) Open the project.
2) Gradle tool window → Tasks → build → `assembleRelease`.

CLI (after Android Studio generates the wrapper JAR or if Gradle is installed):
```
./gradlew assembleRelease
```

APK output:
`app/build/outputs/apk/release/app-release.apk`
