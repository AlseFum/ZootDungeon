/**
 * Parser for //@<name> header / START / END lines.
 *
 * Recognized line shapes (leading whitespace is preserved separately):
 *
 *   //@<name> [--key val | -flag]...      (collapsed state, args optional)
 *   //@<name> START [--key val | -flag]... (expanded state, opener, args optional)
 *   //@<name> END                         (expanded state, closer)
 *
 * Tokenization rules:
 *   --key val  -> args.props.key = val (val is one whitespace-separated token)
 *   -flag      -> args.flags[flag] = true
 *   Anything else is an error (so typos like `---flag` don't get silently dropped).
 */

const HEADER_RE = /^(\s*)\/\/@(\w+)(?:\s+(.*?))?\s*$/;
const START_RE = /^(\s*)\/\/@(\w+)\s+START(?:\s+(.*?))?\s*$/;
const END_RE = /^(\s*)\/\/@(\w+)\s+END\s*$/;

export function parseLine(line) {
    let m;

    if ((m = line.match(END_RE))) {
        return { kind: 'end', indent: m[1], name: m[2] };
    }
    if ((m = line.match(START_RE))) {
        const argsText = (m[3] || '').trim();
        return { kind: 'start', indent: m[1], name: m[2], args: tokenizeArgs(argsText) };
    }
    if ((m = line.match(HEADER_RE))) {
        return { kind: 'header', indent: m[1], name: m[2], args: tokenizeArgs(m[3]) };
    }
    return null;
}

export function tokenizeArgs(text) {
    const props = {};
    const flags = {};
    if (!text) return { props, flags };

    // Simple whitespace split; quoted strings are not yet supported — args are
    // identifiers or short tokens by convention.
    const tokens = text.split(/\s+/).filter(Boolean);
    for (let i = 0; i < tokens.length; i++) {
        const t = tokens[i];
        if (t.startsWith('--')) {
            const key = t.slice(2);
            const val = tokens[++i];
            if (val === undefined) {
                throw new Error(`codegen: --${key} requires a value`);
            }
            props[key] = val;
        } else if (t.startsWith('-') && t.length > 1) {
            const flag = t.slice(1);
            if (flag.includes('=')) {
                const [k, v] = flag.split('=', 2);
                if (v === '') throw new Error(`codegen: -${k}= requires a value`);
                props[k] = v;
            } else {
                flags[flag] = true;
            }
        } else {
            throw new Error(`codegen: unexpected token '${t}' (use --key val or -flag)`);
        }
    }
    return { props, flags };
}

/** Serialize args back to a whitespace-joined string. */
export function formatArgs(args) {
    const out = [];
    for (const [k, v] of Object.entries(args.props)) {
        out.push(`--${k} ${v}`);
    }
    for (const [k, v] of Object.entries(args.flags)) {
        if (v === true) out.push(`-${k}`);
    }
    return out.join(' ');
}
