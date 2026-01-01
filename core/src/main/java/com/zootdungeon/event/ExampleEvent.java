package com.zootdungeon.event;

import com.zootdungeon.utils.EventBus;
import com.zootdungeon.utils.EventBus.Handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * EventBus使用示例
 */
public class ExampleEvent extends EventBus.Event<ExampleEvent, ExampleEvent.Result> {
    
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
        ExampleEvent event = new ExampleEvent();
        event.field1 = field1;
        event.field2 = field2;
        event.field3 = field3;
        return event;
    }
    
    public static Result ok(String resultValue1, int resultValue2) {
        return Result.ok(resultValue1, resultValue2);
    }
    
    public static Result nope() {
        return Result.nope();
    }
    
    public static void on(Handler<ExampleEvent, Result> handler, int priority) {
        EventBus.on(ExampleEvent.class, handler, priority);
    }
    
    public static void on(Handler<ExampleEvent, Result> handler) {
        EventBus.on(ExampleEvent.class, handler);
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
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("EventBus Usage Example");
        System.out.println("========================================\n");
        
        System.out.println(">>> Typed EventBus Example");
        System.out.println("----------------------------------------");
        
        // Register handler 1: High priority (executes first)
        ExampleEvent.on((ExampleEvent e) -> {
            System.out.println("  [Handler 1 - High Priority] Processing event: " + e.field1 + ", value: " + e.field2);
            return e.ok("Handler 1 completed", e.field2 * 2);
        }, 100);
        
        // Register handler 2: Medium priority (may return nope() to skip)
        ExampleEvent.on((ExampleEvent e) -> {
            if (e.field2 > 5) {
                System.out.println("  [Handler 2 - Medium Priority] Value > 5, processing");
                return e.ok("Handler 2 completed (value > 5)", e.field2 + 10);
            }
            System.out.println("  [Handler 2 - Medium Priority] Value <= 5, skipping");
            return e.nope(); // Skip this handler
        }, 50);
        
        // Register handler 3: Low priority (executes last)
        ExampleEvent.on((ExampleEvent e) -> {
            System.out.println("  [Handler 3 - Low Priority] Final processing");
            return e.ok("Handler 3 completed", 0);
        }, 10);
        
        // Create event
        ExampleEvent event1 = ExampleEvent.of("Test Event 1", 8);
        event1.field3 = "Extra data"; // 直接设置字段（TypedEvent使用结构体字段）
        
        System.out.println("\n--- Create event and collect results ---");
        ArrayList<ExampleEvent.Result> results1 = event1.collect();
        
        System.out.println("\n--- Collected results ---");
        System.out.println("Result count: " + results1.size());
        for (ExampleEvent.Result result : results1) {
            System.out.println("  " + result);
        }
        
        // Event instance can be reused
        System.out.println("\n--- Reuse the same event instance ---");
        ArrayList<ExampleEvent.Result> results1_2 = event1.collect();
        System.out.println("Result count: " + results1_2.size());
        
        System.out.println("\n--- Create new event to test conditional handling ---");
        ExampleEvent event2 = ExampleEvent.of("Test Event 2", 3); // Value <= 5, handler 2 will skip
        ArrayList<ExampleEvent.Result> results2 = event2.collect();
        System.out.println("Result count: " + results2.size());
        for (ExampleEvent.Result result : results2) {
            System.out.println("  " + result);
        }
        
        System.out.println("\n");
        
        System.out.println("\n>>> Event instance methods (uses fixed topic)");
        System.out.println("----------------------------------------");
        
        ExampleEvent event4 = ExampleEvent.of("Instance method event", 7);
        
        // Use dispatch() - automatically uses fixed topic from getTopic()
        System.out.println("\n--- Dispatch using event.dispatch() (uses fixed topic) ---");
        event4.dispatch();
        
        // Use collect() - automatically uses fixed topic from getTopic()
        System.out.println("\n--- Collect using event.collect() (uses fixed topic) ---");
        ArrayList<ExampleEvent.Result> results4 = event4.collect();
        System.out.println("Result count: " + results4.size());
        for (ExampleEvent.Result result : results4) {
            System.out.println("  " + result);
        }
        
        System.out.println("\n========================================");
        System.out.println("Example completed");
        System.out.println("========================================");
    }
}

