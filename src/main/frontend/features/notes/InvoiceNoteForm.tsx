import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { invoiceNotesKey, createNote } from './queries';

// Write a note on an invoice. useState holds only what the user is currently typing (CLAUDE.md rule
// 4); the write is a mutation that invalidates ['invoices', invoiceId, 'notes'] so the list refreshes
// itself with the new note on top — no hand-maintained list. Blank text is not submitted (the server
// enforces the same rule). Reuses the shared createNote mutation with an 'invoice' target.
export function InvoiceNoteForm({ invoiceId }: { invoiceId: number }) {
  const [text, setText] = useState('');
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: createNote,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: invoiceNotesKey(invoiceId) });
      setText('');
    },
  });

  return (
    <form
      data-testid="note-form"
      onSubmit={(event) => {
        event.preventDefault();
        if (text.trim().length === 0) {
          return;
        }
        mutation.mutate({ targetType: 'invoice', targetId: invoiceId, text });
      }}
    >
      <input
        data-testid="note-form-text"
        placeholder="Write a note"
        value={text}
        onChange={(event) => setText(event.target.value)}
      />
      <button data-testid="note-form-submit" type="submit">
        Add note
      </button>
    </form>
  );
}
