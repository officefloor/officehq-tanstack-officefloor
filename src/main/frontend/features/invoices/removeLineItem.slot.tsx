import { InvoiceLineItemRow } from '../../slots/defs/invoiceLineItemRow';
import type { LineItem } from './lineItems';
import { useRemoveLineItem } from './lineItems';

// "Take this line off the invoice" — a self-contained row action filling the line-item row region.
// The table is not edited to add it (CLAUDE.md rule 3); it fills the slot the table renders per row.
// Clicking removes the line through the shared mutation, which invalidates ['lineItems', invoiceId]
// so the list and its worked-out total re-read from the server — the total drops with no hand-count.
function RemoveLineItem({ lineItem }: { lineItem: LineItem }) {
  const remove = useRemoveLineItem(lineItem.invoiceId);
  return (
    <button
      type="button"
      data-testid={`lineitem-remove-${lineItem.id}`}
      disabled={remove.isPending}
      onClick={() => remove.mutate(lineItem.id)}
    >
      Remove
    </button>
  );
}

export const contribution = InvoiceLineItemRow.fill({ order: 10, Component: RemoveLineItem });
