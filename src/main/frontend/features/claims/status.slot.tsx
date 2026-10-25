import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { putJson } from '../../api/http';
import { ClaimCells } from '../../slots/defs/claimRow';
import { usePeople } from '../people/people.slot';
import { useUser } from '../session/useSession';
import { claimsKey, type Claim, type ClaimStatus } from './useClaims';

// `review`: a steward's approve (to SETTLED) or reject (back to OPEN, with a reason) of a PENDING claim.
type Change = { status: ClaimStatus; review?: boolean; reason?: string };

const ACTIONS: Array<{ action: string; change: Change; label: string }> = [
  { action: 'open', change: { status: 'OPEN' }, label: 'Open' },
  { action: 'settle', change: { status: 'SETTLED' }, label: 'Settle' },
  { action: 'void', change: { status: 'VOID' }, label: 'Void' },
  { action: 'approve', change: { status: 'SETTLED', review: true }, label: 'Approve' },
];

// A claim's number and status, the buttons that open, settle, void, approve or reject it, and a steward's move of a DRAFT,
// OPEN or PENDING claim to another person (listed then as that person's newest claim). A change its status does not
// allow, a settle or approve by the user who opened the claim, or an approve or reject by a non-steward, is refused by the
// server, and the refusal shows on the claim until its next action (only the last action's error shows). A clerk's settle above the clerk limit leaves it PENDING.
function ClaimStatusCell({ sheetId, claim }: { sheetId: string; user: string; personId: number; claim: Claim }) {
  const [user] = useUser();
  const queryClient = useQueryClient();
  const [reason, setReason] = useState('');
  const [moveTo, setMoveTo] = useState('');
  const people = usePeople(sheetId);
  const change = useMutation({
    mutationFn: (to: Change) =>
      putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/status`, { ...to, by: user ?? '' }),
    onSuccess: () => {
      setReason('');
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });
  const reassign = useMutation({
    mutationFn: (personId: string) => {
      if (personId === '') throw new Error('Choose the person to move the claim to');
      return putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/person`,
        { personId: Number(personId), by: user ?? '' });
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
  });
  // Each action on the claim clears the other's refusal, so one error shows: the last action's.
  const changeTo = (to: Change) => {
    reassign.reset();
    change.mutate(to);
  };
  const busy = change.isPending || reassign.isPending;
  const error = change.error ?? reassign.error;

  return (
    <>
      <span data-testid="claim-number">{claim.number ?? ''}</span>{' '}
      <span data-testid="claim-status">{claim.status}</span>{' '}
      <span data-testid="claim-opened-by">{claim.openedBy ?? ''}</span>{' '}
      {ACTIONS.map((a) => (
        <button
          key={a.action}
          type="button"
          data-testid={`claim-${a.action}`}
          disabled={busy}
          onClick={() => changeTo(a.change)}
        >
          {a.label}
        </button>
      ))}{' '}
      <input
        data-testid="claim-reject-reason"
        placeholder="Reject reason"
        value={reason}
        onChange={(e) => setReason(e.target.value)}
      />
      <button
        type="button"
        data-testid="claim-reject"
        disabled={busy}
        onClick={() => changeTo({ status: 'OPEN', review: true, reason })}
      >
        Reject
      </button>{' '}
      <select data-testid="claim-reassign-to" value={moveTo} onChange={(e) => setMoveTo(e.target.value)}>
        <option value="">Move to…</option>
        {(people.data ?? []).filter((p) => p.id !== claim.personId).map((p) => (
          <option key={p.id} value={String(p.id)}>
            {p.name}
          </option>
        ))}
      </select>
      <button
        type="button"
        data-testid="claim-reassign"
        disabled={busy}
        onClick={() => {
          change.reset();
          reassign.mutate(moveTo);
        }}
      >
        Move
      </button>{' '}
      {error && (
        <span data-testid="claim-error" role="alert">
          {error.message}
        </span>
      )}
    </>
  );
}

export const contribution = ClaimCells.fill({ order: 20, Component: ClaimStatusCell });
