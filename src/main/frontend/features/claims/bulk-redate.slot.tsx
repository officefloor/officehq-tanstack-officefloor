import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { claimsKey, claimsPath } from './claims';
import type { Claim } from './claims';
import { clearSelectedClaims, useSelectedClaims } from './selection';

type BulkRedated = { redated: number; refused: number; claims: Claim[] };

// Re-dates the selected claims together to the date typed beside the button: the server re-dates each in list order
// (people by their list positions, newest claim first) by the rules of re-dating one, and leaves the rest as they
// are. Shows how many of each.
function BulkRedate({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const selected = useSelectedClaims(sheet);
  const [date, setDate] = useState('');
  const redate = useMutation({
    mutationFn: ({ ids, date }: { ids: number[]; date: string }) =>
      postJson<BulkRedated>(byUser(`${claimsPath(sheet)}/bulk-redate`, user), { claims: ids, date }),
    onSuccess: (result) => {
      queryClient.setQueryData(claimsKey(sheet), result.claims);
      clearSelectedClaims(sheet);
    },
  });
  const act = useSubmitOnce(redate);

  return (
    <p>
      <input
        data-testid="bulk-redate-date"
        placeholder="re-date yyyy-mm-dd"
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
        onClick={() => act({ ids: [...selected], date: date.trim() })}
      >
        Re-date selected
      </button>
      {redate.isSuccess && (
        <>
          {' '}Re-dated <strong data-testid="bulk-redated">{redate.data.redated}</strong>, refused{' '}
          <strong data-testid="bulk-redate-refused">{redate.data.refused}</strong>
        </>
      )}
      {redate.isError && (
        <span data-testid="bulk-redate-error" role="alert">
          {' '}The bulk re-date failed.
        </span>
      )}
    </p>
  );
}

export const contribution = SheetPanels.fill({ order: 22.7, Component: BulkRedate });
