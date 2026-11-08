# AutoGG

A lightweight **client-side** Fabric mod for Minecraft **1.21.11**.

Fight someone, press your keybind, and AutoGG runs a **fully configurable command** — default:

```text
/msg {player} GG
```

Targets the **last player you hit**. If you have not hit anyone recently, it uses the **last player who hit you**. Perfect for quick sportsmanship without typing.

---

## Features

- Remembers the **last player you hit** (priority)
- Falls back to the **last player who hit you**
- **Anti-spam** cooldown between successful sends
- Per-engagement send limit (default: one message until you hit or get hit again)
- Fully configurable **command template**
- Configurable **keybind** (Controls → AutoGG)
- **Mod Menu** + **Cloth Config** settings screen
- Safe against `§` kicks (strips illegal formatting)
- Optional distance / online checks
- Works with spears (jab / charge)

---

## How to use

1. Attack another player (or get hit by one)
2. Press the AutoGG keybind (default: **`**)
3. The configured command is sent to that player

---

## Placeholders

Use these in your command template (must include `{player}`):

| Placeholder | Meaning |
|---|---|
| `{player}` | Target player's username |
| `{message}` | Your configured message text |
| `{uuid}` | Target player's UUID |

### Examples

```text
/msg {player} {message}
/tell {player} gf
/w {player} gg ez
msg {player} good fight!
```

---

## Config options

Open **Mods → AutoGG → Config**, or edit `.minecraft/config/autogg.json`.

- **Command template** — the full command to run (must include `{player}`)
- **Message text** — inserted at `{message}` (default `GG`)
- **Send mode**
  - `COMMAND` (recommended): command packet
  - `CHAT`: sends as chat starting with `/`
- **Remember combat target (seconds)** — `0` = until you leave the server
- **Use last attacker as fallback** — prefer who you hit; else who hit you
- **Max distance** — `0` = disabled
- **Require player online/loaded**
- **Anti-spam** — enable cooldown between sends
- **Cooldown (seconds)** — default `3`, max `60`
- **Max sends per hit** — default `1` (hit again to refresh)
- **Action bar feedback**

---

## Dependencies

**Required**
- Fabric API
- Cloth Config

**Recommended**
- Mod Menu

---

## Install

1. Install Fabric for 1.21.11  
2. Put Fabric API + Cloth Config (+ Mod Menu) in `mods`  
3. Put `AutoGG-1.2.0.jar` in `mods`

---

## Notes

- Client-side only — does **not** need to be on the server
- Uses the player's real username (not colored nametags), so it won't trigger illegal character kicks
- Works with `/msg`, `/tell`, `/w`, or any custom command your server supports
- Combat memory and anti-spam clear on disconnect, dimension change, and respawn
