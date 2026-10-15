#!/usr/bin/env python3
"""
Lesson metadata strings for all 21 lessons plus the three course sections.

Lessons 1-3 keep their original Phase 2 strings. Lessons 4-12 ship with written
content; lessons 13-21 are catalogued with an honest "arriving in a later
phase" description so the path shows the full course without pretending the
content exists.

Idempotent: the block is delimited by markers and replaced on every run.
"""
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BEGIN = "    <!-- ===== BEGIN COURSE SECTIONS AND LESSON METADATA (Phase 6) ===== -->"
END = "    <!-- ===== END COURSE SECTIONS AND LESSON METADATA (Phase 6) ===== -->"

# (name, english, hindi)
STRINGS = [
    ("section_beginner_title", "Beginner", "शुरुआती"),
    ("section_beginner_description",
     "What a percentage means, how to convert it, and how to take a percentage of any number.",
     "प्रतिशत का अर्थ, उसे बदलना, और किसी भी संख्या का प्रतिशत निकालना।"),
    ("section_intermediate_title", "Intermediate", "मध्यम"),
    ("section_intermediate_description",
     "Changes, comparisons and the shortcuts that make two-step questions quick.",
     "परिवर्तन, तुलना, और वे शॉर्टकट जो दो-चरणीय प्रश्न तेज़ बना देते हैं।"),
    ("section_competitive_title", "Competitive exam applications", "प्रतियोगी परीक्षा अनुप्रयोग"),
    ("section_competitive_description",
     "The same ideas applied to marks, salary, population, prices and elections.",
     "वही विचार अंकों, वेतन, जनसंख्या, मूल्य और चुनाव पर लागू।"),

    ("lesson_4_title", "Decimals to Percentages", "दशमलव से प्रतिशत"),
    ("lesson_4_description",
     "Turn any decimal into a percentage by shifting the point two places right.",
     "बिंदु को दो स्थान दाएँ खिसकाकर किसी भी दशमलव को प्रतिशत में बदलें।"),
    ("lesson_5_title", "Finding a Percentage of a Number", "संख्या का प्रतिशत निकालना"),
    ("lesson_5_description",
     "Take a percentage of any quantity, including the 1% method.",
     "किसी भी मात्रा का प्रतिशत निकालें, 1% विधि सहित।"),
    ("lesson_6_title", "Finding the Original Whole", "मूल पूरा ज्ञात करना"),
    ("lesson_6_description",
     "Work backwards from a part to the number it came from.",
     "भाग से उसकी मूल संख्या तक पीछे जाएँ।"),
    ("lesson_7_title", "Percentage Increase and Decrease", "प्रतिशत वृद्धि और ह्रास"),
    ("lesson_7_description",
     "Apply a change to the right base, and see why falls and rises are not symmetric.",
     "परिवर्तन सही आधार पर लगाएँ, और जानें कि गिरावट व वृद्धि सममित क्यों नहीं।"),
    ("lesson_8_title", "Successive Percentage Changes", "लगातार प्रतिशत परिवर्तन"),
    ("lesson_8_description",
     "Two changes in a row, and the formula for the net effect.",
     "लगातार दो परिवर्तन और शुद्ध प्रभाव का सूत्र।"),
    ("lesson_9_title", "Percentage Comparison Problems", "प्रतिशत तुलना समस्याएँ"),
    ("lesson_9_description",
     "Convert \"what percent more\" questions into ratios and chain them.",
     "\"कितने प्रतिशत अधिक\" वाले प्रश्नों को अनुपात में बदलें और जोड़ें।"),
    ("lesson_10_title", "Competitive Exam Challenge", "प्रतियोगी परीक्षा चुनौती"),
    ("lesson_10_description",
     "A mixed set across every topic covered so far. Arriving in a later phase.",
     "अब तक के सभी विषयों का मिश्रित सेट। एक बाद के चरण में।"),

    ("lesson_11_title", "Percentage Points vs Percentage Change",
     "प्रतिशत अंक बनाम प्रतिशत परिवर्तन"),
    ("lesson_11_description",
     "Two different measurements that are easy to confuse, and where the difference matters.",
     "दो अलग माप जिन्हें आसानी से मिला दिया जाता है, और यह भेद कहाँ मायने रखता है।"),
    ("lesson_12_title", "Comparing Fractions, Decimals, and Percentages",
     "भिन्न, दशमलव और प्रतिशत की तुलना"),
    ("lesson_12_description",
     "Bring every form onto one scale before comparing.",
     "तुलना से पहले हर रूप को एक ही पैमाने पर लाएँ।"),
    ("lesson_13_title", "Exam Shortcuts and Mental Calculation",
     "परीक्षा शॉर्टकट और मानक गणना"),
    ("lesson_13_description",
     "Shortcuts that hold up under exam pressure, with the limits of each one stated.",
     "वे शॉर्टकट जो परीक्षा के दबाव में टिकते हैं, हर एक की सीमा सहित।"),

    ("lesson_14_title", "Population Increase and Decrease", "जनसंख्या वृद्धि और ह्रास"),
    ("lesson_14_description",
     "Growth, decline and change over two periods. Arriving in a later phase.",
     "वृद्धि, गिरावट और दो अवधियों में परिवर्तन। एक बाद के चरण में।"),
    ("lesson_15_title", "Salary, Expenditure, and Savings", "वेतन, व्यय और बचत"),
    ("lesson_15_description",
     "How income is split, and how one change affects the rest. Arriving in a later phase.",
     "आय का विभाजन, और एक परिवर्तन का बाकी पर प्रभाव। एक बाद के चरण में।"),
    ("lesson_16_title", "Marks, Pass Marks, and Exam Results",
     "अंक, उत्तीर्ण अंक और परीक्षा परिणाम"),
    ("lesson_16_description",
     "Pass percentages, how many needed to pass, and shortfalls. Arriving in a later phase.",
     "उत्तीर्ण प्रतिशत, उत्तीर्ण होने के लिए कितने अंक, और कमी। एक बाद के चरण में।"),
    ("lesson_17_title", "Election and Voting Percentage Problems",
     "चुनाव और मतदान प्रतिशत समस्याएँ"),
    ("lesson_17_description",
     "Votes polled, turnout, shares and margins. Arriving in a later phase.",
     "डाले गए मत, भागीदारी, हिस्सेदारी और अंतर। एक बाद के चरण में।"),
    ("lesson_18_title", "Price Changes, Discounts, and Tax", "मूल्य परिवर्तन, छूट और कर"),
    ("lesson_18_description",
     "Marked price, discount chains and tax added at the till. Arriving in a later phase.",
     "अंकित मूल्य, छूट की शृंखला और दुकान पर लगने वाला कर। एक बाद के चरण में।"),
    ("lesson_19_title", "Profit, Loss, and Percentage Relationships",
     "लाभ, हानि और प्रतिशत संबंध"),
    ("lesson_19_description",
     "Profit and loss percentages and what they are measured against. Arriving in a later phase.",
     "लाभ और हानि के प्रतिशत और वे किसके सापेक्ष मापे जाते हैं। एक बाद के चरण में।"),
    ("lesson_20_title", "Reverse Percentage Problems", "उलटे प्रतिशत प्रश्न"),
    ("lesson_20_description",
     "Given a changed value, recover the original. Arriving in a later phase.",
     "बदले हुए मान से मूल मान प्राप्त करें। एक बाद के चरण में।"),
    ("lesson_21_title", "Mixed Percentage Word Problems", "मिश्रित प्रतिशत शब्द समस्याएँ"),
    ("lesson_21_description",
     "Several percentages interacting in one problem. Arriving in a later phase.",
     "एक ही प्रश्न में कई प्रतिशत एक-दूसरे पर प्रभाव डालते हैं। एक बाद के चरण में।"),

    ("lesson_upcoming_label", "Not yet written", "अभी लिखा नहीं गया"),
    ("lesson_upcoming_body",
     "This lesson is on the path but its content has not been written yet, so it cannot be opened.",
     "यह पाठ पाठ्यक्रम में है पर इसकी सामग्री अभी नहीं लिखी गई, इसलिए इसे खोला नहीं जा सकता।"),
    ("lesson_resume_label", "Resume here", "यहाँ से जारी रखें"),
    ("lesson_resume_empty", "Start with lesson 1", "पाठ 1 से शुरू करें"),
    ("section_progress", "%1$d of %2$d done", "%2$d में से %1$d पूरे"),
]


def escape(text):
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "\\'")


def main():
    for locale, index in (("", 0), ("-hi", 1)):
        path = os.path.join(ROOT, f"app/src/main/res/values{locale}/strings.xml")
        text = open(path, encoding="utf-8").read()

        # Strip every existing definition of every name this script owns, then
        # re-add them inside the marker block. Removing by name rather than by
        # marker keeps the script idempotent even if an earlier hand-edit left a
        # copy outside the markers, which would otherwise be a duplicate
        # resource and fail the merge.
        text = re.sub(re.escape(BEGIN) + r".*?" + re.escape(END), "", text, flags=re.DOTALL)
        for name, _, _ in STRINGS:
            text = re.sub(
                r'\n?[ \t]*<string name="' + re.escape(name) + r'">.*?</string>',
                "", text, flags=re.DOTALL)

        lines = [BEGIN]
        for name, en, hi in STRINGS:
            lines.append(f'    <string name="{name}">{escape(en if index == 0 else hi)}</string>')
        lines.append(END)
        block = "\n".join(lines)

        if "</resources>" not in text:
            raise SystemExit(f"{path}: no </resources>")
        text = text.replace("</resources>", block + "\n</resources>")
        open(path, "w", encoding="utf-8").write(text)
        print(f"{path}: {len(STRINGS)} strings")

    # Verify the catalogue's resource references all exist.
    import xml.etree.ElementTree as ET
    defined = {}
    for locale in ("", "-hi"):
        path = os.path.join(ROOT, f"app/src/main/res/values{locale}/strings.xml")
        root = ET.parse(path).getroot()
        defined[locale] = {node.get("name") for node in root if node.tag == "string"}
    catalog = open(os.path.join(ROOT, "app/src/main/java/com/atlabs/percentpro/content/LessonCatalog.kt"), encoding="utf-8").read()
    missing = set(re.findall(r"R\.string\.(lesson_\d+_(?:title|description)|section_\w+|lesson_upcoming_\w+)", catalog))
    absent = {name for name in missing if name not in defined[""] or name not in defined["-hi"]}
    if absent:
        raise SystemExit(f"catalog references undefined strings: {sorted(absent)}")
    print(f"catalog references {len(missing)} strings, all present in both languages")


if __name__ == "__main__":
    main()
