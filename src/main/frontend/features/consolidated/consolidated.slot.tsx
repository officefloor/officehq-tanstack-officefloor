import { useQuery } from '@tanstack/react-query';
import { getJsonNow } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheet';
import { STATUSES, type Claim } from '../claims/claims';
import { GRADES, gradeOn, type Person } from '../people/people';
import { overdueDues, personDueTotal, sumDue, trimmedSum, type Rate } from '../rates/rates';
import { floorOf, lateOf, type Settings } from '../settings/settings';
import { useAsOf, usePreview, type Sheet } from '../sheet/sheet';

/** One read of a sheet as the store holds it now, or (asOf given, yyyy-mm-dd) as it stood at the end of that day,
 *  whatever date this screen shows its own sheet as of; given a preview date (and no asOf), its current records
 *  figured on that date. Keyed under the sheet, so a load of that sheet re-reads it. */
function useNow<T>(sheetId: string, part: string, asOf = '', preview = '') {
  const query = asOf !== '' ? `?asOf=${encodeURIComponent(asOf)}` : preview !== '' ? `?preview=${encodeURIComponent(preview)}` : '';
  return useQuery({
    queryKey: ['sheet', sheetId, 'now', part, asOf, preview],
    queryFn: () => getJsonNow<T>(`/api/sheets/${encodeURIComponent(sheetId)}${part === '' ? '' : `/${part}`}${query}`),
  });
}

/** The tree's figures gathered so far: per person (in sheet order) their names (name and aliases), due-total and
 *  overdue-total, and the per-grade and per-status figures added up. */
type Figures = { names: string[][]; dues: string[]; overdues: string[]; grades: Record<string, string>;
  statuses: Record<string, number> };

const NONE: Figures = {
  names: [], dues: [], overdues: [],
  grades: Object.fromEntries(GRADES.map((g) => [g, '0.00'])),
  statuses: Object.fromEntries(STATUSES.map((s) => [s, 0])),
};

/** Adds the figures of sheets[at] to `sum`, then goes on to the next sheet; past the last, shows the figures added
 *  up. One component per sheet keeps each sheet's reads in hooks of its own. */
function Accumulate({ sheets, at, sum, asOf, preview }: {
  sheets: string[]; at: number; sum: Figures; asOf: string; preview: string;
}) {
  if (at >= sheets.length) return <Shown figures={sum} />;
  return <SheetFigures key={sheets[at]} sheets={sheets} at={at} sum={sum} asOf={asOf} preview={preview} />;
}

/** A sheet's figures as it shows them now (or, asOf given, as it showed them at the end of that day, with that day
 *  as its today): its people (retired ones included), its due-total and its overdue-total (as its header shows
 *  them), per grade the due-totals of its people on that grade today (retired ones left out, as its summary shows
 *  them), and per status how many of its claims have it. Given a preview date (and no asOf), its current records with
 *  that date as its today. */
function SheetFigures({ sheets, at, sum, asOf, preview }: {
  sheets: string[]; at: number; sum: Figures; asOf: string; preview: string;
}) {
  const sheetId = sheets[at];
  const people = useNow<Person[]>(sheetId, 'people', asOf).data ?? [];
  const claims = useNow<Claim[]>(sheetId, 'claims', asOf, preview).data ?? [];
  const rates = useNow<Rate[]>(sheetId, 'rates', asOf).data ?? [];
  const settings = useNow<Settings>(sheetId, 'settings', asOf).data;
  const sheetToday = useNow<Sheet>(sheetId, '', asOf).data?.today ?? '';
  const shownOn = asOf !== '' ? asOf : preview;
  const today = shownOn !== '' && sheetToday !== '' ? shownOn : sheetToday;
  const floor = floorOf(settings);
  const late = lateOf(settings, today, claims, people);
  const next: Figures = {
    names: [...sum.names, ...people.map((p) => [p.name, ...p.aliases])],
    dues: [...sum.dues, ...people.map((p) => personDueTotal(p, claims, rates, today, floor, late, settings?.cap ?? '', people))],
    overdues: [...sum.overdues, ...people.map((p) => sumDue(overdueDues(claims.filter((c) => c.personId === p.id), [p],
      rates, late, floor)))],
    grades: Object.fromEntries(GRADES.map((g) => [g, sumDue([sum.grades[g],
      ...people.filter((p) => p.retiredFrom === null && gradeOn(p, today) === g)
        .map((p) => personDueTotal(p, claims, rates, today, floor, late, settings?.cap ?? '', people))])])),
    statuses: Object.fromEntries(STATUSES.map((s) => [s,
      sum.statuses[s] + claims.filter((c) => c.status === s).length])),
  };
  return <Accumulate sheets={sheets} at={at + 1} sum={next} asOf={asOf} preview={preview} />;
}

/** A name as names are compared: without regard to case, ignoring surrounding spaces. */
const nameKey = (name: string) => name.trim().toLowerCase();

/** How many people there are, counting as one all whose names or aliases match (as names are compared), directly
 *  or through others. */
function distinctPeople(names: string[][]): number {
  const parent = names.map((_, i) => i);
  const root = (i: number): number => (parent[i] === i ? i : (parent[i] = root(parent[i])));
  const owner = new Map<string, number>();
  names.forEach((ns, i) => ns.forEach((n) => {
    const key = nameKey(n);
    const other = owner.get(key);
    if (other === undefined) owner.set(key, i);
    else parent[root(i)] = root(other);
  }));
  return names.filter((_, i) => root(i) === i).length;
}

function Shown({ figures }: { figures: Figures }) {
  return (
    <p data-testid="consolidated">
      Consolidated: people <strong data-testid="consolidated-people-count">{distinctPeople(figures.names)}</strong>{' '}
      due total <strong data-testid="consolidated-due-total">{trimmedSum(figures.dues)}</strong>{' '}
      overdue total <strong data-testid="consolidated-overdue-total">{trimmedSum(figures.overdues)}</strong>
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
 *  sheet of the tree as it stood at the end of that day, with that date as its today; while it previews a date: every
 *  sheet of the tree as the store holds it now, with that date as its today): the people (those whose names or
 *  aliases match, as names are compared, counted once), the due-total and overdue-total over every person of the tree
 *  (each as their sheet shows it), trimmed once over the whole tree (see trimmedSum), per grade its people's due-totals and per status its claims
 *  counted. A sheet with no children shows its own. */
function Consolidated({ sheetId }: { sheetId: string; user: string }) {
  const [asOf] = useAsOf();
  const [preview] = usePreview();
  const descendants = useNow<string[]>(sheetId, 'descendants').data ?? [];
  return <Accumulate sheets={[sheetId, ...descendants]} at={0} sum={NONE}
    asOf={asOf ?? ''} preview={asOf ? '' : preview} />;
}

export const contribution = SheetBody.fill({ order: -1, Component: Consolidated });
