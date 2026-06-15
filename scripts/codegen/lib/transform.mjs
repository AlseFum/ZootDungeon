import { parseLine, formatArgs } from './parser.mjs';
import { interpolate } from './interpolate.mjs';

/**
 * Expand: header line -> START ... END block.
 * Collapse: START ... END -> single header line.
 *
 * Both operate per-file and return the modified text plus a counter
 * of how many macros were touched.
 */
export function transform(source, handlers, options = {}) {
    const lines = source.split(/\r?\n/);
    const mode = options.mode; // 'expand' | 'collapse'
    const out = [];
    let touched = 0;
    let i = 0;

    while (i < lines.length) {
        const parsed = parseLine(lines[i]);

        if (!parsed) {
            out.push(lines[i]);
            i++;
            continue;
        }

        if (parsed.kind === 'header') {
            if (mode !== 'expand') {
                // Collapse or check on a header: leave it alone, just emit.
                out.push(lines[i]);
                i++;
                continue;
            }
            const handler = handlers[parsed.name];
            if (!handler) {
                throw new Error(`codegen: no handler for macrotype '${parsed.name}' at line ${i + 1}`);
            }
            const ctx = makeCtx({
            name: parsed.name,
            args: parsed.args,
            line: i + 1,
            file: options.file,
            root: options.root
        });
            const body = String(handler(parsed.args, ctx) ?? '');
            const indented = body
                .split('\n')
                .map(l => l.length > 0 ? parsed.indent + l : l)
                .join('\n');
            const argsText = formatArgs(parsed.args);
            out.push(`${parsed.indent}//@${parsed.name} START${argsText ? ' ' + argsText : ''}`);
            if (indented.length > 0) out.push(indented);
            out.push(`${parsed.indent}//@${parsed.name} END`);
            touched++;
            i++;
            continue;
        }

        if (parsed.kind === 'start') {
            if (mode !== 'collapse') {
                // Expand on an already-expanded block: pass through as-is.
                out.push(lines[i]);
                i++;
                continue;
            }
            // Collapse: scan forward to matching END.
            const endIdx = findMatchingEnd(lines, i, parsed.name, parsed.indent);
            if (endIdx < 0) {
                throw new Error(`codegen: missing //@${parsed.name} END for START at line ${i + 1}`);
            }
            const argsText = formatArgs(parsed.args);
            out.push(`${parsed.indent}//@${parsed.name}${argsText ? ' ' + argsText : ''}`);
            touched++;
            i = endIdx + 1;
            continue;
        }

        if (parsed.kind === 'end') {
            if (mode !== 'collapse') {
                // Expand: stray END. Pass through; transform will surface a real error if unbalanced.
                out.push(lines[i]);
            }
            // Collapse: we should never see a free-floating END here because
            // start-matching consumed them. Skip just in case.
            i++;
            continue;
        }
    }

    return { text: out.join(detectLineEnding(source)), touched };
}

function findMatchingEnd(lines, startIdx, name, indent) {
    for (let j = startIdx + 1; j < lines.length; j++) {
        const p = parseLine(lines[j]);
        if (p && p.kind === 'end' && p.name === name && p.indent === indent) {
            return j;
        }
    }
    return -1;
}

function detectLineEnding(source) {
    return source.includes('\r\n') ? '\r\n' : '\n';
}

function makeCtx({ name, args, line, file, root }) {
    return {
        macrotype: name,
        args,
        line,
        file,
        root,
        interpolate
    };
}
