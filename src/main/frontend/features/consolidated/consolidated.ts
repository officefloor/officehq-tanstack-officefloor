import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import type { Claim, ClaimStatus } from '../claims/claims';
import { GRADES, liveHolds } from '../people/people';
import type { Grade, Person } from '../people/people';
import { heldParts, personDueTotal, personSplitTotals, sumDue, trimmedSumDue } from '../rates/rates';
import type { Dated, Late, Rate } from '../rates/rates';
import { personOverdue } from '../overdue/overdue';
import { capOf, floorOf, lateOf } from '../access/access';
import type { Settings } from '../access/access';
import type { Sheet } from '../sheet/sheet';
import { cutTotal } from '../pools/pools';
import type { Pool } from '../pools/pools';

/** Every status the consolidated figures count. */
export const STATUSES: ClaimStatus[] = ['DRAFT', 'OPEN', 'PENDING', 'SETTLED', 'VOID', 'REVIEW'];

/**
 * A sheet's own figures, as its header shows them: its people, its due-total and its overdue-total;
 * and, as its summary shows them, per grade the due-totals of the people on it (retired people left
 * out) added up, and per status how many claims have it.
 */
export type Figures = {
  people: number;
  dueTotal: string;
  overdueTotal: string;
  gradeDueTotals: Record<Grade, string>;
  statusCounts: Record<string, number>;
  /** Each person's names (their name and aliases, as names are compared). */
  names: string[][];
  /** Each person's due-total, in the order of `names`. */
  dues: string[];
  /** Each person's overdue-total, in the order of `names`. */
  overdues: string[];
  /** Each person's settled-total, in the order of `names`. */
  settleds: string[];
  /** Each person's open-total, in the order of `names`. */
  opens: string[];
  /** Each person's cut total, in the order of `names`. */
  cuts: string[];
  /** How many overdue claims count, over everyone on it (the sheet's overdue count). */
  overdueCount: number;
};

const asName = (s: string) => s.trim().toLowerCase();

/** A sheet's figures from what it holds — the one working the header and the consolidated figures share. */
export function sheetFigures(
  people: Person[],
  claims: Claim[],
  rates: Rate[],
  today: string,
  floor: Dated,
  late: Late,
  cap: Dated,
  pools: Pool[] = [],
): Figures {
  const claimsOf = (id: string) => claims.filter((c) => c.personId === id);
  const dueOf = (p: Person) => personDueTotal(rates, p.grades, today, claimsOf(p.id), liveHolds(p, today), floor, late, cap, heldParts(p.id, people, claims, rates, today, floor, late), cutTotal(pools, p.id));
  const active = people.filter((p) => !p.retired);
  const dues = people.map(dueOf);
  const overdueOf = people.map((p) => personOverdue(rates, p.grades, today, claimsOf(p.id), liveHolds(p, today), floor, late));
  const overdues = overdueOf.map((o) => o.total);
  const splits = people.map((p) => personSplitTotals(rates, p.grades, today, claimsOf(p.id), liveHolds(p, today), floor, late, heldParts(p.id, people, claims, rates, today, floor, late)));
  return {
    gradeDueTotals: Object.fromEntries(
      [...new Set([...GRADES, ...people.map((p) => p.grade)])].map((g) => [
        g,
        sumDue(active.filter((p) => p.grade === g).map(dueOf)),
      ]),
    ) as Record<Grade, string>,
    statusCounts: Object.fromEntries(STATUSES.map((s) => [s, claims.filter((c) => c.status === s).length])),
    people: people.length,
    dueTotal: trimmedSumDue(dues),
    overdueTotal: trimmedSumDue(overdues),
    names: people.map((p) => [p.name, ...p.aliases].map(asName)),
    dues,
    overdues,
    settleds: splits.map((s) => s.settled),
    opens: splits.map((s) => s.open),
    cuts: people.map((p) => cutTotal(pools, p.id)),
    overdueCount: overdueOf.reduce((n, o) => n + o.count, 0),
  };
}

/**
 * The figures of a whole tree from each sheet's: the due-total, overdue-total, settled-total, open-total
 * and cut total each trimmed once over
 * every person of the tree, and the people counted once each — people whose names or aliases match
 * (as names are compared), on any sheet of it, are one person.
 */
export function treeTotals(all: Figures[]): {
  people: number;
  dueTotal: string;
  overdueTotal: string;
  settledTotal: string;
  openTotal: string;
  cutTotal: string;
} {
  const names = all.flatMap((f) => f.names);
  const root = names.map((_, i) => i);
  const find = (i: number): number => (root[i] === i ? i : (root[i] = find(root[i])));
  const seen = new Map<string, number>();
  names.forEach((ns, i) =>
    ns.forEach((n) => {
      const j = seen.get(n);
      if (j === undefined) seen.set(n, i);
      else root[find(i)] = find(j);
    }),
  );
  return {
    people: new Set(names.map((_, i) => find(i))).size,
    dueTotal: trimmedSumDue(all.flatMap((f) => f.dues)),
    overdueTotal: trimmedSumDue(all.flatMap((f) => f.overdues)),
    settledTotal: trimmedSumDue(all.flatMap((f) => f.settleds)),
    openTotal: trimmedSumDue(all.flatMap((f) => f.opens)),
    cutTotal: trimmedSumDue(all.flatMap((f) => f.cuts)),
  };
}

const enc = encodeURIComponent;

/**
 * A sheet's figures, every part of it read fresh from the store: as it is now, or, while the screen
 * shows an as-of date, as it stood at the end of that date (each read names it, and the sheet read
 * answers with that date as its today).
 */
export async function readFigures(sheetId: string): Promise<Figures> {
  const [sheet, people, claims, rates, settings, pools] = await Promise.all([
    getJson<Sheet>(`/api/sheets/${enc(sheetId)}`),
    getJson<{ people: Person[] }>(`/api/sheets/${enc(sheetId)}/people`).then((r) => r.people),
    getJson<{ claims: Claim[] }>(`/api/sheets/${enc(sheetId)}/claims`).then((r) => r.claims),
    getJson<{ rates: Rate[] }>(`/api/sheets/${enc(sheetId)}/rates`).then((r) => r.rates),
    getJson<Settings>(`/api/sheets/${enc(sheetId)}/settings`),
    getJson<{ pools: Pool[] }>(`/api/sheets/${enc(sheetId)}/pools`).then((r) => r.pools),
  ]);
  return sheetFigures(people, claims, rates, sheet.today, floorOf(settings), lateOf(settings, sheet.today), capOf(settings), pools);
}

/**
 * The figures of each of the sheet's descendants: the sheets naming it as their parent, theirs, and
 * so on, each as the screen's as-of date shows it (as it is now while none is set). Kept under the
 * sheet's own key with the as-of date, so loading the sheet again, or changing the date, re-reads them all.
 */
export function useDescendantFigures(sheetId: string, asOf: string) {
  return useQuery({
    queryKey: ['sheet', sheetId, 'descendants', asOf] as const,
    queryFn: async () => {
      const { descendants } = await getJson<{ descendants: string[] }>(`/api/sheets/${enc(sheetId)}/descendants`);
      return Promise.all(descendants.map(readFigures));
    },
  });
}
