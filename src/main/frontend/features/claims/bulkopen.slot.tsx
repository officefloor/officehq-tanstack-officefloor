import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useUser } from '../session/session';
import { claimsKey } from './claims';
import { clearSelectedClaims, useSelectedClaims } from './selection';

type Result = { opened: number; refused: number };

// Opens the ticked claims together. The server opens and numbers each that can be opened in list
// order (people in list order, each person's claims newest first) and leaves the rest as they are;
// the bar shows how many were opened and how many refused. The counts are cleared while it runs.
function BulkOpen({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const selected = useSelectedClaims(sheetId);
  const [user] = useUser();
  const [result, setResult] = useState<Result | null>(null);
  const [error, setError] = useState('');

  const once = useOnce();
  const open = useMutation({
    mutationFn: (claimIds: string[]) =>
      postJson<Result>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/bulk-open`, { claimIds }, user),
    onSuccess: (r) => {
      setResult(r);
      clearSelectedClaims(sheetId);
      void queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: () => setError('The selected claims could not be opened.'),
  });

  const press = () => {
    setResult(null);
    setError('');
    const ids = [...selected];
    once((done) => open.mutate(ids, { onSettled: done }));
  };

  return (
    <section data-testid="bulk-claims">
      <button type="button" data-testid="bulk-open" onClick={press} disabled={open.isPending}>
        Open selected ({selected.size})
      </button>
      {result !== null && (
        <>
          {' '}Opened <span data-testid="bulk-opened">{result.opened}</span>, refused{' '}
          <span data-testid="bulk-refused">{result.refused}</span>
        </>
      )}
      {error && (
        <p role="alert" data-testid="bulk-open-error">
          {error}
        </p>
      )}
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 5, Component: BulkOpen });
