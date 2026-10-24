import { useQuery } from '@tanstack/react-query';
import { currentAsOf, getJson, getJsonLive } from '../../api/http';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser, useSheetToday, type SheetInfo } from '../session/session';
import { peoplePath, usePeople, type Person } from '../people/people';
import { claimsPath, useClaims, type Claim } from '../claims/claims';
import { ratesPath, sumDue, useRates, type Rate } from '../rates/rates';
import { settingsPath, useSettings, type Settings } from '../roles/roles';
import { sheetTotals } from '../header/counters.slot';

// The consolidated figures: over the sheet and all its descendants (its children are the sheets naming it as their
// parent, and so on down), the number of people, the due-total and the overdue-total — each sheet's as that sheet
// shows it (its own today, rates, settings), added up. A sheet with no children shows its own figures. While the
// screen shows the sheet as of a date, every sheet of the tree is read as it stood at the end of that date, with that
// date as its today.

type Figures = { people: number; due: string; overdue: string };

const sheetPath = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}`;
const childrenPath = (sheetId: string) => `${sheetPath(sheetId)}/children`;

export const descendantsQueryKey = (sheetId: string) => ['sheet', sheetId, 'descendants'] as const;

/** A sheet's figures, read from the store as the screen shows it (as of its date, if any). */
async function figuresOf(id: string): Promise<Figures> {
  const [info, people, claims, rates, settings] = await Promise.all([
    getJson<SheetInfo>(sheetPath(id)),
    getJson<Person[]>(peoplePath(id)),
    getJson<Claim[]>(claimsPath(id)),
    getJson<Rate[]>(ratesPath(id)),
    getJson<Settings>(settingsPath(id)),
  ]);
  return { people: people.length, ...sheetTotals(people, claims, rates, info.today, settings) };
}

/** Each descendant sheet's figures, read from the store as the screen shows it (a sheet reached twice counts once). */
async function descendantFigures(sheetId: string): Promise<Figures[]> {
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
  const today = useSheetToday();
  const asOf = currentAsOf();
  const descendants = useQuery({
    queryKey: [...descendantsQueryKey(sheet ?? ''), asOf],
    queryFn: () => descendantFigures(sheet ?? ''),
    enabled: !!sheet,
  });
  if (!user || !sheet) return null;
  const allPeople = people.data ?? [];
  const own = { people: allPeople.length, ...sheetTotals(allPeople, claims.data ?? [], rates.data, today, settings.data) };
  const all = [own, ...(descendants.data ?? [])];
  return (
    <section data-testid="consolidated">
      Consolidated: People <strong data-testid="consolidated-people-count">{all.reduce((n, f) => n + f.people, 0)}</strong>
      {' · '}Due <strong data-testid="consolidated-due-total">{sumDue(all.map((f) => f.due))}</strong>
      {' · '}Overdue <strong data-testid="consolidated-overdue-total">{sumDue(all.map((f) => f.overdue))}</strong>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 1.5, Component: ConsolidatedFigures });
