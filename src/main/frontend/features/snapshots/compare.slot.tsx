import { useState } from 'react';
import { SheetPage } from '../../slots/defs/sheetPage';
import { usePeopleDue } from '../header/totals';
import { usePeople } from '../people/people';
import { useSnapshots, type Snapshot } from './snapshots';

/** The person's due-total in the latest SNAPSHOT written when the sheet's today was changed away from the day, or ''
 *  when none was. */
function snapshotOn(snapshots: Snapshot[], personId: number, day: string): string {
  let found = '';
  for (const s of snapshots) if (s.personId === personId && s.day === day) found = s.dueTotal;
  return found;
}

/** Compares two snapshot dates: one line per person in list order, "<name> <then> <now>", where `then` is their
 *  snapshot of the from-date and `now` their snapshot of the to-date, or their due-total now when the to-date is
 *  blank; a missing snapshot is empty. Each line is trimmed. */
function SnapshotCompare({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId);
  const snapshots = useSnapshots(sheetId);
  const due = usePeopleDue(sheetId);
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [asked, setAsked] = useState<{ from: string; to: string } | null>(null);
  const all = snapshots.data ?? [];
  const lines =
    asked === null
      ? []
      : (people.data ?? []).map((p) => {
          const then = snapshotOn(all, p.id, asked.from);
          const now = asked.to === '' ? (due.get(p.id) ?? '') : snapshotOn(all, p.id, asked.to);
          return `${p.name} ${then} ${now}`.trimEnd();
        });

  return (
    <section data-testid="snapshot-compare-panel">
      <h3>Compare snapshots</h3>
      <input
        type="date"
        data-testid="snapshot-from"
        value={from}
        onChange={(e) => {
          setFrom(e.target.value);
          setAsked(null);
        }}
      />{' '}
      <input
        type="date"
        data-testid="snapshot-to"
        value={to}
        onChange={(e) => {
          setTo(e.target.value);
          setAsked(null);
        }}
      />{' '}
      <button type="button" data-testid="snapshot-compare" onClick={() => setAsked({ from: from.trim(), to: to.trim() })}>
        Compare
      </button>
      {lines.map((line, i) => (
        <div key={i} data-testid="snapshot-line">
          {line}
        </div>
      ))}
    </section>
  );
}

export const contribution = SheetPage.fill({ order: 96, Component: SnapshotCompare });
