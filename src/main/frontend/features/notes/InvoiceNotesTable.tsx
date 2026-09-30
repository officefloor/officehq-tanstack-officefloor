import { useQuery } from '@tanstack/react-query';
import { invoiceNotesKey, fetchInvoiceNotes, type Note } from './queries';

// The notes written on one invoice. Queries for itself under ['invoices', invoiceId, 'notes'] —
// never handed its data by a parent (CLAUDE.md rule 5). The server returns them newest first, so the
// rows render in that order untouched. Carries the same stable note data-testid contract as the
// project notes table: a row per note and the text cell the test reads.
export function InvoiceNotesTable({ invoiceId }: { invoiceId: number }) {
  const { data: notes } = useQuery({
    queryKey: invoiceNotesKey(invoiceId),
    queryFn: () => fetchInvoiceNotes(invoiceId),
  });

  if (!notes) {
    return null;
  }

  return (
    <ul data-testid="invoice-notes-list">
      {notes.map((note: Note) => (
        <li key={note.id} data-testid={`note-row-${note.id}`}>
          <span data-testid="note-text">{note.text}</span>
          <time data-testid="note-at" dateTime={note.at}>
            {note.at}
          </time>
        </li>
      ))}
    </ul>
  );
}
