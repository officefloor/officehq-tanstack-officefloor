import { useQuery } from '@tanstack/react-query';
import { paymentsKey, fetchPayments, type Payment } from './queries';
import { formatMoney } from '../../ui/money';

// An invoice's payments — what the client has paid and when. Queries for itself under
// ['payments', invoiceId] — never handed its data by a parent (CLAUDE.md rule 5). Carries the stable
// data-testid contract the test reads.
export function PaymentsTable({ invoiceId }: { invoiceId: number }) {
  const { data: payments } = useQuery({
    queryKey: paymentsKey(invoiceId),
    queryFn: () => fetchPayments(invoiceId),
  });

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
        {payments.map((payment: Payment) => (
          <tr key={payment.id} data-testid={`payment-row-${payment.id}`}>
            <td data-testid="payment-amount">{formatMoney(payment.amount)}</td>
            <td data-testid="payment-date">{payment.date}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
