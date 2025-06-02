# caseylink.com Development Guide

# Project Structure

- `/old-site` - The old next.js / TypeScript website that we want to port to tailwind
- `/src` - Clojure code
- `/src/site/input.css` - main css file
- `/archive` - DO NOT GO INTO THIS FOLDER. Ignore EVERYTHING in this folder.
- `/extra` - Additional information for Claude (documentation, screenshots, etc)
- `/extra/tailwind-css` - a subset of the tailwind css documentation

# Coding Rules

## HTML and CSS

- HTML: Use semantic tags, maintain consistent indentation (2 spaces)
- CSS: Use tailwind v4, which has no config file, everything is configured in `src/site/input.css`
- Naming: Use kebab-case for CSS classes, descriptive names
- Layout: Use Tailwind's container, grid, and flex utilities
- Responsive: Design mobile-first, use Tailwind's responsive modifiers (md:, lg:)
- Accessibility: Include proper ARIA attributes and ensure good contrast ratios

@~/.claude/CLAUDE.clojure.md

## Development Commands

- Execute the main function in `user.clj` with `bb runner`

@~/.claude/CLAUDE.clojure.commands.md

# Reference Documentation

The `extra/` directory contains valuable reference materials:

- **extra/tailwind-css/**: Documentation excerpts from tailwind


@~/.claude/CLAUDE.commit.md
