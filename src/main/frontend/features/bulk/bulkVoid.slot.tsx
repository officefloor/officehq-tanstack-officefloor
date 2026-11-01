import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { claimsKey } from '../claims/useClaims';
import { useVoidReasons } from '../voidreasons/useVoidReasons';
import { useSelectedClaims } from './useSelectedClaims';

/**
 * Void the ticked claims together, with a void reason chosen from the sheet's list (offered only
 * while the sheet has void reasons). Each that can be voided is voided as the row's void would be,
 * in list order (people in list order, each person's newest claim first); each that cannot is left
 * as it is. Shows how many were voided and how many refused; the counts go while the next press runs.
 */
function BulkVoid({ user, sheet }: { user: string; sheet: string }) {
  const queryClient = useQueryClient();
  const [selected, setSelected] = useSelectedClaims();
  const { data: voidReasons = [] } = useVoidReasons(sheet);
  const [reason, setReason] = useState('');
  const bulk = useMutation({
    mutationFn: (claimIds: number[]) =>
      postJson<{ voided: number; refused: number }>(`/api/sheets/${encodeURIComponent(sheet)}/bulk-void`, {
        claimIds,
        user,
        reason: voidReasons.length > 0 && reason !== '' ? reason : null,
      }),
    onSuccess: () => setSelected([]),
    onSettled: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
  });
  return (
    <section data-testid="bulk-void-panel">
      {voidReasons.length > 0 && (
        <>
          <select
            data-testid="bulk-void-reason"
            aria-label="Reason for voiding the selected claims"
            value={reason}
            onChange={(e) => {
              setReason(e.target.value);
              bulk.reset();
            }}
          >
            <option value="">(void reason)</option>
            {voidReasons.map((v) => (
              <option key={v.reason} value={v.reason}>
                {v.reason}
              </option>
            ))}
          </select>{' '}
        </>
      )}
      <button
        type="button"
        data-testid="bulk-void"
        disabled={bulk.isPending}
        onClick={() => {
          bulk.reset();
          bulk.mutate(selected);
        }}
      >
        Void selected ({selected.length})
      </button>
      {bulk.isSuccess && (
        <span>
          {' '}
          Voided <strong data-testid="bulk-voided">{bulk.data.voided}</strong>, refused{' '}
          <strong data-testid="bulk-void-refused">{bulk.data.refused}</strong>
        </span>
      )}
      {bulk.isError && (
        <span data-testid="bulk-void-error" role="alert">
          {' '}
          {selected.length === 0 ? 'Select the claims to void.' : 'Could not void the selected claims.'}
        </span>
      )}
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 16.6, Component: BulkVoid });
