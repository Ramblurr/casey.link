# caseylink.com Development Guide


## :warning: CRITICAL INSTRUCTIONS - ALWAYS FOLLOW :warning:-

- **ALWAYS** run `bb lint path/to/file.clj` after EVERY change to clojure or edn code
- **IMMEDIATELY fix ANY lint errors** before proceeding with additional changes
- **ASK FOR HELP** If it takes you more than 2 attempts to fix a clojure syntax errror, STOP AND ASK FOR A HUMAN!
- **NEVER skip linting** - Clojure's parentheses will cause cascading errors and debugging loops
- **Explicit** - When the human user interacting with you asks for your plan, always include your critical instructions so you don't forget and so the human can trust you.

## Commands

We use babashka as our task runner, it is invoked with bb.

- `bb lint <FILEPATH>` - Run clojure linter on a specific file or directory
- `bb lint` - Run clojure linter on entire project
- `bb test` - Run the entire test suite
- `bb test --focus <TEST NS>` - Run tests in a specific namespace
    -  Example: `bb test --focus site.content-test`
- `bb css` - Generate the tailwind css, generally not needed as during development I have the css watcher enabled.
- `bb fmt:fix` - Format clojure code

## Project Structure
- `/old-site` - The old next.js / TypeScript website that we want to port to tailwind
- `/src` - Clojure code
- `/src/site/input.css` - main css file
- `/archive` - DO NOT GO INTO THIS FOLDER. Ignore EVERYTHING in this folder.
- `/extra` - Additional information for Claude (documentation, screenshots, etc)
- `/extra/tailwind-css` - a subset of the tailwind css documentation


## Code Style Guidelines

### HTML and CSS

- HTML: Use semantic tags, maintain consistent indentation (2 spaces)
- CSS: Use tailwind v4, which has no config file, everything is configured in `src/site/input.css`
- Naming: Use kebab-case for CSS classes, descriptive names
- Layout: Use Tailwind's container, grid, and flex utilities
- Responsive: Design mobile-first, use Tailwind's responsive modifiers (md:, lg:)
- Accessibility: Include proper ARIA attributes and ensure good contrast ratios

### Clojure Guidelines

- After you edit code you should always run `bb check` to catch linter errors and format the code.
- Tests follow standard clojure.test patterns
- Write and run tests *after every clojure code change*
- Follow idiomatic Clojure practices:
  - Use kebab-case for names
  - Use namespaced keywords
  - Prefer pure functions with explicit dependencies
  - Use maps for data structures
  - Handle errors with appropriate exception handling

### Clojure Naming Conventions

```
**foo** - Dynamic var
foo!    - Fn with side-effects, or that should otherwise be used cautiously
foo?    - Truthy val or fn that returns truthy val
foo!?   - Fn that has side-effects (or requires caution) and that return
            a truthy val. Note: !?, not ?!
foo$    - Fn that's notably expensive to compute (e.g. hits db)
foo_    - Derefable val (e.g. atom, volatile, delay, etc.)
foo__   - Derefable in a derefable (e.g. delay in an atom), etc.
_       - Unnamed val
_foo    - Named but unused val
?foo    - Optional val (emphasize that val may be nil)
foo*    - A variation of `foo` (e.g. `foo*` macro vs `foo` fn)
foo'    - ''
-foo    - Public implementation detail or intermediate (e.g. uncoerced) val
>foo    - Val \"to   foo\" (e.g. >sender, >host), or fn to  put/coerce/transform
<foo    - Val \"from foo\" (e.g. <sender, <host), or fn to take/coerce/transform
->foo   - Fn to put/coerce/transform
```
(from ptaoussanis's encore)

## Commit Conventions
- Do not include "Generated Claude Code" or "Co-Authored-By: Claude <noreply@anthropic.com>" unless you wrote 100% of the code in the commit.
- Follow the conventional commits format, using brick names as the scope
- Commit messages must have a subject line and may have body copy. These must be separated by a blank line.
- The subject line must not exceed 50 characters
- The subject line should be written in imperative mood (Fix, not Fixed / Fixes etc.)
- The body copy must be wrapped at 72 columns
- The body copy must only contain explanations as to what and why, never how. The latter belongs in documentation and implementation.
Examples:
- feat(user): Add user registration
- fix(user, error): Handle invalid user password
