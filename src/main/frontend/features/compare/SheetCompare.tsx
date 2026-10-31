import { useIsFetching, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { getJson } from '../../api/http';
import { GRADES, gradeOn, type Person } from '../people/usePeople';
import { useSheetFigures } from '../people/useSheetFigures';
import { formatCents } from '../rates/due';

type GradeDue = (g: (typeof GRADES)[number]) => bigint;

// A grade's due-total as the summary shows it: the due-totals of the people on it today (retired people
// left out, n/a skipped) added up.
const gradeDueOf =
  (people: Person[], today: string | undefined, totalOf: (p: Person) => bigint | null): GradeDue =>
  (g) =>
    people
      .filter((p) => !p.retired && gradeOn(p, today) === g)
      .reduce((sum, p) => sum + (totalOf(p) ?? 0n), 0n);

// Compares this sheet with another: per grade, "<grade> <this sheet's summary due-total> <the other's>"
// (0.00 where the other has no one on it). A sheet that does not exist gives no lines, and is not started
// by asking. Lines are gone while a press is being worked out.
export function SheetCompare({ people, today, totalOf }: { people: Person[]; today: string | undefined; totalOf: (p: Person) => bigint | null }) {
  const [other, setOther] = useState('');
  const [asked, setAsked] = useState<{ sheet: string; press: number } | null>(null);
  const mine = gradeDueOf(people, today, totalOf);
  return (
    <div>
      Compare with sheet <input data-testid="compare-sheet" value={other} onChange={(e) => setOther(e.target.value)} />{' '}
      <button type="button" data-testid="compare" onClick={() => setAsked({ sheet: other.trim(), press: (asked?.press ?? 0) + 1 })}>
        Compare
      </button>
      {asked !== null && asked.sheet !== '' && <Compared key={asked.press} other={asked.sheet} mine={mine} />}
    </div>
  );
}

function Compared({ other, mine }: { other: string; mine: GradeDue }) {
  const { data } = useQuery({
    queryKey: ['sheet-exists', other],
    queryFn: () => getJson<{ exists: boolean }>(`/api/sheets/${encodeURIComponent(other)}/exists`),
    staleTime: 0,
    gcTime: 0,
  });
  if (data === undefined || !data.exists) return null;
  return <OtherLines other={other} mine={mine} />;
}

function OtherLines({ other, mine }: { other: string; mine: GradeDue }) {
  const { people, today, totalOf } = useSheetFigures(other);
  const fetching = useIsFetching({ queryKey: ['sheet', other] });
  if (fetching > 0 || today === undefined) return null;
  const theirs = gradeDueOf(people, today, totalOf);
  return (
    <ul>
      {GRADES.map((g) => (
        <li key={g} data-testid="compare-line">
          {g} {formatCents(mine(g))} {formatCents(theirs(g))}
        </li>
      ))}
    </ul>
  );
}
