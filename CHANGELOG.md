# Changelog

## 2.0.0 (Minecraft 1.21.1, NeoForge)

First release of the 1.21.1 port of Mad Science 1.6.4.

### Ported
- Components, circuits, syringes, DNA samples, genome and memory data reels, with bloodwork decay.
- Liquid DNA and Liquid Mutant DNA.
- DNA Extractor, Syringe Sanitizer, Gene Sequencer, Computer Mainframe, Genome Incubator, Cryogenic Freezer,
  Data Reel Duplicator, Thermosonic Bonder, Clay Furnace, Cryogenic Tube, Soniclocator, Disgusting Meat Cube,
  Announcement System, Magazine Loader and CnC Machine, with the original Techne models, animated textures, GUIs and sounds.
- Werewolf, Creeper Cow, Enderslime, Wooly Cow, Shoggoth, Abomination (and its eggs), Ender Squid; GMO eggs for
  wither skeletons, zombie villagers and skeleton/zombie horses.
- Pulse Rifle, magazines, rounds and grenades, and the rifle parts.
- Laboratory coat, leggings and safety goggles; warning signs with 26 symbols.

### Changed from the original
- Forge Energy instead of Universal Electricity (`energyScale` server option, default 10).
- The Announcement System announces in chat; the original's voice clips belonged to Valve.
- Pulse rifle sounds are vanilla stand-ins and its parts have plain names; CnC book phrases are "pulse rifle barrel",
  "bolt", "receiver", "trigger", "magazine", "bullets" and "grenades", in plain text or binary.
- Multi-block machines are real multi-part blocks.
- Magazines load any number of rounds per craft, up to 99.
- The Werewolf attacks.

### New
- Server options to stop the pulse rifle firing and to keep its blasts from breaking blocks; claims are respected.
- Warning signs are owned by whoever hangs them.
- JEI categories for every machine and info pages for the rest (optional).
- Data-driven recipes for every machine, including the CnC Machine.
