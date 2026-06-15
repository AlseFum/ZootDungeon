import { readdirSync, readFileSync, statSync, writeFileSync } from 'node:fs';
import { join, relative } from 'node:path';

/**
 * Recursively walk a directory and yield .java file paths.
 * `root` is the start directory; the result is an absolute path.
 */
export function* walkJavaFiles(root) {
    const stack = [root];
    while (stack.length) {
        const dir = stack.pop();
        let entries;
        try {
            entries = readdirSync(dir, { withFileTypes: true });
        } catch {
            continue;
        }
        for (const e of entries) {
            const p = join(dir, e.name);
            if (e.isDirectory()) {
                stack.push(p);
            } else if (e.isFile() && e.name.endsWith('.java')) {
                yield p;
            }
        }
    }
}

/** Read a file as utf-8 text. */
export function readText(path) {
    return readFileSync(path, 'utf8');
}

/** Write a file as utf-8 text. */
export function writeText(path, text) {
    writeFileSync(path, text, 'utf8');
}

/** True if `path` looks like our macro markers — used by --check. */
export function fileHasMarker(text) {
    return /\/\/@\w+/.test(text);
}

export function relativeToCwd(path) {
    return relative(process.cwd(), path);
}
