import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson } from '../../api/http';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { usePeople } from '../people/people';
import type { Claim } from './claims';
import { claimsKey } from './claims';
import { useOneAtATime } from '../../query/useOneAtATime';

type Action = 'open' | 'settle' | 'approve' | 'reject' | 'void' | 'resettle';

const ACTIONS: Array<{ action: Action; label: string }> = [
  { action: 'open', label: 'Open' },
  { action: 'settle', label: 'Settle' },
  { action: 'approve', label: 'Approve' },
  { action: 'reject', label: 'Reject' },
  { action: 'void', label: 'Void' },
  { action: 'resettle', label: 'Re-settle' },
];

const REFUSED: Partial<Record<Action, string>> = {
  open: ' (your role may not be allowed to open a claim)',
  settle: ' (the user who opened it cannot settle it, one whose due is above the proof threshold needs a proof, and your role may not be allowed to settle)',
  void: ' (your role may not be allowed to void a claim)',
  approve: ' (only a steward approves a pending claim, and not the user who opened it; one whose due is above the proof threshold needs a proof)',
  reject: ' (only a steward rejects a pending claim, and a reason is required)',
  resettle: ' (only a steward re-settles a settled claim)',
};

// A claim's status and the buttons that change it. Every button is shown; the server refuses a
// change the status does not allow, and the refusal shows as the claim's error. A clerk's settle
// above the clerk limit makes the claim PENDING; a reject sends the reason typed beside it. A role
// allowed to reassign (standing rule: a steward) moves a DRAFT, OPEN or PENDING claim to the person chosen beside the
// Move button. A steward re-settles a SETTLED claim, freezing it again at today's figure. An OPEN claim is deferred to the
// date typed beside the Defer button. The Add proof button raises the claim's proofs count by one (not on a
// SETTLED or VOID claim, nor one dated in a closed period).
export function ClaimStatus({ claim }: { claim: Claim }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [error, setError] = useState('');
  const [reason, setReason] = useState('');
  const [moveTo, setMoveTo] = useState('');
  const [deferTo, setDeferTo] = useState('');
  const others = (usePeople().data ?? []).filter((p) => p.id !== claim.personId);
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const change = useMutation({
    mutationFn: (action: Action) =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheet)}/claims/${claim.id}/${action}?user=${encodeURIComponent(user)}`,
        action === 'reject' ? { reason } : {},
      ),
    onSuccess: async (_c, action) => {
      if (action === 'reject') setReason('');
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    },
    onError: (_e, action) => setError(`This ${claim.status} claim cannot be changed by ${action}${REFUSED[action] ?? ''}.`),
  });

  const reassign = useMutation({
    mutationFn: (personId: number) =>
      putJson<Claim>(`/api/sheets/${encodeURIComponent(sheet)}/claims/${claim.id}/person?user=${encodeURIComponent(user)}`, {
        personId,
      }),
    onSuccess: async () => {
      setMoveTo('');
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    },
    onError: () =>
      setError(`This ${claim.status} claim cannot be moved (only a DRAFT, OPEN or PENDING claim is moved, by a role allowed to reassign).`),
  });

  const defer = useMutation({
    mutationFn: (until: string) =>
      putJson<Claim>(`/api/sheets/${encodeURIComponent(sheet)}/claims/${claim.id}/deferral?user=${encodeURIComponent(user)}`, {
        until,
      }),
    onSuccess: async () => {
      setDeferTo('');
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    },
    onError: () =>
      setError(`This ${claim.status} claim cannot be deferred to that date (only an OPEN claim, to a date after today, outside a closed period).`),
  });

  const proof = useMutation({
    mutationFn: () =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheet)}/claims/${claim.id}/proofs?user=${encodeURIComponent(user)}`, {}),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    },
    onError: () =>
      setError(`This ${claim.status} claim cannot be given a proof (not a SETTLED or VOID claim, nor one in a closed period).`),
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
            once(() => change.mutateAsync(action));
          }}
        >
          {label}
        </button>
      ))}
      <input
        data-testid="claim-reject-reason"
        aria-label="Reject reason"
        placeholder="reason"
        value={reason}
        onChange={(e) => {
          setReason(e.target.value);
          setError('');
        }}
      />
      <select
        data-testid="claim-reassign-to"
        aria-label="Move to"
        value={moveTo}
        onChange={(e) => {
          setMoveTo(e.target.value);
          setError('');
        }}
      >
        <option value="">move to…</option>
        {others.map((p) => (
          <option key={p.id} value={String(p.id)}>
            {p.name}
          </option>
        ))}
      </select>
      <button
        type="button"
        data-testid="claim-reassign"
        disabled={reassign.isPending}
        onClick={() => {
          setError('');
          if (!moveTo) {
            setError('Choose the person to move the claim to.');
            return;
          }
          once(() => reassign.mutateAsync(Number(moveTo)));
        }}
      >
        Move
      </button>
      <input
        data-testid="claim-defer-date"
        aria-label="Defer until"
        placeholder="defer until yyyy-mm-dd"
        value={deferTo}
        onChange={(e) => {
          setDeferTo(e.target.value);
          setError('');
        }}
      />
      <button
        type="button"
        data-testid="claim-defer"
        disabled={defer.isPending}
        onClick={() => {
          setError('');
          const until = deferTo.trim();
          if (!/^\d{4}-\d{2}-\d{2}$/.test(until)) {
            setError('Type the date to defer the claim until as yyyy-mm-dd.');
            return;
          }
          once(() => defer.mutateAsync(until));
        }}
      >
        Defer
      </button>
      Proofs: <span data-testid="claim-proofs">{claim.proofs}</span>{' '}
      <button
        type="button"
        data-testid="claim-proof"
        disabled={proof.isPending}
        onClick={() => {
          setError('');
          once(() => proof.mutateAsync());
        }}
      >
        Add proof
      </button>
      {error && <span data-testid="claim-error">{error}</span>}
    </>
  );
}
