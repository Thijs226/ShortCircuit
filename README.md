# Short Circuit

Short Circuit is an Android app that helps you stop endlessly scrolling YouTube Shorts. You get one free minute of scrolling every hour, and you can solve a sliding ball maze to earn a longer break.

## Download

**[Download Short Circuit v1.5 for Android](downloads/ShortCircuit-v1.5.apk?raw=1)**  
Requires Android 8.0 or newer and the official YouTube app.

1. Download and install the APK. You may need to allow installation from your browser or file manager.
2. Open Short Circuit and select **Set up blocking**.
3. Enable **Short Circuit blocker** in Android Accessibility settings.
4. If Android shows **Restricted setting**, open the app's system information page, tap the three-dot menu, and select **Allow restricted settings**, then enable the service.

When updating from an earlier version, switch the accessibility service off and back on. The published APK uses the same signing certificate as the previous personal builds.

## How it works

- **One free minute per hour:** the minute starts automatically when you open Shorts while it is available. You can watch and scroll through any Shorts during that minute. The next minute becomes available 60 minutes after the previous one started.
- **A gentle stop:** once a break ends, finish the current Short. The next detected swipe returns you to YouTube Home.
- **Puzzle breaks:** solve a sliding tile maze to earn more scrolling time. Connect the ball to the goal by moving the tiles into empty spaces.

| Difficulty | Board | Empty spaces | Break |
| --- | --- | --- | --- |
| Easy | 3 × 3 | 1 | 2 minutes |
| Normal | 4 × 4 | 2 | 5 minutes |
| Hard | 5 × 5 | 4 | 7 minutes |
| Demon | 6 × 6 | 6 | 10 minutes |

Breaks count down even if YouTube is closed. They do not stack, and an active puzzle break does not consume your unused free minute. Restarting the phone ends an active break; the free-minute cooldown is retained where possible.

## Privacy and limitations

Short Circuit processes YouTube's accessibility interface locally. It has no Internet permission, analytics, advertising, or login. It does not save video titles or creator names. The accessibility service must remain enabled for the blocker to work.

YouTube interface changes, device behavior, and background restrictions can affect detection and Home navigation. The app currently supports the official YouTube Android app, not browser or third-party clients. If something goes wrong, open **Setup help & privacy > Copy diagnostics** in the app. The report contains technical event details and version information, not video titles or account details.

## Build from source

You need JDK 17 or later, Android SDK platform 35, Android Build Tools 35.0.0, Python 3, and Bash. No Gradle or third-party Android runtime dependencies are required. Set `JAVA_HOME` and `ANDROID_HOME`, or place them in `tools/jdk` and `tools/android`.

```bash
bash test.sh
read -rs -p 'Signing keystore password: ' SC_KEYSTORE_PASSWORD; echo
export SC_KEYSTORE_PASSWORD
bash build.sh
```

The build creates `dist/ShortCircuit-v1.5.apk`. A new signing keystore is generated if none is present. To update an already installed build, use the same signing key as that build via `SIGNING_KEY=/path/to/build.keystore`. An APK signed with a different key cannot update the published version in place. Keep your own signing key and password private and backed up.

The v1.5 build passed 64 automated core checks. It targets Android API 34, compiles against API 35, and supports API 26 and later. Source artwork is in [`art/`](art/); the runtime texture atlas is in `res/drawable-nodpi/`.

## License

Short Circuit is available for noncommercial use under [CC BY-NC-SA 4.0](LICENSE), with [additional permissions](LICENSE-ADDITIONAL-PERMISSIONS.md) waiving credit and change-notice requirements for the original material. You may modify and share it noncommercially; shared adaptations must use the same or a compatible ShareAlike license. Preserve license and warranty-disclaimer notices. Other contributors may have their own attribution requirements.

Because commercial use is restricted, this is **source-available**, not an OSI-approved open-source license.
