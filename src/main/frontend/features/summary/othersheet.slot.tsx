import { useQueries } from '@tanstack/react-query';
import { useState } from 'react';
import { getJsonNow } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheet';
import { type Claim, useClaims } from '../claims/claims';
import { GRADES, gradeOn, type Person, usePeople } from '../people/people';
import { personDueTotal, sumDue, type Rate, useRates } from '../rates/rates';
import { floorOf, lateOf, type Settings, useCap, useFloor, useLate } from '../settings/settings';
import { type Sheet, useSheetToday } from '../sheet/sheet';

/** Per grade the due-totals of a sheet's people on it today added up (retired ones left out), as its summary
 *  shows them. */
function gradeTotals(people: Person[], today: string, due: (p: Person) => string): Record<string, string> {
  const active = people.filter((p) => p.retiredFrom === null);
  return Object.fromEntries(GRADES.map((g) => [g,
    sumDue(active.filter((p) => gradeOn(p, today) === g).map(due))]));
}

/** Reads another sheet as the store holds it now, fresh at each compare: its per-grade summary due-totals, or
 *  null while they load, or 'missing' for a sheet that does not exist. */
function useOtherGrades(otherId: string, run: number): Record<string, string> | null | 'missing' {
  const base = `/api/sheets/${encodeURIComponent(otherId)}`;
  const parts = ['', '/people', '/claims', '/rates', '/settings'];
  const results = useQueries({
    queries: parts.map((part) => ({
      queryKey: ['sheet', otherId, 'compare', part, run],
      queryFn: () => getJsonNow<unknown>(`${base}${part}`),
      enabled: otherId !== '',
      retry: false,
      staleTime: Infinity,
      gcTime: 0,
    })),
  });
  if (otherId === '') return null;
  if (results[0].isError) return 'missing';
  if (results.some((r) => r.isPending)) return null;
  if (results.some((r) => r.isError)) return 'missing';
  const [sheet, people, claims, rates, settings] = results.map((r) => r.data) as
    [Sheet, Person[], Claim[], Rate[], Settings];
  const today = sheet.today;
  const floor = floorOf(settings);
  const late = lateOf(settings, today, claims, people);
  return gradeTotals(people, today,
    (p) => personDueTotal(p, claims, rates, today, floor, late, settings?.cap ?? ''));
}

/** Compares this sheet with another, named by id: per grade of this sheet, <grade> <this sheet's summary
 *  due-total for it> <the other sheet's> (0.00 where the other has no one on it). No lines for a sheet that does
 *  not exist. */
function OtherSheetCompare({ sheetId }: { sheetId: string; user: string }) {
  const [entry, setEntry] = useState('');
  const [compared, setCompared] = useState({ id: '', run: 0 });
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const cap = useCap(sheetId);
  const other = useOtherGrades(compared.id, compared.run);
  const mine = gradeTotals(people, today,
    (p) => personDueTotal(p, claims, rates, today, floor, late, cap));

  return (
    <section>
      <h3>Compare with another sheet</h3>
      <input data-testid="compare-sheet" value={entry} onChange={(e) => setEntry(e.target.value)} />
      <button type="button" data-testid="compare"
        onClick={() => setCompared((c) => ({ id: entry.trim(), run: c.run + 1 }))}>
        Compare
      </button>
      {other !== null && other !== 'missing' && (
        <ul>
          {GRADES.map((g) => (
            <li key={g} data-testid="compare-line" style={{ whiteSpace: 'pre' }}>
              {`${g} ${mine[g]} ${other[g]}`}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 24.6, Component: OtherSheetCompare });
