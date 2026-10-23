import { useQuery } from '@tanstack/react-query';
import { getJsonNow } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheet';
import { STATUSES, type Claim } from '../claims/claims';
import { GRADES, gradeOn, type Person } from '../people/people';
import { personDueTotal, sheetDueTotal, sheetOverdueTotal, sumDue, type Rate } from '../rates/rates';
import { floorOf, lateOf, type Settings } from '../settings/settings';
import { useAsOf, type Sheet } from '../sheet/sheet';

/** One read of a sheet as the store holds it now, or (asOf given, yyyy-mm-dd) as it stood at the end of that day,
 *  whatever date this screen shows its own sheet as of. Keyed under the sheet, so a load of that sheet re-reads it. */
function useNow<T>(sheetId: string, part: string, asOf = '') {
  return useQuery({
    queryKey: ['sheet', sheetId, 'now', part, asOf],
    queryFn: () => getJsonNow<T>(`/api/sheets/${encodeURIComponent(sheetId)}${part === '' ? '' : `/${part}`}`
      + (asOf === '' ? '' : `?asOf=${encodeURIComponent(asOf)}`)),
  });
}

type Figures = { people: number; due: string; overdue: string; grades: Record<string, string>;
  statuses: Record<string, number> };

const NONE: Figures = {
  people: 0, due: '0.00', overdue: '0.00',
  grades: Object.fromEntries(GRADES.map((g) => [g, '0.00'])),
  statuses: Object.fromEntries(STATUSES.map((s) => [s, 0])),
};

/** Adds the figures of sheets[at] to `sum`, then goes on to the next sheet; past the last, shows the figures added
 *  up. One component per sheet keeps each sheet's reads in hooks of its own. */
function Accumulate({ sheets, at, sum, asOf }: { sheets: string[]; at: number; sum: Figures; asOf: string }) {
  if (at >= sheets.length) return <Shown figures={sum} />;
  return <SheetFigures key={sheets[at]} sheets={sheets} at={at} sum={sum} asOf={asOf} />;
}

/** A sheet's figures as it shows them now (or, asOf given, as it showed them at the end of that day, with that day
 *  as its today): its people (retired ones included), its due-total and its overdue-total (as its header shows
 *  them), per grade the due-totals of its people on that grade today (retired ones left out, as its summary shows
 *  them), and per status how many of its claims have it. */
function SheetFigures({ sheets, at, sum, asOf }: { sheets: string[]; at: number; sum: Figures; asOf: string }) {
  const sheetId = sheets[at];
  const people = useNow<Person[]>(sheetId, 'people', asOf).data ?? [];
  const claims = useNow<Claim[]>(sheetId, 'claims', asOf).data ?? [];
  const rates = useNow<Rate[]>(sheetId, 'rates', asOf).data ?? [];
  const settings = useNow<Settings>(sheetId, 'settings', asOf).data;
  const sheetToday = useNow<Sheet>(sheetId, '', asOf).data?.today ?? '';
  const today = asOf !== '' && sheetToday !== '' ? asOf : sheetToday;
  const floor = floorOf(settings);
  const late = lateOf(settings, today, claims, people);
  const next: Figures = {
    people: sum.people + people.length,
    due: sumDue([sum.due, sheetDueTotal(people, claims, rates, today, floor, late, settings?.cap ?? '')]),
    overdue: sumDue([sum.overdue, sheetOverdueTotal(people, claims, rates, late, floor)]),
    grades: Object.fromEntries(GRADES.map((g) => [g, sumDue([sum.grades[g],
      ...people.filter((p) => p.retiredFrom === null && gradeOn(p, today) === g)
        .map((p) => personDueTotal(p, claims, rates, today, floor, late, settings?.cap ?? ''))])])),
    statuses: Object.fromEntries(STATUSES.map((s) => [s,
      sum.statuses[s] + claims.filter((c) => c.status === s).length])),
  };
  return <Accumulate sheets={sheets} at={at + 1} sum={next} asOf={asOf} />;
}

function Shown({ figures }: { figures: Figures }) {
  return (
    <p data-testid="consolidated">
      Consolidated: people <strong data-testid="consolidated-people-count">{figures.people}</strong>{' '}
      due total <strong data-testid="consolidated-due-total">{figures.due}</strong>{' '}
      overdue total <strong data-testid="consolidated-overdue-total">{figures.overdue}</strong>
      {GRADES.map((g) => (
        <span key={g}>
          {' '}grade {g} due total <strong data-testid={`consolidated-grade-${g}-due-total`}>{figures.grades[g]}</strong>
        </span>
      ))}
      {STATUSES.map((s) => (
        <span key={s}>
          {' '}{s} <strong data-testid={`consolidated-status-${s}-count`}>{figures.statuses[s]}</strong>
        </span>
      ))}
    </p>
  );
}

/** Consolidated figures over the sheet and all its descendants (its children — the sheets naming it as their
 *  parent — theirs, and so on), as the store holds them now (while this screen shows the sheet as of a date: every
 *  sheet of the tree as it stood at the end of that day, with that date as its today): the people, and each sheet's due-total and
 *  overdue-total (as that sheet shows them) added up, per grade its people's due-totals and per status its claims
 *  counted. A sheet with no children shows its own. */
function Consolidated({ sheetId }: { sheetId: string; user: string }) {
  const [asOf] = useAsOf();
  const descendants = useNow<string[]>(sheetId, 'descendants').data ?? [];
  return <Accumulate sheets={[sheetId, ...descendants]} at={0} sum={NONE}
    asOf={asOf ?? ''} />;
}

export const contribution = SheetBody.fill({ order: -1, Component: Consolidated });
