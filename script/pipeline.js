import path from 'path';
import {
    insertContent,
    removeContent,
    deleteGeneratedFile,
    generateFromTemplate,
    scanDirectory
} from './io.js';

/**
 * 简化版任务系统
 *
 * 概念：
 * - op:   原子 IO 操作，使用数组表示，例如：
 *   ['insert',  fileSpec, identifier, content]
 *   ['remove',  fileSpec, identifier]
 *   ['generate', templateSpec, outputName, config]
 *   ['delete',  fileSpec]
 *   ['custom',  name, asyncHandler]
 *
 * - task: 一串 op，顺序执行
 *   { id, ops }
 *
 * - job: 一组可以并行执行的 task + 依赖
 *   { id, tasks, dependsOn, parallel, paths }
 *
 * - pipeline: 多个 job 的集合，负责：
 *   - scan 目录，获取 markers 和 templates；
 *   - 根据 fileSpec / templateSpec 解析真实路径；
 *   - 校验 id、依赖、op 合法性；
 *   - job 级拓扑排序；
 *   - 按顺序执行 job（job 内 task 可串并行）。
 *
 * 特殊 fileSpec：
 *   - 'thisfile'            : 使用当前 op 的 identifier，在 scan 结果中找到唯一的文件
 *   - 'filewithin:marker'   : 使用给定 marker 名，在 scan 结果中找到唯一的文件
 *
 * 特殊 templateSpec：
 *   - 'tpl:xxx'             : 使用文件名 'xxx' 或 'xxx.tpl' 去 scan 出来的 templates 里按 basename 匹配
 *   - 直接写文件名          : 例如 'food.tpl'，同样按 basename 匹配
 *
 * 路径映射：
 *   - job.paths[fileKey]       => 具体路径
 *   - pipeline.paths[fileKey]  => 作为全局 fallback
 *   解析 fileSpec / templateSpec 时优先查找 job.paths，其次 pipeline.paths。
 */

/**
 * JobBuilder - 链式构建 job
 */
class JobBuilder {
    constructor(id, initialTaskDefs = [], options = {}) {
        this.id = id;
        this._taskDefs = Array.isArray(initialTaskDefs) ? [...initialTaskDefs] : (initialTaskDefs ? [initialTaskDefs] : []);
        this.dependsOn = [];
        this.parallel = options.parallel === true;
        this.paths = options.paths || {};
    }

    /**
     * 添加一个 task 定义
     *
     * taskDef 可以是：
     *  - Op[]                    => 一串 op，构成一个 task
     *  - () => Op[]              => 延迟创建的 task
     *  - { id, ops }             => 显式 Task 定义
     */
    and(taskDef) {
        if (taskDef) {
            this._taskDefs.push(taskDef);
        }
        return this;
    }

    /**
     * 声明依赖（在这些 job 之后执行）
     *
     * 参数可以是 jobId 字符串，或其他 JobBuilder / job 对象
     */
    after(...jobOrIds) {
        for (const j of jobOrIds) {
            if (!j) continue;
            if (typeof j === 'string') {
                this.dependsOn.push(j);
            } else if (j instanceof JobBuilder) {
                this.dependsOn.push(j.id);
            } else if (typeof j.id === 'string') {
                this.dependsOn.push(j.id);
            }
        }
        return this;
    }

    /**
     * 设置 job 级路径映射
     */
    withPaths(paths) {
        this.paths = { ...(this.paths || {}), ...(paths || {}) };
        return this;
    }

    /**
     * 展开内部 taskDefs，生成标准 Job 定义
     */
    build() {
        const tasks = [];
        const usedTaskIds = new Set();

        this._taskDefs.forEach((def, index) => {
            let task;

            // 函数 => 调用后得到真正的定义
            if (typeof def === 'function') {
                def = def();
            }

            // 纯 ops 数组 => 自动生成 task id
            if (Array.isArray(def)) {
                const id = `${this.id}#${index}`;
                task = { id, ops: def };
            } else if (def && typeof def === 'object' && Array.isArray(def.ops)) {
                task = { id: def.id || `${this.id}#${index}`, ops: def.ops };
            } else if (def == null) {
                return;
            } else {
                throw new Error(`Job ${this.id}: 无法识别的 task 定义`);
            }

            if (!task.id) {
                throw new Error(`Job ${this.id}: task 缺少 id`);
            }
            if (usedTaskIds.has(task.id)) {
                throw new Error(`Job ${this.id}: task id 重复: ${task.id}`);
            }
            usedTaskIds.add(task.id);

            tasks.push(task);
        });

        return {
            id: this.id,
            tasks,
            dependsOn: [...new Set(this.dependsOn)],
            parallel: this.parallel,
            paths: this.paths || {}
        };
    }
}

/**
 * 创建 JobBuilder
 *
 * 用法示例：
 *   const foodJob = createJob('food')
 *       .and(() => createFood('GoldApple', 120))
 *       .after('validate');
 */
export function createJob(id, initialTaskDefs = [], options = {}) {
    return new JobBuilder(id, initialTaskDefs, options);
}

/**
 * 工具：根据 job 定义做拓扑排序
 */
function topoSortJobs(jobs) {
    const byId = new Map();
    jobs.forEach(job => {
        if (byId.has(job.id)) {
            throw new Error(`Job id 重复: ${job.id}`);
        }
        byId.set(job.id, job);
    });

    const visited = new Set();
    const visiting = new Set();
    const order = [];

    function visit(id) {
        if (visited.has(id)) return;
        if (visiting.has(id)) {
            throw new Error(`检测到 job 循环依赖: ${id}`);
        }
        const job = byId.get(id);
        if (!job) {
            throw new Error(`依赖的 job 不存在: ${id}`);
        }

        visiting.add(id);
        for (const depId of job.dependsOn || []) {
            visit(depId);
        }
        visiting.delete(id);
        visited.add(id);
        order.push(job);
    }

    jobs.forEach(job => {
        if (!visited.has(job.id)) {
            visit(job.id);
        }
    });

    return order;
}

/**
 * 根据 fileSpec / identifier 从 scan 结果与 paths 映射中解析真实文件路径
 */
function resolveFileSpec(fileSpec, identifier, scanResult, jobPaths = {}, pipelinePaths = {}) {
    if (!fileSpec) {
        throw new Error(`fileSpec 不能为空`);
    }

    // 特殊值: thisfile
    if (fileSpec === 'thisfile') {
        if (!identifier) {
            throw new Error(`使用 thisfile 时必须提供 identifier`);
        }
        const matches = scanResult.markers.filter(m => m.identifier === identifier);
        if (matches.length === 0) {
            throw new Error(`标记 ${identifier} 未在任何文件中找到`);
        }
        if (matches.length > 1) {
            throw new Error(`标记 ${identifier} 在多个文件中出现，无法唯一确定 thisfile`);
        }
        return matches[0].file;
    }

    // 特殊值: filewithin:marker
    if (fileSpec.startsWith('filewithin:')) {
        const marker = fileSpec.slice('filewithin:'.length);
        if (!marker) {
            throw new Error(`filewithin: 后必须跟标记名`);
        }
        const matches = scanResult.markers.filter(m => m.identifier === marker);
        if (matches.length === 0) {
            throw new Error(`标记 ${marker} 未在任何文件中找到`);
        }
        if (matches.length > 1) {
            throw new Error(`标记 ${marker} 在多个文件中出现，无法唯一确定 filewithin`);
        }
        return matches[0].file;
    }

    // 先查 job.paths / pipeline.paths 映射
    if (!fileSpec.includes('/') && !fileSpec.includes('\\') && !fileSpec.startsWith('.')) {
        if (jobPaths && jobPaths[fileSpec]) {
            return jobPaths[fileSpec];
        }
        if (pipelinePaths && pipelinePaths[fileSpec]) {
            return pipelinePaths[fileSpec];
        }
    }

    // 普通路径：如果带分隔符或以 . 开头，直接认为是路径
    if (fileSpec.includes('/') || fileSpec.includes('\\') || fileSpec.startsWith('.')) {
        return fileSpec;
    }

    // 其他情况：直接返回，交给上层决定（例如再做变量替换）
    return fileSpec;
}

/**
 * 根据 templateSpec 在 scanResult.templates 中解析模板文件
 *
 * 支持：
 *   - 'tpl:food'   => 在 templates 里找 basename === 'food.tpl'
 *   - 'food.tpl'   => 在 templates 里找 basename === 'food.tpl'
 *   - 路径         => 直接使用
 *   - 映射表       => 先查 job.paths / pipeline.paths
 */
function resolveTemplateSpec(templateSpec, scanResult, jobPaths = {}, pipelinePaths = {}) {
    if (!templateSpec) {
        throw new Error(`templateSpec 不能为空`);
    }

    let spec = templateSpec;

    // 先查映射表
    if (!spec.includes('/') && !spec.includes('\\') && !spec.startsWith('.')) {
        if (jobPaths && jobPaths[spec]) {
            return jobPaths[spec];
        }
        if (pipelinePaths && pipelinePaths[spec]) {
            return pipelinePaths[spec];
        }
    }

    // 特殊前缀: tpl:xxx
    if (spec.startsWith('tpl:')) {
        spec = spec.slice(4);
        if (!spec.includes('.')) {
            spec = `${spec}.tpl`;
        }
    }

    // 显式路径
    if (spec.includes('/') || spec.includes('\\') || spec.startsWith('.')) {
        return spec;
    }

    // 按文件名匹配
    const matches = scanResult.templates.filter(fullPath => {
        return path.basename(fullPath) === spec;
    });

    if (matches.length === 0) {
        throw new Error(`模板 ${spec} 未找到`);
    }
    if (matches.length > 1) {
        throw new Error(`模板 ${spec} 在多个位置出现，无法唯一确定`);
    }

    return matches[0];
}

/**
 * JobPipeline：聚合 jobs，负责：
 *  - scan；
 *  - 校验 id / 依赖；
 *  - 展开 fileSpec / templateSpec；
 *  - 拓扑排序；
 *  - 执行。
 */
export class JobPipeline {
    constructor(jobs = [], options = {}) {
        this.jobs = jobs;
        this.name = options.name || 'job-pipeline';
        this.scanResult = null;
        this.compiledJobs = null;
        this.paths = options.paths || {};
    }

    /**
     * 编译 pipeline（scan + 校验 + 解析）
     */
    async compile(rootDir = process.cwd()) {
        // 1) scan
        this.scanResult = await scanDirectory(rootDir);

        // 2) 展开 JobBuilder 为标准 job 对象
        const builtJobs = this.jobs.map(job => {
            if (job instanceof JobBuilder) return job.build();
            return job;
        });

        // 3) 校验 job id 唯一
        const jobIdSet = new Set();
        for (const job of builtJobs) {
            if (!job.id) {
                throw new Error(`存在没有 id 的 job`);
            }
            if (jobIdSet.has(job.id)) {
                throw new Error(`job id 重复: ${job.id}`);
            }
            jobIdSet.add(job.id);
        }

        // 4) 校验依赖引用
        const allJobIds = new Set(builtJobs.map(j => j.id));
        for (const job of builtJobs) {
            for (const depId of job.dependsOn || []) {
                if (!allJobIds.has(depId)) {
                    throw new Error(`job ${job.id} 依赖不存在的 job: ${depId}`);
                }
            }
        }

        // 5) 校验 task id 唯一并解析 op 中的 fileSpec/templateSpec
        const taskIdSet = new Set();
        for (const job of builtJobs) {
            if (!Array.isArray(job.tasks)) {
                throw new Error(`job ${job.id} 缺少 tasks`);
            }

            const jobPaths = job.paths || {};
            const pipelinePaths = this.paths || {};

            for (const task of job.tasks) {
                if (!task.id) {
                    throw new Error(`job ${job.id} 中存在没有 id 的 task`);
                }
                if (taskIdSet.has(task.id)) {
                    throw new Error(`task id 重复: ${task.id}`);
                }
                taskIdSet.add(task.id);

                if (!Array.isArray(task.ops)) {
                    throw new Error(`task ${task.id} 缺少 ops`);
                }

                // 解析 ops 中的 fileSpec/templateSpec
                task.ops = task.ops.map(op => {
                    if (!Array.isArray(op) || op.length === 0) {
                        throw new Error(`task ${task.id} 中存在非法 op`);
                    }
                    const [type, a, b, c] = op;

                    switch (type) {
                        case 'insert': {
                            const fileSpec = a;
                            const identifier = b;
                            const content = c;
                            const filePath = resolveFileSpec(fileSpec, identifier, this.scanResult, jobPaths, pipelinePaths);
                            return ['insert', filePath, identifier, content];
                        }
                        case 'remove': {
                            const fileSpec = a;
                            const identifier = b;
                            const filePath = resolveFileSpec(fileSpec, identifier, this.scanResult, jobPaths, pipelinePaths);
                            return ['remove', filePath, identifier];
                        }
                        case 'delete': {
                            const fileSpec = a;
                            const filePath = resolveFileSpec(fileSpec, null, this.scanResult, jobPaths, pipelinePaths);
                            return ['delete', filePath];
                        }
                        case 'generate': {
                            // 支持两种形式：
                            // 1) ['generate', tplSpec, outputName, config]
                            // 2) ['generate', tplSpec, config]，其中 config.outputName 必须存在
                            const tplSpec = a;
                            let outputName;
                            let cfg;

                            if (typeof b === 'string') {
                                outputName = b;
                                cfg = c || {};
                            } else if (b && typeof b === 'object') {
                                cfg = b;
                                outputName = b.outputName;
                            }

                            if (!outputName) {
                                throw new Error(`task ${task.id} 的 generate op 缺少 outputName`);
                            }

                            const templatePath = resolveTemplateSpec(tplSpec, this.scanResult, jobPaths, pipelinePaths);
                            return ['generate', templatePath, outputName, cfg || {}];
                        }
                        case 'custom': {
                            // ['custom', name, handler]
                            const name = a;
                            const handler = b;
                            if (typeof handler !== 'function') {
                                throw new Error(`task ${task.id} 的 custom op handler 必须是函数`);
                            }
                            return ['custom', name, handler];
                        }
                        default:
                            throw new Error(`task ${task.id} 包含未知 op 类型: ${type}`);
                    }
                });
            }
        }

        // 6) 计算执行顺序
        this.compiledJobs = topoSortJobs(builtJobs);
    }

    /**
     * 打印执行计划
     */
    print() {
        if (!this.compiledJobs) {
            console.log('Pipeline 尚未编译');
            return;
        }

        console.log(`📋 Pipeline: ${this.name}`);
        this.compiledJobs.forEach((job, idx) => {
            console.log(`  ${idx + 1}. Job ${job.id} [tasks: ${job.tasks.length}] dependsOn: ${job.dependsOn.join(', ') || '(none)'}`);
        });
    }

    /**
     * 执行 pipeline
     * - dryRun: 只打印，不实际执行 IO
     */
    async run(options = {}) {
        const { dryRun = false } = options;

        if (!this.compiledJobs) {
            await this.compile();
        }

        const jobStatus = new Map(); // id -> 'pending' | 'running' | 'completed' | 'failed' | 'skipped'
        // 在整个 pipeline 级别收集所有 insert 操作，按 (filePath, identifier) 分组，统一合并插入
        const pendingInserts = new Map(); // key -> { filePath, identifier, chunks: [] }

        const bufferInsert = (filePath, identifier, content) => {
            const key = `${filePath}::${identifier}`;
            let entry = pendingInserts.get(key);
            if (!entry) {
                entry = { filePath, identifier, chunks: [] };
                pendingInserts.set(key, entry);
            }
            if (content && content.length > 0) {
                entry.chunks.push(content);
            }
        };

        for (const job of this.compiledJobs) {
            // 如果依赖中有失败的 job，则跳过
            const failedDeps = (job.dependsOn || []).filter(id => jobStatus.get(id) === 'failed');
            if (failedDeps.length > 0) {
                console.log(`⏭️  跳过 job ${job.id}（依赖失败: ${failedDeps.join(', ')}）`);
                jobStatus.set(job.id, 'skipped');
                continue;
            }

            console.log(`🚀 Job start: ${job.id}`);
            jobStatus.set(job.id, 'running');

            let jobFailed = false;

            const runTask = async (task) => {
                console.log(`  ▶ Task: ${task.id}`);

                for (const op of task.ops) {
                    const [type, a, b, c] = op;

                    try {
                        if (dryRun) {
                            console.log(`    [DRY] ${type}`, a ?? '', b ?? '', c ?? '');
                            continue;
                        }

                        switch (type) {
                            case 'insert': {
                                // 先缓冲，稍后统一合并插入，避免同一标记多次写入
                                bufferInsert(a, b, c);
                                break;
                            }
                            case 'remove':
                                await removeContent(a, b);
                                break;
                            case 'delete':
                                await deleteGeneratedFile(a);
                                break;
                            case 'generate':
                                await generateFromTemplate(a, b, c);
                                break;
                            case 'custom':
                                await b(); // handler
                                break;
                            default:
                                throw new Error(`未知 op 类型: ${type}`);
                        }
                    } catch (error) {
                        console.error(`    ✗ op 失败 (${type}): ${error.message}`);
                        throw error;
                    }
                }
            };

            try {
                if (job.parallel) {
                    await Promise.all(job.tasks.map(t => runTask(t)));
                } else {
                    for (const task of job.tasks) {
                        await runTask(task);
                    }
                }
            } catch (error) {
                jobFailed = true;
            }

            if (jobFailed) {
                console.log(`❌ Job failed: ${job.id}`);
                jobStatus.set(job.id, 'failed');
            } else {
                console.log(`✅ Job finish: ${job.id}`);
                jobStatus.set(job.id, 'completed');
            }
        }

        // 所有 job 执行完毕后，统一 flush 缓冲的 insert 操作
        if (!dryRun && pendingInserts.size > 0) {
            console.log('\n📝 开始合并写入插入内容...');
            for (const { filePath, identifier, chunks } of pendingInserts.values()) {
                if (!chunks.length) continue;
                const merged = chunks.join('\n');
                try {
                    await insertContent(filePath, identifier, merged);
                    console.log(`  ✓ insert ${identifier} -> ${filePath} (${chunks.length} 段合并)`);
                } catch (e) {
                    console.error(`  ✗ 合并插入失败 [${identifier} @ ${filePath}]: ${e.message}`);
                }
            }
        }

        return {
            success: Array.from(jobStatus.values()).every(s => s === 'completed' || s === 'skipped'),
            jobStatus
        };
    }
}

/**
 * 创建 Pipeline
 *
 * 用法示例：
 *
 * function createFood(name, hunger) {
 *   return [
 *     ['generate', 'tpl:food', name, { hunger, name }],
 *     ['insert', 'thisfile', 'foodregistry', `new ${name}(...)`]
 *   ];
 * }
 *
 * const validateJob = createJob('validate')
 *   .and([
 *     ['custom', 'validate-templates', async () => { console.log('validate templates'); }]
 *   ]);
 *
 * const foodJob = createJob('food')
 *   .and(() => createFood('GoldApple', 120))
 *   .after(validateJob);
 *
 * const pipeline = createPipeline(validateJob, foodJob, { paths: { assets: 'core/src/.../Assets.java' } });
 * await pipeline.compile();
 * pipeline.print();
 * await pipeline.run();
 */
export function createPipeline(...jobsOrBuildersOrOptions) {
    let jobs = [];
    let options = {};

    if (jobsOrBuildersOrOptions.length > 0) {
        const last = jobsOrBuildersOrOptions[jobsOrBuildersOrOptions.length - 1];
        if (last && typeof last === 'object' && !Array.isArray(last) && !('build' in last) && !('id' in last)) {
            // 认为最后一个是 options
            options = last;
            jobs = jobsOrBuildersOrOptions.slice(0, -1);
        } else {
            jobs = jobsOrBuildersOrOptions;
        }
    }

    jobs = jobs.flat();
    return new JobPipeline(jobs, options);
}


