import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HomeMain } from '../../slots/defs/homeMain';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { NA, dueCents, show, usePersonRate } from '../rates/rates';
import { AMOUNT, addClaim, claimsKey, total, updateClaim, useClaims } from './claims';
import type { Claim, ClaimInput } from './claims';

const EMPTY: ClaimInput = { amount: '', date: '', note: '' };

function ClaimColumns() {
  return (
    <>
      <th>Claimed</th>
      <th>Claims</th>
    </>
  );
}

// A person's claimed total, and their claims: a form that adds a claim or, once a claim's edit is
// pressed, edits it; and the claims listed newest first. An empty date means the sheet's today.
function PersonClaims({ personId }: { personId: number }) {
  const [sheet] = useCurrentSheet();
  const [user] = useCurrentUser();
  const claims = (useClaims().data ?? []).filter((c) => c.personId === personId);
  const rate = usePersonRate()(personId);
  const [editing, setEditing] = useState<number | undefined>();
  const [form, setForm] = useState<ClaimInput>(EMPTY);
  const [error, setError] = useState('');
  const queryClient = useQueryClient();

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
    save.mutate({ amount: form.amount.trim(), date: form.date, note: form.note });
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
              <span data-testid="claim-due">{rate === undefined ? NA : show(dueCents(c.amount, rate))}</span>{' '}
              <button type="button" data-testid="claim-edit" onClick={() => open(c)}>
                Edit
              </button>
            </li>
          ))}
        </ul>
      </td>
    </>
  );
}

// The sheet's claimed-total: everyone's claimed added up.
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
