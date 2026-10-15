# PercentPro — store graphics and screenshot plan

Requirements below are from Play Console Help, **"Add preview assets to showcase
your app"**. **Developer confirm** marks the judgement calls.

---

## Required assets

| Asset | Required? | Dimensions | Format | Notes |
|---|---|---|---|---|
| App icon | **Yes** | 512 × 512 px | 32-bit PNG **with alpha** | Max 1024 KB. Full square — Play applies the rounded mask and drop shadow itself, so do **not** pre-round it. No text, badges, price or ranking marks. |
| Feature graphic | **Yes** | 1024 × 500 px exactly | JPEG or 24-bit PNG, **no alpha** | Banner at the top of the listing, and the cover image if a promo video is added. Keep key content near the centre; it is cropped on some surfaces and overlaid with a play button. |
| Phone screenshots | **Yes, minimum two** | Min dimension 320 px, max 3840 px; longest side no more than 2× the shortest | JPEG or 24-bit PNG, **no alpha** | Up to **8** per device type. Recommended 1080 × 1920. |

## Optional assets

| Asset | Required? | Notes |
|---|---|---|
| Promo video | No | YouTube URL. Must be public, and its thumbnail must match the listing. |
| Tablet screenshots (7" and 10") | No | Optional unless you claim large-screen support. The app does lay out on tablets (640 dp width cap) but has not been visually verified there, so only add these if you have actually checked them. |
| 7"/10" tablet required minimum | Only for apps targeting large screens | Play asks for at least four screenshots demonstrating large-screen behaviour **for apps that declare large-screen support**. **Developer confirm:** PercentPro does not declare a large-screen category, so this likely does not apply. Do not declare it without testing. |
| Wear OS, Android TV, Android XR screenshots | No | The app targets none of these form factors. |
| Per-language screenshots | No, but recommended | Screenshot text does not carry over between locales. A separate Hindi set is worth doing. |

No alpha channel on screenshots or the feature graphic. The 512 × 512 app icon is
the only asset that takes one.

---

## Where the app icon comes from

Every icon asset is **generated**, not hand-drawn. `tools/make_launcher_icon.py`
holds the geometry once and emits all of it, so the adaptive vectors and the
legacy rasters cannot drift apart and show different marks depending on the
device's Android version. Rerun it after any design change:

```bash
python3 tools/make_launcher_icon.py
```

It produces:

- `app/src/main/res/drawable/ic_launcher_background.xml` — flat brand DeepPurple
  `#4A2C8E`, the same value as `DeepPurple` in `ui/theme/Color.kt`.
- `app/src/main/res/drawable/ic_launcher_foreground.xml` — the white `%` mark.
- `app/src/main/res/drawable/ic_launcher_monochrome.xml` — the same mark as a
  flat silhouette, for Android 13+ themed launcher icons.
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml`
  — the adaptive definition wiring the three layers together.
- `app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.png` and
  `ic_launcher_round.png` — the API 23–25 raster fallbacks, at 48/72/96/144/192 px.
- `store/graphics/ic_launcher_play_512.png` — the Play listing icon.

On API 26 and above, which is effectively every current device, the launcher
resolves to the adaptive vector, so those three drawables are what users actually
see. The rasters matter only for API 23–25.

### The Play listing icon

`store/graphics/ic_launcher_play_512.png` is **512 × 512, full square, with square
corners**. That is deliberate: Play applies its own rounded mask and drop shadow,
so artwork that is already rounded renders smaller than the keyline grid it is
measured against. It is exported at 32-bit RGBA and is well under the 1024 KB
limit. This is a separate upload — Play does not reuse the launcher icon — so it
still has to be uploaded manually from the file above.

The `%` mark sits well inside the circle Play masks to, so nothing is clipped.

### Design of the mark

Two stroked rings and a diagonal slash, in Android's 108-unit adaptive-icon
space. The mark's farthest point from the centre is 34.7 units against a 36-unit
safe radius, so a circular launcher mask cannot clip it. `LauncherIconTest`
re-measures this from the shipped vector on every test run, and also fails if the
Android template green (`#3DDC84`) ever reappears.

---

## Screenshot plan

Six screens, all of which exist in the shipped build. Capture in this order,
because the first two are what most shoppers see before the fold.

**1. Home with daily learning progress**
Shows the daily-goal card, XP, streak, next lesson and the learning-path
shortcut. Strongest single frame for the first screenshot.

**2. Lesson experience, English and Hindi**
Open any lesson and switch language in Settings first if you want the Hindi
frame. Demonstrates the bilingual claim. Capture one language per screenshot
set rather than putting both side by side, since screenshot text does not
localise.

**3. MCQ practice with a visible explanation**
Submit an answer so the explanation and the correct/your-answer labels are on
screen. This frame is what proves "explanations", which is a real differentiator
and easy for a reviewer to verify.

**4. Timed mock test with the question palette open**
Tap the palette icon so the numbered grid with answered and review-marked states
is visible, with the timer in the header.

**5. Mock-test result analysis**
The score, negative-marking summary and the review/answer breakdown.

**6. Achievements and Learning insights**
Either screen works; insights is the stronger differentiator because it shows
per-topic accuracy and weak-topic detection. Include achievements if you have one
screen budget to spare.

All six are reachable without any setup beyond installing the app and tapping
through, and none requires a login.

---

## Capture steps

Automated capture was not possible here — no emulator or device is available in
this environment. To capture real screenshots:

**On a physical device (best colour fidelity):**

1. Install the signed release build:
   `adb install -r app/build/outputs/apk/release/app-release.apk`
2. Launch `com.atlabs.percentpro` and complete onboarding (or skip it).
3. Set the device to a fixed 1080 × 1920 or larger screen, animations off
   (Developer options → Animation scale 0), and the system font scale at 100% so
   text renders at its intended size.
4. Turn on dark mode too if you intend to submit a dark set; the app follows the
   system setting.
5. Navigate to each screen and capture with `adb exec-out screencap -p > 01-home.png`.
   Capture full frames, not partials.
6. Convert if needed (`PNG → JPEG` at quality 90 if file size is a problem) and
   **strip any alpha channel**.
7. Verify each file: 320–3840 px per side, longest side no more than 2× the
   shortest, no alpha, under the size limit.

**On an emulator:** create an AVD at API 36 (the app's targetSdk; minSdk is 23) or
any recent system image, use the same settings, and drive the UI with `adb shell
input tap` against the `testTag`s already in the code (`mock_landing_list`,
`achievements_list`, `settings_version`, and so on). The instrumented journey
tests in `app/src/androidTest` use those same tags.

**Verify before uploading:** open every PNG in an image viewer and confirm the text
is legible at Play's rendering size, and that no screenshot shows a status bar
clock reading that looks like a placeholder.

**Do not fabricate.** Every frame above is a real screen. Do not mock up a
screenshot in a design tool, do not composite features that do not exist, and do
not add a phone frame with invented content inside it.

---

## Developer confirm

- Final icon artwork and whether the current adaptive icon reads well at Play's
  small sizes.
- Feature graphic design. The brand palette (deep purple `#4A2C8E`, lavender
  `#EDE7FF`) is in `ui/theme/Color.kt`.
- Whether to submit a Hindi screenshot set.
- Whether to declare large-screen support, which would raise the tablet
  screenshot requirement to four.