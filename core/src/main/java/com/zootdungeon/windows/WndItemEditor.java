package com.zootdungeon.windows;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.zootdungeon.items.Item;
import com.zootdungeon.items.armor.Armor;
import com.zootdungeon.items.rings.Ring;
import com.zootdungeon.items.wands.Wand;
import com.zootdungeon.items.weapon.Weapon;
import com.zootdungeon.items.weapon.ambushWeapon.AmbushWeapon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.scenes.PixelScene;
import com.zootdungeon.sprites.ItemSprite;
import com.zootdungeon.ui.RedButton;
import com.zootdungeon.ui.RenderedTextBlock;
import com.zootdungeon.ui.Window;
import com.zootdungeon.utils.GLog;
import com.watabou.noosa.BitmapText;
import com.watabou.utils.Reflection;

public class WndItemEditor extends Window {

    private static final int WIDTH = 180;
    private static final int BTN_HEIGHT = 20;
    private static final int GAP = 2;
    private static final int MARGIN = 2;

    private Item item;
    private int pos;
    private Map<String, Field> editableFields = new HashMap<>();
    private Map<String, Object> configValues = new HashMap<>();

    public WndItemEditor(Item item) {
        super();

        this.item = item;

        IconTitle title = new IconTitle(new ItemSprite(item), Messages.titleCase(item.name()));
        title.setRect(0, 0, WIDTH, 0);
        add(title);
        pos = (int) title.bottom() + GAP;

        // 尝试获取 getConfig 方法
        try {
            Method getConfig = item.getClass().getMethod("getConfig");
            Object config = getConfig.invoke(item);
            if (config instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> configMap = (Map<String, Object>) config;
                configValues.putAll(configMap);
            }
        } catch (Exception e) {
            // getConfig 方法不存在或调用失败，使用默认字段
        }

        // 添加基本信息显示
        addInfo("物品类型", item.getClass().getSimpleName());
        addInfo("等级", String.valueOf(item.level()));
        addInfo("数量", String.valueOf(item.quantity()));
        addInfo("已鉴定", item.isIdentified() ? "是" : "否");
        addInfo("诅咒", item.cursed ? "是" : "否");

        // 添加可编辑字段
        addEditableField("等级", "level", item.level(), Integer.class);
        addEditableField("数量", "quantity", item.quantity(), Integer.class);
        addEditableField("诅咒", "cursed", item.cursed, Boolean.class);

        // 根据物品类型添加特定字段
        if (item instanceof Weapon) {
            Weapon weapon = (Weapon) item;
            addEditableFieldSafe("Tier", "tier", Integer.class);
            addEditableField("附魔硬化", "enchantHardened", weapon.enchantHardened, Boolean.class);
            addEditableField("诅咒灌注加成", "curseInfusionBonus", weapon.curseInfusionBonus, Boolean.class);
            addEditableField("精通药剂加成", "masteryPotionBonus", weapon.masteryPotionBonus, Boolean.class);
        }

        if (item instanceof Armor) {
            // 使用反射来安全访问可能不存在的字段
            addEditableFieldSafe("附魔硬化", "enchantHardened", Boolean.class);
            addEditableFieldSafe("诅咒灌注加成", "curseInfusionBonus", Boolean.class);
            addEditableFieldSafe("精通药剂加成", "masteryPotionBonus", Boolean.class);
        }
        if (item instanceof AmbushWeapon) {
            AmbushWeapon aw = (AmbushWeapon) item;
            addEditableField("突袭倍率加成", "ambushRate", aw.ambushRate, Float.class);
        }

        if (item instanceof Wand) {
            Wand wand = (Wand) item;
            addEditableField("充能", "curCharges", wand.curCharges, Integer.class);
        }

        if (item instanceof Ring) {
            // 使用反射来安全访问可能不存在的字段
            addEditableFieldSafe("等级加成", "levelBonus", Integer.class);
        }

        // 添加从 getConfig 获取的字段
        for (Map.Entry<String, Object> entry : configValues.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Integer) {
                addConfigField(key, (Integer) value);
            } else if (value instanceof Boolean) {
                addConfigField(key, (Boolean) value);
            } else if (value instanceof Float) {
                addConfigField(key, (Float) value);
            } else if (value instanceof String) {
                addConfigField(key, (String) value);
            }
        }

        resize(WIDTH, pos);
    }

    private void addInfo(String label, String value) {
        RenderedTextBlock info = PixelScene.renderTextBlock(6);
        info.text(label + ": " + value, WIDTH - MARGIN * 2);
        info.setPos(MARGIN, pos);
        add(info);
        pos = (int) info.bottom() + GAP;
    }

    private void addEditableField(String label, String fieldName, Object currentValue, Class<?> type) {
        try {
            Field field = findField(item.getClass(), fieldName);
            if (field != null) {
                editableFields.put(fieldName, field);
                addFieldButton(label, fieldName, currentValue, type);
            }
        } catch (Exception e) {
            // 字段不存在，跳过
        }
    }

    private void addEditableFieldSafe(String label, String fieldName, Class<?> type) {
        try {
            Field field = findField(item.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                Object currentValue = field.get(item);
                editableFields.put(fieldName, field);
                addFieldButton(label, fieldName, currentValue, type);
            }
        } catch (Exception e) {
            // 字段不存在，跳过
        }
    }

    private void addConfigField(String key, Integer value) {
        addFieldButton(key, "config." + key, value, Integer.class);
    }

    private void addConfigField(String key, Boolean value) {
        addFieldButton(key, "config." + key, value, Boolean.class);
    }

    private void addConfigField(String key, Float value) {
        addFieldButton(key, "config." + key, value, Float.class);
    }

    private void addConfigField(String key, String value) {
        addFieldButton(key, "config." + key, value, String.class);
    }

    private void addFieldButton(String label, String fieldName, Object currentValue, Class<?> type) {
        String buttonText = label + ": " + currentValue;
        RedButton btn = new RedButton(buttonText) {
            @Override
            protected void onClick() {
                editField(fieldName, currentValue, type);
            }
        };
        btn.setRect(MARGIN, pos, WIDTH - MARGIN * 2, BTN_HEIGHT);
        add(btn);
        pos += BTN_HEIGHT + GAP;
    }

    private void editField(String fieldName, Object currentValue, Class<?> type) {
        if (fieldName.startsWith("config.")) {
            // 处理配置字段
            String configKey = fieldName.substring(7);
            editConfigField(configKey, currentValue, type);
        } else {
            // 处理普通字段
            Field field = editableFields.get(fieldName);
            if (field != null) {
                editRegularField(field, currentValue, type);
            }
        }
    }

    private void editRegularField(Field field, Object currentValue, Class<?> type) {
        if (type == Integer.class) {
            GameScene.show(new WndTextInput(
                    "编辑 " + field.getName(),
                    "当前值: " + currentValue,
                    String.valueOf(currentValue),
                    10,
                    false,
                    "确定",
                    "取消"
            ) {
                @Override
                public void onSelect(boolean positive, String text) {
                    if (positive) {
                        try {
                            int value = Integer.parseInt(text);
                            field.setAccessible(true);
                            field.set(item, value);
                            GLog.p("已将 " + field.getName() + " 设置为 " + value);
                            hide();
                            // 重新打开窗口以显示更新后的值
                            GameScene.show(new WndItemEditor(item));
                        } catch (Exception e) {
                            GLog.w("设置失败: " + e.getMessage());
                        }
                    }
                }
            });
        } else if (type == Boolean.class) {
            boolean newValue = !((Boolean) currentValue);
            try {
                field.setAccessible(true);
                field.set(item, newValue);
                GLog.p("已将 " + field.getName() + " 设置为 " + newValue);
                hide();
                GameScene.show(new WndItemEditor(item));
            } catch (Exception e) {
                GLog.w("设置失败: " + e.getMessage());
            }
        } else if (type == Float.class) {
            GameScene.show(new WndTextInput(
                    "编辑 " + field.getName(),
                    "当前值: " + currentValue,
                    String.valueOf(currentValue),
                    20,
                    false,
                    "确定",
                    "取消"
            ) {
                @Override
                public void onSelect(boolean positive, String text) {
                    if (positive) {
                        try {
                            float value = Float.parseFloat(text);
                            field.setAccessible(true);
                            field.set(item, value);
                            GLog.p("已将 " + field.getName() + " 设置为 " + value);
                            hide();
                            GameScene.show(new WndItemEditor(item));
                        } catch (Exception e) {
                            GLog.w("设置失败: " + e.getMessage());
                        }
                    }
                }
            });
        } else if (type == String.class) {
            GameScene.show(new WndTextInput(
                    "编辑 " + field.getName(),
                    "当前值: " + currentValue,
                    String.valueOf(currentValue),
                    50,
                    false,
                    "确定",
                    "取消"
            ) {
                @Override
                public void onSelect(boolean positive, String text) {
                    if (positive) {
                        try {
                            field.setAccessible(true);
                            field.set(item, text);
                            GLog.p("已将 " + field.getName() + " 设置为 " + text);
                            hide();
                            GameScene.show(new WndItemEditor(item));
                        } catch (Exception e) {
                            GLog.w("设置失败: " + e.getMessage());
                        }
                    }
                }
            });
        }
    }

    private void editConfigField(String configKey, Object currentValue, Class<?> type) {
        // 尝试调用 setConfig 方法
        try {
            Method setConfig = item.getClass().getMethod("setConfig", String.class, Object.class);
            if (type == Integer.class) {
                GameScene.show(new WndTextInput(
                        "编辑 " + configKey,
                        "当前值: " + currentValue,
                        String.valueOf(currentValue),
                        10,
                        false,
                        "确定",
                        "取消"
                ) {
                    @Override
                    public void onSelect(boolean positive, String text) {
                        if (positive) {
                            try {
                                int value = Integer.parseInt(text);
                                setConfig.invoke(item, configKey, value);
                                GLog.p("已将 " + configKey + " 设置为 " + value);
                                hide();
                                GameScene.show(new WndItemEditor(item));
                            } catch (Exception e) {
                                GLog.w("设置失败: " + e.getMessage());
                            }
                        }
                    }
                });
            } else if (type == Boolean.class) {
                boolean newValue = !((Boolean) currentValue);
                try {
                    setConfig.invoke(item, configKey, newValue);
                    GLog.p("已将 " + configKey + " 设置为 " + newValue);
                    hide();
                    GameScene.show(new WndItemEditor(item));
                } catch (Exception e) {
                    GLog.w("设置失败: " + e.getMessage());
                }
            } else if (type == Float.class) {
                GameScene.show(new WndTextInput(
                        "编辑 " + configKey,
                        "当前值: " + currentValue,
                        String.valueOf(currentValue),
                        20,
                        false,
                        "确定",
                        "取消"
                ) {
                    @Override
                    public void onSelect(boolean positive, String text) {
                        if (positive) {
                            try {
                                float value = Float.parseFloat(text);
                                setConfig.invoke(item, configKey, value);
                                GLog.p("已将 " + configKey + " 设置为 " + value);
                                hide();
                                GameScene.show(new WndItemEditor(item));
                            } catch (Exception e) {
                                GLog.w("设置失败: " + e.getMessage());
                            }
                        }
                    }
                });
            } else if (type == String.class) {
                GameScene.show(new WndTextInput(
                        "编辑 " + configKey,
                        "当前值: " + currentValue,
                        String.valueOf(currentValue),
                        50,
                        false,
                        "确定",
                        "取消"
                ) {
                    @Override
                    public void onSelect(boolean positive, String text) {
                        if (positive) {
                            try {
                                setConfig.invoke(item, configKey, text);
                                GLog.p("已将 " + configKey + " 设置为 " + text);
                                hide();
                                GameScene.show(new WndItemEditor(item));
                            } catch (Exception e) {
                                GLog.w("设置失败: " + e.getMessage());
                            }
                        }
                    }
                });
            }
        } catch (Exception e) {
            GLog.w("该物品不支持 setConfig 方法");
        }
    }

    private void addSTRReqOverrideButton(Weapon weapon) {
        // 尝试查找或创建 strReqOverride 字段
        Field overrideField = findField(weapon.getClass(), "strReqOverride");
        int currentSTRReq = weapon.STRReq();
        
        String buttonText = "力量需求覆盖: ";
        if (overrideField != null) {
            try {
                overrideField.setAccessible(true);
                Object overrideValue = overrideField.get(weapon);
                if (overrideValue != null) {
                    buttonText += overrideValue + " (覆盖)";
                } else {
                    buttonText += currentSTRReq + " (默认)";
                }
            } catch (Exception e) {
                buttonText += currentSTRReq;
            }
        } else {
            buttonText += currentSTRReq + " (点击设置覆盖)";
        }
        
        RedButton btn = new RedButton(buttonText) {
            @Override
            protected void onClick() {
                editSTRReqOverride(weapon, overrideField, currentSTRReq);
            }
        };
        btn.setRect(MARGIN, pos, WIDTH - MARGIN * 2, BTN_HEIGHT);
        add(btn);
        pos += BTN_HEIGHT + GAP;
    }

    private void editSTRReqOverride(Weapon weapon, Field overrideField, int currentValue) {
        // 尝试使用配置系统或直接添加字段
        try {
            // 先尝试使用配置系统
            Method setConfig = weapon.getClass().getMethod("setConfig", String.class, Object.class);
            Method getConfig = weapon.getClass().getMethod("getConfig");
            Object config = getConfig.invoke(weapon);
            
            int currentOverride = currentValue;
            if (config instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> configMap = (Map<String, Object>) config;
                if (configMap.containsKey("strReqOverride")) {
                    currentOverride = ((Number) configMap.get("strReqOverride")).intValue();
                }
            }
            
            GameScene.show(new WndTextInput(
                    "编辑力量需求覆盖",
                    "当前值: " + currentOverride + "\n(设置为 -1 使用默认计算值)",
                    String.valueOf(currentOverride),
                    10,
                    false,
                    "确定",
                    "取消"
            ) {
                @Override
                public void onSelect(boolean positive, String text) {
                    if (positive) {
                        try {
                            int value = Integer.parseInt(text);
                            setConfig.invoke(weapon, "strReqOverride", value == -1 ? null : value);
                            GLog.p("已将力量需求覆盖设置为 " + (value == -1 ? "默认" : value));
                            hide();
                            GameScene.show(new WndItemEditor(weapon));
                        } catch (Exception e) {
                            GLog.w("设置失败: " + e.getMessage());
                        }
                    }
                }
            });
            return;
        } catch (Exception e) {
            // 配置系统不可用，尝试直接修改字段
        }
        
        // 如果配置系统不可用，尝试直接修改字段（需要字段已存在）
        if (overrideField != null) {
            GameScene.show(new WndTextInput(
                    "编辑力量需求覆盖",
                    "当前值: " + currentValue + "\n(设置为 -1 使用默认计算值)",
                    String.valueOf(currentValue),
                    10,
                    false,
                    "确定",
                    "取消"
            ) {
                @Override
                public void onSelect(boolean positive, String text) {
                    if (positive) {
                        try {
                            int value = Integer.parseInt(text);
                            overrideField.setAccessible(true);
                            overrideField.set(weapon, value == -1 ? null : value);
                            GLog.p("已将力量需求覆盖设置为 " + (value == -1 ? "默认" : value));
                            hide();
                            GameScene.show(new WndItemEditor(weapon));
                        } catch (Exception e) {
                            GLog.w("设置失败: " + e.getMessage());
                        }
                    }
                }
            });
        } else {
            // 如果字段不存在，提示用户修改 tier 来间接影响 STRReq
            GLog.w("该武器类型不支持直接覆盖力量需求。请通过修改 Tier 来间接影响力量需求。");
        }
    }

    private Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                return field;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
}

