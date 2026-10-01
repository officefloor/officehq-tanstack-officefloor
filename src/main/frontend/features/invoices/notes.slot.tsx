import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { InvoiceDetail } from '../../slots/defs/invoiceDetail';
import { createNote, listNotes, notesKey, type Note } from './notesApi';

// Notes the user writes on an invoice — one panel filling the invoice.detail region: the list of
// notes (newest first) above a form to add one. Mirrors the project notes panel against the same
// generic notes endpoint, scoped with targetType 'invoice'. Reads server data under
// ['notes', 'invoice', id] (never copied into state); the server returns the rows already
// newest-first, so the list renders them in order. What the user is currently typing lives in
// useState (uncommitted input); on a successful write we invalidate the same key so the list
// refetches with the new note on top — no hand-maintained list, no import between list and form.
function InvoiceNotes({ invoiceId }: { invoiceId: number }) {
  const targetType = 'invoice';
  const queryClient = useQueryClient();
  const [text, setText] = useState('');
  const { data: notes } = useQuery({
    queryKey: notesKey(targetType, invoiceId),
    queryFn: () => listNotes(targetType, invoiceId),
  });
  const mutation = useMutation({
    mutationFn: createNote,
    onSuccess: () => {
      setText('');
      void queryClient.invalidateQueries({ queryKey: notesKey(targetType, invoiceId) });
    },
  });

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    if (text.trim() === '') {
      return;
    }
    mutation.mutate({ targetType, targetId: invoiceId, text: text.trim() });
  };

  return (
    <section data-testid="invoice-notes">
      <ul data-testid="invoice-notes-list">
        {(notes ?? []).map((note: Note) => (
          <li key={note.id} data-testid={`note-row-${note.id}`}>
            <span data-testid="note-text">{note.text}</span>
          </li>
        ))}
      </ul>
      <form onSubmit={submit}>
        <input
          data-testid="note-form-text"
          placeholder="Write a note"
          value={text}
          onChange={(e) => setText(e.target.value)}
        />
        <button data-testid="note-form-submit" type="submit" disabled={mutation.isPending}>
          Add note
        </button>
      </form>
    </section>
  );
}

export const contribution = InvoiceDetail.fill({ order: 50, Component: InvoiceNotes });
