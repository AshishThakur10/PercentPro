#!/usr/bin/env python3
"""
Decides which examinations each question is written for.

The bank is percentage-only, so what a real exam section actually asks depends on
how much of its Quant section percentage covers. SSC spends far more of its
quantitative aptitude on percentages than IBPS does, where the same section is
mostly simplification, approximation and data interpretation.

Rather than tag every question for every exam, each exam gets a slice of the
bank chosen to match its own section profile: a weighted set of topics, a
difficulty mix, and a cap on how many questions it takes. That is what makes a
paper feel like the exam it is named after, and it is also the honest
description, because the tag says "written for this exam's section" rather than
"taken from this exam".

Questions are shared between exams on purpose. A bank of 157 questions cannot
supply 15 unique questions to seven papers, and the same percentage question is
genuinely asked by more than one of these examinations. What a paper may not
share is a question outside its own section profile.

Assignment is deterministic: the same bank always produces the same tags, so
re-running the generator never reshuffles which paper a question lands in.

Run from the project root:
    python3 tools/emit_questions.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from question_bank_data import QUESTIONS, T, exam_pattern  # noqa: E402

# The exams a paper can be built for. `ssc_mts` is deliberately absent: its
# published pattern could not be confirmed, so ExamCatalog marks it unavailable
# and no question is tagged for it.
ACTIVE_EXAMS = [
    "ssc_cgl", "ssc_chsl", "ssc_gd", "rrb_ntpc", "rrb_group_d", "ibps_po",
    "ibps_clerk",
]

DIFFICULTIES = ["EASY", "MEDIUM", "HARD"]

# Questions in one app paper. The real sections run 20-35 questions, but this
# app only covers percentage, so a paper is a drill through the percentage part
# of a section rather than a replica of it. The exam card says so.
PAPER_SIZE = 15

# ---------------------------------------------------------------------------
# Section profiles
# ---------------------------------------------------------------------------
# `topics` is a weight per short topic key from question_bank_data.T; a topic
# absent from a profile is out of that exam's percentage scope and never tagged.
# `mix` is the share of the exam's questions per difficulty band: the Railway
# and SSC graduate papers lean harder than the banking prelims, which are fast
# and forgiving.

PROFILES = {
    "ssc_cgl": {
        "topics": {
            "pct_of": 3, "whole": 3, "incdec": 3, "succ": 3, "short": 3,
            "comp": 2, "points": 2, "repr": 2, "marks": 2, "price": 2,
            "profit": 2, "salary": 2, "pop": 2, "vote": 2, "reverse": 2,
            "mixed": 2, "dec": 2, "frac": 1, "visual": 1, "what": 1,
        },
        "mix": {"EASY": 0.25, "MEDIUM": 0.45, "HARD": 0.30},
        "cap": 60,
    },
    "ssc_chsl": {
        # Same paper shape as CGL, so the same spread, weighted a little more
        # towards the everyday arithmetic CHSL leans on.
        "topics": {
            "pct_of": 3, "whole": 3, "incdec": 3, "succ": 3, "short": 3,
            "comp": 2, "points": 2, "repr": 2, "marks": 2, "price": 2,
            "profit": 2, "salary": 2, "pop": 2, "vote": 2, "reverse": 2,
            "mixed": 2, "dec": 2, "frac": 1, "visual": 1, "what": 1,
        },
        "mix": {"EASY": 0.30, "MEDIUM": 0.45, "HARD": 0.25},
        "cap": 60,
    },
    "ssc_gd": {
        # Elementary Mathematics. Percentages appear, but beside a lot of plain
        # arithmetic, so the mechanical topics carry more weight and the
        # multi-step ones much less.
        "topics": {
            "what": 3, "visual": 2, "frac": 3, "dec": 3, "pct_of": 3,
            "whole": 2, "incdec": 2, "succ": 1, "comp": 2, "points": 2,
            "repr": 2, "short": 2, "marks": 2, "price": 2, "profit": 2,
            "salary": 2, "pop": 1, "vote": 1, "reverse": 1, "mixed": 1,
        },
        "mix": {"EASY": 0.50, "MEDIUM": 0.40, "HARD": 0.10},
        "cap": 60,
    },
    "rrb_ntpc": {
        # Graduate level: arithmetic-heavy with a real appetite for multi-step
        # questions, so the harder topics are weighted up.
        "topics": {
            "pct_of": 3, "whole": 3, "incdec": 3, "succ": 3, "comp": 3,
            "short": 3, "mixed": 3, "points": 2, "repr": 2, "marks": 2,
            "price": 2, "profit": 2, "salary": 2, "pop": 2, "vote": 2,
            "reverse": 2, "dec": 2, "frac": 1, "visual": 1, "what": 1,
        },
        "mix": {"EASY": 0.20, "MEDIUM": 0.45, "HARD": 0.35},
        "cap": 60,
    },
    "rrb_group_d": {
        # Level 1, much the same trade as SSC GD but slightly less elementary.
        "topics": {
            "what": 2, "visual": 2, "frac": 3, "dec": 3, "pct_of": 3,
            "whole": 3, "incdec": 2, "succ": 2, "comp": 2, "points": 2,
            "repr": 2, "short": 2, "marks": 2, "price": 2, "profit": 2,
            "salary": 2, "pop": 2, "vote": 1, "reverse": 2, "mixed": 1,
        },
        "mix": {"EASY": 0.40, "MEDIUM": 0.45, "HARD": 0.15},
        "cap": 60,
    },
    "ibps_po": {
        # The banking prelims are fast and percentage is a smaller share of the
        # section than for SSC, so this takes the reliable middle of the bank
        # rather than the whole spread.
        "topics": {
            "pct_of": 3, "price": 3, "profit": 3, "salary": 3, "whole": 2,
            "incdec": 2, "succ": 2, "comp": 2, "points": 2, "repr": 2,
            "short": 2, "marks": 2, "reverse": 2, "dec": 1, "frac": 1,
            "mixed": 1, "what": 1, "visual": 1, "pop": 1, "vote": 1,
        },
        "mix": {"EASY": 0.35, "MEDIUM": 0.50, "HARD": 0.15},
        "cap": 50,
    },
    "ibps_clerk": {
        # Clerk numerical ability is the gentlest of the seven and the most
        # percentage-heavy, so it takes the widest and easiest slice.
        "topics": {
            "pct_of": 3, "whole": 3, "incdec": 3, "price": 3, "profit": 3,
            "salary": 3, "what": 2, "visual": 2, "frac": 2, "dec": 2,
            "comp": 2, "points": 2, "repr": 2, "short": 2, "marks": 2,
            "reverse": 2, "pop": 2, "mixed": 1, "succ": 1, "vote": 1,
        },
        "mix": {"EASY": 0.45, "MEDIUM": 0.45, "HARD": 0.10},
        "cap": 50,
    },
}

_TOPIC_KEY_BY_LESSON = {name: short for short, name in T.items()}


def _topic_key_for(lesson_id):
    """Resolves a LessonId enum name back to its short authoring alias."""
    return _TOPIC_KEY_BY_LESSON.get(lesson_id, lesson_id.lower())


def _apportion(mix, total):
    """
    Splits `total` across difficulty bands using the largest-remainder method.

    Rounding the bands independently would make the quotas sum to something
    other than `total`, so the leftover places go to the bands with the biggest
    fractional parts. Ties break by band order, which keeps it deterministic.
    """
    exact = {band: mix.get(band, 0.0) * total for band in DIFFICULTIES}
    quota = {band: int(exact[band]) for band in DIFFICULTIES}
    leftover = total - sum(quota.values())
    order = sorted(
        DIFFICULTIES,
        key=lambda band: (-(exact[band] - quota[band]), DIFFICULTIES.index(band)),
    )
    for band in order[:leftover]:
        quota[band] += 1
    return quota


def _pick_for_exam(exam_id):
    """
    The question ids this exam is tagged for.

    Within each difficulty band the questions are ordered by how central the topic
    is to the exam and then by id, so the most representative questions are taken
    first and the ordering never depends on dictionary iteration order.
    """
    profile = PROFILES[exam_id]
    weights = profile["topics"]
    cap = profile["cap"]

    pools = {}
    for band in DIFFICULTIES:
        band_questions = [q for q in QUESTIONS if q["difficulty"] == band]
        pools[band] = sorted(
            band_questions,
            key=lambda q: (
                -weights.get(_topic_key_for(q["topic"]), 0),
                q["id"],
            ),
        )

    quota = _apportion(profile["mix"], cap)
    picked = []
    taken = {band: 0 for band in DIFFICULTIES}

    # Fill each band's own quota first, then let a short band borrow from the
    # nearest other band rather than leaving a paper unable to start.
    for band in DIFFICULTIES:
        for question in pools[band][:quota[band]]:
            picked.append(question["id"])
            taken[band] += 1

    shortfall = cap - len(picked)
    for offset in (1, 2):
        for band in DIFFICULTIES:
            if shortfall <= 0:
                break
            donor = DIFFICULTIES[
                (DIFFICULTIES.index(band) + offset) % len(DIFFICULTIES)
            ]
            spare = [
                q["id"] for q in pools[donor][taken[donor]:]
            ]
            while spare and shortfall > 0:
                picked.append(spare.pop(0))
                taken[donor] += 1
                shortfall -= 1

    assert len(picked) == cap, f"{exam_id}: got {len(picked)} questions, wanted {cap}"
    return picked


def assign():
    """Returns {question_id: [exam_id, ...]} for the whole bank."""
    if not QUESTIONS:
        return {}
    assignment = {q["id"]: [] for q in QUESTIONS}
    for exam_id in ACTIVE_EXAMS:
        for question_id in _pick_for_exam(exam_id):
            assignment[question_id].append(exam_id)
    return assignment


def verify(assignment):
    """
    Fails the build rather than shipping a paper that cannot be started or that
    covers only one topic and so does not resemble the section.
    """
    if not QUESTIONS:
        return
    for exam_id in ACTIVE_EXAMS:
        ids = [qid for qid, exams in assignment.items() if exam_id in exams]
        if len(ids) < PAPER_SIZE:
            raise AssertionError(
                f"{exam_id}: only {len(ids)} questions tagged, but a paper "
                f"needs {PAPER_SIZE}"
            )
        by_id = {q["id"]: q for q in QUESTIONS}
        topics = {_topic_key_for(by_id[i]["topic"]) for i in ids}
        if len(topics) < 5:
            raise AssertionError(
                f"{exam_id}: only {len(topics)} topics covered; a paper drawn "
                f"from one topic would not resemble the section"
            )


def apply(assignment):
    """Rewrites each question's exam list and builds its provenance tags."""
    for question in QUESTIONS:
        exams = assignment[question["id"]]
        question["exams"] = exams
        question["provenance"] = [exam_pattern(exam_id) for exam_id in exams]