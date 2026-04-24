package com.zootdungeon.codegen;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * 把 {@link #getAssetsRoot() assetsRoot} 下每个 {@code *.json} 解析成一个
 * {@code MobSprite} 子类源文件，写入 {@link #getOutputDirectory()}。
 *
 * <p>典型 JSON 形态见 {@code core/src/main/assets/cola/mob_sprites/sample_mob.json}。
 * 字段说明：</p>
 * <ul>
 *     <li>{@code className}：生成类名，例如 {@code SampleMobSprite}。</li>
 *     <li>{@code textureLabel}：{@code SpriteRegistry} 里的 label，运行时优先查它；
 *         {@code null} 或空字符串表示永远走 fallback。</li>
 *     <li>{@code fallbackTexture}：label 未注册时用的资源路径，比如
 *         {@code sprites/rat.png}。</li>
 *     <li>{@code frameWidth} / {@code frameHeight}：单帧像素尺寸。</li>
 *     <li>{@code initialAnimation}：构造完后 {@code play(...)} 的动画，默认
 *         {@code idle}；缺省但 JSON 有 {@code idle} 时自动使用它。</li>
 *     <li>{@code animations}：{@code key -> {fps, loop, frames[]}}。只支持
 *         {@code CharSprite} 里已有的字段：{@code idle / run / attack / operate
 *         / zap / die}；遇到未知 key 会报错，避免静默丢失动画。</li>
 * </ul>
 */
public abstract class GenerateAtlasSourcesTask extends DefaultTask {

    /** 允许写入动画的字段名（与 {@code CharSprite} 保持一致）。 */
    private static final List<String> SUPPORTED_ANIMATIONS = List.of(
            "idle", "run", "attack", "operate", "zap", "die");

    @InputDirectory
    public abstract DirectoryProperty getAssetsRoot();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @Input
    @Optional
    public abstract Property<String> getGeneratedPackage();

    @TaskAction
    public void run() throws IOException {
        Logger log = getLogger();
        Path root = getAssetsRoot().get().getAsFile().toPath();
        Path outRoot = getOutputDirectory().get().getAsFile().toPath();
        String pkg = getGeneratedPackage().getOrElse("com.zootdungeon.generated.sprites");

        if (!Files.isDirectory(root)) {
            log.warn("[atlas-codegen] assetsRoot does not exist or is not a directory: {}", root);
            cleanOutputDir(outRoot);
            return;
        }

        // 先清空输出目录，让删除 JSON 后对应的 .java 也消失。
        cleanOutputDir(outRoot);

        Path pkgDir = outRoot.resolve(pkg.replace('.', '/'));
        Files.createDirectories(pkgDir);

        List<Path> jsonFiles = collectJsonFiles(root);
        if (jsonFiles.isEmpty()) {
            log.lifecycle("[atlas-codegen] no *.json found under {}", root);
            return;
        }

        log.lifecycle("[atlas-codegen] generating {} sprite class(es) into {}",
                jsonFiles.size(), pkgDir);

        Gson gson = new GsonBuilder().setLenient().create();
        for (Path file : jsonFiles) {
            generateFromJson(file, root, pkgDir, pkg, gson, log);
        }
    }

    private static List<Path> collectJsonFiles(Path root) throws IOException {
        List<Path> out = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".json"))
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(out::add);
        }
        return out;
    }

    /** 如果目录存在就把里面的内容清掉（目录本身保留，方便下一步写入）。 */
    private static void cleanOutputDir(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder())
                    .filter(p -> !p.equals(dir))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }
    }

    private static void generateFromJson(
            Path jsonFile, Path root, Path pkgDir, String pkg, Gson gson, Logger log)
            throws IOException {

        SpriteDef def;
        try (Reader reader = Files.newBufferedReader(jsonFile, StandardCharsets.UTF_8)) {
            def = gson.fromJson(reader, SpriteDef.class);
        } catch (RuntimeException e) {
            throw new GradleException(
                    "Failed to parse " + jsonFile + ": " + e.getMessage(), e);
        }

        if (def == null) {
            throw new GradleException("Empty JSON: " + jsonFile);
        }
        validate(def, jsonFile);

        Path outFile = pkgDir.resolve(def.className + ".java");
        Path relativeJson = root.relativize(jsonFile);
        String source = renderJava(def, pkg, relativeJson);
        Files.writeString(outFile, source, StandardCharsets.UTF_8);

        log.lifecycle("[atlas-codegen]   {} -> {}.{}",
                relativeJson, pkg, def.className);
    }

    private static void validate(SpriteDef def, Path jsonFile) {
        if (def.className == null || def.className.isBlank()) {
            throw new GradleException("Missing className in " + jsonFile);
        }
        if (!def.className.matches("[A-Z][A-Za-z0-9_]*")) {
            throw new GradleException(
                    "Invalid className '" + def.className
                            + "' in " + jsonFile + " (must be a valid Java identifier "
                            + "starting with an uppercase letter)");
        }
        if (def.frameWidth <= 0 || def.frameHeight <= 0) {
            throw new GradleException(
                    "frameWidth/frameHeight must be positive in " + jsonFile);
        }
        if (def.animations == null || def.animations.isEmpty()) {
            throw new GradleException("animations map is empty in " + jsonFile);
        }
        for (Map.Entry<String, AnimationDef> e : def.animations.entrySet()) {
            String name = e.getKey();
            if (!SUPPORTED_ANIMATIONS.contains(name)) {
                throw new GradleException(
                        "Unsupported animation '" + name + "' in " + jsonFile
                                + " (supported: " + SUPPORTED_ANIMATIONS + ")");
            }
            AnimationDef anim = e.getValue();
            if (anim == null) {
                throw new GradleException("Animation '" + name + "' is null in " + jsonFile);
            }
            if (anim.fps <= 0) {
                throw new GradleException(
                        "Animation '" + name + "' must have fps > 0 in " + jsonFile);
            }
            if (anim.frames == null || anim.frames.length == 0) {
                throw new GradleException(
                        "Animation '" + name + "' must have frames in " + jsonFile);
            }
        }
        if (def.initialAnimation != null && !def.initialAnimation.isBlank()
                && !def.animations.containsKey(def.initialAnimation)) {
            throw new GradleException(
                    "initialAnimation '" + def.initialAnimation
                            + "' is not defined in animations map in " + jsonFile);
        }
    }

    private static String renderJava(SpriteDef def, String pkg, Path sourceRel) {
        StringBuilder sb = new StringBuilder(1024);
        String timestamp = OffsetDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        // 把 Windows 的反斜杠规格化成正斜杠，避免生成文件里出现转义问题。
        String sourcePosix = sourceRel.toString().replace('\\', '/');

        sb.append("// AUTO-GENERATED by atlas-codegen. DO NOT EDIT.\n");
        sb.append("// Source : ").append(sourcePosix).append('\n');
        sb.append("// Created: ").append(timestamp).append('\n');
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import com.watabou.noosa.TextureFilm;\n");
        sb.append("import com.zootdungeon.sprites.MobSprite;\n\n");
        sb.append("public final class ").append(def.className)
                .append(" extends MobSprite {\n\n");
        sb.append("    public ").append(def.className).append("() {\n");
        sb.append("        super();\n\n");

        sb.append("        TextureFilm frames = textureWithFallback(")
                .append(quote(def.textureLabel)).append(", ")
                .append(quote(def.fallbackTexture)).append(", ")
                .append(def.frameWidth).append(", ")
                .append(def.frameHeight).append(");\n\n");

        // 按固定顺序输出，保证生成结果稳定（对 diff / version control 友好）。
        for (String name : SUPPORTED_ANIMATIONS) {
            AnimationDef anim = def.animations.get(name);
            if (anim == null) {
                continue;
            }
            sb.append("        ").append(name).append(" = new Animation(")
                    .append(anim.fps).append(", ").append(anim.loop).append(");\n");
            sb.append("        ").append(name).append(".frames(frames");
            for (int f : anim.frames) {
                sb.append(", ").append(f);
            }
            sb.append(");\n\n");
        }

        String initial = def.initialAnimation;
        if (initial == null || initial.isBlank()) {
            initial = def.animations.containsKey("idle") ? "idle" : null;
        }
        if (initial != null) {
            sb.append("        play(").append(initial).append(");\n");
        }
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    /** 把任意字符串转换成 Java 字面量（包含 null 的特殊处理）。 */
    private static String quote(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder out = new StringBuilder(s.length() + 2);
        out.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': out.append("\\\\"); break;
                case '"':  out.append("\\\""); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        out.append('"');
        return out.toString();
    }

    // ==== JSON POJOs（Gson 按字段名直接反序列化） ====

    /** JSON 顶层结构。 */
    static final class SpriteDef {
        String className;
        String textureLabel;
        String fallbackTexture;
        int frameWidth;
        int frameHeight;
        String initialAnimation;
        // LinkedHashMap 保持 JSON 声明顺序，虽然我们实际输出是按 SUPPORTED_ANIMATIONS。
        LinkedHashMap<String, AnimationDef> animations;
    }

    /** 单条动画定义。 */
    static final class AnimationDef {
        int fps;
        boolean loop;
        int[] frames;
    }
}
