/**
 * Minimal string interpolation.
 *
 * Replaces `${path.to.value}` in a template with the corresponding
 * value from `vars`. Returns empty string for missing keys.
 *
 * This is intentionally tiny: it does NOT support conditionals, loops,
 * or any other DSL. Callers compose such behavior in plain JS and feed
 * the final string here if they want a final pass.
 *
 * Paths are dot-notation: `${a.b.c}` walks vars.a.b.c.
 * Escape a literal `${` with `$${`.
 */
export function interpolate(template, vars) {
    if (template == null) return '';
    return String(template).replace(/\$\$\{/g, '\u0000ESCAPED${').replace(/\$\{([^{}]+)\}/g, (_, path) => {
        const trimmed = path.trim();
        if (!trimmed) return '';
        const value = trimmed.split('.').reduce((acc, key) => (acc == null ? acc : acc[key]), vars);
        return value == null ? '' : String(value);
    }).replace(/\u0000ESCAPED\$\{/g, '${');
}
