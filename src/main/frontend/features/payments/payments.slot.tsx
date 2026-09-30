import { InvoiceDetail } from '../../slots/defs/invoiceDetail';
import { PaymentForm } from './PaymentForm';
import { PaymentsTable } from './PaymentsTable';

// The payments panel on an invoice's detail page — one new *.slot.tsx file filling the InvoiceDetail
// region (CLAUDE.md rule 3). Nothing existing is edited to add it. Shows what the client has paid and
// lets a new payment be recorded; the form and list query/mutate for themselves under the shared key.
function InvoicePaymentsPanel({ invoiceId }: { invoiceId: number }) {
  return (
    <section data-testid="invoice-payments">
      <PaymentForm invoiceId={invoiceId} />
      <PaymentsTable invoiceId={invoiceId} />
    </section>
  );
}

export const contribution = InvoiceDetail.fill({ order: 10, Component: InvoicePaymentsPanel });
