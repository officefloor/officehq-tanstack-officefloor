import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { InvoiceDetail } from '../../slots/defs/invoiceDetail';
import { invoiceStatus } from '../../ui/invoiceStatus';
import type { Invoice } from './InvoicesPanel';

// The invoice's status on its detail page — its own file, filling the invoice-detail slot above the
// line items. It is worked out from the payments, not stored: this panel queries for itself
// (['invoices'], the SAME key the amount-due cell owns) and derives the status from the invoice's
// amount and amountDue. Recording a payment invalidates ['invoices'], so the status refreshes from
// SENT to PARTIAL to PAID as the invoice is paid off. Carries data-testid="invoice-status".
export const contribution = InvoiceDetail.fill({
  order: 5,
  Component: ({ invoiceId }: { invoiceId: number }) => {
    const invoices = useQuery({
      queryKey: ['invoices'],
      queryFn: () => getJson<Invoice[]>('/api/invoices'),
    });
    const invoice = (invoices.data ?? []).find((i) => i.id === invoiceId);
    return (
      <section data-testid="invoice-status-panel">
        Status: <span data-testid="invoice-status">{invoice ? invoiceStatus(invoice) : ''}</span>
      </section>
    );
  },
});
