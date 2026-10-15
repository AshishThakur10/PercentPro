#!/usr/bin/env python3
"""
Authoring source for the PercentPro question bank (Phase 6).

Every question is produced by a generator that *computes* the correct answer
from exact rational arithmetic (fractions.Fraction), then derives the three
distractors from it. Nothing is hand-typed as an answer key, so an arithmetic
mistake becomes a failing assertion in `validate()` rather than a question
shipped to a learner.

emit_questions.py turns this into QuestionBank.kt and appends the new questions
after the existing ones, keeping the original 24 ids and their order untouched.
"""
from fractions import Fraction

QUESTIONS = []


def frac(value):
    return Fraction(value)


def fmt(value, max_dp=4):
    """Renders a Fraction as a clean exam-style string."""
    value = Fraction(value)
    if value.denominator == 1:
        return str(value.numerator)
    # Exact decimal if it terminates within the allowed precision.
    scaled = value * 10 ** max_dp
    if scaled.denominator == 1:
        text = f"{scaled.numerator / 10 ** max_dp:.{max_dp}f}".rstrip("0").rstrip(".")
        return text or "0"
    whole = value.numerator // value.denominator
    remainder = value - whole
    approx = round(float(remainder), 2)
    return f"{whole + approx:g}" if whole else f"{approx:g}"


def num(value):
    """Int when whole, otherwise a 2dp string."""
    value = Fraction(value)
    if value.denominator == 1:
        return str(value.numerator)
    return f"{float(value):.2f}".rstrip("0").rstrip(".")


def question(qid, topic, difficulty, en, hi, options, correct, expl_en, expl_hi):
    """options: list of (en, hi); correct: index."""
    assert len(options) == 4, f"{qid}: needs exactly 4 options"
    assert 0 <= correct <= 3, f"{qid}: correct index out of range"
    en_vals = [o[0] for o in options]
    assert len(set(en_vals)) == 4, f"{qid}: duplicate English options: {en_vals}"
    hi_vals = [o[1] for o in options]
    assert len(set(hi_vals)) == 4, f"{qid}: duplicate Hindi options: {hi_vals}"
    for en_o, hi_o in options:
        assert en_o and hi_o, f"{qid}: empty option"
        assert en_o != hi_o or not any(c.isalpha() for c in en_o), \
            f"{qid}: option looks untranslated: {en_o!r}"
    assert en and hi and expl_en and expl_hi, f"{qid}: empty content"
    QUESTIONS.append({
        "id": qid, "topic": topic, "difficulty": difficulty,
        "en": en, "hi": hi, "options": options,
        "correct": correct, "explanation_en": expl_en, "explanation_hi": expl_hi,
    })


# ---------------------------------------------------------------------------
# Distractor helpers
# ---------------------------------------------------------------------------

def numeric_options(correct_text, distractors, suffix=""):
    """Builds four options from one correct string and three distinct others."""
    values = [correct_text] + [d for d in distractors if d != correct_text]
    assert len(set(values)) >= 4, f"only {len(set(values))} distinct values for {correct_text}"
    values = values[:4]
    return values


def ordered(correct, others):
    """Shuffles deterministically and returns (options, index of correct)."""
    pool = [correct] + [o for o in others if o != correct]
    pool = pool[:4]
    # Deterministic interleave so the answer is not always in the same slot.
    order = [(pool[0], 0)]
    for i, item in enumerate(pool[1:]):
        order.append((item, i % 2))
    order.sort(key=lambda pair: (pair[1], pool.index(pair[0])))
    options = [item for item, _ in order]
    return options, options.index(correct)


E, M, H = "EASY", "MEDIUM", "HARD"

T = {
    "what": "WHAT_IS_A_PERCENTAGE",
    "visual": "PERCENTAGES_VISUALLY",
    "frac": "FRACTIONS_TO_PERCENTAGES",
    "dec": "DECIMALS_TO_PERCENTAGES",
    "pct_of": "PERCENTAGE_OF_A_NUMBER",
    "whole": "ORIGINAL_WHOLE",
    "incdec": "PERCENTAGE_INCREASE_DECREASE",
    "succ": "SUCCESSIVE_PERCENTAGE_CHANGES",
    "comp": "PERCENTAGE_COMPARISON",
    "points": "PERCENTAGE_POINTS_VS_CHANGE",
    "repr": "COMPARING_REPRESENTATIONS",
    "short": "EXAM_SHORTCUTS",
    "pop": "POPULATION_CHANGE",
    "salary": "SALARY_EXPENDITURE_SAVINGS",
    "marks": "MARKS_AND_RESULTS",
    "vote": "ELECTION_VOTING",
    "price": "PRICE_DISCOUNT_TAX",
    "profit": "PROFIT_LOSS",
    "reverse": "REVERSE_PERCENTAGE",
    "mixed": "MIXED_WORD_PROBLEMS",
}