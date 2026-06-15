# codegen

Macro expander/collapser for ZootDungeon source comments.

## Markers

```java
//@<name> [--key val | -flag]...        // collapsed
//@<name> START [--key val | -flag]...   // expanded opener
   <generated body, indented to match>
//@<name> END                             // expanded closer
```

`--key val` puts `val` into `args.props.key`.
`-flag` puts `true` into `args.flags.flag`.

## Commands

```bash
node scripts/codegen/codegen.mjs expand
node scripts/codegen/codegen.mjs collapse
node scripts/codegen/codegen.mjs check --expect=expanded
node scripts/codegen/codegen.mjs check --expect=collapsed
```

## Adding a new macrotype

1. Add a handler to `config/skills.js`:
   ```js
   'my_macro': (args, ctx) => `/* body using ${args.props.x} */`
   ```
2. Place a `//@my_macro` line in any `.java` file under `core/src/main/java/`.
3. Run `expand` to materialize, `collapse` to revert.
