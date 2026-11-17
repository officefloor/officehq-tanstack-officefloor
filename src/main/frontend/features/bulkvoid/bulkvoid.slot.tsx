import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { SheetPage } from '../../slots/defs/sheetPage';
import { claimsKey } from '../claims/claims';
import { useSelectedClaims } from '../claims/selection';
import { useVoidReasons } from '../voidreasons/voidreasons';

type Result = { voided: number; refused: number };

/** Voids the selected claims together, with the void reason chosen (the select shows while the sheet has void
 *  reasons): each is voided in list order exactly as its row's void does; each that cannot is left as it is. Shows
 *  how many were voided and how many refused; the counts are cleared while a new press runs. */
function BulkVoid({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [selected, setSelected] = useSelectedClaims();
  const voidReasons = useVoidReasons(sheetId);
  const [reason, setReason] = useState('');
  const bulkVoid = useMutation({
    mutationFn: (claimIds: number[]) =>
      postJson<Result>(`/api/sheets/${encodeURIComponent(sheetId)}/bulk-void`, { claimIds, reason, by: user }),
    onSuccess: async () => {
      setSelected([]);
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });

  return (
    <p>
      {(voidReasons.data ?? []).length > 0 && (
        <>
          <select
            data-testid="bulk-void-reason"
            aria-label="Void reason for the selected claims"
            value={reason}
            onChange={(e) => {
              setReason(e.target.value);
              bulkVoid.reset();
            }}
          >
            <option value="">(no reason)</option>
            {(voidReasons.data ?? []).map((r) => (
              <option key={r.reason} value={r.reason}>
                {r.reason}
              </option>
            ))}
          </select>{' '}
        </>
      )}
      <button
        type="button"
        data-testid="bulk-void"
        disabled={bulkVoid.isPending}
        onClick={() => {
          bulkVoid.reset();
          bulkVoid.mutate(selected);
        }}
      >
        Void selected ({selected.length})
      </button>
      {bulkVoid.isSuccess && (
        <>
          {' '}
          Voided <span data-testid="bulk-voided">{bulkVoid.data.voided}</span>, refused{' '}
          <span data-testid="bulk-void-refused">{bulkVoid.data.refused}</span>
        </>
      )}
      {bulkVoid.isError && <span data-testid="bulk-void-error"> Could not void the selected claims.</span>}
    </p>
  );
}

export const contribution = SheetPage.fill({ order: 26.5, Component: BulkVoid });
