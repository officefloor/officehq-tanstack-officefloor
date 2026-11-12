import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { AppHeader } from '../../slots/defs/appHeader';
import { useAsOf, useCurrentSheet } from '../../session/session';
import { asOfKey, asOfPath, sheetKey } from '../sheet/sheet';
import { GRADES, gradeOn, usePeople } from '../people/people';
import { STATUSES, statusCount, useClaims } from '../claims/claims';
import { useSheetToday } from '../sheet/sheet';
import { dueTotal } from '../rates/rates';
import { usePersonDue, useSheetDueTotal } from '../rates/due.slot';
import { useSheetOverdue } from '../overdue/overdue.slot';
import { useFigure } from '../locale/locale';

/** Under the sheet's key, so loading the sheet again re-reads its descendants. */
const descendantsKey = (sheet: string) => [...sheetKey(sheet), 'descendants'] as const;

/** The ids of the sheets naming the sheet as their parent, their children, and so on (as of the screen's
 *  as-of date, when one is set). */
function useDescendants(sheet: string) {
  const [asOf] = useAsOf();
  return useQuery({
    queryKey: asOfKey(descendantsKey(sheet), asOf),
    queryFn: () => getJson<string[]>(asOfPath(`/api/sheets/${encodeURIComponent(sheet)}/descendants`, asOf)),
    enabled: sheet !== '',
  });
}

type Figures = {
  people: number;
  due: string[];
  overdue: string[];
  gradeDue: Record<string, string[]>;
  statuses: Record<string, number>;
};

const noFigures = (): Figures => ({
  people: 0,
  due: [],
  overdue: [],
  gradeDue: Object.fromEntries(GRADES.map((g) => [g, []])),
  statuses: Object.fromEntries(STATUSES.map((s) => [s, 0])),
});

/** Adds the first sheet's figures (as that sheet shows them) to those so far, then the rest's, one sheet per
 *  component so each reads its sheet with the same hooks; shows the sums once every sheet is added. */
function Consolidate({ sheets, sofar }: { sheets: string[]; sofar: Figures }) {
  const fig = useFigure();
  const [first, ...rest] = sheets;
  const people = usePeople(first);
  const due = useSheetDueTotal(first);
  const overdue = useSheetOverdue(first);
  const claims = useClaims(first);
  const today = useSheetToday(first);
  const personDue = usePersonDue(first);
  const active = (people.data ?? []).filter((p) => !p.retired);
  const figures: Figures = {
    people: sofar.people + (people.data ?? []).length,
    due: [...sofar.due, due],
    overdue: [...sofar.overdue, overdue.total],
    gradeDue: Object.fromEntries(
      GRADES.map((g) => [g, [...sofar.gradeDue[g], ...active.filter((p) => gradeOn(p.grades, today) === g).map(personDue)]]),
    ),
    statuses: Object.fromEntries(STATUSES.map((s) => [s, sofar.statuses[s] + statusCount(claims.data, s)])),
  };
  if (rest.length > 0) return <Consolidate sheets={rest} sofar={figures} />;
  return (
    <>
      People <strong data-testid="consolidated-people-count">{figures.people}</strong>{' '}
      Due total <strong data-testid="consolidated-due-total">{fig(dueTotal(figures.due))}</strong>{' '}
      Overdue total <strong data-testid="consolidated-overdue-total">{fig(dueTotal(figures.overdue))}</strong>
      {GRADES.map((g) => (
        <span key={g}>
          {' '}Grade {g} due total{' '}
          <strong data-testid={`consolidated-grade-${g}-due-total`}>{fig(dueTotal(figures.gradeDue[g]))}</strong>
        </span>
      ))}
      {STATUSES.map((s) => (
        <span key={s}>
          {' '}{s} <strong data-testid={`consolidated-status-${s}-count`}>{figures.statuses[s]}</strong>
        </span>
      ))}
    </>
  );
}

// The consolidated figures over the sheet and all its descendants: their people, and each sheet's due- and
// overdue-totals (the header's) added up; per grade, the due-totals of the people on it on their sheet's today
// (retired people left out) added up; and per status, how many claims have it. While the screen has an as-of date, every sheet of the tree is read
// as it stood at the end of that day, with that date as its today. A sheet with no children shows its own.
function ConsolidatedFigures() {
  const [sheet] = useCurrentSheet();
  const descendants = useDescendants(sheet);
  if (sheet === '' || !descendants.data) return null;
  return (
    <p data-testid="consolidated-figures">
      Consolidated{' '}
      <Consolidate key={descendants.data.join(',')} sheets={[sheet, ...descendants.data]} sofar={noFigures()} />
    </p>
  );
}

export const contribution = AppHeader.fill({ order: 31, Component: ConsolidatedFigures });
