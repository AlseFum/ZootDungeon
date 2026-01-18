package com.zootdungeon.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.items.Heap;
import com.zootdungeon.items.Item;
import com.watabou.utils.PathFinder;

public final class Select {
    
    private Select() {}
    
    public interface Command<T> {
        Set<T> execute(Set<T> input);
    }
    
    public static PlaceBuilder place() {
        return new PlaceBuilder();
    }
    
    public static class PlaceBuilder {
        private List<Command<Integer>> commands = new ArrayList<>();
        
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
        
        public PlaceBuilder where(CharBuilder who) {
            commands.add(new PlaceCommandWhere(who));
            return this;
        }
        
        public PlaceBuilder and(PlaceBuilder other) {
            commands.add(new PlaceCommandAnd(other));
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
    
    private static class PlaceCommandAnd implements Command<Integer> {
        private PlaceBuilder other;
        PlaceCommandAnd(PlaceBuilder other) {
            this.other = other;
        }
        @Override
        public Set<Integer> execute(Set<Integer> input) {
            if (input == null) {
                input = new HashSet<>();
            }
            Set<Integer> rightPlaces = other.query();
            Set<Integer> result = new HashSet<>(input);
            result.retainAll(rightPlaces);
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
        
        public HeapBuilder onGround(PlaceBuilder source) {
            commands.add(new HeapCommandOnGround(source));
            return this;
        }
        
        public HeapBuilder include(Class<? extends Item> itemClass) {
            commands.add(new HeapCommandInclude(itemClass));
            return this;
        }
        
        public HeapBuilder exclude(Class<? extends Item> itemClass) {
            commands.add(new HeapCommandExclude(itemClass));
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
                if (heap != null && heap.peek() != null && itemClass.isInstance(heap.peek())) {
                    result.add(heap);
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
                if (heap != null && (heap.peek() == null || !itemClass.isInstance(heap.peek()))) {
                    result.add(heap);
                }
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
    
    public static CharBuilder chars() {
        return new CharBuilder();
    }
    
    public static class CharBuilder {
        private List<Command<Char>> commands = new ArrayList<>();
        
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
        
        public CharBuilder that(Predicate<Char> predicate) {
            commands.add(new CharCommandThat(predicate));
            return this;
        }
        
        public CharBuilder and(CharBuilder other) {
            commands.add(new CharCommandAnd(other));
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
            if (input == null) {
                input = new HashSet<>();
            }
            Set<Char> rightChars = other.query();
            Set<Char> result = new HashSet<>(input);
            result.retainAll(rightChars);
            return result;
        }
    }

}
