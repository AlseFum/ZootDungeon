package com.zootdungeon.utils;

import com.zootdungeon.items.material.Gold;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Lua脚本管理器
 * 提供Lua脚本的加载、执行和管理功能
 * 
 * <h2>Lua集成指南</h2>
 * 
 * <p>本项目已集成Lua脚本支持，使用Luaj库（纯Java实现，跨平台兼容）。</p>
 * 
 * <h3>依赖</h3>
 * 
 * <p>Lua支持通过Luaj库提供，已在{@code core/build.gradle}中配置：</p>
 * <pre>{@code
 * implementation 'org.luaj:luaj-jse:3.0.1'
 * }</pre>
 * 
 * <h3>基本使用</h3>
 * 
 * <h4>1. 获取LuaScriptManager实例</h4>
 * <pre>{@code
 * LuaScriptManager lua = LuaScriptManager.getInstance();
 * }</pre>
 * 
 * <h4>2. 执行Lua脚本字符串</h4>
 * <pre>{@code
 * String script = "return 1 + 2";
 * LuaValue result = lua.loadString("test", script);
 * int value = result.toint(); // 结果为3
 * }</pre>
 * 
 * <h4>3. 从文件加载Lua脚本</h4>
 * 
 * <p>将Lua脚本文件放在{@code core/src/main/assets/scripts/}目录下，然后加载：</p>
 * <pre>{@code
 * // 加载脚本（相对于assets目录）
 * LuaValue script = lua.loadFile("scripts/example.lua");
 * 
 * // 执行脚本
 * script.call();
 * }</pre>
 * 
 * <h4>4. 调用Lua函数</h4>
 * 
 * <p>假设有一个Lua脚本文件{@code scripts/calculator.lua}：</p>
 * <pre>{@code
 * function add(a, b)
 *     return a + b
 * end
 * 
 * function multiply(a, b)
 *     return a * b
 * end
 * }</pre>
 * 
 * <p>在Java中调用：</p>
 * <pre>{@code
 * LuaScriptManager lua = LuaScriptManager.getInstance();
 * 
 * // 调用add函数
 * LuaValue result = lua.callFunction("scripts/calculator.lua", "add", 
 *     LuaValue.valueOf(5), LuaValue.valueOf(3));
 * int sum = result.toint(); // 结果为8
 * 
 * // 调用multiply函数
 * result = lua.callFunction("scripts/calculator.lua", "multiply", 
 *     LuaValue.valueOf(4), LuaValue.valueOf(7));
 * int product = result.toint(); // 结果为28
 * }</pre>
 * 
 * <h4>5. 设置和获取全局变量</h4>
 * <pre>{@code
 * LuaScriptManager lua = LuaScriptManager.getInstance();
 * 
 * // 设置全局变量
 * lua.setGlobal("playerLevel", 10);
 * lua.setGlobal("playerName", "Hero");
 * 
 * // 在Lua脚本中使用
 * String script = "return playerName .. ' is level ' .. playerLevel";
 * LuaValue result = lua.loadString("test", script);
 * String output = result.tojstring(); // "Hero is level 10"
 * }</pre>
 * 
 * <h3>内置函数</h3>
 * 
 * <p>Lua脚本可以使用以下内置函数：</p>
 * 
 * <h4>log(message)</h4>
 * <p>输出日志信息</p>
 * <pre>{@code
 * log("Hello from Lua!")
 * }</pre>
 * 
 * <h4>random(min, max)</h4>
 * <p>生成随机整数</p>
 * <pre>{@code
 * local value = random(1, 100)
 * }</pre>
 * 
 * <h4>randomFloat()</h4>
 * <p>生成0到1之间的随机浮点数</p>
 * <pre>{@code
 * local value = randomFloat()
 * }</pre>
 * 
 * <h3>在游戏中使用示例</h3>
 * 
 * <h4>示例1：动态伤害计算</h4>
 * 
 * <p>创建{@code scripts/damage_calc.lua}：</p>
 * <pre>{@code
 * function calculateDamage(baseDamage, strength, weaponBonus)
 *     local damage = baseDamage + (strength * 2) + weaponBonus
 *     local crit = randomFloat()
 *     if crit > 0.9 then
 *         damage = damage * 2
 *         log("Critical hit!")
 *     end
 *     return damage
 * end
 * }</pre>
 * 
 * <p>在Java中使用：</p>
 * <pre>{@code
 * LuaScriptManager lua = LuaScriptManager.getInstance();
 * LuaValue damage = lua.callFunction("scripts/damage_calc.lua", "calculateDamage",
 *     LuaValue.valueOf(10),  // baseDamage
 *     LuaValue.valueOf(5),  // strength
 *     LuaValue.valueOf(3)   // weaponBonus
 * );
 * int finalDamage = damage.toint();
 * }</pre>
 * 
 * <h4>示例2：AI行为脚本</h4>
 * 
 * <p>创建{@code scripts/mob_ai.lua}：</p>
 * <pre>{@code
 * function shouldAttack(playerHealth, mobHealth, distance)
 *     if distance > 5 then
 *         return false  -- 太远，不攻击
 *     end
 *     if playerHealth < mobHealth * 0.5 then
 *         return true   -- 玩家血量低，攻击
 *     end
 *     return randomFloat() > 0.3  -- 30%概率攻击
 * end
 * }</pre>
 * 
 * <h3>高级用法</h3>
 * 
 * <h4>直接访问Globals</h4>
 * 
 * <p>如果需要更高级的操作，可以获取Globals对象：</p>
 * <pre>{@code
 * Globals globals = LuaScriptManager.getInstance().getGlobals();
 * 
 * // 注册自定义Java函数到Lua
 * globals.set("customFunction", new org.luaj.vm2.lib.ZeroArgFunction() {
 *     @Override
 *     public LuaValue call() {
 *         // 实现自定义逻辑
 *         return LuaValue.valueOf("result");
 *     }
 * });
 * }</pre>
 * 
 * <h4>清除缓存</h4>
 * <pre>{@code
 * // 清除特定脚本的缓存
 * lua.clearCache("scripts/example.lua");
 * 
 * // 清除所有脚本缓存
 * lua.clearCache(null);
 * }</pre>
 * 
 * <h3>注意事项</h3>
 * 
 * <ol>
 *   <li><b>性能</b>：Lua脚本执行有一定开销，避免在频繁调用的代码路径中使用</li>
 *   <li><b>错误处理</b>：Lua脚本执行失败时会返回{@code LuaValue.NIL}，记得检查返回值</li>
 *   <li><b>线程安全</b>：LuaScriptManager是单例，但在多线程环境下需要注意同步</li>
 *   <li><b>文件路径</b>：从assets加载时，路径应该以{@code /}开头，如{@code /scripts/example.lua}</li>
 * </ol>
 * 
 * <h3>更多资源</h3>
 * 
 * <ul>
 *   <li><a href="https://github.com/luaj/luaj">Luaj官方文档</a></li>
 *   <li><a href="https://www.lua.org/manual/5.3/">Lua 5.3参考手册</a></li>
 * </ul>
 * 
 * @author ColaDungeon Team
 */
public class LuaScriptManager {
    
    private static LuaScriptManager instance;
    private final Globals globals;
    private final Map<String, LuaValue> loadedScripts;
    
    private LuaScriptManager() {
        globals = JsePlatform.standardGlobals();
        loadedScripts = new HashMap<>();
        setupGlobalFunctions();
    }
    
    /**
     * 获取LuaScriptManager单例
     */
    public static synchronized LuaScriptManager getInstance() {
        if (instance == null) {
            instance = new LuaScriptManager();
        }
        return instance;
    }
    
    /**
     * 设置全局函数，供Lua脚本调用
     */
    private void setupGlobalFunctions() {
        // 注册日志函数
        globals.set("log", new org.luaj.vm2.lib.OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                GLog.i("[Lua] " + arg.tojstring());
                return LuaValue.NIL;
            }
        });
        
        // 注册随机数函数
        globals.set("random", new org.luaj.vm2.lib.TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue min, LuaValue max) {
                int minVal = min.toint();
                int maxVal = max.toint();
                return LuaValue.valueOf(com.watabou.utils.Random.Int(minVal, maxVal));
            }
        });
        
        // 注册随机浮点数函数
        globals.set("randomFloat", new org.luaj.vm2.lib.ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(com.watabou.utils.Random.Float());
            }
        });
        
        // ========== 暴露Dungeon对象给Lua ==========
        setupDungeonBinding();
        
        // ========== 反射功能 ==========
        setupReflectionBinding();
    }
    
    /**
     * 设置反射功能的Lua绑定
     */
    private void setupReflectionBinding() {
        LuaValue reflection = LuaValue.tableOf();
        
        // 获取类
        reflection.set("getClass", new org.luaj.vm2.lib.OneArgFunction() {
            @Override
            public LuaValue call(LuaValue className) {
                try {
                    String name = className.tojstring();
                    Class<?> clazz = Class.forName(name);
                    return createClassBinding(clazz);
                } catch (ClassNotFoundException e) {
                    GLog.w("Class not found: " + className.tojstring());
                    return LuaValue.NIL;
                } catch (Exception e) {
                    GLog.w("Failed to get class: " + e.getMessage());
                    return LuaValue.NIL;
                }
            }
        });
        
        // 创建对象实例
        reflection.set("newInstance", new org.luaj.vm2.lib.VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                try {
                    if (args.narg() < 1) {
                        return LuaValue.NIL;
                    }
                    
                    String className = args.arg(1).tojstring();
                    Class<?> clazz = Class.forName(className);
                    
                    // 获取构造函数参数类型
                    java.util.List<Class<?>> paramTypes = new java.util.ArrayList<>();
                    java.util.List<Object> paramValues = new java.util.ArrayList<>();
                    
                    for (int i = 2; i <= args.narg(); i++) {
                        LuaValue arg = args.arg(i);
                        if (arg.isint()) {
                            paramTypes.add(int.class);
                            paramValues.add(arg.toint());
                        } else if (arg.isnumber()) {
                            paramTypes.add(double.class);
                            paramValues.add(arg.todouble());
                        } else if (arg.isstring()) {
                            paramTypes.add(String.class);
                            paramValues.add(arg.tojstring());
                        } else if (arg.isboolean()) {
                            paramTypes.add(boolean.class);
                            paramValues.add(arg.toboolean());
                        } else {
                            // 尝试作为对象传递
                            paramTypes.add(Object.class);
                            paramValues.add(null);
                        }
                    }
                    
                    java.lang.reflect.Constructor<?> constructor = clazz.getConstructor(
                        paramTypes.toArray(new Class[0]));
                    Object instance = constructor.newInstance(paramValues.toArray());
                    
                    return createObjectBinding(instance);
                } catch (Exception e) {
                    GLog.w("Failed to create instance: " + e.getMessage());
                    return LuaValue.NIL;
                }
            }
        });
        
        // 调用静态方法
        reflection.set("callStatic", new org.luaj.vm2.lib.VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                try {
                    if (args.narg() < 2) {
                        return LuaValue.NIL;
                    }
                    
                    String className = args.arg(1).tojstring();
                    String methodName = args.arg(2).tojstring();
                    Class<?> clazz = Class.forName(className);
                    
                    // 获取方法参数
                    java.util.List<Class<?>> paramTypes = new java.util.ArrayList<>();
                    java.util.List<Object> paramValues = new java.util.ArrayList<>();
                    
                    for (int i = 3; i <= args.narg(); i++) {
                        LuaValue arg = args.arg(i);
                        if (arg.isint()) {
                            paramTypes.add(int.class);
                            paramValues.add(arg.toint());
                        } else if (arg.isnumber()) {
                            paramTypes.add(double.class);
                            paramValues.add(arg.todouble());
                        } else if (arg.isstring()) {
                            paramTypes.add(String.class);
                            paramValues.add(arg.tojstring());
                        } else if (arg.isboolean()) {
                            paramTypes.add(boolean.class);
                            paramValues.add(arg.toboolean());
                        }
                    }
                    
                    java.lang.reflect.Method method = clazz.getMethod(methodName,
                        paramTypes.toArray(new Class[0]));
                    Object result = method.invoke(null, paramValues.toArray());
                    
                    return convertToLuaValue(result);
                } catch (Exception e) {
                    GLog.w("Failed to call static method: " + e.getMessage());
                    return LuaValue.NIL;
                }
            }
        });
        
        // 获取静态字段
        reflection.set("getStaticField", new org.luaj.vm2.lib.TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue className, LuaValue fieldName) {
                try {
                    Class<?> clazz = Class.forName(className.tojstring());
                    java.lang.reflect.Field field = clazz.getField(fieldName.tojstring());
                    Object value = field.get(null);
                    return convertToLuaValue(value);
                } catch (Exception e) {
                    GLog.w("Failed to get static field: " + e.getMessage());
                    return LuaValue.NIL;
                }
            }
        });
        
        // 设置静态字段
        reflection.set("setStaticField", new org.luaj.vm2.lib.ThreeArgFunction() {
            @Override
            public LuaValue call(LuaValue className, LuaValue fieldName, LuaValue value) {
                try {
                    Class<?> clazz = Class.forName(className.tojstring());
                    java.lang.reflect.Field field = clazz.getField(fieldName.tojstring());
                    Object javaValue = convertFromLuaValue(value, field.getType());
                    field.set(null, javaValue);
                    return LuaValue.valueOf(true);
                } catch (Exception e) {
                    GLog.w("Failed to set static field: " + e.getMessage());
                    return LuaValue.valueOf(false);
                }
            }
        });
        
        globals.set("Java", reflection);
    }
    
    /**
     * 创建类的绑定
     */
    private LuaValue createClassBinding(Class<?> clazz) {
        LuaValue binding = LuaValue.tableOf();
        
        // 类名
        binding.set("name", LuaValue.valueOf(clazz.getName()));
        binding.set("simpleName", LuaValue.valueOf(clazz.getSimpleName()));
        
        // 创建实例
        binding.set("new", new org.luaj.vm2.lib.VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                try {
                    java.util.List<Class<?>> paramTypes = new java.util.ArrayList<>();
                    java.util.List<Object> paramValues = new java.util.ArrayList<>();
                    
                    for (int i = 1; i <= args.narg(); i++) {
                        LuaValue arg = args.arg(i);
                        if (arg.isint()) {
                            paramTypes.add(int.class);
                            paramValues.add(arg.toint());
                        } else if (arg.isnumber()) {
                            paramTypes.add(double.class);
                            paramValues.add(arg.todouble());
                        } else if (arg.isstring()) {
                            paramTypes.add(String.class);
                            paramValues.add(arg.tojstring());
                        }
                    }
                    
                    java.lang.reflect.Constructor<?> constructor = clazz.getConstructor(
                        paramTypes.toArray(new Class[0]));
                    Object instance = constructor.newInstance(paramValues.toArray());
                    
                    return createObjectBinding(instance);
                } catch (Exception e) {
                    GLog.w("Failed to create instance: " + e.getMessage());
                    return LuaValue.NIL;
                }
            }
        });
        
        return binding;
    }
    
    /**
     * 创建对象的绑定
     */
    private LuaValue createObjectBinding(Object obj) {
        LuaValue binding = LuaValue.tableOf();
        final Object target = obj;
        final Class<?> clazz = obj.getClass();
        
        // 调用方法
        binding.set("call", new org.luaj.vm2.lib.VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                try {
                    if (args.narg() < 1) {
                        return LuaValue.NIL;
                    }
                    
                    String methodName = args.arg(1).tojstring();
                    java.util.List<Class<?>> paramTypes = new java.util.ArrayList<>();
                    java.util.List<Object> paramValues = new java.util.ArrayList<>();
                    
                    for (int i = 2; i <= args.narg(); i++) {
                        LuaValue arg = args.arg(i);
                        if (arg.isint()) {
                            paramTypes.add(int.class);
                            paramValues.add(arg.toint());
                        } else if (arg.isnumber()) {
                            paramTypes.add(double.class);
                            paramValues.add(arg.todouble());
                        } else if (arg.isstring()) {
                            paramTypes.add(String.class);
                            paramValues.add(arg.tojstring());
                        } else if (arg.isboolean()) {
                            paramTypes.add(boolean.class);
                            paramValues.add(arg.toboolean());
                        }
                    }
                    
                    java.lang.reflect.Method method = clazz.getMethod(methodName,
                        paramTypes.toArray(new Class[0]));
                    Object result = method.invoke(target, paramValues.toArray());
                    
                    return convertToLuaValue(result);
                } catch (Exception e) {
                    GLog.w("Failed to call method: " + e.getMessage());
                    return LuaValue.NIL;
                }
            }
        });
        
        // 获取字段
        binding.set("get", new org.luaj.vm2.lib.OneArgFunction() {
            @Override
            public LuaValue call(LuaValue fieldName) {
                try {
                    java.lang.reflect.Field field = clazz.getField(fieldName.tojstring());
                    Object value = field.get(target);
                    return convertToLuaValue(value);
                } catch (Exception e) {
                    GLog.w("Failed to get field: " + e.getMessage());
                    return LuaValue.NIL;
                }
            }
        });
        
        // 设置字段
        binding.set("set", new org.luaj.vm2.lib.TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue fieldName, LuaValue value) {
                try {
                    java.lang.reflect.Field field = clazz.getField(fieldName.tojstring());
                    Object javaValue = convertFromLuaValue(value, field.getType());
                    field.set(target, javaValue);
                    return LuaValue.valueOf(true);
                } catch (Exception e) {
                    GLog.w("Failed to set field: " + e.getMessage());
                    return LuaValue.valueOf(false);
                }
            }
        });
        
        return binding;
    }
    
    /**
     * 将Java对象转换为Lua值
     */
    private LuaValue convertToLuaValue(Object obj) {
        if (obj == null) {
            return LuaValue.NIL;
        } else if (obj instanceof Integer) {
            return LuaValue.valueOf((Integer) obj);
        } else if (obj instanceof Double || obj instanceof Float) {
            return LuaValue.valueOf(((Number) obj).doubleValue());
        } else if (obj instanceof Boolean) {
            return LuaValue.valueOf((Boolean) obj);
        } else if (obj instanceof String) {
            return LuaValue.valueOf((String) obj);
        } else {
            // 对于其他对象，创建绑定
            return createObjectBinding(obj);
        }
    }
    
    /**
     * 将Lua值转换为Java对象
     */
    private Object convertFromLuaValue(LuaValue value, Class<?> targetType) {
        if (targetType == int.class || targetType == Integer.class) {
            return value.toint();
        } else if (targetType == double.class || targetType == Double.class || targetType == float.class || targetType == Float.class) {
            return value.todouble();
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            return value.toboolean();
        } else if (targetType == String.class) {
            return value.tojstring();
        } else {
            return null;
        }
    }
    
    /**
     * 设置Dungeon对象的Lua绑定
     */
    private void setupDungeonBinding() {
        // 创建一个Lua表来代表Dungeon对象
        LuaValue dungeon = LuaValue.tableOf();
        
        // 暴露静态字段
        dungeon.set("depth", new org.luaj.vm2.lib.ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(com.zootdungeon.Dungeon.depth);
            }
        });
        
        dungeon.set("gold", new org.luaj.vm2.lib.ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(com.zootdungeon.Dungeon.gold);
            }
        });
        
        // 设置金币
        dungeon.set("setGold", new org.luaj.vm2.lib.OneArgFunction() {
            @Override
            public LuaValue call(LuaValue amount) {
                com.zootdungeon.Dungeon.gold = amount.toint();
                return LuaValue.valueOf(com.zootdungeon.Dungeon.gold);
            }
        });
        
        // 获取hero对象
        dungeon.set("hero", new org.luaj.vm2.lib.ZeroArgFunction() {
            @Override
            public LuaValue call() {
                if (com.zootdungeon.Dungeon.hero != null) {
                    return setupHeroBinding();
                }
                return LuaValue.NIL;
            }
        });
        
        // 获取level对象
        dungeon.set("level", new org.luaj.vm2.lib.ZeroArgFunction() {
            @Override
            public LuaValue call() {
                if (com.zootdungeon.Dungeon.level != null) {
                    return setupLevelBinding();
                }
                return LuaValue.NIL;
            }
        });
        
        // 放置物品到指定位置
        dungeon.set("drop", new org.luaj.vm2.lib.TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue itemType, LuaValue cell) {
                if (com.zootdungeon.Dungeon.level != null) {
                    try {
                        String type = itemType.tojstring().toLowerCase();
                        int pos = cell.toint();
                        if (pos >= 0 && pos < com.zootdungeon.Dungeon.level.length()) {
                            com.zootdungeon.items.Item item = null;
                            
                            switch (type) {
                                case "gold":
                                    item = new Gold(100);
                                    break;
                                case "healing":
                                case "heal":
                                    item = new com.zootdungeon.items.potions.PotionOfHealing();
                                    break;
                                case "strength":
                                    item = new com.zootdungeon.items.potions.PotionOfStrength();
                                    break;
                                case "upgrade":
                                    item = new com.zootdungeon.items.scrolls.ScrollOfUpgrade();
                                    break;
                                default:
                                    return LuaValue.valueOf(false);
                            }
                            
                            if (item != null) {
                                com.zootdungeon.items.Heap heap = com.zootdungeon.Dungeon.level.drop(item, pos);
                                if (heap != null && heap.sprite != null) {
                                    heap.sprite.drop();
                                }
                                return LuaValue.valueOf(true);
                            }
                        }
                    } catch (Exception e) {
                        GLog.w("Failed to drop item: " + e.getMessage());
                    }
                }
                return LuaValue.valueOf(false);
            }
        });
        
        // 将dungeon表设置为全局变量
        globals.set("Dungeon", dungeon);
    }
    
    /**
     * 设置Hero对象的Lua绑定
     */
    private LuaValue setupHeroBinding() {
        LuaValue hero = LuaValue.tableOf();
        final com.zootdungeon.actors.hero.Hero h = com.zootdungeon.Dungeon.hero;
        
        // HP相关
        hero.set("HP", new org.luaj.vm2.lib.ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(h.HP);
            }
        });
        
        hero.set("setHP", new org.luaj.vm2.lib.OneArgFunction() {
            @Override
            public LuaValue call(LuaValue hp) {
                int newHP = Math.min(hp.toint(), h.HT);
                h.HP = Math.max(0, newHP);
                return LuaValue.valueOf(h.HP);
            }
        });
        
        hero.set("maxHP", new org.luaj.vm2.lib.ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(h.HT);
            }
        });
        
        hero.set("heal", new org.luaj.vm2.lib.OneArgFunction() {
            @Override
            public LuaValue call(LuaValue amount) {
                int heal = amount.toint();
                h.HP = Math.min(h.HP + heal, h.HT);
                return LuaValue.valueOf(h.HP);
            }
        });
        
        hero.set("damage", new org.luaj.vm2.lib.OneArgFunction() {
            @Override
            public LuaValue call(LuaValue amount) {
                int damage = amount.toint();
                h.damage(damage, null);
                return LuaValue.valueOf(h.HP);
            }
        });
        
        // 位置相关
        hero.set("pos", new org.luaj.vm2.lib.ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(h.pos);
            }
        });
        
        hero.set("teleport", new org.luaj.vm2.lib.OneArgFunction() {
            @Override
            public LuaValue call(LuaValue cell) {
                int pos = cell.toint();
                if (com.zootdungeon.Dungeon.level != null && pos >= 0 && pos < com.zootdungeon.Dungeon.level.length()) {
                    h.pos = pos;
                    if (h.sprite != null) {
                        h.sprite.place(pos);
                    }
                    return LuaValue.valueOf(true);
                }
                return LuaValue.valueOf(false);
            }
        });
        
        // 等级相关
        hero.set("level", new org.luaj.vm2.lib.ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(h.lvl);
            }
        });
        
        // 经验相关
        hero.set("exp", new org.luaj.vm2.lib.ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(h.exp);
            }
        });
        
        hero.set("maxExp", new org.luaj.vm2.lib.ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(h.maxExp());
            }
        });
        
        return hero;
    }
    
    /**
     * 设置Level对象的Lua绑定
     */
    private LuaValue setupLevelBinding() {
        LuaValue level = LuaValue.tableOf();
        final com.zootdungeon.levels.Level l = com.zootdungeon.Dungeon.level;
        
        // 尺寸相关
        level.set("width", new org.luaj.vm2.lib.ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(l.width());
            }
        });
        
        level.set("height", new org.luaj.vm2.lib.ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(l.height());
            }
        });
        
        level.set("length", new org.luaj.vm2.lib.ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(l.length());
            }
        });
        
        // 位置检查
        level.set("isValid", new org.luaj.vm2.lib.OneArgFunction() {
            @Override
            public LuaValue call(LuaValue cell) {
                int pos = cell.toint();
                return LuaValue.valueOf(pos >= 0 && pos < l.length());
            }
        });
        
        level.set("isPassable", new org.luaj.vm2.lib.OneArgFunction() {
            @Override
            public LuaValue call(LuaValue cell) {
                int pos = cell.toint();
                if (pos >= 0 && pos < l.length()) {
                    return LuaValue.valueOf(l.passable[pos]);
                }
                return LuaValue.valueOf(false);
            }
        });
        
        // 放置物品
        level.set("drop", new org.luaj.vm2.lib.TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue item, LuaValue cell) {
                try {
                    // 这里item应该是从Java传入的Item对象
                    // 为了简化，我们接受字符串类型，然后创建对应的物品
                    String itemType = item.tojstring().toLowerCase();
                    int pos = cell.toint();
                    if (pos >= 0 && pos < l.length()) {
                        com.zootdungeon.items.Item itemObj = null;
                        switch (itemType) {
                            case "gold":
                                itemObj = new Gold(100);
                                break;
                            case "healing":
                                itemObj = new com.zootdungeon.items.potions.PotionOfHealing();
                                break;
                            default:
                                return LuaValue.valueOf(false);
                        }
                        if (itemObj != null) {
                            com.zootdungeon.items.Heap heap = l.drop(itemObj, pos);
                            if (heap != null && heap.sprite != null) {
                                heap.sprite.drop();
                            }
                            return LuaValue.valueOf(true);
                        }
                    }
                } catch (Exception e) {
                    GLog.w("Failed to drop item on level: " + e.getMessage());
                }
                return LuaValue.valueOf(false);
            }
        });
        
        return level;
    }
    
    /**
     * 从字符串加载并执行Lua脚本
     * 
     * @param scriptName 脚本名称（用于缓存）
     * @param scriptContent Lua脚本内容
     * @return 执行结果
     */
    public LuaValue loadString(String scriptName, String scriptContent) {
        try {
            LuaValue chunk = globals.load(scriptContent);
            loadedScripts.put(scriptName, chunk);
            return chunk.call();
        } catch (Exception e) {
            GLog.w("Failed to load Lua script: " + scriptName + ", error: " + e.getMessage());
            return LuaValue.NIL;
        }
    }
    
    /**
     * 从文件加载Lua脚本
     * 
     * @param filePath 文件路径（相对于assets目录）
     * @return 加载的LuaValue对象
     */
    public LuaValue loadFile(String filePath) {
        if (loadedScripts.containsKey(filePath)) {
            return loadedScripts.get(filePath);
        }
        
        try {
            // 尝试从assets目录加载
            InputStream inputStream = com.zootdungeon.Assets.class.getResourceAsStream("/" + filePath);
            if (inputStream == null) {
                // 如果assets中没有，尝试从文件系统加载
                File file = new File(filePath);
                if (file.exists()) {
                    inputStream = new FileInputStream(file);
                } else {
                    GLog.w("Lua script file not found: " + filePath);
                    return LuaValue.NIL;
                }
            }
            
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();
            
            String scriptContent = new String(buffer, "UTF-8");
            LuaValue chunk = globals.load(scriptContent);
            loadedScripts.put(filePath, chunk);
            return chunk;
        } catch (IOException e) {
            GLog.w("Failed to load Lua script file: " + filePath + ", error: " + e.getMessage());
            return LuaValue.NIL;
        }
    }
    
    /**
     * 执行已加载的Lua脚本
     * 
     * @param scriptName 脚本名称
     * @param args 传递给脚本的参数
     * @return 执行结果
     */
    public LuaValue execute(String scriptName, LuaValue... args) {
        LuaValue chunk = loadedScripts.get(scriptName);
        if (chunk == null) {
            GLog.w("Script not loaded: " + scriptName);
            return LuaValue.NIL;
        }
        
        try {
            if (args.length > 0) {
                Varargs result = chunk.invoke(LuaValue.varargsOf(args));
                return result.arg1(); // 返回第一个返回值
            } else {
                return chunk.call();
            }
        } catch (Exception e) {
            GLog.w("Failed to execute Lua script: " + scriptName + ", error: " + e.getMessage());
            return LuaValue.NIL;
        }
    }
    
    /**
     * 调用Lua脚本中的函数
     * 
     * @param scriptName 脚本名称
     * @param functionName 函数名
     * @param args 函数参数
     * @return 函数返回值
     */
    public LuaValue callFunction(String scriptName, String functionName, LuaValue... args) {
        LuaValue chunk = loadedScripts.get(scriptName);
        if (chunk == null) {
            // 如果脚本未加载，尝试加载
            chunk = loadFile(scriptName);
            if (chunk == LuaValue.NIL) {
                return LuaValue.NIL;
            }
        }
        
        // 先执行脚本以定义函数
        chunk.call();
        
        // 获取函数并调用
        LuaValue func = globals.get(functionName);
        if (func.isnil() || !func.isfunction()) {
            GLog.w("Function not found in script: " + functionName);
            return LuaValue.NIL;
        }
        
        try {
            if (args.length > 0) {
                Varargs result = func.invoke(LuaValue.varargsOf(args));
                return result.arg1(); // 返回第一个返回值
            } else {
                return func.call();
            }
        } catch (Exception e) {
            GLog.w("Failed to call Lua function: " + functionName + ", error: " + e.getMessage());
            return LuaValue.NIL;
        }
    }
    
    /**
     * 设置全局变量供Lua脚本使用
     * 
     * @param name 变量名
     * @param value 变量值
     */
    public void setGlobal(String name, Object value) {
        if (value instanceof String) {
            globals.set(name, LuaValue.valueOf((String) value));
        } else if (value instanceof Integer) {
            globals.set(name, LuaValue.valueOf((Integer) value));
        } else if (value instanceof Double || value instanceof Float) {
            globals.set(name, LuaValue.valueOf(((Number) value).doubleValue()));
        } else if (value instanceof Boolean) {
            globals.set(name, LuaValue.valueOf((Boolean) value));
        } else {
            globals.set(name, LuaValue.valueOf(value.toString()));
        }
    }
    
    /**
     * 获取全局变量
     * 
     * @param name 变量名
     * @return 变量值
     */
    public LuaValue getGlobal(String name) {
        return globals.get(name);
    }
    
    /**
     * 清除缓存的脚本
     * 
     * @param scriptName 脚本名称，如果为null则清除所有
     */
    public void clearCache(String scriptName) {
        if (scriptName == null) {
            loadedScripts.clear();
        } else {
            loadedScripts.remove(scriptName);
        }
    }
    
    /**
     * 获取Globals对象，用于高级操作
     * 
     * @return Globals对象
     */
    public Globals getGlobals() {
        return globals;
    }
    
    // ==================== 示例代码 ====================
    
    /**
     * 内嵌的示例Lua脚本
     * 包含常用的游戏逻辑函数示例
     */
    public static final String EXAMPLE_LUA_SCRIPT = 
        "-- 示例Lua脚本\n" +
        "-- 这个脚本演示了如何在游戏中使用Lua\n" +
        "\n" +
        "-- 计算伤害的函数\n" +
        "function calculateDamage(baseDamage, strength, weaponBonus)\n" +
        "    local damage = baseDamage + (strength * 2) + weaponBonus\n" +
        "    local crit = randomFloat()\n" +
        "    if crit > 0.9 then\n" +
        "        damage = damage * 2\n" +
        "        log(\"Critical hit! Damage doubled!\")\n" +
        "    end\n" +
        "    return damage\n" +
        "end\n" +
        "\n" +
        "-- 判断是否应该攻击的函数\n" +
        "function shouldAttack(playerHealth, mobHealth, distance)\n" +
        "    if distance > 5 then\n" +
        "        return false  -- 太远，不攻击\n" +
        "    end\n" +
        "    if playerHealth < mobHealth * 0.5 then\n" +
        "        return true   -- 玩家血量低，攻击\n" +
        "    end\n" +
        "    return randomFloat() > 0.3  -- 30%概率攻击\n" +
        "end\n" +
        "\n" +
        "-- 简单的随机事件函数\n" +
        "function randomEvent()\n" +
        "    local roll = random(1, 100)\n" +
        "    if roll <= 10 then\n" +
        "        return \"rare\"\n" +
        "    elseif roll <= 30 then\n" +
        "        return \"uncommon\"\n" +
        "    else\n" +
        "        return \"common\"\n" +
        "    end\n" +
        "end\n" +
        "\n" +
        "-- 主函数（脚本加载时执行）\n" +
        "log(\"Example Lua script loaded successfully!\")\n";
    
    /**
     * 示例1：执行简单的Lua脚本字符串
     */
    public static void example1_SimpleScript() {
        LuaScriptManager lua = LuaScriptManager.getInstance();
        
        // 执行简单的计算
        String script = "return 10 + 20 * 2";
        LuaValue result = lua.loadString("simple_calc", script);
        int value = result.toint();
        GLog.i("计算结果: " + value); // 输出: 50
    }
    
    /**
     * 示例2：从内嵌脚本加载并调用Lua函数
     */
    public static void example2_LoadAndCallFunction() {
        LuaScriptManager lua = LuaScriptManager.getInstance();
        
        // 加载内嵌脚本
        lua.loadString("example_script", EXAMPLE_LUA_SCRIPT);
        
        // 调用calculateDamage函数
        LuaValue damage = lua.callFunction("example_script", "calculateDamage",
            LuaValue.valueOf(10),  // baseDamage
            LuaValue.valueOf(5),   // strength
            LuaValue.valueOf(3)    // weaponBonus
        );
        
        if (!damage.isnil()) {
            int finalDamage = damage.toint();
            GLog.i("计算出的伤害: " + finalDamage);
        }
    }
    
    /**
     * 示例3：使用全局变量
     */
    public static void example3_GlobalVariables() {
        LuaScriptManager lua = LuaScriptManager.getInstance();
        
        // 设置全局变量
        lua.setGlobal("playerLevel", 15);
        lua.setGlobal("playerName", "Hero");
        lua.setGlobal("playerHealth", 100);
        
        // 在Lua脚本中使用这些变量
        String script = 
            "local message = playerName .. ' (Level ' .. playerLevel .. ') has ' .. playerHealth .. ' HP'\n" +
            "log(message)\n" +
            "return message";
        
        LuaValue result = lua.loadString("player_info", script);
        String output = result.tojstring();
        GLog.i("Lua输出: " + output);
    }
    
    /**
     * 示例4：调用AI决策函数
     */
    public static void example4_AIDecision() {
        LuaScriptManager lua = LuaScriptManager.getInstance();
        
        // 加载内嵌脚本
        lua.loadString("example_script", EXAMPLE_LUA_SCRIPT);
        
        // 调用shouldAttack函数
        LuaValue shouldAttack = lua.callFunction("example_script", "shouldAttack",
            LuaValue.valueOf(50),  // playerHealth
            LuaValue.valueOf(100), // mobHealth
            LuaValue.valueOf(3)     // distance
        );
        
        if (!shouldAttack.isnil() && shouldAttack.toboolean()) {
            GLog.i("AI决定：攻击玩家");
        } else {
            GLog.i("AI决定：不攻击");
        }
    }
    
    /**
     * 示例5：随机事件生成
     */
    public static void example5_RandomEvent() {
        LuaScriptManager lua = LuaScriptManager.getInstance();
        
        // 加载内嵌脚本
        lua.loadString("example_script", EXAMPLE_LUA_SCRIPT);
        
        // 调用randomEvent函数
        LuaValue event = lua.callFunction("example_script", "randomEvent");
        
        if (!event.isnil()) {
            String eventType = event.tojstring();
            GLog.i("随机事件类型: " + eventType);
        }
    }
    
    /**
     * 示例6：动态执行多次计算
     */
    public static void example6_MultipleCalculations() {
        LuaScriptManager lua = LuaScriptManager.getInstance();
        
        // 加载内嵌脚本
        lua.loadString("example_script", EXAMPLE_LUA_SCRIPT);
        
        // 执行多次伤害计算
        for (int i = 0; i < 5; i++) {
            LuaValue damage = lua.callFunction("example_script", "calculateDamage",
                LuaValue.valueOf(20),
                LuaValue.valueOf(10),
                LuaValue.valueOf(5)
            );
            
            if (!damage.isnil()) {
                GLog.i("第" + (i + 1) + "次伤害计算: " + damage.toint());
            }
        }
    }
}

