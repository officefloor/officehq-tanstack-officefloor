import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet } from '../../session/session';
import { gradeOn, usePeople } from '../people/people';
import { dueTotal } from '../rates/rates';
import { usePersonDue } from '../rates/due.slot';
import { sheetKey, useSheetToday } from '../sheet/sheet';
import { useFigure } from '../locale/locale';
import { useGradeCodes } from '../grades/grades';

/** Per grade, the sheet's summary due-total: the due-totals of the people on it today (retired people left
 *  out) added up, skipping n/a. */
function useGradeDueTotals(sheet: string): Record<string, string> {
  const grades = useGradeCodes(sheet);
  const people = usePeople(sheet);
  const today = useSheetToday(sheet);
  const personDue = usePersonDue(sheet);
  const active = (people.data ?? []).filter((p) => !p.retired);
  return Object.fromEntries(
    grades.map((g) => [g, dueTotal(active.filter((p) => gradeOn(p.grades, today) === g).map(personDue))]),
  );
}

function CompareLines({ other }: { other: string }) {
  const fig = useFigure();
  const [sheet] = useCurrentSheet();
  const mine = useGradeDueTotals(sheet);
  const theirs = useGradeDueTotals(other);
  // Every grade either sheet has, the current sheet's first; a grade a sheet lacks has no one on it.
  const grades = [...new Set([...Object.keys(mine), ...Object.keys(theirs)])];
  return (
    <ul>
      {grades.map((g) => (
        <li key={g} data-testid="compare-line">
          {g} {fig(mine[g])} {fig(theirs[g])}
        </li>
      ))}
    </ul>
  );
}

/** Shows the comparison once the other sheet's existence is known; a sheet that does not exist gives no lines. */
function Comparison({ other, run }: { other: string; run: number }) {
  const exists = useQuery({
    queryKey: ['compare-exists', other, run],
    queryFn: () => getJson<boolean>(`/api/sheets/${encodeURIComponent(other)}/exists`),
    staleTime: Infinity,
  });
  if (!exists.data) return null;
  return <CompareLines other={other} />;
}

// Compares this sheet with another, named by id: one line per grade of this sheet, the grade, this sheet's
// summary due-total for it and the other sheet's (0.00 when the other has no one on it).
function CompareSheets() {
  const [sheet] = useCurrentSheet();
  const [input, setInput] = useState('');
  const queryClient = useQueryClient();
  const [compared, setCompared] = useState<{ other: string; run: number } | null>(null);
  if (sheet === '') return null;
  return (
    <section>
      <h2>Compare sheets</h2>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          const other = input.trim();
          // Each comparison re-reads the other sheet from the store.
          if (other !== '') void queryClient.invalidateQueries({ queryKey: sheetKey(other) });
          setCompared(other === '' ? null : { other, run: (compared?.run ?? 0) + 1 });
        }}
      >
        <input data-testid="compare-sheet" placeholder="Other sheet id" value={input} onChange={(e) => setInput(e.target.value)} />
        <button type="submit" data-testid="compare">
          Compare
        </button>
      </form>
      {compared && <Comparison key={compared.run} other={compared.other} run={compared.run} />}
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 46.5, Component: CompareSheets });
