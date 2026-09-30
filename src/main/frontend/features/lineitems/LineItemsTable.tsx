import { useQuery } from '@tanstack/react-query';
import { lineItemsKey, fetchLineItems, type LineItem } from './queries';
import { formatMoney } from '../../ui/money';

// An invoice's line items plus the derived total. Queries for itself under ['lineitems', invoiceId]
// — never handed its data by a parent (CLAUDE.md rule 5). Each line's amount is qty * unitPrice; the
// invoice total is a client-side aggregate of the same rows, so it stays in step with them for free
// ("work out the total for me"). Carries the stable data-testid contract the test reads.
export function LineItemsTable({ invoiceId }: { invoiceId: number }) {
  const { data: lineItems } = useQuery({
    queryKey: lineItemsKey(invoiceId),
    queryFn: () => fetchLineItems(invoiceId),
  });

  if (!lineItems) {
    return null;
  }

  const total = lineItems.reduce(
    (sum, line) => sum + Number(line.qty) * Number(line.unitPrice),
    0,
  );

  return (
    <>
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
          {lineItems.map((line: LineItem) => (
            <tr key={line.id} data-testid={`lineitem-row-${line.id}`}>
              <td data-testid="lineitem-description">{line.description}</td>
              <td data-testid="lineitem-qty">{line.qty}</td>
              <td data-testid="lineitem-unitprice">{formatMoney(line.unitPrice)}</td>
              <td data-testid="lineitem-amount">
                {formatMoney(Number(line.qty) * Number(line.unitPrice))}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <p data-testid="invoice-amount">{formatMoney(total)}</p>
    </>
  );
}
