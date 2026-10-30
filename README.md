# LBC – Low Battery Camera (Android)

A simple camera app for photos and video that is gentle on your battery, with
AI-written **Giveaway**, **For Sale** and **Swap** cards: take a photo or video of
an item and get a ready-to-share card with a short, friendly description.

Also available on iOS. Website: [gogoodmood.com](https://gogoodmood.com)

## Features

- **Photo and video camera** built on CameraX, with zoom, front/back switch, flash,
  timer and a dim mode to save power.
- **AI card button.** One tap turns your photo or video into a shareable card:
  - **Giveaway** – "free to a good home", with a description in your language.
  - **For Sale** – a price tag in one of 55 currencies, plus a short description.
  - **Swap** – say what you are looking for, or leave it as "open to offers".
- **Private gallery** protected by fingerprint, face or a passcode.
- **Languages:** English, German, French, Indonesian, Japanese, Korean,
  Chinese (Simplified and Traditional) and Filipino (Taglish).
- **Privacy first:** the AI is only used when you tap *Fill with AI*, and you are
  asked for consent before anything is sent.

## Tech stack

| Area | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Camera | CameraX |
| AI | Claude (Haiku) through a small Cloudflare Workers endpoint |
| Splash | AndroidX Core SplashScreen |
| Min / target SDK | 24 / 36 |

## Project layout

```
app/src/main/java/com/example/bodycamoutdoorgear/
  ui/            Compose screens (camera, gallery, settings, welcome)
  giveaway/      Card content, rendering, AI client and legal text
  viewmodel/     Settings and camera state
  privategallery/ Locked gallery
  i18n/          Translation helper (JSON files in assets/translations)
```

## Building

1. Open the project in Android Studio (latest stable).
2. Let Gradle sync, then run the `app` configuration on a device or emulator.

Release signing uses a local `keystore.properties` that is not part of this
repository; debug builds need nothing extra.

## How it was built

Developed by Pieter Natanael together with [Claude Code](https://claude.com/claude-code):
features are planned in conversation, built on branches and merged after review.

## License

All rights reserved. The source is visible for review only; see [LICENSE](LICENSE).
