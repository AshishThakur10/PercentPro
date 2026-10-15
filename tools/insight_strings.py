#!/usr/bin/env python3
"""
Phase 7 strings: insights dashboard, topic statuses, recommendations,
targeted practice and trend labels. Emitted to both locales, idempotently.
"""
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BEGIN = "    <!-- ===== BEGIN PHASE 7 INSIGHT STRINGS ===== -->"
END = "    <!-- ===== END PHASE 7 INSIGHT STRINGS ===== -->"

STRINGS = [
    # --- screen ---
    ("insights_title", "Learning insights", "सीखने की जानकारी"),
    ("insights_subtitle",
     "Where you are strong and what to work on next.",
     "आप कहाँ मजबूत हैं और आगे क्या करना है।"),
    ("insights_overall_attempted", "Questions answered", "उत्तर दिए गए प्रश्न"),
    ("insights_overall_accuracy", "Overall accuracy", "कुल शुद्धता"),
    ("insights_lessons_completed", "Lessons completed", "पूरे किए गए पाठ"),
    ("insights_strongest_topic", "Strongest topic", "सबसे मजबूत विषय"),
    ("insights_weakest_topic", "Needs the most work", "सबसे अधिक मेहनत चाहिए"),
    ("insights_suggested_next", "Suggested next step", "सुझाया गया अगला कदम"),
    ("insights_by_topic", "Topic by topic", "विषयवार विवरण"),
    ("insights_recent", "Recent performance", "हाल का प्रदर्शन"),
    ("insights_no_data_title", "No answers recorded yet",
     "अभी तक कोई उत्तर दर्ज नहीं हुआ"),
    ("insights_no_data_body",
     "Answer questions in practice or a mock test and your strengths and weak spots will appear here.",
     "प्रैक्टिस या मॉक टेस्ट में प्रश्नों के उत्तर दें, और आपकी मजबूती व कमजोरियाँ यहाँ दिखने लगेंगी।"),
    ("insights_legacy_note",
     "Practice sessions answered before this feature started are not included, because only session totals were saved.",
     "इस सुविधा से पहले दिए गए प्रैक्टिस उत्तर शामिल नहीं हैं, क्योंकि केवल सत्र का कुल सहेजा गया था।"),
    ("insights_back", "Back", "वापस"),

    # --- topic statuses ---
    ("insights_status_not_enough_data", "Not enough data", "पर्याप्त आँकड़े नहीं"),
    ("insights_status_needs_practice", "Needs practice", "अभ्यास चाहिए"),
    ("insights_status_improving", "Improving", "सुधार हो रहा है"),
    ("insights_status_strong", "Strong", "मजबूत"),
    ("insights_status_not_enough_data_body",
     "Answer at least %1$d questions here to see a verdict.",
     "यहाँ कम से कम %1$d प्रश्नों के उत्तर दें तो नतीजा दिखेगा।"),
    ("insights_easy_only_caution",
     "Mostly easy questions so far, so this may not reflect harder topics.",
     "अब तक ज़्यादातर आसान प्रश्न, इसलिए यह कठिन विषयों का सही संकेत नहीं दे सकता।"),

    # --- trends ---
    ("insights_trend_up", "Up %1$d points", "%1$d अंक ऊपर"),
    ("insights_trend_down", "Down %1$d points", "%1$d अंक नीचे"),
    ("insights_trend_steady", "Holding steady", "स्थिर बना हुआ"),
    ("insights_trend_locked",
     "Keep practising to unlock your trend",
     "अपना रुझान देखने के लिए अभ्यास जारी रखें"),
    ("insights_recent_window", "Last %1$d answers", "पिछले %1$d उत्तर"),
    ("insights_recent_accuracy", "Recent accuracy", "हाल की शुद्धता"),
    ("insights_previous_accuracy", "Previous %1$d answers", "पिछले %1$d उत्तर"),
    ("insights_vs_previous", "Compared with the %1$d answers before",
     "पहले के %1$d उत्तरों की तुलना में"),

    # --- recommendation ---
    ("rec_title_resume_lesson", "Finish your current lesson", "अपना चालू पाठ पूरा करें"),
    ("rec_title_continue_course", "Continue the course", "पाठ्यक्रम जारी रखें"),
    ("rec_title_practice_weak", "Practise a weak topic", "कमजोर विषय का अभ्यास करें"),
    ("rec_title_step_up", "Try harder questions", "कठिन प्रश्न आज़माएँ"),
    ("rec_title_mixed_review", "Do a mixed review", "मिश्रित समीक्षा करें"),
    ("rec_title_start_lesson_one", "Start with the first lesson", "पहले पाठ से शुरू करें"),
    ("rec_title_keep_practising", "Keep consolidating", "मजबूती बनाए रखें"),
    ("rec_continue_learning", "Continue learning", "सीखना जारी रखें"),
    ("rec_practice_weak_topics", "Practise weak topics", "कमजोर विषयों का अभ्यास"),
    ("rec_view_insights", "View insights", "जानकारी देखें"),
    ("rec_start_practice", "Start practice", "अभ्यास शुरू करें"),
    ("rec_open_insights", "Open insights", "जानकारी खोलें"),

    ("rec_reason_lesson_in_progress", "You have not finished this lesson yet.",
     "आपने यह पाठ अभी पूरा नहीं किया है।"),
    ("rec_reason_next_lesson", "This is the next lesson on your path.",
     "आपके पाठ्यक्रम में यह अगला पाठ है।"),
    ("rec_reason_low_accuracy", "Your accuracy here is %1$d%%.",
     "यहाँ आपकी शुद्धता %1$d%% है।"),
    ("rec_reason_strong_on_easy", "Easy questions here are %1$d%% correct.",
     "यहाँ आसान प्रश्नों में %1$d%% सही हैं।"),
    ("rec_reason_balanced", "You have practised %1$d topics at a similar level.",
     "आपने %1$d विषय समान स्तर पर अभ्यास किए हैं।"),
    ("rec_reason_no_history", "Answer a few questions to get a tailored suggestion.",
     "व्यक्तिगत सुझाव के लिए कुछ प्रश्नों के उत्तर दें।"),
    ("rec_reason_course_complete", "You have finished every available lesson.",
     "आपने सभी उपलब्ध पाठ पूरे कर लिए हैं।"),

    # --- targeted practice ---
    ("targeted_title_mixed", "Mixed review", "मिश्रित समीक्षा"),
    ("targeted_empty_title", "No questions available", "कोई प्रश्न उपलब्ध नहीं"),
    ("targeted_empty_difficulty_body",
     "This topic has no questions at that level yet. Start with any difficulty.",
     "इस विषय में अभी उस स्तर के प्रश्न नहीं हैं। कोई भी स्तर चुनकर शुरू करें।"),
    ("targeted_empty_body", "There is nothing to practise for this selection yet.",
     "इस चयन के लिए अभी अभ्यास करने को कुछ नहीं है।"),
    ("targeted_mixed_subtitle", "Questions across %1$d topics", "%1$d विषयों के प्रश्न"),
    ("targeted_topic_subtitle", "%1$d questions", "%1$d प्रश्न"),
    ("targeted_start", "Start", "शुरू करें"),
    ("targeted_questions_count", "%1$d questions selected", "%1$d प्रश्न चुने गए"),

    ("insights_topic_counts", "%1$d answered, %2$d correct", "%1$d उत्तर, %2$d सही"),
    ("insights_accuracy_value", "%1$d%%", "%1$d%%"),
    ("insights_recent_accuracy_value", "%1$d%% accuracy", "%1$d%% शुद्धता"),
    ("insights_lessons_completed_value", "%1$d lessons completed", "%1$d पाठ पूरे"),
    ("insights_topics_covered", "%1$d topics practised", "%1$d विषयों का अभ्यास"),
    # --- accessibility ---
    ("cd_insights_accuracy", "Accuracy %1$d percent from %2$d answers",
     "%2$d उत्तरों से शुद्धता %1$d प्रतिशत"),
    ("cd_topic_performance",
     "%1$s: %2$d answers, %3$d correct, %4$d percent accuracy, %5$s",
     "%1$s: %2$d उत्तर, %3$d सही, %4$d प्रतिशत शुद्धता, %5$s"),
    ("cd_recommendation", "Suggestion: %1$s. %2$s", "सुझाव: %1$s। %2$s"),
    ("cd_status_indicator", "Status: %1$s", "स्थिति: %1$s"),
]


def escape(text):
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "\\'")


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
