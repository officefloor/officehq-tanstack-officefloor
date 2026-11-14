import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { dueOf, showDue } from '../rates/api';
import { addClaim, claimsKey, moveClaim, updateClaim, type Claim, type ClaimAction, type ClaimInput } from './api';

const EMPTY: ClaimInput = { amount: '', date: '', note: '' };

/** A person's claims: a form that adds a claim or edits one, and the claims newest first, each
 *  with its status, its due at the person's grade's rate (`pct`; none when the grade has no rate)
 *  and buttons that change its status. */
export function PersonClaims({ sheetId, user, personId, claims, pct }: {
  sheetId: string; user: string; personId: number; claims: Claim[]; pct: string | undefined;
}) {
  const queryClient = useQueryClient();
  const [form, setForm] = useState<ClaimInput>(EMPTY);
  const [editing, setEditing] = useState<number | null>(null);

  const save = useMutation({
    mutationFn: (v: { id: number | null; input: ClaimInput }) =>
      v.id === null ? addClaim(sheetId, user, personId, v.input) : updateClaim(sheetId, user, v.id, v.input),
    onSuccess: () => {
      setForm(EMPTY);
      setEditing(null);
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });

  // The claim whose last status change was refused; cleared by the next action on any claim here.
  const move = useMutation({
    mutationFn: (v: { id: number; action: ClaimAction }) => moveClaim(sheetId, user, v.id, v.action),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
  });

  const act = (id: number, action: ClaimAction) => {
    move.reset();
    move.mutate({ id, action });
  };

  const change = (patch: Partial<ClaimInput>) => {
    setForm((f) => ({ ...f, ...patch }));
    save.reset();
  };

  const open = (c: Claim) => {
    setForm({ amount: c.amount, date: c.date, note: c.note });
    setEditing(c.id);
    save.reset();
    move.reset();
  };

  const cancel = () => {
    setForm(EMPTY);
    setEditing(null);
    save.reset();
  };

  return (
    <div>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          save.reset();
          save.mutate({ id: editing, input: form });
        }}
      >
        <input data-testid="claim-form-amount" placeholder="Amount" value={form.amount}
          onChange={(e) => change({ amount: e.target.value })} />{' '}
        <input type="date" data-testid="claim-form-date" value={form.date}
          onChange={(e) => change({ date: e.target.value })} />{' '}
        <input data-testid="claim-form-note" placeholder="Note" value={form.note}
          onChange={(e) => change({ note: e.target.value })} />{' '}
        <button type="submit" data-testid="claim-form-submit" disabled={save.isPending}>
          {editing === null ? 'Add claim' : 'Save claim'}
        </button>
        {save.isError && (
          <span data-testid="claim-form-error">A claim needs an amount of 0.00 or more with up to two decimals.</span>
        )}
      </form>
      <table>
        <tbody>
          {claims.map((c) => (
            <tr key={c.id} data-testid={`claim-row-${c.id}`}>
              <td data-testid="claim-number">{c.number ?? ''}</td>
              <td data-testid="claim-amount">{Number(c.amount).toFixed(2)}</td>
              <td data-testid="claim-date">{c.date}</td>
              <td data-testid="claim-note">{c.note}</td>
              <td data-testid="claim-due">{showDue(dueOf(c.amount, pct))}</td>
              <td data-testid="claim-status">{c.status}</td>
              <td>
                {(['open', 'settle', 'void'] as const).map((a) => (
                  <button key={a} type="button" data-testid={`claim-${a}`} disabled={move.isPending}
                    onClick={() => act(c.id, a)}>
                    {a === 'open' ? 'Open' : a === 'settle' ? 'Settle' : 'Void'}
                  </button>
                ))}{' '}
                <button type="button" data-testid="claim-edit" onClick={() => open(c)}>
                  Edit
                </button>
                {editing === c.id && (
                  <button type="button" data-testid="claim-cancel" onClick={cancel}>
                    Cancel
                  </button>
                )}
                {move.isError && move.variables?.id === c.id && (
                  <span data-testid="claim-error">A {c.status} claim cannot be {move.variables.action === 'open'
                    ? 'opened' : move.variables.action === 'settle' ? 'settled' : 'voided'}.</span>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
