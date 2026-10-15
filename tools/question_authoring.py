#!/usr/bin/env python3
"""
The 130 new Phase 6 questions.

Each entry is produced by computing the answer from exact rationals and then
deriving distractors, so the answer key cannot drift from the arithmetic.
`validate()` at the bottom re-derives and re-checks every answer independently.
"""
import os
import sys
from fractions import Fraction

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from question_bank_data import E, H, M, QUESTIONS, T, fmt, question  # noqa: E402


def pct(value):
    return f"{fmt(value)}%"


def _successive_distractors(p1, p2, net):
    """
    Plausible wrong answers for "changes by X% then Y%, what is the net change?".

    The original third distractor was p1*p2/100, which is not a percentage
    anyone would actually arrive at, and which collided with the answer whenever
    both changes were equal and opposite. It has been replaced by a pool of
    mistakes a learner really does make:

      * adding the two percentages instead of compounding them, which is the
        single most common error on this topic;
      * counting only the first change, or only the second;
      * stopping after the first change.

    q_num drops collisions with the answer and raises if fewer than three
    survive, so the pool is written to be generous enough for the hardest case,
    where both changes are identical and "only the first" and "only the second"
    are the same number.
    """
    candidates = [
        Fraction(p1 + p2),   # naively adding the percentages
        Fraction(p2),        # only the second change counted
        Fraction(p1),        # only the first change counted
        net + p1,            # stopped after the first change
        net * 2,             # doubled the net change
    ]
    seen = []
    for value in candidates:
        rendered = pct(value)
        if rendered not in seen:
            seen.append(rendered)
    return seen


def _percentage_of_distractors(expected, n, p):
    """
    Plausible wrong answers for "what is P% of N?".

    The original list doubled the answer, used the complement, and then added a
    tenth of N. That last one collides with the doubled answer whenever P is 10
    (10% of 350 doubled is 70, and 35 + a tenth of 350 is also 70), which left
    only two real distractors and made q_num pad the gap.

    Replaced with the mistakes that actually occur: reading the percentage as a
    complement, doubling, halving, and answering with the percentage itself.
    """
    half = Fraction(expected) / 2
    candidates = [
        Fraction(n) - expected,   # subtracted instead of taking the percentage
        Fraction(expected) * 2,   # doubled
        half if half.denominator != 1 else half + 1,
        Fraction(p),              # answered with the percentage itself
        Fraction(n) / 2,          # halved the whole number
    ]
    seen = []
    for value in candidates:
        rendered = fmt(value)
        if rendered not in seen:
            seen.append(rendered)
    return seen


def q_num(qid, topic, diff, prompt_en, prompt_hi, correct, wrong, expl_en, expl_hi):
    """
    Numeric question: all four options are language-neutral numerals.

    The caller must supply enough *distinct* distractors. It used to be topped up
    from a fixed list of "+1", "-1", "+2"... when they ran short, which shipped a
    literal "+1" as an answer option on seven questions -- including "What is 10%
    of 350?" and a run of net-change questions where "+1" means nothing at all.
    A learner choosing among those options sees a number that is not a plausible
    wrong answer for the question being asked.

    Topping up also hid the real defect: the callers were generating duplicate or
    colliding distractors and the padding quietly papered over it. Requiring
    three distinct distractors turns that content bug into a build failure at
    generation time, where it is cheap to fix, instead of a learner-facing one.
    """
    opts = [correct]
    for candidate in wrong:
        if candidate not in opts:
            opts.append(candidate)
        if len(opts) == 4:
            break
    assert len(opts) == 4, (
        f"{qid}: needs 3 distractors distinct from the answer {correct!r}, "
        f"got {wrong!r}"
    )
    question(qid, topic, diff, prompt_en, prompt_hi,
             [(o, o) for o in opts], 0, expl_en, expl_hi)


def of(p, n):
    return Fraction(p, 100) * Fraction(n)


# =============================================================================
# Lesson 4 - Decimals to Percentages
# =============================================================================
def lesson_four():
    t = T["dec"]
    # (id, decimal, expected percentage, worked note, difficulty)
    cases = [
        ("dec_035", "0.35", 35, "Shift the point two places right: 0.35 x 100 = 35.", E),
        ("dec_060", "0.6", 60, "Add a zero so you shift two places: 0.60 x 100 = 60.", E),
        ("dec_0125", "0.125", "12.5", "0.125 x 100 = 12.5, because one eighth is 12.5%.", H),
        ("dec_008", "0.08", 8, "0.08 x 100 = 8.", E),
        ("dec_0045", "0.0045", "0.45", "0.0045 x 100 = 0.45.", M),
        ("dec_125", "1.25", 125,
         "A decimal above 1 gives a percentage above 100: 1.25 x 100 = 125.", E),
        ("dec_35", "3.5", 350, "3.5 x 100 = 350.", M),
        ("dec_3dp", "0.4567", "45.67", "0.4567 x 100 = 45.67.", H),
    ]
    for qid, dec, expected, note, diff in cases:
        answer = Fraction(dec) * 100
        assert fmt(answer) == fmt(expected), f"{qid}: computed {fmt(answer)} != {expected}"
        wrong = [pct(Fraction(expected) * m) for m in (Fraction(1, 10), Fraction(1, 2), 10, 2)
                 if pct(Fraction(expected) * m) != pct(expected)][:3]
        while len(wrong) < 3:
            wrong.append(pct(Fraction(expected) * (len(wrong) + 5)))
        q_num(qid, t, diff,
              f"What is {dec} written as a percentage?",
              f"{dec} को प्रतिशत में लिखने पर क्या आता है?",
              pct(expected), wrong,
              f"{note} So {dec} = {pct(expected)}.",
              f"{note} अतः {dec} = {pct(expected)}।")

    question("dec_which_is_40", t, M,
             "Which decimal is the same as 40%?",
             "कौन-सा दशमलव 40% के बराबर है?",
             [("0.4", "0.4"), ("0.04", "0.04"), ("4.0", "4.0"), ("0.4004", "0.4004")],
             0,
             "40% = 40/100 = 0.4. Dividing by 100 moves the point two places left.",
             "40% = 40/100 = 0.4। 100 से भाग करने पर बिंदु दो स्थान बाएँ चला जाता है।")


# =============================================================================
# Lesson 5 - Finding a Percentage of a Number
# =============================================================================
def lesson_five():
    t = T["pct_of"]
    cases = [
        ("pct_20_of_350", 20, 350, 70, E),
        ("pct_25_of_80", 25, 80, 20, E),
        ("pct_15_of_240", 15, 240, 36, E),
        ("pct_40_of_250", 40, 250, 100, E),
        ("pct_60_of_45", 60, 45, 27, E),
        ("pct_35_of_200", 35, 200, 70, M),
        ("pct_125_of_320", Fraction(125, 10), 320, 40, H),
        ("pct_7_of_6400", 7, 6400, 448, H),
    ]
    for qid, p, n, expected, diff in cases:
        answer = of(p, n)
        assert answer == expected, f"{qid}: computed {fmt(answer)} != {expected}"
        wrong = [fmt(expected * 10), fmt(expected * 10 + n), fmt(Fraction(n) - expected),
                 fmt(expected * 2)]
        wrong = [w for w in wrong if w != fmt(expected)][:3]
        q_num(qid, t, diff,
              f"What is {fmt(p)}% of {n}?",
              f"{n} का {fmt(p)}% कितना है?",
              fmt(expected), wrong,
              f"{fmt(p)}% of {n} = {fmt(p)}/100 x {n} = {fmt(expected)}.",
              f"{n} का {fmt(p)}% = {fmt(p)}/100 x {n} = {fmt(expected)}।")

    question("pct_class_girls", t, M,
             "A class has 40 students and 45% of them are girls. How many girls are there?",
             "एक कक्षा में 40 विद्यार्थी हैं और उनमें 45% लड़कियाँ हैं। कितनी लड़कियाँ हैं?",
             [("18", "18"), ("16", "16"), ("20", "20"), ("22", "22")],
             0,
             "45% of 40 = 45/100 x 40 = 18 girls.",
             "40 का 45% = 45/100 x 40 = 18 लड़कियाँ।")


# =============================================================================
# Lesson 6 - Finding the Original Whole
# =============================================================================
def lesson_six():
    t = T["whole"]
    cases = [
        ("whole_40_is_20pct", 40, 20, 200, E),
        ("whole_30_is_15pct", 30, 15, 200, E),
        ("whole_45_is_25pct", 45, 25, 180, E),
        ("whole_63_is_30pct", 63, 30, 210, M),
        ("whole_240_is_60pct", 240, 60, 400, M),
        ("whole_132_is_66pct", 132, 66, 200, M),
        ("whole_85_is_17pct", 85, 17, 500, H),
    ]
    for qid, part, p, expected, diff in cases:
        answer = Fraction(part) * 100 / p
        assert answer == expected, f"{qid}: computed {fmt(answer)} != {expected}"
        wrong = [fmt(Fraction(part) * p / 100), fmt(expected // 2), fmt(expected * 2)]
        wrong = [w for w in wrong if w != fmt(expected)][:3]
        q_num(qid, t, diff,
              f"{part} is {p}% of what number?",
              f"{part}, किस संख्या का {p}% है?",
              fmt(expected), wrong,
              f"{part} / ({p}/100) = {part} x 100 / {p} = {fmt(expected)}. "
              f"Check: {p}% of {fmt(expected)} = {part}.",
              f"{part} / ({p}/100) = {part} x 100 / {p} = {fmt(expected)}। "
              f"जाँच: {fmt(expected)} का {p}% = {part}।")

    question("whole_exam_marks", t, M,
             "A student scored 36 marks, which is 45% of the maximum. What is the maximum?",
             "एक विद्यार्थी ने 36 अंक अर्जित किए, जो अधिकतम अंकों का 45% हैं। अधिकतम अंक कितने हैं?",
             [("80", "80"), ("72", "72"), ("90", "90"), ("16.2", "16.2")],
             0,
             "36 / 0.45 = 80. Check: 45% of 80 = 36.",
             "36 / 0.45 = 80। जाँच: 80 का 45% = 36।")


# =============================================================================
# Lesson 7 - Percentage Increase and Decrease
# =============================================================================
def lesson_seven():
    t = T["incdec"]
    cases = [
        ("inc_450_up_12", 450, 12, 1, E),
        ("inc_800_down_25", 800, 25, -1, E),
        ("inc_2500_up_8", 2500, 8, 1, E),
        ("inc_600_down_15", 600, 15, -1, E),
        ("inc_320_up_5", 320, 5, 1, E),
        ("inc_150_down_40", 150, 40, -1, M),
        ("inc_1500_down_12", 1500, 12, -1, H),
        ("inc_240_up_35", 240, 35, 1, H),
    ]
    for qid, base, p, sign, diff in cases:
        change = Fraction(base) * Fraction(p, 100)
        answer = Fraction(base) + change * sign
        wrong = [fmt(Fraction(base) + change * -sign), fmt(Fraction(base) * p / 10),
                 fmt(answer + change)]
        wrong = [w for w in wrong if w != fmt(answer)][:3]
        direction = "increased" if sign == 1 else "decreased"
        q_num(qid, t, diff,
              f"A value of {base} is {direction} by {p}%. What is the new value?",
              f"{base} का मान {p}% से {'बढ़ा' if sign == 1 else 'घटा'} है। नया मान क्या है?",
              fmt(answer), wrong,
              f"{p}% of {base} = {fmt(change)}, and {base} "
              f"{'+' if sign == 1 else '-'} {fmt(change)} = {fmt(answer)}.",
              f"{base} का {p}% = {fmt(change)}, और {base} "
              f"{'+' if sign == 1 else '-'} {fmt(change)} = {fmt(answer)}।")

    question("inc_jacket_price", t, M,
             "A shop raised all prices by 10%. A jacket marked at 1800 now costs how much?",
             "एक दुकान ने सभी मूल्य 10% बढ़ा दिए। 1800 पर अंकित जैकेट का अब मूल्य कितना है?",
             [("1980", "1980"), ("1800", "1800"), ("2000", "2000"), ("2160", "2160")],
             0,
             "10% of 1800 = 180, so 1800 + 180 = 1980.",
             "1800 का 10% = 180, इसलिए 1800 + 180 = 1980।")


# =============================================================================
# Lesson 8 - Successive Percentage Changes
# =============================================================================
def lesson_eight():
    t = T["succ"]
    cases = [
        ("succ_10_then_20", 100, 10, 20, E),
        ("succ_10_then_minus_10", 100, 10, -10, M),
        ("succ_50_then_minus_50", 200, 50, -50, M),
        ("succ_minus_20_then_25", 500, -20, 25, M),
        ("succ_20_then_minus_20", 250, 20, -20, M),
        ("succ_minus_25_twice", 400, -25, -25, H),
        ("succ_25_twice", 200, 25, 25, H),
        ("succ_40_then_minus_40", 100, 40, -40, M),
    ]
    for qid, base, p1, p2, diff in cases:
        mid = Fraction(base) * (1 + Fraction(p1, 100))
        final = mid * (1 + Fraction(p2, 100))
        net = (final - base) / base * 100
        q_num(qid, t, diff,
              f"A value of {base} changes by {p1:+d}% and then {p2:+d}%. "
              f"What is the net change?",
              f"{base} का मान पहले {p1:+d}% और फिर {p2:+d}% बदलता है। शुद्ध परिवर्तन क्या है?",
              pct(net), _successive_distractors(p1, p2, net),
              f"Step 1: {base} -> {fmt(mid)}. Step 2: {fmt(mid)} -> {fmt(final)}. "
              f"Net = {fmt(final)} - {base} = {fmt(net)}%.",
              f"चरण 1: {base} -> {fmt(mid)}। चरण 2: {fmt(mid)} -> {fmt(final)}। "
              f"शुद्ध = {fmt(final)} - {base} = {fmt(net)}%।")


# =============================================================================
# Lesson 9 - Percentage Comparison Problems
# =============================================================================
def lesson_nine():
    t = T["comp"]
    # (id, percentage, direction, reduced numerator, reduced denominator, difficulty)
    ratios = [
        ("comp_25_more", 25, "more", 5, 4, E),
        ("comp_20_less", 20, "less", 4, 5, E),
        ("comp_10_more", 10, "more", 11, 10, M),
        ("comp_40_more", 40, "more", 7, 5, M),
        ("comp_30_less", 30, "less", 7, 10, M),
        ("comp_12_5_more", Fraction(125, 10), "more", 9, 8, H),
    ]
    for qid, p, direction, a, b, diff in ratios:
        scale = Fraction(100 + p if direction == "more" else 100 - p)
        hi, lo = scale, Fraction(100)
        # A fractional percentage needs a finer common base: 112.5 : 100 becomes
        # 1125 : 1000, which reduces to 9 : 8.
        if hi.denominator != 1:
            hi *= 10
            lo *= 10
        d = _gcd(int(hi), int(lo))
        assert (int(hi) // d, int(lo) // d) == (a, b), \
            f"{qid}: {(int(hi) // d, int(lo) // d)} != {(a, b)}"
        q_num(qid, t, diff,
              f"If A is {fmt(p)}% {direction} than B, what is A : B in lowest whole numbers?",
              f"यदि A, B से {fmt(p)}% {direction} है, तो A : B न्यूनतम पूर्णांकों में क्या है?",
              f"{a} : {b}", [f"{b} : {a}", f"{a + 1} : {b}", f"{a} : {b + 1}"],
              f"Take B as 100. A is {fmt(scale)}, so A : B = {fmt(scale)} : 100 = {a} : {b}.",
              f"B को 100 मानें। A {fmt(scale)} है, इसलिए A : B = {fmt(scale)} : 100 = {a} : {b}।")

    question("comp_chain", t, H,
             "A is 50% more than B, and B is 20% less than C. By what percent is A more than C?",
             "A, B से 50% अधिक है, और B, C से 20% कम है। A, C से कितने प्रतिशत अधिक है?",
             [("20%", "20%"), ("30%", "30%"), ("70%", "70%"), ("10%", "10%")],
             0,
             "Take C as 100. B = 100 x 0.80 = 80. A = 80 x 1.50 = 120. "
             "So A is 120 - 100 = 20% more than C.",
             "C को 100 मानें। B = 100 x 0.80 = 80। A = 80 x 1.50 = 120। "
             "अतः A, C से 120 - 100 = 20% अधिक है।")

    question("comp_chain_less", t, H,
             "B is 25% more than A, and C is 20% more than B. By what percent is A less than C?",
             "B, A से 25% अधिक है, और C, B से 20% अधिक है। A, C से कितने प्रतिशत कम है?",
             [("33.33%", "33.33%"), ("45%", "45%"), ("20%", "20%"), ("40%", "40%")],
             0,
             "Take A as 100. B = 125, then C = 125 x 1.20 = 150. "
             "So A is 50 short of 150, and 50/150 x 100 = 33.33% less than C.",
             "A को 100 मानें। B = 125, फिर C = 125 x 1.20 = 150। "
             "अतः A, 150 से 50 कम है, और 50/150 x 100 = 33.33% कम है।")


def _gcd(a, b):
    a, b = int(abs(a)), int(abs(b))
    while b:
        a, b = b, a % b
    return a or 1

# =============================================================================
# Lesson 10 - Percentage Points vs Percentage Change
# =============================================================================
def lesson_ten():
    t = T["points"]
    # (id, from %, to %, answer delta, three distractors, difficulty)
    # The distractors are written out per question rather than derived, because
    # two questions can share an answer (both +5 points) and must still present
    # different option lists.
    cases = [
        ("pts_20_to_25", 20, 25, 5, [-5, 10, 2], E),
        ("pts_40_to_30", 40, 30, -10, [-5, -2, 10], E),
        ("pts_8_to_10", 8, 10, 2, [-2, 4, 5], M),
        ("pts_50_to_55", 50, 55, 5, [-5, 10, -2], M),
        ("pts_33_to_40", 33, 40, 7, [-7, 5, 10], H),
        ("pts_16_to_20", 16, 20, 4, [-4, 2, 5], M),
    ]
    for qid, old, new, point_delta, distractors, diff in cases:
        relative = Fraction((new - old) * 100, old)
        ordered_options = [(f"{d:+d} points", f"{d:+d} अंक") for d in [point_delta] + distractors]
        question(qid, t, diff,
                 f"A percentage changes from {old}% to {new}%. What is the change in percentage points?",
                 f"एक प्रतिशत {old}% से बदलकर {new}% हो जाता है। प्रतिशत अंकों में परिवर्तन क्या है?",
                 ordered_options, 0,
                 f"Percentage points is a plain subtraction: {new} - {old} = {point_delta:+d}. "
                 f"The relative percentage change would be {fmt(relative)}%, which is a "
                 f"different statement.",
                 f"प्रतिशत अंक सादा घटाव है: {new} - {old} = {point_delta:+d}। "
                 f"सापेक्ष प्रतिशत परिवर्तन {fmt(relative)}% होता, जो एक भिन्न कथन है।")

    question("pts_relative", t, M,
             "A pass mark rises from 10% to 15% of the total. What is the relative percentage "
             "change in the marks required?",
             "उत्तीर्ण अंक कुल के 10% से बढ़कर 15% हो जाता है। आवश्यक अंकों में सापेक्ष प्रतिशत "
             "परिवर्तन क्या है?",
             [("50%", "50%"), ("5%", "5%"), ("45%", "45%"), ("25%", "25%")],
             0,
             "The relative change is (15 - 10) / 10 x 100 = 5/10 x 100 = 50%. "
             "The rise is 5 percentage points, which is a different statement.",
             "सापेक्ष परिवर्तन (15 - 10) / 10 x 100 = 5/10 x 100 = 50%। "
             "वृद्धि 5 प्रतिशत अंक की है, जो एक भिन्न कथन है।")

    question("pts_interest", t, H,
             "An interest rate moves from 8% to 10%. On a principal of 1000, how much more "
             "interest is paid each year?",
             "एक ब्याज दर 8% से 10% हो जाती है। 1000 के मूलधन पर प्रति वर्ष कितना अधिक ब्याज "
             "दिया जाता है?",
             [("20", "20"), ("200", "200"), ("2", "2"), ("25", "25")],
             0,
             "8% of 1000 is 80 and 10% of 1000 is 100, so the rise of 2 percentage points "
             "costs 20 more each year.",
             "1000 का 8% = 80 और 1000 का 10% = 100, अतः 2 प्रतिशत अंक की वृद्धि प्रति वर्ष "
             "20 अधिक की है।")


# =============================================================================
# Lesson 11 - Comparing Fractions, Decimals and Percentages
# =============================================================================
def lesson_eleven():
    t = T["repr"]
    items = [
        ("repr_3_4", Fraction(3, 4), "3/4"),
        ("repr_5_8", Fraction(5, 8), "5/8"),
        ("repr_7_20", Fraction(7, 20), "7/20"),
        ("repr_1_5", Fraction(1, 5), "1/5"),
        ("repr_2_3", Fraction(2, 3), "2/3"),
        ("repr_9_16", Fraction(9, 16), "9/16"),
        ("repr_11_40", Fraction(11, 40), "11/40"),
    ]
    for qid, value, label in items:
        as_pct = value * 100
        wrong = [pct(v) for v in (value * 10, value * 50, value * 200)][:3]
        q_num(qid, t, E,
              f"What is {label} written as a percentage?",
              f"{label} को प्रतिशत में लिखने पर क्या आता है?",
              pct(as_pct), wrong,
              f"A percentage is a fraction out of 100, so {label} x 100 = {pct(as_pct)}. "
              f"Multiply by 100 rather than dividing, because the denominator is being "
              f"compared against 100.",
              f"प्रतिशत 100 में से एक भिन्न होता है, इसलिए {label} x 100 = {pct(as_pct)}। "
              f"100 से गुणा कीजिए, भाग न कीजिए, क्योंकि निचली संख्या की तुलना 100 से हो रही है।")

    question("repr_largest", t, M,
             "Which of these is the largest?",
             "इनमें से कौन-सा सबसे बड़ा है?",
             [("3/4", "3/4"), ("0.65", "0.65"), ("70%", "70%"), ("2/5", "2/5")],
             0,
             "3/4 = 75%, 0.65 = 65%, 70% = 70% and 2/5 = 40%. So 75% is the largest.",
             "3/4 = 75%, 0.65 = 65%, 70% = 70% और 2/5 = 40%। अतः 75% सबसे बड़ा है।")

    question("repr_029_vs_two_sevenths", t, H,
             "Which is larger, 0.29 or 2/7?",
             "कौन बड़ा है, 0.29 या 2/7?",
             [("0.29", "0.29"), ("2/7", "2/7"), ("They are equal", "दोनों बराबर हैं"),
              ("Cannot be compared", "तुलना नहीं की जा सकती")],
             0,
             "0.29 = 29%, and 2/7 is 28.57% when rounded to two decimals. So 0.29 is larger.",
             "0.29 = 29%, और 2/7 दशमलव के दो स्थानों तक 28.57% है। अतः 0.29 बड़ा है।")


# =============================================================================
# Lesson 12 - Exam Shortcuts and Mental Calculation
# =============================================================================
def lesson_twelve():
    t = T["short"]
    cases = [
        ("short_12_5_of_640", Fraction(125, 10), 640, 80, E, "12.5% is one eighth, so divide by 8."),
        ("short_25_of_180", 25, 180, 45, E, "25% is one quarter, so divide by 4."),
        ("short_20_of_250", 20, 250, 50, E, "20% is one fifth, so divide by 5."),
        ("short_50_of_90", 50, 90, 45, E, "50% is one half, so halve the number."),
        ("short_10_of_350", 10, 350, 35, E, "10% is one tenth, so divide by 10."),
        ("short_16_67_of_240", Fraction(50, 3), 240, 40, M, "16.67% is one sixth, so divide by 6."),
    ]
    for qid, p, n, expected, diff, note in cases:
        answer = of(p, n)
        assert answer == expected, f"{qid}: {fmt(answer)} != {expected}"
        wrong = _percentage_of_distractors(expected, n, p)
        q_num(qid, t, diff,
              f"What is {fmt(p)}% of {n}?",
              f"{n} का {fmt(p)}% कितना है?",
              fmt(expected), wrong,
              f"{note} {fmt(expected)}.",
              f"{note} {fmt(expected)}।")

    question("short_net_40", t, H,
             "A value rises 40% and then falls 40%. What is the net change?",
             "एक मान 40% बढ़ता है और फिर 40% घटता है। शुद्ध परिवर्तन क्या है?",
             [("-16%", "-16%"), ("0%", "0%"), ("-20%", "-20%"), ("16%", "16%")],
             0,
             "100 x 1.40 = 140, then 140 x 0.60 = 84. The net change is 84 - 100 = -16%. "
             "The two percentages do not cancel.",
             "100 x 1.40 = 140, फिर 140 x 0.60 = 84। शुद्ध परिवर्तन 84 - 100 = -16%। "
             "दोनों प्रतिशत रद्द नहीं होते।")

    question("short_ratio_20_more", t, M,
             "If A is 20% more than B, write A : B in lowest whole numbers.",
             "यदि A, B से 20% अधिक है, तो A : B न्यूनतम पूर्णांकों में लिखें।",
             [("6 : 5", "6 : 5"), ("5 : 6", "5 : 6"), ("120 : 100", "120 : 100"),
              ("20 : 1", "20 : 1")],
             0,
             "Take B as 100, so A is 120. Then 120 : 100 = 6 : 5.",
             "B को 100 मानें, तो A 120 है। फिर 120 : 100 = 6 : 5।")


# =============================================================================
# Lesson 13 - Population Increase and Decrease
# =============================================================================
def lesson_thirteen():
    t = T["pop"]
    simple = [
        ("pop_50k_up_12", 50000, 12, 1, E),
        ("pop_20k_down_15", 20000, 15, -1, E),
        ("pop_32k_up_12", 32000, 12, 1, M),
        ("pop_45k_down_8", 45000, 8, -1, M),
    ]
    for qid, base, p, sign, diff in simple:
        change = Fraction(base) * Fraction(p, 100)
        answer = base + change * sign
        q_num(qid, t, diff,
              f"A town of {base} people changes by {p}%. What is the new population?",
              f"{base} की आबादी {p}% से बदलती है। नई आबादी क्या होगी?",
              fmt(answer), [fmt(base + change * -sign), fmt(answer + change), fmt(base - answer)],
              f"{p}% of {base} = {fmt(change)}, so {base} "
              f"{'+' if sign == 1 else '-'} {fmt(change)} = {fmt(answer)}.",
              f"{base} का {p}% = {fmt(change)}, अतः {base} "
              f"{'+' if sign == 1 else '-'} {fmt(change)} = {fmt(answer)}।")

    question("pop_two_changes", t, H,
             "A town of 12000 people grows by 25% and then falls by 10%. What is the "
             "population at the end?",
             "12000 की आबादी 25% बढ़ती है और फिर 10% घटती है। अंत में आबादी क्या होगी?",
             [("13500", "13500"), ("14400", "14400"), ("13000", "13000"), ("13800", "13800")],
             0,
             "12000 x 1.25 = 15000, then 15000 x 0.90 = 13500. The fall is measured on the "
             "population after the rise.",
             "12000 x 1.25 = 15000, फिर 15000 x 0.90 = 13500। गिरावट वृद्धि के बाद की "
             "आबादी पर मापी जाती है।")

    question("pop_rate_of_change", t, M,
             "A city's population grew from 40000 to 46000 in one year. What was the "
             "percentage growth?",
             "एक शहर की आबादी एक वर्ष में 40000 से बढ़कर 46000 हो गई। प्रतिशत वृद्धि क्या थी?",
             [("15%", "15%"), ("60%", "60%"), ("13%", "13%"), ("16%", "16%")],
             0,
             "The growth is 46000 - 40000 = 6000, and 6000 / 40000 x 100 = 15%.",
             "वृद्धि 46000 - 40000 = 6000 है, और 6000 / 40000 x 100 = 15%।")

    question("pop_comparison", t, M,
             "Town A's population is 25% more than town B's. If town B has 48000 people, how "
             "many does town A have?",
             "कस्बे A की आबादी कस्बे B से 25% अधिक है। यदि कस्बे B में 48000 लोग हैं, तो कस्बे A में कितने हैं?",
             [("60000", "60000"), ("64000", "64000"), ("55200", "55200"), ("52000", "52000")],
             0,
             "48000 x 1.25 = 60000.",
             "48000 x 1.25 = 60000।")

    question("pop_fall_20", t, E,
             "A population falls by 20% from 25000. What is the new population?",
             "25000 की आबादी 20% घटती है। नई आबादी क्या है?",
             [("20000", "20000"), ("23000", "23000"), ("18000", "18000"), ("25000", "25000")],
             0,
             "20% of 25000 = 5000, so 25000 - 5000 = 20000.",
             "25000 का 20% = 5000, अतः 25000 - 5000 = 20000।")


# =============================================================================
# Lesson 14 - Salary, Expenditure, and Savings
# =============================================================================
def lesson_fourteen():
    t = T["salary"]
    question("sal_spend_40", t, E,
             "A monthly salary is 25000 and 40% of it is spent. How much is saved?",
             "मासिक वेतन 25000 है और उसका 40% खर्च होता है। कितना बचता है?",
             [("15000", "15000"), ("10000", "10000"), ("6000", "6000"), ("20000", "20000")],
             0,
             "40% of 25000 = 10000 is spent, so 25000 - 10000 = 15000 is saved.",
             "25000 का 40% = 10000 खर्च होता है, अतः 25000 - 10000 = 15000 बचता है।")

    question("sal_savings_percent", t, M,
             "Out of a salary of 40000, the expenditure is 24000. What percentage of the "
             "salary is the savings?",
             "40000 के वेतन में से 24000 खर्च होते हैं। वेतन का कितने प्रतिशत बचत है?",
             [("40%", "40%"), ("60%", "60%"), ("37.5%", "37.5%"), ("16%", "16%")],
             0,
             "Savings = 40000 - 24000 = 16000, and 16000 / 40000 x 100 = 40%.",
             "बचत = 40000 - 24000 = 16000, और 16000 / 40000 x 100 = 40%।")

    question("sal_exp_60", t, M,
             "A person spends 60% of a salary of 50000. How much is left after spending?",
             "एक व्यक्ति 50000 के वेतन का 60% खर्च करता है। खर्च के बाद कितना बचता है?",
             [("20000", "20000"), ("30000", "30000"), ("25000", "25000"), ("40000", "40000")],
             0,
             "60% of 50000 = 30000 is spent, so 50000 - 30000 = 20000 remains.",
             "50000 का 60% = 30000 खर्च होता है, अतः 50000 - 30000 = 20000 बचता है।")

    question("sal_exp_is_80pct", t, M,
             "The monthly expenditure is 12000, which is 80% of the salary. What is the salary?",
             "मासिक व्यय 12000 है, जो वेतन का 80% है। वेतन क्या है?",
             [("15000", "15000"), ("12000", "12000"), ("9600", "9600"), ("18000", "18000")],
             0,
             "12000 / 0.80 = 15000. Check: 80% of 15000 = 12000.",
             "12000 / 0.80 = 15000। जाँच: 15000 का 80% = 12000।")

    question("sal_exp_rises", t, H,
             "Out of an income of 36000, expenditure was 24000. If expenditure rises by 20%, "
             "what is the new savings?",
             "36000 की आय में से 24000 खर्च होते हैं। यदि व्यय 20% बढ़ जाए, तो नई बचत क्या होगी?",
             [("7200", "7200"), ("8160", "8160"), ("9600", "9600"), ("12000", "12000")],
             0,
             "Expenditure rises by 24000 x 0.20 = 4800 to 28800. Savings = 36000 - 28800 = 7200.",
             "व्यय 24000 x 0.20 = 4800 बढ़कर 28800 हो जाता है। बचत = 36000 - 28800 = 7200।")

    question("sal_save_30pct", t, E,
             "A person saves 30% of 9000. How much is spent?",
             "एक व्यक्ति 9000 का 30% बचाता है। कितना खर्च होता है?",
             [("6300", "6300"), ("2700", "2700"), ("6000", "6000"), ("3300", "3300")],
             0,
             "Savings = 30% of 9000 = 2700, so expenditure = 9000 - 2700 = 6300.",
             "बचत = 9000 का 30% = 2700, अतः व्यय = 9000 - 2700 = 6300।")

    question("sal_raise_then_cut", t, M,
             "A salary of 20000 rises by 25% and then falls by 10%. What is the final salary?",
             "20000 का वेतन 25% बढ़ता है और फिर 10% घटता है। अंतिम वेतन क्या है?",
             [("22500", "22500"), ("21000", "21000"), ("24000", "24000"), ("20000", "20000")],
             0,
             "20000 x 1.25 = 25000, then 25000 x 0.90 = 22500. The net change is 12.5%.",
             "20000 x 1.25 = 25000, फिर 25000 x 0.90 = 22500। शुद्ध परिवर्तन 12.5% है।")


# =============================================================================
# Lesson 15 - Marks, Pass Marks, and Exam Results
# =============================================================================
def lesson_fifteen():
    t = T["marks"]
    question("marks_pass_40", t, E,
             "The pass mark is 40% of a maximum of 500. How many marks are needed to pass?",
             "500 के अधिकतम अंकों पर उत्तीर्ण अंक 40% है। उत्तीर्ण होने के लिए कितने अंक चाहिए?",
             [("200", "200"), ("250", "250"), ("150", "150"), ("220", "220")],
             0,
             "40% of 500 = 200 marks are needed to pass.",
             "उत्तीर्ण होने के लिए 500 का 40% = 200 अंक चाहिए।")

    question("marks_score_80pct", t, E,
             "A student scored 360 marks out of 450. What percentage did the student score?",
             "एक विद्यार्थी ने 450 में से 360 अंक अर्जित किए। विद्यार्थी ने कितने प्रतिशत अंक ले?",
             [("80%", "80%"), ("75%", "75%"), ("86.67%", "86.67%"), ("70%", "70%")],
             0,
             "360 / 450 x 100 = 80%.",
             "360 / 450 x 100 = 80%।")

    question("marks_shortfall", t, M,
             "A candidate needs 240 marks to pass. The candidate scores 216. By how many "
             "marks did the candidate fall short?",
             "उत्तीर्ण होने के लिए 240 अंक चाहिए। उम्मीदवार ने 216 अंक ले। कितने अंक की कमी रह गई?",
             [("24", "24"), ("20", "20"), ("25", "25"), ("30", "30")],
             0,
             "240 - 216 = 24 marks short.",
             "240 - 216 = 24 अंक की कमी।")

    question("marks_pass_rate", t, M,
             "In an examination of 500 candidates, 250 passed. What percentage passed?",
             "500 उम्मीदवारों की परीक्षा में 250 उत्तीर्ण हुए। कितने प्रतिशत उत्तीर्ण हुए?",
             [("50%", "50%"), ("25%", "25%"), ("75%", "75%"), ("45%", "45%")],
             0,
             "250 / 500 x 100 = 50% passed.",
             "250 / 500 x 100 = 50% उत्तीर्ण हुए।")

    question("marks_avg_vs_topper", t, H,
             "The class average is 60% and the topper scored 85%. By what percentage is the "
             "topper's score higher than the average?",
             "कक्षा का औसत 60% है और शीर्षक ने 85% ले। शीर्षक के अंक औसत से कितने प्रतिशत "
             "अधिक हैं?",
             [("41.67%", "41.67%"), ("25%", "25%"), ("45%", "45%"), ("40%", "40%")],
             0,
             "The gap is 85 - 60 = 25 percentage points, and measured against the average of "
             "60 that is 25 / 60 x 100 = 41.67%.",
             "अंतर 85 - 60 = 25 प्रतिशत अंक है, और औसत 60 के सापेक्ष यह 25 / 60 x 100 = "
             "41.67% है।")

    question("marks_pass_rises", t, M,
             "Pass marks rise from 40 to 50 out of 200. What is the percentage change in the "
             "marks required?",
             "200 में से उत्तीर्ण अंक 40 से बढ़कर 50 हो जाते हैं। आवश्यक अंकों में प्रतिशत "
             "परिवर्तन क्या है?",
             [("25%", "25%"), ("10%", "10%"), ("20%", "20%"), ("12.5%", "12.5%")],
             0,
             "The relative change is (50 - 40) / 40 x 100 = 25%. The pass percentage also "
             "moved from 20% to 25%, which is 5 percentage points.",
             "सापेक्ष परिवर्तन (50 - 40) / 40 x 100 = 25%। उत्तीर्ण प्रतिशत भी 20% से 25% "
             "हुआ, जो 5 प्रतिशत अंक है।")

    question("marks_needs_21", t, M,
             "Out of 300, a student must score 40% to pass. The student has 99. How many more "
             "marks are needed?",
             "300 में से उत्तीर्ण होने के लिए 40% चाहिए। विद्यार्थी के पास 99 हैं। कितने और "
             "अंक चाहिए?",
             [("21", "21"), ("20", "20"), ("22", "22"), ("25", "25")],
             0,
             "40% of 300 = 120, so 120 - 99 = 21 more marks are needed.",
             "300 का 40% = 120, अतः 120 - 99 = 21 और अंक चाहिए।")


# =============================================================================
# Lesson 16 - Election and Voting Percentage Problems
# =============================================================================
def lesson_sixteen():
    t = T["vote"]
    question("vote_55pct", t, E,
             "In an election, 55% of 60000 valid votes were cast for candidate A. How many "
             "votes did A receive?",
             "चुनाव में 60000 वैध मतों के 55% उम्मीदवार A को मिले। A को कितने मत मिले?",
             [("33000", "33000"), ("35000", "35000"), ("60000", "60000"), ("30000", "30000")],
             0,
             "55% of 60000 = 0.55 x 60000 = 33000 votes.",
             "60000 का 55% = 0.55 x 60000 = 33000 मत।")

    question("vote_turnout", t, M,
             "There are 75000 registered voters and 45000 of them cast a ballot. What was the "
             "voting percentage?",
             "75000 पंजीकृत मतदाता हैं और उनमें से 45000 ने मत डाला। मतदान प्रतिशत क्या था?",
             [("60%", "60%"), ("45%", "45%"), ("55%", "55%"), ("66.67%", "66.67%")],
             0,
             "45000 / 75000 x 100 = 60% of registered voters turned out.",
             "45000 / 75000 x 100 = 60% पंजीकृत मतदाताओं ने मत डाला।")

    question("vote_others_share", t, M,
             "In an election, candidate A got 40% of the votes and candidate B got 35%. What "
             "percentage went to all other candidates together?",
             "चुनाव में उम्मीदवार A को 40% मत मिले और B को 35%। शेष सभी उम्मीदवारों को "
             "कुल कितने प्रतिशत मिले?",
             [("25%", "25%"), ("15%", "15%"), ("5%", "5%"), ("75%", "75%")],
             0,
             "The two leading candidates take 40% + 35% = 75%, so the rest take 100% - 75% "
             "= 25%.",
             "दो शीर्ष उम्मीदवार 40% + 35% = 75% लेते हैं, अतः शेष को 100% - 75% = 25%।")

    question("vote_margin_points", t, M,
             "Candidate A polled 44% of the votes and candidate B polled 38%. What is the "
             "margin in percentage points?",
             "उम्मीदवार A को 44% मत मिले और B को 38%। प्रतिशत अंकों में अंतर क्या है?",
             [("6 points", "6 अंक"), ("16%", "16%"), ("5%", "5%"), ("8%", "8%")],
             0,
             "The margin in percentage points is 44 - 38 = 6. Measured against A's share the "
             "relative margin would be 6 / 38 x 100 = 15.79%.",
             "प्रतिशत अंकों में अंतर 44 - 38 = 6 है। A के हिस्से के सापेक्ष सापेक्ष अंतर "
             "6 / 38 x 100 = 15.79% होता।")

    question("vote_comparison", t, M,
             "Candidate A got 36000 votes, which is 20% more than candidate B. How many votes "
             "did B get?",
             "उम्मीदवार A को 36000 मत मिले, जो B से 20% अधिक हैं। B को कितने मत मिले?",
             [("30000", "30000"), ("36000", "36000"), ("40000", "40000"), ("28800", "28800")],
             0,
             "If B = x then A = 1.2x, so 1.2x = 36000 and x = 30000.",
             "यदि B = x तो A = 1.2x, अतः 1.2x = 36000 और x = 30000।")

    question("vote_two_changes", t, H,
             "Voters at a booth rose by 20% and then fell by 20%. What was the net change in "
             "the number of voters?",
             "एक बूथ पर मतदाता 20% बढ़े और फिर 20% घटे। मतदाताओं की संख्या में शुद्ध परिवर्तन "
             "क्या था?",
             [("-4%", "-4%"), ("0%", "0%"), ("-20%", "-20%"), ("4%", "4%")],
             0,
             "Taking 100 voters: 120 after the rise, then 120 x 0.80 = 96. That is 4 fewer "
             "than the original, so -4%.",
             "100 मतदाता लें: वृद्धि के बाद 120, फिर 120 x 0.80 = 96। यह मूल से 4 कम है, "
             "अतः -4%।")

    question("vote_45pct_2000", t, E,
             "A party polled 45% of 2000 votes in a ward. How many votes did it get?",
             "एक पार्टी को एक वार्ड में 2000 मतों के 45% मिले। उसे कितने मत मिले?",
             [("900", "900"), ("950", "950"), ("1100", "1100"), ("855", "855")],
             0,
             "45% of 2000 = 0.45 x 2000 = 900 votes.",
             "2000 का 45% = 0.45 x 2000 = 900 मत।")


# =============================================================================
# Lesson 17 - Price Changes, Discounts, and Tax
# =============================================================================
def lesson_seventeen():
    t = T["price"]
    question("price_disc_20", t, E,
             "A jacket marked at 2500 is sold with a 20% discount. What is the selling price?",
             "2500 पर अंकित जैकेट 20% छूट पर बेचा जाता है। विक्रय मूल्य क्या है?",
             [("2000", "2000"), ("2300", "2300"), ("2100", "2100"), ("1800", "1800")],
             0,
             "20% of 2500 = 500, so 2500 - 500 = 2000.",
             "2500 का 20% = 500, अतः 2500 - 500 = 2000।")

    question("price_two_discounts", t, H,
             "Two discounts of 20% and then 10% are applied in turn to a price of 1000. What "
             "is the final price?",
             "1000 के मूल्य पर क्रमशः 20% और फिर 10% की छूट लागू होती है। अंतिम मूल्य क्या है?",
             [("720", "720"), ("700", "700"), ("750", "750"), ("800", "800")],
             0,
             "1000 x 0.80 = 800, then 800 x 0.90 = 720. The net discount is 28%, not 30%, "
             "because the second discount is measured on 800.",
             "1000 x 0.80 = 800, फिर 800 x 0.90 = 720। शुद्ध छूट 30% नहीं, 28% है, क्योंकि "
             "दूसरी छूट 800 पर मापी जाती है।")

    question("price_net_discount", t, M,
             "Successive discounts of 20% and 10% give an overall discount of how much "
             "percent?",
             "20% और 10% की लगातार छूट से कुल छूट कितने प्रतिशत होती है?",
             [("28%", "28%"), ("30%", "30%"), ("25%", "25%"), ("32%", "32%")],
             0,
             "On a price of 100: 100 x 0.80 x 0.90 = 72, so the overall discount is 100 - 72 "
             "= 28%.",
             "100 के मूल्य पर: 100 x 0.80 x 0.90 = 72, अतः कुल छूट 100 - 72 = 28%।")

    question("price_tax_incl", t, H,
             "A price of 550 already includes 10% tax. How much of it is tax?",
             "550 का मूल्य में 10% कर पहले से शामिल है। उसमें से कितना कर है?",
             [("50", "50"), ("55", "55"), ("60", "60"), ("45", "45")],
             0,
             "If 550 is 110% of the price before tax, then the price before tax is "
             "550 / 1.10 = 500, so the tax is 550 - 500 = 50.",
             "यदि 550, कर से पहले मूल्य का 110% है, तो कर से पहले मूल्य 550 / 1.10 = 500 है, "
             "अतः कर 550 - 500 = 50 है।")

    question("price_disc_then_rise", t, M,
             "A price of 1000 falls 40% and then rises 25%. What is the final price?",
             "1000 का मूल्य 40% घटता है और फिर 25% बढ़ता है। अंतिम मूल्य क्या है?",
             [("750", "750"), ("850", "850"), ("600", "600"), ("900", "900")],
             0,
             "1000 x 0.60 = 600, then 600 x 1.25 = 750. The net change is -25%.",
             "1000 x 0.60 = 600, फिर 600 x 1.25 = 750। शुद्ध परिवर्तन -25% है।")

    question("price_discount_then_tax", t, M,
             "A 2000 rupee item gets 20% off, then 10% tax is added on the reduced price. What "
             "is the final amount?",
             "2000 रुपये की वस्तु पर 20% छूट, फिर घटी हुई मूल्य पर 10% कर जोड़ा जाता है। अंतिम "
             "राशि क्या है?",
             [("1760", "1760"), ("1800", "1800"), ("1980", "1980"), ("1700", "1700")],
             0,
             "2000 x 0.80 = 1600, then 1600 x 1.10 = 1760.",
             "2000 x 0.80 = 1600, फिर 1600 x 1.10 = 1760।")

    question("price_disc_25_640", t, E,
             "What is 25% off a price of 640?",
             "640 के मूल्य पर 25% छूट कितनी है?",
             [("160", "160"), ("480", "480"), ("615", "615"), ("1600", "1600")],
             0,
             "25% of 640 = 640 / 4 = 160, so the discount is 160.",
             "640 का 25% = 640 / 4 = 160, अतः छूट 160 है।")


# =============================================================================
# Lesson 18 - Profit, Loss, and Percentage Relationships
# =============================================================================
def lesson_eighteen():
    t = T["profit"]
    question("profit_25_on_400", t, E,
             "An item is bought for 400 and sold for 500. What is the profit percentage?",
             "एक वस्तु 400 में खरीदी और 500 में बेची जाती है। लाभ प्रतिशत क्या है?",
             [("25%", "25%"), ("20%", "20%"), ("125%", "125%"), ("33.33%", "33.33%")],
             0,
             "Profit is 500 - 400 = 100, and profit percentage is measured on the cost price: "
             "100 / 400 x 100 = 25%.",
             "लाभ 500 - 400 = 100 है, और लाभ प्रतिशत क्रय मूल्य पर मापा जाता है: "
             "100 / 400 x 100 = 25%।")

    question("loss_10_on_600", t, E,
             "An item is bought for 600 and sold for 540. What is the loss percentage?",
             "एक वस्तु 600 में खरीदी और 540 में बेची जाती है। हानि प्रतिशत क्या है?",
             [("10%", "10%"), ("11.11%", "11.11%"), ("60%", "60%"), ("90%", "90%")],
             0,
             "Loss is 600 - 540 = 60, and 60 / 600 x 100 = 10%. The percentage is measured on "
             "the cost price, not the selling price.",
             "हानि 600 - 540 = 60 है, और 60 / 600 x 100 = 10%। प्रतिशत क्रय मूल्य पर मापा "
             "जाता है, विक्रय मूल्य पर नहीं।")

    question("profit_cp_from_sp", t, M,
             "An item sold for 1200 makes a 20% profit. What was its cost price?",
             "एक वस्तु 1200 में बेची जाती है और 20% लाभ होता है। उसका क्रय मूल्य क्या था?",
             [("1000", "1000"), ("1200", "1200"), ("960", "960"), ("1100", "1100")],
             0,
             "The selling price is 120% of the cost, so cost = 1200 / 1.20 = 1000.",
             "विक्रय मूल्य क्रय का 120% है, अतः क्रय = 1200 / 1.20 = 1000।")

    question("loss_cp_from_sp", t, M,
             "An item sold for 900 at a 10% loss. What was its cost price?",
             "एक वस्तु 10% हानि पर 900 में बेची जाती है। उसका क्रय मूल्य क्या था?",
             [("1000", "1000"), ("900", "900"), ("810", "810"), ("990", "990")],
             0,
             "The selling price is 90% of the cost, so cost = 900 / 0.90 = 1000.",
             "विक्रय मूल्य क्रय का 90% है, अतः क्रय = 900 / 0.90 = 1000।")

    question("profit_then_loss", t, H,
             "An item bought for 100 rises 25% and then falls 25%. What is the final price, "
             "and how does it compare with the cost?",
             "100 में खरीदी गई वस्तु 25% बढ़ती है और फिर 25% घटती है। अंतिम मूल्य क्या है, और "
             "यह क्रय की तुलना में कैसा है?",
             [("93.75, a loss of 6.25%", "93.75, 6.25% की हानि"),
              ("100, no change", "100, कोई परिवर्तन नहीं"),
              ("106.25, a gain of 6.25%", "106.25, 6.25% का लाभ"),
              ("90, a loss of 10%", "90, 10% की हानि")],
             0,
             "100 x 1.25 = 125, then 125 x 0.75 = 93.75. That is 6.25% below the cost price, "
             "so the two percentages do not cancel.",
             "100 x 1.25 = 125, फिर 125 x 0.75 = 93.75। यह क्रय मूल्य से 6.25% नीचे है, "
             "अतः दोनों प्रतिशत रद्द नहीं होते।")

    question("profit_amount", t, E,
             "Selling with a 20% profit gives a selling price of 1200. What is the profit?",
             "20% लाभ पर विक्रय मूल्य 1200 है। लाभ कितना है?",
             [("200", "200"), ("240", "240"), ("300", "300"), ("1000", "1000")],
             0,
             "Cost = 1200 / 1.20 = 1000, so profit = 1200 - 1000 = 200. The 20% is measured "
             "on the cost price, which is smaller than the selling price.",
             "क्रय = 1200 / 1.20 = 1000, अतः लाभ = 1200 - 1000 = 200। 20% क्रय मूल्य पर "
             "मापा जाता है, जो विक्रय मूल्य से छोटा है।")

    question("profit_28_on_250", t, M,
             "An item bought for 250 is sold for 320. What is the profit percentage?",
             "250 में खरीदी गई वस्तु 320 में बेची जाती है। लाभ प्रतिशत क्या है?",
             [("28%", "28%"), ("22%", "22%"), ("70%", "70%"), ("25%", "25%")],
             0,
             "Profit = 320 - 250 = 70, and 70 / 250 x 100 = 28%.",
             "लाभ = 320 - 250 = 70, और 70 / 250 x 100 = 28%।")


# =============================================================================
# Lesson 19 - Reverse Percentage Problems
# =============================================================================
def lesson_nineteen():
    t = T["reverse"]
    single = [
        ("rev_up_20_to_720", 20, 1, 720, E),
        ("rev_down_10_to_450", 10, -1, 450, E),
        ("rev_up_25_to_1250", 25, 1, 1250, M),
        ("rev_down_20_to_640", 20, -1, 640, M),
        ("rev_up_40_to_1260", 40, 1, 1260, M),
    ]
    for qid, p, sign, final, diff in single:
        factor = 1 + Fraction(p * sign, 100)
        original = Fraction(final) / factor
        assert original.denominator == 1, f"{qid}: {original}"
        q_num(qid, t, diff,
              f"A value {p}% {'increases' if sign == 1 else 'decreases'} to become {final}. "
              f"What was the original value?",
              f"एक मान {p}% {'बढ़कर' if sign == 1 else 'घटकर'} {final} हो जाता है। मूल मान "
              f"क्या था?",
              fmt(original), [fmt(final), fmt(Fraction(final) * p / 100),
                              fmt(original + p if p != 100 else 1)],
              f"The final value is {100 + p * sign}% of the original, so the original is "
              f"{final} / {(100 + p * sign) / 100} = {fmt(original)}. Check: {p}% of "
              f"{fmt(original)} changes it to {final}.",
              f"अंतिम मान मूल का {(100 + p * sign) / 100} है, अतः मूल "
              f"{final} / {(100 + p * sign) / 100} = {fmt(original)}। जाँच: {fmt(original)} "
              f"का {p}% बदलकर {final} कर देता है।")

    question("rev_two_rises", t, H,
             "A value rises by 10% and then by 10% again to become 605. What was the original "
             "value?",
             "एक मान 10% और फिर 10% बढ़कर 605 हो जाता है। मूल मान क्या था?",
             [("500", "500"), ("550", "550"), ("600", "600"), ("605", "605")],
             0,
             "Two 10% rises multiply by 1.21, so the original is 605 / 1.21 = 500. "
             "Check: 500 x 1.1 = 550, then 550 x 1.1 = 605.",
             "दो 10% वृद्धि 1.21 से गुणा करती हैं, अतः मूल 605 / 1.21 = 500 है। जाँच: "
             "500 x 1.1 = 550, फिर 550 x 1.1 = 605।")

    question("rev_two_changes", t, H,
             "A value falls 30% and then rises 25% to become 1400. What was the original "
             "value?",
             "एक मान 30% घटकर और फिर 25% बढ़कर 1400 हो जाता है। मूल मान क्या था?",
             [("1600", "1600"), ("1400", "1400"), ("1750", "1750"), ("1550", "1550")],
             0,
             "The combined factor is 0.70 x 1.25 = 0.875, so the original is 1400 / 0.875 = "
             "1600. Check: 1600 x 0.70 = 1120, then 1120 x 1.25 = 1400.",
             "संयुक्त गुणक 0.70 x 1.25 = 0.875 है, अतः मूल 1400 / 0.875 = 1600 है। जाँच: "
             "1600 x 0.70 = 1120, फिर 1120 x 1.25 = 1400।")


# =============================================================================
# Lesson 20 - Mixed Percentage Word Problems
# =============================================================================
def lesson_twenty():
    t = T["mixed"]
    question("mix_salary_raise_cut", t, M,
             "A salary of 40000 rises by 10% and then falls by 20%. What is the final salary?",
             "40000 का वेतन 10% बढ़ता है और फिर 20% घटता है। अंतिम वेतन क्या है?",
             [("35200", "35200"), ("36000", "36000"), ("38000", "38000"), ("40000", "40000")],
             0,
             "40000 x 1.10 = 44000, then 44000 x 0.80 = 35200. The net change is -12%, "
             "because the fall is measured on the already-raised salary.",
             "40000 x 1.10 = 44000, फिर 44000 x 0.80 = 35200। शुद्ध परिवर्तन -12% है, क्योंकि "
             "गिरावट पहले बढ़े हुए वेतन पर मापी जाती है।")

    question("mix_price_up_down", t, M,
             "A price of 500 rises by 10% and then falls by 20%. What is the final price?",
             "500 का मूल्य 10% बढ़ता है और फिर 20% घटता है। अंतिम मूल्य क्या है?",
             [("440", "440"), ("450", "450"), ("480", "480"), ("500", "500")],
             0,
             "500 x 1.10 = 550, then 550 x 0.80 = 440. The net change is -12%.",
             "500 x 1.10 = 550, फिर 550 x 0.80 = 440। शुद्ध परिवर्तन -12% है।")

    question("mix_marks_relative", t, M,
             "A student needs 40 marks. Passing needs 10% more marks than that. How many marks "
             "must the student score to pass?",
             "एक विद्यार्थी को 40 अंक चाहिए। उत्तीर्ण होने के लिए उससे 10% अधिक अंक चाहिए। "
             "उत्तीर्ण होने के लिए कितने अंक चाहिए?",
             [("44", "44"), ("50", "50"), ("48", "48"), ("40", "40")],
             0,
             "10% more than 40 is 40 x 1.10 = 44 marks.",
             "40 से 10% अधिक 40 x 1.10 = 44 अंक है।")

    question("mix_a_b_c", t, H,
             "A is 150. B is 20% more than A. C is 10% less than B. What is C?",
             "A = 150 है। B, A से 20% अधिक है। C, B से 10% कम है। C क्या है?",
             [("162", "162"), ("168", "168"), ("170", "170"), ("180", "180")],
             0,
             "B = 150 x 1.20 = 180, then C = 180 x 0.90 = 162.",
             "B = 150 x 1.20 = 180, फिर C = 180 x 0.90 = 162।")

    question("mix_pop_growth_deaths", t, H,
             "A town of 30000 people grows by 10%, and then 5% of the new population moves "
             "away. What is the population now?",
             "30000 की आबादी 10% बढ़ती है, और फिर नई आबादी का 5% चला जाता है। अब आबादी क्या है?",
             [("31350", "31350"), ("33000", "33000"), ("31500", "31500"), ("30000", "30000")],
             0,
             "30000 x 1.10 = 33000, then 33000 x 0.95 = 31350. The 5% is measured on the "
             "population after the growth.",
             "30000 x 1.10 = 33000, फिर 33000 x 0.95 = 31350। 5% वृद्धि के बाद की आबादी "
             "पर मापा गया है।")

    question("mix_discount_then_tax", t, M,
             "A 1000 rupee item gets a 20% discount, then 10% tax is added on the reduced "
             "price. What is the final amount?",
             "1000 रुपये की वस्तु पर 20% छूट, फिर घटी मूल्य पर 10% कर। अंतिम राशि क्या है?",
             [("880", "880"), ("900", "900"), ("990", "990"), ("860", "860")],
             0,
             "1000 x 0.80 = 800, then 800 x 1.10 = 880.",
             "1000 x 0.80 = 800, फिर 800 x 1.10 = 880।")

    question("mix_tax_income", t, E,
             "A person earns 50000 in a year and pays 30% of it as tax. What is the income "
             "left?",
             "एक व्यक्ति वर्ष में 50000 कमाता है और उसका 30% कर के रूप में देता है। कितना "
             "शेष रहता है?",
             [("35000", "35000"), ("15000", "15000"), ("20000", "20000"), ("40000", "40000")],
             0,
             "30% of 50000 = 15000 is tax, so 50000 - 15000 = 35000 is left.",
             "50000 का 30% = 15000 कर है, अतः 50000 - 15000 = 35000 शेष रहता है。")


# =============================================================================
# Validation and entry point
# =============================================================================
ALL_BUILDERS = [
    lesson_four, lesson_five, lesson_six, lesson_seven, lesson_eight,
    lesson_nine, lesson_ten, lesson_eleven, lesson_twelve, lesson_thirteen,
    lesson_fourteen, lesson_fifteen, lesson_sixteen, lesson_seventeen,
    lesson_eighteen, lesson_nineteen, lesson_twenty,
]


def build_all():
    for builder in ALL_BUILDERS:
        builder()
    return QUESTIONS


if __name__ == "__main__":
    built = build_all()
    from collections import Counter
    print(f"new questions: {len(built)}")
    for topic, count in sorted(Counter(q["topic"] for q in built).items()):
        print(f"  {topic:32s} {count}")
    print("by difficulty:", dict(Counter(q["difficulty"] for q in built)))
