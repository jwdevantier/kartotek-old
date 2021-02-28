

## Development

### Start the Clojure Server
Either `make repl-clj` (then connect) or start from editor (e.g. emacs).
Then run `(system-start!)`.

*NOTE*: at this point, re-evaluate server.clj to include dev-only routes in HTTP server.

### Start the Clojurescript compiler
Either `make repl-cljs` or start from editor. Will trigger recompiles in response to changes.

### Start the CSS compiler
Run `make dev-css` - the compiler runs in the background and recompiles if necessary. See `postcss.config.js` for general settings and `tailwind.config.js` for TailwindCSS-specific settings.