package com.zootdungeon.sprites;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.zootdungeon.CDSettings;

/**
 * Simple JSON-driven texture pack loader.
 *
 * Expected JSON structure (all fields optional):
 *
 * {
 *   "id": "minecraft",
 *   "buffs": { "small": "minecraft/buffs.png", "large": "minecraft/large_buffs.png" },
 *   "ui":    { "icons": "minecraft/icons.png" },
 *   "tilemaps": {
 *     "theme:sewers": { "tiles": "minecraft/tiles_sewers.png", "water": "minecraft/water_sewers.png" }
 *   },
 *   "tilesets": {
 *     "theme:sewers": { "texture": "minecraft/tiles_sewers.png", "json": "minecraft/tiles_sewers.json" }
 *   },
 *   "mobs": {
 *     "mod:rat": { "texture": "minecraft/rat.png", "w": 16, "h": 15 }
 *   }
 * }
 */
public final class TexturePackManager {

    private static String currentPackId = "";

    private TexturePackManager() {}

    public static String currentPackId() {
        return currentPackId;
    }

    /**
     * Loads and applies the texture pack configured in CDSettings, if any.
     * Safe to call multiple times; will re-apply if the pack id changes.
     */
    public static void initFromSettings() {
        String packId = CDSettings.texturePack();
        if (packId == null) packId = "";
        applyPack(packId);
    }

    /**
     * Applies a texture pack by id. An empty or "default" id resets to
     * the built-in textures (no JSON is loaded).
     */
    public static void applyPack(String packId) {
        if (packId == null) packId = "";

        currentPackId = packId;

        if (packId.isEmpty() || "default".equals(packId)) {
            // Use built-in textures; SpriteRegistry already has correct defaults.
            return;
        }

        String path = "cola/packs/" + packId + ".json";
        FileHandle handle = Gdx.files.internal(path);
        if (!handle.exists()) {
            System.out.println("[TexturePack] Pack json not found: " + path);
            return;
        }

        try {
            JsonValue root = new JsonReader().parse(handle);

            // Buff atlases
            JsonValue buffs = root.get("buffs");
            if (buffs != null && buffs.isObject()) {
                String small = buffs.getString("small", null);
                String large = buffs.getString("large", null);
                SpriteRegistry.setBuffTextures(small, large);
            }

            // UI icons
            JsonValue ui = root.get("ui");
            if (ui != null && ui.isObject()) {
                String icons = ui.getString("icons", null);
                SpriteRegistry.setUiIconsTexture(icons);
            }

            // Tilemaps (tiles + water texture pairs)
            JsonValue tilemaps = root.get("tilemaps");
            if (tilemaps != null && tilemaps.isObject()) {
                for (JsonValue entry : tilemaps) {
                    String key = entry.name;
                    if (key == null) continue;
                    String tiles = entry.getString("tiles", null);
                    String water = entry.getString("water", null);
                    if (tiles == null && water == null) continue;
                    SpriteRegistry.registerTilemap(
                            key,
                            water != null
                                    ? new SpriteRegistry.TilemapDef(tiles, water)
                                    : new SpriteRegistry.TilemapDef(tiles)
                    );
                }
            }

            // Tilesets (JSON-driven DungeonTileSheet remapping)
            JsonValue tilesets = root.get("tilesets");
            if (tilesets != null && tilesets.isObject()) {
                for (JsonValue entry : tilesets) {
                    String key = entry.name;
                    if (key == null) continue;
                    String tex = entry.getString("texture", null);
                    String json = entry.getString("json", null);
                    if (tex == null || json == null) continue;
                    SpriteRegistry.registerTilesetFromJson(key, tex, json);
                }
            }

            // Mobs
            JsonValue mobs = root.get("mobs");
            if (mobs != null && mobs.isObject()) {
                for (JsonValue entry : mobs) {
                    String key = entry.name;
                    if (key == null) continue;
                    String texture = entry.getString("texture", null);
                    int w = entry.getInt("w", 0);
                    int h = entry.getInt("h", 0);
                    if (texture == null || w <= 0 || h <= 0) continue;
                    SpriteRegistry.registerMob(key,
                            new SpriteRegistry.MobDef(texture, w, h));
                }
            }

            System.out.println("[TexturePack] Applied texture pack: " + packId);

        } catch (Exception e) {
            System.out.println("[TexturePack] Failed to apply texture pack: " + packId);
            e.printStackTrace();
        }
    }
}


