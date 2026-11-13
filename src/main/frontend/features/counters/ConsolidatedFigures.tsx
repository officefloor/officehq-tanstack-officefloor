import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useClaims } from '../claims/claims';
import type { Claim } from '../claims/claims';
import { usePeople } from '../people/people';
import type { Person } from '../people/people';
import { total, useRates } from '../rates/rates';
import type { Rate } from '../rates/rates';
import { sheetKey, useCurrentSheet } from '../session/session';
import type { SheetInfo } from '../session/session';
import { dueRules, useDueRules } from '../settings/settings';
import type { Settings } from '../settings/settings';
import { sheetTotals } from './HeaderCounters';

/** One sheet's figures: how many people, its due-total and overdue-total as that sheet shows them. */
type Figures = { people: number; due: string; overdue: string };

/** The query key for the figures of the sheet's descendants; under sheetKey, so re-loading the sheet re-reads them. */
export const descendantFiguresKey = (sheet: string) => [...sheetKey(sheet), 'descendant-figures'] as const;

/** A descendant's figures, worked out from what the store holds for it, by its own today and rules. */
async function figuresOf(sheet: string): Promise<Figures> {
  const path = `/api/sheets/${encodeURIComponent(sheet)}`;
  const [info, people, claims, rates, settings] = await Promise.all([
    getJson<SheetInfo>(path),
    getJson<Person[]>(`${path}/people`),
    getJson<Claim[]>(`${path}/claims`),
    getJson<Rate[]>(`${path}/rates`),
    getJson<Settings>(`${path}/settings`),
  ]);
  return { people: people.length, ...sheetTotals(people, claims, rates, dueRules(settings, info.today, claims)) };
}

/** The figures of each of the sheet's descendants (its children, theirs and so on). */
function useDescendantFigures() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: descendantFiguresKey(sheet),
    queryFn: async () => {
      const ids = await getJson<string[]>(`/api/sheets/${encodeURIComponent(sheet)}/descendants`);
      return Promise.all(ids.map(figuresOf));
    },
    enabled: !!sheet,
  });
}

// The consolidated figures over the sheet and all its descendants: the people counted, and each sheet's
// due-total and overdue-total as that sheet shows them, added up. A sheet with no children shows its own.
export function ConsolidatedFigures() {
  const people = usePeople().data ?? [];
  const claims = useClaims().data ?? [];
  const rates = useRates().data ?? [];
  const rules = useDueRules();
  const descendants = useDescendantFigures().data ?? [];

  const all: Figures[] = [{ people: people.length, ...sheetTotals(people, claims, rates, rules) }, ...descendants];
  return (
    <p data-testid="consolidated-figures">
      Consolidated — People: <strong data-testid="consolidated-people-count">{all.reduce((n, f) => n + f.people, 0)}</strong>
      {' · '}Due: <strong data-testid="consolidated-due-total">{total(all.map((f) => f.due))}</strong>
      {' · '}Overdue: <strong data-testid="consolidated-overdue-total">{total(all.map((f) => f.overdue))}</strong>
    </p>
  );
}
