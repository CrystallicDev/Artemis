# Artemis — Technical Documentation

Artemis is a **client-side** Forge 1.8.9 mod. It does not add any command a player can use to
create waypoints, markers, glows, etc. Instead, it makes a Forge client **respond to Apollo exactly
like a Lunar Client would**: everything is driven by the **server** through Lunar Client's Apollo
plugin (or any plugin that speaks the Apollo protocol).

This document explains, from a **server developer's** point of view, how to drive each feature.

---

## Table of contents

- [How it works](#how-it-works)
- [Detecting Artemis / Lunar clients](#detecting-artemis--lunar-clients)
- [Apollo modules](#apollo-modules)
- [Artemis-specific features](#artemis-specific-features)
  - [Full RGB / Hex text](#full-rgb--hex-text)
  - [Hex in the scoreboard & tab list](#hex-in-the-scoreboard--tab-list)
  - [Custom chat (`artemis:chat`)](#custom-chat-artemischat)
  - [Glint (NBT)](#glint-nbt)
  - [Inventory interaction (NBT)](#inventory-interaction-nbt)
- [Client-side usage](#client-side-usage)
- [Notes & limitations](#notes--limitations)

---

## How it works

When a client joins a server, Artemis sends a plugin-message **channel registration** announcing the
channels it understands:

- `lunar:apollo` — the standard Apollo protocol channel. Registering it is what makes the server treat
  the client as a Lunar client (same mechanism as a real Lunar Client).
- `artemis:chat` — an Artemis-only channel used by the [custom chat](#custom-chat-artemischat) feature.

Apollo messages travel on `lunar:apollo` as `google.protobuf.Any` payloads (`type_url` + `value`).
Artemis parses them and dispatches to the matching module. **You do not need to build these packets
by hand** — use the official Apollo API and it will just work.

---

## Detecting Artemis / Lunar clients

Artemis is picked up by Apollo's normal client detection, because it registers `lunar:apollo`. With
the Apollo API you can therefore resolve an `ApolloPlayer` for a client running Artemis.

If you need to tell **Artemis specifically** apart from a genuine Lunar Client (for example to decide
whether to send a hex-colored team prefix, see below), listen for the registration of the
`artemis:chat` channel — only Artemis registers it.

```java
@EventHandler
public void onRegister(PlayerRegisterChannelEvent event) {
    if (event.getChannel().equals("artemis:chat")) {
        // This player is running Artemis (not vanilla Lunar).
        artemisPlayers.add(event.getPlayer().getUniqueId());
    }
}
```

| Channel registered | Meaning |
|--------------------|---------|
| `lunar:apollo`     | Lunar Client **or** Artemis — full Apollo support |
| `artemis:chat`     | Artemis specifically — supports the custom chat channel |

---

## Apollo modules

All of the modules below are driven through the **standard Apollo API**. Artemis implements the
client side of each, so the server code is identical to what you would write for real Lunar clients.
Refer to the linked Apollo documentation for the exact API calls.

| Module | What Artemis does | Apollo docs |
|--------|-------------------|-------------|
| Glow | Colored outline (full RGB) around targeted players/entities | [glow](https://lunarclient.dev/apollo/developers/modules/glow) |
| TeamView | Colored marker above tracked team members (full RGB) | [team](https://lunarclient.dev/apollo/developers/modules/team) |
| Waypoint | Beam + label at a location; create / edit / remove | [waypoint](https://lunarclient.dev/apollo/developers/modules/waypoint) |
| Marker | Diamond marker with owner / coordinates / distance / description | [marker](https://lunarclient.dev/apollo/developers/modules/marker) |
| Cooldown | Custom cooldown display (item icon, duration, colors) | [cooldown](https://lunarclient.dev/apollo/developers/modules/cooldown) |
| Colored Fire | Per-player RGB fire tint | [coloredfire](https://lunarclient.dev/apollo/developers/modules/coloredfire) |
| Limb | Hide body parts / armor pieces per player | [limb](https://lunarclient.dev/apollo/developers/modules/limb) |
| Nametag | Custom nametag lines (supports hex) | [nametag](https://lunarclient.dev/apollo/developers/modules/nametag) |
| Vignette | Full-screen textured vignette overlay | [vignette](https://lunarclient.dev/apollo/developers/modules/vignette) |

Typical Apollo usage looks like this (glow example — see the Apollo docs for the precise signatures):

```java
Apollo.getApolloModuleManager().getModule(GlowModule.class)
    .ifPresent(module -> module.overrideGlow(
        apolloPlayer,          // recipient (the Artemis client)
        target,                // player/entity to glow
        Color.decode("#2299FF")// any RGB color
    ));
```

Two modules use item **NBT** instead of the Apollo protocol — see
[Glint](#glint-nbt) and [Inventory interaction](#inventory-interaction-nbt).

---

## Artemis-specific features

These go beyond a plain Apollo port: on 1.8.9 both vanilla and Lunar fall back to the 16 legacy
colors in most UI spots, whereas Artemis renders true 24-bit color everywhere.

### Full RGB / Hex text

Artemis teaches the game's font renderer the **1.16 hex format** `§x§R§R§G§G§B§B`. Any string that
reaches the font renderer with that sequence is drawn with its exact RGB color — this covers:

- chat
- the tab list (player list)
- the scoreboard sidebar
- team prefixes / suffixes
- floating nametags (above the head)
- `/title` and `/subtitle`

The format, byte for byte, is `§` (`U+00A7`) followed by `x`, then each of the 6 hex digits, each one
also preceded by `§`. For `0x2299FF`:

```
§x§2§2§9§9§f§f
```

**Producing it (Adventure):**

```java
LegacyComponentSerializer hex = LegacyComponentSerializer.builder()
    .character('§')   // §  (NOT '&')
    .hexColors()           // emit §x§r§r§g§g§b§b instead of down-sampling
    .build();

String legacy = hex.serialize(Component.text("Hello").color(TextColor.color(0x2299FF)));
// legacy == "§x§2§2§9§9§f§fHello"
```

Common mistakes that render as plain white / wrong color:

| You sent | Problem |
|----------|---------|
| `&x&2&2&9&9&f&f` | `&` instead of `§` — Artemis only reads `§` (`U+00A7`) |
| `§x2299ff` | missing the inner `§` before each hex digit |
| `§#2299FF` | the `§#RRGGBB` format is not supported |

> Rendering works under **OptiFine** and alongside mods that re-render the HUD themselves (e.g.
> **OldAnimations**, which reverts the tab list to its 1.7.10 rendering), because the hook sits on the
> single font-rendering entry point every draw call passes through.

### Hex in the scoreboard & tab list

Vanilla 1.8.9 caps a team `prefix`/`suffix` at **16 characters** on the wire, and a client rejects
(and disconnects on) anything longer. A full hex sequence (`§x§r§r§g§g§b§b`) is already 14 characters,
so the vanilla API is unusable for hex team colors.

Artemis lifts that cap **client-side**, exactly like a Lunar client. To color a player name in the
tab list / sidebar with hex:

1. Put the hex sequence in the team **prefix** (using the serializer shown above).
2. Send the team packet **directly** (NMS / a packet library), bypassing Bukkit's `Team#setPrefix`
   16-char check.
3. **Only send it to Artemis (or Lunar) clients.** A vanilla client receiving a >16-char prefix will
   be kicked with a decoder error. Use per-player scoreboards: hex prefix for Artemis clients, a
   regular 16-color prefix for everyone else.

```
Tab list line = <team prefix> + <player name> + <team suffix>
                 ^^^^^^^^^^^^^  put "§x…" here
```

### Custom chat (`artemis:chat`)

Artemis exposes a dedicated plugin-message channel, `artemis:chat`, to display rich (hex) messages in
the **vanilla chat** (position, scroll, fade and deletion are all handled natively). Messages are
addressed by an integer id so they can be updated or removed later.

**Channel:** `artemis:chat`
**Payload:** written with a `DataOutputStream` (i.e. big-endian, `writeUTF` for strings):

| Opcode (`byte`) | Meaning | Following data |
|-----------------|---------|----------------|
| `0` | display | `int id`, `UTF json` |
| `1` | remove  | `int id` |
| `2` | clear   | *(none)* |

- `id` — a message identifier. `0` means a normal, non-removable line. Any non-zero id can later be
  targeted by `remove`, or re-sent with opcode `0` to **replace** the existing line (live update).
- `json` — an Adventure component serialized as **Gson JSON** (`GsonComponentSerializer`). Hex colors
  in the component are rendered exactly.

```java
// Display / update a message with id 42
ByteArrayOutputStream buf = new ByteArrayOutputStream();
DataOutputStream out = new DataOutputStream(buf);
out.writeByte(0);                 // display
out.writeInt(42);                 // id
out.writeUTF(GsonComponentSerializer.gson().serialize(component));
player.sendPluginMessage(plugin, "artemis:chat", buf.toByteArray());
```

> Only send this to clients that registered `artemis:chat`. For everyone else, fall back to a normal
> chat message with the color down-sampled to the 16 legacy colors.

### Glint (NBT)

The Glint module colors an item's enchantment shimmer. It is driven by **item NBT**, not the Apollo
protocol, so it travels with the item through normal Minecraft packets.

- Path: `tag.lunar.glint`
- Type: **int**, an **ARGB** color (e.g. `0xFFFF0000` = opaque red)

```java
// NMS-side, on the item's compound tag:
NBTTagCompound lunar = tag.getCompoundTag("lunar");
lunar.setInt("glint", 0xFFFF0000);   // red glint
tag.setTag("lunar", lunar);
```

The item does not need to be actually enchanted.

### Inventory interaction (NBT)

The Inventory module changes how items behave inside GUIs. Also driven by NBT, under the same `lunar`
compound:

| NBT key (`tag.lunar.*`) | Type | Effect |
|-------------------------|------|--------|
| `unclickable`      | bool   | The slot cannot be clicked / moved |
| `openUrl`          | string | Clicking opens the URL (respects the client's "chat links" setting) |
| `runCommand`       | string | Clicking runs the command |
| `suggestCommand`   | string | Clicking pre-fills the command in chat |
| `copyToClipboard`  | string | Clicking copies the text to the clipboard |
| `hideItemTooltip`  | bool   | The item shows no tooltip |
| `hideSlotHighlight`| bool   | The slot's hover highlight is hidden |

```java
NBTTagCompound lunar = tag.getCompoundTag("lunar");
lunar.setBoolean("unclickable", true);
lunar.setString("runCommand", "/spawn");
tag.setTag("lunar", lunar);
```

---

## Client-side usage

Artemis has no gameplay commands. The only client command is:

- **`/artemis`** — opens the configuration screen, where the player can drag the **Cooldown** HUD to
  reposition it and choose its layout direction (right / left / down / up). Settings persist in
  `config/artemis.properties`.

Language files are provided in English (default) and French.

---

## Notes & limitations

- Artemis is **client-only** and assumes **OptiFine** is installed.
- Colors are always resolved from what the **server** sends: Artemis only renders hex when the color
  is encoded in the `§x` format inside the relevant string (team prefix, title, chat component, …).
- Distinguish Artemis from vanilla Lunar via the `artemis:chat` channel when a feature (custom chat,
  hex team prefixes) would break a non-Artemis client.
```
