"""Generates item models, lang, tags, recipes and sound definitions, and copies the original textures under their new names.

Run from the project root: python scripts/gen_assets.py
Texture copying needs the original source checked out in reference/ms-164 (local only); everything else is self-contained.
"""
import json
import shutil
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
ASSETS = RES / "assets/madscience"
DATA = RES / "data/madscience"
ORIG = ROOT / "reference/ms-164/src/main/resources/assets/madscience"
MOD = "madscience"

# species id -> (display name, has syringe, has sample)
SPECIES = {
    "bat": ("Bat", True, True),
    "cave_spider": ("Cave Spider", True, True),
    "chicken": ("Chicken", True, True),
    "cow": ("Cow", True, True),
    "creeper": ("Creeper", True, True),
    "enderman": ("Enderman", True, True),
    "ghast": ("Ghast", False, True),
    "horse": ("Horse", True, True),
    "mushroom_cow": ("Mooshroom", True, True),
    "ocelot": ("Ocelot", True, True),
    "pig": ("Pig", True, True),
    "pig_zombie": ("Zombified Piglin", False, False),
    "sheep": ("Sheep", True, True),
    "skeleton": ("Skeleton", False, True),
    "slime": ("Slime", False, True),
    "spider": ("Spider", True, True),
    "squid": ("Squid", True, True),
    "villager": ("Villager", True, True),
    "witch": ("Witch", True, True),
    "wolf": ("Wolf", True, True),
    "zombie": ("Zombie", True, True),
}

# entity types each syringe draws blood from (the original checked classes in this order, so subtypes are listed explicitly)
DNA_SOURCES = {
    "bat": ["minecraft:bat"],
    "cave_spider": ["minecraft:cave_spider"],
    "chicken": ["minecraft:chicken"],
    "cow": ["minecraft:cow"],
    "creeper": ["minecraft:creeper"],
    "enderman": ["minecraft:enderman"],
    "horse": ["minecraft:horse", "minecraft:donkey", "minecraft:mule", "minecraft:skeleton_horse", "minecraft:zombie_horse"],
    "mushroom_cow": ["minecraft:mooshroom"],
    "ocelot": ["minecraft:ocelot", "minecraft:cat"],
    "pig": ["minecraft:pig"],
    "sheep": ["minecraft:sheep"],
    "spider": ["minecraft:spider"],
    "squid": ["minecraft:squid", "minecraft:glow_squid"],
    "villager": ["minecraft:villager", "minecraft:wandering_trader"],
    "witch": ["minecraft:witch"],
    "wolf": ["minecraft:wolf"],
    "zombie": ["minecraft:zombie", "minecraft:zombie_villager", "minecraft:husk", "minecraft:drowned"],
    "mutant": ["minecraft:zombified_piglin"],
}

# simple items: id -> (original texture name, English name, lore tooltip from the original)
SIMPLE = {
    "component_case": ("componentCase", "Case Component", 'A computer case, also known as a "computer chassis", "tower" or simply "case", is the enclosure that contains most of the components of a computer.'),
    "component_cpu": ("componentCPU", "CPU Component", "A central processing unit (CPU) is the hardware within a computer that carries out the instructions of a computer program by performing the basic arithmetical, logical, and input/output operations of the system."),
    "component_fan": ("componentFan", "Fan Component", "A computer fan is any fan inside, or attached to, a computer case used for active cooling."),
    "component_fused_quartz": ("componentFusedQuartz", "Fused Quartz Component", "Fused quartz is glass consisting of silica in amorphous (non-crystalline) form. Its optical and thermal properties are superior to those of other types of glass due to its purity."),
    "component_magnetic_tape": ("componentMagneticTape", "Magnetic Tape Component", "Magnetic tape data storage uses digital recording on magnetic tape to store digital information. Modern magnetic tape is most commonly packaged in data reels."),
    "component_power_supply": ("componentPowerSupply", "Power Supply Component", "Provides consistent and clean power to machines and computer equipment."),
    "component_ram": ("componentRAM", "RAM Component", "Random-access memory (RAM) is a form of computer data storage. A random-access device allows stored data to be accessed directly in any random order."),
    "component_screen": ("componentScreen", "Screen Component", "A monitor or a display is an electronic visual display for computers."),
    "component_silicon_wafer": ("componentSiliconWafer", "Silicon Wafer Component", "A wafer is a thin slice of semiconductor material, such as a silicon crystal, used in the fabrication of integrated circuits and other microdevices."),
    "component_transistor": ("componentTransistor", "Transistor Component", "A transistor is a semiconductor device used to amplify and switch electronic signals and electrical power."),
    "component_computer": ("componentComputer", "Computer Component", "A computer is a general purpose device that can be programmed to carry out a set of arithmetic or logical operations."),
    "component_thumper": ("componentThumper", "Thumper", "Used in the recipe for the Soniclocator. The Soniclocator builds up energy in the goop inside these thumpers, which was collected from the Enderslime mob."),
    "component_enderslime": ("componentEnderslime", "Enderslime", "Used in the creation of a Thumper which then goes into your Soniclocator."),
    "circuit_comparator": ("circuitComparator", "Comparator Circuit Component", "Enables electronic devices to compare items to other items."),
    "circuit_diamond": ("circuitDiamond", "Diamond Circuit Component", "Advanced circuit used as central core in several machines."),
    "circuit_emerald": ("circuitEmerald", "Emerald Circuit Component", "Advanced circuit that is used in the creation of some of the most scientifically accurate equipment known to Minecraft."),
    "circuit_ender_eye": ("circuitEnderEye", "Ender Eye Circuit Component", "Advanced circuit that controls sorting data inside of machines. Using the same technology that guides you to Strongholds, this circuit can move huge data-sets around and know exactly where they should go."),
    "circuit_ender_pearl": ("circuitEnderPearl", "Ender Pearl Circuit Component", "Very powerful monitoring circuit that can hold data in a seemingly infinite buffer thanks to the magic of Enderman teleportation technology."),
    "circuit_glowstone": ("circuitGlowstone", "Glowstone Circuit Component", "Unique integrated-circuit-like semiconductor amplifying device."),
    "circuit_redstone": ("circuitRedstone", "Redstone Circuit Component", "Creates a multi-purpose circuit that accepts digital data as input, processes it according to instructions stored in its memory, and provides results as output."),
    "circuit_spider_eye": ("circuitSpiderEye", "Spider Eye Circuit Component", "Sensitive circuit that is used in monitoring and analysis of items not directly visible to the naked eye."),
    "syringe_empty": ("needleEmpty", "Empty Syringe", "Collects DNA samples from mobs or yourself! Hit a mob with it, or use it to draw your own blood."),
    "syringe_dirty": ("needleDirty", "Dirty Syringe", "Don't play with dirty needles!"),
}

# texture copies: original name -> new name (items/)
LAYER_TEXTURES = {
    "needleDNA1": "syringe_layer_1", "needleDNA2": "syringe_layer_2", "needleDNA_overlay": "syringe_overlay",
    "dnaSample": "dna_sample", "dnaSample_overlay": "dna_sample_overlay",
    "genomeDataReel1": "data_reel_layer_1", "genomeDataReel2": "data_reel_layer_2", "genomeDataReel_overlay": "data_reel_overlay",
}

lang = {}
generated = []


def write_json(path: Path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")
    generated.append(path)


def item_model(name, layers):
    write_json(ASSETS / f"models/item/{name}.json",
               {"parent": "minecraft:item/generated", "textures": {f"layer{i}": f"{MOD}:item/{tex}" for i, tex in enumerate(layers)}})


def copy_texture(src: Path, dst: Path):
    """Copies a texture, padding odd sizes (some originals are 15x15) up to the next multiple of 16 so mipmaps work."""
    dst.parent.mkdir(parents=True, exist_ok=True)
    img = Image.open(src)
    w, h = img.size
    pw, ph = -(-w // 16) * 16, -(-h // 16) * 16
    if (pw, ph) == (w, h):
        shutil.copyfile(src, dst)
        return
    padded = Image.new("RGBA", (pw, ph), (0, 0, 0, 0))
    padded.paste(img.convert("RGBA"), ((pw - w) // 2, (ph - h) // 2))
    padded.save(dst)


def copy_textures():
    if not ORIG.exists():
        print("reference/ms-164 not found, skipping texture copy")
        return
    out = ASSETS / "textures/item"
    for new, (old, _, _) in SIMPLE.items():
        copy_texture(ORIG / f"textures/items/{old}.png", out / f"{new}.png")
    for old, new in LAYER_TEXTURES.items():
        copy_texture(ORIG / f"textures/items/{old}.png", out / f"{new}.png")


def items():
    for name, (_, english, lore) in SIMPLE.items():
        item_model(name, [name])
        lang[f"item.{MOD}.{name}"] = english
        lang[f"item.{MOD}.{name}.tooltip"] = lore

    syringe_layers = ["syringe_layer_1", "syringe_layer_2", "syringe_overlay"]
    reel_layers = ["data_reel_layer_1", "data_reel_layer_2", "data_reel_overlay"]
    sample_layers = ["dna_sample_overlay", "dna_sample"]

    item_model("syringe_mutant", syringe_layers)
    lang[f"item.{MOD}.syringe_mutant"] = "Filled Syringe of Mutant DNA"
    lang[f"item.{MOD}.syringe_mutant.tooltip"] = "Corrupted DNA from a genetically altered organism, or a pre-existing freak of nature."
    item_model("data_reel_empty", reel_layers)
    lang[f"item.{MOD}.data_reel_empty"] = "Empty Data Reel"
    lang[f"item.{MOD}.data_reel_empty.tooltip"] = "Magnetic tape data storage. This reel is empty and may have any sort of data imprinted onto it."

    for sid, (english, has_syringe, has_sample) in SPECIES.items():
        if has_syringe:
            item_model(f"syringe_{sid}", syringe_layers)
            lang[f"item.{MOD}.syringe_{sid}"] = f"Filled Syringe of {english} DNA"
            lang[f"item.{MOD}.syringe_{sid}.tooltip"] = "Blood drawn from a living creature. Goes bad over time unless kept in a Cryogenic Freezer."
        if has_sample:
            item_model(f"dna_{sid}", sample_layers)
            lang[f"item.{MOD}.dna_{sid}"] = f"Sample of {english} DNA"
            lang[f"item.{MOD}.dna_{sid}.tooltip"] = "Extracted DNA. Feed it to a Gene Sequencer to build up a genome. Goes bad over time unless kept cold."
        item_model(f"genome_{sid}", reel_layers)
        lang[f"item.{MOD}.genome_{sid}"] = f"Sequenced {english} Genome"
        lang[f"item.{MOD}.genome_{sid}.tooltip"] = "Genome data reel. Each matching DNA sample sequenced onto it brings it closer to completion."


def tags():
    for sid, types in DNA_SOURCES.items():
        write_json(DATA / f"tags/entity_type/dna_source/{sid}.json", {"replace": False, "values": types})


def shaped(name, result, pattern, key, count=1):
    write_json(DATA / f"recipe/{name}.json", {
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
        "key": {k: ({"tag": v[1:]} if v.startswith("#") else {"item": v}) for k, v in key.items()},
        "result": {"id": result, "count": count}})


def shapeless(name, result, ingredients, count=1):
    write_json(DATA / f"recipe/{name}.json", {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [({"tag": v[1:]} if v.startswith("#") else {"item": v}) for v in ingredients],
        "result": {"id": result, "count": count}})


def smelting(name, result, ingredient, count=1, xp=0.1):
    write_json(DATA / f"recipe/{name}.json", {
        "type": "minecraft:smelting", "category": "misc",
        "ingredient": {"tag": ingredient[1:]} if ingredient.startswith("#") else {"item": ingredient},
        "result": {"id": result, "count": count}, "experience": xp, "cookingtime": 200})


def recipes():
    m = lambda n: f"{MOD}:{n}"
    transistor = m("component_transistor")
    for circuit, center in {
        "comparator": "minecraft:comparator", "diamond": "#c:gems/diamond", "emerald": "#c:gems/emerald",
        "ender_eye": "minecraft:ender_eye", "ender_pearl": "#c:ender_pearls", "glowstone": "#c:dusts/glowstone",
        "redstone": "#c:dusts/redstone", "spider_eye": "minecraft:spider_eye",
    }.items():
        shaped(f"circuit_{circuit}", m(f"circuit_{circuit}"), ["TTT", "TCT", "TTT"], {"T": transistor, "C": center})

    shaped("component_case", m("component_case"), ["121", "2 2", "121"], {"1": "#c:ingots/iron", "2": "#c:rods/wooden"})
    shaped("component_computer", m("component_computer"), ["ECE", "FBD", "EAE"], {
        "A": m("component_screen"), "B": m("component_cpu"), "C": m("component_fan"),
        "D": m("component_power_supply"), "E": m("component_case"), "F": m("component_ram")})
    shaped("component_fan", m("component_fan"), ["121", "222", "121"], {"1": m("component_case"), "2": "#c:ingots/iron"})
    smelting("component_fused_quartz_from_quartz", m("component_fused_quartz"), "#c:gems/quartz")
    smelting("component_fused_quartz_from_quartz_block", m("component_fused_quartz"), "minecraft:quartz_block", count=4)
    shapeless("component_fused_quartz_from_fire_charge", m("component_fused_quartz"), ["minecraft:fire_charge", "#minecraft:sand"], count=8)
    shaped("component_magnetic_tape", m("component_magnetic_tape"), ["111", "222"], {"1": "#c:dusts/redstone", "2": "#c:slime_balls"})
    shaped("component_power_supply", m("component_power_supply"), ["141", "323", "141"], {
        "1": "#c:ingots/iron", "2": m("circuit_redstone"), "3": transistor, "4": "#c:storage_blocks/redstone"})
    shaped("component_screen", m("component_screen"), ["444", "333", "212"], {
        "1": m("circuit_diamond"), "2": m("circuit_redstone"), "3": transistor, "4": "#c:glass_blocks/colorless"})
    shaped("data_reel_empty", m("data_reel_empty"), ["111", "121", "111"], {"1": m("component_magnetic_tape"), "2": m("circuit_emerald")})


def sounds():
    # Vanilla stand-ins until the user confirms the original sounds are covered by the permission (see PERMISSION.md).
    defs = {
        "syringe.stab_player": "minecraft:entity.bee.sting",
        "syringe.stab_mob": "minecraft:entity.bee.sting",
    }
    write_json(ASSETS / "sounds.json", {name: {"sounds": [{"name": event, "type": "event"}]} for name, event in defs.items()})


def nbt_payload(tag_type, value):
    """Minimal big-endian NBT encoder: compound=dict, list=('list', type, items), int=int, string=str."""
    import struct
    if tag_type == 3:
        return struct.pack(">i", value)
    if tag_type == 8:
        raw = value.encode("utf-8")
        return struct.pack(">H", len(raw)) + raw
    if tag_type == 9:
        _, item_type, items = value
        return struct.pack(">bi", item_type if items else 0, len(items)) + b"".join(nbt_payload(item_type, i) for i in items)
    if tag_type == 10:
        out = b""
        for key, (t, v) in value.items():
            raw = key.encode("utf-8")
            out += struct.pack(">bH", t, len(raw)) + raw + nbt_payload(t, v)
        return out + b"\x00"
    raise ValueError(tag_type)


def empty_structure(name, size=3):
    """An all-air structure template for GameTests."""
    import gzip
    import struct
    root = {
        "DataVersion": (3, 3955),
        "size": (9, ("list", 3, [size, size, size])),
        "palette": (9, ("list", 10, [{"Name": (8, "minecraft:air")}])),
        "blocks": (9, ("list", 10, [])),
        "entities": (9, ("list", 10, [])),
    }
    data = struct.pack(">bH", 10, 0) + nbt_payload(10, root)
    path = DATA / f"structure/{name}.nbt"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(gzip.compress(data, mtime=0))
    generated.append(path)


def misc_lang():
    lang["itemGroup.madscience"] = "Mad Science"
    lang["tooltip.madscience.hold_shift"] = "Hold SHIFT for more information."


copy_textures()
items()
tags()
recipes()
sounds()
misc_lang()
empty_structure("gametest/empty")
write_json(ASSETS / "lang/en_us.json", dict(sorted(lang.items())))
print(f"wrote {len(generated)} files")
