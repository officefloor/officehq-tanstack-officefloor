import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { peopleKey, peopleQuery } from './peopleQuery';
import type { Person } from './peopleQuery';
import { useSelectedPeople } from './useSelectedPeople';

const DATE = /^\d{4}-\d{2}-\d{2}$/;

/**
 * Retires every ticked person from a date (blank: the sheet's today), one at a time in list order,
 * through the same retire the row's button takes, so each writes the same audit lines. A person the
 * server refuses is left as they were and not counted.
 */
function BulkRetire({ user, sheetId }: { user: string; sheetId: string }) {
  const queryClient = useQueryClient();
  const people = useQuery(peopleQuery(sheetId)).data ?? [];
  const [selected, setSelected] = useSelectedPeople();
  const [from, setFrom] = useState('');
  const [error, setError] = useState('');
  const [retired, setRetired] = useState<number | undefined>();
  const once = useSubmitOnce();

  const apply = useMutation({
    mutationFn: async (targets: Person[]): Promise<number> => {
      let changed = 0;
      for (const p of targets) {
        try {
          await putJson<Person>(
            `/api/sheets/${encodeURIComponent(sheetId)}/people/${p.id}/retire`,
            { from: from.trim() },
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
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
      setRetired(n);
    },
    onError: () => setError('The people could not be retired.'),
  });

  return (
    <section data-testid="bulk-retire-panel">
      <input
        data-testid="bulk-retire-date"
        placeholder="retire from yyyy-mm-dd"
        value={from}
        onChange={(e) => {
          setFrom(e.target.value);
          setError('');
        }}
      />{' '}
      <button
        type="button"
        data-testid="bulk-retire"
        disabled={apply.isPending}
        onClick={() => {
          setError('');
          setRetired(undefined);
          const f = from.trim();
          if (f !== '' && !DATE.test(f)) {
            setError('The date must be yyyy-mm-dd.');
            return;
          }
          // In list order, whatever order they were ticked in.
          once(() => apply.mutateAsync(people.filter((p) => selected.includes(p.id))));
        }}
      >
        Retire selected
      </button>
      {retired !== undefined && (
        <>
          {' '}
          Retired <span data-testid="bulk-retired">{retired}</span>
        </>
      )}
      {error && <span data-testid="person-error">{error}</span>}
    </section>
  );
}

export const contribution = SheetPanel.fill({
  order: 12.5,
  Component: ({ user, sheetId }) => <BulkRetire key={sheetId} user={user} sheetId={sheetId} />,
});
