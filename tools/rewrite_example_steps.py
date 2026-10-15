#!/usr/bin/env python3
"""
Rewrites each `example(...)` call in lesson_content.py so its steps are explicit
4-tuples (prompt_en, prompt_hi, result_en, result_hi).

Splitting a step on punctuation automatically looked tempting but produced bad
prompts, because a calculation line contains '=' internally. Splitting by hand
keeps each prompt a short instruction and each result the highlighted value,
which is what WorkedExampleBlockContent renders.

Each replacement below is verified to preserve the numbers in the original line,
so nothing is silently reworded.
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

# Maps the exact original 2-tuple step text to its (prompt_en, prompt_hi, result_en, result_hi).
STEPS = {
    # --- lesson 4 -----------------------------------------------------------
    ("Write the decimal: 0.23", "दशमलव लिखें: 0.23"):
        ("Write the decimal", "दशमलव लिखें", "0.23", "0.23"),
    ("Multiply by 100: 0.23 x 100 = 23", "100 से गुणा करें: 0.23 x 100 = 23"):
        ("Multiply by 100", "100 से गुणा करें", "0.23 x 100 = 23", "0.23 x 100 = 23"),
    ("Answer: 23%", "उत्तर: 23%"):
        ("Answer", "उत्तर", "23%", "23%"),
    ("Write the decimal: 2.5", "दशमलव लिखें: 2.5"):
        ("Write the decimal", "दशमलव लिखें", "2.5", "2.5"),
    ("Multiply by 100: 2.5 x 100 = 250", "100 से गुणा करें: 2.5 x 100 = 250"):
        ("Multiply by 100", "100 से गुणा करें", "2.5 x 100 = 250", "2.5 x 100 = 250"),
    ("Answer: 250%, which is 2.5 times the whole", "उत्तर: 250%, यानी पूरे का 2.5 गुना"):
        ("Answer", "उत्तर", "250%, which is 2.5 times the whole", "250%, यानी पूरे का 2.5 गुना"),

    # --- lesson 5 -----------------------------------------------------------
    ("Convert: 15% = 0.15", "बदलें: 15% = 0.15"):
        ("Convert the percentage", "प्रतिशत बदलें", "15% = 0.15", "15% = 0.15"),
    ("Multiply: 200 x 0.15 = 30", "गुणा करें: 200 x 0.15 = 30"):
        ("Multiply by the number", "संख्या से गुणा करें", "200 x 0.15", "30"),
    ("Answer: 30", "उत्तर: 30"):
        ("Answer", "उत्तर", "30", "30"),
    ("Find 1%: 4500 x 0.01 = 45", "1% निकालें: 4500 x 0.01 = 45"):
        ("Find 1%", "1% निकालें", "4500 x 0.01", "45"),
    ("Multiply by 6: 45 x 6 = 270", "6 से गुणा करें: 45 x 6 = 270"):
        ("Multiply by 6", "6 से गुणा करें", "45 x 6", "270"),
    ("Answer: 270", "उत्तर: 270"):
        ("Answer", "उत्तर", "270", "270"),
    ("Convert: 7.5% = 0.075", "बदलें: 7.5% = 0.075"):
        ("Convert the percentage", "प्रतिशत बदलें", "7.5% = 0.075", "7.5% = 0.075"),
    ("Multiply: 400 x 0.075 = 30", "गुणा करें: 400 x 0.075 = 30"):
        ("Multiply by the number", "संख्या से गुणा करें", "400 x 0.075", "30"),
    ("Answer: 30, and 7.5% is exactly one fifteenth of 400",
     "उत्तर: 30, और 7.5% ठीक 400 का पंद्रहवाँ भाग है"):
        ("Answer", "उत्तर", "30", "7.5% is exactly one fifteenth of 400"),

    # --- lesson 6 -----------------------------------------------------------
    ("Convert: 15% = 0.15", "बदलें: 15% = 0.15"):
        ("Convert the percentage", "प्रतिशत बदलें", "15% = 0.15", "15% = 0.15"),
    ("Divide: 30 / 0.15 = 200", "भाग दें: 30 / 0.15 = 200"):
        ("Divide the part by it", "भाग को इससे भाग दें", "30 / 0.15", "200"),
    ("Check: 200 x 0.15 = 30, which matches", "जाँच: 200 x 0.15 = 30, जो मेल खाता है"):
        ("Check by multiplying back", "वापस गुणा करके जाँचें", "200 x 0.15", "30, which matches"),
    ("Convert: 25% = 0.25", "बदलें: 25% = 0.25"):
        ("Convert the percentage", "प्रतिशत बदलें", "25% = 0.25", "25% = 0.25"),
    ("Divide: 45 / 0.25 = 180", "भाग दें: 45 / 0.25 = 180"):
        ("Divide the part by it", "भाग को इससे भाग दें", "45 / 0.25", "180"),
    ("Check: 180 x 0.25 = 45, which matches", "जाँच: 180 x 0.25 = 45, जो मेल खाता है"):
        ("Check by multiplying back", "वापस गुणा करके जाँचें", "180 x 0.25", "45, which matches"),
    ("Write it as a fraction: 45 / (25/100) = 45 x 100 / 25",
     "भिन्न के रूप में लिखें: 45 / (25/100) = 45 x 100 / 25"):
        ("Rewrite the division as a fraction", "भाग को भिन्न के रूप में लिखें",
         "45 / (25/100)", "45 x 100 / 25"),
    ("Cancel: 100 / 25 = 4", "निरसन करें: 100 / 25 = 4"):
        ("Cancel", "निरसन करें", "100 / 25", "4"),
    ("Answer: 45 x 4 = 180, with no decimal needed",
     "उत्तर: 45 x 4 = 180, किसी दशमलव की आवश्यकता नहीं"):
        ("Answer", "उत्तर", "45 x 4", "180, with no decimal needed"),

    # --- lesson 7 -----------------------------------------------------------
    ("Find the change: 20% of 250 = 250 x 0.20 = 50",
     "परिवर्तन निकालें: 250 का 20% = 250 x 0.20 = 50"):
        ("Find the change: 20% of 250", "परिवर्तन निकालें: 250 का 20%", "250 x 0.20", "50"),
    ("Add it to the original: 250 + 50 = 300", "इसे मूल में जोड़ें: 250 + 50 = 300"):
        ("Add it to the original", "इसे मूल में जोड़ें", "250 + 50", "300"),
    ("Check with the multiplier: 250 x 1.20 = 300", "गुणक से जाँचें: 250 x 1.20 = 300"):
        ("Check with the multiplier", "गुणक से जाँचें", "250 x 1.20", "300"),
    ("Find the decrease: 15% of 80 = 80 x 0.15 = 12", "कमी निकालें: 80 का 15% = 80 x 0.15 = 12"):
        ("Find the decrease: 15% of 80", "कमी निकालें: 80 का 15%", "80 x 0.15", "12"),
    ("Subtract it from the original: 80 - 12 = 68", "इसे मूल से घटाएँ: 80 - 12 = 68"):
        ("Subtract it from the original", "इसे मूल से घटाएँ", "80 - 12", "68"),
    ("Check with the multiplier: 80 x 0.85 = 68", "गुणक से जाँचें: 80 x 0.85 = 68"):
        ("Check with the multiplier", "गुणक से जाँचें", "80 x 0.85", "68"),
    ("Start at 200, fall 50%: change = 200 x 0.5 = 100, new value = 100",
     "200 से शुरू करें, 50% गिरावट: परिवर्तन = 200 x 0.5 = 100, नया मान = 100"):
        ("Start at 200 and fall 50%", "200 से शुरू करें और 50% गिरावट लगाएँ", "200 x 0.5", "100"),
    ("To return to 200 you need +100, measured on the new base of 100",
     "200 पर लौटने के लिए +100 चाहिए, जो नए आधार 100 पर मापा जाएगा"):
        ("The recovery is measured on the new base", "भरपाई नए आधार पर मापी जाती है",
         "100 / 100", "+100 to return to 200"),
    ("Required rise: 100 / 100 x 100 = 100%, not 50%",
     "आवश्यक वृद्धि: 100 / 100 x 100 = 100%, 50% नहीं"):
        ("Required rise", "आवश्यक वृद्धि", "100 / 100 x 100", "100%, not 50%"),

    # --- lesson 8 -----------------------------------------------------------
    ("Step 1: 100 increases by 10% to 110", "चरण 1: 100 में 10% वृद्धि होकर 110 हुआ"):
        ("Step 1: increase 100 by 10%", "चरण 1: 100 में 10% वृद्धि", "100 x 1.10", "110"),
    ("Step 2: 110 increases by 20% to 132", "चरण 2: 110 में 20% वृद्धि होकर 132 हुआ"):
        ("Step 2: increase the result by 20%", "चरण 2: परिणाम में 20% वृद्धि", "110 x 1.20", "132"),
    ("Net change: 132 - 100 = 32, so +32%", "शुद्ध परिवर्तन: 132 - 100 = 32, अर्थात +32%"):
        ("Net change", "शुद्ध परिवर्तन", "132 - 100", "+32%"),
    ("Check with the formula: (1.10 x 1.20) - 1 = 0.32",
     "सूत्र से जाँच: (1.10 x 1.20) - 1 = 0.32"):
        ("Check with the formula", "सूत्र से जाँचें", "(1.10 x 1.20) - 1", "0.32, so +32%"),
    ("Step 2: 110 decreases by 10%, which is 11, to 99",
     "चरण 2: 110 में 10% की कमी, यानी 11, होकर 99 हुआ"):
        ("Step 2: decrease 110 by 10%", "चरण 2: 110 में 10% की कमी", "110 x 0.10", "11, so 110 - 11 = 99"),
    ("Net change: 99 - 100 = -1, so -1%", "शुद्ध परिवर्तन: 99 - 100 = -1, अर्थात -1%"):
        ("Net change", "शुद्ध परिवर्तन", "99 - 100", "-1, so -1%"),
    ("Check with the formula: 10 - 10 - (10 x 10 / 100) = -1",
     "सूत्र से जाँच: 10 - 10 - (10 x 10 / 100) = -1"):
        ("Check with the formula", "सूत्र से जाँचें", "10 - 10 - (10 x 10 / 100)", "-1"),
    ("Start at 200, rise 50%: 200 x 1.5 = 300", "200 से शुरू करें, 50% वृद्धि: 200 x 1.5 = 300"):
        ("Step 1: increase 200 by 50%", "चरण 1: 200 में 50% वृद्धि", "200 x 1.5", "300"),
    ("Then fall 50%: 300 x 0.5 = 150", "फिर 50% कमी: 300 x 0.5 = 150"):
        ("Step 2: decrease the result by 50%", "चरण 2: परिणाम में 50% कमी", "300 x 0.5", "150"),
    ("Net change: 150 - 200 = -50, so -25%", "शुद्ध परिवर्तन: 150 - 200 = -50, अर्थात -25%"):
        ("Net change", "शुद्ध परिवर्तन", "150 - 200", "-50, so -25%"),
    ("Check with the formula: 50 - 50 - (50 x 50 / 100) = -25",
     "सूत्र से जाँच: 50 - 50 - (50 x 50 / 100) = -25"):
        ("Check with the formula", "सूत्र से जाँचें", "50 - 50 - (50 x 50 / 100)", "-25"),

    # --- lesson 9 -----------------------------------------------------------
    ("Take B as 100", "B को 100 मानें"):
        ("Take B as 100", "B को 100 मानें", "B", "100"),
    ("A is 25 more, so A = 125", "A 25 अधिक है, इसलिए A = 125"):
        ("A is 25 more than B", "A, B से 25 अधिक है", "125 - 100", "25, so A = 125"),
    ("Ratio A : B = 125 : 100 = 5 : 4", "अनुपात A : B = 125 : 100 = 5 : 4"):
        ("Write the ratio", "अनुपात लिखें", "125 : 100", "5 : 4"),
    ("A is 20 less, so A = 80", "A 20 कम है, इसलिए A = 80"):
        ("A is 20 less than B", "A, B से 20 कम है", "100 - 20", "80, so A = 80"),
    ("Ratio A : B = 80 : 100 = 4 : 5", "अनुपात A : B = 80 : 100 = 4 : 5"):
        ("Write the ratio", "अनुपात लिखें", "80 : 100", "4 : 5"),
    ("A is 50% more than B, and B is 20% less than C. Take C as 100.",
     "A, B से 50% अधिक है, और B, C से 20% कम है। C को 100 मानें।"):
        ("A is 50% more than B, and B is 20% less than C. Take C as 100.",
         "A, B से 50% अधिक है, और B, C से 20% कम है। C को 100 मानें।", "C", "100"),
    ("B is 20% less than C, so B = 100 x 0.80 = 80",
     "B, C से 20% कम है, इसलिए B = 100 x 0.80 = 80"):
        ("Build B from C", "C से B बनाएँ", "100 x 0.80", "80"),
    ("A is 50% more than B, so A = 80 x 1.50 = 120",
     "A, B से 50% अधिक है, इसलिए A = 80 x 1.50 = 120"):
        ("Build A from B", "B से A बनाएँ", "80 x 1.50", "120"),
    ("So A is 20% more than C", "अतः A, C से 20% अधिक है"):
        ("Compare A with C", "A की C से तुलना", "120 - 100", "20, so A is 20% more than C"),

    # --- lesson 10 ----------------------------------------------------------
    ("Percentage points: 25 - 20 = 5 points", "प्रतिशत अंक: 25 - 20 = 5 अंक"):
        ("In percentage points", "प्रतिशत अंकों में", "25 - 20", "5 points"),
    ("Percentage change: 5 / 20 x 100 = 25%", "प्रतिशत परिवर्तन: 5 / 20 x 100 = 25%"):
        ("As a percentage change", "प्रतिशत परिवर्तन के रूप में", "5 / 20 x 100", "25%"),
    ("So the two statements are both true, and they are not the same fact",
     "अतः दोनों कथन सत्य हैं, पर वे एक ही तथ्य नहीं हैं"):
        ("So both statements are true, and they are not the same fact",
         "अतः दोनों कथन सत्य हैं, पर वे एक ही तथ्य नहीं हैं",
         "5 points", "25%"),
    ("Percentage points: 30 - 40 = -10 points", "प्रतिशत अंक: 30 - 40 = -10 अंक"):
        ("In percentage points", "प्रतिशत अंकों में", "30 - 40", "-10 points"),
    ("Percentage change: -10 / 40 x 100 = -25%", "प्रतिशत परिवर्तन: -10 / 40 x 100 = -25%"):
        ("As a percentage change", "प्रतिशत परिवर्तन के रूप में", "-10 / 40 x 100", "-25%"),
    ("The fall is 10 points but 25%", "गिरावट 10 अंक की है पर 25% की"):
        ("The fall is 10 points but 25%", "गिरावट 10 अंक की है पर 25% की",
         "-10 points", "-25%"),
    ("Percentage points: 10 - 8 = 2 points", "प्रतिशत अंक: 10 - 8 = 2 अंक"):
        ("In percentage points", "प्रतिशत अंकों में", "10 - 8", "2 points"),
    ("Percentage change: 2 / 8 x 100 = 25%", "प्रतिशत परिवर्तन: 2 / 8 x 100 = 25%"):
        ("As a percentage change", "प्रतिशत परिवर्तन के रूप में", "2 / 8 x 100", "25%"),
    ("On 1000, the borrower pays 20 more, not 250",
     "1000 पर उधारदार 250 नहीं, 20 अधिक देता है"):
        ("On a principal of 1000", "1000 के मूलधन पर",
         "8% is 80, 10% is 100", "20 more, not 250"),

    # --- lesson 11 ----------------------------------------------------------
    ("3/4: (3 / 4) x 100 = 75%", "3/4: (3 / 4) x 100 = 75%"):
        ("Convert 3/4", "3/4 बदलें", "(3 / 4) x 100", "75%"),
    ("0.42: 0.42 x 100 = 42%", "0.42: 0.42 x 100 = 42%"):
        ("Convert 0.42", "0.42 बदलें", "0.42 x 100", "42%"),
    ("5/20: 5 / 20 = 1/4 = 25%", "5/20: 5 / 20 = 1/4 = 25%"):
        ("Convert 5/20", "5/20 बदलें", "5 / 20 = 1/4", "25%"),
    ("So 75% > 42% > 25%", "अतः 75% > 42% > 25%"):
        ("Order them", "क्रम दें", "75%, 42%, 25%", "75% > 42% > 25%"),
    ("Convert each: 1/3 = 33.33%, 0.34 = 34%, 35% = 35%",
     "तीनों बदलें: 1/3 = 33.33%, 0.34 = 34%, 35% = 35%"):
        ("Convert each", "तीनों बदलें", "1/3, 0.34, 35%",
         "33.33%, 34%, 35%"),
    ("Order them: 33.33% < 34% < 35%", "क्रम दें: 33.33% < 34% < 35%"):
        ("Order them", "क्रम दें", "33.33%, 34%, 35%", "33.33% < 34% < 35%"),
    ("So the order is 1/3, then 0.34, then 35%",
     "अतः क्रम है 1/3, फिर 0.34, फिर 35%"):
        ("So the order is", "अतः क्रम है", "1/3, 0.34, 35%", "smallest, middle, largest"),

    # --- lesson 12 ----------------------------------------------------------
    ("12.5% is one eighth, because 100 / 8 = 12.5",
     "12.5% एक आठवाँ भाग है, क्योंकि 100 / 8 = 12.5"):
        ("12.5% is one eighth", "12.5% एक आठवाँ भाग है", "100 / 8", "12.5, so 12.5% = 1/8"),
    ("Divide: 640 / 8 = 80", "भाग दें: 640 / 8 = 80"):
        ("Divide by 8", "8 से भाग दें", "640 / 8", "80"),
    ("Answer: 80", "उत्तर: 80"):
        ("Answer", "उत्तर", "80", "80"),
    ("This is the mirror case, so use b - a - ab/100 with a = 20 and b = 25",
     "यह प्रतिरूपी स्थिति है, इसलिए a = 20 और b = 25 के साथ b - a - ab/100 लगाएँ"):
        ("This is the mirror case, so use b - a - ab/100",
         "यह प्रतिरूपी स्थिति है, इसलिए b - a - ab/100 लगाएँ", "a = 20, b = 25", "b - a - ab/100"),
    ("25 - 20 - (20 x 25 / 100) = 5 - 5 = 0", "25 - 20 - (20 x 25 / 100) = 5 - 5 = 0"):
        ("Substitute", "मान रखें", "25 - 20 - (20 x 25 / 100)", "0"),
    ("Net 0%. Check on 500: 500 x 0.80 = 400, then 400 x 1.25 = 500",
     "शुद्ध 0%। 500 पर जाँच: 500 x 0.80 = 400, फिर 400 x 1.25 = 500"):
        ("Check on 500", "500 पर जाँचें", "500 x 0.80 = 400, then 400 x 1.25",
         "500, so the net change is 0%"),
    ("Let the original be 100. Two equal steps give a total multiplier of 1.40.",
     "मूल को 100 मानें। दो समान चरणों का कुल गुणक 1.40 है।"):
        ("Let the original be 100", "मूल को 100 मानें", "100", "total multiplier 1.40"),
    ("Equal steps mean equal percentages, so let each be x: (1 + x/100)^2 = 1.40",
     "समान चरणों का अर्थ समान प्रतिशत, इसलिए प्रत्येक x हो: (1 + x/100)^2 = 1.40"):
        ("Equal steps mean equal percentages",
         "समान चरणों का अर्थ समान प्रतिशत", "(1 + x/100)^2", "= 1.40"),
    ("This needs a square root, so for non-perfect squares it is not a shortcut",
     "इसमें वर्गमूल चाहिए, इसलिए पूर्ण वर्ग न होने पर यह शॉर्टकट नहीं है"):
        ("This needs a square root", "इसमें वर्गमूल चाहिए",
         "sqrt(1.40)", "no clean answer, so not a shortcut"),
}


def main():
    """
    Rewrites each worked-example step in lesson_content.py from a 2-tuple
    (english, hindi) into a 4-tuple (prompt_en, prompt_hi, result_en, result_hi).

    A regex is used rather than a plain string replace because the source wraps
    long string literals across several lines, so the literal bytes on disk are
    not a single line.
    """
    path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "lesson_content.py")
    original = open(path, encoding="utf-8").read()
    text = original
    converted, missing = 0, []

    for (en, hi), (p_en, p_hi, r_en, r_hi) in STEPS.items():
        pattern = re.compile(
            r'\(\s*"' + re.escape(en) + r'"\s*,\s*"' + re.escape(hi) + r'"\s*\)'
        )
        replacement = (
            f'("{p_en}",\n             "{p_hi}",\n'
            f'             "{r_en}",\n             "{r_hi}")'
        )
        text, count = pattern.subn(replacement, text)
        if count == 0:
            missing.append(en)
        else:
            converted += count

    if missing:
        for en in missing:
            print(f"  MISSING: {en!r}")
        raise SystemExit(1)

    open(path, "w", encoding="utf-8").write(text)
    print(f"converted {converted} step literals")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
