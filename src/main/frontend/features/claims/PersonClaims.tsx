import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { addClaim, claimsKey, updateClaim, type Claim, type ClaimInput } from './api';

const EMPTY: ClaimInput = { amount: '', date: '', note: '' };

/** A person's claims: a form that adds a claim or edits one, and the claims newest first. */
export function PersonClaims({ sheetId, user, personId, claims }: {
  sheetId: string; user: string; personId: number; claims: Claim[];
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

  const change = (patch: Partial<ClaimInput>) => {
    setForm((f) => ({ ...f, ...patch }));
    save.reset();
  };

  const open = (c: Claim) => {
    setForm({ amount: c.amount, date: c.date, note: c.note });
    setEditing(c.id);
    save.reset();
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
              <td data-testid="claim-amount">{Number(c.amount).toFixed(2)}</td>
              <td data-testid="claim-date">{c.date}</td>
              <td data-testid="claim-note">{c.note}</td>
              <td>
                <button type="button" data-testid="claim-edit" onClick={() => open(c)}>
                  Edit
                </button>
                {editing === c.id && (
                  <button type="button" data-testid="claim-cancel" onClick={cancel}>
                    Cancel
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
