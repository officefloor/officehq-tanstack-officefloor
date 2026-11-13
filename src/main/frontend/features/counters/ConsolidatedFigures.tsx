import { useQuery } from '@tanstack/react-query';
import { currentAsOf, getLiveJson } from '../../api/http';
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

/** The query key for the figures of the sheet's descendants (and, as of a date, its own); under sheetKey, so re-loading the sheet re-reads them. */
export const descendantFiguresKey = (sheet: string) => [...sheetKey(sheet), 'descendant-figures'] as const;

/** A sheet's figures, worked out from what the store holds for it now, by its own today and rules. */
async function figuresOf(sheet: string): Promise<Figures> {
  const path = `/api/sheets/${encodeURIComponent(sheet)}`;
  const [info, people, claims, rates, settings] = await Promise.all([
    getLiveJson<SheetInfo>(path),
    getLiveJson<Person[]>(`${path}/people`),
    getLiveJson<Claim[]>(`${path}/claims`),
    getLiveJson<Rate[]>(`${path}/rates`),
    getLiveJson<Settings>(`${path}/settings`),
  ]);
  return { people: people.length, ...sheetTotals(people, claims, rates, dueRules(settings, info.today, claims)) };
}

/**
 * The figures of each of the sheet's descendants (its children, theirs and so on), as they stand now; while the
 * screen shows the sheet as of a date, the sheet's own figures as they stand now come first.
 */
function useStoredFigures(asOf: string) {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: [...descendantFiguresKey(sheet), asOf],
    queryFn: async () => {
      const ids = await getLiveJson<string[]>(`/api/sheets/${encodeURIComponent(sheet)}/descendants`);
      return Promise.all((asOf ? [sheet, ...ids] : ids).map(figuresOf));
    },
    enabled: !!sheet,
  });
}

// The consolidated figures over the sheet and all its descendants: the people counted, and each sheet's
// due-total and overdue-total as that sheet shows them, added up. A sheet with no children shows its own. They are
// the figures as the store holds them now, even while the screen shows the sheet as of a date.
export function ConsolidatedFigures() {
  const asOf = currentAsOf();
  const people = usePeople().data ?? [];
  const claims = useClaims().data ?? [];
  const rates = useRates().data ?? [];
  const rules = useDueRules();
  const stored = useStoredFigures(asOf).data ?? [];

  const all: Figures[] = asOf ? stored : [{ people: people.length, ...sheetTotals(people, claims, rates, rules) }, ...stored];
  return (
    <p data-testid="consolidated-figures">
      Consolidated — People: <strong data-testid="consolidated-people-count">{all.reduce((n, f) => n + f.people, 0)}</strong>
      {' · '}Due: <strong data-testid="consolidated-due-total">{total(all.map((f) => f.due))}</strong>
      {' · '}Overdue: <strong data-testid="consolidated-overdue-total">{total(all.map((f) => f.overdue))}</strong>
    </p>
  );
}
