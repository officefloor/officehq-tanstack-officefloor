import { InvoiceDetail } from '../../slots/defs/invoiceDetail';
import { InvoiceNotesTable } from './InvoiceNotesTable';
import { InvoiceNoteForm } from './InvoiceNoteForm';

// The notes panel on an invoice's detail page — one new *.slot.tsx file filling the InvoiceDetail
// region (CLAUDE.md rule 3). Nothing existing is edited to add it. Shows the invoice's notes newest
// first and the form to add one; each queries/mutates for itself under the shared notes key. Notes
// were already generic over their target, so this is a second target alongside projects, not new
// note machinery.
function InvoiceNotesPanel({ invoiceId }: { invoiceId: number }) {
  return (
    <section data-testid="invoice-notes">
      <InvoiceNoteForm invoiceId={invoiceId} />
      <InvoiceNotesTable invoiceId={invoiceId} />
    </section>
  );
}

export const contribution = InvoiceDetail.fill({ order: 20, Component: InvoiceNotesPanel });
