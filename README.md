# Mad Science (1.21.1 NeoForge port)

![Mad Science](docs/logo.png)

Machines, items and mobs for building your own genetics laboratory. Draw blood from mobs, extract and sequence their DNA,
merge genomes in a computer mainframe and hatch genetically modified creatures. Power your lab with the memories of
villagers, find ores with seismic thumps, and arm yourself with a pulse rifle you machined from blocks of iron.
*Remember kids, science has no limits... and no bounds...*

This is a port of **Mad Science** for Minecraft 1.6.4 to Minecraft **1.21.1** on **NeoForge**.

## Original mod

- Original Mad Science by **Maxwolf Goodliffe** (developer) and **Fox Diller** (co-developer), with art by **Prowler**.
- Original source: <https://github.com/BuiltBrokenModding/Mad-Science>
- Mob designs suggested by community members credited in the original: Bart74 (Wooly Cow), Deuce_Loosely (Shoggoth),
  monodemono (Abomination), Pyrobrine (Wither Skeleton, Villager Zombie, Skeleton/Zombie Horse) and TheTechnician (Ender Squid).

The port is released under the MIT licence with the original authors' permission. Material in the original download that
belonged to other people is not included: the Universal Electricity API, the announcer voice clips (from Valve's Half-Life),
the pulse rifle sound effects and the original logo.

## What's in it

**Genetics**
- Syringes: hit a mob (or use one on yourself) to draw blood. Filled syringes, DNA samples and unfinished genomes slowly
  decay unless kept in a Cryogenic Freezer.
- DNA Extractor, Syringe Sanitizer, Gene Sequencer, Computer Mainframe (water-cooled, can overheat), Genome Incubator,
  Data Reel Duplicator and Cryogenic Freezer, with the original models, animations, GUIs and sounds.
- Genetically modified creatures: Werewolf, Creeper Cow, Enderslime, Wooly Cow, Shoggoth, Abomination (it lays eggs),
  Ender Squid and the Disgusting Meat Cube, plus wither skeletons, zombie villagers and skeleton/zombie horses.
- Liquid DNA and Liquid Mutant DNA fluids.

**Power and industry**
- Cryogenic Tube (3 blocks tall): grows a villager and turns its neural activity into power, more for more learned professions.
- Soniclocator (3 blocks tall): thumps the ground to swap a chosen block throughout its chunk for gravel and collect it.
- Thermosonic Bonder: makes silicon wafers, transistors, CPUs and RAM.
- Clay Furnace: an ancient way to turn one ore block into a block of metal.
- Announcement System: reads a written book out in chat on a redstone pulse.

**Weapons**
- CnC Machine (2 blocks tall): water-jet cuts blocks of iron into weapon parts, programmed by a written book naming the part
  (plain text, or binary ASCII like the original).
- Pulse Rifle with a two-digit ammo counter, underslung grenade launcher, magazines, rounds and grenades.
- Magazine Loader (2 blocks tall): loads 95 rounds into empty magazines.

**And**: laboratory coat, leggings and safety goggles, and warning signs with 26 hazard symbols.

JEI is supported (optional): every machine has a recipe category, and the machines without recipes have info pages.

## Server owners

Everything gameplay-related is in the server config (`serverconfig/madscience-server.toml`):

- `pulseRifleEnabled`: set to false to stop the pulse rifle firing.
- `bulletsDamageWorld`: set to false to keep grenades and critical rounds from breaking blocks. Block damage also needs the
  `mobGriefing` game rule, and every blast goes through the normal explosion events, so claim mods can stop it.
- The Soniclocator only takes blocks a player could break there (claims apply), and the Abomination's egg-laying follows
  `mobGriefing`.
- `energyScale` converts the original Universal Electricity values to FE (default 10), and there are options for bloodwork
  decay, the Clay Furnace time, the Cryogenic Tube's output, the Soniclocator's range and more.

Warning signs belong to whoever hangs them: only the owner (or a server operator) can change or remove one.

## Differences from the original

- Power is Forge Energy (FE) instead of Universal Electricity, scaled by `energyScale`.
- The Announcement System posts its message in chat instead of playing voice clips.
- The pulse rifle family uses vanilla sounds and plain names, and the CnC Machine's book phrases are now
  "pulse rifle barrel", "bolt", "receiver", "trigger", "magazine", "bullets" and "grenades".
- Magazines take any number of rounds per craft (up to 99) rather than one at a time.
- Multi-block machines are proper multi-part blocks instead of invisible "ghost" blocks.
- The Werewolf actually attacks (the original's AI never did).
- Recipes are data-driven JSON and use `c:` tags, so modpacks can change them.

## Building

```
./gradlew build
```

Needs Java 21. The jar ends up in `build/libs`.

## Licence

MIT, see [LICENSE](LICENSE).
