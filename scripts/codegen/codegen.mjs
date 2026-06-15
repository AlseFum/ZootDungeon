#!/usr/bin/env node
import { existsSync } from 'node:fs';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { dirname } from 'node:path';

import { loadConfig } from './lib/config.mjs';
import { walkJavaFiles, readText, writeText, fileHasMarker, relativeToCwd } from './lib/scanner.mjs';
import { transform } from './lib/transform.mjs';

const __dirname = dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = resolve(__dirname, '..', '..');

const USAGE = `Usage: codegen <command> [options]

Commands:
  expand                       Expand all //@<name> header lines.
  collapse                     Collapse all //@<name> START...END blocks.
  check [--expect=collapsed|expanded]
                               Verify current state; exit non-zero on mismatch.

Options:
  --root=<path>                Source root (default: core/src/main/java)
  --config=<path>              Handler config (default: scripts/codegen/config — directory auto-merged)
  --only=<glob-substring>      Only touch files whose path contains this substring
  --write                      Actually write files (default for expand/collapse)
  --check                      Don't write, just report (default for check)
  -h, --help                   Show this help
`;

function parseArgs(argv) {
    const out = { _: [] };
    for (const a of argv) {
        if (a === '-h' || a === '--help') out.help = true;
        else if (a === '--write') out.write = true;
        else if (a === '--check') out.write = false;
        else if (a.startsWith('--') && a.includes('=')) {
            const [k, v] = a.slice(2).split('=', 2);
            out[k] = v;
        } else {
            out._.push(a);
        }
    }
    return out;
}

async function main() {
    const argv = parseArgs(process.argv.slice(2));
    if (argv.help || argv._.length === 0) {
        process.stdout.write(USAGE);
        return;
    }
    const cmd = argv._[0];
    if (!['expand', 'collapse', 'check'].includes(cmd)) {
        process.stderr.write(`Unknown command: ${cmd}\n\n${USAGE}`);
        process.exit(2);
    }

    const root = resolve(REPO_ROOT, argv.root || 'core/src/main/java');
    const configPath = resolve(REPO_ROOT, argv.config || 'scripts/codegen/config');
    const only = argv.only || null;

    if (!existsSync(root)) {
        process.stderr.write(`codegen: source root not found: ${root}\n`);
        process.exit(2);
    }
    if (!existsSync(configPath)) {
        process.stderr.write(`codegen: config not found: ${configPath}\n`);
        process.exit(2);
    }

    const handlers = await loadConfig(configPath);
    const mode = cmd === 'check' ? null : cmd;
    const write = cmd === 'check' ? false : (argv.write !== false);
    const expect = argv.expect || null;

    let totalTouched = 0;
    let filesScanned = 0;
    let filesChanged = 0;
    let filesWithMarkers = 0;
    const mismatches = [];

    for (const file of walkJavaFiles(root)) {
        if (only) {
            const normalized = file.toLowerCase().replace(/\\/g, '/');
            const want = only.toLowerCase().replace(/\\/g, '/');
            if (!normalized.includes(want)) continue;
        }
        filesScanned++;
        const original = readText(file);
        if (!fileHasMarker(original)) continue;
        filesWithMarkers++;

        if (cmd === 'check') {
            checkFile(original, expect, file, mismatches);
            continue;
        }

        const { text, touched } = transform(original, handlers, { mode, file, root });
        totalTouched += touched;
        if (text === original) continue;
        if (write) writeText(file, text);
        filesChanged++;
        process.stdout.write(`${mode === 'expand' ? 'expanded' : 'collapsed'} ${touched} in ${relativeToCwd(file)}\n`);
    }

    if (cmd === 'check') {
        if (mismatches.length === 0) {
            process.stdout.write(`codegen check: OK (${filesWithMarkers} files with markers)\n`);
            return;
        }
        process.stderr.write(`codegen check: FAILED\n`);
        for (const m of mismatches) process.stderr.write(`  ${m}\n`);
        process.exit(1);
    }

    process.stdout.write(
        `\ncodegen ${cmd}: scanned ${filesScanned} files, ${filesWithMarkers} with markers, ${filesChanged} changed, ${totalTouched} macros touched.\n`
    );
}

function checkFile(text, expect, file, mismatches) {
    if (!expect) return; // without --expect, check just verifies well-formedness elsewhere
    const hasStart = /\/\/@\w+\s+START\b/.test(text);
    const hasHeader = /^\s*\/\/@\w+\s+(?!START|END)(\S*\s*)*$/m.test(text);
    if (expect === 'collapsed' && hasStart) {
        mismatches.push(`${relativeToCwd(file)}: expected collapsed, found START block`);
    } else if (expect === 'expanded' && hasHeader) {
        mismatches.push(`${relativeToCwd(file)}: expected expanded, found bare header`);
    }
}

main().catch(err => {
    process.stderr.write(`codegen: ${err.message}\n`);
    if (process.env.CODEGEN_DEBUG) console.error(err);
    process.exit(1);
});
