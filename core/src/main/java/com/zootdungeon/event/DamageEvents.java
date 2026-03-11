package com.zootdungeon.event;

import com.zootdungeon.mechanics.Damage;
import com.zootdungeon.utils.EventBus;
import com.zootdungeon.utils.EventBus.Handler;

import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;

public final class DamageEvents {

    private DamageEvents() {}

    public static class DamageComputeRequested extends EventBus.Event<DamageComputeRequested, UnaryOperator<Float>> {
        public Damage.DamageContext context;
        public static DamageComputeRequested of(Damage.DamageContext context) {
            DamageComputeRequested e = new DamageComputeRequested();
            e.context = context;
            return e;
        }
        public static void on(Handler<DamageComputeRequested, UnaryOperator<Float>> handler, int priority) {
            EventBus.on(DamageComputeRequested.class, handler, priority);
        }
        public static void on(Handler<DamageComputeRequested, UnaryOperator<Float>> handler) {
            EventBus.on(DamageComputeRequested.class, handler);
        }
    }

    public static class DamageMitigationRequested extends EventBus.Event<DamageMitigationRequested, UnaryOperator<Float>> {
        public Damage.DamageContext context;
        public static DamageMitigationRequested of(Damage.DamageContext context) {
            DamageMitigationRequested e = new DamageMitigationRequested();
            e.context = context;
            return e;
        }
        public static void on(Handler<DamageMitigationRequested, UnaryOperator<Float>> handler, int priority) {
            EventBus.on(DamageMitigationRequested.class, handler, priority);
        }
        public static void on(Handler<DamageMitigationRequested, UnaryOperator<Float>> handler) {
            EventBus.on(DamageMitigationRequested.class, handler);
        }
    }

    public static class DamageApplyRequested extends EventBus.Event<DamageApplyRequested, BiConsumer<Damage.DamageContext, Damage.DamageResult>> {
        public Damage.DamageContext context;
        public static DamageApplyRequested of(Damage.DamageContext context) {
            DamageApplyRequested e = new DamageApplyRequested();
            e.context = context;
            return e;
        }
        public static void on(Handler<DamageApplyRequested, BiConsumer<Damage.DamageContext, Damage.DamageResult>> handler, int priority) {
            EventBus.on(DamageApplyRequested.class, handler, priority);
        }
        public static void on(Handler<DamageApplyRequested, BiConsumer<Damage.DamageContext, Damage.DamageResult>> handler) {
            EventBus.on(DamageApplyRequested.class, handler);
        }
    }

    public static class DamageFeedbackRequested extends EventBus.Event<DamageFeedbackRequested, Object> {
        public Damage.DamageContext context;
        public Damage.DamageResult result;
        public static DamageFeedbackRequested of(Damage.DamageContext context, Damage.DamageResult result) {
            DamageFeedbackRequested e = new DamageFeedbackRequested();
            e.context = context;
            e.result = result;
            return e;
        }
        public static void on(Handler<DamageFeedbackRequested, Object> handler, int priority) {
            EventBus.on(DamageFeedbackRequested.class, handler, priority);
        }
        public static void on(Handler<DamageFeedbackRequested, Object> handler) {
            EventBus.on(DamageFeedbackRequested.class, handler);
        }
    }

    public static class DamageApplied extends EventBus.Event<DamageApplied, Object> {
        public Damage.DamageContext context;
        public Damage.DamageResult result;
        public static DamageApplied of(Damage.DamageContext context, Damage.DamageResult result) {
            DamageApplied e = new DamageApplied();
            e.context = context;
            e.result = result;
            return e;
        }
        public static void on(Handler<DamageApplied, Object> handler, int priority) {
            EventBus.on(DamageApplied.class, handler, priority);
        }
        public static void on(Handler<DamageApplied, Object> handler) {
            EventBus.on(DamageApplied.class, handler);
        }
    }
}

