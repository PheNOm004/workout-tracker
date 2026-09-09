from pathlib import Path
import xml.etree.ElementTree as ET
import re, json
from svgpathtools import parse_path
from PIL import Image, ImageDraw, ImageFont

out = Path(__file__).resolve().parent
root = ET.parse(out/'timego-reference-lighter-bodymap.svg').getroot()
im=Image.open(out/'svg-preview.png').convert('RGB').resize((1536,1024))
draw=ImageDraw.Draw(im)
font=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',12)
rows=[]
for el in root.findall('{http://www.w3.org/2000/svg}path'):
    p=parse_path(el.attrib['d'])
    nums=re.findall(r'-?\d+(?:\.\d+)?',el.attrib.get('transform','translate(0 0)'))
    x,y=map(float,nums)
    p=p.translated(complex(x,y))
    x0,x1,y0,y1=p.bbox()
    n=int(el.attrib['id'].split('-')[-1])
    rows.append(dict(id=n,fill=el.attrib['fill'],bounds=[round(v,1) for v in (x0,y0,x1,y1)],area=round(abs(p.area()),1),d=p.d()))
    if el.attrib['fill']=='#C1C7CC':
        cx,cy=(x0+x1)/2,(y0+y1)/2
        draw.text((cx,cy),str(n),fill='#dc143c',font=font,anchor='mm',stroke_width=1,stroke_fill='white')
im.save(out/'region-ids.png')
(out/'regions.json').write_text(json.dumps(rows,indent=2))
print('\n'.join(f"{r['id']:3} {r['fill']} {r['bounds']} area={r['area']}" for r in rows))
