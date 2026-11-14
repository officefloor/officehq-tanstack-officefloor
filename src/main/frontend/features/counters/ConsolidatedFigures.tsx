import { useQuery } from '@tanstack/react-query';
import { useAliases } from '../aliases/aliases';
import type { Alias } from '../aliases/aliases';
import { currentAsOf, getJson, getLiveJson } from '../../api/http';
import { useClaims } from '../claims/claims';
import type { Claim } from '../claims/claims';
import { GRADES, liveHolds, usePeople } from '../people/people';
import type { Person } from '../people/people';
import { personOverdue } from '../overdue/overdue';
import { personDue, total, trimmedTotal, useRates } from '../rates/rates';
import type { Rate } from '../rates/rates';
import { sheetKey, useCurrentSheet } from '../session/session';
import type { SheetInfo } from '../session/session';
import { dueRules, useDueRules } from '../settings/settings';
import type { DueRules, Settings } from '../settings/settings';
import type { Pool } from '../pools/pools';
import { nameKey, STATUSES } from './HeaderCounters';

/**
 * One sheet's figures: each person's names (their name and aliases, as names are compared), each person's due-total
 * and overdue-total, per grade the due-totals of the people on it (retired people left out) added up, and per status
 * how many claims have it, and how many overdue claims count (as the rollup's overdue count).
 */
export type Figures = {
  names: string[][];
  dues: string[];
  overdues: string[];
  grades: Record<string, string>;
  statuses: Record<string, number>;
  overdueCount: number;
};

/** A sheet's figures, worked out from its people, aliases, claims and rates by its rules. */
export function sheetFigures(people: Person[], aliases: Alias[], claims: Claim[], rates: Rate[], rules: DueRules): Figures {
  const mine = (id: number) => claims.filter((c) => c.personId === id);
  const current = people.filter((p) => !p.retired);
  const gradeDue = (g: string) =>
    total(
      current
        .filter((p) => p.grade === g)
        .map((p) => personDue(claims.filter((c) => c.personId === p.id), rates, p.grades, liveHolds(p, rules.today), rules, { personId: p.id, claims, people })),
    );
  return {
    names: people.map((p) =>
      [p.name, ...aliases.filter((a) => a.personId === p.id).map((a) => a.alias)].map(nameKey).filter((k) => k),
    ),
    dues: people.map((p) => personDue(mine(p.id), rates, p.grades, liveHolds(p, rules.today), rules, { personId: p.id, claims, people })),
    overdues: people.map((p) => personOverdue(mine(p.id), rates, p.grades, liveHolds(p, rules.today), rules).total),
    grades: Object.fromEntries(GRADES.map((g) => [g, gradeDue(g)])),
    statuses: Object.fromEntries(STATUSES.map((s) => [s, claims.filter((c) => c.status === s).length])),
    overdueCount: people.reduce((n, p) => n + personOverdue(mine(p.id), rates, p.grades, liveHolds(p, rules.today), rules).count, 0),
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
  const [info, people, aliases, claims, rates, settings, pools] = await Promise.all([
    getJson<SheetInfo>(path),
    getJson<Person[]>(`${path}/people`),
    getJson<Alias[]>(`${path}/aliases`),
    getJson<Claim[]>(`${path}/claims`),
    getJson<Rate[]>(`${path}/rates`),
    getJson<Settings>(`${path}/settings`),
    getJson<Pool[]>(`${path}/pools`),
  ]);
  return sheetFigures(people, aliases, claims, rates, dueRules(settings, info.today, claims, pools));
}

/**
 * The figures of each of the sheet's descendants (its children, theirs and so on), as they stand now, or as they
 * stood at the end of the date the screen shows.
 */
export function useStoredFigures(asOf: string) {
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

/** How many people there are over the tree: people whose names or aliases match are one person. */
function distinctPeople(names: string[][]): number {
  const parent = names.map((_, i) => i);
  const root = (i: number): number => (parent[i] === i ? i : (parent[i] = root(parent[i])));
  const byName = new Map<string, number>();
  names.forEach((keys, i) =>
    keys.forEach((k) => {
      const j = byName.get(k);
      if (j === undefined) byName.set(k, i);
      else parent[root(i)] = root(j);
    }),
  );
  return new Set(names.map((_, i) => root(i))).size;
}

// The consolidated figures over the sheet and all its descendants: the people counted once over the tree (people
// whose names or aliases match are one), and the due-total and overdue-total of every person of the tree added up,
// trimmed once of the single highest and lowest over the whole tree; per grade, the due-totals of the people on it
// (retired people left out), and per status, the count of claims with it, each added up over the tree. A sheet with no children shows its own. While the
// screen shows the sheet as of a date, every sheet of the tree is taken as it stood at the end of that day.
export function ConsolidatedFigures() {
  const asOf = currentAsOf();
  const people = usePeople().data ?? [];
  const claims = useClaims().data ?? [];
  const rates = useRates().data ?? [];
  const rules = useDueRules();
  const aliases = useAliases().data ?? [];
  const stored = useStoredFigures(asOf).data ?? [];

  const all: Figures[] = [sheetFigures(people, aliases, claims, rates, rules), ...stored];
  return (
    <p data-testid="consolidated-figures">
      Consolidated — People: <strong data-testid="consolidated-people-count">{distinctPeople(all.flatMap((f) => f.names))}</strong>
      {' · '}Due: <strong data-testid="consolidated-due-total">{trimmedTotal(all.flatMap((f) => f.dues))}</strong>
      {' · '}Overdue: <strong data-testid="consolidated-overdue-total">{trimmedTotal(all.flatMap((f) => f.overdues))}</strong>
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
