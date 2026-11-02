import { useState, type ChangeEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PERSON_ROW_SPAN, PersonCells, PersonColumnHead, PersonDetail } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheetBody';
import { usePeople } from '../people/usePeople';
import { due, rateFor, useRates } from '../rates/useRates';
import { claimed, claimsKey, useClaims, type Claim } from './useClaims';

/** A person's claimed: the sum of their counted (OPEN or SETTLED) claims' amounts. */
function PersonClaimed({ sheetId, personId }: { sheetId: string; personId: number }) {
  const claims = useClaims(sheetId);
  return <td data-testid="person-claimed">{claimed((claims.data ?? []).filter((c) => c.personId === personId))}</td>;
}

type ClaimAction = 'open' | 'settle' | 'void';

/** A claim's status changes; the server refuses one its status does not allow, and a settle by
 *  the user who opened the claim. */
const ACTIONS: [ClaimAction, string][] = [
  ['open', 'Open'],
  ['settle', 'Settle'],
  ['void', 'Void'],
];

/** A person's claims, newest first, under their row, with a form that adds a claim, or edits one
 *  when its editor is open. A blank date takes the sheet's today. */
function PersonClaims({ sheetId, personId }: { sheetId: string; personId: number }) {
  const claims = useClaims(sheetId);
  const people = usePeople(sheetId);
  const rates = useRates(sheetId);
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<number | null>(null);
  const [amount, setAmount] = useState('');
  const [date, setDate] = useState('');
  const [note, setNote] = useState('');

  const clearForm = () => {
    setEditing(null);
    setAmount('');
    setDate('');
    setNote('');
  };

  const save = useMutation({
    mutationFn: (f: { id: number | null; amount: string; date: string; note: string }) => {
      const sheet = `/api/sheets/${encodeURIComponent(sheetId)}`;
      const path = f.id === null ? `${sheet}/people/${personId}/claims` : `${sheet}/claims/${f.id}`;
      return postJson<Claim>(path, { amount: f.amount, date: f.date, note: f.note });
    },
    onSuccess: async () => {
      clearForm();
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });

  // A status change (open, settle, void) of one claim; a refusal shows on that claim's row.
  const action = useMutation({
    mutationFn: (a: { id: number; action: ClaimAction }) =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${a.id}/${a.action}`, {}),
    onSettled: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });

  const act = (id: number, a: ClaimAction) => {
    action.reset();
    action.mutate({ id, action: a });
  };

  const openEditor = (c: Claim) => {
    save.reset();
    action.reset();
    setEditing(c.id);
    setAmount(c.amount);
    setDate(c.date);
    setNote(c.note);
  };

  const cancel = () => {
    save.reset();
    clearForm();
  };

  const field = (set: (v: string) => void) => (e: ChangeEvent<HTMLInputElement>) => {
    set(e.target.value);
    save.reset();
  };

  const mine = (claims.data ?? []).filter((c) => c.personId === personId);
  // A claim's due: the one fixed when it was settled, else its amount at the rate for its person's
  // grade in force on its date ('n/a' when none applies).
  const grade = people.data?.find((p) => p.id === personId)?.grade;

  return (
    <tr>
      <td colSpan={PERSON_ROW_SPAN}>
        <form
          data-testid="claim-form"
          onSubmit={(e) => {
            e.preventDefault();
            save.reset();
            save.mutate({ id: editing, amount, date, note });
          }}
        >
          <label>
            Amount <input data-testid="claim-form-amount" inputMode="decimal" value={amount} onChange={field(setAmount)} />
          </label>{' '}
          <label>
            Date{' '}
            <input
              type="text"
              placeholder="yyyy-mm-dd"
              data-testid="claim-form-date"
              value={date}
              onChange={field(setDate)}
            />
          </label>{' '}
          <label>
            Note <input data-testid="claim-form-note" value={note} onChange={field(setNote)} />
          </label>{' '}
          <button type="submit" data-testid="claim-form-submit" disabled={save.isPending}>
            {editing === null ? 'Add claim' : 'Save claim'}
          </button>
          {save.isError && <p data-testid="claim-form-error">{save.error.message}</p>}
        </form>
        <table data-testid="claims-list">
          <tbody>
            {mine.map((c) => (
              <tr key={c.id} data-testid={`claim-row-${c.id}`}>
                <td data-testid="claim-number">{c.number ?? ''}</td>
                <td data-testid="claim-amount">{c.amount}</td>
                <td data-testid="claim-date">{c.date}</td>
                <td data-testid="claim-note">{c.note}</td>
                <td data-testid="claim-due">{c.due ?? due(c.amount, rateFor(rates.data, grade, c.date))}</td>
                <td data-testid="claim-status">{c.status}</td>
                <td data-testid="claim-opened-by">{c.openedBy ?? ''}</td>
                <td>
                  {ACTIONS.map(([a, label]) => (
                    <button
                      key={a}
                      type="button"
                      data-testid={`claim-${a}`}
                      disabled={action.isPending}
                      onClick={() => act(c.id, a)}
                    >
                      {label}
                    </button>
                  ))}
                  {action.isError && action.variables?.id === c.id && (
                    <span data-testid="claim-error">{action.error.message}</span>
                  )}
                </td>
                <td>
                  {editing === c.id ? (
                    <button type="button" data-testid="claim-cancel" onClick={cancel}>
                      Cancel
                    </button>
                  ) : (
                    <button type="button" data-testid="claim-edit" onClick={() => openEditor(c)}>
                      Edit
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {claims.isError && <p data-testid="claims-error">{claims.error.message}</p>}
      </td>
    </tr>
  );
}

/** The sheet's claimed-total: everyone's claimed added up. */
function ClaimedTotal({ sheetId }: { sheetId: string }) {
  const claims = useClaims(sheetId);
  return (
    <p>
      Claimed total: <strong data-testid="rollup-claimed-total">{claimed(claims.data ?? [])}</strong>
    </p>
  );
}

export const contributions = [
  PersonColumnHead.fill({ order: 10, Component: () => <th>Claimed</th> }),
  PersonCells.fill({ order: 10, Component: PersonClaimed }),
  PersonDetail.fill({ order: 10, Component: PersonClaims }),
  SheetBody.fill({ order: 20, Component: ({ sheetId }) => <ClaimedTotal sheetId={sheetId} /> }),
];
