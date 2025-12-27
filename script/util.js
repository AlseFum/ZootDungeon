import fs from 'fs';
import path from 'path';

/**
 * 简单的 glob 模式匹配函数
 * 支持的模式：
 * - * 匹配任意字符（除了路径分隔符）
 * - ** 匹配任意字符包括路径分隔符（递归匹配）
 * - ? 匹配单个字符
 * - [abc] 字符类
 * @param {string} pattern - glob 模式
 * @param {string} str - 要匹配的字符串
 * @returns {boolean} 是否匹配
 */
export const matchGlob = (pattern, str) => {
    // 标准化路径分隔符
    const normalizedPattern = pattern.replace(/\\/g, '/');
    const normalizedStr = str.replace(/\\/g, '/');
    
    // 将 glob 模式转换为正则表达式
    let regexStr = normalizedPattern
        .replace(/\./g, '\\.')           // 转义点号
        .replace(/\*\*/g, '{{DOUBLE_STAR}}')  // 临时替换 **
        .replace(/\*/g, '[^/]*')         // * 匹配非路径分隔符的任意字符
        .replace(/\{\{DOUBLE_STAR\}\}/g, '.*') // ** 匹配任意字符包括路径分隔符
        .replace(/\?/g, '[^/]')          // ? 匹配单个非路径分隔符字符
        .replace(/\[([^\]]+)\]/g, '[$1]'); // 字符类 [abc]
    
    // 添加行首和行尾锚点
    regexStr = '^' + regexStr + '$';
    
    const regex = new RegExp(regexStr);
    return regex.test(normalizedStr);
}

/**
 * 检查路径是否匹配任何 glob 模式
 * @param {string[]} patterns - glob 模式数组
 * @param {string} filePath - 文件路径
 * @returns {boolean} 是否匹配
 */
export function matchesAnyPattern(patterns, filePath) {
    if (!patterns || patterns.length === 0) {
        return true; // 如果没有模式，默认匹配
    }
    return patterns.some(pattern => matchGlob(pattern, filePath));
}

/**
 * 通用的目录遍历函数
 * @param {string} rootDir - 根目录路径
 * @param {Object} options - 配置选项
 * @param {number} [options.maxDepth] - 最大递归深度，-1 表示无限制，默认为 -1
 * @param {string[]} [options.include] - include glob 模式数组（白名单），只应用于文件
 * @param {string[]} [options.exclude] - exclude glob 模式数组（黑名单），只应用于文件
 * @param {Function} [options.onFile] - 文件回调函数 (filePath, relativePath, depth) => void
 * @param {Function} [options.onDirectory] - 目录回调函数 (dirPath, relativePath, depth) => boolean | void
 *                                           返回 false 可跳过该目录的遍历
 * @returns {Promise<Object>} 返回遍历结果对象
 *                            { files: [], directories: [] }
 */
export async function walkDirectory(rootDir, options = {}) {
    const {
        maxDepth = -1,
        include = [],
        exclude = [],
        onFile = null,
        onDirectory = null
    } = options;

    const results = {
        files: [],
        directories: []
    };

    // 标准化根目录路径
    const normalizedRoot = path.resolve(rootDir).replace(/\\/g, '/');

    /**
     * 递归遍历目录
     * @param {string} currentDir - 当前目录路径
     * @param {number} depth - 当前深度
     * @param {string} relativePath - 相对于根目录的路径
     */
    async function walk(currentDir, depth = 0, relativePath = '') {
        // 检查深度限制
        if (maxDepth > 0 && depth > maxDepth) {
            return;
        }

        const entries = await fs.promises.readdir(currentDir, { withFileTypes: true });
        for (const entry of entries) {
            const fullPath = path.join(currentDir, entry.name).replace(/\\/g, '/');
            const relativeFilePath = relativePath ? `${relativePath}/${entry.name}` : entry.name;
            const relativeDirPath = relativePath ? `${relativePath}/${entry.name}` : entry.name;

            if (entry.isDirectory()) {
                // 目录不受 include/exclude 影响，只通过 onDirectory 回调控制
                // 记录目录
                results.directories.push({
                    path: fullPath,
                    relativePath: relativeDirPath,
                    depth: depth
                });

                // 调用目录回调
                let shouldContinue = true;
                if (onDirectory) {
                    const result = await onDirectory(fullPath, relativeDirPath, depth);
                    if (result === false) {
                        shouldContinue = false;
                    }
                }

                // 继续遍历子目录
                if (shouldContinue) {
                    await walk(fullPath, depth + 1, relativeDirPath);
                }
            } else if (entry.isFile()) {
                // 检查是否应该排除该文件
                if (exclude.length > 0 && matchesAnyPattern(exclude, relativeFilePath)) {
                    continue;
                }

                // 检查是否应该包含该文件（如果有 include 模式）
                if (include.length > 0 && !matchesAnyPattern(include, relativeFilePath)) {
                    continue;
                }

                // 记录文件
                const fileInfo = {
                    path: fullPath,
                    relativePath: relativeFilePath,
                    depth: depth
                };
                results.files.push(fileInfo);

                // 调用文件回调
                if (onFile) {
                    await onFile(fullPath, relativeFilePath, depth);
                }
            }
        }
    }

    await walk(normalizedRoot, 0, '');
    return results;
}
