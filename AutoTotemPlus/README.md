# Auto Totem Plus 1.0.0

Fabric client mod for Minecraft 1.21.11 (Java 21).

## What it does
- Watches for the local player using a Totem of Undying.
- Only starts recovery when the offhand held a Totem.
- Opens the player's inventory, finds another Totem, and swaps it into the offhand.
- Closes the inventory after the offhand refill is observed.
- Shows an action-bar warning when no replacement Totem is available.
- Press **G** to toggle Auto Totem Plus on or off.

## Timing
The refill process has a 30 client-tick timeout (1.5 seconds at 20 TPS). The mod acts as soon as the inventory state is synchronized. Server latency, server-side inventory rules, and client performance can affect the actual completion time.

## Install
Put the built `AutoTotemPlus-1.0.0.jar` in the Minecraft `mods` folder. Install Fabric Loader and Fabric API for Minecraft 1.21.11.

This is a client-side mod. In multiplayer, the server remains authoritative and may reject or delay inventory actions.
