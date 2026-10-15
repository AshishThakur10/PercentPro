#!/usr/bin/env python3
"""
Authoring source for PercentPro lesson prose (Phase 6, lessons 4-12).

Content lives here as structured data and is emitted into:
  - app/src/main/res/values/strings.xml        (English)
  - app/src/main/res/values-hi/strings.xml     (Hindi)
  - LessonContentRepository.kt                  (block structure)

Keeping one source means the block list and the strings it references cannot
drift apart: the emitter fails if a block names a string that was not emitted.

Every numeric claim below was checked by hand and is restated in the tests in
LessonContentMathTest, which recomputes each worked example from its own inputs.
"""

# Block kinds
INTRO = "intro"
EXPLAIN = "explain"
FORMULA = "formula"
EXAMPLE = "example"
MISTAKE = "mistake"
CHECK = "check"
SUMMARY = "summary"

LESSONS = []


def lesson(key, blocks):
    LESSONS.append({"key": key, "blocks": blocks})


def intro(t_en, t_hi, b_en, b_hi):
    return (INTRO, t_en, t_hi, b_en, b_hi)


def explain(t_en, t_hi, b_en, b_hi, bullets=None):
    return (EXPLAIN, t_en, t_hi, b_en, b_hi, bullets or [])


def formula(f_en, f_hi, c_en, c_hi):
    return (FORMULA, f_en, f_hi, c_en, c_hi)


def example(t_en, t_hi, steps):
    """steps: list of (prompt_en, prompt_hi, result_en, result_hi)"""
    return (EXAMPLE, t_en, t_hi, steps)


def mistake(t_en, t_hi, b_en, b_hi):
    return (MISTAKE, t_en, t_hi, b_en, b_hi)


def check(t_en, t_hi, b_en, b_hi, answer_en, answer_hi):
    return (CHECK, t_en, t_hi, b_en, b_hi, answer_en, answer_hi)


def summary(t_en, t_hi, bullets):
    return (SUMMARY, t_en, t_hi, bullets)


# =============================================================================
# Lesson 4 - Decimals to Percentages
# =============================================================================
lesson("l04", [
    intro(
        "Turn decimals into percentages",
        "दशमलव को प्रतिशत में बदलें",
        "A percentage is just a decimal scaled by 100. Because 1 = 100/100, converting "
        "between a decimal and a percentage is the same operation as converting between "
        "metres and centimetres: you scale by 100. In this lesson you will learn to move the "
        "decimal point two places to the right, and you will see why decimals larger than 1 "
        "produce percentages larger than 100%.",
        "प्रतिशत केवल 100 से गुणी की गई दशमलव होती है। चूँकि 1 = 100/100, दशमलव को प्रतिशत "
        "में बदलना वही क्रिया है जैसे मीटर को सेंटीमीटर में बदलना: 100 से गुणा करना। इस पाठ में आप "
        "दशमलव बिंदु को दो स्थान दाएँ खिसकाना सीखेंगे, और यह भी जानेंगे कि 1 से बड़े दशमलव से "
        "100% से बड़ा प्रतिशत कैसे बनता है।"
    ),
    explain(
        "The rule: shift two places right",
        "नियम: दो स्थान दाएँ खिसकाएँ",
        "Multiplying by 100 moves the decimal point two places to the right. You can move the "
        "point itself, or you can write out the shifted number. Either way the answer is the "
        "decimal written as a percentage. If there is only one digit after the point, add a "
        "zero so you still shift two places.",
        "100 से गुणा करने पर दशमलव बिंदु दो स्थान दाएँ चला जाता है। आप बिंदु को हिला सकते हैं, या "
        "खिसका हुआ नंबर लिख सकते हैं। दोनों तरीकों से उत्तर वही आता है। यदि बिंदु के बाद केवल "
        "एक अंक हो, तो एक शून्य जोड़ दें ताकि दो स्थान खिसकें।",
        [
            ("0.45  ->  0.45 x 100  ->  45",
             "0.45  ->  0.45 x 100  ->  45"),
            ("0.07  ->  0.07 x 100  ->  7",
             "0.07  ->  0.07 x 100  ->  7"),
            ("0.9  ->  0.9 x 100  ->  90 (one digit, so add a zero)",
             "0.9  ->  0.9 x 100  ->  90 (एक अंक, इसलिए शून्य जोड़ें)"),
            ("0.06  ->  6",
             "0.06  ->  6"),
        ]
    ),
    formula(
        "percentage = decimal x 100",
        "प्रतिशत = दशमलव x 100",
        "Equivalently: move the decimal point two places to the right.",
        "यह समतुल्य रूप से: दशमलव बिंदु को दो स्थान दाएँ खिसकाएँ।"
    ),
    explain(
        "Decimals above 1 give percentages above 100",
        "1 से बड़े दशमलव से 100% से बड़ा प्रतिशत",
        "A percentage above 100% is not an error. It simply means the part is larger than the "
        "whole being measured. A score of 1.25 as a decimal is 125% of a full mark, and a "
        "quantity that grew to 2.5 times its starting size is 250% of what it was. The rule "
        "does not change at all; only the size of the number changes.",
        "100% से बड़ा प्रतिशत कोई त्रुटि नहीं है। इसका अर्थ बस यह है कि भाग, जिसका माप लिया "
        "जा रहा है, उस पूरे से बड़ा है। दशमलव 1.25 अंक पूरे अंक के 125% के बराबर है, और जो "
        "मात्रा अपने शुरुआती आकार की 2.5 गुनी हो गई, वह 250% है। नियम वही रहता है; केवल संख्या "
        "बड़ी होती है।"
    ),
    example(
        "Example: 0.23 as a percentage",
        "उदाहरण: 0.23 को प्रतिशत में",
        [
            ("Write the decimal",
             "दशमलव लिखें",
             "0.23",
             "0.23"),
            ("Multiply by 100",
             "100 से गुणा करें",
             "0.23 x 100 = 23",
             "0.23 x 100 = 23"),
            ("Answer",
             "उत्तर",
             "23%",
             "23%"),
        ]
    ),
    example(
        "Example: 2.5 as a percentage",
        "उदाहरण: 2.5 को प्रतिशत में",
        [
            ("Write the decimal",
             "दशमलव लिखें",
             "2.5",
             "2.5"),
            ("Multiply by 100",
             "100 से गुणा करें",
             "2.5 x 100 = 250",
             "2.5 x 100 = 250"),
            ("Answer",
             "उत्तर",
             "250%, which is 2.5 times the whole",
             "250%, यानी पूरे का 2.5 गुना"),
        ]
    ),
    mistake(
        "Shifting the wrong way",
        "गलत दिशा में खिसकाना",
        "Moving the decimal point to the left divides by 100 and turns 0.45 into 4.5%. That is "
        "the conversion from a percentage to a decimal, done backwards. Remember which "
        "direction you are going: to a percentage, the number gets bigger, so the point moves "
        "right.",
        "दशमलव बिंदु को बाएँ खिसकाना 100 से भाग करता है और 0.45 को 4.5% बना देता है। यह प्रतिशत "
        "से दशमलव में बदलने का काम उल्टी दिशा में करना है। याद रखें कि आप किस दिशा में जा रहे "
        "हैं: प्रतिशत की ओर संख्या बड़ी होती है, इसलिए बिंदु दाएँ जाता है।"
    ),
    check(
        "Knowledge check",
        "ज्ञान परीक्षण",
        "Convert 0.8 into a percentage, then convert 0.035 into a percentage.",
        "0.8 को प्रतिशत में बदलें, फिर 0.035 को प्रतिशत में बदलें।",
        "0.8 x 100 = 80%, and 0.035 x 100 = 3.5%.",
        "0.8 x 100 = 80%, और 0.035 x 100 = 3.5%।"
    ),
    summary(
        "Summary",
        "सारांश",
        [
            ("To convert a decimal to a percentage, multiply by 100, which shifts the point two places right.",
             "दशमलव को प्रतिशत में बदलने के लिए 100 से गुणा करें, जिससे बिंदु दो स्थान दाएँ खिसकता है।"),
            ("Decimals below 1 give percentages below 100%; decimals above 1 give percentages above 100%.",
             "1 से छोटे दशमलव 100% से छोटा प्रतिशत देते हैं; 1 से बड़े दशमलव 100% से बड़ा प्रतिशत देते हैं।"),
            ("If there is only one digit after the point, add a zero before shifting.",
             "यदि बिंदु के बाद केवल एक अंक हो, तो खिसकाने से पहले एक शून्य जोड़ें।"),
        ]
    ),
])

# =============================================================================
# Lesson 5 - Finding a Percentage of a Number
# =============================================================================
lesson("l05", [
    intro(
        "Find a percentage of any number",
        "किसी भी संख्या का प्रतिशत ज्ञात करें",
        "This is the single most-used percentage operation in aptitude questions. The idea is "
        "that a percentage is a fraction of 100, so to take p% of a number you convert p% into "
        "a decimal, multiply, and read the result. You will also learn the 1% method, which "
        "turns many exam questions into one-step mental arithmetic.",
        "यह अभिरुचि प्रश्नों में सबसे अधिक प्रयोग होने वाला प्रतिशत क्रियाविधि है। विचार यह "
        "है कि प्रतिशत 100 का एक भिन्न होता है, इसलिए किसी संख्या का p% निकालने के लिए p% को "
        "दशमलव में बदलें, गुणा करें और उत्तर पढ़ें। आप 1% विधि भी सीखेंगे, जो कई प्रश्नों को "
        "एक-चरणीय मानक हिसाब से बदल देती है।"
    ),
    explain(
        "The three-step method",
        "तीन-चरणीय विधि",
        "Write the percentage as a fraction over 100, multiply by the number, then divide by "
        "100. If you can cancel the 100 against something in the question first, the division "
        "disappears entirely and you are left with a single multiplication.",
        "प्रतिशत को 100 के ऊपर भिन्न के रूप में लिखें, संख्या से गुणा करें, फिर 100 से भाग "
        "दें। यदि आप पहले प्रश्न में उपलब्ध किसी पद से 100 का निरसन कर सकें, तो भाग पूरी "
        "तरह समाप्त हो जाता है और एक ही गुणा शेष रहता है।",
        [
            ("Convert: 15% = 15/100 = 3/20",
             "बदलें: 15% = 15/100 = 3/20"),
            ("Multiply: 200 x 15 = 3000",
             "गुणा करें: 200 x 15 = 3000"),
            ("Divide by 100: 3000 / 100 = 30",
             "100 से भाग दें: 3000 / 100 = 30"),
        ]
    ),
    formula(
        "part = (p / 100) x number",
        "भाग = (p / 100) x संख्या",
        "Equivalently: part = number x p, then move the decimal point two places left.",
        "समतुल्य रूप से: भाग = संख्या x p, फिर दशमलव बिंदु दो स्थान बाएँ खिसकाएँ।"
    ),
    explain(
        "The 1% method",
        "1% विधि",
        "One percent is simply one hundredth, so 1% of a number is that number with its "
        "decimal point shifted two places left. Once you know 1%, any whole-number percentage is "
        "just that multiplied: 6% is six times 1%. This is the fastest route when the "
        "percentage is a small whole number, which is exactly what aptitude papers tend to use.",
        "एक प्रतिशत एक सौवाँ भाग होता है, इसलिए किसी संख्या का 1% वही संख्या होता है जिसका "
        "दशमलव बिंदु दो स्थान बाएँ खिसका हो। 1% जान लेने पर कोई भी पूर्णांक प्रतिशत उसका "
        "गुणा है: 6% यानी 1% का छह गुना। यह तब सबसे तेज़ तरीका है जब प्रतिशत छोटा पूर्णांक हो, "
        "और अभिरुचि प्रश्न-पत्र प्रायः ऐसा ही देते हैं।"
    ),
    example(
        "Example: 15% of 200",
        "उदाहरण: 200 का 15%",
        [
            ("Convert the percentage",
             "प्रतिशत बदलें",
             "15% = 0.15",
             "15% = 0.15"),
            ("Multiply by the number",
             "संख्या से गुणा करें",
             "200 x 0.15",
             "30"),
            ("Answer",
             "उत्तर",
             "30",
             "30"),
        ]
    ),
    example(
        "Example: 6% of 4500, using 1%",
        "उदाहरण: 4500 का 6%, 1% विधि से",
        [
            ("Find 1%",
             "1% निकालें",
             "4500 x 0.01",
             "45"),
            ("Multiply by 6",
             "6 से गुणा करें",
             "45 x 6",
             "270"),
            ("Answer",
             "उत्तर",
             "270",
             "270"),
        ]
    ),
    example(
        "Example: 7.5% of 400",
        "उदाहरण: 400 का 7.5%",
        [
            ("Convert the percentage",
             "प्रतिशत बदलें",
             "7.5% = 0.075",
             "7.5% = 0.075"),
            ("Multiply by the number",
             "संख्या से गुणा करें",
             "400 x 0.075",
             "30"),
            ("Answer",
             "उत्तर",
             "30",
             "7.5% is exactly one fifteenth of 400"),
        ]
    ),
    mistake(
        "Taking the percentage of the wrong number",
        "गलत संख्या का प्रतिशत लेना",
        "15% of 200 is 30, not 150. The number 150 is 75% of 200, because it is three quarters "
        "of it. Always check that your answer is smaller than the original number when the "
        "percentage is below 100%; if it is larger, you have multiplied instead of found the "
        "part.",
        "200 का 15% 30 है, 150 नहीं। संख्या 150, 200 का 75% है, क्योंकि वह उसका तीन चौथाई "
        "है। जब प्रतिशत 100% से कम हो तो हमेशा जाँचें कि आपका उत्तर मूल संख्या से छोटा है; यदि "
        "वह बड़ा है, तो आपने भाग निकालने के बजाय गुणा कर दिया है।"
    ),
    check(
        "Knowledge check",
        "ज्ञान परीक्षण",
        "Find 12% of 350 using the 1% method.",
        "1% विधि से 350 का 12% निकालें।",
        "1% of 350 = 3.5, so 12% = 3.5 x 12 = 42.",
        "350 का 1% = 3.5, इसलिए 12% = 3.5 x 12 = 42।"
    ),
    summary(
        "Summary",
        "सारांश",
        [
            ("part = (p / 100) x number. Convert first, then multiply.",
             "भाग = (p / 100) x संख्या। पहले बदलें, फिर गुणा करें।"),
            ("1% of a number is that number shifted two places left; then multiply by the percentage.",
             "किसी संख्या का 1% वही संख्या दो स्थान बाएँ खिसककर है; फिर प्रतिशत से गुणा करें।"),
            ("A percentage below 100% must give an answer below the original number.",
             "100% से कम प्रतिशत का उत्तर मूल संख्या से कम होना चाहिए।"),
        ]
    ),
])

# =============================================================================
# Lesson 6 - Finding the Original Whole
# =============================================================================
lesson("l06", [
    intro(
        "Work backwards to the original value",
        "मूल मान तक पीछे जाएँ",
        "When you know a part and the percentage it represents, you are being asked for the "
        "whole. The operation is the exact inverse of the previous lesson: divide by the "
        "percentage instead of multiplying. This is the operation behind every 'is what "
        "percent of' and every 'what was the original' question.",
        "जब आपको एक भाग और वह प्रतिशत ज्ञात हो, तो आपसे पूछा जा रहा है कि पूरा क्या था। यह "
        "क्रिया पिछले पाठ की ठीक विपरीत है: गुणा करने के बजाय प्रतिशत से भाग दें। हर 'किसका "
        "कितने प्रतिशत' और हर 'मूल क्या था' प्रश्न के पीछे यही क्रिया होती है।"
    ),
    explain(
        "The method, and the check that proves it",
        "विधि, और उसे सिद्ध करने वाली जाँच",
        "Turn the percentage into a decimal, then divide the part by that decimal. Because "
        "multiplication and division undo each other, you can always verify the answer by "
        "multiplying it back by the same percentage. If the product is not the original part, "
        "the arithmetic has gone wrong somewhere.",
        "प्रतिशत को दशमलव में बदलें, फिर भाग को उस दशमलव से भाग दें। चूँकि गुणा और भाग एक "
        "दूसरे को पूरा करते हैं, आप उत्तर को उसी प्रतिशत से वापस गुणा करके हमेशा जाँच सकते हैं। "
        "यदि गुणनफल मूल भाग नहीं है, तो कहीं हिसाब गड़बड़ है।"
    ),
    formula(
        "whole = part / (p / 100)",
        "पूरा = भाग / (p / 100)",
        "Check it by multiplying the whole by p% and confirming you recover the part.",
        "पूरे को p% से गुणा करके जाँचें कि भाग वापस मिलता है।"
    ),
    example(
        "Example: 30 is 15% of what?",
        "उदाहरण: 30, किसका 15% है?",
        [
            ("Convert the percentage",
             "प्रतिशत बदलें",
             "15% = 0.15",
             "15% = 0.15"),
            ("Divide the part by it",
             "भाग को इससे भाग दें",
             "30 / 0.15",
             "200"),
            ("Check by multiplying back",
             "वापस गुणा करके जाँचें",
             "200 x 0.15",
             "30, which matches"),
        ]
    ),
    example(
        "Example: 45 is 25% of what?",
        "उदाहरण: 45, किसका 25% है?",
        [
            ("Convert the percentage",
             "प्रतिशत बदलें",
             "25% = 0.25",
             "25% = 0.25"),
            ("Divide the part by it",
             "भाग को इससे भाग दें",
             "45 / 0.25",
             "180"),
            ("Check by multiplying back",
             "वापस गुणा करके जाँचें",
             "180 x 0.25",
             "45, which matches"),
        ]
    ),
    example(
        "Example: cancelling before dividing",
        "उदाहरण: भाग से पहले निरसन",
        [
            ("Rewrite the division as a fraction",
             "भाग को भिन्न के रूप में लिखें",
             "45 / (25/100)",
             "45 x 100 / 25"),
            ("Cancel",
             "निरसन करें",
             "100 / 25",
             "4"),
            ("Answer",
             "उत्तर",
             "45 x 4",
             "180, with no decimal needed"),
        ]
    ),
    mistake(
        "Multiplying instead of dividing",
        "गुणा करना, भाग न करना",
        "If 30 is 15% of the whole, the whole must be larger than 30, because 30 is only a "
        "part of it. Any method that produces a number smaller than the given part is wrong. "
        "When the percentage is below 100%, dividing always moves you upward.",
        "यदि 30, पूरे का 15% है, तो पूरा 30 से बड़ा होना चाहिए, क्योंकि 30 उसका केवल एक भाग "
        "है। कोई भी विधि जो दिए गए भाग से छोटी संख्या दे, वह गलत है। जब प्रतिशत 100% से कम हो, "
        "तो भाग करना हमेशा आपको ऊपर ले जाता है।"
    ),
    check(
        "Knowledge check",
        "ज्ञान परीक्षण",
        "84 is 12% of what number?",
        "84 किस संख्या का 12% है?",
        "84 / 0.12 = 700. Check: 700 x 0.12 = 84.",
        "84 / 0.12 = 700। जाँच: 700 x 0.12 = 84।"
    ),
    summary(
        "Summary",
        "सारांश",
        [
            ("whole = part / (p / 100). This is the inverse of finding a percentage of a number.",
             "पूरा = भाग / (p / 100)। यह संख्या का प्रतिशत निकालने की विपरीत क्रिया है।"),
            ("Always multiply back to check the answer.",
             "उत्तर जाँचने के लिए हमेशा वापस गुणा करें।"),
            ("Cancelling 100 against a factor of p turns the division into a multiplication.",
             "p के किसी गुणनखंड के साथ 100 का निरसन करने से भाग एक गुणा बन जाता है।"),
        ]
    ),
])

# =============================================================================
# Lesson 7 - Percentage Increase and Decrease
# =============================================================================
lesson("l07", [
    intro(
        "Increase and decrease, and which base to use",
        "वृद्धि और ह्रास, और किस आधार का उपयोग करें",
        "When a price, a salary or a population rises by a percentage, the percentage is always "
        "taken of the starting value. That single rule causes most of the errors in this "
        "topic. You will also learn why a 50% fall needs more than a 50% rise to recover, "
        "which is the idea that examiners return to again and again.",
        "जब कोई मूल्य, वेतन या जनसंख्या प्रतिशत में बढ़ती है, तो प्रतिशत हमेशा शुरुआती मान "
        "का होता है। इसी एक नियम से इस विषय की अधिकांश त्रुटियाँ होती हैं। आप यह भी सीखेंगे कि "
        "50% की कमी की भरपाई के लिए 50% से अधिक वृद्धि क्यों चाहिए, और यही विचार परीक्षक बार-बार "
        "पूछते हैं।"
    ),
    explain(
        "Always take the percentage of the original",
        "प्रतिशत हमेशा मूल का लें",
        "The percentage describes the change, and the change is measured against where the "
        "quantity started. So to find how much a 20% rise on 250 adds, calculate 20% of 250 and "
        "add that to 250. Calculating 20% of the new value instead would give a different, "
        "incorrect answer.",
        "प्रतिशत परिवर्तन का वर्णन करता है, और परिवर्तन मापा जाता है उस मात्रा के शुरुआती "
        "मूल्य के सापेक्ष। इसलिए 250 पर 20% वृद्धि में जो जुड़ेगा, वह 250 का 20% निकालकर 250 "
        "में जोड़ना है। नए मान का 20% निकालने से अलग और गलत उत्तर आएगा।"
    ),
    formula(
        "change = value x (p / 100)      new value = value x (1 + p / 100)",
        "परिवर्तन = मान x (p / 100)      नया मान = मान x (1 + p / 100)",
        "Use 1 - p/100 for a decrease.",
        "ह्रास के लिए 1 - p/100 का प्रयोग करें।"
    ),
    example(
        "Example: 250 increased by 20%",
        "उदाहरण: 250 में 20% वृद्धि",
        [
            ("Find the change: 20% of 250",
             "परिवर्तन निकालें: 250 का 20%",
             "250 x 0.20",
             "50"),
            ("Add it to the original",
             "इसे मूल में जोड़ें",
             "250 + 50",
             "300"),
            ("Check with the multiplier",
             "गुणक से जाँचें",
             "250 x 1.20",
             "300"),
        ]
    ),
    example(
        "Example: 80 decreased by 15%",
        "उदाहरण: 80 में 15% की कमी",
        [
            ("Find the decrease: 15% of 80",
             "कमी निकालें: 80 का 15%",
             "80 x 0.15",
             "12"),
            ("Subtract it from the original",
             "इसे मूल से घटाएँ",
             "80 - 12",
             "68"),
            ("Check with the multiplier",
             "गुणक से जाँचें",
             "80 x 0.85",
             "68"),
        ]
    ),
    explain(
        "Why falls and rises are not symmetric",
        "गिरावट और वृद्धि सममित क्यों नहीं हैं",
        "After a 50% fall from 200 you are at 100, which is half the original. To return to 200 "
        "you must now gain 100 on a base of 100, which is a 100% rise, not 50%. The loss and "
        "the recovery are measured against different bases, so they can never cancel unless "
        "both are zero.",
        "200 से 50% गिरने पर आप 100 पर पहुँचते हैं, जो मूल का आधा है। 200 पर लौटने के लिए अब "
        "100 के आधार पर 100 बढ़ाना होगा, यानी 100% वृद्धि, 50% नहीं। हानि और भरपाई अलग-अलग "
        "आधार पर मापी जाती हैं, इसलिए दोनों शून्य होने पर ही वे रद्द हो सकती हैं।"
    ),
    example(
        "Example: recovering from a fall",
        "उदाहरण: गिरावट से उबरना",
        [
            ("Start at 200 and fall 50%",
             "200 से शुरू करें और 50% गिरावट लगाएँ",
             "200 x 0.5",
             "100"),
            ("The recovery is measured on the new base",
             "भरपाई नए आधार पर मापी जाती है",
             "100 / 100",
             "+100 to return to 200"),
            ("Required rise",
             "आवश्यक वृद्धि",
             "100 / 100 x 100",
             "100%, not 50%"),
        ]
    ),
    mistake(
        "Applying the percentage to the new value",
        "नए मान पर प्रतिशत लगाना",
        "For a 20% rise on 250, working out 20% of 300 gives 60 and an answer of 310. That is "
        "wrong because 300 is the value you are trying to find, not the one you started from. "
        "Whenever the question gives you the starting value and the percentage, the starting "
        "value is the base.",
        "250 पर 20% वृद्धि में, 300 का 20% निकालने पर 60 मिलता है और उत्तर 310। यह गलत है "
        "क्योंकि 300 वही मान है जिसे आप खोज रहे हैं, वह नहीं जिससे आपने शुरू किया। जब प्रश्न "
        "शुरुआती मान और प्रतिशत दोनों दे, तो आधार शुरुआती मान ही होता है।"
    ),
    check(
        "Knowledge check",
        "ज्ञान परीक्षण",
        "A quantity falls from 400 by 25%. What is the new value, and what rise on the new "
        "value would bring it back to 400?",
        "एक मात्रा 400 से 25% घटती है। नया मान क्या है, और नए मान पर कितनी वृद्धि उसे 400 "
        "पर लौटा देगी?",
        "400 - 100 = 300. To recover, 100 / 300 x 100 = 33.33% (about 33.33%, one third).",
        "400 - 100 = 300। वापस पाने के लिए 100 / 300 x 100 = 33.33% (लगभग 33.33%, यानी एक तिहाई)।"
    ),
    summary(
        "Summary",
        "सारांश",
        [
            ("The percentage is always calculated on the starting value.",
             "प्रतिशत हमेशा शुरुआती मान पर ही निकाला जाता है।"),
            ("new value = value x (1 + p/100) for a rise, value x (1 - p/100) for a fall.",
             "वृद्धि के लिए नया मान = मान x (1 + p/100), कमी के लिए मान x (1 - p/100)।"),
            ("A fall and the rise needed to undo it are measured on different bases, so they are not equal.",
             "गिरावट और उसे पूरा करने वाली वृद्धि अलग आधार पर मापी जाती हैं, इसलिए बराबर नहीं होतीं।"),
        ]
    ),
])

# =============================================================================
# Lesson 8 - Successive Percentage Changes
# =============================================================================
lesson("l08", [
    intro(
        "Two changes in a row",
        "लगातार दो परिवर्तन",
        "When a price rises 10% and then rises 20%, the second change is not applied to the "
        "original price; it is applied to the price after the first change. This lesson gives "
        "you a formula that produces the net effect without doing the two-step arithmetic, "
        "which is what makes these questions quick rather than laborious.",
        "जब कोई मूल्य 10% बढ़ता है और फिर 20% बढ़ता है, तो दूसरा परिवर्तन मूल मूल्य पर नहीं, "
        "पहले परिवर्तन के बाद के मूल्य पर लागू होता है। इस पाठ में एक सूत्र दिया गया है जो दो-चरणीय "
        "गणना किए बिना शुद्ध प्रभाव बता देता है, और इसी से ये प्रश्न तेज़ होते हैं।"
    ),
    explain(
        "Build the multiplier",
        "गुणक बनाएँ",
        "Each change multiplies the current value by a number slightly above or below 1. A "
        "10% rise multiplies by 1.10, a 20% fall multiplies by 0.80. Chaining the multipliers "
        "gives the overall multiplier, and subtracting 1 from it gives the net percentage "
        "change.",
        "हर परिवर्तन मौजूदा मान को 1 से थोड़ा ऊपर या नीचे की संख्या से गुणा करता है। 10% वृद्धि "
        "1.10 से गुणा करती है, 20% कमी 0.80 से। गुणकों को जोड़ने से कुल गुणक मिलता है, और उसमें "
        "से 1 घटाने पर शुद्ध प्रतिशत परिवर्तन मिलता है।"
    ),
    formula(
        "net % = (multiplier1 x multiplier2 - 1) x 100",
        "शुद्ध % = (गुणक1 x गुणक2 - 1) x 100",
        "For a rise of a% then a fall of b%: net % = a - b - (a x b / 100).",
        "a% वृद्धि के बाद b% कमी के लिए: शुद्ध % = a - b - (a x b / 100)।"
    ),
    example(
        "Example: +10% then +20%",
        "उदाहरण: +10% फिर +20%",
        [
            ("Step 1: increase 100 by 10%",
             "चरण 1: 100 में 10% वृद्धि",
             "100 x 1.10",
             "110"),
            ("Step 2: increase the result by 20%",
             "चरण 2: परिणाम में 20% वृद्धि",
             "110 x 1.20",
             "132"),
            ("Net change",
             "शुद्ध परिवर्तन",
             "132 - 100",
             "+32%"),
            ("Check with the formula",
             "सूत्र से जाँचें",
             "(1.10 x 1.20) - 1",
             "0.32, so +32%"),
        ]
    ),
    example(
        "Example: +10% then -10%",
        "उदाहरण: +10% फिर -10%",
        [
            ("Step 1: increase 100 by 10%",
             "चरण 1: 100 में 10% वृद्धि",
             "100 x 1.10",
             "110"),
            ("Step 2: decrease 110 by 10%",
             "चरण 2: 110 में 10% की कमी",
             "110 x 0.10",
             "11, so 110 - 11 = 99"),
            ("Net change",
             "शुद्ध परिवर्तन",
             "99 - 100",
             "-1, so -1%"),
            ("Check with the formula",
             "सूत्र से जाँचें",
             "10 - 10 - (10 x 10 / 100)",
             "-1"),
        ]
    ),
    explain(
        "The form to memorise",
        "याद रखने योग्य रूप",
        "A rise of a% followed by a fall of b% gives a net change of a - b - ab/100. The "
        "correction term is always subtracted, because the fall was measured on a base that the "
        "rise had already enlarged. When a and b are equal, the net is always negative, however "
        "large the percentage, which is a useful sanity check.",
        "a% वृद्धि के बाद b% कमी से शुद्ध परिवर्तन a - b - ab/100 आता है। संशोधन पद हमेशा "
        "घटाया जाता है, क्योंकि कमी उस आधार पर मापी गई थी जिसे वृद्धि ने पहले ही बढ़ा दिया था। "
        "जब a और b बराबर हों, तो शुद्ध परिवर्तन हमेशा ऋणात्मक होता है, चाहे प्रतिशत कितना भी "
        "बड़ा हो, और यह एक उपयोगी जाँच है।"
    ),
    example(
        "Example: +50% then -50%",
        "उदाहरण: +50% फिर -50%",
        [
            ("Step 1: increase 200 by 50%",
             "चरण 1: 200 में 50% वृद्धि",
             "200 x 1.5",
             "300"),
            ("Step 2: decrease the result by 50%",
             "चरण 2: परिणाम में 50% कमी",
             "300 x 0.5",
             "150"),
            ("Net change",
             "शुद्ध परिवर्तन",
             "150 - 200",
             "-50, so -25%"),
            ("Check with the formula",
             "सूत्र से जाँचें",
             "50 - 50 - (50 x 50 / 100)",
             "-25"),
        ]
    ),
    mistake(
        "Adding the two percentages",
        "दोनों प्रतिशत जोड़ देना",
        "Adding 10% and 20% gives 30%, but the correct net is 32%. The extra 2% appears "
        "because the 20% was applied to 110 rather than to 100, so it carries a little of the "
        "first rise with it. Adding is wrong in every case except when one of the changes is "
        "zero.",
        "10% और 20% जोड़ने पर 30% आता है, जबकि सही शुद्ध मान 32% है। अतिरिक्त 2% इसलिए आता है "
        "क्योंकि 20% 100 के बजाय 110 पर लगा, इसलिए वह पहली वृद्धि का कुछ भाग भी साथ ले गया। "
        "एक परिवर्तन शून्य होने को छोड़कर, हर स्थिति में जोड़ना गलत है।"
    ),
    check(
        "Knowledge check",
        "ज्ञान परीक्षण",
        "A value of 500 falls by 20% and then rises by 25%. What is the final value, and "
        "what is the net change?",
        "500 का मान 20% घटता है और फिर 25% बढ़ता है। अंतिम मान क्या है, और शुद्ध परिवर्तन क्या?",
        "500 x 0.80 = 400, then 400 x 1.25 = 500. Net change 0%, because 25 - 20 - "
        "(20 x 25 / 100) = 0.",
        "500 x 0.80 = 400, फिर 400 x 1.25 = 500। शुद्ध परिवर्तन 0%, क्योंकि 25 - 20 - "
        "(20 x 25 / 100) = 0।"
    ),
    summary(
        "Summary",
        "सारांश",
        [
            ("Each change multiplies the current value, not the original one.",
             "हर परिवर्तन मौजूदा मान को गुणा करता है, मूल को नहीं।"),
            ("Chain the multipliers, then subtract 1 to get the net decimal change.",
             "गुणकों को जोड़ें, फिर 1 घटाकर शुद्ध दशमलव परिवर्तन पाएँ।"),
            ("A rise of a% then a fall of b% gives a net of a - b - ab/100.",
             "a% वृद्धि के बाद b% कमी से शुद्ध मान a - b - ab/100 आता है।"),
        ]
    ),
])

# =============================================================================
# Lesson 9 - Percentage Comparison Problems
# =============================================================================
lesson("l09", [
    intro(
        "When is one quantity what percent more than another?",
        "एक मात्रा दूसरी से कितने प्रतिशत अधिक है?",
        "Comparison questions usually ask for a ratio, not a raw number: if A is 25% more than "
        "B, what is A to B? The method is to put both quantities on the same footing of 100 "
        "and read off the ratio. Once you can do that, chained comparisons across three or "
        "four quantities become routine.",
        "तुलना प्रश्न आम तौर पर कच्ची संख्या नहीं, अनुपात माँगते हैं: यदि A, B से 25% अधिक है, "
        "तो A : B क्या है? विधि यह है कि दोनों मात्राओं को 100 के एक ही आधार पर रखें और अनुपात "
        "पढ़ लें। यह आ जाने पर तीन या चार मात्राओं तक की शृंखलाबद्ध तुलना भी सामान्य हो जाती है।"
    ),
    explain(
        "Put both on a scale of 100",
        "दोनों को 100 के पैमाने पर रखें",
        "If B is taken as 100, then A being 25% more than B must be 125. The ratio is simply "
        "125 to 100. If A is 20% less than B, then with B as 100, A is 80, giving 80 to 100. "
        "Always make the quantity that the percentage is measured from equal to 100.",
        "यदि B को 100 मान लिया जाए, तो B से 25% अधिक A 125 होगा। अनुपात सीधे 125 : 100 है। "
        "यदि A, B से 20% कम है, तो B को 100 मानने पर A 80 होगा, जो 80 : 100 देता है। हमेशा उस "
        "मात्रा को 100 बनाएँ जिसके सापेक्ष प्रतिशत दिया गया है।"
    ),
    formula(
        "If A is p% more than B, then A : B = (100 + p) : 100",
        "यदि A, B से p% अधिक है, तो A : B = (100 + p) : 100",
        "For p% less, use (100 - p) : 100.",
        "p% कम के लिए (100 - p) : 100 का प्रयोग करें।"
    ),
    example(
        "Example: A is 25% more than B",
        "उदाहरण: A, B से 25% अधिक है",
        [
            ("Take B as 100",
             "B को 100 मानें",
             "B",
             "100"),
            ("A is 25 more than B",
             "A, B से 25 अधिक है",
             "125 - 100",
             "25, so A = 125"),
            ("Write the ratio",
             "अनुपात लिखें",
             "125 : 100",
             "5 : 4"),
        ]
    ),
    example(
        "Example: A is 20% less than B",
        "उदाहरण: A, B से 20% कम है",
        [
            ("Take B as 100",
             "B को 100 मानें",
             "B",
             "100"),
            ("A is 20 less than B",
             "A, B से 20 कम है",
             "100 - 20",
             "80, so A = 80"),
            ("Write the ratio",
             "अनुपात लिखें",
             "80 : 100",
             "4 : 5"),
        ]
    ),
    explain(
        "Chaining two comparisons",
        "दो तुलनाओं को जोड़ना",
        "When B is itself a percentage of C, express everything in terms of C and the chain "
        "collapses into one multiplication. Take C as 100, build B from it, then build A from "
        "B. The result tells you A against C directly, which is usually what the question asks.",
        "जब B स्वयं C का कोई प्रतिशत है, तो सब कुछ C के रूप में व्यक्त करें और शृंखला एक ही "
        "गुणा में समा जाती है। C को 100 मानें, उससे B बनाएँ, फिर B से A बनाएँ। परिणाम "
        "सीधे A और C का संबंध बताता है, जो प्रायः ही प्रश्न माँगता है।"
    ),
    example(
        "Example: chaining through B",
        "उदाहरण: B से होकर शृंखला",
        [
            ("A is 50% more than B, and B is 20% less than C. Take C as 100.",
             "A, B से 50% अधिक है, और B, C से 20% कम है। C को 100 मानें।",
             "C",
             "100"),
            ("Build B from C",
             "C से B बनाएँ",
             "100 x 0.80",
             "80"),
            ("Build A from B",
             "B से A बनाएँ",
             "80 x 1.50",
             "120"),
            ("Compare A with C",
             "A की C से तुलना",
             "120 - 100",
             "20, so A is 20% more than C"),
        ]
    ),
    mistake(
        "Reversing the ratio",
        "अनुपात उलट देना",
        "If A is 25% more than B, then A is the larger quantity and must be the larger term of "
        "the ratio. Writing A : B = 4 : 5 would claim that A is the smaller one, which "
        "contradicts the question. As a quick check, the first term should feel bigger "
        "whenever the percentage is positive.",
        "यदि A, B से 25% अधिक है, तो A बड़ी मात्रा है और अनुपात में बड़ा पद होना चाहिए। "
        "A : B = 4 : 5 लिखना यह दावा करेगा कि A छोटी है, जो प्रश्न के विपरीत है। त्वरित जाँच: "
        "प्रतिशत धनात्मक होने पर पहला पद बड़ा महसूस होना चाहिए।"
    ),
    check(
        "Knowledge check",
        "ज्ञान परीक्षण",
        "If A is 10% more than B, write A : B in lowest whole-number terms.",
        "यदि A, B से 10% अधिक है, तो A : B को न्यूनतम पूर्णांक पदों में लिखें।",
        "A : B = 110 : 100 = 11 : 10.",
        "A : B = 110 : 100 = 11 : 10।"
    ),
    summary(
        "Summary",
        "सारांश",
        [
            ("If A is p% more than B, then A : B = (100 + p) : 100.",
             "यदि A, B से p% अधिक है, तो A : B = (100 + p) : 100।"),
            ("For p% less, the ratio is (100 - p) : 100.",
             "p% कम के लिए अनुपात (100 - p) : 100 है।"),
            ("Chain comparisons by expressing everything against the common quantity and multiplying the factors.",
             "तुलनाओं को उभयनिष्ठ मात्रा के सापेक्ष व्यक्त करके और गुणनखंडों को गुणा करके शृंखलाबद्ध करें।"),
        ]
    ),
])

# =============================================================================
# Lesson 10 (order) - Percentage Points vs Percentage Change
# =============================================================================
lesson("l10", [
    intro(
        "Percentage points and percentage change are different",
        "प्रतिशत अंक और प्रतिशत परिवर्तन अलग हैं",
        "News reports and exam papers both talk about percentages, but two different "
        "calculations hide behind the word. A change in percentage points is a plain "
        "subtraction. A percentage change is a relative shift measured against the starting "
        "value. Mixing them up is one of the most common traps in this subject, so this lesson "
        "makes the distinction explicit.",
        "समाचार और परीक्षा दोनों प्रतिशत की बात करते हैं, पर इस शब्द के पीछे दो अलग गणनाएँ "
        "हैं। प्रतिशत अंकों में परिवर्तन एक सादा घटाव है। प्रतिशत परिवर्तन एक सापेक्ष बदलाव है "
        "जो शुरुआती मान पर मापा जाता है। इन दोनों को मिलाना इस विषय की सबसे आम चुनौतियों में "
        "से एक है, इसलिए यह पाठ इस भेद को स्पष्ट करता है।"
    ),
    explain(
        "Two different measurements",
        "दो अलग माप",
        "A percentage point is an absolute unit. Going from 20% to 25% is a rise of 5 "
        "percentage points, full stop. A percentage change asks by what proportion of the "
        "original the value moved: from 20% to 25% is a rise of 5 out of 20, which is 25% "
        "relative. The base for a percentage change is the earlier value; there is no base at "
        "all in a percentage-point difference.",
        "प्रतिशत अंक एक निरपेक्ष इकाई है। 20% से 25% जाना 5 प्रतिशत अंकों की वृद्धि है, बस "
        "इतना ही। प्रतिशत परिवर्तन पूछता है कि मान अपने मूल के कितने भाग से हिला: 20% से 25% "
        "में 20 में से 5 की वृद्धि है, जो सापेक्ष रूप से 25% है। प्रतिशत परिवर्तन का आधार "
        "पहले का मान है; प्रतिशत-अंक अंतर में कोई आधार होता ही नहीं।"
    ),
    formula(
        "percentage points = new % - old %",
        "प्रतिशत अंक = नया % - पुराना %",
        "percentage change = (new - old) / old x 100",
        "प्रतिशत परिवर्तन = (नया - पुराना) / पुराना x 100"
    ),
    example(
        "Example: 20% rises to 25%",
        "उदाहरण: 20% से बढ़कर 25%",
        [
            ("In percentage points",
             "प्रतिशत अंकों में",
             "25 - 20",
             "5 points"),
            ("As a percentage change",
             "प्रतिशत परिवर्तन के रूप में",
             "5 / 20 x 100",
             "25%"),
            ("So both statements are true, and they are not the same fact",
             "अतः दोनों कथन सत्य हैं, पर वे एक ही तथ्य नहीं हैं",
             "5 points",
             "25%"),
        ]
    ),
    example(
        "Example: 40% falls to 30%",
        "उदाहरण: 40% से घटकर 30%",
        [
            ("In percentage points",
             "प्रतिशत अंकों में",
             "30 - 40",
             "-10 points"),
            ("As a percentage change",
             "प्रतिशत परिवर्तन के रूप में",
             "-10 / 40 x 100",
             "-25%"),
            ("The fall is 10 points but 25%",
             "गिरावट 10 अंक की है पर 25% की",
             "-10 points",
             "-25%"),
        ]
    ),
    explain(
        "Where the distinction matters",
        "यह भेद कहाँ मायने रखता है",
        "Interest rates, tax slabs, poll shares and pass marks are all quoted in percentage "
        "points. A loan rate moving from 8% to 10% is described as a rise of 2 percentage "
        "points, and that is what the borrower pays, in absolute terms, on a fixed principal. "
        "Describing it as a 25% rise would suggest the rate itself grew by a quarter, which is "
        "a true but very different fact.",
        "ब्याज दर, कर सीमाएँ, जनमत सर्वेक्षण और उत्तीर्ण अंक सब प्रतिशत अंकों में बताए जाते हैं। "
        "8% से 10% होने वाली दर को 2 प्रतिशत अंकों की वृद्धि कहा जाता है, और स्थिर मूलधन पर "
        "यही राशि उधारदार को देनी पड़ती है। इसे 25% वृद्धि कहना यह भ्रम पैदा करेगा कि दर "
        "स्वयं एक चौथाई बढ़ी, जो सत्य तो है पर बहुत भिन्न तथ्य है।"
    ),
    example(
        "Example: an interest rate from 8% to 10%",
        "उदाहरण: ब्याज दर 8% से 10%",
        [
            ("In percentage points",
             "प्रतिशत अंकों में",
             "10 - 8",
             "2 points"),
            ("As a percentage change",
             "प्रतिशत परिवर्तन के रूप में",
             "2 / 8 x 100",
             "25%"),
            ("On a principal of 1000",
             "1000 के मूलधन पर",
             "8% is 80, 10% is 100",
             "20 more, not 250"),
        ]
    ),
    mistake(
        "Saying 'rose by 5%' when it rose by 5 points",
        "'5% बढ़ा' कहना जबकि 5 अंक बढ़ा",
        "If a pass mark rises from 33% to 40%, the marks needed went up by 7 percentage "
        "points. But a student who needed 33 out of 100 now needs 40, which is a rise of about "
        "21% in the marks themselves. Reporting only one of the two hides the other.",
        "यदि उत्तीर्ण अंक 33% से 40% हो जाए, तो आवश्यक अंक 7 प्रतिशत अंक बढ़ गए। पर जिस "
        "विद्यार्थी को 100 में से 33 चाहिए थे, अब 40 चाहिए, यानी अंकों में लगभग 21% की वृद्धि। "
        "दोनों में से केवल एक बताना दूसरे को छिपा देता है।"
    ),
    check(
        "Knowledge check",
        "ज्ञान परीक्षण",
        "A discount moves from 10% to 15%. State both the change in percentage points and the "
        "relative percentage change.",
        "एक छूट 10% से 15% हो जाती है। प्रतिशत अंक में परिवर्तन और सापेक्ष प्रतिशत परिवर्तन, दोनों बताएँ।",
        "5 percentage points, and 5 / 10 x 100 = 50% relative increase.",
        "5 प्रतिशत अंक, और 5 / 10 x 100 = 50% सापेक्ष वृद्धि।"
    ),
    summary(
        "Summary",
        "सारांश",
        [
            ("Percentage points are a subtraction with no base.",
             "प्रतिशत अंक बिना आधार के घटाव हैं।"),
            ("Percentage change divides by the earlier value.",
             "प्रतिशत परिवर्तन पहले के मान से भाग देकर निकाला जाता है।"),
            ("Both can be true at once; say which one you mean.",
             "दोनों एक साथ सत्य हो सकते हैं; बताएँ कि आपका कौन-सा अभिप्राय है।"),
        ]
    ),
])

# =============================================================================
# Lesson 11 (order) - Comparing Fractions, Decimals and Percentages
# =============================================================================
lesson("l11", [
    intro(
        "Three ways of writing the same quantity",
        "एक ही मात्रा को लिखने के तीन तरीके",
        "A proportion can be written as a fraction, as a decimal, or as a percentage, and all "
        "three can appear in the same question. Comparing them is easy once everything is "
        "converted to one form. The conversions are: multiply a fraction by 100 to get a "
        "percentage, and multiply a decimal by 100 to get a percentage.",
        "एक अनुपात को भिन्न, दशमलव या प्रतिशत के रूप में लिखा जा सकता है, और एक ही प्रश्न में "
        "तीनों आ सकते हैं। एक ही रूप में बदलने पर तुलना आसान हो जाती है। रूपांतरण हैं: प्रतिशत "
        "पाने के लिए भिन्न को 100 से गुणा करें, और दशमलव को 100 से गुणा करें।"
    ),
    explain(
        "Normalise before comparing",
        "तुलना से पहले एक ही रूप में बदलें",
        "Comparing 33/100 with 1/3 directly tells you nothing, because the denominators "
        "differ. Converting both to percentages puts them on a common scale: 33/100 is 33%, "
        "and 1/3 is 33.33%. Now the comparison is immediate. When one of the three forms is "
        "already a percentage, convert the others to match it.",
        "33/100 की तुलना 1/3 से सीधे करने से कुछ नहीं निकलता, क्योंकि निचली संख्याएँ अलग हैं। "
        "दोनों को प्रतिशत में बदलने पर वे एक ही पैमाने पर आ जाते हैं: 33/100 यानी 33%, और 1/3 "
        "यानी 33.33%। अब तुलना तुरंत स्पष्ट है। जब तीनों में से एक रूप पहले से प्रतिशत हो, तो "
        "बाकी को उसी में बदलें।"
    ),
    formula(
        "fraction a/b -> (a / b) x 100 %        decimal -> x 100 %",
        "भिन्न a/b -> (a / b) x 100 %        दशमलव -> x 100 %",
        "Common fractions worth memorising: 1/2 = 50%, 1/4 = 25%, 3/4 = 75%, 1/5 = 20%, 1/10 = 10%.",
        "याद रखने योग्य सामान्य भिन्न: 1/2 = 50%, 1/4 = 25%, 3/4 = 75%, 1/5 = 20%, 1/10 = 10%।"
    ),
    explain(
        "Common fractions",
        "सामान्य भिन्न",
        "A handful of fractions carry most of the weight in these questions, because they have "
        "clean percentage equivalents. If you know them by heart you can recognise an answer "
        "without computing anything.",
        "कुछ भिन्न इन प्रश्नों का अधिकांश भार ढोते हैं, क्योंकि उनके साफ प्रतिशत रूप हैं। "
        "यदि आप इन्हें रट लें, तो आप बिना कुछ गणना किए उत्तर पहचान लेते हैं।",
        [
            ("1/2 = 50% and 2/3 of a half is 33.33%, not 50%",
             "1/2 = 50%, और आधे का 2/3 यानी 33.33% है, 50% नहीं"),
            ("1/5 = 20%, so 3/5 = 60%",
             "1/5 = 20%, इसलिए 3/5 = 60%"),
            ("1/8 = 12.5%, which makes 12.5% problems quick",
             "1/8 = 12.5%, जिससे 12.5% वाले प्रश्न तेज़ हो जाते हैं"),
        ]
    ),
    example(
        "Example: converting three forms",
        "उदाहरण: तीनों रूपों में बदलना",
        [
            ("Convert 3/4",
             "3/4 बदलें",
             "(3 / 4) x 100",
             "75%"),
            ("Convert 0.42",
             "0.42 बदलें",
             "0.42 x 100",
             "42%"),
            ("Convert 5/20",
             "5/20 बदलें",
             "5 / 20 = 1/4",
             "25%"),
            ("Order them",
             "क्रम दें",
             "75%, 42%, 25%",
             "75% > 42% > 25%"),
        ]
    ),
    example(
        "Example: ordering three quantities",
        "उदाहरण: तीन मात्राओं का क्रम",
        [
            ("Convert each",
             "तीनों बदलें",
             "1/3, 0.34, 35%",
             "33.33%, 34%, 35%"),
            ("Order them",
             "क्रम दें",
             "33.33%, 34%, 35%",
             "33.33% < 34% < 35%"),
            ("So the order is",
             "अतः क्रम है",
             "1/3, 0.34, 35%",
             "smallest, middle, largest"),
        ]
    ),
    mistake(
        "Comparing across forms without converting",
        "बिना बदले अलग-अलग रूपों की तुलना",
        "0.3 looks bigger than 25% but it is only 30% against 25%, so in this case the decimal "
        "does win. Try 0.29 against 2/7: 0.29 is 29%, while 2/7 is about 28.57%, so the decimal "
        "wins again. But try 0.4 against 3/8: 0.4 is 40% and 3/8 is 37.5%, and now the "
        "fraction looks larger than the decimal if you never converted. Always convert.",
        "0.3, 25% से बड़ा दिखता है, पर वह केवल 30% के विरुद्ध 25% है, अतः यहाँ दशमलव जीतता है। "
        "0.29 और 2/7 आज़माएँ: 0.29 यानी 29%, जबकि 2/7 लगभग 28.57%, फिर भी दशमलव जीता। पर 0.4 "
        "और 3/8 आज़माएँ: 0.4 यानी 40% और 3/8 यानी 37.5%, और यहाँ बिना बदले भिन्न दशमलव से "
        "बड़ा दिखेगा। हमेशा बदलें।"
    ),
    check(
        "Knowledge check",
        "ज्ञान परीक्षण",
        "Which is larger, 0.29 or 2/7?",
        "कौन बड़ा है, 0.29 या 2/7?",
        "0.29 = 29%, and 2/7 is about 28.57%, so 0.29 is larger.",
        "0.29 = 29%, और 2/7 लगभग 28.57%, अतः 0.29 बड़ा है।"
    ),
    summary(
        "Summary",
        "सारांश",
        [
            ("Multiply fractions and decimals by 100 to reach percentages.",
             "प्रतिशत तक पहुँचने के लिए भिन्न और दशमलव को 100 से गुणा करें।"),
            ("Compare only after everything is in the same form.",
             "केवल तभी तुलना करें जब सब कुछ एक ही रूप में हो।"),
            ("Know 1/2, 1/4, 3/4, 1/5 and 1/10 by heart.",
             "1/2, 1/4, 3/4, 1/5 और 1/10 याद रखें।"),
        ]
    ),
])

# =============================================================================
# Lesson 12 (order) - Exam Shortcuts and Mental Calculation
# =============================================================================
lesson("l12", [
    intro(
        "Shortcuts that hold up under pressure",
        "दबाव में भी सही रहने वाली शॉर्टकट विधियाँ",
        "This lesson collects the techniques that survive exam conditions. Each one is stated "
        "with the situation it is good for and, just as importantly, the situation where it "
        "does not apply. A shortcut presented without its limits is worse than no shortcut, "
        "because it produces a confident wrong answer. None of the methods below are tricks "
        "that work only for tidy numbers.",
        "यह पाठ उन तकनीकों को एकत्र करता है जो परीक्षा की परिस्थितियों में टिकती हैं। हर एक को "
        "उस स्थिति के साथ दिया गया है जिसमें यह उपयोगी है, और उतना ही महत्वपूर्ण रूप से वह स्थिति "
        "भी जहाँ यह लागू नहीं होती। बिना सीमा बताए बताई गई शॉर्टकट विधि किसी शॉर्टकट से भी बुरी "
        "होती है, क्योंकि वह आत्मविश्वासपूर्ण गलत उत्तर देती है। नीचे दी गई कोई विधि केवल "
        "सुंदर संख्याओं के लिए काम नहीं करती।"
    ),
    explain(
        "Shortcut 1: clean fractions of 100",
        "शॉर्टकट 1: 100 के साफ भिन्न",
        "Halves, quarters, fifths and tenths have exact percentage equivalents, so the whole "
        "question collapses into one subtraction or division. This is the fastest route when "
        "the numbers are friendly. It is not a general method: there is no clean fraction "
        "behind 33%, and forcing 33% into 'a third' gives an answer that is wrong rather than "
        "approximate.",
        "आधे, चौथाई, पाँचवें और दसवें भागों के ठीक प्रतिशत रूप होते हैं, इसलिए पूरा प्रश्न एक "
        "घटाव या भाग में समा जाता है। जब संख्याएँ अनुकूल हों, यह सबसे तेज़ रास्ता है। यह सामान्य "
        "विधि नहीं है: 33% के पीछे कोई साफ भिन्न नहीं, और 33% को 'एक तिहाई' में बाँधने से उत्तर "
        "गलत निकलता है, अनुमानित नहीं।"
    ),
    example(
        "Example: 12.5% of 640",
        "उदाहरण: 640 का 12.5%",
        [
            ("12.5% is one eighth",
             "12.5% एक आठवाँ भाग है",
             "100 / 8",
             "12.5, so 12.5% = 1/8"),
            ("Divide by 8",
             "8 से भाग दें",
             "640 / 8",
             "80"),
            ("Answer",
             "उत्तर",
             "80",
             "80"),
        ]
    ),
    explain(
        "Shortcut 2: the net formula for two changes",
        "शॉर्टकट 2: दो परिवर्तनों के लिए शुद्ध सूत्र",
        "For exactly two changes there is a direct formula. A rise of a% followed by a fall of "
        "b% nets a - b - ab/100. The mirror case, a fall of a% then a rise of b%, nets "
        "b - a - ab/100. Either way the correction term ab/100 is subtracted. This replaces "
        "two rounds of multiplying the running value, which is where arithmetic slips usually "
        "happen. Use it for exactly two changes; with three changes, multiply the three factors "
        "instead.",
        "ठीक दो परिवर्तनों के लिए सीधा सूत्र है। a% वृद्धि के बाद b% कमी से शुद्ध मान "
        "a - b - ab/100 आता है। इसका प्रतिरूपी स्थिति, a% कमी के बाद b% वृद्धि, से शुद्ध मान "
        "b - a - ab/100 आता है। दोनों में संशोधन पद ab/100 घटाया जाता है। यह बरतीबार मान को दो "
        "बार गुणा करने की जगह लेता है, जहाँ गणना में चूक आमतौर पर होती है। इसे ठीक दो परिवर्तनों "
        "के लिए प्रयोग करें; तीन परिवर्तनों में तीन गुणकों को गुणा करें।"
    ),
    example(
        "Example: 20% fall then 25% rise",
        "उदाहरण: 20% कमी फिर 25% वृद्धि",
        [
            ("This is the mirror case, so use b - a - ab/100",
             "यह प्रतिरूपी स्थिति है, इसलिए b - a - ab/100 लगाएँ",
             "a = 20, b = 25",
             "b - a - ab/100"),
            ("Substitute",
             "मान रखें",
             "25 - 20 - (20 x 25 / 100)",
             "0"),
            ("Check on 500",
             "500 पर जाँचें",
             "500 x 0.80 = 400, then 400 x 1.25",
             "500, so the net change is 0%"),
        ]
    ),
    explain(
        "Shortcut 3: ratio form for comparisons",
        "शॉर्टकट 3: तुलना के लिए अनुपात रूप",
        "Convert a 'what percent more or less' question into a ratio on a scale of 100 and "
        "reduce it. This is especially fast when both percentages are whole numbers, because "
        "the ratio terms are then whole numbers too and reduction is quick.",
        "'कितने प्रतिशत अधिक या कम' वाले प्रश्न को 100 के पैमाने पर अनुपात में बदलें और उसे "
        "सरल करें। यह विशेष रूप से तब तेज़ है जब दोनों प्रतिशत पूर्णांक हों, क्योंकि तब अनुपात "
        "के पद भी पूर्णांक होते हैं और सरल करना शीघ्र होता है।"
    ),
    example(
        "Example: a 40% rise in two equal steps",
        "उदाहरण: दो समान चरणों में 40% वृद्धि",
        [
            ("Let the original be 100",
             "मूल को 100 मानें",
             "100",
             "total multiplier 1.40"),
            ("Equal steps mean equal percentages",
             "समान चरणों का अर्थ समान प्रतिशत",
             "(1 + x/100)^2",
             "= 1.40"),
            ("This needs a square root",
             "इसमें वर्गमूल चाहिए",
             "sqrt(1.40)",
             "no clean answer, so not a shortcut"),
        ]
    ),
    mistake(
        "Treating a special case as a general rule",
        "विशेष स्थिति को सामान्य नियम समझना",
        "Halving and halving again gives one quarter, so two successive 50% falls leave 25%. "
        "That is true. But it does not generalise: two 40% falls leave 36%, not 20%, because "
        "the second fall is measured on the already-reduced value. Similarly, a 33% fall is "
        "close to one third but is not one third. Always use the exact formula unless the "
        "numbers really are clean.",
        "आधा करने के बाद फिर आधा करने से एक चौथाई बनता है, इसलिए लगातार दो 50% गिरावट से 25% "
        "बचता है। यह सत्य है। पर यह सामान्य नहीं होता: दो 40% गिरावट से 36% बचता है, 20% नहीं, "
        "क्योंकि दूसरी गिरावट पहले से घटे मान पर मापी जाती है। इसी प्रकार 33% गिरावट एक तिहाई "
        "के निकट है पर एक तिहाई नहीं। जब तक संख्याएँ सचमुच साफ न हों, हमेशा सूत्र ही लगाएँ।"
    ),
    check(
        "Knowledge check",
        "ज्ञान परीक्षण",
        "A price falls 40% and then rises 40%. Using 100 as the base, what is the final value "
        "and the net change?",
        "कोई मूल्य 40% घटता है और फिर 40% बढ़ता है। 100 को आधार मानकर अंतिम मान और शुद्ध "
        "परिवर्तन बताएँ।",
        "100 x 0.60 = 60, then 60 x 1.40 = 84. Net -16%, since 40 - 40 - "
        "(40 x 40 / 100) = -16.",
        "100 x 0.60 = 60, फिर 60 x 1.40 = 84। शुद्ध -16%, क्योंकि 40 - 40 - "
        "(40 x 40 / 100) = -16।"
    ),
    summary(
        "Summary",
        "सारांश",
        [
            ("Clean fractions of 100 are fast but only for clean numbers.",
             "100 के साफ भिन्न तेज़ हैं पर केवल साफ संख्याओं के लिए।"),
            ("For exactly two changes use a - b - ab/100 after a rise, or b - a - ab/100 after a fall.",
             "ठीक दो परिवर्तनों के लिए वृद्धि के बाद a - b - ab/100, और कमी के बाद b - a - ab/100 लगाएँ।"),
            ("Turn comparison questions into ratios on a scale of 100, then reduce.",
             "तुलना वाले प्रश्नों को 100 के पैमाने पर अनुपात में बदलें, फिर सरल करें।"),
            ("Never extend a shortcut beyond the case where it is exact.",
             "शॉर्टकट को उस स्थिति से आगे कभी बढ़ाएँ नहीं जहाँ वह ठीक हो।"),
        ]
    ),
])

if __name__ == "__main__":
    print(f"authored lessons: {len(LESSONS)}")
    for entry in LESSONS:
        print(f"  {entry['key']}: {len(entry['blocks'])} blocks")