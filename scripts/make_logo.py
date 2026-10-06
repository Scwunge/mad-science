"""Builds the mod logo (mods list) and a square icon (project page) from the mod's own textures.

Run from the project root: python scripts/make_logo.py
The syringe is tinted like a filled mutant DNA syringe; the lettering uses a system font only to rasterise the title.
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
ITEMS = ROOT / "src/main/resources/assets/madscience/textures/item"
GREEN, LIGHT_GREEN = (0x51, 0xA0, 0x3E), (0x7E, 0xBF, 0x6E)


def tinted(name, colour):
    img = Image.open(ITEMS / f"{name}.png").convert("RGBA")
    r, g, b, a = img.split()
    grey = Image.merge("RGB", (r, g, b)).convert("L")
    out = Image.new("RGBA", img.size)
    out.putdata([(v * colour[0] // 255, v * colour[1] // 255, v * colour[2] // 255, al) for v, al in zip(grey.get_flattened_data(), a.get_flattened_data())])
    return out


def syringe():
    img = Image.new("RGBA", (16, 16))
    for layer in (tinted("syringe_layer_1", GREEN), tinted("syringe_layer_2", LIGHT_GREEN),
                  Image.open(ITEMS / "syringe_overlay.png").convert("RGBA")):
        img.alpha_composite(layer)
    return img


def font(size):
    for name in ("impact.ttf", "arialbd.ttf"):
        try:
            return ImageFont.truetype(f"C:/Windows/Fonts/{name}", size)
        except OSError:
            continue
    return ImageFont.load_default()


def logo():
    text_font = font(72)
    x, y = 116, 18
    text_width = ImageDraw.Draw(Image.new("RGBA", (1, 1))).textbbox((0, 0), "MAD SCIENCE", font=text_font)[2]
    width, height = x + text_width + 12, 120
    img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    icon = syringe().resize((96, 96), Image.NEAREST)
    img.alpha_composite(icon, (8, 12))
    draw = ImageDraw.Draw(img)
    # a dark outline keeps the title readable on light and dark backgrounds
    for dx in (-3, 0, 3):
        for dy in (-3, 0, 3):
            draw.text((x + dx, y + dy), "MAD SCIENCE", font=text_font, fill=(20, 30, 20, 255))
    draw.text((x, y), "MAD SCIENCE", font=text_font, fill=(0x9C, 0xE0, 0x5A, 255))
    return img


def icon():
    img = Image.new("RGBA", (256, 256), (24, 32, 24, 255))
    img.alpha_composite(syringe().resize((224, 224), Image.NEAREST), (16, 16))
    return img


if __name__ == "__main__":
    logo().save(ROOT / "src/main/resources/madscience_logo.png")
    (ROOT / "docs").mkdir(exist_ok=True)
    icon().save(ROOT / "docs/icon.png")
    logo().save(ROOT / "docs/logo.png")
    print("wrote madscience_logo.png, docs/icon.png, docs/logo.png")
