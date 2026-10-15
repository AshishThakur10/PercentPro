#!/usr/bin/env python3
"""
Emits lesson prose into strings.xml (English + Hindi) and LessonContentRepository.kt.

Run from the project root:
    python3 tools/emit_lesson_content.py

Idempotent: the generated region of each file is delimited by marker comments and
replaced wholesale on every run, so re-running after editing lesson_content.py
never leaves duplicate strings behind.
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from lesson_content import (  # noqa: E402
    LESSONS, INTRO, EXPLAIN, FORMULA, EXAMPLE, MISTAKE, CHECK, SUMMARY,
)

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app/src/main/res")
BEGIN = "    <!-- ==== BEGIN GENERATED LESSON PROSE (Phase 6) ==== -->"
END = "    <!-- ==== END GENERATED LESSON PROSE (Phase 6) ==== -->"


def xml_escape(text):
    """
    Escapes text for an Android string resource.

    The apostrophe needs the backslash form: aapt parses string resources with
    C-style escapes and treats a bare `'` as an invalid unicode escape sequence,
    which fails resource compilation.
    """
    return (
        text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("'", "\\'")
    )


def indent(text):
    """Wrap a long string so the generated XML stays readable on a phone-sized diff."""
    return " ".join(text.split())


def has_prose(text):
    """
    True when the string contains an actual word.

    A run of three or more letters. This deliberately ignores single letters, so a
    formula line like "0.45 x 100 = 45" is treated as language-neutral, which it is.
    """
    return re.search(r"[A-Za-z]{3,}", re.sub(r"%\d+\$[sd]", "", text)) is not None


def collect():
    """Returns {string_name: (english, hindi)} plus a Kotlin block spec per lesson."""
    strings = {}
    kotlin = {}

    for lesson in LESSONS:
        key = lesson["key"]
        blocks = []
        used_names = set()

        def put(name, en, hi):
            assert en and en.strip(), f"empty English for {name}"
            assert hi and hi.strip(), f"empty Hindi for {name}"
            # A pure formula or numeric line is legitimately identical in both
            # languages, so only prose is required to differ.
            if has_prose(en):
                assert en.strip() != hi.strip(), f"untranslated copy for {name}"
            assert name not in strings, f"duplicate string name {name}"
            strings[name] = (indent(en), indent(hi))
            return name

        for index, block in enumerate(lesson["blocks"], start=1):
            kind = block[0]
            bid = f"{key}_b{index}"
            assert bid not in used_names, f"duplicate block id {bid}"
            used_names.add(bid)

            if kind == INTRO:
                _, t_en, t_hi, b_en, b_hi = block
                blocks.append(("IntroductionBlock", bid, [
                    put(f"{bid}_title", t_en, t_hi),
                    put(f"{bid}_body", b_en, b_hi),
                ]))
            elif kind == EXPLAIN:
                _, t_en, t_hi, b_en, b_hi, bullets = block
                bullet_names = [
                    put(f"{bid}_b{n}", b[0], b[1]) for n, b in enumerate(bullets, start=1)
                ]
                blocks.append(("ExplanationBlock", bid, [
                    put(f"{bid}_title", t_en, t_hi),
                    put(f"{bid}_body", b_en, b_hi),
                ], bullet_names))
            elif kind == FORMULA:
                _, f_en, f_hi, c_en, c_hi = block
                blocks.append(("FormulaBlock", bid, [
                    put(f"{bid}_formula", f_en, f_hi),
                    put(f"{bid}_caption", c_en, c_hi),
                ]))
            elif kind == EXAMPLE:
                _, t_en, t_hi, steps = block
                title_name = put(f"{bid}_title", t_en, t_hi)
                step_names = []
                for n, (p_en, p_hi, r_en, r_hi) in enumerate(steps, start=1):
                    prompt = put(f"{bid}_s{n}_prompt", p_en, p_hi)
                    result = put(f"{bid}_s{n}_result", r_en, r_hi)
                    step_names.append((prompt, result))
                blocks.append(("WorkedExampleBlock", bid, title_name, step_names))
            elif kind in (MISTAKE, CHECK):
                _, t_en, t_hi, b_en, b_hi = block[:5]
                entries = [put(f"{bid}_title", t_en, t_hi), put(f"{bid}_body", b_en, b_hi)]
                if kind == CHECK:
                    entries.append(put(f"{bid}_answer", block[5], block[6]))
                blocks.append((kind, bid, entries))
            elif kind == SUMMARY:
                _, t_en, t_hi, bullets = block
                bullet_names = [put(f"{bid}_b{n}", b[0], b[1]) for n, b in enumerate(bullets, start=1)]
                blocks.append(("SummaryBlock", bid, put(f"{bid}_title", t_en, t_hi), bullet_names))
            else:
                raise AssertionError(f"unknown block kind {kind}")

        kotlin[key] = blocks

    return strings, kotlin


def render_strings(strings, language_index):
    """
    Emits one locale's copy of the generated strings.

    Both locales use the same resource names: English lives in values/ and Hindi
    in values-hi/, which is how the existing runtime language switch resolves
    them. Emitting a suffixed duplicate into values/ would leave hundreds of
    unused resources behind.
    """
    lines = [BEGIN]
    for name in sorted(strings):
        text = xml_escape(strings[name][language_index])
        # formatted="false": the prose contains literal % signs from formulas,
        # which aapt would otherwise try to read as format specifiers.
        lines.append(f'    <string name="{name}" formatted="false">{text}</string>')
    lines.append(END)
    return "\n".join(lines)


def render_kotlin(lesson_ids, kotlin):
    out = []
    out.append("package com.atlabs.percentpro.content")
    out.append("")
    out.append("import com.atlabs.percentpro.R")
    out.append("import com.atlabs.percentpro.model.LessonId")
    out.append("")
    out.append("/**")
    out.append(" * Body content for every lesson that has been written.")
    out.append(" *")
    out.append(" * Generated from `tools/lesson_content.py` by `tools/emit_lesson_content.py`,")
    out.append(" * which also writes the matching strings. Editing the Python source keeps the")
    out.append(" * block list and the string ids in step; editing this file by hand will be")
    out.append(" * overwritten on the next run.")
    out.append(" *")
    out.append(" * A lesson with no entry here is reported by [blocksFor] as an empty list, and")
    out.append(" * callers treat that as \"not implemented\" rather than opening an empty screen.")
    out.append(" */")
    out.append("class LessonContentRepository {")
    out.append("")
    out.append("    fun blocksFor(lessonId: LessonId): List<LessonBlock> = when (lessonId) {")
    out.append("        LessonId.WHAT_IS_A_PERCENTAGE -> lessonOne()")
    out.append("        LessonId.PERCENTAGES_VISUALLY -> lessonTwo()")
    out.append("        LessonId.FRACTIONS_TO_PERCENTAGES -> lessonThree()")
    for key, lesson_id in lesson_ids:
        out.append(f"        LessonId.{lesson_id} -> {camel(key)}()")
    out.append("        else -> emptyList()")
    out.append("    }")
    out.append("")
    out.append("    fun isImplemented(lessonId: LessonId): Boolean = blocksFor(lessonId).isNotEmpty()")

    for key, _ in lesson_ids:
        out.append("")
        out.append(f"    private fun {camel(key)}(): List<LessonBlock> = listOf(")
        blocks = kotlin[key]
        for n, block in enumerate(blocks):
            out.extend(render_block(block, len(blocks) > 1, n == len(blocks) - 1))
        out.append("    )")

    out.append("}")
    return "\n".join(out)


def render_block(block, has_comma_after, is_last):
    """Renders one already-processed block (see collect()) as Kotlin."""
    kind = block[0]
    bid = block[1]

    if kind == "IntroductionBlock":
        _, _, [title, body] = block
        lines = [f"        IntroductionBlock(", f'            id = "{bid}",',
                 f"            titleRes = R.string.{title},",
                 f"            bodyRes = R.string.{body}", "        )"]

    elif kind == "ExplanationBlock":
        _, _, [title, body], bullets = block
        lines = [f"        ExplanationBlock(", f'            id = "{bid}",',
                 f"            titleRes = R.string.{title},",
                 f"            bodyRes = R.string.{body}"]
        lines = attach_bullets(lines, bullets)
        lines.append("        )")

    elif kind == "FormulaBlock":
        _, _, [formula, caption] = block
        lines = [f"        FormulaBlock(", f'            id = "{bid}",',
                 f"            formulaRes = R.string.{formula},",
                 f"            captionRes = R.string.{caption}", "        )"]

    elif kind == "WorkedExampleBlock":
        _, _, title, steps = block
        lines = [f"        WorkedExampleBlock(", f'            id = "{bid}",',
                 f"            titleRes = R.string.{title},",
                 "            steps = listOf("]
        for p_name, r_name in steps:
            lines.append(f"                ExampleStep(R.string.{p_name}, R.string.{r_name}),")
        lines.append("            )")
        lines.append("        )")

    elif kind == MISTAKE:
        # Rendered as an ExplanationBlock: the lesson engine has no dedicated
        # tip block, and lessons 1-3 already use ExplanationBlock for tips.
        _, _, [title, body] = block
        lines = [f"        ExplanationBlock(", f'            id = "{bid}",',
                 f"            titleRes = R.string.{title},",
                 f"            bodyRes = R.string.{body}", "        )"]

    elif kind == CHECK:
        # The answer is shown as the single bullet under the question, so the
        # knowledge check stays readable without a separate reveal control.
        _, _, [title, body, answer] = block
        lines = [f"        ExplanationBlock(", f'            id = "{bid}",',
                 f"            titleRes = R.string.{title},",
                 f"            bodyRes = R.string.{body},",
                 "            bullets = listOf(",
                 f"                R.string.{answer}", "            )", "        )"]

    elif kind == "SummaryBlock":
        _, _, title, bullets = block
        lines = [f"        SummaryBlock(", f'            id = "{bid}",',
                 f"            titleRes = R.string.{title}"]
        lines = attach_bullets(lines, bullets)
        lines.append("        )")

    else:
        raise AssertionError(f"unknown processed block kind {kind}")

    if not is_last:
        lines[-1] += ","
    return lines


def bullet_list(bullets):
    """
    Renders the `bullets = listOf(...)` argument, without a leading separator.

    The caller terminates the preceding named argument first, so the comma never
    ends up stranded on a line of its own.
    """
    if not bullets:
        return []
    out = ["            bullets = listOf("]
    out += [f"                R.string.{b}," for b in bullets]
    out.append("            )")
    return out


def attach_bullets(lines, bullets):
    """Terminates the last emitted argument and appends the bullets argument."""
    if not bullets:
        return lines
    lines[-1] += ","
    return lines + bullet_list(bullets)


def camel(key):
    return f"lesson{key[1:].upper()}"


LESSON_IDS = [
    ("l04", "DECIMALS_TO_PERCENTAGES"),
    ("l05", "PERCENTAGE_OF_A_NUMBER"),
    ("l06", "ORIGINAL_WHOLE"),
    ("l07", "PERCENTAGE_INCREASE_DECREASE"),
    ("l08", "SUCCESSIVE_PERCENTAGE_CHANGES"),
    ("l09", "PERCENTAGE_COMPARISON"),
    ("l10", "PERCENTAGE_POINTS_VS_CHANGE"),
    ("l11", "COMPARING_REPRESENTATIONS"),
    ("l12", "EXAM_SHORTCUTS"),
]


def patch(path, begin, end, replacement):
    text = open(path, encoding="utf-8").read()
    pattern = re.compile(re.escape(begin) + r".*?" + re.escape(end), re.DOTALL)
    if not pattern.search(text):
        raise SystemExit(f"markers not found in {path}")
    text = pattern.sub(lambda _: replacement, text)
    open(path, "w", encoding="utf-8").write(text)


def main():
    strings, kotlin = collect()

    for locale, index in (("", 0), ("-hi", 1)):
        path = os.path.join(RES, f"values{locale}", "strings.xml")
        replacement = render_strings(strings, index)
        text = open(path, encoding="utf-8").read()
        if BEGIN in text:
            patch(path, BEGIN, END, replacement)
        else:
            text = text.replace("</resources>", replacement + "\n</resources>")
            open(path, "w", encoding="utf-8").write(text)

    target = os.path.join(ROOT, "app/src/main/java/com/atlabs/percentpro/content/LessonContentRepository.kt")
    # Block rendering happens in main(), which splices into the existing file.

    existing = open(target, encoding="utf-8").read()
    m = re.search(
        r"    fun blocksFor\(lessonId: LessonId\): List<LessonBlock> = when \(lessonId\) \{.*?\n    \}\n",
        existing, re.DOTALL)
    if not m:
        raise SystemExit("could not find blocksFor in LessonContentRepository.kt")

    dispatch = []
    dispatch.append("    fun blocksFor(lessonId: LessonId): List<LessonBlock> = when (lessonId) {")
    dispatch.append("        LessonId.WHAT_IS_A_PERCENTAGE -> lessonOne()")
    dispatch.append("        LessonId.PERCENTAGES_VISUALLY -> lessonTwo()")
    dispatch.append("        LessonId.FRACTIONS_TO_PERCENTAGES -> lessonThree()")
    for key, lesson_id in LESSON_IDS:
        dispatch.append(f"        LessonId.{lesson_id} -> {camel(key)}()")
    dispatch.append("        else -> emptyList()")
    dispatch.append("    }")
    new_block = "\n".join(dispatch) + "\n"

    head = existing[: m.start()]
    tail = existing[m.end():]

    # Keep lessonOne/Two/Three verbatim; append the generated lessons after them.
    marker = "\n    private fun lessonThree(): List<LessonBlock> = listOf("
    if marker in tail:
        idx = tail.index(marker)
        kept = tail[:idx]
        tail_rest = tail[idx:]
    else:
        raise SystemExit("could not find lessonThree")

    # Strip the trailing closing brace of lessonThree and re-add the generated body.
    close = tail_rest.rstrip()
    if not close.endswith("}"):
        raise SystemExit("unexpected lessonThree tail")
    tail_rest = close[: close.rfind("}")].rstrip() + "\n"

    # Everything from the first generated lesson onwards is owned by this
    # script, so it is truncated and rewritten on every run. That keeps the
    # generator idempotent: running it twice must not duplicate a function.
    first_generated = f"    private fun {camel(LESSON_IDS[0][0])}(): List<LessonBlock> = listOf("
    if first_generated in tail_rest:
        tail_rest = tail_rest[: tail_rest.index(first_generated)].rstrip() + "\n"

    generated = []
    for key, _ in LESSON_IDS:
        blocks = kotlin[key]
        generated.append("")
        generated.append(f"    private fun {camel(key)}(): List<LessonBlock> = listOf(")
        for n, blk in enumerate(blocks):
            generated.extend(render_block(blk, True, n == len(blocks) - 1))
        generated.append("    )")

    open(target, "w", encoding="utf-8").write(
        head + new_block + kept + tail_rest + "\n" + "\n".join(generated) + "\n}\n"
    )

    print(f"emitted {len(strings)} strings x2 locales, {len(LESSON_IDS)} lessons")


if __name__ == "__main__":
    main()