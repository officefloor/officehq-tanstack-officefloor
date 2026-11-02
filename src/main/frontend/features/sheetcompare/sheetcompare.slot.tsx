import { useState } from 'react';
import type { FormEvent } from 'react';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { twoDecimals } from '../rates/rates';
import { useGradeDue, useSheetExists } from './sheetcompare';

// The other sheet's figures, read only once it is known to exist (reading a sheet makes it exist).
function OtherLines({ sheet, other }: { sheet: string; other: string }) {
  const mine = useGradeDue(sheet);
  const theirs = useGradeDue(other);
  if (!mine.ready || !theirs.ready) return null;
  return (
    <>
      {mine.grades.map((g) => (
        <div key={g} data-testid="compare-line">
          {`${g} ${twoDecimals(mine.due[g])} ${twoDecimals(theirs.due[g] ?? 0n)}`}
        </div>
      ))}
    </>
  );
}

function Comparison({ sheet, other }: { sheet: string; other: string }) {
  const exists = useSheetExists(other);
  if (!exists.isSuccess || exists.isFetching || !exists.data) return null;
  return <OtherLines sheet={sheet} other={other} />;
}

// Compares this sheet with another by id: one line per grade of this sheet, its summary due-total for the grade then
// the other sheet's (0.00 for a grade the other sheet has no one on). A sheet that does not exist gives no lines.
function SheetCompare({ sheet }: { sheet: string; user: string }) {
  const [entry, setEntry] = useState('');
  // Each press remounts the comparison, so the old lines go and every figure is read afresh.
  const [asked, setAsked] = useState<{ other: string; n: number } | null>(null);

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setAsked((prev) => ({ other: entry.trim(), n: (prev?.n ?? 0) + 1 }));
  };

  return (
    <form onSubmit={submit}>
      Compare with sheet:{' '}
      <input data-testid="compare-sheet" type="text" placeholder="sheet id" value={entry}
        onChange={(e) => setEntry(e.target.value)} />{' '}
      <button type="submit" data-testid="compare">
        Compare
      </button>
      {asked && asked.other !== '' && <Comparison key={asked.n} sheet={sheet} other={asked.other} />}
    </form>
  );
}

export const contribution = SheetPanels.fill({ order: 86, Component: SheetCompare });
