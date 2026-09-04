# AE2 Crafting Interface

This context names the crafting entries and states presented by the AE2 crafting interface in the client-only AppliedExpansion-GTNH add-on.

## Language

**Crafting Order**:
The collection of crafting entries presented for one AE2 crafting request.
_Avoid_: Order, job

**Crafting Entry**:
An item or fluid represented in a crafting order together with its amount and state.
_Avoid_: Row, stack

**Item Entry**:
A crafting entry representing an item.
_Avoid_: Item row, item stack

**Fluid Entry**:
A crafting entry representing a fluid.
_Avoid_: Fluid row, fluid stack

**Entry State**:
The status describing a crafting entry's current relationship to its crafting order.
_Avoid_: Item state, fluid state, sort group, category

**In-progress Entry**:
A crafting entry that is currently being crafted.
_Avoid_: Active entry, running entry

**Pending Entry**:
A crafting entry that still needs to be crafted but is not currently in progress.
_Avoid_: Queued entry, waiting entry

**Completed Entry**:
A crafting entry whose required crafting work has finished.
_Avoid_: Finished entry, crafted entry

**Existing Entry**:
A crafting entry whose required amount is already available to the crafting order.
_Avoid_: Available entry, stored entry
