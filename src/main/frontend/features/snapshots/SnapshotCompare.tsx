import { useState } from 'react';
import { useClaims } from '../claims/claims';
import { liveHolds, usePeople } from '../people/people';
import { personDue, useRates } from '../rates/rates';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { useDueRules } from '../settings/settings';
import { useSnapshots } from './snapshots';
import type { Snapshot } from './snapshots';

/** The due-total of the latest snapshot written for the person as the sheet left `day`; "" when there is none. */
function snapshotOn(snapshots: Snapshot[], personId: number, day: string): string {
  let found = '';
  for (const s of snapshots) if (s.personId === personId && s.day === day) found = s.dueTotal;
  return found;
}

// Two snapshot dates compared, shown once the button is pressed: one line per person, in list order, as
// `<name> <then> <now>` — `then` their snapshot for the from-date, `now` their snapshot for the to-date, or their
// due-total now when the to-date is blank; a missing figure is left empty and the line trimmed.
export function SnapshotCompare() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople().data ?? [];
  const snapshots = useSnapshots().data ?? [];
  const claims = useClaims().data ?? [];
  const rates = useRates().data ?? [];
  const rules = useDueRules();
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [compared, setCompared] = useState<{ from: string; to: string } | null>(null);

  if (!user || !sheet) return null;

  const lines = compared
    ? people.map((p) => {
        const then = snapshotOn(snapshots, p.id, compared.from);
        const now =
          compared.to === ''
            ? personDue(claims.filter((c) => c.personId === p.id), rates, p.grades, liveHolds(p, rules.today), rules)
            : snapshotOn(snapshots, p.id, compared.to);
        return [p.name, then, now].join(' ').trim();
      })
    : [];

  return (
    <section>
      <input type="date" data-testid="snapshot-from" value={from} onChange={(e) => setFrom(e.target.value)} />
      <input type="date" data-testid="snapshot-to" value={to} onChange={(e) => setTo(e.target.value)} />
      <button type="button" data-testid="snapshot-compare" onClick={() => setCompared({ from, to })}>
        Compare snapshots
      </button>
      {compared && (
        <ul data-testid="snapshot-lines">
          {lines.map((l, i) => (
            <li key={i} data-testid="snapshot-line">
              {l}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
