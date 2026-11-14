"""Genera docs/uml/skillswap.mdj (StarUML) y docs/uml/modelo.md (Mermaid) a partir de model.py."""
import itertools
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from model import D, E, R  # noqa: E402

OUT_DIR = sys.argv[1]
_ids = itertools.count(1)


def new_id():
    return "SKSW%016d=" % next(_ids)


def ref(obj):
    return {"$ref": obj["_id"]}


CHAR_W, LINE_H, FONT = 7.6, 15, "Arial;13;0"


# ---------------------------------------------------------------- parsing de firmas
def parse_op(text):
    mods = set()
    while True:
        m = re.match(r"(static|abstract)\s+", text)
        if not m:
            break
        mods.add(m.group(1))
        text = text[m.end():]
    vis = {"+": "public", "-": "private", "#": "protected", "~": "package"}[text[0]]
    m = re.match(r"(\w+)\((.*)\)(?::\s*(.+))?$", text[1:])
    name, params, ret = m.group(1), m.group(2), m.group(3)
    plist = []
    depth, cur = 0, ""
    for ch in params:
        if ch == "<":
            depth += 1
        elif ch == ">":
            depth -= 1
        if ch == "," and depth == 0:
            plist.append(cur.strip())
            cur = ""
        else:
            cur += ch
    if cur.strip():
        plist.append(cur.strip())
    parsed = []
    for p in plist:
        pn, pt = [x.strip() for x in p.split(":", 1)]
        parsed.append((pn, pt))
    return dict(vis=vis, name=name, params=parsed, ret=ret, static="static" in mods, abstract="abstract" in mods)


def op_text(op, compact):
    sym = {"public": "+", "private": "-", "protected": "#", "package": "~"}[op["vis"]]
    ps = ", ".join(n if compact else f"{n}: {t}" for n, t in op["params"])
    return f"{sym}{op['name']}({ps})" + (f": {op['ret']}" if op["ret"] else "")


def parse_attr(text):
    vis = {"+": "public", "-": "private", "#": "protected", "~": "package"}.get(text[0])
    if text.strip() == "...":
        return dict(vis="public", name="...", type="")
    name, typ = [x.strip() for x in text[1:].split(":", 1)]
    return dict(vis=vis, name=name, type=typ)


# Firmas largas: se muestran sin tipos de parámetros para que las cajas no queden enormes.
def compact(op):
    return len(op_text(op, False)) > 64


# ---------------------------------------------------------------- modelo StarUML
project = {"_type": "Project", "_id": new_id(), "name": "SkillSwap", "ownedElements": []}
model = {"_type": "UMLModel", "_id": new_id(), "_parent": ref(project), "name": "SkillSwap", "ownedElements": []}
project["ownedElements"].append(model)

packages = {}


def package(path):
    if path in packages:
        return packages[path]
    parts = path.split(".")
    parent = model if len(parts) == 1 else package(".".join(parts[:-1]))
    pkg = {"_type": "UMLPackage", "_id": new_id(), "_parent": ref(parent), "name": parts[-1], "ownedElements": []}
    parent["ownedElements"].append(pkg)
    packages[path] = pkg
    return pkg


elements = {}
for name, spec in E.items():
    pkg = package(spec["pkg"])
    kind = spec["kind"]
    t = {"interface": "UMLInterface", "enum": "UMLEnumeration"}.get(kind, "UMLClass")
    e = {"_type": t, "_id": new_id(), "_parent": ref(pkg), "name": name}
    if kind == "abstract":
        e["isAbstract"] = True
    if kind == "record":
        e["stereotype"] = "record"
    spec["parsed_attrs"] = [parse_attr(a) for a in spec["attrs"]]
    spec["parsed_ops"] = [parse_op(o) for o in spec["ops"]]
    if spec["parsed_attrs"]:
        e["attributes"] = []
        for a in spec["parsed_attrs"]:
            attr = {"_type": "UMLAttribute", "_id": new_id(), "_parent": ref(e), "name": a["name"],
                    "visibility": a["vis"]}
            if a["type"]:
                attr["type"] = a["type"]
            a["model"] = attr
            e["attributes"].append(attr)
    if spec["parsed_ops"]:
        e["operations"] = []
        for o in spec["parsed_ops"]:
            op = {"_type": "UMLOperation", "_id": new_id(), "_parent": ref(e), "name": o["name"],
                  "visibility": o["vis"], "parameters": []}
            if o["static"]:
                op["isStatic"] = True
            if o["abstract"] or kind == "interface":
                op["isAbstract"] = True
            # Firmas largas: los parámetros van sin tipo, igual que se miden las cajas.
            for pn, pt in o["params"]:
                param = {"_type": "UMLParameter", "_id": new_id(), "_parent": ref(op), "name": pn}
                if not compact(o):
                    param["type"] = pt
                op["parameters"].append(param)
            if o["ret"]:
                op["parameters"].append({"_type": "UMLParameter", "_id": new_id(), "_parent": ref(op),
                                         "type": o["ret"], "direction": "return"})
            o["model"] = op
            e["operations"].append(op)
    if spec["literals"]:
        e["literals"] = []
        spec["literal_models"] = []
        for lit in spec["literals"]:
            lm = {"_type": "UMLEnumerationLiteral", "_id": new_id(), "_parent": ref(e), "name": lit}
            e["literals"].append(lm)
            spec["literal_models"].append(lm)
    pkg["ownedElements"].append(e)
    elements[name] = e

relations = []
for r in R:
    kind = r[0]
    src = elements[r[1]]
    tgt = elements[r[2]]
    src.setdefault("ownedElements", [])
    if kind == "assoc":
        _, a, b, ma, mb, role, composite = r
        rel = {"_type": "UMLAssociation", "_id": new_id(), "_parent": ref(src)}
        end1 = {"_type": "UMLAssociationEnd", "_id": new_id(), "_parent": ref(rel), "reference": ref(src),
                "multiplicity": ma}
        end2 = {"_type": "UMLAssociationEnd", "_id": new_id(), "_parent": ref(rel), "reference": ref(tgt),
                "multiplicity": mb, "navigable": "navigable"}
        if role:
            end2["name"] = role
        if composite:
            end1["aggregation"] = "composite"
        rel["end1"], rel["end2"] = end1, end2
    else:
        t = {"gen": "UMLGeneralization", "real": "UMLInterfaceRealization", "dep": "UMLDependency"}[kind]
        rel = {"_type": t, "_id": new_id(), "_parent": ref(src), "source": ref(src), "target": ref(tgt)}
        if kind == "dep" and r[3]:
            rel["stereotype"] = r[3].strip("«»")
    src["ownedElements"].append(rel)
    relations.append((r, rel))


# ---------------------------------------------------------------- vistas
def label(parent, text=None, left=0, top=0, width=0, visible=True, bold=False, align=None):
    v = {"_type": "LabelView", "_id": new_id(), "_parent": ref(parent), "font": "Arial;13;1" if bold else FONT,
         "left": left, "top": top, "height": 13}
    if not visible:
        v["visible"] = False
    if width:
        v["width"] = width
    if text is not None:
        v["text"] = text
    if align is not None:
        v["horizontalAlignment"] = align
    return v


def hidden_compartment(t, parent, model_el, left, top):
    return {"_type": t, "_id": new_id(), "_parent": ref(parent), "model": ref(model_el), "visible": False,
            "font": FONT, "left": left, "top": top, "width": 10, "height": 10}


def node_view(diagram, name, x, y):
    spec, m = E[name], elements[name]
    kind = spec["kind"]
    stereo = {"interface": "«interface»", "enum": "«enumeration»", "record": "«record»"}.get(kind)
    attr_lines = [f"{'+-#~'[['public','private','protected','package'].index(a['vis'])]}{a['name']}"
                  + (f": {a['type']}" if a["type"] else "") for a in spec["parsed_attrs"]]
    op_lines = [op_text(o, compact(o)) for o in spec["parsed_ops"]]
    lit_lines = spec["literals"]
    longest = max([len(name) + 4, len(stereo or "")] + [len(s) for s in attr_lines + op_lines + lit_lines])
    width = int(longest * CHAR_W) + 24
    vt = {"interface": "UMLInterfaceView", "enum": "UMLEnumerationView"}.get(kind, "UMLClassView")
    view = {"_type": vt, "_id": new_id(), "_parent": ref(diagram), "model": ref(m), "subViews": [],
            "font": FONT, "containerChangeable": True, "left": x, "top": y, "width": width}
    top = y
    name_h = 25 + (13 if stereo else 0)
    nc = {"_type": "UMLNameCompartmentView", "_id": new_id(), "_parent": ref(view), "model": ref(m), "subViews": [],
          "font": FONT, "left": x, "top": top, "width": width, "height": name_h}
    st = label(nc, stereo, x + 5, top + 5, width - 10, visible=bool(stereo))
    nm = label(nc, name, x + 5, top + 5 + (17 if stereo else 0), width - 10, bold=True)
    ns = label(nc, "(from Model)", x + 5, top, 0, visible=False)
    pr = label(nc, None, x + 5, top, 0, visible=False, align=1)
    nc["subViews"] = [st, nm, ns, pr]
    nc.update(stereotypeLabel=ref(st), nameLabel=ref(nm), namespaceLabel=ref(ns), propertyLabel=ref(pr))
    view["subViews"].append(nc)
    top += name_h

    def compartment(t, rows, item_type):
        nonlocal top
        if not rows:
            return hidden_compartment(t, view, m, x, top)
        h = len(rows) * LINE_H + 10
        c = {"_type": t, "_id": new_id(), "_parent": ref(view), "model": ref(m), "subViews": [], "font": FONT,
             "left": x, "top": top, "width": width, "height": h}
        for i, (model_el, text, italic) in enumerate(rows):
            c["subViews"].append({"_type": item_type, "_id": new_id(), "_parent": ref(c), "model": ref(model_el),
                                  "font": "Arial;13;2" if italic else FONT, "left": x + 5, "top": top + 5 + i * LINE_H,
                                  "width": width - 10, "height": 13, "text": text, "horizontalAlignment": 0})
        top += h
        return c

    attrs = compartment("UMLAttributeCompartmentView",
                        [(a["model"], t, False) for a, t in zip(spec["parsed_attrs"], attr_lines)], "UMLAttributeView")
    ops = compartment("UMLOperationCompartmentView",
                      [(o["model"], t, o["abstract"] or kind == "interface")
                       for o, t in zip(spec["parsed_ops"], op_lines)], "UMLOperationView")
    view["subViews"] += [attrs, ops]
    rec = hidden_compartment("UMLReceptionCompartmentView", view, m, x, y)
    tpl = hidden_compartment("UMLTemplateParameterCompartmentView", view, m, x, y)
    view["subViews"] += [rec, tpl]
    view.update(nameCompartment=ref(nc), attributeCompartment=ref(attrs), operationCompartment=ref(ops),
                receptionCompartment=ref(rec), templateParameterCompartment=ref(tpl))
    if kind == "enum":
        lits = compartment("UMLEnumerationLiteralCompartmentView",
                           [(lm, lit, False) for lm, lit in zip(spec["literal_models"], spec["literals"])],
                           "UMLEnumerationLiteralView")
        view["subViews"].append(lits)
        view["enumerationLiteralCompartment"] = ref(lits)
    if not spec["parsed_attrs"]:
        view["suppressAttributes"] = True
    if not spec["parsed_ops"]:
        view["suppressOperations"] = True
    view["height"] = top - y
    return view


def border_point(box, toward):
    x, y, w, h = box
    cx, cy = x + w / 2, y + h / 2
    dx, dy = toward[0] - cx, toward[1] - cy
    if dx == 0 and dy == 0:
        return cx, cy
    sx = (w / 2) / abs(dx) if dx else float("inf")
    sy = (h / 2) / abs(dy) if dy else float("inf")
    s = min(sx, sy)
    return round(cx + dx * s), round(cy + dy * s)


def edge_label(edge, model_el, alpha, distance, position=None, visible=False, text=None):
    v = {"_type": "EdgeLabelView", "_id": new_id(), "_parent": ref(edge), "model": ref(model_el), "font": FONT,
         "height": 13, "alpha": alpha, "distance": distance, "hostEdge": ref(edge)}
    if position is not None:
        v["edgePosition"] = position
    if not visible:
        v["visible"] = False
    if text:
        v.update(text=text, width=len(text) * CHAR_W)
    return v


def edge_view(diagram, r, rel, views):
    tail_v, head_v = views[r[1]], views[r[2]]
    tb = (tail_v["left"], tail_v["top"], tail_v["width"], tail_v["height"])
    hb = (head_v["left"], head_v["top"], head_v["width"], head_v["height"])
    tc = (tb[0] + tb[2] / 2, tb[1] + tb[3] / 2)
    hc = (hb[0] + hb[2] / 2, hb[1] + hb[3] / 2)
    p1, p2 = border_point(tb, hc), border_point(hb, tc)
    t = {"assoc": "UMLAssociationView", "gen": "UMLGeneralizationView", "real": "UMLInterfaceRealizationView",
         "dep": "UMLDependencyView"}[r[0]]
    v = {"_type": t, "_id": new_id(), "_parent": ref(diagram), "model": ref(rel), "subViews": [], "font": FONT,
         "head": ref(head_v), "tail": ref(tail_v), "lineStyle": 1, "points": f"{p1[0]}:{p1[1]};{p2[0]}:{p2[1]}",
         "showVisibility": True}
    has_stereo = r[0] == "dep" and r[3]
    name_l = edge_label(v, rel, 1.5707963267948966, 15, 1)
    st_l = edge_label(v, rel, 1.5707963267948966, 30, 1, visible=bool(has_stereo),
                      text=r[3] if has_stereo else None)
    pr_l = edge_label(v, rel, -1.5707963267948966, 15, 1)
    v["subViews"] += [name_l, st_l, pr_l]
    v.update(nameLabel=ref(name_l), stereotypeLabel=ref(st_l), propertyLabel=ref(pr_l))
    if r[0] == "assoc":
        end1, end2 = rel["end1"], rel["end2"]
        labels = {}
        for prefix, end, pos, sign in [("tail", end1, 2, 1), ("head", end2, 0, -1)]:
            role = edge_label(v, end, sign * 0.5235987755982988, 30, pos, visible=bool(end.get("name")),
                              text=end.get("name"))
            prop = edge_label(v, end, sign * 0.7853981633974483, 40, pos)
            mult = edge_label(v, end, -sign * 0.5235987755982988, 25, pos, visible=True, text=end["multiplicity"])
            v["subViews"] += [role, prop, mult]
            labels.update({f"{prefix}RoleNameLabel": ref(role), f"{prefix}PropertyLabel": ref(prop),
                           f"{prefix}MultiplicityLabel": ref(mult)})
        tq = hidden_compartment("UMLQualifierCompartmentView", v, end1, 0, 0)
        hq = hidden_compartment("UMLQualifierCompartmentView", v, end2, 0, 0)
        v["subViews"] += [tq, hq]
        v.update(labels)
        v.update(showEndOrder="hide", tailQualifiersCompartment=ref(tq), headQualifiersCompartment=ref(hq))
    return v


for i, (dname, layout) in enumerate(D):
    diagram = {"_type": "UMLClassDiagram", "_id": new_id(), "_parent": ref(model), "name": dname, "ownedViews": []}
    if i == 0:
        diagram["defaultDiagram"] = True
    views = {}
    for name, (x, y) in layout.items():
        views[name] = node_view(diagram, name, x, y)
        diagram["ownedViews"].append(views[name])
    for r, rel in relations:
        if r[1] in views and r[2] in views:
            diagram["ownedViews"].append(edge_view(diagram, r, rel, views))
    model["ownedElements"].insert(i, diagram)

with open(os.path.join(OUT_DIR, "skillswap.mdj"), "w", encoding="utf-8") as f:
    json.dump(project, f, ensure_ascii=False, indent="\t")


# ---------------------------------------------------------------- Mermaid
def mm_name(n):
    return n.replace("<", "~").replace(">", "~")


def mm_id(n):
    return re.sub(r"\W", "", n.split("<")[0])


lines = ["# Modelo de clases de SkillSwap", "",
         "Diagramas de clases de **dominio** y **aplicación** generados a partir del código, con los mismos elementos "
         "que `skillswap.mdj` (StarUML). GitHub los renderiza para revisarlos sin abrir StarUML.", "",
         "Patrones que se ven en los diagramas:", "",
         "- **State**: `BookingState` y sus estados (contratación); `PublicationState` y sus estados (publicación).",
         "- **Composite**: `CatalogItem` → `Category` (compuesto) y `Skill` (hoja).",
         "- **Puertos y adaptadores (Clean Architecture)**: casos de uso como interfaces de entrada, "
         "repositorios y consultas como puertos de salida.", ""]
for dname, layout in D:
    lines += [f"## {dname}", "", "```mermaid", "classDiagram"]
    names = list(layout)
    for n in names:
        spec = E[n]
        lines.append(f"    class {mm_id(n)}[\"{n.replace('<', '&lt;').replace('>', '&gt;')}\"] {{")
        stereo = {"interface": "interface", "enum": "enumeration", "record": "record", "abstract": "abstract"}.get(spec["kind"])
        if stereo:
            lines.append(f"        <<{stereo}>>")
        for lit in spec["literals"]:
            lines.append(f"        {lit}")
        for a in spec["attrs"]:
            lines.append("        " + mm_name(a))
        for o in spec["parsed_ops"]:
            t = mm_name(op_text(o, True))
            if o["static"]:
                t += "$"
            if o["abstract"]:
                t += "*"
            lines.append("        " + t)
        lines.append("    }")
    for r in R:
        if r[1] not in layout or r[2] not in layout:
            continue
        a, b = mm_id(r[1]), mm_id(r[2])
        if r[0] == "assoc":
            arrow = "*--" if r[6] else "-->"
            lines.append(f'    {a} "{r[3]}" {arrow} "{r[4]}" {b}' + (f" : {r[5]}" if r[5] else ""))
        elif r[0] == "gen":
            lines.append(f"    {b} <|-- {a}")
        elif r[0] == "real":
            lines.append(f"    {b} <|.. {a}")
        else:
            lines.append(f"    {a} ..> {b}" + (f" : {r[3]}" if r[3] else ""))
    lines += ["```", ""]
with open(os.path.join(OUT_DIR, "modelo.md"), "w", encoding="utf-8") as f:
    f.write("\n".join(lines))
print("ok", len(E), "elements", len(R), "relations")
