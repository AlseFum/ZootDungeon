import fs from 'fs';
import path from 'path';
import { walkDirectory } from './util.js';

/**
 * 文件系统中间层
 * 提供事务性的文件操作：读取从硬盘，修改缓存在内存，调用 submit 才写入硬盘
 * 同时管理插入操作和模板映射的缓存，方便回退和追踪
 */
class FileSystemLayer {
    /**
     * 构造函数
     */
    constructor() {
        /** @type {Map<string, string[]>} 文件路径 -> 文件行数组的缓存 */
        this.cache = new Map();
        
        /** @type {Set<string>} 被修改的文件路径集合 */
        this.dirtyFiles = new Set();
        
        /** @type {Set<string>} 已读取的文件路径集合（用于跟踪） */
        this.readFiles = new Set();
        
        /** @type {Set<string>} 已标记为删除的文件路径集合 */
        this.deletedFiles = new Set();
        
        /** @type {Object<string, Array<{id: string, content: [number, number]}>>} 记录标记信息，key 为文件路径，value 为标记数组 */
        this.markers = {};
        
        /** @type {Object<string, Array>} 记录模板文件的映射关系，key 为模板文件路径 */
        this.templates = {};
        
        /** @type {string|null} 时间戳 */
        this.timestamp = null;
    }

    /**
     * 读取文件
     * 优先从缓存读取，如果缓存中没有则从硬盘读取并缓存（按行存储）
     * @param {string} filePath - 文件路径
     * @param {string} [encoding='utf8'] - 编码格式
     * @returns {Promise<string>} 文件内容
     */
    async readFile(filePath, encoding = 'utf8',asLines=false) {
        const normalizedPath = path.resolve(filePath).replace(/\\/g, '/');
        
        // 如果缓存中有，直接返回（将行数组合并为字符串）
        if (this.cache.has(normalizedPath)) {
            return asLines ? this.cache.get(normalizedPath) : this.cache.get(normalizedPath).join('\n');
        }

        // 从硬盘读取
        // 先检查文件是否存在
        const exists = await fs.promises.access(normalizedPath).then(() => true).catch(() => false);
        if (!exists) {
            // 文件不存在，设置为空
            const emptyLines = [];
            this.cache.set(normalizedPath, emptyLines);
            this.readFiles.add(normalizedPath);
            return asLines ? emptyLines : '';
        }
        
        const content = await fs.promises.readFile(normalizedPath, encoding);
        // 按行分割并缓存
        const lines = content.split('\n');
        this.cache.set(normalizedPath, lines);
        this.readFiles.add(normalizedPath);
        return asLines ? lines : content;
    }

    /**
     * 同步读取文件
     * @param {string} filePath - 文件路径
     * @param {string} [encoding='utf8'] - 编码格式
     * @returns {string} 文件内容
     */
    readFileSync(filePath, encoding = 'utf8') {
        const normalizedPath = path.resolve(filePath).replace(/\\/g, '/');
        
        // 如果缓存中有，直接返回（将行数组合并为字符串）
        if (this.cache.has(normalizedPath)) {
            return this.cache.get(normalizedPath).join('\n');
        }

        // 从硬盘读取
        // 先检查文件是否存在
        if (!fs.existsSync(normalizedPath)) {
            // 文件不存在，设置为空
            const emptyLines = [];
            this.cache.set(normalizedPath, emptyLines);
            this.readFiles.add(normalizedPath);
            return '';
        }
        
        const content = fs.readFileSync(normalizedPath, encoding);
        // 按行分割并缓存
        const lines = content.split('\n');
        this.cache.set(normalizedPath, lines);
        this.readFiles.add(normalizedPath);
        return content;
    }

    /**
     * 写入文件（只写入缓存，不写入硬盘）
     * @param {string} filePath - 文件路径
     * @param {string} content - 文件内容
     * @param {string} [encoding='utf8'] - 编码格式
     */
    writeFile(filePath, content, encoding = 'utf8') {
        const normalizedPath = path.resolve(filePath).replace(/\\/g, '/');
        // 如果文件被标记为删除，先移除删除标记
        this.deletedFiles.delete(normalizedPath);
        // 按行分割并缓存
        const lines = content.split('\n');
        this.cache.set(normalizedPath, lines);
        this.dirtyFiles.add(normalizedPath);
    }

    /**
     * 标记文件为删除（只标记，不实际删除，调用 submit 时才删除）
     * @param {string} filePath - 文件路径
     * @returns {{success: boolean, value?: any, error?: string}} Result 对象
     */
    deleteFile(filePath) {
        const normalizedPath = path.resolve(filePath).replace(/\\/g, '/');
        
        // 如果文件已经被标记为删除，返回成功
        if (this.deletedFiles.has(normalizedPath)) {
            return {
                success: true,
                value: { alreadyDeleted: true }
            };
        }
        
        // 标记为删除
        this.deletedFiles.add(normalizedPath);
        // 从 dirtyFiles 中移除（如果存在）
        this.dirtyFiles.delete(normalizedPath);
        // 从缓存中移除（可选，保留也可以）
        // this.cache.delete(normalizedPath);
        
        return {
            success: true,
            value: { deleted: true }
        };
    }

    /**
     * 检查文件是否被标记为删除
     * @param {string} filePath - 文件路径
     * @returns {boolean}
     */
    isDeleted(filePath) {
        const normalizedPath = path.resolve(filePath).replace(/\\/g, '/');
        return this.deletedFiles.has(normalizedPath);
    }

    /**
     * 检查文件是否存在（在文件系统中）
     * @param {string} filePath - 文件路径
     * @returns {boolean}
     */
    hasFile(filePath) {
        const normalizedPath = path.resolve(filePath).replace(/\\/g, '/');
        return fs.existsSync(normalizedPath);
    }

    /**
     * 检查文件是否被修改（dirty）
     * @param {string} filePath - 文件路径
     * @returns {boolean}
     */
    isDirty(filePath) {
        const normalizedPath = path.resolve(filePath).replace(/\\/g, '/');
        return this.dirtyFiles.has(normalizedPath);
    }

    /**
     * 获取所有被修改的文件列表
     * @returns {string[]}
     */
    getDirtyFiles() {
        return Array.from(this.dirtyFiles);
    }

    /**
     * 获取所有已读取的文件列表
     * @returns {string[]}
     */
    getReadFiles() {
        return Array.from(this.readFiles);
    }

    /**
     * 获取所有已标记为删除的文件列表
     * @returns {string[]}
     */
    getDeletedFiles() {
        return Array.from(this.deletedFiles);
    }

    /**
     * 提交所有修改到硬盘
     * @param {Object} [options] - 选项
     * @param {boolean} [options.createDir=true] - 如果目录不存在是否创建
     * @param {Function} [options.onWrite] - 写入文件时的回调 (filePath) => void
     * @returns {Promise<{success: number, failed: number, errors: Array}>}
     */
    async submit(options = {}) {
        const {
            createDir = true,
            onWrite = null
        } = options;

        const result = {
            success: 0,
            failed: 0,
            errors: []
        };

        // 第一步：检查所有 dirtyFiles 是否可写
        const validationErrors = [];
        for (const filePath of this.dirtyFiles) {
            const dir = path.dirname(filePath);
            
            // 检查目录是否存在或可创建
            if (createDir) {
                const dirExists = await fs.promises.access(dir).then(() => true).catch(() => false);
                if (!dirExists) {
                    // 尝试创建目录
                    const canCreate = await fs.promises.mkdir(dir, { recursive: true }).then(() => true).catch((error) => {
                        validationErrors.push({
                            file: filePath,
                            error: `无法创建目录: ${error.message}`
                        });
                        return false;
                    });
                    if (!canCreate) {
                        continue;
                    }
                }
            } else {
                // 不创建目录时，检查目录是否存在
                const dirExists = await fs.promises.access(dir).then(() => true).catch(() => false);
                if (!dirExists) {
                    validationErrors.push({
                        file: filePath,
                        error: `目录不存在: ${dir}`
                    });
                    continue;
                }
            }
            
            // 检查文件是否可写（如果文件存在，检查写入权限；如果不存在，检查目录写入权限）
            const fileExists = await fs.promises.access(filePath).then(() => true).catch(() => false);
            if (fileExists) {
                // 文件存在，检查写入权限
                const canWrite = await fs.promises.access(filePath, fs.constants.W_OK).then(() => true).catch((error) => {
                    validationErrors.push({
                        file: filePath,
                        error: `文件不可写: ${error.message}`
                    });
                    return false;
                });
                if (!canWrite) {
                    continue;
                }
            } else {
                // 文件不存在，检查目录写入权限
                const canWrite = await fs.promises.access(dir, fs.constants.W_OK).then(() => true).catch((error) => {
                    validationErrors.push({
                        file: filePath,
                        error: `目录不可写: ${error.message}`
                    });
                    return false;
                });
                if (!canWrite) {
                    continue;
                }
            }
        }

        // 如果有验证错误，报错并返回，不写入任何文件
        if (validationErrors.length > 0) {
            console.error('✗ 提交失败：以下文件无法写入：');
            validationErrors.forEach(err => {
                console.error(`  - ${err.file}: ${err.error}`);
            });
            result.failed = validationErrors.length;
            result.errors = validationErrors;
            return result;
        }

        // 第二步：所有检查通过，开始写入文件
        for (const filePath of this.dirtyFiles) {
            const lines = this.cache.get(filePath);
            // 将行数组合并为字符串
            const content = lines.join('\n');
            
            // 确保目录存在（之前已经检查过，这里确保创建）
            if (createDir) {
                const dir = path.dirname(filePath);
                await fs.promises.mkdir(dir, { recursive: true });
            }

            // 写入文件
            await fs.promises.writeFile(filePath, content, 'utf8');
            
            if (onWrite) {
                onWrite(filePath);
            }
            
            result.success++;
        }

        // 检查缓存中其他文件，如果文件系统中不存在就创建
        for (const [filePath, lines] of this.cache.entries()) {
            // 跳过已经在 dirtyFiles 中处理过的文件
            if (this.dirtyFiles.has(filePath)) {
                continue;
            }
            // 跳过已标记为删除的文件
            if (this.deletedFiles.has(filePath)) {
                continue;
            }
            
            // 检查文件是否存在
            const exists = await fs.promises.access(filePath).then(() => true).catch(() => false);
            if (!exists) {
                // 将行数组合并为字符串
                const content = lines.join('\n');
                
                // 确保目录存在
                if (createDir) {
                    const dir = path.dirname(filePath);
                    await fs.promises.mkdir(dir, { recursive: true }).catch(() => {});
                }

                // 创建文件
                await fs.promises.writeFile(filePath, content, 'utf8').then(() => {
                    if (onWrite) {
                        onWrite(filePath);
                    }
                    result.success++;
                }).catch((error) => {
                    result.failed++;
                    result.errors.push({
                        file: filePath,
                        error: error.message
                    });
                    console.error(`✗ 创建文件失败: ${filePath} - ${error.message}`);
                });
            }
        }

        // 第三步：处理删除的文件
        for (const filePath of this.deletedFiles) {
            const exists = await fs.promises.access(filePath).then(() => true).catch(() => false);
            if (exists) {
                await fs.promises.unlink(filePath).then(() => {
                    if (onWrite) {
                        onWrite(filePath);
                    }
                    result.success++;
                }).catch((error) => {
                    result.failed++;
                    result.errors.push({
                        file: filePath,
                        error: `删除文件失败: ${error.message}`
                    });
                    console.error(`✗ 删除文件失败: ${filePath} - ${error.message}`);
                });
            } else {
                // 文件不存在，也算成功（可能已经被删除了）
                result.success++;
            }
        }

        // 清空 dirty 标记和 deleted 标记（但保留缓存内容）
        this.dirtyFiles.clear();
        this.deletedFiles.clear();

        return result;
    }

    /**
     * 清空所有缓存
     */
    clear() {
        this.cache.clear();
        this.dirtyFiles.clear();
        this.readFiles.clear();
        this.deletedFiles.clear();
    }

    /**
     * 重置指定文件的修改状态（从缓存中移除，下次读取会从硬盘重新读取）
     * @param {string} filePath - 文件路径
     */
    resetFile(filePath) {
        const normalizedPath = path.resolve(filePath).replace(/\\/g, '/');
        this.cache.delete(normalizedPath);
        this.dirtyFiles.delete(normalizedPath);
        this.readFiles.delete(normalizedPath);
        this.deletedFiles.delete(normalizedPath);
    }

    /**
     * 记录标记信息
     * 如果标记已存在，则更新它；否则添加新标记
     * @param {string} filePath - 文件路径
     * @param {string} id - 标记标识符
     * @param {number} start - 内容起始行号（从1开始）
     * @param {number} end - 内容结束行号（从1开始，包含该行）
     */
    recordMarker(filePath, id, start, end) {
        const normalizedPath = path.resolve(filePath).replace(/\\/g, '/');
        if (!this.markers[normalizedPath]) {
            this.markers[normalizedPath] = [];
        }
        
        // 查找是否已存在相同的标记
        const existingMarker = this.markers[normalizedPath].find(m => m.id === id);
        if (existingMarker) {
            // 更新已存在的标记
            existingMarker.content = [start, end];
        } else {
            // 添加新标记
            this.markers[normalizedPath].push({
                id: id,
                content: [start, end]
            });
        }
    }

    /**
     * 根据标记设置文件内容并更新标记范围
     * @param {string} filePath - 文件路径
     * @param {string} id - 标记标识符
     * @param {string} content - 要设置的内容
     * @returns {{success: boolean, value?: any, error?: string}} Result 对象
     */
    setByMarker(filePath, id, content) {
        const normalizedPath = path.resolve(filePath).replace(/\\/g, '/');
        
        // 确保文件在缓存中
        if (!this.cache.has(normalizedPath)) {
            // 如果缓存中没有，尝试从硬盘读取（使用类自己的方法，文件不存在会返回空）
            this.readFileSync(normalizedPath, 'utf8');
        }

        // 查找对应的标记
        if (!this.markers[normalizedPath]) {
            return {
                success: false,
                error: `文件 ${normalizedPath} 中没有标记记录`
            };
        }

        const marker = this.markers[normalizedPath].find(m => m.id === id);
        if (!marker) {
            return {
                success: false,
                error: `未找到标记 ${id} 在文件 ${normalizedPath}`
            };
        }

        // 获取当前文件的行数组
        const lines = this.cache.get(normalizedPath);
        const [oldStart, oldEnd] = marker.content;
        
        // 将新内容按行分割
        const newLines = content.split('\n');
        
        // 计算新的行号范围（从1开始）
        const newStart = oldStart;
        const newEnd = oldStart + newLines.length - 1;
        
        // 替换指定范围的行（转换为数组索引，从0开始）
        lines.splice(oldStart - 1, oldEnd - oldStart + 1, ...newLines);
        
        // 更新标记的范围
        marker.content = [newStart, newEnd];
        
        // 更新后续标记的行号（因为行数可能变化）
        const lineDiff = newLines.length - (oldEnd - oldStart + 1);
        if (lineDiff !== 0) {
            for (const m of this.markers[normalizedPath]) {
                if (m.id !== id && m.content[0] > oldEnd) {
                    // 如果标记在修改范围之后，需要调整行号
                    m.content[0] += lineDiff;
                    m.content[1] += lineDiff;
                }
            }
        }
        
        // 标记文件为已修改
        this.dirtyFiles.add(normalizedPath);
        
        return {
            success: true,
            value: { newStart, newEnd, lineDiff }
        };
    }

    /**
     * 根据标记获取文件内容
     * @param {string} filePath - 文件路径
     * @param {string} id - 标记标识符
     * @returns {{success: boolean, value?: string, error?: string}} Result 对象
     */
    getByMarker(filePath, id) {
        const normalizedPath = path.resolve(filePath).replace(/\\/g, '/');
        
        // 确保文件在缓存中
        if (!this.cache.has(normalizedPath)) {
            // 如果缓存中没有，尝试从硬盘读取（使用类自己的方法，文件不存在会返回空）
            this.readFileSync(normalizedPath, 'utf8');
        }

        // 查找对应的标记
        if (!this.markers[normalizedPath]) {
            return {
                success: false,
                error: `文件 ${normalizedPath} 中没有标记记录`
            };
        }

        const marker = this.markers[normalizedPath].find(m => m.id === id);
        if (!marker) {
            return {
                success: false,
                error: `未找到标记 ${id} 在文件 ${normalizedPath}`
            };
        }

        // 获取文件的行数组
        const lines = this.cache.get(normalizedPath);
        const [start, end] = marker.content;
        
        // 提取标记范围内的行（转换为数组索引，从0开始）
        const markerLines = lines.slice(start - 1, end);
        
        // 合并为字符串返回
        const content = markerLines.join('\n');
        
        return {
            success: true,
            value: content
        };
    }

    /**
     * 记录模板映射
     * @param {string} templateFile - 模板文件路径
     * @param {string} generatedFile - 生成的文件路径
     * @param {string} identifier - 标识符
     * @param {Object} config - 配置对象
     */
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

}

/**
 * 创建一个扫描函数，用于扫描目录并在 fls 中登记找到的 marker
 * @param {Object} config - 配置对象
 * @param {string[]} [config.include] - 包含的文件模式（glob）
 * @param {string[]} [config.exclude] - 排除的文件模式（glob）
 * @param {FileSystemLayer} fls - FileSystemLayer 实例
 * @returns {Function} 返回一个函数，接受路径参数进行扫描
 */
function createMarkerScanner(config, fls) {
    const {
        include = [
            '**/*.java',
            '**/*.tpl',
            '**/*.template',
            '**/*.properties',
            '**/*.xml',
            '**/*.json',
            '**/*.md',
            '**/*.txt',
            '**/*.gradle',
            '**/*.kt'
        ],
        exclude = []
    } = config;

    /**
     * 扫描指定路径，在 fls 中登记找到的 marker
     * @param {string} directory - 要扫描的目录路径
     * @returns {Promise<{success: boolean, value?: {filesScanned: number, markersRecorded: number}, error?: string}>}
     */
    return async function scanPath(directory) {
        let filesScanned = 0;
        let markersRecorded = 0;

        return await walkDirectory(directory, {
            include: include,
            exclude: exclude,
            onFile: async (filePath, relativePath) => {
                filesScanned++;
                
                const normalizedPath = path.resolve(filePath).replace(/\\/g, '/');
                
                // 清除该文件的旧标记（如果存在），以便刷新
                if (fls.markers[normalizedPath]) {
                    fls.markers[normalizedPath] = [];
                }
                
                // 读取文件内容（使用 fls 的方法，文件不存在会返回空）
                const content = await fls.readFile(filePath, 'utf8');
                const lines = content.split('\n');

                // 用于跟踪已插入的内容范围
                const insertedRanges = new Map(); // identifier -> {start, end}
                // 用于跟踪已登记的标记（避免重复登记同一个标记）
                const recordedMarkers = new Set(); // identifier

                for (let i = 0; i < lines.length; i++) {
                    const line = lines[i];

                    // 先检查是否是范围标记（+++ 或 ---），如果是则跳过普通标记检查
                    // 查找已插入的内容范围开始标记 //@@identifier+++
                    const startMatch = line.match(/\/\/@@([a-zA-Z0-9\-_$]+)\+\+\+/);
                    if (startMatch) {
                        const identifier = startMatch[1];
                        insertedRanges.set(identifier, { start: i + 1 });
                        // 标记已处理，跳过后续检查
                        continue;
                    }

                    // 查找已插入的内容范围结束标记 //@@identifier---
                    const endMatch = line.match(/\/\/@@([a-zA-Z0-9\-_$]+)---/);
                    if (endMatch) {
                        const identifier = endMatch[1];
                        const range = insertedRanges.get(identifier);
                        if (range && range.start) {
                            const endLine = i + 1;
                            // 登记已插入的内容范围（这会覆盖之前登记的单个标记）
                            fls.recordMarker(filePath, identifier, range.start, endLine);
                            markersRecorded++;
                            insertedRanges.delete(identifier);
                            recordedMarkers.add(identifier);
                        }
                        // 标记已处理，跳过后续检查
                        continue;
                    }

                    // 查找普通 //@@identifier 标记（不包括 +++ 和 ---）
                    // 确保行中不包含 +++ 或 ---，然后匹配标识符
                    if (!line.includes('+++') && !line.includes('---')) {
                        const markerMatch = line.match(/\/\/@@([a-zA-Z0-9\-_$]+)/);
                        if (markerMatch) {
                            const identifier = markerMatch[1];
                            const lineNumber = i + 1;
                            
                            // 只登记一次（避免与已插入内容范围的标记重复）
                            if (!recordedMarkers.has(identifier)) {
                                // 登记标记（单个标记，start 和 end 都是同一行）
                                fls.recordMarker(filePath, identifier, lineNumber, lineNumber);
                                markersRecorded++;
                                recordedMarkers.add(identifier);
                            }
                        }
                    }
                }
            }
        }).then(() => {
            return {
                success: true,
                value: {
                    filesScanned,
                    markersRecorded
                }
            };
        }).catch((error) => {
            return {
                success: false,
                error: `扫描目录失败: ${directory} - ${error.message}`
            };
        });
    };
}

export {
    FileSystemLayer,
    createMarkerScanner
};
