import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { clearRowError } from '../people/rowError';
import { useUser } from '../session/session';
import { clearSelectedPeople, useSelectedPeople } from '../bulkgrade/selection';

// Holds the ticked people together from a from-date ('' for the sheet's today) to an optional to-date.
// The server holds each in list order exactly as the row's hold would; the bar shows how many were
// held. The count is cleared while it runs. A refusal shows the error and changes nothing.
function BulkHold({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const selected = useSelectedPeople(sheetId);
  const [user] = useUser();
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [held, setHeld] = useState<number | null>(null);
  const [error, setError] = useState('');

  const once = useOnce();
  const apply = useMutation({
    mutationFn: (personIds: string[]) =>
      postJson<{ held: number }>(
        `/api/sheets/${encodeURIComponent(sheetId)}/people/bulk-hold`,
        { personIds, from: from.trim(), to: to.trim() },
        user,
      ),
    onSuccess: (r) => {
      setHeld(r.held);
      clearSelectedPeople(sheetId);
      void queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] });
    },
    onError: () => setError('The selected people could not be put on hold.'),
  });

  const press = () => {
    setHeld(null);
    setError('');
    const ids = [...selected];
    ids.forEach(clearRowError);
    once((done) => apply.mutate(ids, { onSettled: done }));
  };

  return (
    <section data-testid="bulk-hold-bar">
      <label>
        Hold from{' '}
        <input
          type="date"
          data-testid="bulk-hold-from"
          value={from}
          onChange={(e) => {
            setFrom(e.target.value);
            setError('');
          }}
        />
      </label>{' '}
      <label>
        to{' '}
        <input
          type="date"
          data-testid="bulk-hold-to"
          value={to}
          onChange={(e) => {
            setTo(e.target.value);
            setError('');
          }}
        />
      </label>{' '}
      <button type="button" data-testid="bulk-hold" onClick={press} disabled={apply.isPending}>
        Hold selected ({selected.size})
      </button>
      {held !== null && (
        <>
          {' '}Held <span data-testid="bulk-held">{held}</span>
        </>
      )}
      {error && (
        <p role="alert" data-testid="person-error">
          {error}
        </p>
      )}
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 7.6, Component: BulkHold });
