import { pathToFileURL } from 'node:url';
import { resolve, isAbsolute } from 'node:path';
import { statSync, readdirSync } from 'node:fs';

/**
 * Load one or more config modules. Each module must default-export a plain
 * object whose keys are macrotype names and whose values are
 * `(args, ctx) => string` handlers. Handlers from later files override
 * earlier ones for the same macrotype name.
 *
 * @param {string} configPath  Absolute or cwd-relative path. Can be:
 *   - a single .js / .mjs file
 *   - a directory; every .js / .mjs file in it is loaded and merged
 * @returns {Promise<Record<string, (args: any, ctx: any) => string>>}
 */
export async function loadConfig(configPath) {
    const abs = isAbsolute(configPath)
        ? configPath
        : resolve(process.cwd(), configPath);

    const stat = statSync(abs);
    const files = stat.isDirectory()
        ? readdirSync(abs)
              .filter(f => f.endsWith('.js') || f.endsWith('.mjs'))
              .sort()
              .map(f => resolve(abs, f))
        : [abs];

    const merged = {};
    for (const file of files) {
        const mod = await import(pathToFileURL(file).href);
        const handlers = mod.default;
        if (!handlers || typeof handlers !== 'object') {
            throw new Error(
                `codegen: config at ${file} must default-export { [name]: (args, ctx) => string }`
            );
        }
        for (const [name, fn] of Object.entries(handlers)) {
            if (typeof fn !== 'function') {
                throw new Error(
                    `codegen: handler for '${name}' in ${file} must be a function`
                );
            }
        }
        Object.assign(merged, handlers);
    }
    return merged;
}
