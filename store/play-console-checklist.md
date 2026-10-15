# PercentPro — Play Console preparation checklist

Everything here is for you to do **in Play Console by hand**. Nothing in this
repository uploads anything, changes a Play setting, or touches the signing key.
Requirements are from Play Console Help pages cited inline.

Items marked **developer confirm** are decisions or declarations only you can
make. I have not made them on your behalf.

---

## A. Before you start — two things to confirm first

**1. Your developer account type determines your testing obligations.**
Google's own help pages do not state one universal rule, and the requirement
varies by account and by when the account was created. Check the eligibility
message Play Console shows for your specific app — it is authoritative for you in
a way that any general article is not.

- **New personal developer accounts** have historically been required to run a
  **closed test with at least 12 testers opted in for 14 continuous days** before
  production access is granted.
- Accounts that already have production access, and many organisation accounts,
  are not subject to that gate.
- Apps that were never published and are **permanently private / internal only**
  are exempt from the target-API availability rule as well.

**developer confirm:** read the eligibility text in your Play Console and work to
what it says, not to this checklist.

**2. Play App Signing.** For apps created after August 2021, Google Play manages
the signing key and the app is signed with a **play signing key**; your local key
becomes the **upload key** used only to authenticate uploads. If your app was
created earlier it may still be signed with your own key as the app signing key.

**developer confirm:** when you first upload, note whether Play offers you the
"Play App Signing" enrolment and what it says your current key's role becomes.
**Do not enrol or reset anything as part of this phase.** If you ever need to
reset an upload key, that is a deliberate, separate action with its own
recovery procedure — nothing here should trigger it.

The build in this repository is signed with the key that was already configured
locally. That key was not modified, renamed, regenerated or replaced.

---

## B. App content (Play Console → Policy and programs → App content)

- [ ] **Privacy policy** — host a public, non-geofenced, **non-PDF** page and
      enter its URL. Required even with no data collection. Content checklist:
      `store/privacy-data-safety.md` §5. **developer confirm:** the URL and the
      publisher entity named in the policy must match your listing.
- [ ] **Ads** — answer "Does your app contain ads?" **No.** There is no ad SDK
      in the build.
- [ ] **App access** — mark "all functionality available without special
      access". PercentPro has no login, no restricted area and no demo account.
      Only provide sign-in details if you later add one.
- [ ] **Content ratings** — complete the IARC questionnaire. Answer for this app
      as it actually is: no violence, no sexual content, no profanity, no
      gambling, no user-generated content, no messaging, no location sharing, no
      purchases. **developer confirm:** the questionnaire is a declaration you
      make; do not copy an answer from another app.
- [ ] **Target audience and content** — select the age band you have actually
      considered. The app is general-audience study material. **developer confirm:**
      your band choice determines which policies apply to you.
- [ ] **News app declaration** — not applicable; PercentPro is not a news app.
- [ ] **Permissions declaration** — not expected. The app requests no runtime
      permissions and its only manifest permission is its own signature-scoped
      marker. Only complete this form if Play asks you for it at review.
- [ ] **Government apps / financial features / health** declarations — confirm
      none apply. **developer confirm.**
- [ ] **Data safety form** — see `store/privacy-data-safety.md` §7. Required for
      every app except one that is exclusively on internal testing.
- [ ] **Account deletion** — not applicable; no account exists. Do not provide a
      deletion URL you do not operate.

---

## C. Store presence (Grow users → Store presence → Main store listing)

- [ ] **App details** — name, short description, full description.
      English copy: `store/listing-en.md`. Hindi copy: `store/listing-hi.md`.
      Re-run `python3 tools/check_listing_length.py` after any edit.
- [ ] **App name**: keep `PercentPro` unless you use the localised name in
      `store/listing-hi.md`. In-app display name stays `PercentPro` in both
      languages, unchanged.
- [ ] **Category**: Education. Secondary category optional — leave unset if you
      have no preference.
- [ ] **Contact details** — email, phone, website, marketing email, address.
      All required values are yours; the app contains no URLs.
- [ ] **Privacy policy URL** — repeat of B; the same URL.
- [ ] **App icon (512 × 512)** — already generated at
      `store/graphics/ic_launcher_play_512.png`. Upload this file; confirm it
      stays square-cornered after Play's own masking.
- [ ] **Feature graphic (1024 × 500)** and **screenshots** — still to produce.
      Specs and plan: `store/screenshots-plan.md`.
- [ ] **Translations** — add Hindi as a second language. Localise the name,
      short description, full description and release notes with the supplied
      copy, and supply per-language screenshots.

---

## D. Release and testing

- [ ] **Create the app** if it does not exist yet. The package name must be
      `com.atlabs.percentpro`. **This name must never be reused or changed** —
      it is permanent once the app exists in Play.
- [ ] **Internal testing** — upload the release bundle, share the opt-in link,
      install and exercise it on at least one real device and one emulator.
- [ ] **Verify the uploaded artifact.** After upload, Play Console shows the
      parsed manifest. Confirm target API 36, version code 1, version name, and
      the absence of debug flags. Play rejects debuggable uploads; ours is not.
- [ ] **Closed testing** — only if your account's eligibility message requires it.
      If required: create the track, upload a signed release, share the opt-in
      link, and get **12 testers opted in for 14 continuous days**.
      - Only testers who open the opt-in link and install from Play count. The
        opted-in number in Play Console is the one to watch, not your list.
      - Recruit more than 12; an uninstall can break continuity.
      - **developer confirm:** whether this applies to your account.
- [ ] **Pre-launch report** — run it and read the warnings. It catches issues
      such as device-permission problems and accessibility issues.
- [ ] **Release notes** — `store/listing-en.md` and `store/listing-hi.md`.
- [ ] **Release to production** — request access if required, then choose a
      **staged rollout** (for example 10%) for the first public release so a
      crash is not immediately everyone's problem.

---

## E. Final submission review

- [ ] Store listing text matches what the app actually does. Every feature
      claimed is implemented; nothing about past exam papers, official
      affiliation, or guaranteed results is claimed.
- [ ] Screenshots are real captures of the shipped build.
- [ ] Privacy policy URL loads in a logged-out browser.
- [ ] Data safety answers match the privacy policy wording.
- [ ] Content rating completed.
- [ ] Signed release built from a clean checkout with no secrets in source.
- [ ] No debug or internal-only wording left in the listing.

---

## F. Explicitly out of scope for this phase

Do **not** do any of the following until you have decided to:

- upload a bundle to any track;
- publish or roll out to production;
- change Play Console settings such as visibility or country availability;
- reset, replace, regenerate or re-enrol any signing key;
- enrol in or opt out of Play App Signing.

This repository produced a signed release bundle locally and stopped there.
Approve the next step explicitly before anything is uploaded.