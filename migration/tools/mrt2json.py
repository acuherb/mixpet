# -*- coding: utf-8 -*-
"""
mrt2json.py — PetSystem Stimulsoft .mrt 老模板 → 前端可渲染 JSON 版式

用法（在仓库根运行）：
    python3 migration/tools/mrt2json.py

输入：/home/debian/下载/PetSystem/web/PetFx/reports/ 下 11 个模板
输出：mix-ui/public/mrt/<模板名>.json

背景：老系统 121 个 .mrt 有两种方言 —— Net（体检 7 种，组件基本平铺）与
Flex（B超/X光，组件嵌在 ReportTitleBand/DataBand/FooterBand 内，坐标相对 Band）。
本脚本统一用 ElementTree 递归累加坡接坐标，产出厘米绝对坐标的轻量版式描述：

    {w, h, ml, mt, comps:[{t, x, y, w, h, s(文本), f(字号), b(粗体), a(对齐),
                            bd(边框), bg(底色), cb(勾选绑定), ib(图片绑定), ln(线型)}]}

前端 z-mrtPrint.vue 按此渲染，{temp.X} 表达式在运行时用数据字典替换。
只保留打印所需信息（文本/复选框/图片/线/矩形），不迁 Stimulsoft 的数据引擎与脚本。
"""
import json
import os
import re
import xml.etree.ElementTree as ET
from pathlib import Path

SRC = Path("/home/debian/下载/PetSystem/web/PetFx/reports")
DST = Path(__file__).resolve().parents[2] / "mix-ui/public/mrt"

TEMPLATES = [
    "ReportCheckupHealthReport", "ReportCheckupOralReport", "ReportCheckupPathoReport",
    "ReportCheckupPifuReport", "ReportCheckupTijianReport", "ReportCheckupYachiReport",
    "ReportCheckupYankehReport",
    "ReportTypeB", "ReportTypeBFull", "ReportTypeX", "ReportTypeXFull",
]

COMP_KINDS = {"Text", "CheckBox", "Image", "HorizontalLinePrimitive",
              "VerticalLinePrimitive", "RectanglePrimitive", "Cap"}
BAND_KINDS = {"ReportTitleBand", "HeaderBand", "DataBand", "FooterBand",
              "PageFooterBand", "ReportSummaryBand", "PageHeaderBand"}


def prop(el, tag, default=""):
    c = el.find(tag)
    return (c.text or "").strip() if c is not None and c.text else default


def rect(el):
    """ClientRectangle: 'x,y,w,h' 厘米"""
    try:
        parts = [float(v) for v in prop(el, "ClientRectangle").split(",")]
        return parts if len(parts) == 4 else [0, 0, 0, 0]
    except (ValueError, AttributeError):
        return [0, 0, 0, 0]


def simplify_expr(text):
    """Stimulsoft 表达式收敛：{temp.X} 保留前端绑定，其余求和/页码等去掉或转固定值"""
    if not text:
        return ""
    text = text.replace("{PageNumber}", "1").replace("{TotalPageCount}", "1")
    text = re.sub(r"\{Sum\([^}]*\)\}", "", text)
    text = re.sub(r"\{(?:Time|Today)\}", "{Time}", text)
    return text.strip()


def parse_font(el):
    raw = prop(el, "Font")  # 宋体,9.75,Bold,Point,False,134
    bits = raw.split(",")
    size = 9.0
    bold = False
    if len(bits) >= 2:
        try:
            size = float(bits[1])
        except ValueError:
            pass
    if len(bits) >= 3:
        bold = "bold" in bits[2].lower()
    return round(size, 1), bold


def border_of(el):
    raw = prop(el, "Border")
    if not raw or raw.split(";")[0].lower() in ("none",):
        return 0
    return 1


def comp_of(el, ox, oy):
    x, y, w, h = rect(el)
    kind = el.get("type")
    x, y = x + ox, y + oy
    d = {"x": round(x, 3), "y": round(y, 3), "w": round(w, 3), "h": round(h, 3)}
    if kind == "Text":
        d["t"] = "text"
        d["s"] = simplify_expr(prop(el, "Text"))
        d["f"], d["b"] = parse_font(el)
        d["a"] = prop(el, "HorAlignment", "Left").lower()
        d["v"] = prop(el, "VertAlignment", "Top").lower()
        d["bd"] = border_of(el)
        bg = prop(el, "Brush")
        if bg and bg.lower() not in ("transparent", "white"):
            d["bg"] = bg
        brush = prop(el, "TextBrush")
        if brush and brush.lower() not in ("black", "transparent"):
            d["fg"] = brush
        wrap = prop(el, "WordWrap")
        if wrap.lower() == "true":
            d["ww"] = 1
    elif kind == "CheckBox":
        d["t"] = "check"
        d["bd"] = 1
        m = re.search(r"\{(?:Date\.)?temp\.([^}]+)\}", prop(el, "Checked"))
        if m:
            d["cb"] = m.group(1)
    elif kind == "Image":
        d["t"] = "image"
        m = re.search(r"\{(?:Date\.)?temp\.([^}]+)\}", prop(el, "ImageURL") or prop(el, "Image"))
        if m:
            d["ib"] = m.group(1)
        d["bd"] = border_of(el)
    elif kind in ("HorizontalLinePrimitive", "VerticalLinePrimitive"):
        d["t"] = "line"
        d["ln"] = "h" if w >= h else "v"
        # 线宽可能为 0，渲染时给 1px
        if d["ln"] == "h" and h < 0.02:
            d["h"] = 0.02
        if d["ln"] == "v" and w < 0.02:
            d["w"] = 0.02
    elif kind == "RectanglePrimitive":
        d["t"] = "rect"
        d["bd"] = 1
    elif kind == "Cap":
        d["t"] = "cap"
        d["bd"] = 1
    else:
        return None
    return d


def walk(container, ox, oy, out):
    """遍历 components 容器，Band 内组件坐标相对 Band，递归累加偏移"""
    for el in container:
        kind = el.get("type") or ""
        if kind in COMP_KINDS:
            c = comp_of(el, ox, oy)
            if c:
                out.append(c)
        if kind in BAND_KINDS:
            bx, by, _, _ = rect(el)
            sub = el.find("Components")
            if sub is not None:
                walk(sub, ox + bx, oy + by, out)
        # 非 Band 容器（如 Panel）也递归，但坐标不叠加（暂未遇到）
        sub = el.find("Components") if kind not in BAND_KINDS else None
        if sub is not None and kind not in COMP_KINDS:
            walk(sub, ox, oy, out)


def convert(name):
    root = ET.parse(SRC / (name + ".mrt")).getroot()
    page = next((e for e in root.iter() if e.get("type") == "Page"), None)
    if page is None:
        raise RuntimeError(name + ": 未找到 Page")
    w = float(prop(page, "PageWidth", "21"))
    h = float(prop(page, "PageHeight", "29.7"))
    # Margins 在 Page 直属子元素里（Flex: 1,1,1,1 厘米；Net 有时 0）
    ml = mt = 0.0
    for c in page:
        if c.tag == "Margins" and c.text:
            try:
                vals = [float(v) for v in c.text.split(",")]
                if len(vals) == 4:
                    ml, mt = vals[0], vals[1]
            except ValueError:
                pass
            break
    comps = []
    top = page.find("Components")
    if top is not None:
        walk(top, 0, 0, comps)
    # 丢弃页面外与空文本组件（减少体积，不影响版式）
    comps = [c for c in comps if c["y"] < h and c["x"] < w
             and (c["t"] != "text" or c.get("s"))]
    return {"name": name, "w": w, "h": h, "ml": ml, "mt": mt, "comps": comps}


def main():
    DST.mkdir(parents=True, exist_ok=True)
    for name in TEMPLATES:
        data = convert(name)
        # 前端按短名取模板：CheckupTijian / TypeB / TypeXFull ...
        short = ("Checkup" + name[len("ReportCheckup"):].replace("Report", "")
                 if name.startswith("ReportCheckup") else name.replace("Report", ""))
        (DST / (short + ".json")).write_text(
            json.dumps(data, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
        print(f"{name:36s} → {short + '.json':22s} {data['w']}×{data['h']}cm "
              f"边距{data['ml']}/{data['mt']} 组件{len(data['comps'])}")
    print(f"\n输出目录: {DST}")


if __name__ == "__main__":
    main()
