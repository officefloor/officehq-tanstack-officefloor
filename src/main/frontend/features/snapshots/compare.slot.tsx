import { useState } from 'react';
import type { FormEvent } from 'react';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { fetchSnapshotCompare } from './snapshots';

// Compares two snapshot dates: per person in list order, their due-total in the SNAPSHOT written on leaving the
// from-date, then likewise for the to-date (or their due-total now when it is blank). A blank from-date is the
// sheet's today.
function SnapshotCompare({ sheet }: { sheet: string; user: string }) {
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [lines, setLines] = useState<string[]>([]);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    // The old lines go as soon as the comparison is asked for again.
    setLines([]);
    setLines(await fetchSnapshotCompare(sheet, from.trim(), to.trim()));
  };

  return (
    <form onSubmit={submit}>
      Compare snapshots:{' '}
      <input data-testid="snapshot-from" type="text" placeholder="from yyyy-mm-dd" value={from}
        onChange={(e) => setFrom(e.target.value)} />{' '}
      <input data-testid="snapshot-to" type="text" placeholder="to yyyy-mm-dd" value={to}
        onChange={(e) => setTo(e.target.value)} />{' '}
      <button type="submit" data-testid="snapshot-compare">
        Compare
      </button>
      {lines.map((line, i) => (
        <div key={i} data-testid="snapshot-line">
          {line}
        </div>
      ))}
    </form>
  );
}

export const contribution = SheetPanels.fill({ order: 85, Component: SnapshotCompare });
