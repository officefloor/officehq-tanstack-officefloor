import { useQuery } from '@tanstack/react-query';
import { currentAsOf, currentPreview, figureToday, getJson, getJsonLive } from '../../api/http';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser, useSheetToday, type SheetInfo } from '../session/session';
import { GRADES, gradeOn, nameKey, peoplePath, usePeople, type Person } from '../people/people';
import { claimsPath, useClaims, type Claim } from '../claims/claims';
import { ratesPath, sumDue, trimmedSum, useRates, type Rate } from '../rates/rates';
import { settingsPath, useSettings, type Settings } from '../roles/roles';
import { overdueOf } from '../claims/overdue.slot';
import { personDue } from '../rates/due.slot';
import { STATUSES } from '../summary/summary.slot';
import { cutTotalOf, poolsPath, usePools, type Pool } from '../pools/pools';
import { splitOf } from '../rates/split.slot';
import { useGradeCodes } from '../grades/grades';

// The consolidated figures: over the sheet and all its descendants (its children are the sheets naming it as their
// parent, and so on down), the number of people — each person once across the tree: people whose names or aliases
// match (as names are compared) are one person — and the due-total and the overdue-total: each person's figure as
// their sheet shows it (its own today, rates, settings), with the trim applied once over all people of the tree (the
// single highest and lowest figure left out when three or more have one); likewise the settled-total, the open-total
// and the cut total, each person's as their sheet shows it, trimmed once over the tree. A sheet with no children shows its own figures. While the
// screen shows the sheet as of a date, every sheet of the tree is read as it stood at the end of that date, with that
// date as its today. Per grade, the due-totals of the people not retired on that grade (in force on each sheet's
// today), added up over the tree (skipping n/a); per status, how many claims over the tree have it.

export type Figures = {
  /** Per person, the names they go by (name and aliases), as names are compared. */
  people: string[][];
  /** Per person, their due-total and overdue-total. */
  dues: string[];
  overdues: string[];
  /** Per person, their settled-total, open-total and cut total. */
  settleds: string[];
  opens: string[];
  cuts: string[];
  /** How many of its people's claims are overdue, as the rollup counts them. */
  overdueCount: number;
  /** Per grade (A, B, C and any a person not retired has in force), the due-total of the people on it. */
  grades: Record<string, string>;
  statuses: Record<string, number>;
};

/** A sheet's figures, from its people, claims, rates, today and settings. */
export function figuresFrom(people: Person[], claims: Claim[], rates: Rate[] | undefined, today: string | undefined,
  settings: Settings | undefined, pools: Pool[] | undefined): Figures {
  const current = people.filter((p) => p.retiredFrom === null);
  return {
    people: people.map((p) => [p.name, ...(p.aliases ?? [])].map(nameKey)),
    dues: people.map((p) => personDue(p, claims, rates, today, settings, people, pools)),
    overdues: people.map((p) => overdueOf(p, claims, rates, today, settings).total),
    settleds: people.map((p) => splitOf(p, claims, rates, today, settings).settled),
    opens: people.map((p) => splitOf(p, claims, rates, today, settings).open),
    cuts: people.map((p) => cutTotalOf(pools ?? [], p.id)),
    overdueCount: people.reduce((n, p) => n + overdueOf(p, claims, rates, today, settings).count, 0),
    grades: Object.fromEntries([...new Set([...GRADES, ...current.map((p) => gradeOn(p, today))])].map((g) => [g, sumDue(current.filter((p) => gradeOn(p, today) === g)
      .map((p) => personDue(p, claims, rates, today, settings, people, pools)))])),
    statuses: Object.fromEntries(STATUSES.map((s) => [s, claims.filter((c) => c.status === s).length])),
  };
}

/** How many people the tree has, counting as one those whose names or aliases match (and so on, transitively). */
function distinctPeople(people: string[][]): number {
  const parent = people.map((_, i) => i);
  const root = (i: number): number => (parent[i] === i ? i : (parent[i] = root(parent[i])));
  const byKey = new Map<string, number>();
  people.forEach((keys, i) => keys.forEach((k) => {
    const j = byKey.get(k);
    if (j === undefined) byKey.set(k, i);
    else parent[root(i)] = root(j);
  }));
  return people.filter((_, i) => root(i) === i).length;
}

const sheetPath = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}`;
const childrenPath = (sheetId: string) => `${sheetPath(sheetId)}/children`;

export const descendantsQueryKey = (sheetId: string) => ['sheet', sheetId, 'descendants'] as const;

/** A sheet's figures, read from the store as the screen shows it (as of its date, if any). */
export async function figuresOf(id: string): Promise<Figures> {
  const [info, people, claims, rates, settings, pools] = await Promise.all([
    getJson<SheetInfo>(sheetPath(id)),
    getJson<Person[]>(peoplePath(id)),
    getJson<Claim[]>(claimsPath(id)),
    getJson<Rate[]>(ratesPath(id)),
    getJson<Settings>(settingsPath(id)),
    getJson<Pool[]>(poolsPath(id)),
  ]);
  return figuresFrom(people, claims, rates, figureToday(info.today), settings, pools);
}

/** Each descendant sheet's figures, read from the store as the screen shows it (a sheet reached twice counts once). */
export async function descendantFigures(sheetId: string): Promise<Figures[]> {
  const seen = new Set([sheetId]);
  const ids: string[] = [];
  for (let level = [sheetId]; level.length > 0; ) {
    const children = (await Promise.all(level.map((id) => getJsonLive<string[]>(childrenPath(id))))).flat();
    level = children.filter((id) => !seen.has(id) && (seen.add(id), true));
    ids.push(...level);
  }
  return Promise.all(ids.map(figuresOf));
}

function ConsolidatedFigures() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople();
  const claims = useClaims();
  const rates = useRates();
  const settings = useSettings();
  const pools = usePools();
  const today = useSheetToday();
  const gradeCodes = useGradeCodes();
  const asOf = currentAsOf();
  const descendants = useQuery({
    queryKey: [...descendantsQueryKey(sheet ?? ''), asOf, currentPreview()],
    queryFn: () => descendantFigures(sheet ?? ''),
    enabled: !!sheet,
  });
  if (!user || !sheet) return null;
  const own = figuresFrom(people.data ?? [], claims.data ?? [], rates.data, today, settings.data, pools.data);
  const all = [own, ...(descendants.data ?? [])];
  return (
    <section data-testid="consolidated">
      Consolidated: People <strong data-testid="consolidated-people-count">{distinctPeople(all.flatMap((f) => f.people))}</strong>
      {' · '}Due <strong data-testid="consolidated-due-total">{trimmedSum(all.flatMap((f) => f.dues))}</strong>
      {' · '}Overdue <strong data-testid="consolidated-overdue-total">{trimmedSum(all.flatMap((f) => f.overdues))}</strong>
      {' · '}Settled <strong data-testid="consolidated-settled-total">{trimmedSum(all.flatMap((f) => f.settleds))}</strong>
      {' · '}Open <strong data-testid="consolidated-open-total">{trimmedSum(all.flatMap((f) => f.opens))}</strong>
      {' · '}Cut <strong data-testid="consolidated-cut-total">{trimmedSum(all.flatMap((f) => f.cuts))}</strong>
      <div>
        {gradeCodes.map((g) => (
          <span key={g}>
            {' · '}Grade {g} due{' '}
            <strong data-testid={`consolidated-grade-${g}-due-total`}>{sumDue(all.map((f) => f.grades[g] ?? 'n/a'))}</strong>
          </span>
        ))}
      </div>
      <div>
        {STATUSES.map((s) => (
          <span key={s}>
            {' · '}{s}{' '}
            <strong data-testid={`consolidated-status-${s}-count`}>{all.reduce((n, f) => n + f.statuses[s], 0)}</strong>
          </span>
        ))}
      </div>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 1.5, Component: ConsolidatedFigures });
