import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { createJob, createPipeline } from './pipeline.js';

/**
 * Demo 辅助：在 build/testpipeline 下创建用于测试的文件/模板
 * 包含若干标记，用于验证 thisfile / filewithin / tpl:xxx 等解析逻辑。
 */
async function ensureTestFiles() {
  const root = path.join('build', 'testpipeline');
  const templatesDir = path.join(root, 'templates');

  await fs.promises.mkdir(templatesDir, { recursive: true });

  // 模板：food.tpl，包含 //@@$$-* 标记
  const foodTplPath = path.join(templatesDir, 'food.tpl');
  const foodTplContent = [
    'package test.pipeline;',
    '',
    'public class $$-classname {',
    '    //@@$$-init',
    '',
    '    public void eat() {',
    '        //@@$$-execute',
    '    }',
    '}',
    ''
  ].join('\n');
  await fs.promises.writeFile(foodTplPath, foodTplContent, 'utf8');

  // 注册文件，包含唯一标记 //@@foodregistry
  const registryPath = path.join(root, 'FoodRegistry.txt');
  const registryContent = [
    'Food Registry:',
    '//@@foodregistry',
    ''
  ].join('\n');
  await fs.promises.writeFile(registryPath, registryContent, 'utf8');

  // 额外资源文件，测试 paths 映射
  const assetsPath = path.join(root, 'Assets.txt');
  const assetsContent = [
    'Assets:',
    '//@@assets',
    ''
  ].join('\n');
  await fs.promises.writeFile(assetsPath, assetsContent, 'utf8');

  return { root, templatesDir, foodTplPath, registryPath, assetsPath };
}

/**
 * 工具：构建一个简单的“生成食物类 + 注册”的 task
 *
 * 使用：
 *  - 模板：tpl:food  => build/testpipeline/templates/food.tpl
 *  - 标记：filewithin:foodregistry / thisfile
 */
function createFoodTask(name, hunger) {
  return [
    // 从模板生成一个类文件，使用 tpl:food 映射到 food.tpl
    ['generate', 'tpl:food', name, {
      classname: name,
      hunger,
      init: `System.out.println("Init ${name}");`,
      execute: `System.out.println("Eat ${name}, hunger + ${hunger}");`
    }],

    // 在唯一的 //@@foodregistry 标记处插入注册代码
    ['insert', 'thisfile', 'foodregistry', `new ${name}(${hunger}),`]
    // 也可以写成 ['insert', 'filewithin:foodregistry', 'foodregistry', `new ${name}(${hunger}),`]
  ];
}

/**
 * Demo 1：最简单的单个任务流水线
 */
export async function demoSingleFood() {
  console.log('=== Demo 1: 单个食物生成 ===');
  const env = await ensureTestFiles();

  const foodJob = createJob('food')
    .and(() => createFoodTask('GoldenApple', 120))
    .withPaths({
      // 示意：可以给 job 定义自己的路径别名
      registry: path.join(env.root, 'FoodRegistry.txt')
    });

  const pipeline = createPipeline(
    foodJob,
    {
      name: 'demo-single-food',
      // pipeline 级路径映射（job 找不到时作为兜底）
      paths: {
        assets: env.assetsPath
      }
    }
  );

  await pipeline.compile(env.root);
  await pipeline.run();
}

/**
 * Demo 2：多个食物 + job 依赖
 */
export async function demoMultiFoodWithValidate() {
  console.log('=== Demo 2: 多个食物 + 校验任务 ===');
  const env = await ensureTestFiles();

  const validateJob = createJob('validate')
    .and([
      ['custom', 'validate-templates', async () => {
        // 简单检查模板文件是否存在
        try {
          await fs.promises.access(env.foodTplPath);
          console.log('✔ 模板存在:', env.foodTplPath);
        } catch {
          throw new Error(`模板不存在: ${env.foodTplPath}`);
        }
      }]
    ]);

  const foodJob = createJob('food', [], { parallel: true })
    .and(() => createFoodTask('GoldenApple', 120))
    .and(() => createFoodTask('SilverApple', 80))
    .and(() => createFoodTask('BronzeApple', 40))
    .after(validateJob)
    .withPaths({
      // 这里演示使用 job.paths 映射
      registry: env.registryPath
    });

  const pipeline = createPipeline(
    validateJob,
    foodJob,
    {
      name: 'demo-multi-food',
      paths: {
        assets: env.assetsPath
      }
    }
  );

  await pipeline.compile(env.root);
  await pipeline.run();
}

/**
 * Demo 3：多个 job，演示失败时只停止当前 job，后续依赖 job 不再执行
 */
export async function demoJobErrorIsolation() {
  console.log('=== Demo 3: Job 失败隔离 ===');
  const env = await ensureTestFiles();

  const okJob = createJob('ok-job')
    .and([
      ['custom', 'ok-step', async () => {
        console.log('OK job running...');
      }]
    ]);

  const failJob = createJob('fail-job')
    .and([
      ['custom', 'fail-step', async () => {
        console.log('Failing job...');
        throw new Error('expected failure in fail-job');
      }]
    ])
    .after(okJob);

  const afterJob = createJob('after-job')
    .and([
      ['custom', 'after-step', async () => {
        console.log('This should NOT run because depend job failed');
      }]
    ])
    .after(failJob);

  const pipeline = createPipeline(
    okJob,
    failJob,
    afterJob,
    { name: 'demo-job-error-isolation' }
  );

  await pipeline.compile(env.root);
  await pipeline.run();
}

/**
 * Demo 4：使用 filewithin:identifier 和 pipeline 级路径别名
 */
export function createCleanupTask() {
  return [
    // 在唯一的 //@@assets 标记处插入一行
    ['insert', 'assets', 'assets', 'CLEANUP_MARKER'],
    // 再在 filewithin:foodregistry 所在文件底部追加一个标记（这里用 insert + 自己的标记）
    ['insert', 'filewithin:foodregistry', 'cleanup', '// cleanup done']
  ];
}

export async function demoPathsAndFileWithin() {
  console.log('=== Demo 4: 路径映射 + filewithin ===');
  const env = await ensureTestFiles();

  const cleanupJob = createJob('cleanup')
    .and(() => createCleanupTask());

  const pipeline = createPipeline(
    cleanupJob,
    {
      name: 'demo-paths-filewithin',
      paths: {
        assets: env.assetsPath
      }
    }
  );

  await pipeline.compile(env.root);
  await pipeline.run();
}

/**
 * CLI 入口：
 *   node script/pipeline-demo.js demoSingleFood
 *   node script/pipeline-demo.js demoMultiFoodWithValidate
 *   node script/pipeline-demo.js demoJobErrorIsolation
 *   node script/pipeline-demo.js demoPathsAndFileWithin
 */
async function main() {
  const demos = {
    demoSingleFood,
    demoMultiFoodWithValidate,
    demoJobErrorIsolation,
    demoPathsAndFileWithin
  };

  const name = process.argv[2];

  if (!name || !demos[name]) {
    console.log('可用 Demo:');
    Object.keys(demos).forEach(key => console.log(' -', key));
    console.log('\n用法:');
    console.log('  node script/pipeline-demo.js <demoName>');
    process.exit(1);
  }

  try {
    await demos[name]();
    console.log('\n✔ demo 完成');
  } catch (err) {
    console.error('\n✗ demo 失败:', err);
    process.exit(1);
  }
}

// 仅在作为主脚本执行时运行 demo（ESM 环境下使用 fileURLToPath 判断）
const __filename = fileURLToPath(import.meta.url);
if (process.argv[1] && path.resolve(process.argv[1]) === __filename) {
  main().catch(err => {
    console.error('运行 demo 失败:', err);
    process.exit(1);
  });
}


