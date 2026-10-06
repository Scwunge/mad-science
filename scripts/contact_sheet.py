"""Tiles harness screenshots into one image for quick review. Usage: python scripts/contact_sheet.py <glob> <out.png> [cols] [scale]"""
import sys
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
shots = sorted((ROOT / "runs/client/screenshots").glob(sys.argv[1]))
out = Path(sys.argv[2])
cols = int(sys.argv[3]) if len(sys.argv) > 3 else 3
scale = float(sys.argv[4]) if len(sys.argv) > 4 else 0.5
images = [Image.open(p).convert("RGB") for p in shots]
w, h = int(images[0].width * scale), int(images[0].height * scale)
rows = (len(images) + cols - 1) // cols
sheet = Image.new("RGB", (cols * w, rows * (h + 14)), "white")
draw = ImageDraw.Draw(sheet)
for i, (img, path) in enumerate(zip(images, shots)):
    x, y = (i % cols) * w, (i // cols) * (h + 14)
    sheet.paste(img.resize((w, h)), (x, y + 14))
    draw.text((x + 2, y + 1), path.stem.replace("madscience-", ""), fill="black")
sheet.save(out)
print(f"{len(images)} shots -> {out}")
