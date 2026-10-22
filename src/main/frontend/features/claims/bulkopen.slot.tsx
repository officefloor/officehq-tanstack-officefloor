import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetBody } from '../../slots/defs/sheet';
import { claimsKey, useClaims } from './claims';
import { clearSelected, useSelectedClaims } from './selection';

type BulkOpenResult = { opened: number; refused: number };

/** The button that opens every selected claim together. The server opens each that can be opened, in list
 *  order, and leaves the rest as they are; the bar then shows how many were opened and how many refused. */
function BulkOpen({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const claims = useClaims(sheetId).data ?? [];
  const chosen = useSelectedClaims(sheetId);
  const ids = claims.filter((c) => chosen.has(c.id)).map((c) => c.id);
  const [result, setResult] = useState<BulkOpenResult | null>(null);
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const open = useMutation({
    mutationFn: (claimIds: number[]) =>
      postJson<BulkOpenResult>(`/api/sheets/${encodeURIComponent(sheetId)}/bulk-open`, { user, claimIds }),
    onSuccess: async (r) => {
      clearSelected(sheetId);
      setResult(r);
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (e) => setError(e instanceof HttpError && e.body ? e.body : 'Could not open the selected claims.'),
    onSettled: once.done,
  });

  return (
    <p>
      <button
        type="button"
        data-testid="bulk-open"
        disabled={open.isPending}
        onClick={() => {
          setError('');
          setResult(null);
          if (ids.length === 0) {
            setError('Select the claims to open.');
            return;
          }
          if (!once.start()) return;
          open.mutate(ids);
        }}
      >
        Open selected
      </button>
      {result && (
        <>
          {' '}Opened <span data-testid="bulk-opened">{result.opened}</span>, refused{' '}
          <span data-testid="bulk-refused">{result.refused}</span>
        </>
      )}
      {error && (
        <span data-testid="bulk-open-error" role="alert">
          {' '}{error}
        </span>
      )}
    </p>
  );
}

export const contribution = SheetBody.fill({ order: 12, Component: BulkOpen });
