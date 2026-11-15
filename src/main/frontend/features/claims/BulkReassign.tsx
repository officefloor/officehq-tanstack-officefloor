import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { bulkReassignClaims, claimsKey } from './api';

/** A select of a person and a button that moves the selected claims to them together, and how many it
 *  moved and refused. The counts are cleared while it runs; the selection is cleared once it is done. */
export function BulkReassign({ sheetId, user, selected, people, onDone }: {
  sheetId: string; user: string; selected: number[]; people: Array<{ id: number; name: string }>; onDone: () => void;
}) {
  const queryClient = useQueryClient();
  const runOnce = useSubmitOnce();
  const [target, setTarget] = useState('');
  const to = target !== '' && people.some((p) => String(p.id) === target) ? target : String(people[0]?.id ?? '');
  const bulk = useMutation({
    mutationFn: (v: { ids: number[]; to: number }) => bulkReassignClaims(sheetId, user, v.ids, v.to),
    onSuccess: () => {
      onDone();
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });

  return (
    <div>
      <select data-testid="bulk-reassign-to" value={to}
        onChange={(e) => {
          setTarget(e.target.value);
          bulk.reset();
        }}>
        {people.map((p) => <option key={p.id} value={String(p.id)}>{p.name}</option>)}
      </select>{' '}
      <button type="button" data-testid="bulk-reassign" disabled={bulk.isPending || to === ''}
        onClick={() => {
          const ids = [...selected];
          const toId = Number(to);
          runOnce(() => {
            bulk.reset();
            return bulk.mutateAsync({ ids, to: toId });
          });
        }}>
        Reassign selected ({selected.length})
      </button>
      {bulk.isSuccess && (
        <>
          {' '}Reassigned <span data-testid="bulk-reassigned">{bulk.data.reassigned}</span>,{' '}
          refused <span data-testid="bulk-reassign-refused">{bulk.data.refused}</span>
        </>
      )}
      {bulk.isError && <span data-testid="bulk-reassign-error">The selected claims could not be reassigned.</span>}
    </div>
  );
}
