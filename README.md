# Pronto Soggiorno

Independent, open-source Android app that checks the status of an Italian residence permit
(*permesso di soggiorno*) and notifies you when it changes — for example when it is ready for collection.

> **Not affiliated** with the Polizia di Stato, the Ministero dell'Interno or Poste Italiane.
> The app reads the public status page at `https://questure.poliziadistato.it/stranieri/`,
> the same one you can open in a browser. The official portal is always the authoritative source.

## Privacy principles
- **One external connection only:** HTTPS requests to `questure.poliziadistato.it`. Enforced in code
  by an allowlist (`core/.../PortalOnlyInterceptor.kt`); redirects are never followed.
- No backend, no accounts, no analytics, no ads, no crash reporting, no remote fonts.
- Application numbers stay on the device (encrypted with the Android Keystore) and are never logged.
- Only your own numbers: the app is not a tool to look up other people's applications.

## Project structure
| Module | Content |
|---|---|
| `core/` | Pure Kotlin: portal client, tolerant RSS parser, status classifier. Unit tested, no Android. |
| `app/` | Android app: Jetpack Compose + Material 3 screens (MVVM, Navigation Compose), Room history, DataStore settings, Android Keystore encryption, WorkManager background checks, notifications. |

## Build & test
Requires a JDK 17+ (and the Android SDK for `:app`).

```
./gradlew :core:test           # brain: client, parser, classifier
./gradlew :app:assembleDebug   # Android debug APK
```

Optional live check against the real portal with **your own** number (never committed):

```powershell
$env:PORTAL_SMOKE_NUMBER = "<your number>"; ./gradlew :core:test --tests "*LivePortalSmokeTest*"
```

## Adding a new portal status
The classifier only knows texts verified against real responses. To add one, see
`core/src/test/resources/fixtures/README.md` — never query numbers that are not yours.

## Translations
The app is available in Italian, English, Spanish, French, Arabic, Russian and Ukrainian
(`app/src/main/res/values-xx/`). Rules for translators:
- Keep the Italian administrative terms as they are (*pratica*, *assicurata*, *Questura*,
  *permesso di soggiorno*): they are the words printed on the documents and on the portal.
- Never describe the app as "official" or as speaking for the Polizia di Stato.
- The text published by the portal is always shown in Italian, untranslated.

Check that all languages have the same keys and placeholders:
```
python tools/check_strings.py app/src/main/res
```
Debug builds include the pseudo-locales `en-XA` (long texts) and `ar-XB` (right-to-left) for layout testing.

## License
[GPL-3.0](LICENSE). Any redistributed version must remain open source.

Third-party assets bundled in the app:
- [Inter](https://github.com/rsms/inter) typeface — SIL Open Font License 1.1 ([third_party/inter/OFL.txt](third_party/inter/OFL.txt)).
- Material Icons (vector paths) — Apache License 2.0.
