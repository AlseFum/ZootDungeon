package com.zootdungeon.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.blobs.Blob;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.items.Heap;
import com.zootdungeon.items.Item;
import com.watabou.utils.PathFinder;

public final class Select {

    /**
     * Select DSL cheatsheet (核心语法总览)
     *
     * <p>Place (position / cell index): {@link #place()}</p>
     * <ul>
     *   <li><b>source</b>: {@code point(pos)}, {@code point(x,y)}, {@code rect(...)}, {@code square(...)}, {@code all()}</li>
     *   <li><b>of</b>: {@code of(Set<Integer>)}, {@code of(PlaceBuilder)} (显式指定输入源，便于接收“实际/selector”)</li>
     *   <li><b>combine</b>: {@code and(...)}, {@code or(...)}, {@code except(...)} (alias: {@code but(...)})</li>
     *   <li><b>shape ops</b>: {@code borderOf()}, {@code expandAt(n)}, {@code shrink(n)}, {@code expand(n)}</li>
     *   <li><b>where</b>: {@code where(Char/CharBuilder)}, {@code where(Heap/HeapBuilder)}</li>
     * </ul>
     *
     * <p>Heap: {@link #heap()}</p>
     * <ul>
     *   <li><b>source</b>: {@code at(PlaceBuilder)}, {@code at(pos)}, {@code all()}</li>
     *   <li><b>of</b>: {@code of(Set<Heap>)}, {@code of(HeapBuilder)}, {@code of(Heap)}</li>
     *   <li><b>filter</b>: {@code include(Item/Class)}, {@code exclude(Item/Class)}, {@code empty()}, {@code that(...)}</li>
     *   <li><b>combine</b>: {@code and(...)}, {@code or(...)}, {@code except(...)}</li>
     * </ul>
     *
     * <p>Char: {@link #chars()}</p>
     * <ul>
     *   <li><b>source</b>: {@code all()}, {@code ally()}, {@code enemy()}, {@code theEnemy()}, {@code theAlly()}, {@code theBlob(BlobClass)}</li>
     *   <li><b>of</b>: {@code of(Set<Char>)}, {@code of(CharBuilder)}, {@code of(Char)}</li>
     *   <li><b>filter</b>: {@code withProperty(...)}, {@code withBuff(Buff/Class)}, {@code seen()}, {@code at(PlaceBuilder)}, {@code mayDrop(Item/Class)}, {@code that(...)}</li>
     *   <li><b>combine</b>: {@code and(...)}, {@code or(...)}, {@code except(...)}</li>
     * </ul>
     *
     * <p>Example:</p>
     * <pre>
     * Set&lt;Integer&gt; border = Select.place().rect(5,5,6,6).borderOf().expandAt(2).query();
     * Set&lt;Heap&gt; hs = Select.heap().at(borderSel).include(Gold.class).query();
     * Set&lt;Char&gt; enemies = Select.chars().enemy().seen().at(Select.place().of(border)).query();
     * </pre>
     */
    
    private Select() {}
    
    public interface Command<T> {
        Set<T> execute(Set<T> input);
    }
    
    public static PlaceBuilder place() {
        return new PlaceBuilder();
    }

    /**
     * 预制：先 {@link PathFinder#buildDistanceMap(int, boolean[], int)}，再取路径步数在 {@code (0, maxSteps]} 内的格子
     *（与 {@link PathFinder#distance} 一致）。
     */
    public static PlaceBuilder placePathRing(int origin, boolean[] passable, int maxSteps) {
        PathFinder.buildDistanceMap(origin, passable, maxSteps);
        return place().all().that(
                i -> i >= 0 && i < PathFinder.distance.length
                        && PathFinder.distance[i] <= maxSteps
                        && PathFinder.distance[i] > 0);
    }

    public static class PlaceBuilder {
        private List<Command<Integer>> commands = new ArrayList<>();

        /** 显式指定输入源（selector） */
        public PlaceBuilder of(PlaceBuilder other) {
            commands.add(new PlaceCommandOfBuilder(other));
            return this;
        }

        /** 显式指定输入源（实际集合） */
        public PlaceBuilder of(Set<Integer> places) {
            commands.add(new PlaceCommandOfSet(places));
            return this;
        }

        /** 显式指定输入源（单点） */
        public PlaceBuilder of(int pos) {
            return of(Select.place().point(pos));
        }

        /** 单点（以格子 index 计） */
        public PlaceBuilder point(int pos) {
            commands.add(new PlaceCommandPoint(pos));
            return this;
        }

        /** 单点（以坐标计） */
        public PlaceBuilder point(int x, int y) {
            return at(x, y);
        }

        /** 全图所有位置 */
        public PlaceBuilder all() {
            commands.add(new PlaceCommandAll());
            return this;
        }

        /** 基于当前选择做过滤 */
        public PlaceBuilder that(Predicate<Integer> predicate) {
            commands.add(new PlaceCommandThat(predicate));
            return this;
        }
        
        public PlaceBuilder rect(int x, int y, int width, int height) {
            commands.add(new PlaceCommandRect(x, y, width, height));
            return this;
        }
        
        public PlaceBuilder square(int x, int y, int size) {
            commands.add(new PlaceCommandSquare(x, y, size));
            return this;
        }
        
        public PlaceBuilder at(int x, int y) {
            commands.add(new PlaceCommandAbsolute(x, y));
            return this;
        }
        
        public PlaceBuilder expand() {
            commands.add(new PlaceCommandExpand(1));
            return this;
        }
        
        public PlaceBuilder expand(int scale) {
            commands.add(new PlaceCommandExpand(scale));
            return this;
        }

        /** 只从边界向外扩展（包含原图形）。 */
        public PlaceBuilder expandAt() {
            return expandAt(1);
        }

        /** 只从边界向外扩展（包含原图形）。 */
        public PlaceBuilder expandAt(int scale) {
            commands.add(new PlaceCommandExpandAt(scale));
            return this;
        }

        /** 取当前图形边界（4邻域）。 */
        public PlaceBuilder borderOf() {
            commands.add(new PlaceCommandBorderOf());
            return this;
        }

        /** 向内收缩（腐蚀），按边界移除。 */
        public PlaceBuilder shrink() {
            return shrink(1);
        }

        /** 向内收缩（腐蚀），按边界移除。 */
        public PlaceBuilder shrink(int scale) {
            commands.add(new PlaceCommandShrink(scale));
            return this;
        }
        
        public PlaceBuilder where(CharBuilder who) {
            commands.add(new PlaceCommandWhere(who));
            return this;
        }

        /** where(Char) */
        public PlaceBuilder where(Char who) {
            return where(Select.chars().that(ch -> ch == who));
        }

        /** where(Heap selector) */
        public PlaceBuilder where(HeapBuilder heaps) {
            commands.add(new PlaceCommandWhereHeap(heaps));
            return this;
        }

        /** where(Heap) */
        public PlaceBuilder where(Heap heap) {
            return where(Select.heap().that(h -> h == heap));
        }
        
        public PlaceBuilder and(PlaceBuilder other) {
            commands.add(new PlaceCommandAnd(other));
            return this;
        }

        public PlaceBuilder or(PlaceBuilder other) {
            commands.add(new PlaceCommandOr(other));
            return this;
        }

        public PlaceBuilder except(PlaceBuilder exclude) {
            commands.add(new PlaceCommandExcept(exclude));
            return this;
        }
        
        public PlaceBuilder but(PlaceBuilder exclude) {
            commands.add(new PlaceCommandBut(exclude));
            return this;
        }
        
        public Set<Integer> query() {
            Set<Integer> result = null;
            for (Command<Integer> cmd : commands) {
                result = cmd.execute(result);
            }
            return result != null ? result : new HashSet<>();
        }
    }
    
    private static class PlaceCommandRect implements Command<Integer> {
        private int x, y, width, height;
        PlaceCommandRect(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
        @Override
        public Set<Integer> execute(Set<Integer> input) {
            Set<Integer> result = new HashSet<>();
            if (Dungeon.level != null) {
                int levelWidth = Dungeon.level.width();
                int levelHeight = Dungeon.level.length() / levelWidth;
                for (int i = x; i < x + width; i++) {
                    for (int j = y; j < y + height; j++) {
                        if (i >= 0 && i < levelWidth && j >= 0 && j < levelHeight) {
                            int pos = i + j * levelWidth;
                            if (pos >= 0 && pos < Dungeon.level.length()) {
                                result.add(pos);
                            }
                        }
                    }
                }
            }
            return result;
        }
    }
    
    private static class PlaceCommandSquare implements Command<Integer> {
        private int x, y, size;
        PlaceCommandSquare(int x, int y, int size) {
            this.x = x;
            this.y = y;
            this.size = size;
        }
        @Override
        public Set<Integer> execute(Set<Integer> input) {
            Set<Integer> result = new HashSet<>();
            if (Dungeon.level != null) {
                for (int i = x; i < x + size; i++) {
                    for (int j = y; j < y + size; j++) {
                        int pos = i + j * Dungeon.level.width();
                        if (pos >= 0 && pos < Dungeon.level.length()) {
                            result.add(pos);
                        }
                    }
                }
            }
            return result;
        }
    }
    
    private static class PlaceCommandAbsolute implements Command<Integer> {
        private int x, y;
        PlaceCommandAbsolute(int x, int y) {
            this.x = x;
            this.y = y;
        }
        @Override
        public Set<Integer> execute(Set<Integer> input) {
            Set<Integer> result = new HashSet<>();
            if (Dungeon.level != null) {
                int pos = x + y * Dungeon.level.width();
                if (pos >= 0 && pos < Dungeon.level.length()) {
                    result.add(pos);
                }
            }
            return result;
        }
    }
    
    private static class PlaceCommandExpand implements Command<Integer> {
        private int scale;
        PlaceCommandExpand(int scale) {
            this.scale = scale;
        }
        @Override
        public Set<Integer> execute(Set<Integer> input) {
            if (input == null || input.isEmpty()) {
                return new HashSet<>();
            }
            
            Set<Integer> expandedPlaces = new HashSet<>(input);
            
            Set<Integer> current = input;
            for (int s = 0; s <= scale; s++) {
                Set<Integer> next = new HashSet<>();
                for (int place : current) {
                    if (place >= 0 && place < Dungeon.level.length()) {
                        for (int offset : PathFinder.NEIGHBOURS8) {
                            int newPos = place + offset;
                            if (newPos >= 0 && newPos < Dungeon.level.length()) {
                                if (expandedPlaces.add(newPos)) {
                                    next.add(newPos);
                                }
                            }
                        }
                    }
                }
                current = next;
            }
            
            return expandedPlaces;
        }
    }

    private static class PlaceCommandPoint implements Command<Integer> {
        private final int pos;
        PlaceCommandPoint(int pos) {
            this.pos = pos;
        }
        @Override
        public Set<Integer> execute(Set<Integer> input) {
            Set<Integer> result = new HashSet<>();
            if (Dungeon.level != null && pos >= 0 && pos < Dungeon.level.length()) {
                result.add(pos);
            }
            return result;
        }
    }

    private static class PlaceCommandOfBuilder implements Command<Integer> {
        private final PlaceBuilder other;
        PlaceCommandOfBuilder(PlaceBuilder other) {
            this.other = other;
        }
        @Override
        public Set<Integer> execute(Set<Integer> input) {
            return other != null ? other.query() : new HashSet<>();
        }
    }

    private static class PlaceCommandOfSet implements Command<Integer> {
        private final Set<Integer> places;
        PlaceCommandOfSet(Set<Integer> places) {
            this.places = places;
        }
        @Override
        public Set<Integer> execute(Set<Integer> input) {
            return places != null ? new HashSet<>(places) : new HashSet<>();
        }
    }

    private static class PlaceCommandAll implements Command<Integer> {
        @Override
        public Set<Integer> execute(Set<Integer> input) {
            Set<Integer> result = new HashSet<>();
            if (Dungeon.level == null) return result;
            for (int i = 0; i < Dungeon.level.length(); i++) {
                result.add(i);
            }
            return result;
        }
    }

    private static class PlaceCommandThat implements Command<Integer> {
        private final Predicate<Integer> predicate;
        PlaceCommandThat(Predicate<Integer> predicate) {
            this.predicate = predicate;
        }
        @Override
        public Set<Integer> execute(Set<Integer> input) {
            if (input == null) input = new HashSet<>();
            if (predicate == null) return input;
            Set<Integer> result = new HashSet<>();
            for (int p : input) {
                if (predicate.test(p)) result.add(p);
            }
            return result;
        }
    }

    private static class PlaceCommandBorderOf implements Command<Integer> {
        @Override
        public Set<Integer> execute(Set<Integer> input) {
            if (input == null || input.isEmpty() || Dungeon.level == null) return new HashSet<>();
            Set<Integer> result = new HashSet<>();
            for (int p : input) {
                boolean border = false;
                for (int off : PathFinder.NEIGHBOURS4) {
                    int n = p + off;
                    if (n < 0 || n >= Dungeon.level.length() || !input.contains(n)) {
                        border = true;
                        break;
                    }
                }
                if (border) result.add(p);
            }
            return result;
        }
    }

    private static class PlaceCommandExpandAt implements Command<Integer> {
        private final int scale;
        PlaceCommandExpandAt(int scale) {
            this.scale = Math.max(0, scale);
        }
        @Override
        public Set<Integer> execute(Set<Integer> input) {
            if (input == null || input.isEmpty() || Dungeon.level == null) return new HashSet<>();
            Set<Integer> base = new HashSet<>(input);
            Set<Integer> frontier = new PlaceCommandBorderOf().execute(base);
            Set<Integer> added = new HashSet<>();
            for (int step = 0; step < scale; step++) {
                Set<Integer> next = new HashSet<>();
                for (int p : frontier) {
                    for (int off : PathFinder.NEIGHBOURS8) {
                        int n = p + off;
                        if (n < 0 || n >= Dungeon.level.length()) continue;
                        if (base.contains(n)) continue;
                        if (added.add(n)) next.add(n);
                    }
                }
                frontier = next;
                if (frontier.isEmpty()) break;
            }
            base.addAll(added);
            return base;
        }
    }

    private static class PlaceCommandShrink implements Command<Integer> {
        private final int scale;
        PlaceCommandShrink(int scale) {
            this.scale = Math.max(0, scale);
        }
        @Override
        public Set<Integer> execute(Set<Integer> input) {
            if (input == null || input.isEmpty() || Dungeon.level == null) return new HashSet<>();
            Set<Integer> cur = new HashSet<>(input);
            for (int step = 0; step < scale; step++) {
                Set<Integer> border = new PlaceCommandBorderOf().execute(cur);
                cur.removeAll(border);
                if (cur.isEmpty()) break;
            }
            return cur;
        }
    }
    
    private static class PlaceCommandWhere implements Command<Integer> {
        private CharBuilder who;
        PlaceCommandWhere(CharBuilder who) {
            this.who = who;
        }
        @Override
        public Set<Integer> execute(Set<Integer> input) {
            Set<Integer> result = new HashSet<>();
            if (who == null || Dungeon.level == null) {
                return result;
            }
            Set<Char> chars = who.query();
            for (Char ch : chars) {
                int pos = ch.pos;
                if (pos >= 0 && pos < Dungeon.level.length()) {
                    result.add(pos);
                }
            }
            return result;
        }
    }

    private static class PlaceCommandWhereHeap implements Command<Integer> {
        private final HeapBuilder heaps;
        PlaceCommandWhereHeap(HeapBuilder heaps) {
            this.heaps = heaps;
        }
        @Override
        public Set<Integer> execute(Set<Integer> input) {
            Set<Integer> result = new HashSet<>();
            if (heaps == null || Dungeon.level == null) return result;
            for (Heap h : heaps.query()) {
                if (h != null && h.pos >= 0 && h.pos < Dungeon.level.length()) {
                    result.add(h.pos);
                }
            }
            return result;
        }
    }
    
    private static class PlaceCommandAnd implements Command<Integer> {
        private PlaceBuilder other;
        PlaceCommandAnd(PlaceBuilder other) {
            this.other = other;
        }
        @Override
        public Set<Integer> execute(Set<Integer> input) {
            Set<Integer> rightPlaces = other.query();
            if (input == null) return rightPlaces;
            Set<Integer> result = new HashSet<>(input);
            result.retainAll(rightPlaces);
            return result;
        }
    }

    private static class PlaceCommandOr implements Command<Integer> {
        private final PlaceBuilder other;
        PlaceCommandOr(PlaceBuilder other) {
            this.other = other;
        }
        @Override
        public Set<Integer> execute(Set<Integer> input) {
            Set<Integer> rightPlaces = other.query();
            if (input == null) return rightPlaces;
            Set<Integer> result = new HashSet<>(input);
            result.addAll(rightPlaces);
            return result;
        }
    }

    private static class PlaceCommandExcept implements Command<Integer> {
        private final PlaceBuilder exclude;
        PlaceCommandExcept(PlaceBuilder exclude) {
            this.exclude = exclude;
        }
        @Override
        public Set<Integer> execute(Set<Integer> input) {
            if (input == null) return new HashSet<>();
            Set<Integer> excludePlaces = exclude.query();
            Set<Integer> result = new HashSet<>(input);
            result.removeAll(excludePlaces);
            return result;
        }
    }
    
    private static class PlaceCommandBut implements Command<Integer> {
        private PlaceBuilder exclude;
        PlaceCommandBut(PlaceBuilder exclude) {
            this.exclude = exclude;
        }
        @Override
        public Set<Integer> execute(Set<Integer> input) {
            if (input == null) {
                return new HashSet<>();
            }
            Set<Integer> excludePlaces = exclude.query();
            Set<Integer> result = new HashSet<>(input);
            result.removeAll(excludePlaces);
            return result;
        }
    }
    
    public static HeapBuilder heap() {
        return new HeapBuilder();
    }
    
    public static class HeapBuilder {
        private List<Command<Heap>> commands = new ArrayList<>();

        /** 显式指定输入源（selector） */
        public HeapBuilder of(HeapBuilder other) {
            commands.add(new HeapCommandOfBuilder(other));
            return this;
        }

        /** 显式指定输入源（实际集合） */
        public HeapBuilder of(Set<Heap> heaps) {
            commands.add(new HeapCommandOfSet(heaps));
            return this;
        }

        /** 显式指定输入源（单个 heap） */
        public HeapBuilder of(Heap heap) {
            Set<Heap> s = new HashSet<>();
            if (heap != null) s.add(heap);
            return of(s);
        }
        
        public HeapBuilder onGround(PlaceBuilder source) {
            commands.add(new HeapCommandOnGround(source));
            return this;
        }

        /** alias for onGround */
        public HeapBuilder at(PlaceBuilder source) {
            return onGround(source);
        }

        public HeapBuilder at(int pos) {
            return at(Select.place().point(pos));
        }

        public HeapBuilder at(int x, int y) {
            return at(Select.place().at(x, y));
        }

        /** 全图所有 heap（有堆叠物的格子） */
        public HeapBuilder all() {
            commands.add(new HeapCommandAll());
            return this;
        }

        /** heap.items 为空（一般很少见） */
        public HeapBuilder empty() {
            commands.add(new HeapCommandEmpty());
            return this;
        }
        
        public HeapBuilder include(Class<? extends Item> itemClass) {
            commands.add(new HeapCommandInclude(itemClass));
            return this;
        }

        public HeapBuilder include(Item item) {
            return include(item != null ? item.getClass() : null);
        }
        
        public HeapBuilder exclude(Class<? extends Item> itemClass) {
            commands.add(new HeapCommandExclude(itemClass));
            return this;
        }

        public HeapBuilder exclude(Item item) {
            return exclude(item != null ? item.getClass() : null);
        }

        public HeapBuilder and(HeapBuilder other) {
            commands.add(new HeapCommandAnd(other));
            return this;
        }

        public HeapBuilder or(HeapBuilder other) {
            commands.add(new HeapCommandOr(other));
            return this;
        }

        public HeapBuilder except(HeapBuilder exclude) {
            commands.add(new HeapCommandExcept(exclude));
            return this;
        }
        
        public HeapBuilder that(Predicate<Heap> predicate) {
            commands.add(new HeapCommandThat(predicate));
            return this;
        }
        
        public Set<Heap> query() {
            Set<Heap> result = null;
            for (Command<Heap> cmd : commands) {
                result = cmd.execute(result);
            }
            return result != null ? result : new HashSet<>();
        }
    }
    
    private static class HeapCommandOnGround implements Command<Heap> {
        private PlaceBuilder source;
        HeapCommandOnGround(PlaceBuilder source) {
            this.source = source;
        }
        @Override
        public Set<Heap> execute(Set<Heap> input) {
            if (source == null) {
                return new HashSet<>();
            }
            Set<Heap> result = new HashSet<>();
            for (int place : source.query()) {
                Heap heap = Dungeon.level.heaps.get(place);
                if (heap != null) {
                    result.add(heap);
                }
            }
            return result;
        }
    }

    private static class HeapCommandAll implements Command<Heap> {
        @Override
        public Set<Heap> execute(Set<Heap> input) {
            Set<Heap> result = new HashSet<>();
            if (Dungeon.level == null) return result;
            for (int i = 0; i < Dungeon.level.length(); i++) {
                Heap h = Dungeon.level.heaps.get(i);
                if (h != null) result.add(h);
            }
            return result;
        }
    }

    private static class HeapCommandOfBuilder implements Command<Heap> {
        private final HeapBuilder other;
        HeapCommandOfBuilder(HeapBuilder other) {
            this.other = other;
        }
        @Override
        public Set<Heap> execute(Set<Heap> input) {
            return other != null ? other.query() : new HashSet<>();
        }
    }

    private static class HeapCommandOfSet implements Command<Heap> {
        private final Set<Heap> heaps;
        HeapCommandOfSet(Set<Heap> heaps) {
            this.heaps = heaps;
        }
        @Override
        public Set<Heap> execute(Set<Heap> input) {
            return heaps != null ? new HashSet<>(heaps) : new HashSet<>();
        }
    }

    private static class HeapCommandEmpty implements Command<Heap> {
        @Override
        public Set<Heap> execute(Set<Heap> input) {
            Set<Heap> source = input != null ? input : Select.heap().all().query();
            Set<Heap> result = new HashSet<>();
            for (Heap h : source) {
                if (h != null && (h.items == null || h.items.isEmpty())) {
                    result.add(h);
                }
            }
            return result;
        }
    }
    
    private static class HeapCommandInclude implements Command<Heap> {
        private Class<? extends Item> itemClass;
        HeapCommandInclude(Class<? extends Item> itemClass) {
            this.itemClass = itemClass;
        }
        @Override
        public Set<Heap> execute(Set<Heap> input) {
            if (input == null) {
                return new HashSet<>();
            }
            if (itemClass == null) {
                return input;
            }
            Set<Heap> result = new HashSet<>();
            for (Heap heap : input) {
                if (heap == null || heap.items == null) continue;
                for (Item it : heap.items) {
                    if (it != null && itemClass.isInstance(it)) {
                        result.add(heap);
                        break;
                    }
                }
            }
            return result;
        }
    }
    
    private static class HeapCommandExclude implements Command<Heap> {
        private Class<? extends Item> itemClass;
        HeapCommandExclude(Class<? extends Item> itemClass) {
            this.itemClass = itemClass;
        }
        @Override
        public Set<Heap> execute(Set<Heap> input) {
            if (input == null) {
                return new HashSet<>();
            }
            if (itemClass == null) {
                return input;
            }
            Set<Heap> result = new HashSet<>();
            for (Heap heap : input) {
                if (heap == null) continue;
                boolean found = false;
                if (heap.items != null) {
                    for (Item it : heap.items) {
                        if (it != null && itemClass.isInstance(it)) {
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) result.add(heap);
            }
            return result;
        }
    }
    
    private static class HeapCommandThat implements Command<Heap> {
        private Predicate<Heap> predicate;
        HeapCommandThat(Predicate<Heap> predicate) {
            this.predicate = predicate;
        }
        @Override
        public Set<Heap> execute(Set<Heap> input) {
            if (input == null) {
                return new HashSet<>();
            }
            if (predicate == null) {
                return input;
            }
            Set<Heap> result = new HashSet<>();
            for (Heap heap : input) {
                if (heap != null && predicate.test(heap)) {
                    result.add(heap);
                }
            }
            return result;
        }
    }

    private static class HeapCommandAnd implements Command<Heap> {
        private final HeapBuilder other;
        HeapCommandAnd(HeapBuilder other) {
            this.other = other;
        }
        @Override
        public Set<Heap> execute(Set<Heap> input) {
            Set<Heap> right = other.query();
            if (input == null) return right;
            Set<Heap> result = new HashSet<>(input);
            result.retainAll(right);
            return result;
        }
    }

    private static class HeapCommandOr implements Command<Heap> {
        private final HeapBuilder other;
        HeapCommandOr(HeapBuilder other) {
            this.other = other;
        }
        @Override
        public Set<Heap> execute(Set<Heap> input) {
            Set<Heap> right = other.query();
            if (input == null) return right;
            Set<Heap> result = new HashSet<>(input);
            result.addAll(right);
            return result;
        }
    }

    private static class HeapCommandExcept implements Command<Heap> {
        private final HeapBuilder exclude;
        HeapCommandExcept(HeapBuilder exclude) {
            this.exclude = exclude;
        }
        @Override
        public Set<Heap> execute(Set<Heap> input) {
            if (input == null) return new HashSet<>();
            Set<Heap> ex = exclude.query();
            Set<Heap> result = new HashSet<>(input);
            result.removeAll(ex);
            return result;
        }
    }
    
    public static CharBuilder chars() {
        return new CharBuilder();
    }
    
    public static class CharBuilder {
        private List<Command<Char>> commands = new ArrayList<>();

        /** 显式指定输入源（selector） */
        public CharBuilder of(CharBuilder other) {
            commands.add(new CharCommandOfBuilder(other));
            return this;
        }

        /** 显式指定输入源（实际集合） */
        public CharBuilder of(Set<Char> chars) {
            commands.add(new CharCommandOfSet(chars));
            return this;
        }

        /** 显式指定输入源（单个 char） */
        public CharBuilder of(Char ch) {
            Set<Char> s = new HashSet<>();
            if (ch != null) s.add(ch);
            return of(s);
        }

        /** 单个：当前英雄目标 */
        public CharBuilder theEnemy() {
            commands.add(new CharCommandTheEnemy());
            return this;
        }

        /** 单个：当前英雄（自身） */
        public CharBuilder theAlly() {
            commands.add(new CharCommandTheAlly());
            return this;
        }

        /** 在指定 Blob 中的所有 Char */
        public CharBuilder theBlob(Class<? extends Blob> blobClass) {
            commands.add(new CharCommandInBlob(blobClass));
            return this;
        }
        
        public CharBuilder ally() {
            commands.add(new CharCommandAlly());
            return this;
        }
        
        public CharBuilder enemy() {
            commands.add(new CharCommandEnemy());
            return this;
        }
        
        public CharBuilder all() {
            commands.add(new CharCommandAll());
            return this;
        }
        
        public CharBuilder withBuff(Class<? extends Buff> buffClass) {
            commands.add(new CharCommandWithBuff(buffClass));
            return this;
        }

        public CharBuilder withBuff(Buff buff) {
            return withBuff(buff != null ? buff.getClass() : null);
        }

        public CharBuilder withProperty(Char.Property property) {
            commands.add(new CharCommandWithProperty(property));
            return this;
        }

        /** 英雄可见（heroFOV）且不隐身（invisible <= 0） */
        public CharBuilder seen() {
            commands.add(new CharCommandSeen());
            return this;
        }

        public CharBuilder at(PlaceBuilder places) {
            commands.add(new CharCommandAt(places));
            return this;
        }

        public CharBuilder at(int pos) {
            return at(Select.place().point(pos));
        }

        /** 可能掉落某类物品（启发式；lootTableId 无法判断时返回 true）。 */
        public CharBuilder mayDrop(Class<? extends Item> itemClass) {
            commands.add(new CharCommandMayDrop(itemClass));
            return this;
        }

        public CharBuilder mayDrop(Item item) {
            return mayDrop(item != null ? item.getClass() : null);
        }
        
        public CharBuilder that(Predicate<Char> predicate) {
            commands.add(new CharCommandThat(predicate));
            return this;
        }
        
        public CharBuilder and(CharBuilder other) {
            commands.add(new CharCommandAnd(other));
            return this;
        }

        public CharBuilder or(CharBuilder other) {
            commands.add(new CharCommandOr(other));
            return this;
        }

        public CharBuilder except(CharBuilder exclude) {
            commands.add(new CharCommandExcept(exclude));
            return this;
        }
        
        public Set<Char> query() {
            Set<Char> result = null;
            for (Command<Char> cmd : commands) {
                result = cmd.execute(result);
            }
            return result != null ? result : new HashSet<>();
        }
    }

    private static class CharCommandTheEnemy implements Command<Char> {
        @Override
        public Set<Char> execute(Set<Char> input) {
            Set<Char> result = new HashSet<>();
            if (Dungeon.hero != null) {
                Char e = Dungeon.hero.enemy();
                if (e != null && e.isAlive()) result.add(e);
            }
            return result;
        }
    }

    private static class CharCommandOfBuilder implements Command<Char> {
        private final CharBuilder other;
        CharCommandOfBuilder(CharBuilder other) {
            this.other = other;
        }
        @Override
        public Set<Char> execute(Set<Char> input) {
            return other != null ? other.query() : new HashSet<>();
        }
    }

    private static class CharCommandOfSet implements Command<Char> {
        private final Set<Char> chars;
        CharCommandOfSet(Set<Char> chars) {
            this.chars = chars;
        }
        @Override
        public Set<Char> execute(Set<Char> input) {
            return chars != null ? new HashSet<>(chars) : new HashSet<>();
        }
    }

    private static class CharCommandTheAlly implements Command<Char> {
        @Override
        public Set<Char> execute(Set<Char> input) {
            Set<Char> result = new HashSet<>();
            if (Dungeon.hero != null && Dungeon.hero.isAlive()) result.add(Dungeon.hero);
            return result;
        }
    }

    private static class CharCommandInBlob implements Command<Char> {
        private final Class<? extends Blob> blobClass;
        CharCommandInBlob(Class<? extends Blob> blobClass) {
            this.blobClass = blobClass;
        }
        @Override
        public Set<Char> execute(Set<Char> input) {
            Set<Char> result = new HashSet<>();
            if (blobClass == null || Dungeon.level == null || Dungeon.level.blobs == null) return result;
            Blob b = Dungeon.level.blobs.get(blobClass);
            if (b == null || b.cur == null) return result;
            for (Char ch : Actor.chars()) {
                if (ch != null && ch.isAlive() && ch.pos >= 0 && ch.pos < b.cur.length && b.cur[ch.pos] > 0) {
                    result.add(ch);
                }
            }
            return result;
        }
    }

    private static class CharCommandWithProperty implements Command<Char> {
        private final Char.Property property;
        CharCommandWithProperty(Char.Property property) {
            this.property = property;
        }
        @Override
        public Set<Char> execute(Set<Char> input) {
            if (property == null) return input != null ? input : new HashSet<>();
            Set<Char> source = input != null ? input : Select.chars().all().query();
            Set<Char> result = new HashSet<>();
            for (Char ch : source) {
                if (ch != null && ch.isAlive() && Char.hasProp(ch, property)) {
                    result.add(ch);
                }
            }
            return result;
        }
    }

    private static class CharCommandSeen implements Command<Char> {
        @Override
        public Set<Char> execute(Set<Char> input) {
            Set<Char> source = input != null ? input : Select.chars().all().query();
            Set<Char> result = new HashSet<>();
            if (Dungeon.level == null || Dungeon.level.heroFOV == null) return result;
            for (Char ch : source) {
                if (ch != null && ch.isAlive() && ch.pos >= 0 && ch.pos < Dungeon.level.heroFOV.length) {
                    if (Dungeon.level.heroFOV[ch.pos] && ch.invisible <= 0) {
                        result.add(ch);
                    }
                }
            }
            return result;
        }
    }

    private static class CharCommandAt implements Command<Char> {
        private final PlaceBuilder places;
        CharCommandAt(PlaceBuilder places) {
            this.places = places;
        }
        @Override
        public Set<Char> execute(Set<Char> input) {
            Set<Char> source = input != null ? input : Select.chars().all().query();
            if (places == null) return source;
            Set<Integer> ps = places.query();
            Set<Char> result = new HashSet<>();
            for (Char ch : source) {
                if (ch != null && ps.contains(ch.pos)) result.add(ch);
            }
            return result;
        }
    }

    private static class CharCommandMayDrop implements Command<Char> {
        private final Class<? extends Item> itemClass;
        CharCommandMayDrop(Class<? extends Item> itemClass) {
            this.itemClass = itemClass;
        }
        @Override
        public Set<Char> execute(Set<Char> input) {
            Set<Char> source = input != null ? input : Select.chars().all().query();
            if (itemClass == null) return source;
            Set<Char> result = new HashSet<>();
            for (Char ch : source) {
                if (!(ch instanceof Mob)) continue;
                Mob m = (Mob) ch;
                // 启发式：尽量不 roll 随机掉落，只检查 loot 元数据；表驱动时无法判断，按“可能”处理。
                try {
                    Field lootTableId = Mob.class.getDeclaredField("lootTableId");
                    lootTableId.setAccessible(true);
                    Object table = lootTableId.get(m);
                    if (table != null) {
                        result.add(ch);
                        continue;
                    }
                } catch (Exception ignored) {}
                try {
                    Field loot = Mob.class.getDeclaredField("loot");
                    loot.setAccessible(true);
                    Object v = loot.get(m);
                    if (v instanceof Class<?> c) {
                        if (itemClass.isAssignableFrom((Class<?>) c)) result.add(ch);
                    } else if (v instanceof Item it) {
                        if (itemClass.isInstance(it)) result.add(ch);
                    }
                } catch (Exception ignored) {}
            }
            return result;
        }
    }
    
    private static class CharCommandAlly implements Command<Char> {
        @Override
        public Set<Char> execute(Set<Char> input) {
            Set<Char> result = new HashSet<>();
            for (Char ch : Actor.chars()) {
                if (ch != null && ch.isAlive() && ch.alignment == Char.Alignment.ALLY) {
                    result.add(ch);
                }
            }
            return result;
        }
    }
    
    private static class CharCommandEnemy implements Command<Char> {
        @Override
        public Set<Char> execute(Set<Char> input) {
            Set<Char> result = new HashSet<>();
            for (Char ch : Actor.chars()) {
                if (ch != null && ch.isAlive() && ch.alignment == Char.Alignment.ENEMY) {
                    result.add(ch);
                }
            }
            return result;
        }
    }
    
    private static class CharCommandAll implements Command<Char> {
        @Override
        public Set<Char> execute(Set<Char> input) {
            Set<Char> result = new HashSet<>();
            for (Char ch : Actor.chars()) {
                if (ch != null && ch.isAlive()) {
                    result.add(ch);
                }
            }
            return result;
        }
    }
    
    private static class CharCommandWithBuff implements Command<Char> {
        private Class<? extends Buff> buffClass;
        CharCommandWithBuff(Class<? extends Buff> buffClass) {
            this.buffClass = buffClass;
        }
        @Override
        public Set<Char> execute(Set<Char> input) {
            if (buffClass == null) {
                return input != null ? input : new HashSet<>();
            }
            Set<Char> source = input != null ? input : Select.chars().all().query();
            Set<Char> result = new HashSet<>();
            for (Char ch : source) {
                if (ch != null && ch.isAlive() && ch.buff(buffClass) != null) {
                    result.add(ch);
                }
            }
            return result;
        }
    }
    
    private static class CharCommandThat implements Command<Char> {
        private Predicate<Char> predicate;
        CharCommandThat(Predicate<Char> predicate) {
            this.predicate = predicate;
        }
        @Override
        public Set<Char> execute(Set<Char> input) {
            if (predicate == null) {
                return input != null ? input : new HashSet<>();
            }
            Set<Char> source = input != null ? input : Select.chars().all().query();
            Set<Char> result = new HashSet<>();
            for (Char ch : source) {
                if (ch != null && predicate.test(ch)) {
                    result.add(ch);
                }
            }
            return result;
        }
    }
    
    private static class CharCommandAnd implements Command<Char> {
        private CharBuilder other;
        CharCommandAnd(CharBuilder other) {
            this.other = other;
        }
        @Override
        public Set<Char> execute(Set<Char> input) {
            Set<Char> rightChars = other.query();
            if (input == null) return rightChars;
            Set<Char> result = new HashSet<>(input);
            result.retainAll(rightChars);
            return result;
        }
    }

    private static class CharCommandOr implements Command<Char> {
        private final CharBuilder other;
        CharCommandOr(CharBuilder other) {
            this.other = other;
        }
        @Override
        public Set<Char> execute(Set<Char> input) {
            Set<Char> right = other.query();
            if (input == null) return right;
            Set<Char> result = new HashSet<>(input);
            result.addAll(right);
            return result;
        }
    }

    private static class CharCommandExcept implements Command<Char> {
        private final CharBuilder exclude;
        CharCommandExcept(CharBuilder exclude) {
            this.exclude = exclude;
        }
        @Override
        public Set<Char> execute(Set<Char> input) {
            if (input == null) return new HashSet<>();
            Set<Char> ex = exclude.query();
            Set<Char> result = new HashSet<>(input);
            result.removeAll(ex);
            return result;
        }
    }

}
