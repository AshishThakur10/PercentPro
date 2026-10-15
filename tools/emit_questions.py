#!/usr/bin/env python3
"""
Appends the Phase 6 questions to QuestionBank.kt, leaving the existing 24
untouched, and regenerates the answer-key test data.

Run from the project root:
    python3 tools/emit_questions.py

The generated questions live between marker comments, so re-running after
editing the authoring source replaces them wholesale instead of duplicating.
"""
import os
import re
import sys
from fractions import Fraction

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from question_authoring import build_all  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BANK = os.path.join(ROOT, "app/src/main/java/com/atlabs/percentpro/content/QuestionBank.kt")
TEST = os.path.join(ROOT, "app/src/test/java/com/atlabs/percentpro/content/QuestionBankTest.kt")
BEGIN = "        // ==== BEGIN GENERATED PHASE 6 QUESTIONS ===="
END = "        // ==== END GENERATED PHASE 6 QUESTIONS ==="

DIFFICULTY = {"EASY": "E", "MEDIUM": "M", "HARD": "H"}

TOPIC_CONST = {
    "DECIMALS_TO_PERCENTAGES": "DECIMALS_TO_PERCENTAGES",
    "PERCENTAGE_OF_A_NUMBER": "PERCENTAGE_OF_A_NUMBER",
    "ORIGINAL_WHOLE": "ORIGINAL_WHOLE",
    "PERCENTAGE_INCREASE_DECREASE": "PERCENTAGE_INCREASE_DECREASE",
    "SUCCESSIVE_PERCENTAGE_CHANGES": "SUCCESSIVE_PERCENTAGE_CHANGES",
    "PERCENTAGE_COMPARISON": "PERCENTAGE_COMPARISON",
    "PERCENTAGE_POINTS_VS_CHANGE": "PERCENTAGE_POINTS_VS_CHANGE",
    "COMPARING_REPRESENTATIONS": "COMPARING_REPRESENTATIONS",
    "EXAM_SHORTCUTS": "EXAM_SHORTCUTS",
    "POPULATION_CHANGE": "POPULATION_CHANGE",
    "SALARY_EXPENDITURE_SAVINGS": "SALARY_EXPENDITURE_SAVINGS",
    "MARKS_AND_RESULTS": "MARKS_AND_RESULTS",
    "ELECTION_VOTING": "ELECTION_VOTING",
    "PRICE_DISCOUNT_TAX": "PRICE_DISCOUNT_TAX",
    "PROFIT_LOSS": "PROFIT_LOSS",
    "REVERSE_PERCENTAGE": "REVERSE_PERCENTAGE",
    "MIXED_WORD_PROBLEMS": "MIXED_WORD_PROBLEMS",
}


def kt(text):
    """Escapes a string for a Kotlin literal."""
    return text.replace("\\", "\\\\").replace('"', '\\"')


def render(qs):
    out = []
    for q in qs:
        options = ",\n".join(
            f'                    "{kt(en)}" to "{kt(hi)}"'
            for en, hi in q["options"]
        )
        out.append(f'''            q(
                id = "{q["id"]}",
                topic = {TOPIC_CONST[q["topic"]]},
                difficulty = {DIFFICULTY[q["difficulty"]]},
                en = "{kt(q["en"])}",
                hi = "{kt(q["hi"])}",
                options = listOf(
{options}
                ),
                correctIndex = {q["correct"]},
                explanationEn = "{kt(q["explanation_en"])}",
                explanationHi = "{kt(q["explanation_hi"])}"
            )''')
    return ",\n".join(out)


TOPIC_IDS = [
    "DECIMALS_TO_PERCENTAGES", "PERCENTAGE_OF_A_NUMBER", "ORIGINAL_WHOLE",
    "PERCENTAGE_INCREASE_DECREASE", "SUCCESSIVE_PERCENTAGE_CHANGES",
    "PERCENTAGE_COMPARISON", "PERCENTAGE_POINTS_VS_CHANGE",
    "COMPARING_REPRESENTATIONS", "EXAM_SHORTCUTS", "POPULATION_CHANGE",
    "SALARY_EXPENDITURE_SAVINGS", "MARKS_AND_RESULTS", "ELECTION_VOTING",
    "PRICE_DISCOUNT_TAX", "PROFIT_LOSS", "REVERSE_PERCENTAGE",
    "MIXED_WORD_PROBLEMS",
]


def ensure_constants(bank):
    """
    Adds the HARD difficulty alias and the Phase 6 topic constants.

    They live in the companion object rather than at the call site so the
    generated questions read like the hand-written ones.
    """
    if "private val H = QuestionDifficulty.HARD" not in bank:
        bank = bank.replace(
            "        private val M = QuestionDifficulty.MEDIUM\n",
            "        private val M = QuestionDifficulty.MEDIUM\n"
            "        private val H = QuestionDifficulty.HARD\n",
            1,
        )
    missing = [t for t in TOPIC_IDS if f"val {t} = LessonId.{t}" not in bank]
    if missing:
        block = "".join(f"        val {t} = LessonId.{t}\n" for t in missing)
        bank = bank.replace(
            "        val FRACTIONS_TO_PERCENTAGES = LessonId.FRACTIONS_TO_PERCENTAGES\n",
            "        val FRACTIONS_TO_PERCENTAGES = LessonId.FRACTIONS_TO_PERCENTAGES\n\n"
            "        // Added in Phase 6. These identifiers are new, so nothing saved before\n"
            "        // this phase can refer to them; the three above keep their original keys.\n"
            + block,
            1,
        )
    return bank


def main():
    questions = build_all()

    ids = [q["id"] for q in questions]
    assert len(ids) == len(set(ids)), "duplicate generated question ids"

    bank = ensure_constants(open(BANK, encoding="utf-8").read())
    open(BANK, "w", encoding="utf-8").write(bank)
    # Only the hand-written questions count as "existing": the generated region
    # is rewritten on every run, so its ids would always look like a clash.
    original_region = bank.split(BEGIN)[0] if BEGIN in bank else bank
    existing = set(re.findall(r'id = "([^"]+)"', original_region))
    clash = existing & set(ids)
    assert not clash, f"generated ids collide with existing ones: {sorted(clash)}"

    # The END marker sits inside the list literal, so the final generated
    # element must carry a trailing comma.
    generated = f"{BEGIN}\n" + render(questions) + ",\n" + END

    # The list literal ends with the last element followed by the closing paren.
    # Splice the generated block in just before that closing paren.
    marker = "\n        )\n    }\n}"
    assert bank.rstrip().endswith("}"), "unexpected QuestionBank.kt tail"
    tail_index = bank.rindex(marker)
    head = bank[:tail_index]
    tail = bank[tail_index:]

    head = head.rstrip()

    if BEGIN in head:
        # Re-running: drop the previous generated block first.
        head = re.sub(re.escape(BEGIN) + r".*?" + re.escape(END), "", head, flags=re.DOTALL).rstrip()
        if not head.endswith(","):
            head += ","
    else:
        # First run: the final existing element needs a comma before the new one.
        assert head.endswith(")"), f"expected the last element to close with ')', got: {head[-20:]!r}"
        head += ","

    head += "\n" + generated

    open(BANK, "w", encoding="utf-8").write(head + tail)

    # Regenerate the expected answer key consumed by QuestionBankTest.
    all_ids = re.findall(r'id = "([^"]+)"', open(BANK, encoding="utf-8").read())
    keys = {}
    for q in questions:
        keys[q["id"]] = q["correct"]
    print(f"bank now has {len(all_ids)} questions")
    print(f"generated {len(questions)} (answer key refreshed)")

    keys_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "answer_keys.py")
    with open(keys_path, "w", encoding="utf-8") as handle:
        handle.write('"""Answer key for the generated Phase 6 questions, keyed by question id."""\n')
        handle.write("ANSWER_KEY = {\n")
        for qid in sorted(keys):
            handle.write(f'    "{qid}": {keys[qid]},\n')
        handle.write("}\n")


if __name__ == "__main__":
    main()