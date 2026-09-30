import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';

// A note as the server returns it: free text kept against a target (an invoice here), plus the ISO
// instant it was written. The notes API is polymorphic — a note names its target by
// targetType/targetId — so the same endpoint serves invoice notes as well as project notes. The
// server returns notes newest first.
export type Note = {
  id: number;
  targetType: string;
  targetId: number;
  text: string;
  at: string;
};

// An invoice's notes, rendered in the invoice-detail context: the list of that invoice's notes,
// newest first, plus a form to add one. Server data is read with useQuery under the ['notes'] key
// and filtered to this invoice (never copied into state, never hand-maintained — rule 5). Adding is a
// useMutation that invalidates ['notes'], so the list refetches itself with the new note on top. The
// only useState here is the text the user is currently typing (rule 4).
export function InvoiceNotesPanel({ invoiceId }: { invoiceId: number }) {
  const queryClient = useQueryClient();
  const notes = useQuery({
    queryKey: ['notes'],
    queryFn: () => getJson<Note[]>('/api/notes'),
  });

  const [text, setText] = useState('');

  const create = useMutation({
    mutationFn: () =>
      postJson<Note>('/api/notes', { targetType: 'invoice', targetId: invoiceId, text }),
    onSuccess: () => {
      setText('');
      void queryClient.invalidateQueries({ queryKey: ['notes'] });
    },
  });

  // Server returns notes newest first; keep that order and narrow to this invoice.
  const rows = (notes.data ?? []).filter(
    (n) => n.targetType === 'invoice' && n.targetId === invoiceId,
  );

  return (
    <section data-testid="invoice-notes">
      <form
        data-testid="note-form"
        onSubmit={(e) => {
          e.preventDefault();
          if (!text.trim()) {
            return;
          }
          create.mutate();
        }}
      >
        <input
          data-testid="note-form-text"
          placeholder="Write a note"
          value={text}
          onChange={(e) => setText(e.target.value)}
        />
        <button data-testid="note-form-submit" type="submit">
          Add note
        </button>
      </form>

      {rows.length === 0 ? (
        <p data-testid="invoice-notes-empty">No notes yet.</p>
      ) : (
        <ul data-testid="invoice-notes-list">
          {rows.map((n) => (
            <li key={n.id} data-testid={`note-row-${n.id}`}>
              <span data-testid="note-text">{n.text}</span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
