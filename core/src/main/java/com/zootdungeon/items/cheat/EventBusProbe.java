package com.zootdungeon.items.cheat;

import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.Item;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.SpriteRegistry;
import com.zootdungeon.utils.EventBus;
import com.zootdungeon.windows.WndGeneral;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class EventBusProbe extends Item {

    private static final String AC_OPEN = "OPEN";

    private static final String BUNDLE_PROBE_ID = "probe_id";
    private static final String BUNDLE_TYPED_SEQ = "typed_seq";
    private static final String BUNDLE_MAP_SEQ = "map_seq";
    private static final String BUNDLE_TYPED_DISPATCH = "typed_dispatch";
    private static final String BUNDLE_TYPED_COLLECT = "typed_collect";
    private static final String BUNDLE_MAP_DISPATCH = "map_dispatch";
    private static final String BUNDLE_MAP_COLLECT = "map_collect";
    private static final String BUNDLE_TYPED_DISPATCH_ON = "typed_dispatch_on";
    private static final String BUNDLE_TYPED_COLLECT_A_ON = "typed_collect_a_on";
    private static final String BUNDLE_TYPED_COLLECT_B_ON = "typed_collect_b_on";
    private static final String BUNDLE_MAP_DISPATCH_ON = "map_dispatch_on";
    private static final String BUNDLE_MAP_COLLECT_A_ON = "map_collect_a_on";
    private static final String BUNDLE_MAP_COLLECT_B_ON = "map_collect_b_on";
    private static final String BUNDLE_LOG_SIZE = "log_size";
    private static final String BUNDLE_LOG_PREFIX = "log_";

    private static int nextProbeSerial = 1;

    static {
        SpriteRegistry.texture("sheet.cola.event_bus_probe", "cola/debug_bag.png")
                .setXY("event_bus_probe", 0, 0, 32, 32);
    }

    {
        image = SpriteRegistry.byLabel("event_bus_probe");
        defaultAction = AC_OPEN;
        unique = true;
    }

    private String probeId;
    private int typedSequence;
    private int mapSequence;

    private boolean typedDispatchEnabled;
    private boolean typedCollectAEnabled;
    private boolean typedCollectBEnabled;
    private boolean mapDispatchEnabled;
    private boolean mapCollectAEnabled;
    private boolean mapCollectBEnabled;

    private String lastTypedDispatch;
    private String lastTypedCollect;
    private String lastMapDispatch;
    private String lastMapCollect;

    private final ArrayList<String> logs = new ArrayList<>();

    private transient EventBus.Handler<TypedProbeEvent, Object> typedDispatchHandler;
    private transient EventBus.Handler<TypedProbeEvent, String> typedCollectHandlerA;
    private transient EventBus.Handler<TypedProbeEvent, String> typedCollectHandlerB;
    private transient EventBus.Handler<Map<String, Object>, Object> mapDispatchHandler;
    private transient EventBus.Handler<Map<String, Object>, Object> mapCollectHandlerA;
    private transient EventBus.Handler<Map<String, Object>, Object> mapCollectHandlerB;

    public EventBusProbe() {
        ensureProbeId();
        resetSummaries();
        initHandlers();
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        actions.add(AC_OPEN);
        return actions;
    }

    @Override
    public String actionName(String action, Hero hero) {
        if (AC_OPEN.equals(action)) {
            return Messages.get(EventBusProbe.class, "ac_open");
        }
        return super.actionName(action, hero);
    }

    @Override
    public void execute(Hero hero, String action) {
        if (AC_OPEN.equals(action)) {
            showWindow();
            return;
        }
        super.execute(hero, action);
    }

    @Override
    public String name() {
        return Messages.get(EventBusProbe.class, "name");
    }

    @Override
    public String desc() {
        return Messages.get(EventBusProbe.class, "desc");
    }

    @Override
    public boolean isUpgradable() {
        return false;
    }

    @Override
    public boolean isIdentified() {
        return true;
    }

    @Override
    public int value() {
        return 1;
    }

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        ensureProbeId();
        bundle.put(BUNDLE_PROBE_ID, probeId);
        bundle.put(BUNDLE_TYPED_SEQ, typedSequence);
        bundle.put(BUNDLE_MAP_SEQ, mapSequence);
        bundle.put(BUNDLE_TYPED_DISPATCH, safe(lastTypedDispatch));
        bundle.put(BUNDLE_TYPED_COLLECT, safe(lastTypedCollect));
        bundle.put(BUNDLE_MAP_DISPATCH, safe(lastMapDispatch));
        bundle.put(BUNDLE_MAP_COLLECT, safe(lastMapCollect));
        bundle.put(BUNDLE_TYPED_DISPATCH_ON, typedDispatchEnabled);
        bundle.put(BUNDLE_TYPED_COLLECT_A_ON, typedCollectAEnabled);
        bundle.put(BUNDLE_TYPED_COLLECT_B_ON, typedCollectBEnabled);
        bundle.put(BUNDLE_MAP_DISPATCH_ON, mapDispatchEnabled);
        bundle.put(BUNDLE_MAP_COLLECT_A_ON, mapCollectAEnabled);
        bundle.put(BUNDLE_MAP_COLLECT_B_ON, mapCollectBEnabled);
        bundle.put(BUNDLE_LOG_SIZE, logs.size());
        for (int i = 0; i < logs.size(); i++) {
            bundle.put(BUNDLE_LOG_PREFIX + i, logs.get(i));
        }
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        probeId = bundle.getString(BUNDLE_PROBE_ID);
        ensureProbeId();
        typedSequence = bundle.getInt(BUNDLE_TYPED_SEQ);
        mapSequence = bundle.getInt(BUNDLE_MAP_SEQ);
        lastTypedDispatch = restoreOrDefault(bundle.getString(BUNDLE_TYPED_DISPATCH));
        lastTypedCollect = restoreOrDefault(bundle.getString(BUNDLE_TYPED_COLLECT));
        lastMapDispatch = restoreOrDefault(bundle.getString(BUNDLE_MAP_DISPATCH));
        lastMapCollect = restoreOrDefault(bundle.getString(BUNDLE_MAP_COLLECT));
        typedDispatchEnabled = bundle.getBoolean(BUNDLE_TYPED_DISPATCH_ON);
        typedCollectAEnabled = bundle.getBoolean(BUNDLE_TYPED_COLLECT_A_ON);
        typedCollectBEnabled = bundle.getBoolean(BUNDLE_TYPED_COLLECT_B_ON);
        mapDispatchEnabled = bundle.getBoolean(BUNDLE_MAP_DISPATCH_ON);
        mapCollectAEnabled = bundle.getBoolean(BUNDLE_MAP_COLLECT_A_ON);
        mapCollectBEnabled = bundle.getBoolean(BUNDLE_MAP_COLLECT_B_ON);
        logs.clear();
        int logSize = bundle.getInt(BUNDLE_LOG_SIZE);
        for (int i = 0; i < logSize; i++) {
            String line = bundle.getString(BUNDLE_LOG_PREFIX + i);
            if (line != null && !line.isEmpty()) {
                logs.add(line);
            }
        }
        initHandlers();
        syncHandlers();
    }

    private void showWindow() {
        WndGeneral.Builder builder = WndGeneral.make()
                .title(Messages.get(EventBusProbe.class, "title"));
        builder.tab(Messages.get(EventBusProbe.class, "typed_tab"), this::fillTypedTab);
        builder.tab(Messages.get(EventBusProbe.class, "map_tab"), this::fillMapTab);
        builder.tab(Messages.get(EventBusProbe.class, "log_tab"), this::fillLogTab);
        builder.show();
    }

    private void fillTypedTab(WndGeneral.PaneBuilder p) {
        p.line(Messages.get(EventBusProbe.class, "typed_intro"));
        p.row(Messages.get(EventBusProbe.class, "label_topic"), typedTopic());
        p.row(Messages.get(EventBusProbe.class, "label_dispatch_listener"), state(typedDispatchEnabled));
        p.row(Messages.get(EventBusProbe.class, "label_collect_a"), state(typedCollectAEnabled));
        p.row(Messages.get(EventBusProbe.class, "label_collect_b"), state(typedCollectBEnabled));
        p.row(Messages.get(EventBusProbe.class, "label_last_dispatch"), safe(lastTypedDispatch));
        p.row(Messages.get(EventBusProbe.class, "label_last_collect"), safe(lastTypedCollect));
        p.option(toggleLabel("toggle_dispatch_listener", typedDispatchEnabled), () -> {
            setTypedDispatchEnabled(!typedDispatchEnabled);
            reopen();
        });
        p.option(toggleLabel("toggle_collect_a", typedCollectAEnabled), () -> {
            setTypedCollectAEnabled(!typedCollectAEnabled);
            reopen();
        });
        p.option(toggleLabel("toggle_collect_b", typedCollectBEnabled), () -> {
            setTypedCollectBEnabled(!typedCollectBEnabled);
            reopen();
        });
        p.option(Messages.get(EventBusProbe.class, "action_dispatch_once"), () -> {
            runTypedDispatch();
            reopen();
        });
        p.option(Messages.get(EventBusProbe.class, "action_collect_once"), () -> {
            runTypedCollect();
            reopen();
        });
        p.option(Messages.get(EventBusProbe.class, "action_reset_typed"), () -> {
            resetTyped();
            reopen();
        });
    }

    private void fillMapTab(WndGeneral.PaneBuilder p) {
        p.line(Messages.get(EventBusProbe.class, "map_intro"));
        p.row(Messages.get(EventBusProbe.class, "label_topic"), mapTopic());
        p.row(Messages.get(EventBusProbe.class, "label_dispatch_listener"), state(mapDispatchEnabled));
        p.row(Messages.get(EventBusProbe.class, "label_collect_a"), state(mapCollectAEnabled));
        p.row(Messages.get(EventBusProbe.class, "label_collect_b"), state(mapCollectBEnabled));
        p.row(Messages.get(EventBusProbe.class, "label_last_dispatch"), safe(lastMapDispatch));
        p.row(Messages.get(EventBusProbe.class, "label_last_collect"), safe(lastMapCollect));
        p.option(toggleLabel("toggle_dispatch_listener", mapDispatchEnabled), () -> {
            setMapDispatchEnabled(!mapDispatchEnabled);
            reopen();
        });
        p.option(toggleLabel("toggle_collect_a", mapCollectAEnabled), () -> {
            setMapCollectAEnabled(!mapCollectAEnabled);
            reopen();
        });
        p.option(toggleLabel("toggle_collect_b", mapCollectBEnabled), () -> {
            setMapCollectBEnabled(!mapCollectBEnabled);
            reopen();
        });
        p.option(Messages.get(EventBusProbe.class, "action_dispatch_once"), () -> {
            runMapDispatch();
            reopen();
        });
        p.option(Messages.get(EventBusProbe.class, "action_collect_once"), () -> {
            runMapCollect();
            reopen();
        });
        p.option(Messages.get(EventBusProbe.class, "action_reset_map"), () -> {
            resetMap();
            reopen();
        });
    }

    private void fillLogTab(WndGeneral.PaneBuilder p) {
        p.line(Messages.get(EventBusProbe.class, "log_intro"));
        if (logs.isEmpty()) {
            p.line(Messages.get(EventBusProbe.class, "log_empty"));
        } else {
            for (String line : logs) {
                p.line(line);
            }
        }
        p.option(Messages.get(EventBusProbe.class, "action_clear_log"), () -> {
            logs.clear();
            appendLog(Messages.get(EventBusProbe.class, "log_cleared"));
            reopen();
        });
        p.option(Messages.get(EventBusProbe.class, "action_reset_all"), () -> {
            resetTyped();
            resetMap();
            reopen();
        });
    }

    private void reopen() {
        showWindow();
    }

    private void runTypedDispatch() {
        TypedProbeEvent event = typedEvent("dispatch");
        event.dispatch();
        lastTypedDispatch = "seq=" + event.sequence + ", payload=" + event.payload;
        appendLog("typed dispatch -> " + lastTypedDispatch);
    }

    private void runTypedCollect() {
        TypedProbeEvent event = typedEvent("collect");
        ArrayList<String> results = event.collect();
        lastTypedCollect = formatResults(results);
        appendLog("typed collect -> " + lastTypedCollect);
    }

    private void runMapDispatch() {
        Map<String, Object> data = newMapData("dispatch");
        EventBus.dispatchMap(mapTopic(), data);
        lastMapDispatch = "seq=" + data.get("seq") + ", payload=" + data.get("payload");
        appendLog("map dispatch -> " + lastMapDispatch);
    }

    private void runMapCollect() {
        Map<String, Object> data = newMapData("collect");
        ArrayList<Object> results = EventBus.collectMap(mapTopic(), data);
        lastMapCollect = formatResults(results);
        appendLog("map collect -> " + lastMapCollect);
    }

    private TypedProbeEvent typedEvent(String payload) {
        ensureProbeId();
        TypedProbeEvent event = new TypedProbeEvent();
        event.topic = typedTopic();
        event.probeId = probeId;
        event.sequence = ++typedSequence;
        event.payload = payload;
        return event;
    }

    private Map<String, Object> newMapData(String payload) {
        ensureProbeId();
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("probeId", probeId);
        data.put("seq", ++mapSequence);
        data.put("payload", payload);
        return data;
    }

    private void setTypedDispatchEnabled(boolean enabled) {
        typedDispatchEnabled = enabled;
        EventBus.TypedEventBus<TypedProbeEvent> bus = EventBus.typed(TypedProbeEvent.class);
        if (enabled) {
            bus.addHandler(typedDispatchHandler, 100, typedTopic());
            appendLog("typed dispatch listener -> " + state(enabled));
        } else {
            bus.removeHandler(typedDispatchHandler);
            appendLog("typed dispatch listener -> " + state(enabled));
        }
    }

    private void setTypedCollectAEnabled(boolean enabled) {
        typedCollectAEnabled = enabled;
        EventBus.TypedEventBus<TypedProbeEvent> bus = EventBus.typed(TypedProbeEvent.class);
        if (enabled) {
            bus.addHandler(typedCollectHandlerA, 60, typedTopic());
            appendLog("typed collect listener A -> " + state(enabled));
        } else {
            bus.removeHandler(typedCollectHandlerA);
            appendLog("typed collect listener A -> " + state(enabled));
        }
    }

    private void setTypedCollectBEnabled(boolean enabled) {
        typedCollectBEnabled = enabled;
        EventBus.TypedEventBus<TypedProbeEvent> bus = EventBus.typed(TypedProbeEvent.class);
        if (enabled) {
            bus.addHandler(typedCollectHandlerB, 20, typedTopic());
            appendLog("typed collect listener B -> " + state(enabled));
        } else {
            bus.removeHandler(typedCollectHandlerB);
            appendLog("typed collect listener B -> " + state(enabled));
        }
    }

    private void setMapDispatchEnabled(boolean enabled) {
        mapDispatchEnabled = enabled;
        if (enabled) {
            EventBus.on(mapTopic(), mapDispatchHandler, 100);
            appendLog("map dispatch listener -> " + state(enabled));
        } else {
            EventBus.offMap(mapTopic(), mapDispatchHandler);
            appendLog("map dispatch listener -> " + state(enabled));
        }
    }

    private void setMapCollectAEnabled(boolean enabled) {
        mapCollectAEnabled = enabled;
        if (enabled) {
            EventBus.on(mapTopic(), mapCollectHandlerA, 60);
            appendLog("map collect listener A -> " + state(enabled));
        } else {
            EventBus.offMap(mapTopic(), mapCollectHandlerA);
            appendLog("map collect listener A -> " + state(enabled));
        }
    }

    private void setMapCollectBEnabled(boolean enabled) {
        mapCollectBEnabled = enabled;
        if (enabled) {
            EventBus.on(mapTopic(), mapCollectHandlerB, 20);
            appendLog("map collect listener B -> " + state(enabled));
        } else {
            EventBus.offMap(mapTopic(), mapCollectHandlerB);
            appendLog("map collect listener B -> " + state(enabled));
        }
    }

    private void resetTyped() {
        if (typedDispatchEnabled) EventBus.typed(TypedProbeEvent.class).removeHandler(typedDispatchHandler);
        if (typedCollectAEnabled) EventBus.typed(TypedProbeEvent.class).removeHandler(typedCollectHandlerA);
        if (typedCollectBEnabled) EventBus.typed(TypedProbeEvent.class).removeHandler(typedCollectHandlerB);
        typedDispatchEnabled = false;
        typedCollectAEnabled = false;
        typedCollectBEnabled = false;
        lastTypedDispatch = Messages.get(EventBusProbe.class, "last_none");
        lastTypedCollect = Messages.get(EventBusProbe.class, "last_none");
        appendLog("typed listeners -> reset");
    }

    private void resetMap() {
        if (mapDispatchEnabled) EventBus.offMap(mapTopic(), mapDispatchHandler);
        if (mapCollectAEnabled) EventBus.offMap(mapTopic(), mapCollectHandlerA);
        if (mapCollectBEnabled) EventBus.offMap(mapTopic(), mapCollectHandlerB);
        mapDispatchEnabled = false;
        mapCollectAEnabled = false;
        mapCollectBEnabled = false;
        lastMapDispatch = Messages.get(EventBusProbe.class, "last_none");
        lastMapCollect = Messages.get(EventBusProbe.class, "last_none");
        appendLog("map listeners -> reset");
    }

    private void resetSummaries() {
        lastTypedDispatch = Messages.get(EventBusProbe.class, "last_none");
        lastTypedCollect = Messages.get(EventBusProbe.class, "last_none");
        lastMapDispatch = Messages.get(EventBusProbe.class, "last_none");
        lastMapCollect = Messages.get(EventBusProbe.class, "last_none");
    }

    private void initHandlers() {
        if (typedDispatchHandler == null) typedDispatchHandler = this::onTypedDispatch;
        if (typedCollectHandlerA == null) typedCollectHandlerA = this::onTypedCollectA;
        if (typedCollectHandlerB == null) typedCollectHandlerB = this::onTypedCollectB;
        if (mapDispatchHandler == null) mapDispatchHandler = this::onMapDispatch;
        if (mapCollectHandlerA == null) mapCollectHandlerA = this::onMapCollectA;
        if (mapCollectHandlerB == null) mapCollectHandlerB = this::onMapCollectB;
    }

    private void syncHandlers() {
        EventBus.TypedEventBus<TypedProbeEvent> typedBus = EventBus.typed(TypedProbeEvent.class);
        typedBus.removeHandler(typedDispatchHandler);
        typedBus.removeHandler(typedCollectHandlerA);
        typedBus.removeHandler(typedCollectHandlerB);
        EventBus.offMap(mapTopic(), mapDispatchHandler);
        EventBus.offMap(mapTopic(), mapCollectHandlerA);
        EventBus.offMap(mapTopic(), mapCollectHandlerB);

        if (typedDispatchEnabled) typedBus.addHandler(typedDispatchHandler, 100, typedTopic());
        if (typedCollectAEnabled) typedBus.addHandler(typedCollectHandlerA, 60, typedTopic());
        if (typedCollectBEnabled) typedBus.addHandler(typedCollectHandlerB, 20, typedTopic());
        if (mapDispatchEnabled) EventBus.on(mapTopic(), mapDispatchHandler, 100);
        if (mapCollectAEnabled) EventBus.on(mapTopic(), mapCollectHandlerA, 60);
        if (mapCollectBEnabled) EventBus.on(mapTopic(), mapCollectHandlerB, 20);
    }

    private Object onTypedDispatch(TypedProbeEvent event) {
        appendLog("typed listener saw seq=" + event.sequence + " payload=" + event.payload);
        return null;
    }

    private String onTypedCollectA(TypedProbeEvent event) {
        String result = "A(seq=" + event.sequence + ", payload=" + event.payload + ")";
        appendLog("typed collect A -> " + result);
        return result;
    }

    private String onTypedCollectB(TypedProbeEvent event) {
        String result = "B(seq=" + event.sequence + ", payload=" + event.payload + ")";
        appendLog("typed collect B -> " + result);
        return result;
    }

    private Object onMapDispatch(Map<String, Object> data) {
        appendLog("map listener saw seq=" + data.get("seq") + " payload=" + data.get("payload"));
        return null;
    }

    private Object onMapCollectA(Map<String, Object> data) {
        String result = "A(seq=" + data.get("seq") + ", payload=" + data.get("payload") + ")";
        appendLog("map collect A -> " + result);
        return result;
    }

    private Object onMapCollectB(Map<String, Object> data) {
        String result = "B(seq=" + data.get("seq") + ", payload=" + data.get("payload") + ")";
        appendLog("map collect B -> " + result);
        return result;
    }

    private void appendLog(String line) {
        if (line == null || line.isEmpty()) return;
        logs.add(0, line);
        while (logs.size() > 12) {
            logs.remove(logs.size() - 1);
        }
    }

    private String typedTopic() {
        ensureProbeId();
        return "eventbus.probe.typed." + probeId;
    }

    private String mapTopic() {
        ensureProbeId();
        return "eventbus.probe.map." + probeId;
    }

    private void ensureProbeId() {
        if (probeId == null || probeId.isEmpty()) {
            probeId = "probe-" + nextProbeSerial++;
        }
    }

    private String state(boolean enabled) {
        return Messages.get(EventBusProbe.class, enabled ? "state_on" : "state_off");
    }

    private String toggleLabel(String key, boolean enabled) {
        return Messages.get(EventBusProbe.class, key, state(!enabled));
    }

    private String formatResults(ArrayList<?> results) {
        if (results == null || results.isEmpty()) {
            return "[]";
        }
        return results.toString();
    }

    private String safe(String value) {
        return value != null ? value : Messages.get(EventBusProbe.class, "last_none");
    }

    private String restoreOrDefault(String value) {
        return (value == null || value.isEmpty()) ? Messages.get(EventBusProbe.class, "last_none") : value;
    }

    public static class TypedProbeEvent extends EventBus.Event<TypedProbeEvent, String> {
        public String topic;
        public String probeId;
        public int sequence;
        public String payload;

        @Override
        public String getTopic() {
            return topic != null ? topic : super.getTopic();
        }
    }
}
