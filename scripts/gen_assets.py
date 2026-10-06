"""Generates item models, lang, tags, recipes and sound definitions, and copies the original textures under their new names.

Run from the project root: python scripts/gen_assets.py
Texture copying needs the original source checked out in reference/ms-164 (local only); everything else is self-contained.
"""
import json
import re
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
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8", newline="\n")
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


FLUIDS = {
    # id: (original texture prefix, bucket texture, English name, bucket lore)
    "liquid_dna": ("maddna", "madDNABucket", "Liquid DNA", "Filled bucket of liquid DNA from an organism that is not a mutant."),
    "mutant_dna": ("maddnamutant", "madDNAMutantBucket", "Liquid Mutant DNA",
                   "Filled bucket of corrosive mutant DNA. Highly incompatible with organisms that are not mutants. Use extreme caution when handling!"),
}


def fluids():
    for fid, (orig, bucket_tex, english, lore) in FLUIDS.items():
        if ORIG.exists():
            for kind in ("still", "flowing"):
                copy_texture(ORIG / f"textures/blocks/{orig}_{kind}.png", ASSETS / f"textures/block/{fid}_{kind}.png")
                shutil.copyfile(ORIG / f"textures/blocks/{orig}_{kind}.png.mcmeta", ASSETS / f"textures/block/{fid}_{kind}.png.mcmeta")
            copy_texture(ORIG / f"textures/items/{bucket_tex}.png", ASSETS / f"textures/item/{fid}_bucket.png")
        write_json(ASSETS / f"blockstates/{fid}.json", {"variants": {"": {"model": f"{MOD}:block/{fid}"}}})
        write_json(ASSETS / f"models/block/{fid}.json", {"textures": {"particle": f"{MOD}:block/{fid}_still"}})
        item_model(f"{fid}_bucket", [f"{fid}_bucket"])
        lang[f"fluid_type.{MOD}.{fid}"] = english
        lang[f"block.{MOD}.{fid}"] = english
        lang[f"item.{MOD}.{fid}_bucket"] = f"Bucket of {english}"
        lang[f"item.{MOD}.{fid}_bucket.tooltip"] = lore
        write_json(RES / f"data/c/tags/fluid/{fid}.json", {"replace": False, "values": [f"{MOD}:{fid}", f"{MOD}:{fid}_flowing"]})


# machines: id -> (original internal name, English name, lore)
MACHINES = {
    "dna_extractor": ("dnaExtractor", "DNA Extractor", "Extracts DNA samples from filled syringes."),
    "sanitizer": ("needleSanitizer", "Syringe Sanitizer", "Cleans dirty needles so they can be reused."),
    "sequencer": ("genomeSequencer", "Gene Sequencer", "Reads DNA samples into data reels to create sequenced genomes."),
    "mainframe": ("computerMainframe", "Computer Mainframe",
                  "Combines multiple sequenced genomes together to make new lifeforms. Needs power, a redstone signal and water to keep cool."),
    "incubator": ("genomeIncubator", "Genome Incubator",
                  "Reads sequenced genomes and encodes this information onto chicken eggs. Needs power and a redstone signal to heat up."),
    "freezer": ("cryoFreezer", "Cryogenic Freezer", "Restores filled syringes and DNA samples to full health. Runs on snow and ice."),
    "duplicator": ("dataDuplicator", "Data Reel Duplicator", "Creates copies of any data reel. Needs a redstone signal."),
    "thermosonic_bonder": ("thermosonicBonder", "Thermosonic Bonder",
                           "Used to electrically interconnect the all-important Silicon Wafers, Transistors, CPUs and RAM chips. Needs power and a redstone signal to heat up."),
    "cryotube": ("cryoTube", "Cryogenic Tube", "Harvests electrical power from villagers' memories! Needs a redstone signal, a villager spawn egg, a data reel and a nether star."),
    "clay_furnace": ("clayFurnace", "Clay Furnace",
                     "Ancient technology that can give a source block from a single ore. Requires a block of coal and to be lit on fire."),
}

# combined genomes: id -> (English name, parent genome pairs, Mainframe ticks)
GMOS = {
    "werewolf": ("Werewolf", [("villager", "wolf")], 2600),
    "meat_cube": ("Disgusting Meat Cube", [("slime", "cow"), ("slime", "pig"), ("slime", "chicken")], 2600),
    "creeper_cow": ("Creeper Cow", [("creeper", "cow")], 2600),
    "enderslime": ("Enderslime", [("enderman", "slime")], 2600),
    "wooly_cow": ("Wooly Cow", [("cow", "sheep")], 2600),
    "shoggoth": ("Shoggoth", [("slime", "squid")], 2600),
    "abomination": ("Abomination", [("enderman", "spider")], 2600),
    "wither_skeleton": ("Wither Skeleton", [("enderman", "skeleton")], 2600),
    "zombie_villager": ("Zombie Villager", [("villager", "zombie")], 2600),
    "skeleton_horse": ("Skeleton Horse", [("horse", "skeleton")], 2600),
    "zombie_horse": ("Zombie Horse", [("zombie", "horse")], 2600),
    "ender_squid": ("Ender Squid", [("enderman", "squid")], 2600),
}
# what the Incubator hatches from each genome (custom creatures are added with their entities)
VANILLA_EGGS = {
    "bat": "bat", "cave_spider": "cave_spider", "chicken": "chicken", "cow": "cow", "creeper": "creeper", "enderman": "enderman",
    "ghast": "ghast", "horse": "horse", "mushroom_cow": "mooshroom", "ocelot": "ocelot", "pig": "pig", "pig_zombie": "zombified_piglin",
    "sheep": "sheep", "skeleton": "skeleton", "slime": "slime", "spider": "spider", "squid": "squid", "villager": "villager",
    "witch": "witch", "wolf": "wolf", "zombie": "zombie",
    "wither_skeleton": "wither_skeleton", "zombie_villager": "zombie_villager", "skeleton_horse": "skeleton_horse", "zombie_horse": "zombie_horse",
}
# custom creature results: genome id -> result item
GMO_RESULTS = {gid: f"madscience:{gid}_spawn_egg" for gid in
               ("werewolf", "creeper_cow", "enderslime", "wooly_cow", "shoggoth", "abomination", "ender_squid")}

# creatures: id -> (original model folder, English name)
ENTITIES = {
    "werewolf": ("gmoWerewolf", "Werewolf"),
    "creeper_cow": ("gmoCreeperCow", "Creeper Cow"),
    "enderslime": ("gmoEnderslime", "Enderslime"),
    "wooly_cow": ("gmoWoolyCow", "Wooly Cow"),
    "shoggoth": ("gmoShoggoth", "Shoggoth"),
    "abomination": ("gmoAbomination", "Abomination"),
    "ender_squid": ("gmoEnderSquid", "Ender Squid"),
}
DYES = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue",
        "brown", "green", "red", "black"]


def entry(item, lo=0, hi=2, looting=True, smelt=False, conditions=None):
    functions = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    if smelt:
        functions.append({"function": "minecraft:furnace_smelt", "conditions": [
            {"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"flags": {"is_on_fire": True}}}]})
    if looting:
        functions.append({"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting",
                          "count": {"type": "minecraft:uniform", "min": 0, "max": 1}})
    pool = {"rolls": 1, "entries": [{"type": "minecraft:item", "name": item, "functions": functions}]}
    if conditions:
        pool["conditions"] = conditions
    return pool


SLIME_SIZE_1 = [{"condition": "minecraft:entity_properties", "entity": "this",
                 "predicate": {"type_specific": {"type": "minecraft:slime", "size": 1}}}]
PLAYER_KILL = [{"condition": "minecraft:killed_by_player"}]


def entities():
    for eid, (orig, english) in ENTITIES.items():
        if ORIG.exists():
            for png in (ORIG / f"models/{orig}").glob("*.png"):
                name = png.stem.replace(orig, eid).lower()
                copy_texture(png, ASSETS / f"textures/entity/{eid}/{name}.png")
        lang[f"entity.{MOD}.{eid}"] = english
        lang[f"item.{MOD}.{eid}_spawn_egg"] = f"{english} Spawn Egg"
        write_json(ASSETS / f"models/item/{eid}_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})
    loot = lambda name, pools: write_json(DATA / f"loot_table/entities/{name}.json", {"type": "minecraft:entity", "pools": pools})
    loot("werewolf", [entry("minecraft:egg")])
    loot("creeper_cow", [entry("minecraft:leather"), entry("minecraft:gunpowder"), entry("minecraft:beef", 1, 3, smelt=True),
                         {"rolls": 1, "entries": [{"type": "minecraft:tag", "name": "minecraft:creeper_drop_music_discs", "expand": True}],
                          "conditions": [{"condition": "minecraft:entity_properties", "entity": "attacker",
                                          "predicate": {"type": "#minecraft:skeletons"}}]}])
    loot("abomination", [entry("minecraft:string"),
                         {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:spider_eye"}],
                          "conditions": PLAYER_KILL + [{"condition": "minecraft:random_chance_with_enchanted_bonus", "enchantment": "minecraft:looting",
                                                        "unenchanted_chance": 0.33, "enchanted_chance": {"type": "minecraft:linear", "base": 0.5, "per_level_above_first": 0.1}}]},
                         {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:ender_pearl"}],
                          "conditions": PLAYER_KILL + [{"condition": "minecraft:random_chance_with_enchanted_bonus", "enchantment": "minecraft:looting",
                                                        "unenchanted_chance": 0.33, "enchanted_chance": {"type": "minecraft:linear", "base": 0.5, "per_level_above_first": 0.1}}]}])
    loot("enderslime", [entry(f"{MOD}:component_enderslime", conditions=SLIME_SIZE_1)])
    loot("shoggoth", [entry("minecraft:slime_ball", conditions=SLIME_SIZE_1)])
    loot("ender_squid", [entry("minecraft:ender_pearl", 0, 1)])
    base = [entry("minecraft:leather"), entry("minecraft:beef", 1, 3, smelt=True)]
    loot("wooly_cow", base)
    for dye in DYES:
        loot(f"wooly_cow/{dye}", [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"minecraft:{dye}_wool"}]}]
             + [{"rolls": 1, "entries": [{"type": "minecraft:loot_table", "value": f"{MOD}:entities/wooly_cow"}]}])


def simple_blocks():
    if ORIG.exists():
        copy_texture(ORIG / "textures/blocks/abominationEgg.png", ASSETS / "textures/block/abomination_egg.png")
        copy_texture(ORIG / "textures/blocks/enderslimeBlock.png", ASSETS / "textures/block/enderslime_block.png")
    write_json(ASSETS / "blockstates/abomination_egg.json", {"variants": {"": {"model": f"{MOD}:block/abomination_egg"}}})
    write_json(ASSETS / "models/block/abomination_egg.json", {"parent": "minecraft:block/dragon_egg",
                                                              "textures": {"all": f"{MOD}:block/abomination_egg", "particle": f"{MOD}:block/abomination_egg"}})
    write_json(ASSETS / "models/item/abomination_egg.json", {"parent": f"{MOD}:block/abomination_egg"})
    write_json(ASSETS / "blockstates/enderslime_block.json", {"variants": {"": {"model": f"{MOD}:block/enderslime_block"}}})
    write_json(ASSETS / "models/block/enderslime_block.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MOD}:block/enderslime_block"}})
    write_json(ASSETS / "models/item/enderslime_block.json", {"parent": f"{MOD}:block/enderslime_block"})
    write_json(DATA / "loot_table/blocks/abomination_egg.json", {"type": "minecraft:block", "pools": []})
    write_json(DATA / "loot_table/blocks/enderslime_block.json", {"type": "minecraft:block", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{MOD}:enderslime_block"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    lang[f"block.{MOD}.abomination_egg"] = "Abomination Egg"
    lang[f"block.{MOD}.abomination_egg.tooltip"] = "Spawned into the world when The Abomination kills another living creature, mob or player alike."
    lang[f"block.{MOD}.enderslime_block"] = "Block of Enderslime"
    lang[f"block.{MOD}.enderslime_block.tooltip"] = "Oh god, it's slimy..."
    m = lambda n: f"{MOD}:{n}"
    shaped("enderslime_block", m("enderslime_block"), ["111", "111", "111"], {"1": m("component_enderslime")})
    shapeless("component_enderslime_from_block", m("component_enderslime"), [m("enderslime_block")], count=9)
    shaped("component_thumper", m("component_thumper"), ["535", "212", "222"], {
        "1": m("component_power_supply"), "2": m("enderslime_block"), "3": "#c:storage_blocks/redstone", "5": "minecraft:piston"})

# machines more than one block tall (the original's "ghost blocks")
TALL_MACHINES = {"cryotube": 3, "soniclocator": 3, "magazine_loader": 2, "cnc_machine": 2}

# Item display transforms for machine items drawn by the block entity model renderer.
MACHINE_ITEM_DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
}


def machines():
    pickaxe = []
    for mid, (orig, english, lore) in MACHINES.items():
        if ORIG.exists():
            for png in (ORIG / f"models/{orig}").glob("*.png"):
                copy_texture(png, ASSETS / f"textures/model/{mid}/{png.name.lower()}")
            copy_texture(ORIG / f"textures/gui/{orig}.png", ASSETS / f"textures/gui/{mid}.png")
            copy_texture(ORIG / f"textures/blocks/{orig}.png", ASSETS / f"textures/block/{mid}.png")
        tall = TALL_MACHINES.get(mid, 1)
        if tall > 1:
            write_json(ASSETS / f"blockstates/{mid}.json", {"variants": {f"part={i}": {"model": f"{MOD}:block/{mid}"} for i in range(3)}})
        else:
            write_json(ASSETS / f"blockstates/{mid}.json", {"variants": {"": {"model": f"{MOD}:block/{mid}"}}})
        write_json(ASSETS / f"models/block/{mid}.json", {"textures": {"particle": f"{MOD}:block/{mid}"}})
        write_json(ASSETS / f"models/item/{mid}.json", {"parent": "minecraft:builtin/entity", "gui_light": "side",
                                                        "textures": {"particle": f"{MOD}:block/{mid}"}, "display": MACHINE_ITEM_DISPLAY})
        # the original Clay Furnace crumbles back to hardened clay when broken
        drop = "minecraft:terracotta" if mid == "clay_furnace" else f"{MOD}:{mid}"
        write_json(DATA / f"loot_table/blocks/{mid}.json", {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": drop}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}] + ([
                           {"condition": "minecraft:block_state_property", "block": f"{MOD}:{mid}", "properties": {"part": "0"}}] if tall > 1 else [])}]})
        lang[f"block.{MOD}.{mid}"] = english
        lang[f"block.{MOD}.{mid}.tooltip"] = lore
        pickaxe.append(f"{MOD}:{mid}")
    write_json(RES / "data/minecraft/tags/block/mineable/pickaxe.json", {"replace": False, "values": pickaxe})


def machine_recipes():
    m = lambda n: f"{MOD}:{n}"
    shaped("dna_extractor", m("dna_extractor"), ["414", "424", "434"], {
        "1": m("circuit_ender_eye"), "2": m("circuit_spider_eye"), "3": m("component_computer"), "4": m("component_case")})
    shaped("sanitizer", m("sanitizer"), ["545", "535", "126"], {
        "1": m("circuit_glowstone"), "2": m("circuit_redstone"), "3": m("component_power_supply"), "4": m("component_fan"),
        "5": m("component_case"), "6": m("circuit_ender_pearl")})
    shaped("sequencer", m("sequencer"), ["172", "858", "364"], {
        "1": m("circuit_emerald"), "2": m("circuit_comparator"), "3": m("circuit_diamond"), "4": m("circuit_ender_eye"),
        "5": m("component_computer"), "6": m("component_power_supply"), "7": m("component_fan"), "8": m("component_case")})
    shaped("mainframe", m("mainframe"), ["111", "121", "111"], {"1": m("component_computer"), "2": m("component_case")})
    shaped("incubator", m("incubator"), ["656", "142", "636"], {
        "1": m("circuit_glowstone"), "2": m("circuit_comparator"), "3": m("component_power_supply"), "4": m("component_computer"),
        "5": m("component_fan"), "6": m("component_case")})
    shaped("freezer", m("freezer"), ["131", "121", "141"], {
        "1": m("component_case"), "2": m("component_computer"), "3": m("circuit_comparator"), "4": m("component_fan")})
    shaped("duplicator", m("duplicator"), ["161", "232", "454"], {
        "1": m("data_reel_empty"), "2": m("component_case"), "3": "minecraft:repeater", "4": m("circuit_spider_eye"),
        "5": m("component_power_supply"), "6": m("component_fan")})
    # the "final sacrifice" block was configurable in the original (a beacon by default); change it with a data pack
    shaped("thermosonic_bonder", m("thermosonic_bonder"), ["343", "353", "121"], {
        "1": "minecraft:glowstone", "2": "minecraft:beacon", "3": "#c:storage_blocks/iron", "4": "#c:storage_blocks/redstone",
        "5": "#c:storage_blocks/diamond"})
    shaped("cryotube", m("cryotube"), ["121", "131", "141"], {
        "1": "#c:storage_blocks/iron", "2": m("circuit_ender_eye"), "3": m("component_computer"), "4": m("component_power_supply")})
    shaped("clay_furnace", m("clay_furnace"), ["111", "121", "111"], {"1": "minecraft:terracotta", "2": "minecraft:furnace"})
    # early-game help for the Thermosonic Bonder's nether star: mutant DNA over a skull in soul sand gives a wither skeleton egg
    shaped("wither_skeleton_spawn_egg", "minecraft:wither_skeleton_spawn_egg", ["212", "232", "242"], {
        "1": m("syringe_mutant"), "2": "minecraft:soul_sand", "3": "minecraft:skeleton_skull", "4": "minecraft:egg"})


# DNA Extractor inputs per species (besides that species' syringe), from the original MadDNA.
DNA_ITEMS = {
    "cave_spider": ["minecraft:fermented_spider_eye"],
    "chicken": ["minecraft:feather", "minecraft:egg", "minecraft:cooked_chicken", "minecraft:chicken"],
    "cow": ["minecraft:leather", "minecraft:cooked_beef", "minecraft:beef", "minecraft:leather_boots", "minecraft:leather_helmet",
            "minecraft:leather_leggings", "minecraft:leather_chestplate", "minecraft:milk_bucket"],
    "creeper": ["minecraft:gunpowder", "minecraft:creeper_head"],
    "enderman": ["minecraft:ender_pearl", "minecraft:ender_eye"],
    "ghast": ["minecraft:ghast_tear"],
    "pig": ["minecraft:porkchop", "minecraft:cooked_porkchop"],
    "sheep": ["#minecraft:wool"],
    "skeleton": ["minecraft:bone", "minecraft:bone_meal", "minecraft:skeleton_skull"],
    "slime": ["minecraft:slime_ball", "minecraft:sticky_piston"],
    "spider": ["minecraft:spider_eye", "minecraft:string"],
    "squid": ["minecraft:ink_sac"],
    "zombie": ["minecraft:zombie_head", "minecraft:rotten_flesh"],
}


def ingredient(v):
    return {"tag": v[1:]} if v.startswith("#") else {"item": v}


def processing_recipes():
    for sid, (_, has_syringe, has_sample) in SPECIES.items():
        if not has_sample:
            continue
        if has_syringe:
            write_json(DATA / f"recipe/dna_extracting/syringe_{sid}.json", {
                "type": f"{MOD}:dna_extracting", "input": {"item": f"{MOD}:syringe_{sid}"},
                "result": {"id": f"{MOD}:dna_{sid}"}, "remainder": {"id": f"{MOD}:syringe_dirty"}})
        for i, item in enumerate(DNA_ITEMS.get(sid, [])):
            name = item.split(":")[1].replace("/", "_")
            write_json(DATA / f"recipe/dna_extracting/{sid}_from_{name}.json", {
                "type": f"{MOD}:dna_extracting", "input": ingredient(item), "result": {"id": f"{MOD}:dna_{sid}"}})
    write_json(DATA / "recipe/sanitizing/syringe.json", {
        "type": f"{MOD}:sanitizing", "input": {"item": f"{MOD}:syringe_dirty"}, "result": {"id": f"{MOD}:syringe_empty"}})
    for sid, (_, _, has_sample) in SPECIES.items():
        if has_sample:
            write_json(DATA / f"recipe/sequencing/{sid}.json", {
                "type": f"{MOD}:sequencing", "input": {"item": f"{MOD}:dna_{sid}"}, "result": {"id": f"{MOD}:genome_{sid}"}})
    for gid, (_, parents, ticks) in GMOS.items():
        for i, (a, b) in enumerate(parents):
            suffix = "" if len(parents) == 1 else f"_from_{b}"
            write_json(DATA / f"recipe/genome_merging/{gid}{suffix}.json", {
                "type": f"{MOD}:genome_merging", "first": {"item": f"{MOD}:genome_{a}"}, "second": {"item": f"{MOD}:genome_{b}"},
                "result": {"id": f"{MOD}:genome_{gid}"}, "time": ticks})
    write_json(DATA / "recipe/genome_merging/pig_zombie.json", {
        "type": f"{MOD}:genome_merging", "first": {"item": f"{MOD}:genome_zombie"}, "second": {"item": f"{MOD}:genome_pig"},
        "result": {"id": f"{MOD}:genome_pig_zombie"}, "time": 1337})
    for gid, egg in VANILLA_EGGS.items():
        write_json(DATA / f"recipe/incubating/{gid}.json", {
            "type": f"{MOD}:incubating", "input": {"item": f"{MOD}:genome_{gid}"}, "result": {"id": f"minecraft:{egg}_spawn_egg"}})
    for name, (inp, out, count) in {
        "silicon_wafer": ("component_fused_quartz", "component_silicon_wafer", 1),
        "transistor": ("component_silicon_wafer", "component_transistor", 16),
        "cpu": ("circuit_redstone", "component_cpu", 1),
        "ram": ("circuit_glowstone", "component_ram", 1),
    }.items():
        write_json(DATA / f"recipe/bonding/{name}.json", {
            "type": f"{MOD}:bonding", "input": {"item": f"{MOD}:{inp}"}, "result": {"id": f"{MOD}:{out}", "count": count}})
    for metal in ("iron", "gold"):
        write_json(DATA / f"recipe/clay_smelting/{metal}.json", {
            "type": f"{MOD}:clay_smelting", "input": {"tag": f"c:ores/{metal}"}, "result": {"id": f"minecraft:{metal}_block"}})
    for gid, result in GMO_RESULTS.items():
        write_json(DATA / f"recipe/incubating/{gid}.json", {
            "type": f"{MOD}:incubating", "input": {"item": f"{MOD}:genome_{gid}"}, "result": {"id": result}})


MEMORIES = {
    "priest": "Filled with huge gaps of data and very minimal flow of ideas.",
    "farmer": "Data set is filled with the same repeating patterns over and over.",
    "butcher": "Good data with input from a variety of sources, only a few ideas.",
    "blacksmith": "High quality data and many ideas and experiences.",
    "librarian": "Best data recorded, each bit is unique and packed with ideas and experiences out of this world.",
}


def memory_items():
    for mid, lore in MEMORIES.items():
        item_model(f"memory_{mid}", ["data_reel_layer_1", "data_reel_layer_2", "data_reel_overlay"])
        lang[f"item.{MOD}.memory_{mid}"] = f"{mid.title()} Memory Reel"
        lang[f"item.{MOD}.memory_{mid}.tooltip"] = lore


def gmo_items():
    reel_layers = ["data_reel_layer_1", "data_reel_layer_2", "data_reel_overlay"]
    genomes = [f"{MOD}:genome_{sid}" for sid in SPECIES] + [f"{MOD}:genome_{gid}" for gid in GMOS]
    for gid, (english, parents, _) in GMOS.items():
        item_model(f"genome_{gid}", reel_layers)
        lang[f"item.{MOD}.genome_{gid}"] = f"{english} Genome"
        lang[f"item.{MOD}.genome_{gid}.tooltip"] = "Combined genome. Write it onto an egg in a Genome Incubator to bring it to life."
    write_json(DATA / "tags/item/genomes.json", {"replace": False, "values": genomes})
    # memory reels join this tag when the Cryogenic Tube is ported
    write_json(DATA / "tags/item/data_reels.json", {"replace": False, "values": ["#madscience:genomes"] + [f"{MOD}:memory_{m}" for m in MEMORIES]})
    bloodwork = [f"{MOD}:syringe_mutant"] + [f"{MOD}:syringe_{s}" for s, (_, syr, _) in SPECIES.items() if syr] \
        + [f"{MOD}:dna_{s}" for s, (_, _, smp) in SPECIES.items() if smp]
    write_json(DATA / "tags/item/bloodwork.json", {"replace": False, "values": bloodwork})


def gui_lang():
    lang.update({
        "gui.madscience.energy": "%s / %s FE",
        "gui.madscience.energy_percent": "Energy %s %%",
        "gui.madscience.progress": "%s / %s",
        "gui.madscience.progress_percent": "Progress %s %%",
        "gui.madscience.heat_percent": "Heat %s %%",
        "gui.madscience.hatching_percent": "Hatching %s %%",
        "gui.madscience.health_percent": "Health %s %%",
        "gui.madscience.neural_percent": "Neural %s %%",
        "gui.madscience.clay_furnace_hint": "Light with flint and steel. Hit it when it stops burning, wait for it to cool, then hit it again.",
        "gui.madscience.millibuckets": "%s mB",
        "gui.madscience.place_empty_bucket": "Place empty bucket",
        "gui.madscience.place_water_bucket": "Place water bucket",
    })


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


# original sound folder -> sound event prefix. The VoxBox (Half-Life VOX clips) and pulse rifle (film sounds) folders are
# deliberately absent: they belong to Valve and the film studio, not the Mad Science authors.
SOUND_FOLDERS = {
    "cncMachine": "cnc_machine", "computerMainframe": "mainframe", "cryoFreezer": "cryo_freezer", "cryoTube": "cryo_tube",
    "dataDuplicator": "data_duplicator", "dnaExtractor": "dna_extractor", "genomeIncubator": "incubator",
    "genomeSequencer": "sequencer", "gmoAbomination": "abomination", "gmoCreeperCow": "creeper_cow", "gmoWerewolf": "werewolf",
    "gmoWoolyCow": "wooly_cow", "magLoader": "magazine_loader", "meatCube": "meat_cube", "needleEmpty": "syringe",
    "needleSanitizer": "sanitizer", "soniclocator": "soniclocator", "thermosonicBonder": "thermosonic_bonder",
}
# event names that read better than the original file names
SOUND_RENAMES = {"syringe.stab": "syringe.stab_mob", "syringe.stabself": "syringe.stab_player"}
# vanilla stand-ins for the excluded sounds
VANILLA_SOUNDS = {
    "pulse_rifle.fire": "minecraft:entity.firework_rocket.blast",
    "pulse_rifle.empty": "minecraft:block.dispenser.fail",
    "pulse_rifle.reload": "minecraft:item.crossbow.loading_end",
    "pulse_rifle.unload": "minecraft:item.crossbow.loading_start",
    "pulse_rifle.magazine_reload": "minecraft:item.armor.equip_iron",
    "pulse_rifle.magazine_unload": "minecraft:item.armor.equip_chain",
    "pulse_rifle.fire_grenade": "minecraft:entity.firework_rocket.launch",
    "pulse_rifle.reload_grenade": "minecraft:item.crossbow.loading_middle",
    "pulse_rifle.chamber_grenade": "minecraft:block.piston.contract",
    "pulse_rifle.grenade_explode": "minecraft:entity.generic.explode",
    "pulse_rifle.ricochet": "minecraft:block.anvil.land",
    "vox_box.chime": "minecraft:block.note_block.chime",
}


def snake(name):
    return re.sub(r"(?<=[a-z0-9])([A-Z])", r"_\1", name).lower()


def sounds():
    if not ORIG.exists():
        print("reference/ms-164 not found, leaving sounds.json alone")
        return
    defs = {}
    if True:
        for folder, prefix in SOUND_FOLDERS.items():
            groups = {}
            for ogg in sorted((ORIG / f"sound/{folder}").glob("*.ogg")):
                base = re.sub(r"\d+$", "", ogg.stem)
                groups.setdefault(snake(base), []).append(ogg)
            for base, files in groups.items():
                event = SOUND_RENAMES.get(f"{prefix}.{base}", f"{prefix}.{base}")
                entries = []
                for ogg in files:
                    target = ASSETS / f"sounds/{prefix}/{ogg.stem.lower()}.ogg"
                    target.parent.mkdir(parents=True, exist_ok=True)
                    shutil.copyfile(ogg, target)
                    entries.append(f"{MOD}:{prefix}/{ogg.stem.lower()}")
                defs[event] = {"sounds": entries}
    for event, vanilla in VANILLA_SOUNDS.items():
        defs[event] = {"sounds": [{"name": vanilla, "type": "event"}]}
    write_json(ASSETS / "sounds.json", dict(sorted(defs.items())))


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
fluids()
machines()
machine_recipes()
entities()
simple_blocks()
memory_items()
processing_recipes()
gmo_items()
gui_lang()
tags()
recipes()
sounds()
misc_lang()
empty_structure("gametest/empty")
write_json(ASSETS / "lang/en_us.json", dict(sorted(lang.items())))
print(f"wrote {len(generated)} files")
