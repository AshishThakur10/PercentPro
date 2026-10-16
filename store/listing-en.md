# PercentPro — Google Play store listing (English)

Copy for the Play Console **Main store listing** page. Limits verified against
Play Console Help ("Create and set up your app") and ("Add preview assets").

| Field | Limit | This draft | Status |
|---|---|---|---|
| App name | 30 | 29 | within |
| Short description | 80 | 73 | within |
| Full description | 4000 | 2,545 | within |
| Release notes | 500 | 492 | within |

Character counts below are verified by `tools/check_listing_length.py`.

---

## App name

```
PercentPro: Learn Percentages
```

29 characters. No emoji, no ranking or price words, no ALL CAPS, as Play's
metadata policy requires.

---

## Short description

```
Master percentages with clear lessons, MCQ practice and timed mock tests.
```

71 characters. One benefit sentence. Google states that extra keywords in this
field do not affect ranking, so it is written for the person reading it rather
than for a crawler.

---

## Full description

```
PercentPro makes percentages and percentage-based aptitude straightforward, in English and Hindi.

LEARN STEP BY STEP
Short lessons take you from what a percentage actually is through to profit and loss, discount and tax, salary and savings, population change, election percentages, marks and results, and reverse and mixed percentage problems. Each block explains one idea, works through a solved example, and finishes with a check so you can tell whether it landed.

PRACTISE BY TOPIC
Pick a topic and work through multiple choice questions written in exam style. Every question comes with a clear explanation, not just a right or wrong mark. Questions are tagged by difficulty, so you can start easy and move up to hard as you improve.

TIMED MOCK TESTS
Sit a 15-question, 15-minute percentage mock test. Mark questions for review, use a question palette to jump around, and submit whenever you are ready. You are scored out of the maximum with positive marking for correct answers and negative marking for incorrect ones, and you can review the whole paper afterwards.

FIND YOUR WEAK TOPICS
A score on its own does not tell you much. PercentPro breaks your answers down by topic, shows whether you are improving or slipping on recent questions, compares your last twenty answers with the twenty before, and points you at the topics that need the most work. It then suggests what to study or practise next.

TRACK YOUR PROGRESS
XP, daily goals, streaks and achievements give your practice a shape. Answer enough questions to hit your daily goal, keep your streak alive, and watch achievements unlock as the totals add up. Set the daily goal yourself, to 10, 20 or 30 questions.

ENGLISH AND HINDI
Switch the entire app between English and Hindi at any time from Settings. Lessons, questions, explanations and every screen are available in both languages, including right down to the button labels.

WORKS COMPLETELY OFFLINE
There is no account to create and nothing to sign in to. There are no ads, no tracking and no analytics in the app. Your progress, your answers and your rewards are stored in private app storage on this phone and are never transmitted anywhere, because the app has no permission to make a network request at all. It works with no connection, and if you uninstall the app that data goes with it.

A note on scope: every question in the app is written for this app to explain and drill percentage. Papers are labelled with the examination whose section they follow and use that examination's published marking scheme and timing, so you can see what the real thing looks like before you sit one. None of them is a reproduction of a past paper. PercentPro is not affiliated with, endorsed by, or a substitute for any exam board.
```

Every claim above maps to something implemented and covered by the automated
test suite. See `store/claim-evidence.md` for the per-claim mapping.

---

## Release notes

```
First release of PercentPro.

Lessons: percentage basics through profit and loss, discount, tax, salary, population and election problems.
Practice: topic-wise MCQs with explanations and difficulty levels.
Mock tests: 15 questions in 15 minutes, with review marks and negative marking.
Progress: XP, daily goals, streaks and achievements.
Insights: per-topic accuracy and weak topics.
English and Hindi throughout, switchable in Settings.
Works fully offline: no account, no ads, no tracking.
```

## Suggested category

**Education.** PercentPro is an educational app: lessons, practice questions,
examinable content and progress tracking. It has no game mechanics that would
place it in a casual-games category, and no document or file creation that would
suggest productivity.

Play also asks for a secondary category. Leave it unset unless you have a
preference — it is optional and a wrong second category is worse than none.

---

## Contact and support fields — developer to complete

Play Console requires these under **Store presence → Main store listing →
Store settings**. The app contains no URLs, so these are values you must supply.
Do not invent them.

| Field | Value |
|---|---|
| Email address | *developer to supply* |
| Phone | *developer to supply — required if the email is not a Gmail address* |
| Website | *optional; only if you host the privacy policy there* |
| Privacy policy URL | *required — see `store/privacy-data-safety.md`* |
| Marketing email | *optional* |
| Street address, city, state, postal code, country | *developer to supply; shown on the listing for some regions* |

These are publisher contact details and are publicly visible on the store
listing.