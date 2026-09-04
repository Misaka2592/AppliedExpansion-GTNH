# Project guidance

## Scope

AppliedExpansion-GTNH is a client-only AE2 add-on for GTNH 2.9.0 beta1-beta2. Keep the current scope limited to item and fluid entry ordering in the AE2 crafting interface.

## Crafting interface ordering

- Treat each item or fluid entry's state as the primary ordering key.
- Use this default priority: in-progress entries, then pending entries, then completed and existing entries at equal priority.
- Within each priority group, preserve GTNH's original ordering, including quantity- and usage-ratio-based orderings. Apply the state priority as a stable grouping over that original order.

## Agent skills

### Issue tracker

Issues and specs are tracked in this repository's GitHub Issues. See `docs/agents/issue-tracker.md`.

### Triage labels

Use the default canonical triage labels. See `docs/agents/triage-labels.md`.

### Domain docs

This repository uses the single-context layout. See `docs/agents/domain.md`.
