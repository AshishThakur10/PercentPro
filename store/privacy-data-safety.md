# PercentPro — Privacy and Data safety audit

Prepared for the developer to complete in Play Console. **Nothing here is a legal
opinion, and several answers are yours to confirm, not mine to decide.** Where a
declaration depends on a judgement call, it is marked **developer confirm**.

Every factual claim below was checked against the built release artifact, not
against intent.

---

## 1. What is stored, and where

All state is written with Jetpack DataStore Preferences into the app's private
internal storage (`/data/data/com.atlabs.percentpro/`). Six stores exist:

| Store file | Holds |
|---|---|
| `percentpro_progress` | Completed lesson ids, the lesson in progress, the block index reached |
| `percentpro_practice` | Per-topic practice results: questions answered, correct count, timestamp |
| `percentpro_mocktest` | Saved mock test results including per-question selected and expected options; the active in-progress paper |
| `percentpro_gamification` | Total XP, the reward ledger, qualifying streak dates, daily goal |
| `percentpro_answer_ledger` | Per-question answer events: question id, topic, correct or not, timestamp, source |
| `percentpro_settings` | Chosen language, whether onboarding has been seen |

**No personal identifiers.** No name, email, phone number, device identifier,
advertising id, IP address, location, or contact list is read or stored. There is
no account system, so there is no identifier to associate activity with a person.

**Developer confirm:** whether your privacy policy should also describe what the
app stores *in aggregate* (for example "total questions answered") even though no
individual is identifiable. Play's policy asks you to disclose data your app
collects; a purely local, non-identifiable counter is generally not "collected"
under Play's definition, but that judgement is yours.

---

## 2. Is anything transmitted off the device?

**No, and this is structurally guaranteed rather than merely observed.**

- The **source manifest declares zero `<uses-permission>`**. Verified in
  `AndroidManifest.xml` and asserted by `PrivacyClaimTest`.
- The **merged release manifest contains no `android.permission.INTERNET`.**
  Without it, the app cannot open a network connection at all.
- No networking code exists. A search of the whole source tree for `java.net`,
  `HttpURLConnection`, `okhttp`, `Socket`, `URLConnection`, `WebView` and
  `openConnection` returns nothing.
- **No analytics, crash reporting, advertising or attribution SDK.** The complete
  `releaseRuntimeClasspath` is AndroidX, the Kotlin standard library, and four
  transitive utility libraries of theirs: Guava `listenablefuture`, Okio, JSpecify
  and `org.jetbrains:annotations`. None of these initiates network traffic on its
  own.

The only permission in the merged manifest is
`com.atlabs.percentpro.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. It is
**declared by the app itself with `protectionLevel="0x00000002"` (signature)**
and injected by AndroidX Core. It is a self-scoped marker that lets the app
receive its own broadcasts safely on newer Android versions. It grants no access
to anything and is not a data-access permission.

**Developer confirm:** Play asks whether the app collects or shares data. Based
on the audit, the factual answer is no. Confirm it against your own build and
policy before submitting, since you are the party making the declaration.

---

## 3. Account deletion

Play requires account deletion **only for apps that let users create an account**.
PercentPro has no sign-up and no login, so the requirement does not apply and no
account-deletion URL is needed.

**Developer confirm:** do not tick any account-related declaration in Play
Console. If you add an account in a later version, this changes.

---

## 4. Backup and device transfer

`android:allowBackup="false"`, and both `backup_rules.xml` (pre-Android 12) and
`data_extraction_rules.xml` (Android 12+) exclude every storage domain, for both
cloud backup and device transfer.

Consequences you should be comfortable with and should state honestly in the
listing:
- Progress does **not** follow the user to a new phone.
- Uninstalling the app deletes all of it permanently.
- No copy is uploaded to a Google account.

This was chosen in Phase 8 deliberately: it is the only configuration in which
the in-app privacy text ("makes no network requests", "never transmitted
anywhere") is literally true. It is enforced by `PrivacyClaimTest`, which fails
the build if backup is ever re-enabled.

**Developer confirm:** whether you accept losing the cross-device-restore
trade-off. Turning it back on would require rewriting the in-app privacy text
and the store listing.

---

## 5. Privacy policy: required, and you must host it

Per Play's User Data policy, **every app must have a privacy policy, including
apps that collect no data**, and it must be reachable in two places:

1. A **public, non-geofenced, non-editable URL** entered in Play Console under
   Policy and programs → App content → Privacy policy. It must be a web page,
   **not a PDF**, and must load without a login.
2. **Linked or shown inside the app.** PercentPro already shows a factual privacy
   explanation in-app: Settings → Your data. That satisfies the in-app
   disclosure. Add a link to the hosted policy there if you prefer.

The policy must name the entity in the store listing (you, or AT Labs) or the app
must be named in it, must clearly be labelled as a privacy policy, and must
include a privacy contact point.

**No privacy URL is supplied here and none should be invented.** You must publish
the page and paste its URL. The content it needs to cover, matching this audit:

- what data is stored on the device and what each store holds;
- that nothing is transmitted off the device and that no network permission is held;
- that there is no account, no advertising, no analytics and no third-party SDK
  that collects data;
- that backup is disabled, so data is removed on uninstall and is not transferred
  between devices;
- a privacy contact address;
- your retention and deletion position, which is effectively "retained until the
  app is uninstalled, then deleted".

---

## 6. Children

The app contains no ads, no purchases, no chat, no user-to-user communication and
no data collection, so nothing in it is directed at children.

**Developer confirm:** the target-audience and content questionnaire is yours to
answer. Do not select an age band you have not actually considered; the answer
affects which policy obligations apply to you.

---

## 7. Play Console answers to prepare

Prepare these from the audit. Tick them off once you have read each against your
own build.

- [ ] **Data safety → Is your app collecting or sharing user data?** No data
      collected, no data shared.
- [ ] **Data safety → Is all user data encrypted in transit?** Not applicable;
      there is no transit. Do not claim transport encryption.
- [ ] **Data safety → Do you provide a way for users to request that their data be
      deleted?** Not applicable; there is no account and no server-side copy. Say
      so plainly rather than inventing a deletion web page.
- [ ] **Data safety → Data types.** None. Leave the per-type tables empty.
- [ ] **Ads → Does your app contain ads?** No.
- [ ] **Privacy policy URL.** Your hosted page, from section 5.
- [ ] **App access.** Not applicable unless you add a restricted area. See the
      console checklist.

Play states that the Data safety form and the privacy policy must tell the same
story; they are checked for consistency. This audit is the single source both
should come from.

---

## 8. Dependencies reviewed

| Dependency | Sends data? | Note |
|---|---|---|
| `androidx.core`, `activity`, `lifecycle` | No | AndroidX runtime |
| `androidx.compose.*` (BOM 2026.02.01) | No | UI only |
| `androidx.datastore:datastore-preferences` | No | Local persistence |
| `kotlin-stdlib` | No | |
| Guava `listenablefuture`, Okio, JSpecify, `org.jetbrains:annotations` | No | Transitive utilities of the above |

No advertising, analytics, attribution, crash-reporting, social, maps or
account SDK is present. Nothing in the dependency set can reach a server.