import { useState } from 'react';
import type { FormEvent } from 'react';
import { useIsFetching, useMutation, useQueryClient } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { GRADES } from '../people/peopleQuery';
import { useGradeDueTotals } from '../rates/due.slot';
import { sheetKey } from '../sheet/sheetQuery';

/** Per grade of this sheet, `<grade> <this sheet's due-total> <the other sheet's>`, as each sheet's summary shows them. */
function CompareLines({ sheetId, other }: { sheetId: string; other: string }) {
  const mine = useGradeDueTotals(sheetId);
  const theirs = useGradeDueTotals(other);
  // Show nothing until both sheets' figures are read, so no line shows a half-loaded sheet.
  const fetching = useIsFetching({ queryKey: sheetKey(other) }) + useIsFetching({ queryKey: sheetKey(sheetId) });
  if (fetching > 0) return null;
  return (
    <>
      {GRADES.map((g, i) => (
        <li key={g} data-testid="compare-line">
          {`${g} ${mine[i]} ${theirs[i]}`}
        </li>
      ))}
    </>
  );
}

/**
 * Compares this sheet with another, named by id: one line per grade (see `CompareLines`). A sheet that does not exist
 * gives no lines, and is not loaded by asking. The other sheet is re-read from the store on every press.
 */
function SheetCompare({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const [other, setOther] = useState('');
  const [compared, setCompared] = useState<string | null>(null);
  const [error, setError] = useState('');

  const compare = useMutation({
    mutationFn: async (id: string) => {
      const exists = id !== '' && (await getJson<boolean>(`/api/sheets/${encodeURIComponent(id)}/exists`));
      if (exists) await queryClient.invalidateQueries({ queryKey: sheetKey(id) });
      return exists ? id : null;
    },
    onSuccess: (id) => setCompared(id),
    onError: (e: Error) => setError(e.message || 'The sheets could not be compared.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setCompared(null);
    compare.mutate(other.trim());
  };

  return (
    <section data-testid="sheet-comparison">
      <h3>Compare with another sheet</h3>
      <form onSubmit={submit}>
        <label>
          Sheet{' '}
          <input
            data-testid="compare-sheet"
            value={other}
            onChange={(e) => {
              setOther(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="compare" disabled={compare.isPending}>
          Compare
        </button>
        {error && <p data-testid="compare-error">{error}</p>}
      </form>
      <ol data-testid="compare-lines">{compared !== null && <CompareLines key={compared} sheetId={sheetId} other={compared} />}</ol>
    </section>
  );
}

// Keyed by sheet, so the id or the lines shown never carry over to another sheet.
export const contribution = SheetPanel.fill({
  order: 39.5,
  Component: ({ sheetId }) => <SheetCompare key={`sheet-compare-${sheetId}`} sheetId={sheetId} />,
});
