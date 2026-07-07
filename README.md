<div align="center">

# Artemis

[![Forge](https://img.shields.io/badge/Loader-Forge-darkgreen)](https://minecraftforge.net/)
[![Modrinth](https://img.shields.io/modrinth/dt/lunar-artemis)](https://modrinth.com/mod/lunar-artemis)

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/nqtsu91)

> This mod is **NOT** a hacked client. If that is what you are looking for, you are in the wrong place.

</div>

**Artemis** is a Forge mod that allows Forge clients to interact with Lunar Client's Apollo plugin, through their open protobuf channel.

The point of Artemis is to create a **compatibility** between Forge and Lunar for servers that relies on Lunar's Glowing, Waypoint, TeamView, Marker or other modules.
This mod is "barebones", meaning there is no way to manually create a Lunar waypoint or marker. Its only purpose is to port some of Lunar's functionalities to Forge.

**Artemis** registers Lunar Client's channels when connecting to a server, so the **official Apollo plugin** can fully interact with the client.

Here is a list of currently supported modules : 
- Chat -> Chat supports live messages. [Chat Module](https://lunarclient.dev/apollo/developers/modules/chat)
- Glowing -> Supports RGB Glowing for specific players / entities. [Glow Module](https://lunarclient.dev/apollo/developers/modules/glow)
- TeamView -> Supports RGB TeamView targets. [TeamView Module](https://lunarclient.dev/apollo/developers/modules/team)
- Waypoint -> Supports the creation, deletion or edition of Waypoints. [Waypoint Module](https://lunarclient.dev/apollo/developers/modules/waypoint)
- Markers -> Servers the creation, deletion or edition of Markers. [Marker Module](https://lunarclient.dev/apollo/developers/modules/marker)
- Colored Fire -> Supports RGB colors for fire display. [Colored Fire Module](https://lunarclient.dev/apollo/developers/modules/coloredfire)
- Cooldown -> Supports custom Cooldown displays. [Cooldown Module](https://lunarclient.dev/apollo/developers/modules/cooldown)
- Glint -> Supports RGB colors for enchantments via NBT. [Glint Module](https://lunarclient.dev/apollo/developers/modules/glint)
- Inventory -> Supports specific item displays in inventories. [Inventory Module](https://lunarclient.dev/apollo/developers/modules/inventory)
- Limb -> Supports partial limb rendering. [Limb Module](https://lunarclient.dev/apollo/developers/modules/limb)
- NameTag -> Supports custom NameTag options. [NameTag Module](https://lunarclient.dev/apollo/developers/modules/nametag)
- Vignette -> Supports custom Vignettes. [Vignette Module](https://lunarclient.dev/apollo/developers/modules/vignette)

## QOL / Fixes

Artemis fixes the "Fast Render" bug that makes the screen black when the Fast Render option is enabled in Optifine.
Artemis allows the repositionning of the Cooldown module via the **/artemis** command.

## Artemis Specific Features



## Availability 
|Loader|Version|
|--------|--------|
| Forge | 1.8.91 |

## Usage 
Feel free to integrate Artemis in any of your modpacks, or create new versions of this mod. However, any redistributed version, modified or not, must stay fully free, and keep a link to the original.

**CrystallicDev is not responsible of any derived Hacked Clients based of Artemis's source code.**
