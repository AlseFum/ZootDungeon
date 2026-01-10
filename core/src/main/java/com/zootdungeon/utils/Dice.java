package com.zootdungeon.utils;

import java.util.*;
import java.util.stream.Collectors;

public class Dice {
    public static class Die {
        public int amount;
        public int sides;
        // 以下表示对单个Die的特殊处理，为-1即是不启用
        public int maxOf = -1;        // 只使用最大的n个
        public int minOf = -1;        // 只使用最小的n个
        public int expandMorethan = -1; // 大于N时追加骰

        public Die(int amount, int sides) {
            this.amount = amount;
            this.sides = sides;
        }
        public static Die of(int amount, int sides) {
            Die die=new Die(amount,sides);
            return die;
        }

        public Die withMaxOf(int maxOf) {
            this.maxOf = maxOf;
            return this;
        }

        public Die withMinOf(int minOf) {
            this.minOf = minOf;
            return this;
        }

        public Die withExpandMorethan(int threshold) {
            this.expandMorethan = threshold;
            return this;
        }
    }

    private List<Object> components; // 可以包含Die或Integer（支持负数）

    public Dice() {
        this.components = new ArrayList<>();
    }

    public Dice(List<Object> components) {
        this.components = new ArrayList<>(components);
    }

    public static Dice of(Object... components) {
        Dice dice = new Dice();
        for (Object comp : components) {
            if (comp instanceof Die || comp instanceof Integer) {
                dice.components.add(comp);
            } else {
                throw new IllegalArgumentException("Only Die or Integer!");
            }
        }
        return dice;
    }

    /**
     * 向当前骰组添加一个骰子组件（如 2d6）。
     */
    public Dice addDie(int amount, int sides) {
        this.components.add(new Die(amount, sides));
        return this;
    }

    /**
     * 向当前骰组添加一个常数（可以为负数）。
     */
    public Dice addConstant(int value) {
        this.components.add(value);
        return this;
    }

    /**
     * 将另一个 Dice 的所有组件合并进当前骰组。
     */
    public Dice addAll(Dice other) {
        if (other != null && other.components != null && !other.components.isEmpty()) {
            this.components.addAll(other.components);
        }
        return this;
    }

    public String describe() {
        if (components.isEmpty()) {
            return "0";
        }

        List<String> parts = new ArrayList<>();
        for (Object comp : components) {
            if (comp instanceof Integer) {
                int value = (Integer) comp;
                // 处理负数显示
                if (value < 0) {
                    parts.add("- " + Math.abs(value));
                } else {
                    parts.add("+ " + value);
                }
            } else if (comp instanceof Die) {
                Die die = (Die) comp;
                StringBuilder sb = new StringBuilder();

                // 处理负数数量的骰子
                if (die.amount < 0) {
                    sb.append("- ");
                } else {
                    sb.append("+ ");
                }

                sb.append(Math.abs(die.amount)).append("d").append(Math.abs(die.sides));

                if (die.maxOf > 0) sb.append("k").append(die.maxOf);
                if (die.minOf > 0) sb.append("l").append(die.minOf);
                if (die.expandMorethan > 0) sb.append("e").append(die.expandMorethan);

                parts.add(sb.toString());
            }
        }

        String result = String.join(" ", parts);
        // 移除开头的"+ "
        if (result.startsWith("+ ")) {
            result = result.substring(2);
        }
        return result;
    }

    public RollResult roll(Random random) {
        List<Map.Entry<Object, int[]>> results = new ArrayList<>();

        for (Object comp : components) {
            if (comp instanceof Integer) {
                // 固定值（支持负数）
                int fixedValue = (Integer) comp;
                results.add(new AbstractMap.SimpleEntry<>(comp, new int[]{fixedValue}));
            } else if (comp instanceof Die) {
                // 骰子（支持负数）
                Die die = (Die) comp;
                int[] rolls = rollDie(die, random);
                results.add(new AbstractMap.SimpleEntry<>(comp, rolls));
            }
        }

        return new RollResult(this, results);
    }

    /**
     * 使用游戏全局 RNG（com.watabou.utils.Random）掷一次骰并返回总和。
     * 这避免了在游戏逻辑中再引入新的 java.util.Random 实例。
     */
    public int rollTotalGame() {
        // 适配器：仅重写 nextInt，使其走 com.watabou.utils.Random
        Random adapter = new Random() {
            @Override
            public int nextInt(int bound) {
                // 使用 Shattered/Cola Dungeon 的全局 RNG
                return com.watabou.utils.Random.Int(bound);
            }
        };
        RollResult res = roll(adapter);
        return res.getTotal();
    }

    private int[] rollDie(Die die, Random random) {
        List<Integer> allRolls = new ArrayList<>();
        int absAmount = Math.abs(die.amount);
        int absSides = Math.abs(die.sides);
        boolean isNegative = (die.amount < 0) ^ (die.sides < 0); // 异或判断最终是否为负数

        // 初始掷骰
        for (int i = 0; i < absAmount; i++) {
            int roll = random.nextInt(absSides) + 1;
            allRolls.add(isNegative ? -roll : roll);
        }

        // 处理追加骰（只对正数面数的骰子有效）
        if (die.expandMorethan > 0 && die.sides > 0) {
            boolean hasExpanded;
            do {
                hasExpanded = false;
                List<Integer> newRolls = new ArrayList<>();

                for (int roll : allRolls) {
                    if (roll >= die.expandMorethan) {
                        int extraRoll = random.nextInt(die.sides) + 1;
                        newRolls.add(isNegative ? -extraRoll : extraRoll);
                        hasExpanded = true;
                    }
                }
                allRolls.addAll(newRolls);
            } while (hasExpanded);
        }

        // 处理取最大/最小值（考虑负数）
        List<Integer> finalRolls = new ArrayList<>(allRolls);
        if (die.maxOf > 0 && finalRolls.size() > die.maxOf) {
            finalRolls.sort((a, b) -> Integer.compare(b, a)); // 降序排列
            finalRolls = finalRolls.subList(0, die.maxOf);
        } else if (die.minOf > 0 && finalRolls.size() > die.minOf) {
            finalRolls.sort(Integer::compareTo); // 升序排列
            finalRolls = finalRolls.subList(0, die.minOf);
        }

        return finalRolls.stream().mapToInt(i -> i).toArray();
    }

    public static class RollResult {
        private final Dice dice;
        private final List<Map.Entry<Object, int[]>> results;

        public RollResult(Dice dice, List<Map.Entry<Object, int[]>> results) {
            this.dice = dice;
            this.results = results;
        }

        public int getTotal() {
            int total = 0;
            for (Map.Entry<Object, int[]> entry : results) {
                for (int value : entry.getValue()) {
                    total += value;
                }
            }
            return total;
        }

        public String describe() {
            if (results.isEmpty()) {
                return "0 = 0";
            }

            List<String> parts = new ArrayList<>();
            for (Map.Entry<Object, int[]> entry : results) {
                if (entry.getKey() instanceof Integer) {
                    int value = entry.getValue()[0];
                    if (value < 0) {
                        parts.add("- " + Math.abs(value));
                    } else {
                        parts.add("+ " + value);
                    }
                } else if (entry.getKey() instanceof Die) {
                    Die die = (Die) entry.getKey();
                    String rollsStr = Arrays.stream(entry.getValue())
                            .mapToObj(val -> String.valueOf(val))
                            .collect(Collectors.joining(", "));

                    if (die.amount < 0) {
                        parts.add("- [" + rollsStr + "]");
                    } else {
                        parts.add("+ [" + rollsStr + "]");
                    }
                }
            }

            String expression = String.join(" ", parts);
            // 移除开头的"+ "
            if (expression.startsWith("+ ")) {
                expression = expression.substring(2);
            }
            return expression + " = " + getTotal();
        }

        public List<Map.Entry<Object, int[]>> getDetailedResults() {
            return new ArrayList<>(results);
        }
    }

    // 使用示例
    public static void main(String[] args) {
        Random random = new Random();

        // 正数骰子
        Dice positiveDice = Dice.of(
                new Die(2, 6).withMaxOf(1),
                new Die(1, 8),
                5
        );

        // 包含负数的复杂骰子组合
        Dice complexDice = Dice.of(
                new Die(2, 6).withMaxOf(1),
                new Die(-1, 4),  // 负数量的骰子
                -3,              // 负固定值
                new Die(1, -4)   // 负面数的骰子
        );

        System.out.println("正数骰子描述: " + positiveDice.describe());
        System.out.println("复杂骰子描述: " + complexDice.describe());

        RollResult result1 = positiveDice.roll(random);
        RollResult result2 = complexDice.roll(random);

        System.out.println("正数掷骰结果: " + result1.describe());
        System.out.println("复杂掷骰结果: " + result2.describe());
    }
}