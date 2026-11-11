import { type ChangeEvent, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson, putJson } from '../../api/http';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import type { Person } from '../people/people';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { type Claim, claimsKey, useClaims } from './claims';

type Fields = { amount: string; date: string; note: string };
const EMPTY: Fields = { amount: '', date: '', note: '' };

function ClaimsHead() {
  return <th>Claims</th>;
}

// A person's claims, newest first, with the form that adds one or edits the one whose editor is open.
function PersonClaims({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const claims = useClaims(sheet);
  const [fields, setFields] = useState<Fields>(EMPTY);
  const [editing, setEditing] = useState<number | undefined>();
  const queryClient = useQueryClient();
  const base = `/api/sheets/${encodeURIComponent(sheet)}`;

  const save = useMutation({
    mutationFn: (f: Fields) =>
      editing !== undefined
        ? putJson<Claim>(`${base}/claims/${editing}`, { ...f, user })
        : postJson<Claim>(`${base}/people/${person.id}/claims`, { ...f, user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
      setFields(EMPTY);
      setEditing(undefined);
    },
  });

  const set = (key: keyof Fields) => (e: ChangeEvent<HTMLInputElement>) => {
    setFields({ ...fields, [key]: e.target.value });
    save.reset();
  };
  const open = (c: Claim) => {
    save.reset();
    setEditing(c.id);
    setFields({ amount: c.amount, date: c.date, note: c.note });
  };
  const cancel = () => {
    save.reset();
    setEditing(undefined);
    setFields(EMPTY);
  };

  return (
    <td>
      <ul>
        {(claims.data ?? [])
          .filter((c) => c.personId === person.id)
          .map((c) => (
            <li key={c.id} data-testid={`claim-row-${c.id}`}>
              <span data-testid="claim-amount">{c.amount}</span>{' '}
              <span data-testid="claim-date">{c.date}</span>{' '}
              <span data-testid="claim-note">{c.note}</span>{' '}
              <button type="button" data-testid="claim-edit" onClick={() => open(c)}>
                Edit
              </button>
              {editing === c.id && (
                <button type="button" data-testid="claim-cancel" onClick={cancel}>
                  Cancel
                </button>
              )}
            </li>
          ))}
      </ul>
      <form
        data-testid="claim-form"
        onSubmit={(e) => {
          e.preventDefault();
          save.reset();
          save.mutate(fields);
        }}
      >
        <input data-testid="claim-form-amount" placeholder="Amount" inputMode="decimal" value={fields.amount} onChange={set('amount')} />
        <input data-testid="claim-form-date" placeholder="Date (yyyy-mm-dd)" value={fields.date} onChange={set('date')} />
        <input data-testid="claim-form-note" placeholder="Note" value={fields.note} onChange={set('note')} />
        <button type="submit" data-testid="claim-form-submit" disabled={save.isPending}>
          {editing !== undefined ? 'Save claim' : 'Add claim'}
        </button>
        {save.isError && (
          <span data-testid="claim-form-error" role="alert">
            {' '}
            {save.error instanceof HttpError && save.error.reason ? `${save.error.reason}.` : 'The claim was refused.'}
          </span>
        )}
      </form>
    </td>
  );
}

export const contributions = [
  PersonColumnHeads.fill({ order: 20, Component: ClaimsHead }),
  PersonColumns.fill({ order: 20, Component: PersonClaims }),
];
