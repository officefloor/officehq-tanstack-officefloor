import { formatMoney } from '../../ui/money';
import { InvoiceLineItemRow } from '../../slots/defs/invoiceLineItemRow';
import { useLineItems } from './lineItems';

// An invoice's line items and what they add up to. Reads its own query key (scoped to the invoice)
// and derives the invoice amount from the rows — the total is never stored on the client, so it
// always matches the list. Each row shows a description, how many, the price of each, and that
// line's own amount (qty * unit price). The table is always rendered, so an invoice with no lines
// yet still shows the region (and a $0.00 total) ready for the first line to be added.
export function LineItemList({ invoiceId }: { invoiceId: number }) {
  const { data: lineItems } = useLineItems(invoiceId);

  if (!lineItems) {
    return null;
  }

  const total = lineItems.reduce(
    (sum, line) => sum + Number(line.qty) * Number(line.unitPrice),
    0,
  );

  return (
    <table data-testid="invoice-lineitems-table">
      <thead>
        <tr>
          <th>Description</th>
          <th>Qty</th>
          <th>Unit</th>
          <th>Unit price</th>
          <th>Amount</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        {lineItems.map((line) => (
          <tr key={line.id} data-testid={`lineitem-row-${line.id}`}>
            <td data-testid="lineitem-description">{line.description}</td>
            <td data-testid="lineitem-qty">{line.qty}</td>
            <td data-testid="lineitem-unit">{line.unit}</td>
            <td data-testid="lineitem-unitprice">{formatMoney(line.unitPrice)}</td>
            <td data-testid="lineitem-amount">
              {formatMoney(Number(line.qty) * Number(line.unitPrice))}
            </td>
            <td>
              <InvoiceLineItemRow.Slot lineItem={line} />
            </td>
          </tr>
        ))}
      </tbody>
      <tfoot>
        <tr>
          <td>Total</td>
          <td></td>
          <td></td>
          <td></td>
          <td data-testid="invoice-amount">{formatMoney(total)}</td>
          <td></td>
        </tr>
      </tfoot>
    </table>
  );
}
