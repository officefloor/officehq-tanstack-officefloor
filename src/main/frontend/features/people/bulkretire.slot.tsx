import { useRef, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { getJson, putJson } from '../../api/http';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser, useSheetToday } from '../session/session';
import { claimsQueryKey } from '../claims/claims';
import { poolsQueryKey } from '../pools/pools';
import { peoplePath, peopleQueryKey, useSelectedPeople, useShowRetired, type Person } from './people';

// Retires every selected person from a date (blank: the sheet's today) with one button, in list order. Each is
// retired exactly as the row's retire does, so the server writes the same PERSON_RETIRED and rollup lines; one it
// refuses is left as it is. The count of people retired shows once all are done, and is gone while it runs; the
// selection is cleared.
function BulkRetire() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const today = useSheetToday();
  const [showRetired] = useShowRetired();
  const { selected, clear } = useSelectedPeople();
  const queryClient = useQueryClient();
  const [date, setDate] = useState('');
  const [result, setResult] = useState<{ retired: number; refused: number } | null>(null);
  const busy = useRef(false);
  if (!user || !sheet) return null;

  const run = async () => {
    if (busy.current) return;
    busy.current = true;
    setResult(null);
    try {
      let retired = 0;
      let refused = 0;
      try {
        const people = await queryClient.fetchQuery({
          queryKey: peopleQueryKey(sheet),
          queryFn: () => getJson<Person[]>(peoplePath(sheet)),
          staleTime: 0,
        });
        const order = people.filter((p) => (showRetired || p.retiredFrom === null) && selected.has(p.id));
        for (const p of order) {
          try {
            await putJson<Person>(`${peoplePath(sheet)}/${p.id}/retire`, { date: date.trim() }, user);
            retired++;
          } catch {
            refused++;
          }
        }
      } catch {
        refused++;
      }
      await queryClient.invalidateQueries({ queryKey: peopleQueryKey(sheet) });
      await queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet) });
      await queryClient.invalidateQueries({ queryKey: poolsQueryKey(sheet) });
      clear();
      setResult({ retired, refused });
    } finally {
      busy.current = false;
    }
  };

  return (
    <div>
      <input
        data-testid="bulk-retire-date"
        placeholder={today ?? 'Retire from (YYYY-MM-DD)'}
        value={date}
        onChange={(e) => { setDate(e.target.value); setResult(null); }}
      />{' '}
      <button type="button" data-testid="bulk-retire" onClick={() => void run()}>
        Retire selected people
      </button>
      {result && (
        <>
          {' '}Retired <span data-testid="bulk-retired">{result.retired}</span>
          {result.retired === 0 && result.refused > 0 && (
            <>{' '}<span data-testid="person-error">The people could not be retired.</span></>
          )}
        </>
      )}
    </div>
  );
}

export const contribution = HomeMain.fill({ order: 20.6, Component: BulkRetire });
