import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { SheetPage } from '../../slots/defs/sheetPage';
import { useClaims } from '../claims/claims';
import { useGradeDue } from '../header/totals';
import { GRADES, usePeople } from '../people/people';
import { useRates } from '../rates/rates';
import { useSheet } from '../session/session';
import { useSettings } from '../settings/settings';

/** The lines comparing this sheet with the other: one per grade, <grade> <this sheet's summary due-total for it>
 *  <the other sheet's>, shown only once every figure of the other sheet has been read. */
function CompareLines({ sheetId, otherId }: { sheetId: string; otherId: string }) {
  const mine = useGradeDue(sheetId);
  const theirs = useGradeDue(otherId);
  const read = [usePeople(otherId), useClaims(otherId), useRates(otherId), useSettings(otherId), useSheet(otherId)];
  if (!read.every((q) => q.isSuccess)) return null;
  return (
    <ul>
      {GRADES.map((g) => (
        <li key={g} data-testid="compare-line">
          {`${g} ${mine(g)} ${theirs(g)}`}
        </li>
      ))}
    </ul>
  );
}

/** Compares this sheet with another by id: each grade's summary due-total on both. A sheet that does not exist gives
 *  no lines. The other sheet is re-read from the store each time Compare is pressed. */
function CompareSheet({ sheetId }: { sheetId: string }) {
  const [other, setOther] = useState('');
  const queryClient = useQueryClient();
  const compare = useMutation({
    mutationFn: async (id: string) => {
      const { exists } = await getJson<{ exists: boolean }>(`/api/sheets/${encodeURIComponent(id)}/exists`, true);
      if (exists && id !== sheetId) queryClient.removeQueries({ queryKey: ['sheet', id] });
      return exists ? id : null;
    },
  });
  return (
    <section data-testid="compare-view">
      <h3>Compare with sheet</h3>
      <input
        data-testid="compare-sheet"
        value={other}
        onChange={(e) => {
          setOther(e.target.value);
          compare.reset();
        }}
      />
      <button
        type="button"
        data-testid="compare"
        disabled={compare.isPending}
        onClick={() => {
          compare.reset();
          compare.mutate(other.trim());
        }}
      >
        Compare
      </button>
      {compare.isError && <p data-testid="compare-error">Could not compare the sheets.</p>}
      {compare.data && <CompareLines sheetId={sheetId} otherId={compare.data} />}
    </section>
  );
}

export const contribution = SheetPage.fill({ order: 42, Component: CompareSheet });
