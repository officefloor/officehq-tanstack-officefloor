import { formatMoney } from '../../ui/money';
import { usePayments } from './payments';

// An invoice's recorded payments — what a client has paid, and when. Reads its own query key
// (scoped to the invoice). The table is always rendered, so an invoice with no payments yet still
// shows the region ready for the first payment to be recorded.
export function PaymentList({ invoiceId }: { invoiceId: number }) {
  const { data: payments } = usePayments(invoiceId);

  if (!payments) {
    return null;
  }

  return (
    <table data-testid="invoice-payments-table">
      <thead>
        <tr>
          <th>Amount</th>
          <th>Date</th>
        </tr>
      </thead>
      <tbody>
        {payments.map((payment) => (
          <tr key={payment.id} data-testid={`payment-row-${payment.id}`}>
            <td data-testid="payment-amount">{formatMoney(payment.amount)}</td>
            <td data-testid="payment-date">{payment.date}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
