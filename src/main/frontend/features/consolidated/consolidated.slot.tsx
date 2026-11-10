import { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useOverdue } from '../overdue/useOverdue';
import { useClaims } from '../claims/useClaims';
import { type Alias, peopleKey, useGrades, type Person, usePeople } from '../people/usePeople';
import { useDueTotals, useSettledOpenTotals } from '../rates/useDueTotals';
import { useCutTotals } from '../cuts/useCutTotals';
import { hundredths, money, sumDues, trimmedSum } from '../rates/useRates';
import { countWithStatus, STATUSES } from '../summary/statusCounts';
import { useFigure } from '../locale/useLocale';
import { useDescendants } from './useDescendants';

/** One sheet's figures: its people (retired included), each as the names they go by (their name and aliases, as
 *  names are compared: ignoring case), joined by '\t' within a person and '\n' between people; each person's
 *  due-total and overdue-total (as its header works them out), and settled-total, open-total and cut total (as its
 *  row shows them), comma separated; and, as its summary shows them, per grade the due-totals of the people on it (retired left
 *  out) added up, and per status how many claims have it. While the screen shows a date, each sheet is read as it stood at the end of that day, with that
 *  date as its today. */
type Figures = { people: string; due: string; overdue: string; settled: string; open: string; cut: string; grades: string; statuses: string };

/** How names are compared: ignoring case (and surrounding spaces). */
const nameKey = (name: string) => name.trim().toLowerCase();

/** The sheet's people (retired included), each as the names they go by (see `Figures.people`), as of the screen's
 *  as-of date when one is set. */
function usePeopleNames(sheetId: string) {
  return useQuery({
    queryKey: [...peopleKey(sheetId), 'names'],
    queryFn: async () => {
      const got = await getJson<{ people: Person[]; aliases?: Alias[] }>(
        `/api/sheets/${encodeURIComponent(sheetId)}/people`);
      return got.people.map((p) => [p.name, ...(got.aliases ?? []).filter((a) => a.personId === p.id).map((a) => a.alias)]
        .map(nameKey).join('\t')).join('\n');
    },
  });
}

/** How many people there are over all sheets' people (see `Figures.people`): people who share a name or alias are
 *  one person (and so, in turn, are those who share one with either). */
function countPeople(sheets: string[]): number {
  const people = sheets.flatMap((s) => (s === '' ? [] : s.split('\n'))).map((p) => p.split('\t'));
  const parent = people.map((_, i) => i);
  const root = (i: number): number => (parent[i] === i ? i : (parent[i] = root(parent[i])));
  const byName = new Map<string, number>();
  people.forEach((names, i) => {
    for (const n of names) {
      const at = byName.get(n);
      if (at === undefined) byName.set(n, i);
      else parent[root(at)] = root(i);
    }
  });
  return people.filter((_, i) => root(i) === i).length;
}

/** Works out one sheet's figures with the same hooks its header uses, and hands them up; shows nothing. */
function SheetFigures({ sheetId, grades: shownGrades, onFigures }: { sheetId: string; grades: readonly string[]; onFigures: (id: string, f: Figures) => void }) {
  const people = usePeopleNames(sheetId).data ?? '';
  const due = [...useDueTotals(sheetId).values()].join(',');
  const overdue = [...useOverdue(sheetId).values()].map((f) => f.total).join(',');
  const settledOpen = [...useSettledOpenTotals(sheetId).values()];
  const settled = settledOpen.map((s) => s.settled).join(',');
  const open = settledOpen.map((s) => s.open).join(',');
  const cut = [...useCutTotals(sheetId).values()].join(',');
  const dueTotals = useDueTotals(sheetId);
  const current = (usePeople(sheetId).data ?? []).filter((p) => !p.retiredFrom);
  const claims = useClaims(sheetId).data;
  // Joined into strings so the effect below only fires when a figure changes.
  const grades = shownGrades.map((g) =>
    sumDues(current.filter((p) => p.grade === g).map((p) => dueTotals.get(p.id) ?? 'n/a'))).join(',');
  const statuses = STATUSES.map((s) => countWithStatus(claims, s)).join(',');
  useEffect(
    () => onFigures(sheetId, { people, due, overdue, settled, open, cut, grades, statuses }),
    [sheetId, people, due, overdue, settled, open, cut, grades, statuses, onFigures],
  );
  return null;
}

/** The consolidated figures over the sheet and all its descendants: the number of people (each counted once over
 *  the tree: people whose names or aliases match are one person), the due-total, overdue-total, settled-total,
 *  open-total and cut total of every person
 *  of the tree added up, leaving out the single highest and the single lowest figure over the whole tree when three
 *  or more people have one, per grade the due-totals of the people on
 *  it added up, and per status the count of claims, all as of the screen's as-of date when one is
 *  set. A sheet with no children shows its own. */
function Consolidated({ sheetId }: { sheetId: string }) {
  const fig = useFigure();
  const grades = useGrades(sheetId);
  const descendants = useDescendants(sheetId);
  const [figures, setFigures] = useState<Record<string, Figures>>({});
  const [onFigures] = useState(() => (id: string, f: Figures) => setFigures((all) => ({ ...all, [id]: f })));
  const sheets = [sheetId, ...(descendants.data ?? [])];
  const shown = sheets.map((id) => figures[id]).filter((f): f is Figures => f !== undefined);
  const sum = (pick: (f: Figures) => string) => money(shown.reduce((s, f) => s + hundredths(pick(f)), 0n));
  const trimmed = (pick: (f: Figures) => string) =>
    trimmedSum(shown.flatMap((f) => (pick(f) === '' ? [] : pick(f).split(','))));
  const gradeDue = (i: number) => sum((f) => f.grades.split(',')[i] ?? '0.00');
  const statusCount = (i: number) => shown.reduce((n, f) => n + Number(f.statuses.split(',')[i]), 0);
  return (
    <p data-testid="consolidated">
      {sheets.map((id) => (
        <SheetFigures key={id} sheetId={id} grades={grades} onFigures={onFigures} />
      ))}
      Consolidated: people <strong data-testid="consolidated-people-count">{countPeople(shown.map((f) => f.people))}</strong>
      {' '}· due total <strong data-testid="consolidated-due-total">{fig(trimmed((f) => f.due))}</strong>
      {' '}· overdue total <strong data-testid="consolidated-overdue-total">{fig(trimmed((f) => f.overdue))}</strong>
      {' '}· settled total <strong data-testid="consolidated-settled-total">{fig(trimmed((f) => f.settled))}</strong>
      {' '}· open total <strong data-testid="consolidated-open-total">{fig(trimmed((f) => f.open))}</strong>
      {' '}· cut total <strong data-testid="consolidated-cut-total">{fig(trimmed((f) => f.cut))}</strong>
      {grades.map((g, i) => (
        <span key={g}>
          {' '}· grade {g} due total <strong data-testid={`consolidated-grade-${g}-due-total`}>{fig(gradeDue(i))}</strong>
        </span>
      ))}
      {STATUSES.map((s, i) => (
        <span key={s}>
          {' '}· {s} <strong data-testid={`consolidated-status-${s}-count`}>{statusCount(i)}</strong>
        </span>
      ))}
    </p>
  );
}

export const contribution = SheetBody.fill({ order: -9, Component: ({ sheetId }) => <Consolidated sheetId={sheetId} /> });
