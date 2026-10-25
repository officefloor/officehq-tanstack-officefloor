import { useState, type ChangeEvent } from 'react';
import { useIsMutating, useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson } from '../../api/http';
import { ClaimCells } from '../../slots/defs/claimRow';
import { PersonCells, PersonHeads } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useUser } from '../session/useSession';
import { useHeld } from '../people/people.slot';
import { claimLockKey, claimSaveKey, claimsKey, sumAmounts, useClaims, useEditingClaim, type Claim } from './useClaims';
import { useSubmitOnce } from '../../mutation/useSubmitOnce';

const AMOUNT = /^-?\d+(\.\d{1,2})?$/;

type ClaimBody = { amount: string; date: string; note: string; corrects: string; ref: string };

// A person's claim form: adds a claim, or — with one of their claims' editor open — saves its
// changes. It is re-mounted for each claim opened, so it starts filled with that claim's values.
function ClaimForm({ sheetId, personId, editing }: { sheetId: string; personId: number; editing?: Claim }) {
  const [, setEditing] = useEditingClaim();
  const [amount, setAmount] = useState(editing?.amount ?? '');
  const [date, setDate] = useState(editing?.date ?? '');
  const [note, setNote] = useState(editing?.note ?? '');
  const [corrects, setCorrects] = useState(editing?.corrects ?? '');
  const [ref, setRef] = useState(editing?.ref ?? '');
  const [user] = useUser();
  const queryClient = useQueryClient();
  const sheet = `/api/sheets/${encodeURIComponent(sheetId)}`;
  const save = useMutation({
    mutationKey: claimSaveKey(sheetId),
    mutationFn: async (body: ClaimBody) => {
      if (body.amount.trim() === '') throw new Error('Enter an amount');
      if (!AMOUNT.test(body.amount.trim())) throw new Error('An amount is a number with up to two decimals');
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
        setCorrects('');
        setRef('');
      }
    },
  });
  const field = (set: (v: string) => void) => (e: ChangeEvent<HTMLInputElement>) => {
    set(e.target.value);
    save.reset();
  };

  // While a claim's editor is being opened, the form may be about to become its editor: it waits.
  const locking = useIsMutating({ mutationKey: claimLockKey(sheetId) }) > 0;
  const submit = useSubmitOnce(save);
  return (
    <form
      data-testid="claim-form"
      onSubmit={(e) => {
        e.preventDefault();
        submit({ amount, date, note, corrects, ref });
      }}
    >
      <input data-testid="claim-form-amount" placeholder="Amount" inputMode="decimal" value={amount} onChange={field(setAmount)} />{' '}
      <input data-testid="claim-form-date" type="date" value={date} onChange={field(setDate)} />{' '}
      <input data-testid="claim-form-note" placeholder="Note" value={note} onChange={field(setNote)} />{' '}
      <input data-testid="claim-form-corrects" placeholder="Corrects no." value={corrects} onChange={field(setCorrects)} />{' '}
      <input data-testid="claim-form-ref" placeholder="Reference" value={ref} onChange={field(setRef)} />{' '}
      <button type="submit" data-testid="claim-form-submit" disabled={save.isPending || locking}>
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

// A person's claimed total, and their claims, newest first, under the form that adds or edits them. A claim's edit and
// cancel buttons are in its status cell, with its locking.
function ClaimsCells({ sheetId, user, personId }: { sheetId: string; user: string; personId: number }) {
  const claims = useClaims(sheetId);
  const [editingId] = useEditingClaim();
  const held = useHeld(sheetId);
  const mine = (claims.data ?? []).filter((c) => c.personId === personId);
  const editing = mine.find((c) => c.id === editingId);

  return (
    <>
      <td data-testid="person-claimed">{sumAmounts(mine, held)}</td>
      <td>
        <ClaimForm key={editing ? `edit-${editing.id}` : 'add'} sheetId={sheetId} personId={personId} editing={editing} />
        <ul data-testid="claim-list">
          {mine.map((c) => (
            <li key={c.id} data-testid={`claim-row-${c.id}`}>
              <span data-testid="claim-amount">{Number(c.amount).toFixed(2)}</span>{' '}
              <span data-testid="claim-date">{c.date}</span>{' '}
              <span data-testid="claim-note">{c.note}</span>{' '}
              <span data-testid="claim-corrects">{c.corrects ?? ''}</span>{' '}
              <span data-testid="claim-ref">{c.ref ?? ''}</span>{' '}
              <ClaimCells.Slot sheetId={sheetId} user={user} personId={personId} claim={c} />
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
  const held = useHeld(sheetId);
  return (
    <section data-testid="claims-rollup">
      <p>
        Claimed total: <strong data-testid="rollup-claimed-total">{sumAmounts(claims.data ?? [], held)}</strong>
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
