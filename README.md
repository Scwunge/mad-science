# Mad Science (1.21.1 NeoForge port)

Machines, items and mobs for building your own genetics laboratory. Draw blood from mobs, extract and sequence their DNA,
merge genomes in a computer mainframe and hatch genetically modified creatures. *Remember kids, science has no limits... and no bounds...*

This is a port of **Mad Science** for Minecraft 1.6.4 to Minecraft **1.21.1** on **NeoForge**.

## Original mod

- Original Mad Science by **Maxwolf Goodliffe** (developer) and **Fox Diller** (co-developer), with art by **Prowler**.
- Original source: <https://github.com/BuiltBrokenModding/Mad-Science>
- Mob designs suggested by community members credited in the original: Bart74 (Wooly Cow), Deuce_Loosely (Shoggoth),
  monodemono (Abomination), Pyrobrine (Wither Skeleton, Villager Zombie, Skeleton/Zombie Horse) and TheTechnician (Ender Squid).

The port is released under the MIT licence with the original authors' permission. See [PERMISSION.md](PERMISSION.md) for the
details and for what is *not* covered (and so is not included).

## Differences from the original

- Power is Forge Energy (FE) instead of Universal Electricity. The original values are scaled by the `energyScale` server config option (default 10).
- The Announcement System (VoxBox) posts its message in chat instead of playing voice clips.
- The pulse rifle family uses vanilla sounds and generic names.
- Recipes are data-driven JSON and use `c:` tags, so modpacks can change them.

## Status

Work in progress. Done so far: components, circuits, syringes, DNA samples and genome reels. Machines, mobs and weapons are next.

## Building

```
./gradlew build
```

Needs Java 21. The jar ends up in `build/libs`.
