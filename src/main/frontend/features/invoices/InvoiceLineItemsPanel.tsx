import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { money } from '../../ui/money';

// A line item as the server returns it: what is being charged for (description), how many (qty) and
// the price of each (unitPrice), plus the id of the invoice it belongs to. The line's own amount is
// qty * unitPrice; the invoice's total is the sum across its lines.
export type LineItem = {
  id: number;
  invoiceId: number;
  description: string;
  qty: number;
  unitPrice: number;
};

// An invoice's line items, rendered in the invoice-detail context: the list of that invoice's lines,
// their derived total, and a form to add one (description, qty, unit price). Server data is read
// with useQuery under the ['lineItems'] key and filtered to the invoice this panel is handed — never
// copied into state, never hand-maintained (rule 5). Adding is a useMutation that invalidates
// ['lineItems'] (so this list refetches) and ['invoices'] (so the invoice's amount elsewhere
// refreshes too, sharing the key rather than importing). The only useState is the fields the user is
// currently typing (rule 4).
export function InvoiceLineItemsPanel({ invoiceId }: { invoiceId: number }) {
  const queryClient = useQueryClient();
  const lineItems = useQuery({
    queryKey: ['lineItems'],
    queryFn: () => getJson<LineItem[]>('/api/lineitems'),
  });

  const [description, setDescription] = useState('');
  const [qty, setQty] = useState('');
  const [unitPrice, setUnitPrice] = useState('');

  const create = useMutation({
    mutationFn: () =>
      postJson<LineItem>('/api/lineitems', {
        invoiceId,
        description,
        qty: Number(qty),
        unitPrice: Number(unitPrice),
      }),
    onSuccess: () => {
      setDescription('');
      setQty('');
      setUnitPrice('');
      void queryClient.invalidateQueries({ queryKey: ['lineItems'] });
      void queryClient.invalidateQueries({ queryKey: ['invoices'] });
    },
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
          if (!description.trim() || !qty.trim() || !unitPrice.trim()) {
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
            <th>Unit price</th>
            <th>Amount</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((li) => (
            <tr key={li.id} data-testid={`lineitem-row-${li.id}`}>
              <td data-testid="lineitem-description">{li.description}</td>
              <td data-testid="lineitem-qty">{li.qty}</td>
              <td data-testid="lineitem-unitprice">{money(Number(li.unitPrice))}</td>
              <td data-testid="lineitem-amount">{money(Number(li.qty) * Number(li.unitPrice))}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
