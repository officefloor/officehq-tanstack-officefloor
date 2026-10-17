import { useState } from 'react';
import type { FormEvent } from 'react';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HomeMain } from '../../slots/defs/homeMain';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { NA, claimDueCents, show, usePersonRate } from '../rates/rates';
import { AMOUNT, CLAIM_ACTIONS, addClaim, canTake, claimAction, claimsKey, selfSettle, total, updateClaim, useClaims } from './claims';
import type { Claim, ClaimAction, ClaimInput } from './claims';

const EMPTY: ClaimInput = { amount: '', date: '', note: '' };
const LABEL: Record<ClaimAction, string> = { open: 'Open', settle: 'Settle', void: 'Void' };
const showDue = (due: bigint | undefined) => (due === undefined ? NA : show(due));
const DONE: Record<ClaimAction, string> = { open: 'opened', settle: 'settled', void: 'voided' };

function ClaimColumns() {
  return (
    <>
      <th>Claimed</th>
      <th>Claims</th>
    </>
  );
}

// A person's claimed total, and their claims: a form that adds a claim or, once a claim's edit is
// pressed, edits it; and the claims listed newest first, each with its status and the buttons that
// change it. An empty date means the sheet's today. A refused status change shows on its claim.
function PersonClaims({ personId }: { personId: number }) {
  const [sheet] = useCurrentSheet();
  const [user] = useCurrentUser();
  const claims = (useClaims().data ?? []).filter((c) => c.personId === personId);
  const rateOf = usePersonRate();
  const [editing, setEditing] = useState<number | undefined>();
  const [form, setForm] = useState<ClaimInput>(EMPTY);
  const [error, setError] = useState('');
  const [claimErrors, setClaimErrors] = useState<Record<number, string>>({});
  const queryClient = useQueryClient();
  const once = useSubmitOnce();

  const clearClaimError = (id: number) =>
    setClaimErrors(({ [id]: _, ...rest }) => rest);
  const refuse = (id: number, message: string) => setClaimErrors((e) => ({ ...e, [id]: message }));

  const act = useMutation({
    mutationFn: ({ claim, action }: { claim: Claim; action: ClaimAction }) =>
      claimAction(sheet!, user, claim.id, action),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheet!) }),
    onError: async (_e, { claim, action }) => {
      refuse(claim.id, `This claim cannot be ${DONE[action]}.`);
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet!) });
    },
  });

  const take = (claim: Claim, action: ClaimAction) => {
    clearClaimError(claim.id);
    if (!canTake(claim, action)) {
      refuse(claim.id, `A ${claim.status} claim cannot be ${DONE[action]}.`);
      return;
    }
    if (selfSettle(claim, user, action)) {
      refuse(claim.id, 'The user who opened a claim cannot settle it.');
      return;
    }
    act.mutate({ claim, action });
  };

  const save = useMutation({
    mutationFn: (input: ClaimInput) =>
      editing !== undefined ? updateClaim(sheet!, user, editing, input) : addClaim(sheet!, user, personId, input),
    onSuccess: async () => {
      setForm(EMPTY);
      setEditing(undefined);
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet!) });
    },
    onError: () => setError('The claim could not be saved: the amount must be 0.00 or more, with up to two decimals.'),
  });

  const change = (patch: Partial<ClaimInput>) => {
    setForm((f) => ({ ...f, ...patch }));
    setError('');
  };

  const open = (claim: Claim) => {
    clearClaimError(claim.id);
    setEditing(claim.id);
    setForm({ amount: claim.amount, date: claim.date, note: claim.note });
    setError('');
  };

  const cancel = () => {
    setEditing(undefined);
    setForm(EMPTY);
    setError('');
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (!AMOUNT.test(form.amount.trim())) {
      setError('The amount must be a number of 0.00 or more, with up to two decimals.');
      return;
    }
    once(() => save.mutateAsync({ amount: form.amount.trim(), date: form.date, note: form.note }));
  };

  return (
    <>
      <td data-testid="person-claimed">{total(claims)}</td>
      <td>
        <form data-testid="claim-form" onSubmit={submit}>
          <input
            data-testid="claim-form-amount"
            placeholder="Amount"
            inputMode="decimal"
            value={form.amount}
            onChange={(e) => change({ amount: e.target.value })}
          />
          <input
            type="date"
            data-testid="claim-form-date"
            value={form.date}
            onChange={(e) => change({ date: e.target.value })}
          />
          <input
            data-testid="claim-form-note"
            placeholder="Note"
            value={form.note}
            onChange={(e) => change({ note: e.target.value })}
          />
          <button type="submit" data-testid="claim-form-submit" disabled={save.isPending}>
            {editing !== undefined ? 'Save claim' : 'Add claim'}
          </button>
          {editing !== undefined && (
            <button type="button" data-testid="claim-form-cancel" onClick={cancel}>
              Cancel
            </button>
          )}
          {error && <span data-testid="claim-form-error">{error}</span>}
        </form>
        <ul data-testid="claim-list">
          {claims.map((c) => (
            <li key={c.id} data-testid={`claim-row-${c.id}`}>
              <span data-testid="claim-amount">{c.amount}</span>{' '}
              <span data-testid="claim-date">{c.date}</span>{' '}
              <span data-testid="claim-note">{c.note}</span>{' '}
              <span data-testid="claim-due">{showDue(claimDueCents(c, rateOf(personId, c.date)))}</span>{' '}
              <span data-testid="claim-status">{c.status}</span>{' '}
              <span data-testid="claim-number">{c.number ?? ''}</span>{' '}
              <span data-testid="claim-opened-by">{c.openedBy ?? ''}</span>{' '}
              <button type="button" data-testid="claim-edit" onClick={() => open(c)}>
                Edit
              </button>
              {CLAIM_ACTIONS.map((a) => (
                <button key={a} type="button" data-testid={`claim-${a}`} onClick={() => take(c, a)}>
                  {LABEL[a]}
                </button>
              ))}
              {claimErrors[c.id] && <span data-testid="claim-error">{claimErrors[c.id]}</span>}
            </li>
          ))}
        </ul>
      </td>
    </>
  );
}

// The sheet's claimed-total: everyone's claimed added up (only OPEN and SETTLED claims count).
function ClaimedTotal() {
  const [sheet] = useCurrentSheet();
  const claims = useClaims();
  if (!sheet) return null;
  return (
    <section data-testid="rollup">
      Claimed total <strong data-testid="rollup-claimed-total">{total(claims.data ?? [])}</strong>
    </section>
  );
}

export const contributions = [
  PersonColumns.fill({ order: 10, Component: ClaimColumns }),
  PersonCells.fill({ order: 10, Component: PersonClaims }),
  HomeMain.fill({ order: 20, Component: ClaimedTotal }),
];
