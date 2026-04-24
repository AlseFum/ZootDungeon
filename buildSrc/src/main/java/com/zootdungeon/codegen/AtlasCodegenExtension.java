package com.zootdungeon.codegen;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;

/**
 * {@code atlasCodegen { ... }} DSL 块在 Gradle 中对应的扩展对象。
 *
 * <p>用 {@code abstract getter} 的方式声明，Gradle 会自动注入
 * {@link org.gradle.api.model.ObjectFactory}、给每个 property 生成实现，
 * 比手写 {@code @Inject} 构造器简洁。</p>
 *
 * <p>当前阶段任务只负责<b>读取 + 打印</b>，不会写文件，所以 {@link #getOutputDirectory()}
 * 仅作为 API 占位；{@link #getGeneratedPackage()} 也暂时不被 task 使用，
 * 保留下来是为了后续真正生成代码时避免再改 DSL。</p>
 */
public abstract class AtlasCodegenExtension {

    /** 要扫描的资源根目录，比如 {@code core/src/main/assets}。 */
    public abstract DirectoryProperty getAssetsRoot();

    /** 生成产物输出目录（当前阶段留空也可以）。 */
    public abstract DirectoryProperty getOutputDirectory();

    /** 未来生成类所属的包名；当前阶段只打印，不使用。 */
    public abstract Property<String> getGeneratedPackage();
}
