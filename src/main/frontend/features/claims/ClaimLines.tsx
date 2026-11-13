import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import type { Claim } from './claims';
import { AMOUNT, claimsKey } from './claims';
import { useOneAtATime } from '../../query/useOneAtATime';
import { useClaimError } from './claimError';

/** A quantity a line takes: a whole number, at least 1. */
const QTY = /^\d+$/;

// The claim's lines, in the order added, and the entries to add one: a label, a quantity and a unit amount. A claim
// with lines takes their quantities times their unit amounts, added up, as its amount. The server refuses a line on a
// claim that is not a DRAFT, in a closed period or locked by another user; the refusal shows as the claim's error.
export function ClaimLines({ claim }: { claim: Claim }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [, setError] = useClaimError(claim.id);
  const [label, setLabel] = useState('');
  const [qty, setQty] = useState('');
  const [unit, setUnit] = useState('');
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const add = useMutation({
    mutationFn: (body: { label: string; qty: string; unit: string }) =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheet)}/claims/${claim.id}/lines?user=${encodeURIComponent(user)}`, body),
    onSuccess: async () => {
      setLabel('');
      setQty('');
      setUnit('');
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    },
    onError: () =>
      setError('This line cannot be added (only a DRAFT claim, outside a closed period and not being edited by another user, takes lines).'),
  });

  return (
    <>
      <ol>
        {(claim.lines ?? []).map((l, i) => (
          <li key={i} data-testid="claim-lines-line">
            {`${l.label} ${l.qty} x ${l.unit}`}
          </li>
        ))}
      </ol>
      <input
        data-testid="claim-line-label"
        placeholder="Label"
        value={label}
        onChange={(e) => {
          setError('');
          setLabel(e.target.value);
        }}
      />{' '}
      <input
        data-testid="claim-line-qty"
        placeholder="Qty"
        size={4}
        value={qty}
        onChange={(e) => {
          setError('');
          setQty(e.target.value);
        }}
      />{' '}
      <input
        data-testid="claim-line-unit"
        placeholder="Unit"
        size={8}
        value={unit}
        onChange={(e) => {
          setError('');
          setUnit(e.target.value);
        }}
      />{' '}
      <button
        type="button"
        data-testid="claim-line-add"
        disabled={add.isPending}
        onClick={() => {
          setError('');
          if (label.trim() === '') {
            setError('Type a label.');
            return;
          }
          if (!QTY.test(qty.trim()) || Number(qty.trim()) < 1) {
            setError('Type a quantity: a whole number, at least 1.');
            return;
          }
          if (!AMOUNT.test(unit.trim())) {
            setError('Type a unit amount with at most two decimals.');
            return;
          }
          once(() => add.mutateAsync({ label: label.trim(), qty: qty.trim(), unit: unit.trim() }));
        }}
      >
        Add line
      </button>
    </>
  );
}
