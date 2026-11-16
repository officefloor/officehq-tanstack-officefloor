import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { SheetPage } from '../../slots/defs/sheetPage';
import { claimsKey } from '../claims/claims';
import { useSelectedClaims } from '../claims/selection';

type Result = { redated: number; refused: number };

/** Re-dates the selected claims together by the signed-in user: each is re-dated in list order (people in order, a
 *  person's claims newest first) by the rules of the single re-date; each that cannot is left as it is. Shows how many
 *  were re-dated and how many refused; the counts are cleared while a new press runs. */
function BulkRedate({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [selected, setSelected] = useSelectedClaims();
  const [date, setDate] = useState('');
  const redate = useMutation({
    mutationFn: (claimIds: number[]) =>
      postJson<Result>(`/api/sheets/${encodeURIComponent(sheetId)}/bulk-redate`, {
        claimIds,
        date: date.trim(),
        by: user,
      }),
    onSuccess: async () => {
      setSelected([]);
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });

  return (
    <p>
      <input
        data-testid="bulk-redate-date"
        aria-label="Re-date selected to"
        placeholder="yyyy-mm-dd"
        value={date}
        onChange={(e) => {
          setDate(e.target.value);
          redate.reset();
        }}
      />{' '}
      <button
        type="button"
        data-testid="bulk-redate"
        disabled={redate.isPending}
        onClick={() => {
          redate.reset();
          redate.mutate(selected);
        }}
      >
        Re-date selected ({selected.length})
      </button>
      {redate.isSuccess && (
        <>
          {' '}
          Re-dated <span data-testid="bulk-redated">{redate.data.redated}</span>, refused{' '}
          <span data-testid="bulk-redate-refused">{redate.data.refused}</span>
        </>
      )}
      {redate.isError && <span data-testid="bulk-redate-error"> Could not re-date the selected claims.</span>}
    </p>
  );
}

export const contribution = SheetPage.fill({ order: 27.5, Component: BulkRedate });
