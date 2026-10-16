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
import exam_authoring  # noqa: E402
from question_authoring import build_all  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BANK = os.path.join(ROOT, "app/src/main/java/com/atlabs/percentpro/content/QuestionBank.kt")
TEST = os.path.join(ROOT, "app/src/test/java/com/atlabs/percentpro/content/QuestionBankTest.kt")
BEGIN = "        // ==== BEGIN GENERATED PHASE 6 QUESTIONS ===="
END = "        // ==== END GENERATED PHASE 6 QUESTIONS ==="

DIFFICULTY = {"EASY": "E", "MEDIUM": "M", "HARD": "H"}

# The exams a generated question can be tagged for. Must match ExamCatalog.kt;
# emit_questions.py checks it against the ids found there.
EXAM_IDS = [
    "ssc_cgl", "ssc_chsl", "ssc_gd", "ssc_mts", "rrb_ntpc", "rrb_group_d",
    "ibps_po", "ibps_clerk",
]

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


def render_provenance(tags):
    """
    Renders a question's provenance list as Kotlin.

    Returns an empty string when a question has no tags, so the argument is
    simply left out of the q() call rather than passing an emptyList that would
    need regenerating whenever a tag is added later.
    """
    if not tags:
        return ""
    elements = [
        f'''                    Provenance.ExamPattern(
                        examId = ExamId("{tag["examId"]}"),
                        paper = LocalizedText.of(
                            "{kt(tag["paperEn"])}", "{kt(tag["paperHi"])}"
                        ),
                        section = LocalizedText.of(
                            "{kt(tag["sectionEn"])}", "{kt(tag["sectionHi"])}"
                        )
                    )'''
        for tag in tags
    ]
    return (
        "                provenance = listOf(\n"
        + ",\n".join(elements)
        + "\n                )"
    )


def render(qs):
    out = []
    for q in qs:
        options = ",\n".join(
            f'                    "{kt(en)}" to "{kt(hi)}"'
            for en, hi in q["options"]
        )
        provenance = render_provenance(q.get("provenance", []))
        tail = ",\n" + provenance if provenance else ""
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
                explanationHi = "{kt(q["explanation_hi"])}"{tail}
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


def check_exam_ids_agree_with_catalog():
    """
    Confirms the generator and ExamCatalog.kt know about the same exams.

    A tag naming an exam the app does not define would render a chip with no
    pattern behind it and no paper could be built for it, so this fails the
    build rather than shipping an orphan.
    """
    catalog = os.path.join(
        ROOT,
        "app/src/main/java/com/atlabs/percentpro/content/ExamCatalog.kt",
    )
    text = open(catalog, encoding="utf-8").read()
    in_catalog = set(re.findall(r'ExamId\("([a-z0-9_]+)"\)', text))
    in_generator = set(EXAM_IDS)

    unknown = in_generator - in_catalog
    assert not unknown, (
        f"generator tags questions for exams ExamCatalog.kt does not define: "
        f"{sorted(unknown)}"
    )
    untagged = in_catalog - in_generator
    assert not untagged, (
        f"ExamCatalog.kt defines exams the generator never tags: "
        f"{sorted(untagged)}; add them to exam_authoring.ACTIVE_EXAMS or mark "
        f"them unavailable"
    )


def main():
    questions = build_all()

    ids = [q["id"] for q in questions]
    assert len(ids) == len(set(ids)), "duplicate generated question ids"

    # Tag the bank before rendering, so a profile that cannot fill a paper is
    # caught here rather than showing up as a dead Start button in the app.
    assignment = exam_authoring.assign()
    exam_authoring.verify(assignment)
    exam_authoring.apply(assignment)
    check_exam_ids_agree_with_catalog()

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