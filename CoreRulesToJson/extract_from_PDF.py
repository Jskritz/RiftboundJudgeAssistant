# ...existing code...
import re
import json
import pathlib
from typing import Optional
from pypdf import PdfReader

PDF_PATH = pathlib.Path(r"c:\Users\Joshua\Documents\CoreRulesToJson\Riftbound Core Rules v1.1-100125.pdf")
OUT_PATH = pathlib.Path(r"c:\Users\Joshua\Documents\CoreRulesToJson\riftbound_core_rules.json")

# Specify which IDs are Level1 (sections) and which are Level2 (shaded subsections).
LEVEL1_IDS = ["000","100","300","700"]   # <- edit as needed
LEVEL2_IDS = ["001","050","101","104","106","119","124","137","143","146","153","162","170","179",
              "301","316","318","324","346","357","395","419","433","437","441","446","454","649",
              "701","706","712","716"]         # <- edit as needed

ID_LINE_RE = re.compile(r'^\s*(\d{1,3}(?:\.[A-Za-z0-9]+)*)\.\s*(.*)$')  # captures "103.1.a.  text" -> "103.1.a", "text"
LAST_UPDATED_RE = re.compile(r'Last\s*Updated\s*[:\-]\s*([\d]{4}-[\d]{2}-[\d]{2})', re.I)

def extract_text_from_pdf(pdf_path: pathlib.Path) -> str:
    reader = PdfReader(str(pdf_path))
    pages = []
    for p in reader.pages:
        txt = p.extract_text() or ""
        pages.append(txt)
    return "\n".join(pages)

def normalize_lines(raw_text: str):
    # collapse multiple spaces but keep line breaks for parsing
    normalized = re.sub(r'[ \t]+', ' ', raw_text)
    lines = [ln.strip() for ln in normalized.splitlines()]
    # drop empty lines
    return [ln for ln in lines if ln]

def build_empty_subsection():
    return {"title": "", "content": "", "examples": [], "subsubsections": {}}

def is_level1(num: str) -> bool:
    top = num.split('.')[0]
    if LEVEL1_IDS:
        return (num in LEVEL1_IDS) or (top in LEVEL1_IDS)
    # fallback: numbers with no dot and ending in "00"
    return ('.' not in num) and num.endswith("00")

def is_level2(num: str) -> bool:
    top = num.split('.')[0]
    if LEVEL2_IDS:
        return (num in LEVEL2_IDS) or (top in LEVEL2_IDS)
    return False

def strip_after_example_line(s: str) -> str:
    """
    Remove any text from the first occurrence of 'Example' or 'Examples' (case-insensitive)
    in the given line, returning the portion before that word. If the word is not present,
    return the original string.
    This is applied only to SubSubsection 'text' field lines per request.
    """
    if not s:
        return s
    m = re.search(r'\bExamples?\b', s, flags=re.I)
    if m:
        return s[:m.start()].rstrip()
    return s

def parse(lines):
    doc = {
        "title": "Riftbound Core Rules (Option B schema)",
        "lastUpdated": "",
        "sections": {}
    }

    # find title / lastUpdated in first lines if present
    for ln in lines[:10]:
        if "riftbound" in ln.lower() and "rules" in ln.lower():
            doc["title"] = ln.strip()
        m = LAST_UPDATED_RE.search(ln)
        if m:
            doc["lastUpdated"] = m.group(1)
    if not doc["lastUpdated"]:
        doc["lastUpdated"] = "2025-10-01"

    current_section: Optional[str] = None
    current_sub_id: Optional[str] = None
    last_target = None  # tuple ("section", sec_id) or ("sub", sec_id, sub_id) or ("subsub", sec_id, sub_id, key)

    for ln in lines:
        m = ID_LINE_RE.match(ln)
        if m:
            num = m.group(1)        # e.g. "103.1.b"
            content = m.group(2).strip()
            # Level1 detection (explicit list or fallback)
            if is_level1(num):
                current_section = num.split('.')[0] if '.' in num else num
                doc["sections"].setdefault(current_section, {"title": "", "intro": "", "subsections": {}})
                if content:
                    doc["sections"][current_section]["title"] = content
                current_sub_id = None
                last_target = ("section", current_section)
                continue

            # Level2 detection (explicit list)
            if is_level2(num):
                if current_section is None:
                    current_section = "000"
                    doc["sections"].setdefault(current_section, {"title": "", "intro": "", "subsections": {}})
                sub_id = num.split('.')[0]  # use top3 as subsection id
                doc["sections"][current_section]["subsections"].setdefault(sub_id, build_empty_subsection())
                if content:
                    doc["sections"][current_section]["subsections"][sub_id]["title"] = content
                current_sub_id = sub_id
                last_target = ("sub", current_section, sub_id)
                continue

            # Otherwise treat as Level3 (sub-subsection)
            if current_section is None:
                current_section = "000"
                doc["sections"].setdefault(current_section, {"title": "", "intro": "", "subsections": {}})
            if current_sub_id is None:
                default_sub = "000"
                doc["sections"][current_section]["subsections"].setdefault(default_sub, build_empty_subsection())
                current_sub_id = default_sub

            subsub_key = num  # full numbering e.g., "103.1", "103.1.a", "102"
            title = ""
            # If it's a bullet/example line, keep in examples; else treat as text and strip after 'Example'
            # examples = []
            # bullet_m = re.match(r'^[\*\u2022•\-]\s*(.*)$', content)
            # if bullet_m:
            #     examples.append(bullet_m.group(1).strip())
            #     text = ""
            # else:
            text = strip_after_example_line(content)

            ss_map = doc["sections"][current_section]["subsections"][current_sub_id].setdefault("subsubsections", {})
            ss_map[subsub_key] = {"title": title, "text": text, "examples": ""}
            last_target = ("subsub", current_section, current_sub_id, subsub_key)
            continue

        # Non-numbered continuation line
        if last_target is None:
            continue

        if last_target[0] == "section":
            sec = last_target[1]
            if doc["sections"].get(sec, {}).get("intro"):
                doc["sections"][sec]["intro"] += "\n" + ln
            else:
                doc["sections"][sec]["intro"] = ln

        elif last_target[0] == "sub":
            _, sec, sub = last_target
            cur = doc["sections"][sec]["subsections"].setdefault(sub, build_empty_subsection())
            if cur.get("content"):
                cur["content"] += "\n" + ln
            else:
                cur["content"] = ln

        elif last_target[0] == "subsub":
            _, sec, sub, key = last_target
            cur = doc["sections"][sec]["subsections"].setdefault(sub, build_empty_subsection())
            ss = cur.setdefault("subsubsections", {}).setdefault(key, {"title": "", "text": "", "examples": []})
            bm = re.match(r'^[\*\u2022•\-]\s*(.*)$', ln)
            if bm:
                ss.setdefault("examples", []).append(bm.group(1).strip())
            else:
                # strip any text after 'Example' in this continuation line before appending
                cleaned = strip_after_example_line(ln)
                if cleaned:
                    if ss.get("text"):
                        ss["text"] += "\n" + cleaned
                    else:
                        ss["text"] = cleaned

    return doc

def main():
    raw = extract_text_from_pdf(PDF_PATH)
    lines = normalize_lines(raw)
    doc = parse(lines)
    OUT_PATH.write_text(json.dumps(doc, ensure_ascii=False, indent=2), encoding='utf-8')
    print(f"Wrote JSON to {OUT_PATH}")

if __name__ == "__main__":
    main()
# ...existing code...