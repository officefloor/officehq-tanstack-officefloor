import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { usePeople } from '../people/people';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import type { Claim } from './claims';
import { claimsKey } from './claims';
import { useOneAtATime } from '../../query/useOneAtATime';
import { useClaimError } from './claimError';

// The people the claim is shared with, as `<name> <share>` in the order given, and the entries to share it: a person and
// a percent of its due (two decimals, more than 0.00). Each holder's due-total counts their part, the claim's own
// person's the rest. The server refuses the claim's own person, a person already holding a share, shares above 100.00
// in all, and a SETTLED, VOID or DELETED claim; the refusal shows as the claim's error.
export function ClaimShares({ claim }: { claim: Claim }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [, setError] = useClaimError(claim.id);
  const people = usePeople().data ?? [];
  const [personId, setPersonId] = useState('');
  const [share, setShare] = useState('');
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const add = useMutation({
    mutationFn: (body: { personId: number; share: string }) =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheet)}/claims/${claim.id}/shares?user=${encodeURIComponent(user)}`, body),
    onSuccess: async () => {
      setPersonId('');
      setShare('');
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    },
    onError: () =>
      setError(
        'This claim cannot be shared so (not with its own person or a holder again, shares at most 100.00 in all, more than 0.00 each, and not a SETTLED, VOID or DELETED claim).',
      ),
  });

  const nameOf = (id: number) => people.find((p) => p.id === id)?.name ?? '';
  return (
    <>
      Shared: <span data-testid="claim-shares">{(claim.shares ?? []).map((s) => `${nameOf(s.personId)} ${s.share}`).join(', ')}</span>{' '}
      <select
        data-testid="claim-share-select"
        aria-label="Share with"
        value={personId}
        onChange={(e) => {
          setError('');
          setPersonId(e.target.value);
        }}
      >
        <option value="">share with…</option>
        {people.map((p) => (
          <option key={p.id} value={String(p.id)}>
            {p.name}
          </option>
        ))}
      </select>{' '}
      <input
        data-testid="claim-share-pct"
        placeholder="Share %"
        size={6}
        value={share}
        onChange={(e) => {
          setError('');
          setShare(e.target.value);
        }}
      />{' '}
      <button
        type="button"
        data-testid="claim-share-add"
        disabled={add.isPending}
        onClick={() => {
          setError('');
          if (!personId) {
            setError('Choose the person to share the claim with.');
            return;
          }
          once(() => add.mutateAsync({ personId: Number(personId), share: share.trim() }));
        }}
      >
        Share
      </button>
    </>
  );
}
