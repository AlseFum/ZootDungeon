import { readFileSync, readdirSync } from 'node:fs';
import { join, relative, sep } from 'node:path';

/**
 * //@fastimport --class <ClassName> [-norewrite]
 *
 * Finds the unique .java file under ctx.root whose top-level
 * declaration matches the given class name, derives its fully-
 * qualified name from the package declaration, and emits
 *
 *     import <com.example.Foo>;
 *
 * at the marker site. If the import is already present in the
 * target file, the handler is a no-op (so expand is idempotent).
 *
 * Notes:
 *   - The scan reads every .java file under ctx.root. It is a
 *     best-effort lookup; performance is acceptable for source
 *     trees of a few thousand files.
 *   - "Class match" means a top-level `class` / `interface` /
 *     `enum` / `record` declaration whose simple name equals
 *     the requested class. Nested classes are NOT followed.
 *   - Package is parsed from a `package x.y.z;` line; if
 *     missing, the class is treated as the default package.
 */

const CLASS_DECL_RE = /^\s*public\s+(?:final\s+|abstract\s+)?(class|interface|enum|record)\s+([A-Z]\w*)\b/m;
const PACKAGE_RE = /^\s*package\s+([\w.]+)\s*;/;

/** Walk root, yielding absolute paths of every .java file. */
function* walkJava(root) {
    const stack = [root];
    while (stack.length) {
        const dir = stack.pop();
        let entries;
        try { entries = readdirSync(dir, { withFileTypes: true }); } catch { continue; }
        for (const e of entries) {
            const p = join(dir, e.name);
            if (e.isDirectory()) stack.push(p);
            else if (e.isFile() && e.name.endsWith('.java')) yield p;
        }
    }
}

/** Build a class-name -> absolute file path index for the source root. */
function indexClasses(root) {
    const index = new Map(); // simpleName -> array of { fqn, file }
    for (const file of walkJava(root)) {
        let text;
        try { text = readFileSync(file, 'utf8'); } catch { continue; }
        const pkgMatch = text.match(PACKAGE_RE);
        const pkg = pkgMatch ? pkgMatch[1] : '';
        const decl = text.match(CLASS_DECL_RE);
        if (!decl) continue;
        const simple = decl[2];
        const fqn = pkg ? `${pkg}.${simple}` : simple;
        const bucket = index.get(simple) ?? [];
        bucket.push({ fqn, file });
        index.set(simple, bucket);
    }
    return index;
}

/** True if ctx.file already has `import <fqn>;`. */
function hasImport(file, fqn) {
    let text;
    try { text = readFileSync(file, 'utf8'); } catch { return false; }
    const re = new RegExp(`^\\s*import\\s+${fqn.replace(/\./g, '\\.')}\\s*;`, 'm');
    return re.test(text);
}

const handlers = {
    'fastimport': (args, ctx) => {
        if (!ctx.root) {
            throw new Error('fastimport: ctx.root is missing (codegen did not pass it)');
        }
        const classes = Object.keys(args.props)
            .filter(k => k === 'class')
            .map(k => args.props[k]);
        if (classes.length === 0) {
            throw new Error('fastimport: at least one --class <Name> is required');
        }
        const index = indexClasses(ctx.root);
        const seen = new Set();
        const imports = [];
        const ambiguous = [];
        for (const name of classes) {
            if (seen.has(name)) continue;
            seen.add(name);
            const bucket = index.get(name);
            if (!bucket || bucket.length === 0) {
                throw new Error(`fastimport: no class named '${name}' under ${ctx.root}`);
            }
            if (bucket.length > 1) {
                const list = bucket
                    .map(b => relative(ctx.root, b.file).split(sep).join('/'))
                    .join(', ');
                ambiguous.push(`${name} -> [${list}]`);
                continue;
            }
            const fqn = bucket[0].fqn;
            if (!hasImport(ctx.file, fqn)) {
                imports.push(`import ${fqn};`);
            }
        }
        if (ambiguous.length) {
            throw new Error(`fastimport: ambiguous (use a more specific import) — ${ambiguous.join('; ')}`);
        }
        return imports.join('\n');
    }
};

export default handlers;
