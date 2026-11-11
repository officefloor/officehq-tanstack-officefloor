import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation } from '@tanstack/react-query';
import { SheetBody } from '../../slots/defs/sheetBody';
import { readFigures } from '../consolidated/consolidated';
import { useGradeCodes } from '../grades/grades';

// The sheet comparison: another sheet's id and a button list, per grade of this sheet,
// <grade> <this sheet's summary due-total for it> <the other sheet's> (0.00 when no one is on it
// there). Both sheets are read fresh on each press; a sheet that does not exist gives no lines.
function CompareSheet({ sheetId }: { sheetId: string }) {
  const GRADES = useGradeCodes(sheetId);
  const [other, setOther] = useState('');
  const [lines, setLines] = useState<string[] | null>(null);

  const compare = useMutation({
    mutationFn: async (otherId: string) => {
      const [own, theirs] = await Promise.all([readFigures(sheetId), readFigures(otherId).catch(() => null)]);
      if (theirs === null) return [];
      return GRADES.map((g) => `${g} ${own.gradeDueTotals[g]} ${theirs.gradeDueTotals[g] || '0.00'}`);
    },
    onSuccess: setLines,
    onError: () => setLines([]),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setLines(null);
    const otherId = other.trim();
    if (otherId === '') setLines([]);
    else compare.mutate(otherId);
  };

  return (
    <section data-testid="compare-sheet-form">
      <form onSubmit={submit}>
        <label>
          Compare with sheet{' '}
          <input data-testid="compare-sheet" value={other} onChange={(e) => setOther(e.target.value)} />
        </label>{' '}
        <button type="submit" data-testid="compare" disabled={compare.isPending}>
          Compare
        </button>
        {lines !== null && (
          <ul>
            {lines.map((line, i) => (
              <li key={i} data-testid="compare-line">
                {line}
              </li>
            ))}
          </ul>
        )}
      </form>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 2.8, Component: CompareSheet });
