import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

import { interpolate } from '../lib/interpolate.mjs';

const here = dirname(fileURLToPath(import.meta.url));

function loadData(rel) {
    const abs = resolve(here, '..', rel);
    return JSON.parse(readFileSync(abs, 'utf8'));
}

/**
 * Default config: expand/collapse handlers for project-specific macrotypes.
 *
 * Each handler receives:
 *   args = { props: { key: value }, flags: { name: true } }
 *   ctx  = { macrotype, args, line, file, interpolate }
 *
 * It returns the body string that goes between START and END lines.
 * Each line of the body will be re-indented to match the header's indent.
 */
const handlers = {
    /**
     * //@skill_sheet_def [--data <path>]
     *
     * Reads a JSON array of { name } objects and emits a `register(SKILL);`
     * call per entry. Demonstrates loading a JSON data source.
     */
    'skill_sheet_def': (args, ctx) => {
        const dataRel = args.props.data || 'data/skills.json';
        const skills = loadData(dataRel);
        if (!Array.isArray(skills)) {
            throw new Error(`skill_sheet_def: ${dataRel} must be a JSON array`);
        }
        return skills
            .map(s => `register(${s.name});`)
            .join('\n');
    },

    /**
     * //@log [--info <msg>] [--positive <msg>] [--negative <msg>]
     *       [--warn <msg>] [--highlight <msg>] [-newline]
     *
     * Generates GLog.* calls at the marker site. Each --<level> <msg>
     * pair becomes one line. Multiple --<level> entries on the same
     * marker are emitted in declaration order.
     *
     * Messages must be a single whitespace-free token; use underscores
     * or camelCase. (Quoted strings are not yet supported by the parser.)
     */
    'log': (args, ctx) => {
        const levelToMethod = {
            info: 'i',
            positive: 'p',
            negative: 'n',
            warn: 'w',
            warning: 'w',
            highlight: 'h'
        };
        const lines = [];
        for (const [key, msg] of Object.entries(args.props)) {
            const method = levelToMethod[key];
            if (!method) {
                throw new Error(
                    `log: unknown --${key} (use info/positive/negative/warn/warning/highlight)`
                );
            }
            lines.push(`GLog.${method}("${msg}");`);
        }
        if (args.flags.newline) {
            lines.push('GLog.newLine();');
        }
        if (lines.length === 0) {
            throw new Error('log: at least one --<level> <msg> or -newline is required');
        }
        return lines.join('\n');
    }
};

export default handlers;
