import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useClaims } from '../claims/claims';
import { liveHolds, peopleQuery } from '../people/people';
import type { Person } from '../people/people';
import { heldParts, personDueTotal, useRates } from '../rates/rates';
import { useCap, useFloor, useLate } from '../access/access';
import { useSheetToday } from '../sheet/sheet';
import { useCutTotals } from '../pools/pools';

/** A person's due-total in the SNAPSHOT written when the sheet's today was changed away from `day` ('' when none; the last when several). */
function snapshotOn(p: Person, day: string): string {
  let due = '';
  for (const line of p.snapshots) {
    const at = line.indexOf(' ');
    if (line.slice(0, at) === day) due = line.slice(at + 1);
  }
  return due;
}

// The snapshot comparison: a from-date, a to-date and a button list, per person in list order,
// <name> <then> <now>: `then` their due-total in the snapshot for the from-date ('' when none),
// `now` likewise for the to-date, or their due-total now when the to-date is blank. Each line is
// trimmed, so an empty figure leaves no trailing space. The people are read fresh on each press.
function SnapshotCompare({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const cap = useCap(sheetId);
  const cutOf = useCutTotals(sheetId);
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [lines, setLines] = useState<string[] | null>(null);
  const [error, setError] = useState('');

  const compare = useMutation({
    mutationFn: (dates: { from: string; to: string }) =>
      queryClient.fetchQuery({ ...peopleQuery(sheetId), staleTime: 0 }).then((people) => ({ people, ...dates })),
    onSuccess: ({ people, from, to }) =>
      setLines(
        people.map((p) => {
          const now =
            to === ''
              ? personDueTotal(rates, p.grades, today, claims.filter((c) => c.personId === p.id), liveHolds(p, today), floor, late, cap, heldParts(p.id, people, claims, rates, today, floor, late), cutOf(p.id))
              : snapshotOn(p, to);
          return [p.name, snapshotOn(p, from), now].join(' ').trim();
        }),
      ),
    onError: () => setError('The snapshots could not be compared.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setLines(null);
    setError('');
    compare.mutate({ from: from.trim(), to: to.trim() });
  };

  return (
    <section data-testid="snapshot-compare-form">
      <form onSubmit={submit}>
        <label>
          Snapshot from{' '}
          <input type="date" data-testid="snapshot-from" value={from} onChange={(e) => setFrom(e.target.value)} />
        </label>{' '}
        <label>
          to{' '}
          <input type="date" data-testid="snapshot-to" value={to} onChange={(e) => setTo(e.target.value)} />
        </label>{' '}
        <button type="submit" data-testid="snapshot-compare" disabled={compare.isPending}>
          Compare snapshots
        </button>
        {lines !== null && (
          <ul>
            {lines.map((line, i) => (
              <li key={i} data-testid="snapshot-line">
                {line}
              </li>
            ))}
          </ul>
        )}
        {error && (
          <p role="alert" data-testid="snapshot-compare-error">
            {error}
          </p>
        )}
      </form>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 2.7, Component: SnapshotCompare });
