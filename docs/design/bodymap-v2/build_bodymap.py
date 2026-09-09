from pathlib import Path
import re
import xml.etree.ElementTree as ET
import cairosvg
import math

ROOT = Path(__file__).resolve().parents[3]
OUT = Path(__file__).resolve().parent
source = (ROOT / 'app/src/main/java/com/lsing/timego/ui/common/MuscleBodyArt.kt').read_text()
pattern = r'MusclePathSpec\("([^"]+)", (null|MuscleGroup\.\w+), isOutline = (true|false), lightness = ([\d.]+)f\)'
front, back = source.split('val BACK_BODY_PATHS', 1)
sets = [re.findall(pattern, front), re.findall(pattern, back)]

def vertices(d):
    tokens = re.findall(r'[a-zA-Z]|-?\d+(?:\.\d+)?', d)
    points, i, x, y, cmd = [], 0, 0, 0, ''
    while i < len(tokens):
        if tokens[i].isalpha():
            cmd = tokens[i]
            i += 1
            continue
        n = float(tokens[i])
        if cmd in 'Mm':
            x, y = n, float(tokens[i+1]); i += 2; cmd = 'l'
        elif cmd == 'l':
            x += n; y += float(tokens[i+1]); i += 2
        elif cmd == 'h':
            x += n; i += 1
        elif cmd == 'v':
            y += n; i += 1
        else:
            raise ValueError(cmd)
        if not points or points[-1] != (x,y):
            points.append((x,y))
    return points

def fmt(p):
    return f'{p[0]:.2f},{p[1]:.2f}'

def straight(points):
    return 'M' + ' L'.join(fmt(p) for p in points) + ' Z'

def simplify(points, epsilon):
    if len(points) < 3:
        return points
    a,b = points[0],points[-1]
    dx,dy = b[0]-a[0],b[1]-a[1]
    denom = dx*dx+dy*dy
    distances = []
    for p in points[1:-1]:
        t = max(0,min(1,((p[0]-a[0])*dx+(p[1]-a[1])*dy)/denom)) if denom else 0
        distances.append(math.hypot(p[0]-a[0]-t*dx,p[1]-a[1]-t*dy))
    maximum = max(distances)
    if maximum <= epsilon:
        return [a,b]
    k = distances.index(maximum)+1
    return simplify(points[:k+1],epsilon)[:-1]+simplify(points[k:],epsilon)

def curved(points):
    # Keep each original component; remove subpixel tracing stair-steps only.
    points = simplify(points + [points[0]], 1.25)[:-1]
    if len(points) < 3:
        return straight(points)
    # A restrained closed cubic spline follows the original anatomical boundary.
    # Tangents are capped so tiny serratus/hand pieces cannot develop loops.
    result = 'M'+fmt(points[0])
    for i,p in enumerate(points):
        before,after,next_after = points[i-1],points[(i+1)%len(points)],points[(i+2)%len(points)]
        cap = min(5.0,math.dist(p,after)*.22)
        def tangent(a,b):
            dx,dy = b[0]-a[0],b[1]-a[1]
            length = math.hypot(dx,dy)
            factor = min(.15,cap/length) if length else 0
            return dx*factor,dy*factor
        t1,t2 = tangent(before,after),tangent(p,next_after)
        c1,c2 = (p[0]+t1[0],p[1]+t1[1]),(after[0]-t2[0],after[1]-t2[1])
        result += ' C'+fmt(c1)+' '+fmt(c2)+' '+fmt(after)
    return result+' Z'

def svg_original():
    parts = ['<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1100 1480"><rect width="1100" height="1480" fill="#11151a"/>']
    for shapes, tx in zip(sets, [0, -170]):
        parts.append(f'<g transform="translate({tx} 20)">')
        for d, group, outline, light in shapes:
            v = round(85 + float(light)*130) if group != 'null' else (43 if outline == 'true' else 103)
            parts.append(f'<path d="{straight(vertices(d))}" fill="rgb({v},{v+5},{v+8})"/>')
        parts.append('</g>')
    return ''.join(parts) + '</svg>'

def refined():
    parts = ['<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1100 1440" role="img" aria-labelledby="title desc">',
             '<title id="title">TimeGo anatomical body map — contour study 02</title>',
             '<desc id="desc">Front and back anatomy. All 176 source components retained, with rounded vector contours. Muscle paths retain their existing TimeGo muscle-group mapping; neutral detail remains unassigned. Transparent background.</desc>']
    for side,shapes,tx in zip(['front','back'],sets,[0,-170]):
        parts.append(f'<g id="{side}" transform="translate({tx} 0)">')
        for i,(d,group,outline,light) in enumerate(shapes):
            name = group.split('.')[-1].lower() if group != 'null' else 'neutral'
            light = float(light)
            if group != 'null':
                # Neutral silver keeps the anatomical relief separate from heat intensity.
                v = round(87+light*161)
                fill = f'#{v:02x}{min(255,v+7):02x}{min(255,v+9):02x}'
            else:
                fill = '#353f48' if outline == 'true' else '#869299'
            path = curved(vertices(d))
            if side == 'front':
                # Replace incomplete tracing scraps with quiet, bilateral facial landmarks.
                face = {
                    81: 'M247 125 Q258 119 269 126 Q258 130 247 125 Z M295 126 Q306 119 317 125 Q306 130 295 126 Z',
                    94: 'M281 142 Q279 154 275 160 Q283 165 290 160 Q285 157 285 153 Q284 147 281 142 Z',
                    102: 'M270 179 Q282 176 295 179 Q283 182 270 179 Z',
                    104: 'M277 191 Q283 193 289 191 Q283 196 277 191 Z',
                }
                path = face.get(i,path)
            parts.append(f'<path id="{side}-{name}-{i:03d}" data-muscle-group="{name}" data-outline="{outline}" data-lightness="{light:.4f}" fill="{fill}" d="{path}"/>')
        parts.append('</g>')
    return '\n'.join(parts+['</svg>'])

if __name__ == '__main__':
    current = svg_original()
    (OUT / 'current-reference.svg').write_text(current)
    cairosvg.svg2png(bytestring=current.encode(), write_to=str(OUT/'current-reference.png'), output_width=770, output_height=1036)
    print('Shape counts:', [len(s) for s in sets])
    result = refined()
    ET.fromstring(result)
    (OUT/'timego-bodymap-v2.svg').write_text(result,encoding='utf-8')
    for side in ['front','back']:
        root = ET.fromstring(result)
        ns = '{http://www.w3.org/2000/svg}'
        for group in list(root.findall(ns+'g')):
            if group.attrib['id'] != side:
                root.remove(group)
            else:
                group.attrib.pop('transform', None)
        root.set('viewBox', '30 30 510 1410' if side == 'front' else '740 30 510 1380')
        ET.register_namespace('', 'http://www.w3.org/2000/svg')
        ET.ElementTree(root).write(OUT/f'timego-bodymap-v2-{side}.svg',encoding='utf-8',xml_declaration=True)
    cairosvg.svg2png(bytestring=result.encode(),write_to=str(OUT/'timego-bodymap-v2-preview.png'),output_width=880,output_height=1152,background_color='#11151a')
    print('SVG validated; front and back exports written.')
