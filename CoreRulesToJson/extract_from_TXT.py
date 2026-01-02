import re
import json
import pathlib
from typing import Optional

TXT_PATH = pathlib.Path(r"C:\Users\Joshua\Documents\GitSources\RiftboundJudgeAssistant\CoreRulesToJson\Riftbound Core Rules v1.2.txt")
OUT_PATH = pathlib.Path(r"C:\Users\Joshua\Documents\GitSources\RiftboundJudgeAssistant\CoreRulesToJson\core_rules.json")

# Edit these lists to explicitly mark Level1 and Level2 ids.
# If LEVEL1_IDS is empty, Level1 will be detected by numbers ending in "00" (e.g., 100, 200).
## Tournament rules
LEVEL1_IDS = ["000","100","200","300","400","500","600","700"]            # e.g. ["000","100","200","300"]
LEVEL2_IDS = ["101","102","103","104","105",
              "201", "202","203","204","205","206",
              "301","302","303","304","305","306",
              "401","402","403","404","405","406","407","408","409","410","411","412","413","414","415","416","417","418","419","420","421","422","423","424"
              "500","501","502","503","504","505","506","507","508","509",
              "601","602","603","604",
              "701","702","703","704"]            # e.g. ["101","102","104"]
## Core Rules
LEVEL1_IDS = ["000","100","300","700"]   # <- edit as needed
LEVEL2_IDS = ["001","050","101","104","119","124","139","145","149","156","165","169","173", "182", 
              "301","318","324","346","357","395","423","437","441","445","450","458","649",
              "701","706","712","716","720","726"]


ID_LINE_RE = re.compile(r'^\s*(\d{1,3}(?:\.[A-Za-z0-9]+)*)\.\s*(.*)$')  # "103.1.a.  text"
LAST_UPDATED_RE = re.compile(r'Last\s*Updated\s*[:\-]\s*([\d]{4}-[\d]{2}-[\d]{2})', re.I)

def normalize_lines(raw: str):
    s = re.sub(r'[ \t]+', ' ', raw)  # collapse spaces but keep newlines
    lines = [ln.strip() for ln in s.splitlines()]
    return [ln for ln in lines if ln]

def is_level1(num: str) -> bool:
    top = num.split('.')[0]
    if LEVEL1_IDS:
        return (num in LEVEL1_IDS) or (top in LEVEL1_IDS)
    return ('.' not in num) and top.endswith("00")

def is_level2(num: str) -> bool:
    top = num.split('.')[0]
    if LEVEL2_IDS:
        # treat as level2 only when the exact id is listed OR when the id has no dot and the top is in LEVEL2_IDS
        return (num in LEVEL2_IDS) or ('.' not in num and top in LEVEL2_IDS)
    return False

def strip_after_example_line(s: str) -> str:
    if not s:
        return s
    m = re.search(r'\bExamples?\b', s, flags=re.I)
    if m:
        return s[:m.start()].rstrip()
    return s

def build_empty_subsection():
    return {"title": "", "content": "", "examples": [], "subsubsections": {}}

def parse_lines(lines):
    doc = {"title": "Tournament Rules (Option B schema)", "lastUpdated": "", "sections": {}}
    # try find last updated
    for ln in lines[:20]:
        m = LAST_UPDATED_RE.search(ln)
        if m:
            doc["lastUpdated"] = m.group(1)
    if not doc["lastUpdated"]:
        doc["lastUpdated"] = "2025-10-01"

    current_section: Optional[str] = None
    current_sub_id: Optional[str] = None
    last_target = None  # ("section", sec_id) or ("sub", sec_id, sub_id) or ("subsub", sec_id, sub_id, key)

    for ln in lines:
        m = ID_LINE_RE.match(ln)
        if m:
            num = m.group(1)
            content = m.group(2).strip()
           # Level1
            if is_level1(num):
                sec = num.split('.')[0]
                current_section = sec
                doc["sections"].setdefault(sec, {"title": "", "intro": "", "subsections": {}})
                if content:
                    doc["sections"][sec]["title"] = content
                    # put level-1 text into the section intro (not the title)
                    #doc["sections"][sec]["intro"] = content
                current_sub_id = None
                last_target = ("section", sec)
                continue
            # Level2
            if is_level2(num):
                if current_section is None:
                    current_section = "000"
                    doc["sections"].setdefault(current_section, {"title": "", "intro": "", "subsections": {}})
                sub_id = num.split('.')[0]
                doc["sections"][current_section]["subsections"].setdefault(sub_id, build_empty_subsection())
                if content:
                    doc["sections"][current_section]["subsections"][sub_id]["title"] = content
                    # put level-2 text into the subsection content (not the title)
                    #doc["sections"][current_section]["subsections"][sub_id]["content"] = content
                current_sub_id = sub_id
                last_target = ("sub", current_section, sub_id)
                continue
            # Level3 (sub-subsection)
            if current_section is None:
                current_section = "000"
                doc["sections"].setdefault(current_section, {"title": "", "intro": "", "subsections": {}})
            if current_sub_id is None:
                default = "000"
                doc["sections"][current_section]["subsections"].setdefault(default, build_empty_subsection())
                current_sub_id = default
            key = num  # full numbering 
            #key = num.rsplit('.', 1)[0] remove the last dot
            # treat bullets as examples; otherwise text (strip after Example)
            examples = []
            bullet_m = re.match(r'^[\*\u2022•\-]\s*(.*)$', content)
            if bullet_m:
                examples.append(bullet_m.group(1).strip())
                text = ""
            else:
                text = strip_after_example_line(content)
            ss = doc["sections"][current_section]["subsections"][current_sub_id].setdefault("subsubsections", {})
            ss[key] = {"title": "", "text": text, "examples": examples}
            last_target = ("subsub", current_section, current_sub_id, key)
            continue

        # continuation lines
        if last_target is None:
            continue
        if last_target[0] == "section":
            sec = last_target[1]
            cur = doc["sections"].setdefault(sec, {"title": "", "intro": "", "subsections": {}})
            if cur.get("intro"):
                cur["intro"] += " " + ln
            else:
                cur["intro"] = ln
        elif last_target[0] == "sub":
            _, sec, sub = last_target
            cur = doc["sections"].setdefault(sec, {"title": "", "intro": "", "subsections": {}})["subsections"].setdefault(sub, build_empty_subsection())
            if cur.get("content"):
                cur["content"] += " " + ln
            else:
                cur["content"] = ln
        elif last_target[0] == "subsub":
            _, sec, sub, key = last_target
            cur_sub = doc["sections"].setdefault(sec, {"title": "", "intro": "", "subsections": {}})["subsections"].setdefault(sub, build_empty_subsection())
            ss = cur_sub.setdefault("subsubsections", {}).setdefault(key, {"title": "", "text": "", "examples": [], "examples_started": False})
            bm = re.match(r'^[\*\u2022•\-]\s*(.*)$', ln)
            if bm:
                ss.setdefault("examples", []).append(bm.group(1).strip())
                ss["examples_started"] = True
            elif re.search(r'\bExamples?\b', ln, flags=re.I):
                # Once we hit "Example", stop adding to text
                ss["examples_started"] = True
            else:
                # Only add to text if we haven't started examples yet
                if not ss.get("examples_started"):
                    cleaned = strip_after_example_line(ln)
                    if cleaned:
                        if ss.get("text"):
                            ss["text"] += " " + cleaned
                        else:
                            ss["text"] = cleaned

    return doc

def main():
    raw = TXT_PATH.read_text(encoding='utf-8', errors='ignore')
    lines = normalize_lines(raw)
    doc = parse_lines(lines)
    
    # Clean up internal flags before saving
    for section in doc.get("sections", {}).values():
        for subsection in section.get("subsections", {}).values():
            for ss in subsection.get("subsubsections", {}).values():
                ss.pop("examples_started", None)
    
    OUT_PATH.write_text(json.dumps(doc, ensure_ascii=False, indent=2), encoding='utf-8')
    print(f"Wrote JSON to {OUT_PATH}")

if __name__ == "__main__":
    main()