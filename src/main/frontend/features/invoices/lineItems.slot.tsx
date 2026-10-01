import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { InvoiceDetail } from '../../slots/defs/invoiceDetail';
import {
  lineItemsKey,
  listLineItems,
  removeLineItem,
  updateLineItem,
  lineAmount,
  type LineItem,
} from './lineItemsApi';
import { formatMoney } from '../../ui/money';

// The invoice's line items — one panel filling the invoice.detail region (the money summary below it,
// subtotal / discount / total, is its own summary.slot.tsx). Reads server data under
// ['invoice-lineitems', invoiceId] (never copied into state); the add form shares the key, so a
// successful add refreshes this with no import between them. Each line can be CHANGED (inline) or
// REMOVED here: both are mutations that invalidate the same key so the list — and, by prefix, the
// summary's subtotal/total — refetch, plus ['invoices'] so the project's stored amount keeps step.
// Each line's amount is derived from the query (never stored), and money renders with two decimals.
// useState holds only what the user is currently typing into the row being edited.
function InvoiceLineItems({ invoiceId }: { invoiceId: number }) {
  const queryClient = useQueryClient();
  const { data: items } = useQuery({
    queryKey: lineItemsKey(invoiceId),
    queryFn: () => listLineItems(invoiceId),
  });

  const [editingId, setEditingId] = useState<number | null>(null);
  const [description, setDescription] = useState('');
  const [qty, setQty] = useState('');
  const [unit, setUnit] = useState('');
  const [unitPrice, setUnitPrice] = useState('');
  const [error, setError] = useState('');

  const refresh = () => {
    void queryClient.invalidateQueries({ queryKey: lineItemsKey(invoiceId) });
    void queryClient.invalidateQueries({ queryKey: ['invoices'] });
  };

  const remove = useMutation({
    mutationFn: removeLineItem,
    onSuccess: refresh,
  });

  const update = useMutation({
    mutationFn: updateLineItem,
    onSuccess: () => {
      setEditingId(null);
      setError('');
      refresh();
    },
  });

  if (!items) {
    return null;
  }

  const startEdit = (item: LineItem) => {
    setEditingId(item.id);
    setDescription(item.description);
    setQty(String(item.qty));
    setUnit(item.unit);
    setUnitPrice(String(item.unitPrice));
    setError('');
  };

  const cancelEdit = () => {
    setEditingId(null);
    setError('');
  };

  const saveEdit = (id: number) => {
    const qtyValue = Number(qty);
    const priceValue = Number(unitPrice);
    if (description.trim() === '') {
      setError('A description is required');
      return;
    }
    if (qty.trim() === '' || Number.isNaN(qtyValue) || qtyValue <= 0) {
      setError('Quantity must be greater than zero');
      return;
    }
    if (unitPrice.trim() === '' || Number.isNaN(priceValue) || priceValue < 0) {
      setError('Price each must be zero or more');
      return;
    }
    update.mutate({
      id,
      description: description.trim(),
      qty: qtyValue,
      unit: unit.trim() === '' ? 'units' : unit.trim(),
      unitPrice: priceValue,
    });
  };

  return (
    <>
      <table data-testid="invoice-lineitems-table">
        <thead>
          <tr>
            <th>Description</th>
            <th>Qty</th>
            <th>Unit</th>
            <th>Unit price</th>
            <th>Amount</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {items.map((item: LineItem) =>
            editingId === item.id ? (
              <tr key={item.id} data-testid={`lineitem-row-${item.id}`}>
                <td>
                  <input
                    data-testid={`lineitem-edit-description-${item.id}`}
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                  />
                </td>
                <td>
                  <input
                    data-testid={`lineitem-edit-qty-${item.id}`}
                    value={qty}
                    onChange={(e) => setQty(e.target.value)}
                  />
                </td>
                <td>
                  <input
                    data-testid={`lineitem-edit-unit-${item.id}`}
                    value={unit}
                    onChange={(e) => setUnit(e.target.value)}
                  />
                </td>
                <td>
                  <input
                    data-testid={`lineitem-edit-unitprice-${item.id}`}
                    value={unitPrice}
                    onChange={(e) => setUnitPrice(e.target.value)}
                  />
                </td>
                <td data-testid="lineitem-amount">
                  {formatMoney(Number(qty) * Number(unitPrice))}
                </td>
                <td>
                  <button
                    data-testid={`lineitem-save-${item.id}`}
                    type="button"
                    onClick={() => saveEdit(item.id)}
                  >
                    Save
                  </button>
                  <button
                    data-testid={`lineitem-cancel-${item.id}`}
                    type="button"
                    onClick={cancelEdit}
                  >
                    Cancel
                  </button>
                </td>
              </tr>
            ) : (
              <tr key={item.id} data-testid={`lineitem-row-${item.id}`}>
                <td data-testid="lineitem-description">{item.description}</td>
                <td data-testid="lineitem-qty">{item.qty}</td>
                <td data-testid="lineitem-unit">{item.unit}</td>
                <td data-testid="lineitem-unitprice">{formatMoney(item.unitPrice)}</td>
                <td data-testid="lineitem-amount">{formatMoney(lineAmount(item))}</td>
                <td>
                  <button
                    data-testid={`lineitem-edit-${item.id}`}
                    type="button"
                    onClick={() => startEdit(item)}
                  >
                    Edit
                  </button>
                  <button
                    data-testid={`lineitem-remove-${item.id}`}
                    type="button"
                    onClick={() => remove.mutate(item.id)}
                  >
                    Remove
                  </button>
                </td>
              </tr>
            ),
          )}
        </tbody>
      </table>
      {error && <p data-testid="lineitem-edit-error">{error}</p>}
    </>
  );
}

export const contribution = InvoiceDetail.fill({ order: 10, Component: InvoiceLineItems });
