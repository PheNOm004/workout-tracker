from pathlib import Path
import shutil
import xml.etree.ElementTree as ET
import numpy as np
from PIL import Image, ImageDraw
import vtracer
import cairosvg

out = Path(__file__).resolve().parent
source = Path('C:/Users/lsing/.codex/generated_images/01a07c55-438b-7d30-83b6-0a89ea8cb793/exec-9dbb4ecc-bc82-4492-aea5-c998a68073ad.png')
shutil.copy2(source,out/'generated-source.png')
im = Image.open(source).convert('RGB')
p = np.array(im)
mask = Image.fromarray(np.where(p.min(axis=2)>230,255,0).astype('uint8')).copy()
for seed in [(0,0),(im.width-1,0),(0,im.height-1),(im.width-1,im.height-1)]:
    ImageDraw.floodfill(mask,seed,128)
alpha = np.where(np.array(mask)==128,0,255).astype('uint8')
colors = np.where((p.mean(axis=2)<120)[...,None],np.array([32,38,44]),np.array([193,199,204])).astype('uint8')
rgba = Image.fromarray(colors).convert('RGBA')
rgba.putalpha(Image.fromarray(alpha))
rgba.save(out/'two-tone-source.png')
target = out/'timego-reference-bodymap.svg'
vtracer.convert_image_to_svg_py(str(out/'two-tone-source.png'),str(target),colormode='color',hierarchical='stacked',mode='spline',filter_speckle=4,color_precision=8,layer_difference=1,corner_threshold=60,length_threshold=3,max_iterations=10,splice_threshold=45,path_precision=2)
ns='{http://www.w3.org/2000/svg}'
ET.register_namespace('','http://www.w3.org/2000/svg')
root = ET.parse(target).getroot()
root.set('viewBox',f'0 0 {im.width} {im.height}')
title = ET.Element(ns+'title'); title.text='TimeGo â€” two-tone athletic muscle map'
root.insert(0,title)
paths=root.findall('.//'+ns+'path')
for i,path in enumerate(paths):
    path.set('id',f'shape-{i:03d}')
    color = path.attrib['fill'].lstrip('#')
    average = sum(int(color[j:j+2],16) for j in (0,2,4))/3
    path.set('fill','#20262C' if average<120 else '#C1C7CC')
fills={p.attrib['fill'].upper() for p in paths}
assert fills=={'#20262C','#C1C7CC'},fills
assert not root.findall('.//'+ns+'image')
ET.ElementTree(root).write(target,encoding='utf-8',xml_declaration=True)
cairosvg.svg2png(url=str(target),write_to=str(out/'svg-preview.png'),output_width=1200,output_height=800,background_color='#FFFFFF')
print(f'Validated: {len(paths)} vector paths, exactly two fill colors, no embedded bitmap. {target.stat().st_size} bytes.')
