import { type ReactNode, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { GRADES, usePeople } from '../people/usePeople';
import { useDueTotals } from '../rates/useDueTotals';
import { sumDues } from '../rates/useRates';

/** Per grade, the due-totals of the people on it (retired left out) added up, as the sheet's summary shows them. */
function useGradeDueTotals(sheetId: string): Map<string, string> | undefined {
  const people = usePeople(sheetId);
  const dueTotals = useDueTotals(sheetId);
  if (people.data === undefined) return undefined;
  const current = people.data.filter((p) => !p.retiredFrom);
  return new Map(GRADES.map((g) => [g, sumDues(current.filter((p) => p.grade === g).map((p) => dueTotals.get(p.id) ?? 'n/a'))]));
}

/** Works out the other sheet's grade due-totals and shows them through `children` once they are read. */
function OtherFigures({ sheetId, children }: { sheetId: string; children: (totals: Map<string, string>) => ReactNode }) {
  const totals = useGradeDueTotals(sheetId);
  return totals === undefined ? null : children(totals);
}

/** Whether the sheet exists in the store, re-read from it; asking never makes it. */
function useSheetExists(sheetId: string, round: number) {
  return useQuery({
    queryKey: ['sheet', sheetId, 'exists', round],
    queryFn: async () =>
      (await getJson<{ exists: boolean }>(`/api/sheets/${encodeURIComponent(sheetId)}/exists`)).exists,
    enabled: sheetId !== '',
  });
}

/** Compares this sheet with another: a sheet id entry and a button listing, per grade of this sheet,
 *  `<grade> <this sheet's summary due-total for it> <the other sheet's>` (0.00 for a grade the other sheet has no
 *  one on); a sheet that does not exist gives no lines. The other sheet is re-read from the store on each press. */
function SheetCompare({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const mine = useGradeDueTotals(sheetId);
  const [other, setOther] = useState('');
  // The other sheet being read (re-read from the store whenever `round` moves on), and whether to show it.
  const [reading, setReading] = useState<{ id: string; round: number }>({ id: '', round: 0 });
  const [shown, setShown] = useState(false);
  const exists = useSheetExists(reading.id, reading.round);

  const read = (id: string) => {
    if (id !== '' && id !== sheetId) queryClient.removeQueries({ queryKey: ['sheet', id] });
    setReading((r) => ({ id, round: r.round + 1 }));
  };

  return (
    <section>
      <input
        data-testid="compare-sheet"
        value={other}
        onChange={(e) => {
          setOther(e.target.value);
          setShown(false);
          read(e.target.value.trim());
        }}
      />{' '}
      <button
        type="button"
        data-testid="compare"
        onClick={() => {
          if (reading.id !== other.trim() || shown) read(other.trim());
          setShown(true);
        }}
      >
        Compare sheets
      </button>
      {reading.id !== '' && exists.data === true && mine !== undefined && (
        <OtherFigures key={`${reading.id}#${reading.round}`} sheetId={reading.id}>
          {(theirs) => (
            <ul>
              {shown &&
                GRADES.map((g) => (
                  <li key={g} data-testid="compare-line">
                    {[g, mine.get(g) ?? '0.00', theirs.get(g) ?? '0.00'].join(' ')}
                  </li>
                ))}
            </ul>
          )}
        </OtherFigures>
      )}
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 44, Component: ({ sheetId }) => <SheetCompare sheetId={sheetId} /> });
