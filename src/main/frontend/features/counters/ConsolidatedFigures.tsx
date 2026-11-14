import { useQuery } from '@tanstack/react-query';
import { currentAsOf, getJson, getLiveJson } from '../../api/http';
import { useClaims } from '../claims/claims';
import type { Claim } from '../claims/claims';
import { GRADES, liveHolds, usePeople } from '../people/people';
import type { Person } from '../people/people';
import { personDue, total, useRates } from '../rates/rates';
import type { Rate } from '../rates/rates';
import { sheetKey, useCurrentSheet } from '../session/session';
import type { SheetInfo } from '../session/session';
import { dueRules, useDueRules } from '../settings/settings';
import type { DueRules, Settings } from '../settings/settings';
import { sheetTotals, STATUSES } from './HeaderCounters';

/**
 * One sheet's figures: how many people, its due-total and overdue-total as that sheet shows them, per grade the
 * due-totals of the people on it (retired people left out) added up, and per status how many claims have it.
 */
type Figures = { people: number; due: string; overdue: string; grades: Record<string, string>; statuses: Record<string, number> };

/** A sheet's figures, worked out from its people, claims and rates by its rules. */
function sheetFigures(people: Person[], claims: Claim[], rates: Rate[], rules: DueRules): Figures {
  const current = people.filter((p) => !p.retired);
  const gradeDue = (g: string) =>
    total(
      current
        .filter((p) => p.grade === g)
        .map((p) => personDue(claims.filter((c) => c.personId === p.id), rates, p.grades, liveHolds(p, rules.today), rules)),
    );
  return {
    people: people.length,
    ...sheetTotals(people, claims, rates, rules),
    grades: Object.fromEntries(GRADES.map((g) => [g, gradeDue(g)])),
    statuses: Object.fromEntries(STATUSES.map((s) => [s, claims.filter((c) => c.status === s).length])),
  };
}

/** The query key for the figures of the sheet's descendants; under sheetKey, so re-loading the sheet re-reads them. */
export const descendantFiguresKey = (sheet: string) => [...sheetKey(sheet), 'descendant-figures'] as const;

/**
 * A sheet's figures, worked out from what the store holds for it now by its own today and rules; while the screen
 * shows a date, as the sheet stood at the end of that day, with that date as its today.
 */
async function figuresOf(sheet: string): Promise<Figures> {
  const path = `/api/sheets/${encodeURIComponent(sheet)}`;
  const [info, people, claims, rates, settings] = await Promise.all([
    getJson<SheetInfo>(path),
    getJson<Person[]>(`${path}/people`),
    getJson<Claim[]>(`${path}/claims`),
    getJson<Rate[]>(`${path}/rates`),
    getJson<Settings>(`${path}/settings`),
  ]);
  return sheetFigures(people, claims, rates, dueRules(settings, info.today, claims));
}

/**
 * The figures of each of the sheet's descendants (its children, theirs and so on), as they stand now, or as they
 * stood at the end of the date the screen shows.
 */
function useStoredFigures(asOf: string) {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: [...descendantFiguresKey(sheet), asOf],
    queryFn: async () => {
      const ids = await getLiveJson<string[]>(`/api/sheets/${encodeURIComponent(sheet)}/descendants`);
      return Promise.all(ids.map(figuresOf));
    },
    enabled: !!sheet,
  });
}

// The consolidated figures over the sheet and all its descendants: the people counted, and each sheet's
// due-total and overdue-total as that sheet shows them, added up; per grade, the due-totals of the people on it
// (retired people left out), and per status, the count of claims with it, each added up over the tree. A sheet with no children shows its own. While the
// screen shows the sheet as of a date, every sheet of the tree is taken as it stood at the end of that day.
export function ConsolidatedFigures() {
  const asOf = currentAsOf();
  const people = usePeople().data ?? [];
  const claims = useClaims().data ?? [];
  const rates = useRates().data ?? [];
  const rules = useDueRules();
  const stored = useStoredFigures(asOf).data ?? [];

  const all: Figures[] = [sheetFigures(people, claims, rates, rules), ...stored];
  return (
    <p data-testid="consolidated-figures">
      Consolidated — People: <strong data-testid="consolidated-people-count">{all.reduce((n, f) => n + f.people, 0)}</strong>
      {' · '}Due: <strong data-testid="consolidated-due-total">{total(all.map((f) => f.due))}</strong>
      {' · '}Overdue: <strong data-testid="consolidated-overdue-total">{total(all.map((f) => f.overdue))}</strong>
      {GRADES.map((g) => (
        <span key={g}>
          {' · '}
          {g}: <strong data-testid={`consolidated-grade-${g}-due-total`}>{total(all.map((f) => f.grades[g]))}</strong>
        </span>
      ))}
      {STATUSES.map((s) => (
        <span key={s}>
          {' · '}
          {s}: <strong data-testid={`consolidated-status-${s}-count`}>{all.reduce((n, f) => n + f.statuses[s], 0)}</strong>
        </span>
      ))}
    </p>
  );
}
