import { useState, type ChangeEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson } from '../../api/http';
import { ClaimCells } from '../../slots/defs/claimRow';
import { PersonCells, PersonHeads } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useUser } from '../session/useSession';
import { claimsKey, sumAmounts, useClaims, useEditingClaim, type Claim } from './useClaims';
import { useSubmitOnce } from '../../mutation/useSubmitOnce';

const AMOUNT = /^\d+(\.\d{1,2})?$/;

type ClaimBody = { amount: string; date: string; note: string };

// A person's claim form: adds a claim, or — with one of their claims' editor open — saves its
// changes. It is re-mounted for each claim opened, so it starts filled with that claim's values.
function ClaimForm({ sheetId, personId, editing }: { sheetId: string; personId: number; editing?: Claim }) {
  const [, setEditing] = useEditingClaim();
  const [amount, setAmount] = useState(editing?.amount ?? '');
  const [date, setDate] = useState(editing?.date ?? '');
  const [note, setNote] = useState(editing?.note ?? '');
  const [user] = useUser();
  const queryClient = useQueryClient();
  const sheet = `/api/sheets/${encodeURIComponent(sheetId)}`;
  const save = useMutation({
    mutationFn: async (body: ClaimBody) => {
      if (body.amount.trim() === '') throw new Error('Enter an amount');
      if (!AMOUNT.test(body.amount.trim())) throw new Error('An amount is a number, 0.00 or more, with up to two decimals');
      // `by` is the signed-in user the change is recorded against.
      const sent = { ...body, by: user ?? '' };
      return editing
        ? putJson<Claim>(`${sheet}/claims/${editing.id}`, sent)
        : postJson<Claim>(`${sheet}/people/${personId}/claims`, sent);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
      if (editing) {
        setEditing(undefined);
      } else {
        setAmount('');
        setDate('');
        setNote('');
      }
    },
  });
  const field = (set: (v: string) => void) => (e: ChangeEvent<HTMLInputElement>) => {
    set(e.target.value);
    save.reset();
  };

  const submit = useSubmitOnce(save);
  return (
    <form
      data-testid="claim-form"
      onSubmit={(e) => {
        e.preventDefault();
        submit({ amount, date, note });
      }}
    >
      <input data-testid="claim-form-amount" placeholder="Amount" inputMode="decimal" value={amount} onChange={field(setAmount)} />{' '}
      <input data-testid="claim-form-date" type="date" value={date} onChange={field(setDate)} />{' '}
      <input data-testid="claim-form-note" placeholder="Note" value={note} onChange={field(setNote)} />{' '}
      <button type="submit" data-testid="claim-form-submit" disabled={save.isPending}>
        {editing ? 'Save claim' : 'Add claim'}
      </button>
      {save.isError && (
        <p data-testid="claim-form-error" role="alert">
          {save.error.message}
        </p>
      )}
    </form>
  );
}

function ClaimsHead() {
  return (
    <>
      <th>Claimed</th>
      <th>Claims</th>
    </>
  );
}

// A person's claimed total, and their claims, newest first, under the form that adds or edits them.
function ClaimsCells({ sheetId, user, personId }: { sheetId: string; user: string; personId: number }) {
  const claims = useClaims(sheetId);
  const [editingId, setEditing] = useEditingClaim();
  const mine = (claims.data ?? []).filter((c) => c.personId === personId);
  const editing = mine.find((c) => c.id === editingId);

  return (
    <>
      <td data-testid="person-claimed">{sumAmounts(mine)}</td>
      <td>
        <ClaimForm key={editing ? `edit-${editing.id}` : 'add'} sheetId={sheetId} personId={personId} editing={editing} />
        <ul data-testid="claim-list">
          {mine.map((c) => (
            <li key={c.id} data-testid={`claim-row-${c.id}`}>
              <span data-testid="claim-amount">{Number(c.amount).toFixed(2)}</span>{' '}
              <span data-testid="claim-date">{c.date}</span>{' '}
              <span data-testid="claim-note">{c.note}</span>{' '}
              <ClaimCells.Slot sheetId={sheetId} user={user} personId={personId} claim={c} />
              {editing?.id === c.id ? (
                <button type="button" data-testid="claim-cancel" onClick={() => setEditing(undefined)}>
                  Cancel
                </button>
              ) : (
                <button type="button" data-testid="claim-edit" onClick={() => setEditing(c.id)}>
                  Edit
                </button>
              )}
            </li>
          ))}
        </ul>
      </td>
    </>
  );
}

// The sheet's claimed-total: everyone's counted claims added up.
function ClaimedTotal({ sheetId }: { sheetId: string; user: string }) {
  const claims = useClaims(sheetId);
  return (
    <section data-testid="claims-rollup">
      <p>
        Claimed total: <strong data-testid="rollup-claimed-total">{sumAmounts(claims.data ?? [])}</strong>
      </p>
      {claims.isError && (
        <p data-testid="claims-error" role="alert">
          {claims.error.message}
        </p>
      )}
    </section>
  );
}

export const contributions = [
  PersonHeads.fill({ order: 10, Component: ClaimsHead }),
  PersonCells.fill({ order: 10, Component: ClaimsCells }),
  SheetBody.fill({ order: 20, Component: ClaimedTotal }),
];
