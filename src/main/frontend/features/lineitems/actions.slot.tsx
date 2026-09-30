import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { LineItemRow } from '../../slots/defs/lineItemRow';
import { lineItemsKey, removeLineItem, updateLineItem } from './queries';

// Per-line actions on an invoice — change this line, or remove it — filling the lineitem.row region
// (CLAUDE.md rule 3), so the line-item table never lists what goes at the end of a row. Both writes
// are mutations that invalidate ['lineitems', invoiceId] (rule 5): the list AND its derived total
// re-query themselves, so removing or changing a line updates the total for free. useState holds
// only the values the user is currently editing (rule 4). Carries the stable data-testid contract.
function LineItemActions({
  invoiceId,
  lineItemId,
  description,
  qty,
  unitPrice,
}: {
  invoiceId: number;
  lineItemId: number;
  description: string;
  qty: number;
  unitPrice: number;
}) {
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState(false);
  const [draftDescription, setDraftDescription] = useState('');
  const [draftQty, setDraftQty] = useState('');
  const [draftUnitPrice, setDraftUnitPrice] = useState('');
  const [error, setError] = useState('');

  const refresh = () => queryClient.invalidateQueries({ queryKey: lineItemsKey(invoiceId) });

  const remove = useMutation({
    mutationFn: () => removeLineItem(invoiceId, lineItemId),
    onSuccess: () => void refresh(),
  });

  const change = useMutation({
    mutationFn: (input: { description: string; qty: number; unitPrice: number }) =>
      updateLineItem(invoiceId, lineItemId, input),
    onSuccess: () => {
      void refresh();
      setEditing(false);
    },
  });

  function startEditing() {
    setDraftDescription(description);
    setDraftQty(String(qty));
    setDraftUnitPrice(String(unitPrice));
    setError('');
    setEditing(true);
  }

  if (!editing) {
    return (
      <td>
        <button
          type="button"
          data-testid={`lineitem-edit-${lineItemId}`}
          onClick={startEditing}
        >
          Change
        </button>
        <button
          type="button"
          data-testid={`lineitem-remove-${lineItemId}`}
          onClick={() => remove.mutate()}
        >
          Remove
        </button>
      </td>
    );
  }

  return (
    <td>
      <input
        data-testid={`lineitem-edit-description-${lineItemId}`}
        value={draftDescription}
        onChange={(event) => setDraftDescription(event.target.value)}
      />
      <input
        data-testid={`lineitem-edit-qty-${lineItemId}`}
        value={draftQty}
        onChange={(event) => setDraftQty(event.target.value)}
      />
      <input
        data-testid={`lineitem-edit-unitprice-${lineItemId}`}
        value={draftUnitPrice}
        onChange={(event) => setDraftUnitPrice(event.target.value)}
      />
      <button
        type="button"
        data-testid={`lineitem-save-${lineItemId}`}
        onClick={() => {
          const quantity = Number(draftQty);
          const price = Number(draftUnitPrice);
          if (
            draftDescription.trim() === '' ||
            !Number.isInteger(quantity) ||
            quantity <= 0 ||
            Number.isNaN(price) ||
            price <= 0
          ) {
            setError('Enter a description, a whole quantity and a price above zero.');
            return;
          }
          setError('');
          change.mutate({ description: draftDescription.trim(), qty: quantity, unitPrice: price });
        }}
      >
        Save
      </button>
      <button
        type="button"
        data-testid={`lineitem-cancel-${lineItemId}`}
        onClick={() => setEditing(false)}
      >
        Cancel
      </button>
      {error !== '' && <span data-testid={`lineitem-edit-error-${lineItemId}`}>{error}</span>}
    </td>
  );
}

export const contribution = LineItemRow.fill({ order: 0, Component: LineItemActions });
