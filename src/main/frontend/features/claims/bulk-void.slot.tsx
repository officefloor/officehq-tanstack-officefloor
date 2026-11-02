import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { useVoidReasons, voidReasonsKey } from '../voidreasons/voidreasons';
import { claimsKey, claimsPath } from './claims';
import type { Claim } from './claims';
import { clearSelectedClaims, useSelectedClaims } from './selection';

type BulkVoided = { voided: number; refused: number; claims: Claim[] };

// Voids the selected claims together: the server voids each in list order (people by their list positions, newest
// claim first) exactly as its row's void does, with the reason chosen here while the sheet has void reasons, and
// leaves the rest as they are. Shows how many of each.
function BulkVoid({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const selected = useSelectedClaims(sheet);
  const { data: voidReasons = [] } = useVoidReasons(sheet);
  const [reason, setReason] = useState('');
  const voids = useMutation({
    mutationFn: (ids: number[]) =>
      postJson<BulkVoided>(byUser(`${claimsPath(sheet)}/bulk-void`, user), { claims: ids, reason }),
    onSuccess: (result) => {
      queryClient.setQueryData(claimsKey(sheet), result.claims);
      // A void counts against the reason it was given, in the summary.
      void queryClient.invalidateQueries({ queryKey: voidReasonsKey(sheet) });
      clearSelectedClaims(sheet);
    },
  });
  const act = useSubmitOnce(voids);

  return (
    <p>
      {voidReasons.length > 0 && (
        <>
          <select
            data-testid="bulk-void-reason"
            value={reason}
            onChange={(e) => {
              setReason(e.target.value);
              voids.reset();
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
      <button type="button" data-testid="bulk-void" disabled={voids.isPending} onClick={() => act([...selected])}>
        Void selected
      </button>
      {voids.isSuccess && (
        <>
          {' '}Voided <strong data-testid="bulk-voided">{voids.data.voided}</strong>, refused{' '}
          <strong data-testid="bulk-void-refused">{voids.data.refused}</strong>
        </>
      )}
      {voids.isError && (
        <span data-testid="bulk-void-error" role="alert">
          {' '}The bulk void failed.
        </span>
      )}
    </p>
  );
}

export const contribution = SheetPanels.fill({ order: 22.6, Component: BulkVoid });
