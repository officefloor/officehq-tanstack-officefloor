import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { money } from '../../ui/money';

// A line item as the server returns it: what is being charged for (description), how many (qty), the
// unit that quantity is measured in (unit — "hours", "days", "items"…) and the price of each
// (unitPrice), plus the id of the invoice it belongs to. The line's own amount is qty * unitPrice;
// the invoice's total is the sum across its lines.
export type LineItem = {
  id: number;
  invoiceId: number;
  description: string;
  qty: number;
  unit: string;
  unitPrice: number;
};

// The fields the user is currently typing into a line they are editing (rule 4): held in state only
// while an edit is open, keyed by the line's id so the row's inputs are controlled.
type Draft = { description: string; qty: string; unit: string; unitPrice: string };

// An invoice's line items, rendered in the invoice-detail context: the list of that invoice's lines,
// their derived total, and a form to add one (description, qty, unit price). Each existing line can
// be CHANGED (edit its fields in place) or REMOVED, and the invoice's total recomputes when it is.
// Server data is read with useQuery under the ['lineItems'] key and filtered to the invoice this
// panel is handed — never copied into state, never hand-maintained (rule 5). Every write is a
// useMutation that invalidates ['lineItems'] (so this list refetches) and ['invoices'] (so the
// invoice's amount elsewhere refreshes too, sharing the key rather than importing). The only useState
// is the add form and the line the user is currently editing (rule 4).
export function InvoiceLineItemsPanel({ invoiceId }: { invoiceId: number }) {
  const queryClient = useQueryClient();
  const lineItems = useQuery({
    queryKey: ['lineItems'],
    queryFn: () => getJson<LineItem[]>('/api/lineitems'),
  });

  const [description, setDescription] = useState('');
  const [qty, setQty] = useState('');
  const [unit, setUnit] = useState('');
  const [unitPrice, setUnitPrice] = useState('');

  // Which line is being edited and its uncommitted field values, or null when nothing is open.
  const [editingId, setEditingId] = useState<number | null>(null);
  const [draft, setDraft] = useState<Draft>({ description: '', qty: '', unit: '', unitPrice: '' });

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['lineItems'] });
    void queryClient.invalidateQueries({ queryKey: ['invoices'] });
  };

  const create = useMutation({
    mutationFn: () =>
      postJson<LineItem>('/api/lineitems', {
        invoiceId,
        description,
        qty: Number(qty),
        unit,
        unitPrice: Number(unitPrice),
      }),
    onSuccess: () => {
      setDescription('');
      setQty('');
      setUnit('');
      setUnitPrice('');
      invalidate();
    },
  });

  const edit = useMutation({
    mutationFn: (vars: { id: number } & Draft) =>
      postJson<LineItem>('/api/lineitems/edit', {
        id: vars.id,
        description: vars.description,
        qty: Number(vars.qty),
        unit: vars.unit,
        unitPrice: Number(vars.unitPrice),
      }),
    onSuccess: () => {
      setEditingId(null);
      invalidate();
    },
  });

  const remove = useMutation({
    mutationFn: (id: number) => postJson<unknown>('/api/lineitems/remove', { id }),
    onSuccess: invalidate,
  });

  const rows = (lineItems.data ?? []).filter((li) => li.invoiceId === invoiceId);
  const total = rows.reduce((sum, li) => sum + Number(li.qty) * Number(li.unitPrice), 0);

  return (
    <section data-testid="invoice-lineitems">
      <p data-testid="invoice-amount">{money(total)}</p>

      <form
        data-testid="lineitem-form"
        onSubmit={(e) => {
          e.preventDefault();
          if (!description.trim() || !qty.trim() || !unit.trim() || !unitPrice.trim()) {
            return;
          }
          create.mutate();
        }}
      >
        <input
          data-testid="lineitem-form-description"
          placeholder="Description"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
        <input
          data-testid="lineitem-form-qty"
          type="number"
          placeholder="Qty"
          value={qty}
          onChange={(e) => setQty(e.target.value)}
        />
        <input
          data-testid="lineitem-form-unit"
          placeholder="Unit"
          value={unit}
          onChange={(e) => setUnit(e.target.value)}
        />
        <input
          data-testid="lineitem-form-unitprice"
          type="number"
          step="0.01"
          placeholder="Unit price"
          value={unitPrice}
          onChange={(e) => setUnitPrice(e.target.value)}
        />
        <button data-testid="lineitem-form-submit" type="submit">
          Add line item
        </button>
      </form>

      <table data-testid="invoice-lineitems-table">
        <thead>
          <tr>
            <th>Description</th>
            <th>Qty</th>
            <th>Unit</th>
            <th>Unit price</th>
            <th>Amount</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {rows.map((li) =>
            editingId === li.id ? (
              <tr key={li.id} data-testid={`lineitem-row-${li.id}`}>
                <td>
                  <input
                    data-testid={`lineitem-edit-description-${li.id}`}
                    value={draft.description}
                    onChange={(e) => setDraft((d) => ({ ...d, description: e.target.value }))}
                  />
                </td>
                <td>
                  <input
                    data-testid={`lineitem-edit-qty-${li.id}`}
                    type="number"
                    value={draft.qty}
                    onChange={(e) => setDraft((d) => ({ ...d, qty: e.target.value }))}
                  />
                </td>
                <td>
                  <input
                    data-testid={`lineitem-edit-unit-${li.id}`}
                    value={draft.unit}
                    onChange={(e) => setDraft((d) => ({ ...d, unit: e.target.value }))}
                  />
                </td>
                <td>
                  <input
                    data-testid={`lineitem-edit-unitprice-${li.id}`}
                    type="number"
                    step="0.01"
                    value={draft.unitPrice}
                    onChange={(e) => setDraft((d) => ({ ...d, unitPrice: e.target.value }))}
                  />
                </td>
                <td data-testid="lineitem-amount">
                  {money(Number(draft.qty || 0) * Number(draft.unitPrice || 0))}
                </td>
                <td>
                  <button
                    data-testid={`lineitem-save-${li.id}`}
                    type="button"
                    onClick={() => {
                      if (
                        !draft.description.trim() ||
                        !draft.qty.trim() ||
                        !draft.unit.trim() ||
                        !draft.unitPrice.trim()
                      ) {
                        return;
                      }
                      edit.mutate({ id: li.id, ...draft });
                    }}
                  >
                    Save
                  </button>
                  <button
                    data-testid={`lineitem-cancel-${li.id}`}
                    type="button"
                    onClick={() => setEditingId(null)}
                  >
                    Cancel
                  </button>
                </td>
              </tr>
            ) : (
              <tr key={li.id} data-testid={`lineitem-row-${li.id}`}>
                <td data-testid="lineitem-description">{li.description}</td>
                <td data-testid="lineitem-qty">{li.qty}</td>
                <td data-testid="lineitem-unit">{li.unit}</td>
                <td data-testid="lineitem-unitprice">{money(Number(li.unitPrice))}</td>
                <td data-testid="lineitem-amount">
                  {money(Number(li.qty) * Number(li.unitPrice))}
                </td>
                <td>
                  <button
                    data-testid={`lineitem-edit-${li.id}`}
                    type="button"
                    onClick={() => {
                      setEditingId(li.id);
                      setDraft({
                        description: li.description,
                        qty: String(li.qty),
                        unit: li.unit,
                        unitPrice: String(li.unitPrice),
                      });
                    }}
                  >
                    Edit
                  </button>
                  <button
                    data-testid={`lineitem-remove-${li.id}`}
                    type="button"
                    onClick={() => remove.mutate(li.id)}
                  >
                    Remove
                  </button>
                </td>
              </tr>
            ),
          )}
        </tbody>
      </table>
    </section>
  );
}
