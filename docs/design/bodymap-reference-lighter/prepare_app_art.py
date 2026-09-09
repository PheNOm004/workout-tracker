from pathlib import Path
import json, re
import xml.etree.ElementTree as ET
from svgpathtools import parse_path
from shapely.geometry import Polygon, LineString
from shapely.ops import split
from shapely.ops import unary_union

out=Path(__file__).resolve().parent
target=out.parent/'bodymap'
target.mkdir(exist_ok=True)
rows=json.loads((out/'regions.json').read_text())
groups={
 'CHEST':[3,4,131,132],
 'LATS':[7,8], 'TRAPS':[9,10,32,34,85],
 'UPPER_BACK':[56,58,78,79], 'LOWER_BACK':[21,24],
 'SIDE_DELTS':[36,38], 'FRONT_DELTS':[134,135], 'REAR_DELTS':[22,23,128,129],
 'BICEPS':[13,14], 'TRICEPS':[46,47,66,67,75,77,109,110],
 'FOREARMS':[31,33,52,55,57,59,73,74,80,81,118,120,127,130,138,140],
 'ABS':[43,44,60,63,64,65,68,69],
 'OBLIQUES':[70,71,93,94,96,98,99,100,101,102,103,104,113,115,116,119,133,136,142,143],
 'QUADS':[11,12,27,28,72,76,82,83],
 'HAMSTRINGS':[17,18,19,20,50,51,53,54],
 'GLUTES':[5,6,88,90], 'CALVES':[15,16,29,30,40,42],
}
assign={i:g for g,ids in groups.items() for i in ids}
assert len(assign)==sum(map(len,groups.values()))

def sample(d):
    p=parse_path(d)
    pts=[]
    for seg in p:
        n=max(2,int(seg.length()/0.65)+1)
        pts.extend((seg.point(i/n).real,seg.point(i/n).imag) for i in range(n))
    return Polygon(pts).buffer(0)

def polygon_d(p):
    return 'M'+' L'.join(f'{x:.2f},{y:.2f}' for x,y in p.exterior.coords)+' Z'

# Divide each existing shoulder cap with a narrow seam; preserve its outer contour.
extras=[]
for i,mirror in [(36,False),(38,True)]:
    poly=sample(rows[i]['d'])
    cut=parse_path('M376 180 C373 211 353 237 337 266')
    pts=[cut.point(t/80) for t in range(81)]
    line=LineString([(942-p.real if mirror else p.real,p.imag) for p in pts])
    pieces=list(poly.difference(line.buffer(1.15)).geoms)
    pieces.sort(key=lambda p:p.area,reverse=True)
    assert len(pieces)==2
    pieces.sort(key=lambda p:p.centroid.x)
    side,front=(pieces[1],pieces[0]) if mirror else (pieces[0],pieces[1])
    rows[i]['d']=polygon_d(side)
    extras.append(dict(id=f'front-delt-{i}',d=polygon_d(front),fill='#C1C7CC',group='FRONT_DELTS'))

# Medial thigh panels occupy the existing charcoal space beside the sartorius.
left='M443 475 Q454 485 459 500 Q457 530 451 562 Q450 523 443 475 Z'
for side in ['left','right']:
    p=parse_path(left)
    if side=='right':
        p=p.scaled(-1,1,origin=complex(471,0))
    extras.append(dict(id=f'adductor-{side}',d=p.d(),fill='#C1C7CC',group='ADDUCTORS'))

# Anatomical fascicle divisions: same heat group, with restrained curved seams.
# All cuts stay inside an approved region, so the outer physique cannot change.
detail_cuts=[
    (3, ['M370 223 Q419 203 478 233', 'M369 252 Q416 270 477 249'],942,4),
    (13,['M357 237 Q377 288 335 353'],942,14),
    (52,['M326 320 Q335 369 303 438'],940,55),
    (12,['M404 422 Q424 516 406 632'],942,11),
    (28,['M407 588 Q431 593 452 617'],942,27),
    (8,['M959 276 Q1000 282 1057 358','M965 309 Q1006 321 1038 387'],2120,7),
    (10,['M996 228 Q1030 247 1068 313'],2120,9),
    (56,['M966 217 Q995 224 1025 264'],2120,58),
    (46,['M953 234 Q964 281 945 339'],2120,47),
    (33,['M910 320 Q918 381 891 470'],2120,31),
    (6,['M977 484 Q1015 512 1067 486'],2120,5),
    (16,['M1006 656 Q1020 726 1001 792'],2120,15),
]
for left_id,cuts,axis_sum,right_id in detail_cuts:
    for row_id,mirror in [(left_id,False),(right_id,True)]:
        poly=sample(rows[row_id]['d'])
        for curve in cuts:
            p=parse_path(curve)
            points=[p.point(t/120) for t in range(121)]
            line=LineString([(axis_sum-v.real if mirror else v.real,v.imag) for v in points])
            poly=poly.difference(line.buffer(.8))
        pieces=list(poly.geoms) if hasattr(poly,'geoms') else [poly]
        pieces=[p for p in pieces if p.area>12]
        pieces.sort(key=lambda p:p.area,reverse=True)
        rows[row_id]['d']=polygon_d(pieces[0])
        for n,piece in enumerate(pieces[1:]):
            extras.append(dict(id=f'detail-{row_id}-{n}',d=polygon_d(piece),fill='#C1C7CC',group=assign[row_id]))

# Quiet untracked contours fill broad blank areas without inventing workout zones.
neutral_details=[
 ('head-front','M443 46 Q472 27 498 48 Q507 62 503 86 L497 113 Q487 133 472 139 Q453 129 445 111 L437 81 Q433 61 443 46 Z'),
 ('head-back','M1035 44 Q1061 29 1086 45 Q1098 57 1098 79 L1090 105 Q1078 115 1062 112 Q1044 115 1032 105 L1026 79 Q1024 59 1035 44 Z'),
 ('wrist-front-left','M302 466 L311 469 Q303 486 302 502 L298 514 Q292 501 297 483 Z'),
 ('palm-front-left','M306 487 Q313 480 319 490 L317 503 Q309 514 309 528 L315 538 Q303 537 301 522 L302 508 Z'),
 ('wrist-back-left','M891 465 L901 470 Q900 484 893 496 L888 511 Q882 499 887 482 Z'),
 ('hand-back-left','M903 486 Q911 489 911 500 L906 514 Q905 529 914 536 L910 542 Q894 536 896 520 Z'),
 ('ankle-front-left','M399 856 L415 857 Q418 874 411 887 L398 907 Q384 917 371 921 L384 902 Q400 882 399 856 Z'),
 ('foot-front-left','M368 924 Q389 921 407 909 Q410 925 397 931 L378 939 L360 940 L351 936 Z'),
 ('ankle-back-left','M980 876 L994 879 Q998 893 993 907 L994 928 Q982 938 969 931 L963 923 Q980 903 980 876 Z'),
 ('groin-left','M442 450 Q443 476 464 495 Q455 505 453 515 Q445 497 438 480 L430 463 Z'),
]
neutral_details += [(name.replace('left','right'),parse_path(d).scaled(-1,1,origin=complex(471 if '-front-' in name or name=='groin-left' else 1060,0)).d()) for name,d in neutral_details if name.endswith('left')]
occupied=unary_union([sample(r['d']) for r in rows+extras if r['fill']=='#C1C7CC'])
base=unary_union([sample(rows[i]['d']) for i in (0,1)])
for name,d in neutral_details:
    detail=sample(d).intersection(base.buffer(-1.6)).difference(occupied.buffer(1.4))
    pieces=list(detail.geoms) if hasattr(detail,'geoms') else [detail]
    for n,p in enumerate(pieces):
        if p.area>15:
            extras.append(dict(id=f'neutral-{name}-{n}',d=polygon_d(p),fill='#C1C7CC',group='NEUTRAL'))

ns='http://www.w3.org/2000/svg'
ET.register_namespace('',ns)
root=ET.Element('{'+ns+'}svg',{'viewBox':'0 0 1536 1024','role':'img','aria-labelledby':'title'})
ET.SubElement(root,'{'+ns+'}title',{'id':'title'}).text='TimeGo approved two-tone muscle map'
ET.SubElement(root,'{'+ns+'}desc').text='Approved lighter physique. Separate shoulder heads and medial thigh panels added for all 18 tracked anatomical groups. Neutral structural details do not represent training intensity.'
halves={side:ET.SubElement(root,'{'+ns+'}g',{'id':side,'data-viewbox':box}) for side,box in [('front','279 20 659 960'),('back','870 20 1250 960')]}
for row in rows+extras:
    p=parse_path(row['d'])
    x0,x1,y0,y1=p.bbox()
    side='front' if (x0+x1)/2<768 else 'back'
    group=row.get('group',assign.get(row['id'],'NEUTRAL'))
    # Exactly two source tones; preserve the existing heat-scale brightness convention.
    attrs={'id':f"{side}-{row['id']}",'data-muscle':group,'data-outline':str(row['fill']=='#20262C').lower(),
           'data-bounds':' '.join(f'{v:.3f}' for v in (x0,y0,x1,y1)),
           'fill':row['fill'],'d':re.sub(r'-?\d+\.\d+',lambda m:f'{float(m[0]):.3f}'.rstrip('0').rstrip('.'),row['d'])}
    ET.SubElement(halves[side],'{'+ns+'}path',attrs)
ET.indent(root)
ET.ElementTree(root).write(target/'bodymap.svg',encoding='utf-8',xml_declaration=True)
print('Wrote annotated SVG with',len(rows+extras),'paths')
