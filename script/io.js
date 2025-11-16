import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
/**
 * 缓存管理类
 * 用于记录所有的插入操作，方便回退
 */
class Cache {
    constructor() {
        this.insertions = [];  // 记录所有的插入操作
        this.templates = {};   // 记录模板文件的映射关系
        this.timestamp = null;
    }

    // 记录插入操作
    recordInsertion(operation) {
        this.insertions.push({
            ...operation,
            timestamp: new Date().toISOString()
        });
    }

    // 记录模板映射
    recordTemplate(templateFile, generatedFile, identifier, config) {
        if (!this.templates[templateFile]) {
            this.templates[templateFile] = [];
        }
        this.templates[templateFile].push({
            generatedFile,
            identifier,
            config
        });
    }

    // 保存到文件
    save(cacheFile = 'script/cache.txt') {
        try {
            const data = {
                insertions: this.insertions,
                templates: this.templates,
                timestamp: new Date().toISOString()
            };
            fs.writeFileSync(cacheFile, JSON.stringify(data, null, 2), 'utf8');
            console.log(`✓ 缓存已保存到 ${cacheFile}`);
        } catch (error) {
            console.error(`✗ 保存缓存失败: ${error.message}`);
        }
    }

    // 从文件加载
    static load(cacheFile = 'script/cache.txt') {
        try {
            if (fs.existsSync(cacheFile)) {
                const data = JSON.parse(fs.readFileSync(cacheFile, 'utf8'));
                const cache = new Cache();
                cache.insertions = data.insertions || [];
                cache.templates = data.templates || {};
                cache.timestamp = data.timestamp;
                return cache;
            }
        } catch (error) {
            console.warn(`⚠ 加载缓存失败: ${error.message}`);
        }
        return new Cache();
    }
}

/**
 * 在文件中插入内容（行内插入）
 * 使用 //@@identifier 标记插入位置
 * 插入后会添加 //@@identifier+++ 和 //@@identifier--- 标记
 */
async function insertContent(filePath, identifier, content) {
    try {
        let fileContent = await fs.promises.readFile(filePath, 'utf8');
        const lines = fileContent.split('\n');
        
        // 查找标记点
        let markerIndex = -1;
        for (let i = 0; i < lines.length; i++) {
            if (lines[i].includes(`//@@${identifier}`) && 
                !lines[i].includes('+++') && 
                !lines[i].includes('---')) {
                markerIndex = i;
                break;
            }
        }

        if (markerIndex === -1) {
            console.warn(`✗ 未找到标记: //@@${identifier} 在 ${filePath}`);
            return false;
        }

        // 检查是否已经插入过内容
        if (markerIndex + 1 < lines.length && lines[markerIndex + 1].includes(`//@@${identifier}+++`)) {
            console.warn(`✗ 内容已经插入过: //@@${identifier} 在 ${filePath}`);
            return false;
        }

        // 插入内容
        const contentLines = content.split('\n');
        lines.splice(
            markerIndex + 1,
            0,
            `//@@${identifier}+++`,
            ...contentLines,
            `//@@${identifier}---`
        );

        await fs.promises.writeFile(filePath, lines.join('\n'), 'utf8');
        console.log(`✓ 已插入内容: //@@${identifier} 在 ${filePath}`);
        return true;
    } catch (error) {
        console.error(`✗ 插入内容失败: ${error.message}`);
        return false;
    }
}

/**
 * 删除整个生成的文件
 * @param {string} filePath - 文件路径
 */
async function deleteGeneratedFile(filePath) {
    try {
        // 首先检查文件是否存在
        const fileExists = await fs.promises.access(filePath).then(() => true).catch(() => false);
        if (!fileExists) {
            console.warn(`✗ 文件不存在: ${filePath}`);
            return false;
        }

        // 检查文件是否在缓存中被记录为生成文件
        const cache = Cache.load();
        let isGeneratedFile = false;
        
        for (const templateFile in cache.templates) {
            const generated = cache.templates[templateFile].find(
                item => item.generatedFile === filePath || item.generatedFile.replace(/\\/g, '/') === filePath.replace(/\\/g, '/')
            );
            if (generated) {
                isGeneratedFile = true;
                break;
            }
        }

        if (!isGeneratedFile) {
            console.warn(`✗ 文件不是通过系统生成的: ${filePath}`);
            console.warn(`  如果确实要删除，请使用其他方法`);
            return false;
        }

        // 删除文件
        await fs.promises.unlink(filePath);
        console.log(`✓ 已删除文件: ${filePath}`);
        
        // 从缓存中移除该文件的所有记录
        for (const templateFile in cache.templates) {
            cache.templates[templateFile] = cache.templates[templateFile].filter(
                item => (item.generatedFile !== filePath && item.generatedFile.replace(/\\/g, '/') !== filePath.replace(/\\/g, '/'))
            );
        }
        
        // 从缓存中移除与该文件相关的所有标记
        const keysToDelete = [];
        for (const identifier in cache.markers) {
            cache.markers[identifier] = cache.markers[identifier].filter(
                marker => (marker.file !== filePath && marker.file.replace(/\\/g, '/') !== filePath.replace(/\\/g, '/'))
            );
            // 如果该标记已经没有任何记录，删除这个标记
            if (cache.markers[identifier].length === 0) {
                keysToDelete.push(identifier);
            }
        }
        
        keysToDelete.forEach(key => delete cache.markers[key]);
        
        cache.save();
        console.log(`✓ 已清理缓存中的相关记录`);
        return true;
    } catch (error) {
        console.error(`✗ 删除文件失败: ${error.message}`);
        return false;
    }
}

/**
 * 删除已插入的内容（行内删除）
 * 删除 //@@identifier+++ 和 //@@identifier--- 之间的所有内容
 */
async function removeContent(filePath, identifier) {
    try {
        let fileContent = await fs.promises.readFile(filePath, 'utf8');
        const lines = fileContent.split('\n');
        
        // 查找起始标记
        let startIndex = -1;
        let endIndex = -1;

        for (let i = 0; i < lines.length; i++) {
            if (lines[i].includes(`//@@${identifier}+++`)) {
                startIndex = i;
            }
            if (lines[i].includes(`//@@${identifier}---`)) {
                endIndex = i;
                break;
            }
        }

        if (startIndex === -1 || endIndex === -1) {
            console.warn(`✗ 未找到标记范围: //@@${identifier}+++ 到 //@@${identifier}--- 在 ${filePath}`);
            return false;
        }

        // 删除标记和内容
        lines.splice(startIndex, endIndex - startIndex + 1);

        await fs.promises.writeFile(filePath, lines.join('\n'), 'utf8');
        console.log(`✓ 已删除内容: //@@${identifier} 在 ${filePath}`);
        return true;
    } catch (error) {
        console.error(`✗ 删除内容失败: ${error.message}`);
        return false;
    }
}

/**
 * 从模板生成新文件
 * @param {string} templatePath - 模板文件路径 (*.tpl 或 *.template)
 * @param {string} outputName   - 输出文件名（不含后缀）
 * @param {Object} config       - 配置对象，包含要插入的内容
 *
 * 约定：模板中所有出现的 `$$` 都会被替换为实际的 outputName
 * 例如：`//@@$$-init` → `//@@Apple-init`，`class $$Item` → `class AppleItem`
 */
async function generateFromTemplate(templatePath, outputName, config) {
    try {
        // 读取模板文件
        let templateContent = await fs.promises.readFile(templatePath, 'utf8');
        const templateExtension = path.extname(templatePath);
        const outputExtension = templateExtension.replace(/\.tpl$|\.template$/i, '.java'); // 或其他输出格式

        const resolvedOutputName = outputName;

        // 全局替换 $$ 为具体的文件名（不再要求前面必须有 //@@）
        templateContent = templateContent.replace(/\$\$/g, resolvedOutputName);

        // 获取输出文件的目录
        const outputDir = path.dirname(templatePath);
        const outputPath = path.join(outputDir, `${resolvedOutputName}${outputExtension || '.java'}`);

        // 创建输出文件
        await fs.promises.writeFile(outputPath, templateContent, 'utf8');
        console.log(`✓ 已生成文件: ${outputPath}`);

        // 将配置内容插入到生成的文件中（仍使用 //@@<name>-<key> 作为标记）
        let insertCount = 0;
        for (const [key, value] of Object.entries(config || {})) {
            const identifier = `${resolvedOutputName}-${key}`;
            if (await insertContent(outputPath, identifier, value)) {
                insertCount++;
            }
        }

        console.log(`✓ 已插入 ${insertCount} 个内容块`);

        // 记录到缓存
        const cache = Cache.load();
        cache.recordTemplate(templatePath, outputPath, resolvedOutputName, config);
        cache.save();

        return outputPath;
    } catch (error) {
        console.error(`✗ 生成文件失败: ${error.message}`);
        return null;
    }
}

/**
 * 扫描目录，找出所有的标记和模板文件
 */
async function scanDirectory(directory) {
    const results = {
        markers: [],          // 找到的所有 //@@identifier 标记
        templates: [],        // 找到的所有 .tpl 和 .template 文件
        insertedRanges: []    // 找到的所有已插入的内容范围
    };

    async function walkDir(currentPath) {
        try {
            const files = await fs.promises.readdir(currentPath);

            for (const file of files) {
                const fullPath = path.join(currentPath, file);
                const stat = await fs.promises.stat(fullPath);

                if (stat.isDirectory()) {
                    // 跳过特定目录
                    if (['build', '.git', 'node_modules', 'script',"assets"].includes(file)) {
                        continue;
                    }
                    await walkDir(fullPath);
                } else if (stat.isFile()) {
                    // 跳过二进制文件和脚本文件
                    const ignoreExtensions = ['.js', '.ogg', '.mp3', '.ttf', '.png', '.jpg', '.jpeg', '.gif', '.so', '.jar', '.class', '.bin'];
                    if (ignoreExtensions.some(ext => file.toLowerCase().endsWith(ext))) {
                        continue;
                    }

                    // 检查是否是模板文件
                    if (file.endsWith('.tpl') || file.endsWith('.template')) {
                        results.templates.push(fullPath);
                    }

                    // 扫描文件中的标记
                    try {
                        const content = await fs.promises.readFile(fullPath, 'utf8');
                        const lines = content.split('\n');

                        for (let i = 0; i < lines.length; i++) {
                            const line = lines[i];

                            // 查找 //@@identifier 标记
                            const markerMatch = line.match(/\/\/@@([a-zA-Z0-9\-_$]+)(?!\+\+\+|---)/);
                            if (markerMatch) {
                                results.markers.push({
                                    file: fullPath,
                                    line: i + 1,
                                    identifier: markerMatch[1],
                                    type: 'marker'
                                });
                            }

                            // 查找已插入的内容范围
                            const startMatch = line.match(/\/\/@@([a-zA-Z0-9\-_]+)\+\+\+/);
                            if (startMatch) {
                                const identifier = startMatch[1];
                                let endLine = -1;

                                // 查找对应的结束标记
                                for (let j = i + 1; j < lines.length; j++) {
                                    if (lines[j].includes(`//@@${identifier}---`)) {
                                        endLine = j;
                                        break;
                                    }
                                }

                                if (endLine !== -1) {
                                    results.insertedRanges.push({
                                        file: fullPath,
                                        identifier: identifier,
                                        startLine: i + 1,
                                        endLine: endLine + 1,
                                        contentLines: endLine - i - 1
                                    });
                                }
                            }
                        }
                    } catch (error) {
                        console.warn(`⚠ 无法读取文件: ${fullPath}`);
                    }
                }
            }
        } catch (error) {
            console.warn(`⚠ 无法访问目录: ${currentPath}`);
        }
    }

    await walkDir(directory);
    return results;
}

/**
 * 主函数：扫描当前工作目录并输出所有模板与标记
 */
async function main() {
    const startDir = process.cwd();
    console.log(`\n开始扫描目录: ${startDir}\n`);

    // 扫描目录
    const results = await scanDirectory(startDir);

    // 输出结果
    console.log('=== 扫描结果 ===\n');

    console.log(`📄 模板文件 (${results.templates.length} 个):`);
    results.templates.forEach(file => {
        console.log(`  - ${file}`);
    });

    console.log(`\n🏷️  标记点 (${results.markers.length} 个):`);
    results.markers.forEach(marker => {
        console.log(`  - ${marker.file}:${marker.line} - //@@${marker.identifier}`);
    });

    console.log(`\n📦 已插入内容范围 (${results.insertedRanges.length} 个):`);
    results.insertedRanges.forEach(range => {
        console.log(`  - ${range.file}:${range.startLine}-${range.endLine} - //@@${range.identifier} (${range.contentLines} 行)`);
    });

    return results;
}

// 如果直接运行此脚本（ESM 环境下的等价 __filename 判断）
const __filename = fileURLToPath(import.meta.url);
if (process.argv[1] && path.resolve(process.argv[1]) === __filename) {
    main().catch(error => {
        console.error('✗ 扫描出错:', error);
        process.exit(1);
    });
}

export {
    Cache,
    insertContent,
    removeContent,
    deleteGeneratedFile,
    generateFromTemplate,
    scanDirectory,
    main
};
