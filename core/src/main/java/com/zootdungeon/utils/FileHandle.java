package com.zootdungeon.utils;

import com.zootdungeon.Assets;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Static utility for default-directory (FileUtils base path) file operations.
 * Each sypnosis.json under the default directory is a credential for resourceIndex.
 * Uses {@link com.badlogic.gdx.files.FileHandle} via FileUtils.
 */
public final class FileHandle {

	private FileHandle() {}


	private static final String TARGET_FILE = "sypnosis.json";

	public static List<Assets.ResourceIndex> scanResourceIndex() {
		List<Assets.ResourceIndex> out = new ArrayList<>();
		collectAndLoadResourceIndex("", out);
		if (out.isEmpty()) {
			System.out.println("[resourceIndex] (empty)");
		} else {
			for (Assets.ResourceIndex index : out) {
				System.out.println("[resourceIndex]");
				System.out.println("    langResources=" + index.langResources);
				System.out.println("    textureResources=" + index.textureResources);
				System.out.println("    soundResources=" + index.soundResources);
				System.out.println("    scriptResources=" + index.scriptResources);
			}
		}
		return out;
	}

	private static void collectAndLoadResourceIndex(String basePath, List<Assets.ResourceIndex> out) {
		com.badlogic.gdx.files.FileHandle dir = FileUtils.getFileHandle(basePath);
		if (dir == null || !dir.isDirectory()) return;
		com.badlogic.gdx.files.FileHandle[] list = dir.list();
		if (list == null) return;
		for (com.badlogic.gdx.files.FileHandle f : list) {
			String name = f.name();
			String relativePath = basePath.isEmpty() ? name : basePath + name;
			if (f.isDirectory()) {
				collectAndLoadResourceIndex(relativePath + "/", out);
			} else if (TARGET_FILE.equals(name)) {
				if (f.exists() && !f.isDirectory() && f.length() > 0) {
					try {
						// 显式按 UTF-8 读取，避免平台默认编码导致中文路径变成锟斤拷
						String json = f.readString("UTF-8");
						Bundle bundle = Bundle.read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
						if (bundle == null || !bundle.contains("id") || bundle.getString("id") == null || bundle.getString("id").isEmpty()) continue;
						Assets.ResourceIndex index = new Assets.ResourceIndex();
						index.restoreFromBundle(bundle);
						String bundleName = bundle.getString("name");
						String bundleId = bundle.getString("id");
						index.displayName = (bundleName != null && !bundleName.isEmpty()) ? bundleName : (bundleId != null ? bundleId : null);
						// 将索引中的路径替换为 FileUtils 可直接读取的 external 路径（sypnosis 所在目录 + 原路径）
						resolveIndexPaths(index, basePath);
						out.add(index);
					} catch (IOException | RuntimeException ignored) {
						// skip invalid or unreadable file
					}
				}
			}
		}
	}

	/**
	 * 将 ResourceIndex 中各 map 的路径值替换为 FileUtils 可直接使用的路径。
	 * 约定：sypnosis.json 里填的路径为「相对本 sypnosis.json 所在目录」，此处会拼上 basePath；
	 * 若值已以 basePath 开头则不再拼接，避免重复。
	 */
	private static void resolveIndexPaths(Assets.ResourceIndex index, String basePath) {
		String prefix = basePath == null || basePath.isEmpty() ? "" : (basePath.endsWith("/") ? basePath : basePath + "/");
		for (Assets.ResourceType type : Assets.ResourceType.values()) {
			Map<String, String> map = index.getResourcesByType(type);
			for (String id : new ArrayList<>(map.keySet())) {
				String rel = map.get(id);
				if (rel == null || rel.isEmpty()) continue;
				// 已是「basePath + 相对」形式则不再拼接
				String resolved = (prefix.isEmpty() || rel.startsWith(prefix)) ? rel : (prefix + rel);
				map.put(id, resolved);
			}
		}
	}
}
