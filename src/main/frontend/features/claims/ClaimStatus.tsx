import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import type { Claim } from './claims';
import { claimsKey } from './claims';

type Action = 'open' | 'settle' | 'void';

const ACTIONS: Array<{ action: Action; label: string }> = [
  { action: 'open', label: 'Open' },
  { action: 'settle', label: 'Settle' },
  { action: 'void', label: 'Void' },
];

// A claim's status and the buttons that change it. Every button is shown; the server refuses a
// change the status does not allow, and the refusal shows as the claim's error.
export function ClaimStatus({ claim }: { claim: Claim }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [error, setError] = useState('');
  const queryClient = useQueryClient();

  const change = useMutation({
    mutationFn: (action: Action) =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheet)}/claims/${claim.id}/${action}?user=${encodeURIComponent(user)}`,
        {},
      ),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    },
    onError: (_e, action) => setError(`This ${claim.status} claim cannot be changed by ${action}${action === 'settle' ? ' (the user who opened it cannot settle it)' : ''}.`),
  });

  return (
    <>
      <span data-testid="claim-status">{claim.status}</span>{' '}
      {ACTIONS.map(({ action, label }) => (
        <button
          key={action}
          type="button"
          data-testid={`claim-${action}`}
          disabled={change.isPending}
          onClick={() => {
            setError('');
            change.mutate(action);
          }}
        >
          {label}
        </button>
      ))}
      {error && <span data-testid="claim-error">{error}</span>}
    </>
  );
}
