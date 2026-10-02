import { useState } from 'react';
import { useCreateInvoiceNote, useInvoiceNotes } from './notes';

// An invoice's notes: a form to write one and the list of them, newest first. The server orders by
// the instant each note was written and stamps a new note with the current instant, so a written
// note appears on top. useState holds only the text the user is currently typing; on submit a
// non-empty note POSTs and invalidates the shared key, then the field is cleared. The row/field
// testids (note-row-*, note-text, note-form-*) match the project notes so notes read the same
// everywhere; only the panel's own testid names the invoice.
export function InvoiceNotes({ invoiceId }: { invoiceId: number }) {
  const { data: notes } = useInvoiceNotes(invoiceId);
  const create = useCreateInvoiceNote(invoiceId);
  const [text, setText] = useState('');

  if (!notes) {
    return null;
  }

  return (
    <section data-testid="invoice-notes">
      <h2>Notes</h2>
      <form
        data-testid="note-form"
        onSubmit={(event) => {
          event.preventDefault();
          if (text.trim().length === 0) {
            return;
          }
          create.mutate(
            { text: text.trim() },
            { onSuccess: () => setText('') },
          );
        }}
      >
        <input
          data-testid="note-form-text"
          placeholder="Write a note"
          value={text}
          onChange={(event) => setText(event.target.value)}
        />
        <button data-testid="note-form-submit" type="submit" disabled={create.isPending}>
          Add note
        </button>
      </form>
      {notes.length === 0 ? (
        <p data-testid="invoice-notes-empty">No notes yet.</p>
      ) : (
        <ul data-testid="invoice-notes-list">
          {notes.map((note) => (
            <li key={note.id} data-testid={`note-row-${note.id}`}>
              <span data-testid="note-text">{note.text}</span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
