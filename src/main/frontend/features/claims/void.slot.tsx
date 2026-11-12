import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HomeMain } from '../../slots/defs/homeMain';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { useVoidReasons } from '../voidreasons/voidreasons';
import { claimsKey, useSelectedClaims } from './claims';

type BulkVoidResult = { voided: number; refused: number };

// Voids the selected claims together: the server voids each in list order exactly as the row's void
// does (with the chosen reason, needed while the sheet has void reasons) and refuses the rest, and
// the counts show once it answers (gone while it runs).
function BulkVoid() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [selected, setSelected] = useSelectedClaims();
  const voidReasons = useVoidReasons(sheet).data ?? [];
  const [reason, setReason] = useState('');
  const queryClient = useQueryClient();

  const bulk = useMutation({
    mutationFn: (claimIds: number[]) =>
      postJson<BulkVoidResult>(`/api/sheets/${encodeURIComponent(sheet)}/bulk-void`, {
        claimIds: claimIds.map(String),
        user,
        reason: voidReasons.length > 0 ? reason : '',
      }),
    onSuccess: async () => {
      setSelected([]);
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    },
  });
  const submit = useSubmitOnce(bulk);

  return (
    <p>
      {voidReasons.length > 0 && (
        <>
          <select
            data-testid="bulk-void-reason"
            aria-label="Void reason for selected"
            value={reason}
            onChange={(e) => {
              setReason(e.target.value);
              bulk.reset();
            }}
          >
            <option value="">(void reason)</option>
            {voidReasons.map((r) => (
              <option key={r.reason} value={r.reason}>
                {r.reason}
              </option>
            ))}
          </select>{' '}
        </>
      )}
      <button type="button" data-testid="bulk-void" disabled={bulk.isPending} onClick={() => submit(selected)}>
        Void selected
      </button>
      {bulk.isSuccess && (
        <>
          {' '}
          Voided <strong data-testid="bulk-voided">{bulk.data.voided}</strong>, refused{' '}
          <strong data-testid="bulk-void-refused">{bulk.data.refused}</strong>
        </>
      )}
      {bulk.isError && (
        <span data-testid="bulk-void-error" role="alert">
          {' '}
          {bulk.error instanceof HttpError && bulk.error.reason ? `${bulk.error.reason}.` : 'The bulk void was refused.'}
        </span>
      )}
    </p>
  );
}

export const contribution = HomeMain.fill({ order: 23.5, Component: BulkVoid });
