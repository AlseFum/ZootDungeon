package com.zootdungeon.items;

import com.zootdungeon.utils.AtomBundle;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

// ----- 核心逻辑 -----
// 按表 id 取 LootTable，对每个 LootPool 做 N 次抽取（N = rolls + bonusRollCount(ctx)）。每次在满足 condition 的 entry 里按权重选一个（权重 = weight + quality * ctx.luck），选中的 entry 执行 generate 得到物品。TableEntry 会递归 roll 子表，用 ThreadLocal 防环。
//
// ----- 用法 -----
// 1) 注册表：LootRegistry.register("表id", table);
//
// 2) 建表：new LootTable().pool(pool1).pool(pool2) ...
//
// 3) 建池：new LootPool().rolls(次数).bonusRolls(浮点额外次数).when(ctx -> 布尔).add(entry) ...
//    - rolls(n)：该池抽 n 次。
//    - bonusRolls(f)：额外次数 = f * (1 + ctx.bonusRollX/100)，再按整数组+小数概率。
//    - when(condition)：池级条件，不满足则整池不执行。
//
// 4) 条目类型与构造：
//    - ItemEntry(weight, Item子类.class) 或 ItemEntry(weight, () -> item实例)
//    - CategoryEntry(weight, Generator.Category) 或 (weight, category, useDefaults)
//    - TableEntry(weight, "子表id")
//    - EmptyEntry(weight)
//    条目可链式：.weight(n).quality(n).when(ctx -> ...).apply((item, ctx) -> 修改后item)
//    quality：有效权重 = weight + quality * ctx.getInt("luck",0)，幸运高时高 quality 条目更易被抽到。
//
// 5) 执行掉落：先 new AtomBundle()，按需 put 键值（见下），再 LootRegistry.roll("表id", ctx) 得 List<Item>，或 rollOne("表id", ctx) 得单个 Item（无则 null）。
//
// 6) ctx 常用键（均为可选，按需放入）：
//    depth(int), pos(int), source(atom 如 "MOB_KILL"), killedByPlayer(int 0/1), luck(int，幸运等级，影响 quality), bonusRollX(int 百分之一，如 20 表示 1.2 倍 bonus 次数)。
//
// 7) 条件与函数：LootCondition 即 (AtomBundle ctx) -> boolean；LootFunction 即 (Item item, AtomBundle ctx) -> Item，用于 entry.apply(fn) 在生成后修改物品。
public final class LootRegistry {

    public static final Map<String, LootTable> TABLES = new HashMap<>();

    public static final ThreadLocal<HashSet<String>> ACTIVE_TABLES = ThreadLocal.withInitial(HashSet::new);

    public LootRegistry() {
    }

    public static void register(String id, LootTable table) {
        if (id == null || table == null) return;
        TABLES.put(id, table);
    }

    public static List<Item> roll(String id, AtomBundle ctx) {
        if (id == null || ctx == null) return new ArrayList<>();
        LootTable table = TABLES.get(id);
        if (table == null) return new ArrayList<>();
        HashSet<String> active = ACTIVE_TABLES.get();
        if (!active.add(id)) return new ArrayList<>();
        try {
            return table.roll(ctx);
        } finally {
            active.remove(id);
        }
    }

    public static Item rollOne(String id, AtomBundle ctx) {
        List<Item> items = roll(id, ctx);
        return items.isEmpty() ? null : items.get(0);
    }

    @FunctionalInterface
    public interface LootCondition {
        boolean test(AtomBundle ctx);
    }

    public interface LootFunction {
        Item apply(Item item, AtomBundle ctx);
    }

    public interface LootEntry {
        boolean matches(AtomBundle ctx);
        int weight(AtomBundle ctx);
        List<Item> generate(AtomBundle ctx);
    }

    public static class LootTable {
        public final List<LootPool> pools = new ArrayList<>();

        public LootTable pool(LootPool pool) {
            if (pool != null) pools.add(pool);
            return this;
        }

        public List<Item> roll(AtomBundle ctx) {
            List<Item> out = new ArrayList<>();
            for (LootPool pool : pools) {
                out.addAll(pool.roll(ctx));
            }
            return out;
        }
    }

    public static class LootPool {
        public int rolls = 1;
        public float bonusRolls = 0f;
        public final List<LootCondition> conditions = new ArrayList<>();
        public final List<LootEntry> entries = new ArrayList<>();

        public LootPool rolls(int rolls) {
            this.rolls = Math.max(0, rolls);
            return this;
        }

        public LootPool bonusRolls(float bonusRolls) {
            this.bonusRolls = Math.max(0f, bonusRolls);
            return this;
        }

        public LootPool when(LootCondition condition) {
            if (condition != null) conditions.add(condition);
            return this;
        }

        public LootPool add(LootEntry entry) {
            if (entry != null) entries.add(entry);
            return this;
        }

        public List<Item> roll(AtomBundle ctx) {
            List<Item> out = new ArrayList<>();
            if (!matches(ctx, conditions)) return out;
            int totalRolls = rolls + bonusRollCount(ctx);
            for (int i = 0; i < totalRolls; i++) {
                LootEntry chosen = chooseEntry(ctx);
                if (chosen != null) out.addAll(chosen.generate(ctx));
            }
            return out;
        }

        public LootEntry chooseEntry(AtomBundle ctx) {
            List<LootEntry> candidates = new ArrayList<>();
            int totalWeight = 0;
            for (LootEntry entry : entries) {
                int w = entry.weight(ctx);
                if (w > 0 && entry.matches(ctx)) {
                    candidates.add(entry);
                    totalWeight += w;
                }
            }
            if (candidates.isEmpty() || totalWeight <= 0) return null;
            int ticket = Random.Int(totalWeight);
            for (LootEntry entry : candidates) {
                ticket -= entry.weight(ctx);
                if (ticket < 0) return entry;
            }
            return null;
        }

        public int bonusRollCount(AtomBundle ctx) {
            int x = ctx.getInt("bonusRollX", 0);
            double v = Math.max(0d, bonusRolls * (1 + x / 100.0));
            int whole = (int) v;
            double frac = v - whole;
            return whole + (frac > 0d && Random.Float() < (float) frac ? 1 : 0);
        }
    }

    public abstract static class BaseEntry<T extends BaseEntry<T>> implements LootEntry {
        public int weight;
        public int quality = 0;
        public final List<LootCondition> conditions = new ArrayList<>();
        public final List<LootFunction> functions = new ArrayList<>();

        public BaseEntry(int weight) {
            this.weight = Math.max(0, weight);
        }

        public T weight(int weight) {
            this.weight = Math.max(0, weight);
            return self();
        }

        public T quality(int quality) {
            this.quality = quality;
            return self();
        }

        public T when(LootCondition condition) {
            if (condition != null) conditions.add(condition);
            return self();
        }

        public T apply(LootFunction function) {
            if (function != null) functions.add(function);
            return self();
        }

        @Override
        public final boolean matches(AtomBundle ctx) {
            return LootRegistry.matches(ctx, conditions);
        }

        @Override
        public final int weight(AtomBundle ctx) {
            int exp = ctx.getInt("quantityBonusRate", 0);
            return Math.max(0, weight + (int) Math.pow(quality, exp));
        }

        @Override
        public final List<Item> generate(AtomBundle ctx) {
            List<Item> generated = create(ctx);
            if (functions.isEmpty()) return generated;
            List<Item> result = new ArrayList<>();
            for (Item item : generated) {
                Item current = item;
                for (LootFunction f : functions) {
                    if (current == null) break;
                    current = f.apply(current, ctx);
                }
                if (current != null) result.add(current);
            }
            return result;
        }

        public abstract List<Item> create(AtomBundle ctx);

        @SuppressWarnings("unchecked")
        public T self() { return (T) this; }
    }

    public static class ItemEntry extends BaseEntry<ItemEntry> {
        public final Supplier<Item> supplier;

        public ItemEntry(int weight, Class<? extends Item> cls) {
            this(weight, () -> Reflection.newInstance(cls));
        }

        public ItemEntry(int weight, Supplier<Item> supplier) {
            super(weight);
            this.supplier = supplier;
        }

        @Override
        public List<Item> create(AtomBundle ctx) {
            List<Item> out = new ArrayList<>();
            if (supplier == null) return out;
            Item item = supplier.get();
            if (item != null) out.add(item);
            return out;
        }
    }

    public static class CategoryEntry extends BaseEntry<CategoryEntry> {
        public final Generator.Category category;
        public final boolean useDefaults;

        public CategoryEntry(int weight, Generator.Category category) {
            this(weight, category, true);
        }

        public CategoryEntry(int weight, Generator.Category category, boolean useDefaults) {
            super(weight);
            this.category = category;
            this.useDefaults = useDefaults;
        }

        @Override
        public List<Item> create(AtomBundle ctx) {
            List<Item> out = new ArrayList<>();
            if (category == null) return out;
            Item item = useDefaults ? Generator.randomUsingDefaults(category) : Generator.random(category);
            if (item != null) out.add(item);
            return out;
        }
    }

    public static class TableEntry extends BaseEntry<TableEntry> {
        public final String tableId;

        public TableEntry(int weight, String tableId) {
            super(weight);
            this.tableId = tableId;
        }

        @Override
        public List<Item> create(AtomBundle ctx) {
            return roll(tableId, ctx);
        }
    }

    public static class EmptyEntry extends BaseEntry<EmptyEntry> {
        public EmptyEntry(int weight) {
            super(weight);
        }

        @Override
        public List<Item> create(AtomBundle ctx) {
            return new ArrayList<>();
        }
    }

    public static boolean matches(AtomBundle ctx, List<LootCondition> conditions) {
        for (LootCondition c : conditions) {
            if (c != null && !c.test(ctx)) return false;
        }
        return true;
    }
}
