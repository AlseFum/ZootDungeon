package com.watabou.utils;

import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight JSON-backed Preferences implementation.
 */
public class JsonPreferences implements Preferences {

	private final FileHandle gdxFile;
	private final File jvmFile;
	private final HashMap<String, Object> values = new HashMap<>();

	public JsonPreferences(FileHandle file) {
		this.gdxFile = file;
		this.jvmFile = null;
		load();
	}

	public JsonPreferences(File file) {
		this.gdxFile = null;
		this.jvmFile = file;
		load();
	}

	private void load() {
		values.clear();
		try {
			String content = readContent();
			if (content == null || content.isEmpty()) return;
			JsonValue root = new JsonReader().parse(content);
			if (root == null || !root.isObject()) return;
			for (JsonValue child = root.child; child != null; child = child.next) {
				if (child.isBoolean()) {
					values.put(child.name, child.asBoolean());
				} else if (child.isLong()) {
					values.put(child.name, child.asLong());
				} else if (child.isDouble()) {
					values.put(child.name, (float) child.asDouble());
				} else if (child.isString()) {
					values.put(child.name, child.asString());
				}
			}
		} catch (Exception ignored) {
			// If malformed, fallback to defaults in callers.
		}
	}

	private String readContent() {
		try {
			if (gdxFile != null) {
				if (!gdxFile.exists() || gdxFile.isDirectory() || gdxFile.length() <= 0) return "";
				return gdxFile.readString("UTF-8");
			} else if (jvmFile != null) {
				if (!jvmFile.exists() || jvmFile.isDirectory() || jvmFile.length() <= 0) return "";
				byte[] bytes = java.nio.file.Files.readAllBytes(jvmFile.toPath());
				return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
			}
		} catch (Exception ignored) {}
		return "";
	}

	@Override
	public Preferences putBoolean(String key, boolean val) {
		values.put(key, val);
		return this;
	}

	@Override
	public Preferences putInteger(String key, int val) {
		values.put(key, val);
		return this;
	}

	@Override
	public Preferences putLong(String key, long val) {
		values.put(key, val);
		return this;
	}

	@Override
	public Preferences putFloat(String key, float val) {
		values.put(key, val);
		return this;
	}

	@Override
	public Preferences putString(String key, String val) {
		values.put(key, val);
		return this;
	}

	@Override
	public Preferences put(Map<String, ?> vals) {
		values.putAll(vals);
		return this;
	}

	@Override
	public boolean getBoolean(String key) {
		return getBoolean(key, false);
	}

	@Override
	public int getInteger(String key) {
		return getInteger(key, 0);
	}

	@Override
	public long getLong(String key) {
		return getLong(key, 0L);
	}

	@Override
	public float getFloat(String key) {
		return getFloat(key, 0f);
	}

	@Override
	public String getString(String key) {
		return getString(key, "");
	}

	@Override
	public boolean getBoolean(String key, boolean defValue) {
		Object v = values.get(key);
		return v instanceof Boolean ? (Boolean) v : defValue;
	}

	@Override
	public int getInteger(String key, int defValue) {
		Object v = values.get(key);
		return v instanceof Number ? ((Number) v).intValue() : defValue;
	}

	@Override
	public long getLong(String key, long defValue) {
		Object v = values.get(key);
		return v instanceof Number ? ((Number) v).longValue() : defValue;
	}

	@Override
	public float getFloat(String key, float defValue) {
		Object v = values.get(key);
		return v instanceof Number ? ((Number) v).floatValue() : defValue;
	}

	@Override
	public String getString(String key, String defValue) {
		Object v = values.get(key);
		return v instanceof String ? (String) v : defValue;
	}

	@Override
	public Map<String, ?> get() {
		return Collections.unmodifiableMap(values);
	}

	@Override
	public boolean contains(String key) {
		return values.containsKey(key);
	}

	@Override
	public void clear() {
		values.clear();
	}

	@Override
	public void remove(String key) {
		values.remove(key);
	}

	@Override
	public void flush() {
		try {
			Json json = new Json(JsonWriter.OutputType.json);
			String out = json.toJson(values);
			if (gdxFile != null) {
				if (gdxFile.parent() != null) gdxFile.parent().mkdirs();
				gdxFile.writeString(out, false, "UTF-8");
			} else if (jvmFile != null) {
				File parent = jvmFile.getParentFile();
				if (parent != null && !parent.exists()) parent.mkdirs();
				java.nio.file.Files.write(jvmFile.toPath(), out.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			}
		} catch (Exception ignored) {
			// Keep behavior permissive like default preferences on IO issues.
		}
	}
}

