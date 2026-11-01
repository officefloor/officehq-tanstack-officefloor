import { useState } from 'react';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { useSheetTotals } from '../claims/useSheetTotals';
import type { Person } from '../people/usePeople';
import { showDue } from '../rates/useRates';

/** The due-total of the person's SNAPSHOT for `day` (the latest one when the today left it more than
 *  once), or '' when there is none. */
function snapshotOn(person: Person, day: string): string {
  let found = '';
  for (const line of person.snapshots) {
    const [left, due] = line.split(' ');
    if (left === day) found = due ?? '';
  }
  return found;
}

/**
 * Compare two snapshot dates: per person in list order, `<name> <then> <now>`. `then` is the due-total
 * of the SNAPSHOT written when the sheet's today left the from-date (empty when there is none); `now`
 * is likewise the one for the to-date, or the person's due-total now when the to-date is blank. Each
 * line is trimmed, so an empty figure leaves no trailing space.
 */
function SnapshotComparePanel({ sheet }: { user: string; sheet: string }) {
  const { perPerson } = useSheetTotals(sheet);
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [shown, setShown] = useState<{ from: string; to: string } | null>(null);

  return (
    <section data-testid="snapshot-compare-panel">
      <h2>Compare snapshots</h2>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          setShown({ from: from.trim(), to: to.trim() });
        }}
      >
        <input
          data-testid="snapshot-from"
          aria-label="From date"
          placeholder="from yyyy-mm-dd"
          value={from}
          onChange={(e) => setFrom(e.target.value)}
        />{' '}
        <input
          data-testid="snapshot-to"
          aria-label="To date"
          placeholder="to yyyy-mm-dd (blank: now)"
          value={to}
          onChange={(e) => setTo(e.target.value)}
        />{' '}
        <button type="submit" data-testid="snapshot-compare">
          Compare
        </button>
      </form>
      {shown && (
        <ul>
          {perPerson.map(({ person, due }) => {
            const then = snapshotOn(person, shown.from);
            const now = shown.to === '' ? showDue(due) : snapshotOn(person, shown.to);
            return (
              <li key={person.id} data-testid="snapshot-line">
                {[person.name, then, now].join(' ').trim()}
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 28.7, Component: SnapshotComparePanel });
