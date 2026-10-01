import { useQuery } from '@tanstack/react-query';
import { InvoiceDetail } from '../../slots/defs/invoiceDetail';
import {
  lineItemsKey,
  listLineItems,
  lineAmount,
  invoiceTotal,
  type LineItem,
} from './lineItemsApi';
import { formatMoney } from '../../ui/money';

// The invoice's line items and what they add up to — one panel filling the invoice.detail region.
// Reads server data under ['invoice-lineitems', invoiceId] (never copied into state); the add form
// shares the key, so a successful add refreshes this with no import between them. The total is
// DERIVED from the same query (never stored), and money renders with two decimals.
function InvoiceLineItems({ invoiceId }: { invoiceId: number }) {
  const { data: items } = useQuery({
    queryKey: lineItemsKey(invoiceId),
    queryFn: () => listLineItems(invoiceId),
  });

  if (!items) {
    return null;
  }

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
          {items.map((item: LineItem) => (
            <tr key={item.id} data-testid={`lineitem-row-${item.id}`}>
              <td data-testid="lineitem-description">{item.description}</td>
              <td data-testid="lineitem-qty">{item.qty}</td>
              <td data-testid="lineitem-unitprice">{formatMoney(item.unitPrice)}</td>
              <td data-testid="lineitem-amount">{formatMoney(lineAmount(item))}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <p data-testid="invoice-amount">{formatMoney(invoiceTotal(items))}</p>
    </>
  );
}

export const contribution = InvoiceDetail.fill({ order: 10, Component: InvoiceLineItems });
