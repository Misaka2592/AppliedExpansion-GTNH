# AE2 Crafting Interface

This context names the item entries and states presented by the AE2 crafting interface in the client-only AppliedExpansion-GTNH add-on.

## Language

**Crafting Order**:
The collection of item entries presented for one AE2 crafting request.
_Avoid_: Order, job

**Item Entry**:
An item represented in a crafting order together with its amount and state.
_Avoid_: Item row, stack

**Item State**:
The status describing an item entry's current relationship to its crafting order.
_Avoid_: Sort group, category

**In-progress Item**:
An item entry that is currently being crafted.
_Avoid_: Active item, running item

**Pending Item**:
An item entry that still needs to be crafted but is not currently in progress.
_Avoid_: Queued item, waiting item

**Completed Item**:
An item entry whose required crafting work has finished.
_Avoid_: Finished item, crafted item

**Existing Item**:
An item entry whose required amount is already available to the crafting order.
_Avoid_: Available item, stored item
