import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { postJson, RefusedError } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { ClaimCells } from '../../slots/defs/claimColumns';
import { peopleQuery } from '../people/peopleQuery';
import { useUser } from '../session/useUser';
import { claimsKey } from './claimsQuery';
import type { Claim } from './claimsQuery';
import { useClaimError } from './useClaimError';

/**
 * The people the claim is shared with, as "<name> <share>" comma and space separated, and the form that shares it with
 * another person on the sheet at a percent (two decimals, more than 0.00). The server refuses the claim's own person,
 * a person already holding a share, shares adding up to more than 100.00, and a SETTLED, VOID or DELETED claim; the
 * refusal shows on the claim (in its shared `claim-error`) until its next action.
 */
function ClaimShare({ sheetId, claim }: { sheetId: string; claim: Claim }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const people = useQuery(peopleQuery(sheetId)).data ?? [];
  const [to, setTo] = useState('');
  const [pct, setPct] = useState('');
  const [, setError] = useClaimError(claim.id);
  const once = useSubmitOnce();
  const share = useMutation({
    mutationFn: (body: { personId: number; share: string }) =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/shares`, body, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
    onError: (e) => setError(e instanceof RefusedError ? e.message : 'The claim could not be shared.'),
  });
  const shown = (claim.shares ?? [])
    .map((s) => `${people.find((p) => p.id === s.personId)?.name ?? ''} ${s.share}`)
    .join(', ');

  return (
    <td>
      <span data-testid="claim-shares">{shown}</span>{' '}
      <select
        data-testid="claim-share-select"
        value={to}
        onChange={(e) => {
          setTo(e.target.value);
          setError('');
        }}
      >
        <option value="">Share with…</option>
        {people.map((p) => (
          <option key={p.id} value={p.id}>
            {p.name}
          </option>
        ))}
      </select>
      <input
        data-testid="claim-share-pct"
        placeholder="Share %"
        size={6}
        value={pct}
        onChange={(e) => {
          setPct(e.target.value);
          setError('');
        }}
      />
      <button
        type="button"
        data-testid="claim-share-add"
        disabled={share.isPending}
        onClick={() => {
          setError('');
          if (!to) {
            setError('Choose a person to share the claim with.');
            return;
          }
          once(() => share.mutateAsync({ personId: Number(to), share: pct }));
        }}
      >
        Share
      </button>
    </td>
  );
}

export const contribution = ClaimCells.fill({ order: 27.7, Component: ClaimShare });
