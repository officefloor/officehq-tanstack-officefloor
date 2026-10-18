import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { PeopleBar } from '../../slots/defs/peopleBar';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { bulkOpenClaims, claimsKey, useSelectedClaims } from './claims';

// Open the ticked claims together. The server opens each that can be opened (a DRAFT not dated inside
// a closed period), numbering them in list order, and leaves the rest; the counts opened and refused
// are shown until the selection changes or the button is pressed again.
function BulkOpen() {
  const [sheet] = useCurrentSheet();
  const [user] = useCurrentUser();
  const selection = useSelectedClaims();
  const [result, setResult] = useState<{ opened: number; refused: number } | null>(null);
  const [failed, setFailed] = useState(false);
  const queryClient = useQueryClient();
  const once = useSubmitOnce();

  const run = useMutation({
    mutationFn: (ids: number[]) => bulkOpenClaims(sheet!, user, ids),
    onSuccess: async (r) => {
      selection.clear();
      setResult(r);
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet!) });
    },
    onError: () => setFailed(true),
  });

  if (!sheet) return null;

  const press = () => {
    setResult(null);
    setFailed(false);
    once(() => run.mutateAsync([...selection.ids]));
  };

  // A result belongs to the selection it left (empty); ticking claims again clears it.
  const shown = result && selection.ids.size === 0 ? result : null;

  return (
    <span data-testid="bulk">
      <button type="button" data-testid="bulk-open" onClick={press} disabled={run.isPending}>
        Open selected ({selection.ids.size})
      </button>
      {shown && (
        <span>
          {' '}Opened <span data-testid="bulk-opened">{shown.opened}</span>, refused{' '}
          <span data-testid="bulk-refused">{shown.refused}</span>
        </span>
      )}
      {failed && <span data-testid="bulk-error"> The selected claims could not be opened.</span>}
    </span>
  );
}

export const contribution = PeopleBar.fill({ order: 15, Component: BulkOpen });
