# Store preparation (Phase 9)

Local artifacts prepared for the Google Play submission. **Nothing here has been
uploaded.** No Play Console setting was changed and no signing credential was
touched.

| File | Purpose |
|---|---|
| `listing-en.md` | English store listing copy: name, short description, full description, release notes, category, contact fields |
| `listing-hi.md` | Hindi store listing copy, same structure |
| `claim-evidence.md` | Every listing claim mapped to its implementation and tests; and what is deliberately **not** claimed |
| `privacy-data-safety.md` | Privacy audit, local data inventory, and the Play Console answers to prepare |
| `screenshots-plan.md` | Required and optional graphics specs, the six-screen capture plan, and how to capture real screenshots |
| `play-console-checklist.md` | Manual Play Console tasks, split into what is mandatory and what is a declaration only you can make |
| `closed-testing-checklist.md` | Phase 10: automated results, track selection, upload, tester setup, crash triage, and what still needs a real device |

## Verifying the copy

```bash
python3 tools/check_listing_length.py
```

Checks all four copy fields in both languages against Play's documented limits
(name 30, short description 80, full description 4000, release notes 500) and
exits non-zero if any field is over. Counts full-width and half-width characters
alike, which is how Play Console counts them, so the Hindi listing is measured
the same way as the English one.

## The build these assets describe

```bash
./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

See the Phase 9 report for the recorded test, build and signing results.