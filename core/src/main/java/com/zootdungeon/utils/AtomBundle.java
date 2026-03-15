package com.zootdungeon.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 类似 Bundle，但只存储整数和 atom（字符串标识符）。
 * 与 S 表达式风格字符串互相转换，格式示例：
 * <pre>
 * (word1 word2 42 key3:word4 key4( word5 word6 key4:(...)))
 * </pre>
 * - 位置元素：atom、整数
 * - 键值对：key:atom 或 key:42
 * - 嵌套：key( ... ) 其中 ... 可包含更多元素
 */
public class AtomBundle {

    private static final Pattern INT_PATTERN = Pattern.compile("^-?\\d+$");

    final List<Object> elements = new ArrayList<>();

    public AtomBundle() {
    }

    // --- 写入 ---

    public void put(String atom) {
        if (atom == null) throw new IllegalArgumentException("atom cannot be null");
        elements.add(atom);
    }

    public void put(int value) {
        elements.add(value);
    }

    public void put(String key, String atom) {
        if (key == null || atom == null) throw new IllegalArgumentException("key and atom cannot be null");
        elements.add(new KeyValue(key, atom));
    }

    public void put(String key, int value) {
        if (key == null) throw new IllegalArgumentException("key cannot be null");
        elements.add(new KeyValue(key, value));
    }

    public void put(String key, AtomBundle nested) {
        if (key == null || nested == null) throw new IllegalArgumentException("key and nested cannot be null");
        elements.add(new KeyValue(key, nested));
    }

    // --- 按位置读取 ---

    public int size() {
        return elements.size();
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    public Object get(int index) {
        if (index < 0 || index >= elements.size()) return null;
        Object o = elements.get(index);
        return o instanceof KeyValue ? null : o;
    }

    public String getAtom(int index) {
        Object o = get(index);
        return o instanceof String ? (String) o : null;
    }

    public int getInt(int index) {
        Object o = get(index);
        return o instanceof Integer ? (Integer) o : 0;
    }

    public int getInt(int index, int defaultValue) {
        Object o = get(index);
        return o instanceof Integer ? (Integer) o : defaultValue;
    }

    public AtomBundle getBundle(int index) {
        if (index < 0 || index >= elements.size()) return null;
        Object o = elements.get(index);
        return o instanceof AtomBundle ? (AtomBundle) o : null;
    }

    // --- 按键读取 ---

    public boolean contains(String key) {
        for (Object o : elements) {
            if (o instanceof KeyValue && ((KeyValue) o).key.equals(key))
                return true;
        }
        return false;
    }

    public String getAtom(String key) {
        Object v = getValue(key);
        return v instanceof String ? (String) v : null;
    }

    public String getAtom(String key, String defaultValue) {
        String a = getAtom(key);
        return a != null ? a : defaultValue;
    }

    public int getInt(String key) {
        Object v = getValue(key);
        return v instanceof Integer ? (Integer) v : 0;
    }

    public int getInt(String key, int defaultValue) {
        Object v = getValue(key);
        return v instanceof Integer ? (Integer) v : defaultValue;
    }

    public AtomBundle getBundle(String key) {
        Object v = getValue(key);
        return v instanceof AtomBundle ? (AtomBundle) v : null;
    }

    private Object getValue(String key) {
        for (Object o : elements) {
            if (o instanceof KeyValue) {
                KeyValue kv = (KeyValue) o;
                if (kv.key.equals(key)) return kv.value;
            }
        }
        return null;
    }

    // --- 迭代位置元素（跳过键值对）---

    public Iterator<Object> iterator() {
        return elements.stream()
                .filter(o -> !(o instanceof KeyValue))
                .iterator();
    }

    // --- 序列化 ---

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        appendTo(sb);
        return sb.toString();
    }

    private void appendTo(StringBuilder sb) {
        sb.append('(');
        for (int i = 0; i < elements.size(); i++) {
            if (i > 0) sb.append(' ');
            Object o = elements.get(i);
            if (o instanceof KeyValue) {
                KeyValue kv = (KeyValue) o;
                sb.append(kv.key);
                if (kv.value instanceof AtomBundle) {
                    sb.append('(');
                    ((AtomBundle) kv.value).appendElementsTo(sb);
                    sb.append(')');
                } else {
                    sb.append(':').append(kv.value);
                }
            } else if (o instanceof String) {
                sb.append((String) o);
            } else if (o instanceof Integer) {
                sb.append(o);
            }
        }
        sb.append(')');
    }

    private void appendElementsTo(StringBuilder sb) {
        for (int i = 0; i < elements.size(); i++) {
            if (i > 0) sb.append(' ');
            Object o = elements.get(i);
            if (o instanceof KeyValue) {
                KeyValue kv = (KeyValue) o;
                sb.append(kv.key);
                if (kv.value instanceof AtomBundle) {
                    sb.append('(');
                    ((AtomBundle) kv.value).appendElementsTo(sb);
                    sb.append(')');
                } else {
                    sb.append(':').append(kv.value);
                }
            } else if (o instanceof String) {
                sb.append((String) o);
            } else if (o instanceof Integer) {
                sb.append(o);
            }
        }
    }

    // --- 反序列化 ---

    public static AtomBundle fromString(String s) {
        if (s == null || s.isBlank()) return new AtomBundle();
        s = s.trim();
        if (!s.startsWith("(") || !s.endsWith(")")) {
            throw new IllegalArgumentException("Expected string in format ( ... )");
        }
        ParseContext ctx = new ParseContext(s, 1, s.length() - 1);
        return parseList(ctx);
    }

    private static AtomBundle parseList(ParseContext ctx) {
        AtomBundle bundle = new AtomBundle();
        while (ctx.pos < ctx.end) {
            skipSpaces(ctx);
            if (ctx.pos >= ctx.end) break;

            char c = ctx.s.charAt(ctx.pos);
            if (c == ')') {
                ctx.pos++;
                break;
            }

            if (c == '(') {
                ctx.pos++;
                AtomBundle nested = parseList(ctx);
                if (ctx.pos < ctx.end && ctx.s.charAt(ctx.pos) == ')') ctx.pos++;
                bundle.elements.add(nested);
                continue;
            }

            StringBuilder tok = readToken(ctx);
            String token = tok.toString();
            if (token.isEmpty()) continue;

            int colon = token.indexOf(':');
            if (colon > 0) {
                String key = token.substring(0, colon);
                String val = colon + 1 < token.length() ? token.substring(colon + 1) : "";
                if (val.equals("(") || (val.isEmpty() && ctx.pos < ctx.end && ctx.s.charAt(ctx.pos) == '(')) {
                    if (val.isEmpty()) ctx.pos++;
                    AtomBundle nested = parseList(ctx);
                    if (ctx.pos < ctx.end && ctx.s.charAt(ctx.pos) == ')') ctx.pos++;
                    bundle.elements.add(new KeyValue(key, nested));
                } else if (!val.isEmpty()) {
                    bundle.elements.add(new KeyValue(key, parseScalar(val)));
                }
                continue;
            }

            if (ctx.pos < ctx.end && ctx.s.charAt(ctx.pos) == '(') {
                ctx.pos++;
                AtomBundle nested = parseList(ctx);
                if (ctx.pos < ctx.end && ctx.s.charAt(ctx.pos) == ')') ctx.pos++;
                bundle.elements.add(new KeyValue(token, nested));
                continue;
            }

            bundle.elements.add(parseScalar(token));
        }
        return bundle;
    }

    private static Object parseScalar(String token) {
        if (INT_PATTERN.matcher(token).matches()) {
            return Integer.parseInt(token);
        }
        return token;
    }

    private static void skipSpaces(ParseContext ctx) {
        while (ctx.pos < ctx.end && Character.isWhitespace(ctx.s.charAt(ctx.pos))) {
            ctx.pos++;
        }
    }


    private static StringBuilder readToken(ParseContext ctx) {
        StringBuilder sb = new StringBuilder();
        while (ctx.pos < ctx.end) {
            char c = ctx.s.charAt(ctx.pos);
            if (Character.isWhitespace(c) || c == '(' || c == ')') break;
            sb.append(c);
            ctx.pos++;
        }
        return sb;
    }

    private static class ParseContext {
        final String s;
        int pos;
        final int end;

        ParseContext(String s, int start, int end) {
            this.s = s;
            this.pos = start;
            this.end = end;
        }
    }

    private static class KeyValue {
        final String key;
        final Object value;

        KeyValue(String key, Object value) {
            this.key = key;
            this.value = value;
        }
    }
}
