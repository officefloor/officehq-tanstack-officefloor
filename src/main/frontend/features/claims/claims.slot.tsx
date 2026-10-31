import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson } from '../../api/http';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import type { Person } from '../people/usePeople';
import { AMOUNT, claimed, claimsKey, useClaims, type Claim } from './useClaims';

/**
 * A person's claims: what they have claimed in all, one form to add a claim or edit the one whose
 * editor is open, and the claims, newest first.
 */
function PersonClaims({ user, sheet, person }: { user: string; sheet: string; person: Person }) {
  const queryClient = useQueryClient();
  const { data: all = [] } = useClaims(sheet);
  const claims = all.filter((c) => c.personId === person.id);
  const [editing, setEditing] = useState<number | null>(null);
  const [amount, setAmount] = useState('');
  const [date, setDate] = useState('');
  const [note, setNote] = useState('');
  const [invalid, setInvalid] = useState(false);
  const base = `/api/sheets/${encodeURIComponent(sheet)}`;

  const save = useMutation({
    mutationFn: (c: { id: number | null; amount: string; date: string; note: string }) => {
      const body = { amount: c.amount, date: c.date, note: c.note, user };
      return c.id === null
        ? postJson<Claim>(`${base}/people/${person.id}/claims`, body)
        : putJson<Claim>(`${base}/claims/${c.id}`, body);
    },
    onSuccess: async () => {
      close();
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    },
  });

  // An error goes as soon as its form is changed or submitted again, or an editor is opened.
  const clearError = () => {
    save.reset();
    setInvalid(false);
  };
  const close = () => {
    setEditing(null);
    setAmount('');
    setDate('');
    setNote('');
  };
  const open = (c: Claim) => {
    clearError();
    setEditing(c.id);
    setAmount(c.amount);
    setDate(c.date);
    setNote(c.note);
  };
  const cancel = () => {
    clearError();
    close();
  };

  return (
    <>
      <td data-testid="person-claimed">{claimed(claims)}</td>
      <td>
        <form
          data-testid="claim-form"
          onSubmit={(e) => {
            e.preventDefault();
            if (!AMOUNT.test(amount.trim())) {
              // Refused here, before anything is sent: the store is untouched.
              save.reset();
              setInvalid(true);
              return;
            }
            setInvalid(false);
            save.mutate({ id: editing, amount: amount.trim(), date: date.trim(), note });
          }}
        >
          <input
            data-testid="claim-form-amount"
            aria-label="Amount"
            placeholder="0.00"
            value={amount}
            onChange={(e) => {
              setAmount(e.target.value);
              clearError();
            }}
          />{' '}
          <input
            data-testid="claim-form-date"
            aria-label="Date"
            placeholder="yyyy-mm-dd"
            value={date}
            onChange={(e) => {
              setDate(e.target.value);
              clearError();
            }}
          />{' '}
          <input
            data-testid="claim-form-note"
            aria-label="Note"
            placeholder="note"
            value={note}
            onChange={(e) => {
              setNote(e.target.value);
              clearError();
            }}
          />{' '}
          <button type="submit" data-testid="claim-form-submit" disabled={save.isPending}>
            {editing === null ? 'Add claim' : 'Save claim'}
          </button>
          {(invalid || save.isError) && (
            <span data-testid="claim-form-error" role="alert">
              {' '}
              {invalid
                ? 'The amount must be 0.00 or more, with up to two decimals.'
                : 'Could not save this claim.'}
            </span>
          )}
        </form>
        <ul data-testid="claim-list">
          {claims.map((c) => (
            <li key={c.id} data-testid={`claim-row-${c.id}`}>
              <span data-testid="claim-amount">{c.amount}</span>{' '}
              <span data-testid="claim-date">{c.date}</span>{' '}
              <span data-testid="claim-note">{c.note}</span>{' '}
              <button type="button" data-testid="claim-edit" onClick={() => open(c)}>
                Edit
              </button>
              {editing === c.id && (
                <>
                  {' '}
                  <button type="button" data-testid="claim-cancel" onClick={cancel}>
                    Cancel
                  </button>
                </>
              )}
            </li>
          ))}
        </ul>
      </td>
    </>
  );
}

export const contributions = [
  PersonColumns.fill({
    order: 10,
    Component: () => (
      <>
        <th>Claimed</th>
        <th>Claims</th>
      </>
    ),
  }),
  PersonCells.fill({ order: 10, Component: PersonClaims }),
];
