package com.zootdungeon.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class EventBus {

    public static final int DEFAULT_PRIORITY = 16;

    // 全局强类型事件总线存储 - 使用Class作为键
    private static final Map<Class<?>, TypedEventBus<?>> typedBuses = new HashMap<>();
    
    // 全局字符串事件总线存储 - 使用topic作为键（Map-based event bus）
    private static final MapEventBus mapEventBus = new MapEventBus();
    
    @SuppressWarnings("unchecked")
    public static <T> TypedEventBus<T> typed(Class<T> eventClass) {
        return (TypedEventBus<T>) typedBuses.computeIfAbsent(eventClass, k -> new TypedEventBus<>(eventClass));
    }
    
    public static <T, R> void on(Class<T> eventClass, Handler<T, R> handler) {
        on(eventClass, handler, DEFAULT_PRIORITY);
    }

    public static <T> void on(Class<T> eventClass, Consumer<T> handler) {
        on(eventClass, (EventBus.Handler<T, Object>) e -> { handler.accept(e); return null; });
    }

    public static <T, R> void on(Class<T> eventClass, Handler<T, R> handler, int priority) {
        typed(eventClass).addHandler(handler, priority);
    }

    public static <T> void on(Class<T> eventClass, Consumer<T> handler, int priority) {
        on(eventClass, (EventBus.Handler<T, Object>) e -> { handler.accept(e); return null; }, priority);
    }
    
    public static void on(String topic, Handler<Map<String, Object>, Object> handler) {
        on(topic, handler, DEFAULT_PRIORITY);
    }
    
    public static void on(String topic, Handler<Map<String, Object>, Object> handler, int priority) {
        mapEventBus.addHandler(topic, handler, priority);
    }
    
    public static void dispatchMap(String topic, Map<String, Object> data) {
        mapEventBus.dispatch(topic, data);
    }
    
    public static ArrayList<Object> collectMap(String topic, Map<String, Object> data) {
        return mapEventBus.collect(topic, data);
    }
    
    public static void offMap(String topic, Handler<Map<String, Object>, Object> handler) {
        mapEventBus.removeHandler(topic, handler);
    }
    
    public static void clearMap(String topic) {
        mapEventBus.clearTopic(topic);
    }
    
    public static void clearAllMap() {
        mapEventBus.removeAll();
    }
    
    public static <T> void dispatchTyped(T event) {
        if (event == null) return;
        dispatchTyped(event, event.getClass().getName());
    }
    
    public static <T> void dispatchTyped(T event, String topic) {
        if (event == null) return;
        @SuppressWarnings("unchecked")
        TypedEventBus<T> bus = (TypedEventBus<T>) typedBuses.get(event.getClass());
        if (bus != null) {
            bus.dispatch(event, topic);
        }
    }
    
    @SuppressWarnings("unchecked")
    public static <T> ArrayList<Object> collectTyped(T event) {
        if (event == null) return new ArrayList<>();
        return collectTyped(event, event.getClass().getName());
    }
    
    @SuppressWarnings("unchecked")
    public static <T> ArrayList<Object> collectTyped(T event, String topic) {
        if (event == null) return new ArrayList<>();
        TypedEventBus<T> bus = (TypedEventBus<T>) typedBuses.get(event.getClass());
        if (bus != null) {
            return bus.collect(event, topic);
        }
        return new ArrayList<>();
    }
    
    public static <T, R> void off(Class<T> eventClass, Handler<T, R> handler) {
        TypedEventBus<T> bus = typed(eventClass);
        if (bus != null) {
            bus.removeHandler(handler);
        }
    }
    
    public static <T> void clearTyped(Class<T> eventClass) {
        TypedEventBus<?> bus = typedBuses.get(eventClass);
        if (bus != null) {
            bus.removeAll();
        }
    }
    
    public static void clearAllTyped() {
        for (TypedEventBus<?> bus : typedBuses.values()) {
            bus.removeAll();
        }
        typedBuses.clear();
    }
    
    @FunctionalInterface
    public interface Handler<T, R> {
        R handle(T event);
    }

    public static <T> void off(Class<T> eventClass, Consumer<T> handler) {
        off(eventClass, (EventBus.Handler<T, Object>) e -> { handler.accept(e); return null; });
    }
    
    // ========== 内部类：强类型事件总线实现 ==========
    
    public static class TypedEventBus<T> {
        
        // 事件类型
        private final Class<T> eventClass;
        
        // 带优先级的处理器列表（按topic分组）
        private final Map<String, List<PriorityHandler<T>>> handlersByTopic = new HashMap<>();
        
        TypedEventBus(Class<T> eventClass) {
            this.eventClass = eventClass;
        }
        
        public Class<T> getEventClass() {
            return eventClass;
        }
        
        public void addHandler(Handler<T, ?> handler) {
            addHandler(handler, DEFAULT_PRIORITY);
        }
        
        public void addHandler(Handler<T, ?> handler, int priority) {
            String defaultTopic = eventClass.getName();
            addHandler(handler, priority, defaultTopic);
        }
        
        public synchronized void addHandler(Handler<T, ?> handler, int priority, String topic) {
            if (handler == null) {
                throw new IllegalArgumentException("Handler cannot be null");
            }
            
            List<PriorityHandler<T>> handlers = handlersByTopic.computeIfAbsent(topic, k -> new ArrayList<>());
            
            // 检查是否已存在
            if (handlers.stream().anyMatch(ph -> ph.handler.equals(handler))) {
                return;
            }
            
            handlers.add(new PriorityHandler<>(handler, priority));
            handlers.sort(Comparator.comparing((PriorityHandler<T> ph) -> ph.priority).reversed());
        }
        
        public synchronized void removeHandler(Handler<T, ?> handler) {
            for (List<PriorityHandler<T>> handlers : handlersByTopic.values()) {
                handlers.removeIf(ph -> ph.handler.equals(handler));
            }
        }
        
        public synchronized void removeAll() {
            handlersByTopic.clear();
        }
        
        public synchronized void dispatch(T event, String topic) {
            if (event == null) return;
            
            List<PriorityHandler<T>> handlers = handlersByTopic.get(topic);
            if (handlers == null || handlers.isEmpty()) {
                return;
            }
            
            // 创建副本以避免并发修改
            PriorityHandler<T>[] handlerArray = handlers.toArray(new PriorityHandler[0]);
            
            for (PriorityHandler<T> ph : handlerArray) {
                if (!handlers.contains(ph)) continue;
                
                try {
                    ph.handler.handle(event);
                } catch (Exception e) {
                    System.err.println("[EventBus] Error in event handler: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        
        @SuppressWarnings("unchecked")
        public synchronized ArrayList<Object> collect(T event, String topic) {
            ArrayList<Object> results = new ArrayList<>();
            if (event == null) return results;
            
            List<PriorityHandler<T>> handlers = handlersByTopic.get(topic);
            if (handlers == null || handlers.isEmpty()) {
                return results;
            }
            
            // 创建副本以避免并发修改
            PriorityHandler<T>[] handlerArray = handlers.toArray(new PriorityHandler[0]);
            
            for (PriorityHandler<T> ph : handlerArray) {
                if (!handlers.contains(ph)) continue;
                
                try {
                    Object result = ph.handler.handle(event);
                    if (result != null) {
                        results.add(result);
                    }
                } catch (Exception e) {
                    System.err.println("[EventBus] Error in event handler: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            return results;
        }
        
        public int numHandlers(String topic) {
            List<PriorityHandler<T>> handlers = handlersByTopic.get(topic);
            return handlers != null ? handlers.size() : 0;
        }
        
        public int totalHandlers() {
            return handlersByTopic.values().stream()
                .mapToInt(List::size)
                .sum();
        }
        
    }
    
    private static class PriorityHandler<T> {
        final Handler<T, ?> handler;
        final int priority;

        PriorityHandler(Handler<T, ?> handler, int priority) {
            this.handler = handler;
            this.priority = priority;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            PriorityHandler<?> that = (PriorityHandler<?>) obj;
            return handler.equals(that.handler);
        }
        
        @Override
        public int hashCode() {
            return handler.hashCode();
        }
    }
    
    // ========== Map-based EventBus（字符串事件总线） ==========
    
    private static class MapEventBus {
        
        // 带优先级的处理器列表（按topic分组）
        private final Map<String, List<PriorityHandler<Map<String, Object>>>> handlersByTopic = new HashMap<>();
        
        public synchronized void addHandler(String topic, Handler<Map<String, Object>, Object> handler, int priority) {
            if (topic == null || handler == null) {
                throw new IllegalArgumentException("Topic and handler cannot be null");
            }
            
            List<PriorityHandler<Map<String, Object>>> handlers = handlersByTopic.computeIfAbsent(topic, k -> new ArrayList<>());
            
            // 检查是否已存在
            if (handlers.stream().anyMatch(ph -> ph.handler.equals(handler))) {
                return;
            }
            
            handlers.add(new PriorityHandler<>(handler, priority));
            handlers.sort(Comparator.comparing((PriorityHandler<Map<String, Object>> ph) -> ph.priority).reversed());
        }
        
        public synchronized void removeHandler(String topic, Handler<Map<String, Object>, Object> handler) {
            List<PriorityHandler<Map<String, Object>>> handlers = handlersByTopic.get(topic);
            if (handlers != null) {
                handlers.removeIf(ph -> ph.handler.equals(handler));
                if (handlers.isEmpty()) {
                    handlersByTopic.remove(topic);
                }
            }
        }
        
        public synchronized void clearTopic(String topic) {
            handlersByTopic.remove(topic);
        }
        
        public synchronized void removeAll() {
            handlersByTopic.clear();
        }
        
        public synchronized void dispatch(String topic, Map<String, Object> data) {
            if (data == null) return;
            
            List<PriorityHandler<Map<String, Object>>> handlers = handlersByTopic.get(topic);
            if (handlers == null || handlers.isEmpty()) {
                return;
            }
            
            // 创建副本以避免并发修改
            PriorityHandler<Map<String, Object>>[] handlerArray = handlers.toArray(new PriorityHandler[0]);
            
            // 按优先级顺序调用处理器
            for (PriorityHandler<Map<String, Object>> priorityHandler : handlerArray) {
                try {
                    priorityHandler.handler.handle(data);
                } catch (Exception e) {
                    // 处理器异常不应该影响其他处理器
                    e.printStackTrace();
                }
            }
        }
        
        public synchronized ArrayList<Object> collect(String topic, Map<String, Object> data) {
            ArrayList<Object> results = new ArrayList<>();
            if (data == null) return results;
            
            List<PriorityHandler<Map<String, Object>>> handlers = handlersByTopic.get(topic);
            if (handlers == null || handlers.isEmpty()) {
                return results;
            }
            
            // 创建副本以避免并发修改
            PriorityHandler<Map<String, Object>>[] handlerArray = handlers.toArray(new PriorityHandler[0]);
            
            // 按优先级顺序调用处理器并收集返回值
            for (PriorityHandler<Map<String, Object>> priorityHandler : handlerArray) {
                try {
                    Object result = priorityHandler.handler.handle(data);
                    if (result != null) {
                        results.add(result);
                    }
                } catch (Exception e) {
                    // 处理器异常不应该影响其他处理器
                    e.printStackTrace();
                }
            }
            
            return results;
        }
        
        public int numHandlers(String topic) {
            List<PriorityHandler<Map<String, Object>>> handlers = handlersByTopic.get(topic);
            return handlers != null ? handlers.size() : 0;
        }
        
        public int totalHandlers() {
            return handlersByTopic.values().stream()
                .mapToInt(List::size)
                .sum();
        }
    }
    
    // ========== Result基类 ==========
    
    public static class Result {
        // 基类本身不包含数据，具体的Result类需要定义自己的字段
    }
    
    // ========== Event基类 ==========
    
    public abstract static class Event<E extends Event<E, R>, R> {

        // 注意：TypedEvent不使用with()方法，应该直接在of()方法中设置字段
        // MapEvent才使用Map，通过EventBus.onMap()/dispatchMap()/collectMap()处理


        public String getTopic() {
            return this.getClass().getName();
        }

        public void dispatch() {
            String topic = getTopic();
            EventBus.dispatchTyped(this, topic);
        }

        @SuppressWarnings("unchecked")
        public ArrayList<R> collect() {
            String topic = getTopic();
            ArrayList<Object> results = EventBus.collectTyped(this, topic);
            // 类型转换：从ArrayList<Object>转换为ArrayList<R>
            // 由于泛型擦除，运行时无法检查类型，直接转换
            return (ArrayList<R>) (ArrayList<?>) results;
        }

        // 注意：TypedEvent不使用Map，因此不需要getData/hasData方法
        // 子类应该直接访问自己的字段
    }

    // ========== 使用示例 ==========

    /**
     * EventBus 使用示例。
     */
    public static class ExampleEvent extends Event<ExampleEvent, ExampleEvent.Result> {

        public String field1;
        public int field2;
        public Object field3;

        public static ExampleEvent of(String field1, int field2) {
            ExampleEvent event = new ExampleEvent();
            event.field1 = field1;
            event.field2 = field2;
            return event;
        }

        public static ExampleEvent of(String field1, int field2, Object field3) {
            ExampleEvent event = of(field1, field2);
            event.field3 = field3;
            return event;
        }

        public Result ok(String resultValue1, int resultValue2) {
            return Result.ok(resultValue1, resultValue2);
        }

        public Result nope() {
            return Result.nope();
        }

        public static class Result {
            public final String resultValue1;
            public final int resultValue2;
            public final boolean success;

            public static Result of(String resultValue1, int resultValue2, boolean success) {
                return new Result(resultValue1, resultValue2, success);
            }

            public static Result ok(String resultValue1, int resultValue2) {
                return new Result(resultValue1, resultValue2, true);
            }

            public static Result nope() {
                return null;
            }

            public static Result failure(String reason) {
                return new Result(reason, 0, false);
            }

            private Result(String resultValue1, int resultValue2, boolean success) {
                this.resultValue1 = resultValue1;
                this.resultValue2 = resultValue2;
                this.success = success;
            }

            @Override
            public String toString() {
                if (!success) {
                    return String.format("失败: %s", resultValue1);
                }
                return String.format("成功: %s (%d)", resultValue1, resultValue2);
            }
        }
    }
}
