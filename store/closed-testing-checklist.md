# PercentPro — closed testing setup and QA checklist

Two separate things live in this document and they must not be confused:

- **Section A — automated.** Already run on this machine. Reproducible, evidence
  recorded in the Phase 10 report.
- **Sections B and C — manual.** Done by you, in Play Console and on real
  devices. Nothing below has been done by me, and nothing has been uploaded.

**Nothing in this repository has been uploaded to Google Play.** No Play Console
setting has been changed, no tester has been added, and no signing key has been
touched.

---

## A. Automated checks — done

Re-run any time with:

```bash
./gradlew test                 # 603 JVM unit tests
./gradlew connectedDebugAndroidTest   # 11 on-device Compose journey tests
./gradlew lintDebug            # static analysis
./gradlew assembleDebug        # sideloadable debug APK
./gradlew bundleRelease        # the upload artifact
```

`connectedDebugAndroidTest` needs a running emulator or a connected device; it
was run against a **Medium Phone API 36 emulator (Android 16, 1080x2400,
420 dpi)**.

---

## B. Before you upload — manual checks only you can do

### B1. Decide the track first

Play Console → **Testing → Internal testing** or **Closed testing**. They are not
interchangeable once you start:

- **Internal testing** — up to 100 testers, available in minutes, and the way to
  smoke-test your own build. No review, no production-access consequence.
- **Closed testing** — for external testers. Adds the opt-in link, tester groups
  and the release tracks.

Start with internal testing. Move to closed only when you want people outside
your account installing it.

**Your testing obligations depend on your account and cannot be determined from
outside it.** Whether you must run a closed test before production access is
stated in Play Console for your specific app. Read that eligibility message; it
is authoritative in a way that any general article is not. Do not assume you are
subject to a 12-tester/14-day rule — or exempt from one — without checking.

### B2. Upload the bundle

1. Play Console → your app → **Testing → Internal testing** → **Create new
   release**.
2. Upload `app/build/outputs/bundle/release/app-release.aab`
   (3,754,553 bytes as built on 2026-10-15).
3. Release name and release notes are free text. Draft release notes are in
   `store/listing-en.md` and `store/listing-hi.md`.
4. After upload, check the parsed manifest Play shows you. It should read:
   package `com.atlabs.percentpro`, version code `1`, version name `1.0`,
   min SDK 23, **target SDK 36**, and no debuggable flag. Stop if anything
   differs.

### B3. Signing

The local build is signed with the key already configured in
`keystore.properties`. When you first upload, note whether Play offers **Play App
Signing** enrolment and what role your key takes as a result.

Do not reset, replace or re-enrol any key as part of this phase. If you ever need
an upload-key reset, that is a separate, deliberate action with its own recovery
procedure.

### B4. Testers and the opt-in link

- Internal: add tester email addresses or pick a Google Group directly in the
  console.
- Closed: create the track, then copy the **opt-in link** Play generates and
  distribute it. Only people who open that link and install from Play count as
  testers. The opted-in figure shown in the console is the number that matters,
  not your mailing list.

### B5. Verify the install actually works through Play

Sideloading the debug APK is not the same as installing through Play. Confirm on
a clean device, with no prior install:

- [ ] Installs from the Play link without error.
- [ ] First launch shows the onboarding pages.
- [ ] Skip **and** "Start learning" both reach Home.
- [ ] Kill and relaunch — onboarding does not reappear.
- [ ] Switch to Hindi; kill and relaunch — still Hindi.
- [ ] All four tabs open (Home, Learn, Practice, Settings).
- [ ] Complete a lesson; progress survives a restart.
- [ ] Answer a practice question; the explanation appears.
- [ ] Run a mock test to submission; the result appears in history.
- [ ] Settings shows the real installed version.

---

## C. After testers have it — monitoring and triage

- [ ] **Pre-launch report** (Play Console → Releases → Pre-launch report) — read
      the warnings rather than dismissing them.
- [ ] **Android vitals** — check crash rate and ANRs once testers have been
      using it.
- [ ] **Play Console feedback / reviews** — collect tester comments.
- [ ] **Crash triage loop:** reproduce locally, fix, bump `versionCode`, rebuild,
      re-upload to the same track. Never re-upload the same version code to Play;
      it is rejected.
- [ ] **Bug tracking:** keep a list of tester reports mapped to fixed commits.
- [ ] Before promoting to production, re-run Section B5 against the exact build
      being promoted.

---

## D. Declarations to verify against the actual app

Full reasoning is in `store/privacy-data-safety.md`. Confirm each answer yourself
before submitting — these are declarations, not something this repository can
assert on your behalf.

- [ ] **Privacy policy** — you must host a public, non-PDF page and enter the URL.
      Nothing has been hosted or invented for you. The app already shows a
      factual privacy explanation under Settings → Your data.
- [ ] **Data safety** — from the audit: no data collected, no data shared, no
      `INTERNET` permission exists in the build at all.
- [ ] **Ads** — "No". No ad SDK is present.
- [ ] **App access** — mark all functionality available without special access.
      There is no login or restricted area.
- [ ] **Content rating** — your declaration. Nothing in the app is violent,
      sexual, gambling or user-generated.
- [ ] **Target audience and content** — your declaration; it determines which
      policies apply to you.
- [ ] **Store listing** — copy in `store/listing-en.md` / `listing-hi.md`. Every
      claim maps to implemented, tested behaviour in `store/claim-evidence.md`.
- [ ] **App icon** — `store/graphics/ic_launcher_play_512.png` is ready to upload.
- [ ] **Feature graphic (1024x500) and screenshots** — still to be produced.
      Capture steps: `store/screenshots-plan.md`. Several real screens were
      captured during Phase 10 QA and can be reused.

---

## E. Not verified here — do this before promoting

Everything below needs a real human on real hardware. No emulator stood in for
it and it is not covered by any test.

- [ ] Physical handset, not an emulator.
- [ ] Small screen (around 5"), where 48dp targets and long Hindi labels are
      tightest.
- [ ] Tablet or large-screen layout (the app caps content at 640 dp but has not
      been seen on one).
- [ ] System font scale at 200%.
- [ ] TalkBack through the whole app.
- [ ] Back gesture during a running mock test — confirm the discard
      confirmation appears.
- [ ] Rotate the device during a lesson and during a live mock test.
- [ ] Airplane mode, to confirm nothing depends on a connection.
- [ ] Low-memory / "don't keep activities" developer option.

---

## F. Local verification record

Rebuild these to reproduce; sizes will differ slightly with your keystore.

| Artifact | Path |
|---|---|
| Unit tests | `./gradlew test` — 603 passing, 0 failing |
| On-device tests | `./gradlew connectedDebugAndroidTest` — 11 passing |
| Lint | `./gradlew lintDebug` — 0 errors, 21 warnings |
| Debug APK | `app/build/outputs/apk/debug/app-debug.apk` |
| Release APK | `app/build/outputs/apk/release/app-release.apk` |
| Release bundle | `app/build/outputs/bundle/release/app-release.aab` |

Do not commit the `.apk` or `.aab` files; both are already covered by
`.gitignore`.