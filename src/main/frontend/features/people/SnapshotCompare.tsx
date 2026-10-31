import { useState } from 'react';
import { showDue } from '../rates/due';
import type { Person } from './usePeople';

/** The due-total of the person's SNAPSHOT taken when the sheet's today was changed away from the date (the
 *  latest when there are several); "" when there is none. */
function snapshotOn(person: Person, date: string): string {
  let found = '';
  for (const line of person.snapshots ?? []) {
    const at = line.indexOf(' ');
    if (line.slice(0, at) === date) found = line.slice(at + 1);
  }
  return found;
}

// Compares two snapshot dates: per person in list order, "<name> <then> <now>" — then the snapshot for the
// from-date, now the snapshot for the to-date or, with the to-date blank, the person's due-total now. Each
// line is trimmed, so an empty figure leaves no trailing space.
export function SnapshotCompare({ people, totalOf }: { people: Person[]; totalOf: (p: Person) => bigint | null }) {
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [asked, setAsked] = useState<{ from: string; to: string } | null>(null);
  const lines =
    asked === null
      ? []
      : people.map((p) =>
          [p.name, snapshotOn(p, asked.from), asked.to === '' ? showDue(totalOf(p)) : snapshotOn(p, asked.to)]
            .join(' ')
            .trim(),
        );
  return (
    <div>
      From <input type="date" data-testid="snapshot-from" value={from} onChange={(e) => setFrom(e.target.value)} /> to{' '}
      <input type="date" data-testid="snapshot-to" value={to} onChange={(e) => setTo(e.target.value)} />{' '}
      <button type="button" data-testid="snapshot-compare" onClick={() => setAsked({ from, to })}>
        Compare snapshots
      </button>
      <ul>
        {lines.map((line, i) => (
          <li key={i} data-testid="snapshot-line">
            {line}
          </li>
        ))}
      </ul>
    </div>
  );
}
