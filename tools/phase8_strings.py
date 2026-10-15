#!/usr/bin/env python3
"""
Phase 8 strings: first-use onboarding, the settings screen, the privacy
explanation and the confirm-before-quit mock-test dialog.

Emitted to both locales, idempotently: rerunning replaces only the marked
block, so hand edits to the rest of strings.xml are preserved.
"""
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BEGIN = "    <!-- ===== BEGIN PHASE 8 STRINGS ===== -->"
END = "    <!-- ===== END PHASE 8 STRINGS ===== -->"

STRINGS = [
    # --- onboarding ---
    # Page one's title reuses R.string.welcome_title, which the home screen
    # already shows, so the app does not carry two copies of the same line.
    ("onboarding_body_1",
     "Short, practical lessons that make percentages and aptitude feel easy.",
     "छोटे और व्यावहारिक पाठ जो प्रतिशत और एप्टीट्यूड को आसान बना देते हैं।"),
    ("onboarding_title_2", "Practise, don't just read",
     "सिर्फ़ पढ़िए नहीं, अभ्यास कीजिए"),
    ("onboarding_body_2",
     "Answer exam-style questions, run a timed mock test and see exactly where you stand.",
     "परीक्षा-शैली के प्रश्नों के उत्तर दीजिए, समयबद्ध मॉक टेस्ट दीजिए और जानिए आपकी वर्तमान स्थिति क्या है।"),
    ("onboarding_title_3", "Everything stays on this phone",
     "सब कुछ इसी फ़ोन पर रहता है"),
    ("onboarding_body_3",
     "Your progress, answers and rewards are stored only on this device. There is no account and no sign-up.",
     "आपकी प्रगति, उत्तर और पुरस्कार केवल इसी डिवाइस पर सहेजे जाते हैं। कोई अकाउंट और साइन-अप नहीं है।"),
    ("onboarding_next", "Next", "आगे"),
    ("onboarding_back", "Back", "पीछे"),
    ("onboarding_skip", "Skip", "छोड़ें"),
    ("onboarding_start", "Start learning", "पढ़ना शुरू करें"),
    ("onboarding_page_indicator", "Page %1$d of %2$d", "पृष्ठ %1$d / %2$d"),

    # --- settings ---
    ("settings_title", "Settings", "सेटिंग्स"),
    ("settings_section_learning", "Learning", "पढ़ाई"),
    ("settings_section_data", "Your data", "आपका डेटा"),
    ("settings_section_about", "About", "ऐप के बारे में"),
    ("settings_daily_goal", "Daily goal", "दैनिक लक्ष्य"),
    ("settings_daily_goal_description",
     "Questions to answer each day to keep your streak.",
     "अपनी स्ट्रीक बनाए रखने के लिए रोज़ाना उत्तर दिए जाने वाले प्रश्न।"),
    ("settings_achievements", "Achievements", "उपलब्धियाँ"),
    ("settings_achievements_value", "%1$d of %2$d unlocked",
     "%2$d में से %1$d अनलॉक"),
    ("settings_progress", "Course progress", "कोर्स प्रगति"),
    ("settings_progress_value", "%1$d of %2$d lessons", "%2$d में से %1$d पाठ"),
    ("settings_version", "Version", "वर्शन"),
    ("settings_privacy_title", "Your data stays on your phone",
     "आपका डेटा आपके फ़ोन पर ही रहता है"),
    ("settings_privacy_body",
     "PercentPro works entirely offline. It makes no network requests and sends nothing to a server, because there is no server.\n\nYour lesson progress, practice answers, mock test results, XP and achievements are stored in private app storage on this device only. Uninstalling the app deletes them, and they cannot be recovered by anyone.\n\nNo account is created, no advertising or analytics SDK is included, and no personal data is collected or shared.",
     "PercentPro पूरी तरह ऑफ़लाइन काम करता है। यह कोई नेटवर्क अनुरोध नहीं करता और किसी सर्वर पर कुछ नहीं भेजता, क्योंकि कोई सर्वर है ही नहीं।\n\nआपकी पाठ प्रगति, अभ्यास के उत्तर, मॉक टेस्ट परिणाम, XP और उपलब्धियाँ केवल इसी डिवाइस पर निजी ऐप स्टोरेज में सहेजी जाती हैं। ऐप हटाने पर ये मिट जाती हैं, और इन्हें कोई वापस नहीं पा सकता।\n\nकोई अकाउंट नहीं बनता, कोई विज्ञापन या एनालिटिक्स SDK शामिल नहीं है, और कोई व्यक्तिगत डेटा एकत्र या साझा नहीं किया जाता।"),
    ("settings_no_reset", "Resetting progress is not available",
     "प्रगति रीसेट करना उपलब्ध नहीं है"),
    ("settings_no_reset_body",
     "Your progress is kept deliberately permanent. Uninstalling the app is the only way to remove it.",
     "आपकी प्रगति जानबूझकर स्थायी रखी जाती है। इसे हटाने का एकमात्र तरीका ऐप को अनइंस्टॉल करना है।"),
    ("settings_built_by", "Made by %1$s", "%1$s द्वारा बनाया गया"),

    # --- mock test result / exit confirmation ---
    ("mock_result_out_of", "Out of %1$s", "%1$s में से"),
    ("cd_option_incorrect", "Incorrect option", "गलत विकल्प"),
    ("mock_exit_dialog_title", "Leave this mock test?",
     "यह मॉक टेस्ट छोड़ें?"),
    ("mock_exit_dialog_body",
     "The paper has %1$d of %2$d questions answered and is still running. Leaving now discards this attempt and your answers cannot be recovered.",
     "%2$d में से %1$d प्रश्नों के उत्तर दिए जा चुके हैं और टेस्ट अभी चल रहा है। अभी जाने पर यह प्रयास रद्द हो जाएगा और आपके उत्तर वापस नहीं मिलेंगे।"),
    ("mock_exit_dialog_confirm", "Leave test", "टेस्ट छोड़ें"),
    ("mock_exit_dialog_cancel", "Keep working", "जारी रखें"),
]


def escape(text):
    # Apostrophes matter here: Android string resources treat a bare ' as the
    # start of an escape, so "don't" must be written don\'t.
    return (text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("'", "\\'"))


def main():
    for locale, index in (("", 0), ("-hi", 1)):
        path = os.path.join(ROOT, f"app/src/main/res/values{locale}/strings.xml")
        text = open(path, encoding="utf-8").read()
        text = re.sub(re.escape(BEGIN) + r".*?" + re.escape(END), "", text, flags=re.DOTALL)
        for name, en, hi in STRINGS:
            text = re.sub(
                r'\n?[ \t]*<string name="' + re.escape(name) + r'"[^>]*>.*?</string>',
                "", text, flags=re.DOTALL)
        lines = [BEGIN]
        for name, en, hi in STRINGS:
            lines.append(
                f'    <string name="{name}" formatted="false">'
                f'{escape(en if index == 0 else hi)}</string>')
        lines.append(END)
        block = "\n".join(lines)
        if "</resources>" not in text:
            raise SystemExit(f"{path}: no </resources>")
        open(path, "w", encoding="utf-8").write(
            text.replace("</resources>", block + "\n</resources>"))
        print(f"{path}: {len(STRINGS)} strings")


if __name__ == "__main__":
    main()