import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { useSheetToday } from '../sheet/useSheetToday';
import { peopleKey, peopleQuery } from './peopleQuery';
import type { Person } from './peopleQuery';
import { useSelectedPeople } from './useSelectedPeople';

const DATE = /^\d{4}-\d{2}-\d{2}$/;

/**
 * Gives every ticked person a hold from a from-date (blank: the sheet's today) to an optional to-date, one at
 * a time in list order, through the same hold the person's row adds, so each writes the same audit lines. A
 * person the server refuses is left as they were and not counted.
 */
function BulkHold({ user, sheetId }: { user: string; sheetId: string }) {
  const queryClient = useQueryClient();
  const people = useQuery(peopleQuery(sheetId)).data ?? [];
  const today = useSheetToday(sheetId);
  const [selected, setSelected] = useSelectedPeople();
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [error, setError] = useState('');
  const [held, setHeld] = useState<number | undefined>();
  const once = useSubmitOnce();

  const apply = useMutation({
    mutationFn: async ({ targets, f, t }: { targets: Person[]; f: string; t: string }): Promise<number> => {
      let changed = 0;
      for (const p of targets) {
        try {
          await putJson<Person>(
            `/api/sheets/${encodeURIComponent(sheetId)}/people/${p.id}/hold`,
            { from: f, to: t },
            user,
          );
          changed++;
        } catch {
          // refused: left as they were
        }
      }
      return changed;
    },
    onSuccess: async (n) => {
      setSelected([]);
      setFrom('');
      setTo('');
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
      setHeld(n);
    },
    onError: () => setError('The holds could not be added.'),
  });

  return (
    <section data-testid="bulk-hold-panel">
      <input
        data-testid="bulk-hold-from"
        placeholder={today || 'from yyyy-mm-dd'}
        value={from}
        onChange={(e) => {
          setFrom(e.target.value);
          setError('');
        }}
      />{' '}
      <input
        data-testid="bulk-hold-to"
        placeholder="to yyyy-mm-dd"
        value={to}
        onChange={(e) => {
          setTo(e.target.value);
          setError('');
        }}
      />{' '}
      <button
        type="button"
        data-testid="bulk-hold"
        disabled={apply.isPending}
        onClick={() => {
          setError('');
          setHeld(undefined);
          const f = from.trim();
          const t = to.trim();
          if ((f !== '' && !DATE.test(f)) || (t !== '' && !DATE.test(t))) {
            setError('The dates must be yyyy-mm-dd.');
            return;
          }
          if (f !== '' && t !== '' && t < f) {
            setError('The to-date is before the from-date.');
            return;
          }
          // In list order, whatever order they were ticked in.
          once(() => apply.mutateAsync({ targets: people.filter((p) => selected.includes(p.id)), f, t }));
        }}
      >
        Hold selected
      </button>
      {held !== undefined && (
        <>
          {' '}
          Held <span data-testid="bulk-held">{held}</span>
        </>
      )}
      {error && <span data-testid="person-error">{error}</span>}
    </section>
  );
}

export const contribution = SheetPanel.fill({
  order: 12.2,
  Component: ({ user, sheetId }) => <BulkHold key={sheetId} user={user} sheetId={sheetId} />,
});
