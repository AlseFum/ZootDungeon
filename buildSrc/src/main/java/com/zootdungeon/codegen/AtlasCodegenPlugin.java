package com.zootdungeon.codegen;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;

/**
 * 入口插件：
 * <ol>
 *     <li>给目标工程注册 {@code atlasCodegen} 扩展块；</li>
 *     <li>注册 {@code generateAtlasSources} 任务；</li>
 *     <li>在 Java 插件存在时，把生成目录挂到 {@code main} source set 上，
 *         这样 {@code compileJava} 会自动依赖此任务。</li>
 * </ol>
 */
public class AtlasCodegenPlugin implements Plugin<Project> {

    public static final String EXTENSION_NAME = "atlasCodegen";
    public static final String TASK_NAME = "generateAtlasSources";
    public static final String TASK_GROUP = "codegen";

    @Override
    public void apply(Project project) {
        AtlasCodegenExtension ext = project.getExtensions().create(
                EXTENSION_NAME, AtlasCodegenExtension.class);

        TaskProvider<GenerateAtlasSourcesTask> taskProvider = project.getTasks().register(
                TASK_NAME, GenerateAtlasSourcesTask.class, task -> {
                    task.setGroup(TASK_GROUP);
                    task.setDescription(
                            "Parse mob-sprite JSON specs and generate MobSprite subclasses.");

                    task.getAssetsRoot().convention(ext.getAssetsRoot());
                    task.getOutputDirectory().convention(ext.getOutputDirectory());
                    task.getGeneratedPackage().convention(ext.getGeneratedPackage());
                });

        // 等 Java 插件就绪后再注入 source set；withType(...) 对应「现在有 / 将来 apply」
        // 两种情况都会触发，比直接 getPlugins().apply 再读安全。
        project.getPlugins().withType(JavaPlugin.class, plugin -> {
            JavaPluginExtension java = project.getExtensions().getByType(JavaPluginExtension.class);
            SourceSet main = java.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
            // 用 TaskProvider 喂 srcDir，Gradle 会自动把 compileJava 的 inputs
            // 挂到这个 task 的 outputs 上，不用再手写 dependsOn。
            main.getJava().srcDir(taskProvider.flatMap(GenerateAtlasSourcesTask::getOutputDirectory));
        });
    }
}
