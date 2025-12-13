package com.zootdungeon.items;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.mobs.Mob;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Simple loot table registry that can be used both at compile-time (Java registration)
 * and at runtime (e.g. via JSON loaders) to define extra loot behavior.
 *
 * Existing code (Mob.loot, Generator, etc.) is kept for compatibility and can
 * coexist with tables defined here.
 */
public final class LootRegistry {

    private LootRegistry() {}

    private static final Map<String, LootTable> TABLES = new HashMap<>();

    public static void register(String id, LootTable table) {
        if (id == null || table == null) return;
        TABLES.put(id, table);
    }

    public static LootTable get(String id) {
        return TABLES.get(id);
    }

    /**
     * Rolls a table and returns all generated items (may be empty).
     */
    public static List<Item> roll(String id, LootContext ctx) {
        LootTable t = TABLES.get(id);
        if (t == null) return new ArrayList<>();
        return t.roll(ctx);
    }

    /**
     * Convenience: roll and return only the first item (or null).
     */
    public static Item rollOne(String id, LootContext ctx) {
        List<Item> items = roll(id, ctx);
        return items.isEmpty() ? null : items.get(0);
    }

    // -----------------------------
    // LootContext / Table / Entries
    // -----------------------------

    public static final class LootContext {
        public enum Source {
            MOB_KILL,
            ROOM_REWARD,
            CHEST_OPEN,
            OTHER
        }

        public final Mob mob;
        public final Char killer;
        public final int depth;
        public final int pos;
        public final Source source;

        private LootContext(Mob mob, Char killer, int depth, int pos, Source source) {
            this.mob = mob;
            this.killer = killer;
            this.depth = depth;
            this.pos = pos;
            this.source = source;
        }

        public static LootContext forMobKill(Mob mob, Char killer, int pos) {
            int depth = Dungeon.depth;
            return new LootContext(mob, killer, depth, pos, Source.MOB_KILL);
        }
    }

    public interface LootEntry {
        boolean matches(LootContext ctx);
        int weight();
        List<Item> generate(LootContext ctx);
    }

    public static class LootTable {
        private final List<LootEntry> entries = new ArrayList<>();
        private int rolls = 1;

        public LootTable add(LootEntry e) {
            if (e != null) entries.add(e);
            return this;
        }

        public LootTable rolls(int rolls) {
            this.rolls = Math.max(1, rolls);
            return this;
        }

        public List<Item> roll(LootContext ctx) {
            List<Item> out = new ArrayList<>();
            for (int r = 0; r < rolls; r++) {
                // filter entries that match context and have positive weight
                List<LootEntry> candidates = new ArrayList<>();
                int totalWeight = 0;
                for (LootEntry e : entries) {
                    if (e.weight() > 0 && e.matches(ctx)) {
                        candidates.add(e);
                        totalWeight += e.weight();
                    }
                }
                if (candidates.isEmpty() || totalWeight <= 0) {
                    break;
                }
                int ticket = Random.Int(totalWeight);
                LootEntry chosen = null;
                for (LootEntry e : candidates) {
                    ticket -= e.weight();
                    if (ticket < 0) {
                        chosen = e;
                        break;
                    }
                }
                if (chosen != null) {
                    out.addAll(chosen.generate(ctx));
                }
            }
            return out;
        }
    }

    // -----------------------------
    // Built-in simple entries
    // -----------------------------

    public static LootEntry forItemSupplier(int weight, Supplier<Item> supplier) {
        return new SimpleEntry(weight, supplier);
    }

    public static LootEntry forItemClass(int weight, Class<? extends Item> cls) {
        return new SimpleEntry(weight, () -> {
            try {
                return cls.newInstance();
            } catch (Exception e) {
                return null;
            }
        });
    }

    private static class SimpleEntry implements LootEntry {
        private final int weight;
        private final Supplier<Item> supplier;

        SimpleEntry(int weight, Supplier<Item> supplier) {
            this.weight = Math.max(0, weight);
            this.supplier = supplier;
        }

        @Override
        public boolean matches(LootContext ctx) {
            return true;
        }

        @Override
        public int weight() {
            return weight;
        }

        @Override
        public List<Item> generate(LootContext ctx) {
            List<Item> list = new ArrayList<>();
            Item it = supplier.get();
            if (it != null) list.add(it);
            return list;
        }
    }
}


