from pathlib import Path
import shutil
import xml.etree.ElementTree as ET
from PIL import Image, ImageDraw
import numpy as np
import vtracer
import cairosvg

OUT = Path(__file__).resolve().parent
SOURCE = Path('C:/Users/lsing/.codex/generated_images/01a07c55-438b-7d30-83b6-0a89ea8cb793/exec-cdd2a2ea-ad50-4f68-b3ea-8dd63a9029fc.png')
shutil.copy2(SOURCE, OUT/'original-drawing.png')
im = Image.open(SOURCE).convert('RGB')
pixels = np.array(im)
mask = Image.fromarray(np.where(pixels.min(axis=2)>235,255,0).astype('uint8')).copy()
for seed in [(0,0),(im.width-1,0),(0,im.height-1),(im.width-1,im.height-1)]:
    ImageDraw.floodfill(mask,seed,128)
alpha = np.where(np.array(mask)==128,0,255).astype('uint8')
rgba = im.convert('RGBA')
rgba.putalpha(Image.fromarray(alpha))
rgba.save(OUT/'drawing-transparent.png')
vtracer.convert_image_to_svg_py(str(OUT/'drawing-transparent.png'),str(OUT/'timego-bodymap-original.svg'),colormode='color',hierarchical='stacked',mode='spline',filter_speckle=1,color_precision=8,layer_difference=3,corner_threshold=60,length_threshold=2,max_iterations=10,splice_threshold=45,path_precision=2)
svg_path = OUT/'timego-bodymap-original.svg'
root = ET.parse(svg_path).getroot()
ns = '{http://www.w3.org/2000/svg}'
ET.register_namespace('', 'http://www.w3.org/2000/svg')
root.set('viewBox',f'0 0 {im.width} {im.height}')
title = ET.Element(ns+'title'); title.text='TimeGo — original anatomical illustration'
root.insert(0,title)
desc = ET.Element(ns+'desc'); desc.text='Original generated front and back fitness anatomy artwork, converted to editable cubic vector paths. Transparent exterior. Artistic concept; muscle identifiers and app integration are not yet assigned.'
root.insert(1,desc)
paths = root.findall('.//'+ns+'path')
for i,p in enumerate(paths):
    p.set('id',f'anatomy-detail-{i:04d}')
ET.ElementTree(root).write(svg_path,encoding='utf-8',xml_declaration=True)
assert not root.findall('.//'+ns+'image'), 'Raster payload is not permitted'
cairosvg.svg2png(url=str(svg_path),write_to=str(OUT/'vector-preview.png'),output_width=1536,output_height=1024,background_color='#15191d')
print(f'Vector paths: {len(paths)}; SVG bytes: {svg_path.stat().st_size}; no embedded raster images.')
