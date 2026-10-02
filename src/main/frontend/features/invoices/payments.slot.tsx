import { InvoiceDetail } from '../../slots/defs/invoiceDetail';
import { PaymentList } from './PaymentList';
import { PaymentForm } from './PaymentForm';

// The payments panel on the invoice detail page — its own file, filling the shared invoice.detail
// region. The detail route is not touched to add it beyond rendering the region (CLAUDE.md rule 3):
// it lists what a client has paid and offers a form to record another.
function InvoicePayments({ invoiceId }: { invoiceId: number }) {
  return (
    <section data-testid="invoice-payments">
      <h2>Payments</h2>
      <PaymentList invoiceId={invoiceId} />
      <PaymentForm invoiceId={invoiceId} />
    </section>
  );
}

export const contribution = InvoiceDetail.fill({ order: 10, Component: InvoicePayments });
