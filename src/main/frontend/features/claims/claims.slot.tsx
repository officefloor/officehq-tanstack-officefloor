import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson } from '../../api/http';
import { byUser } from '../session/session';
import { PersonColumnHeaders, PersonColumns } from '../../slots/defs/personRow';
import { peoplePath } from '../people/people';
import type { Grade, Person } from '../people/people';
import { useSheetToday } from '../sheet/sheet';
import { dueCents, rateOf, showDue, useRates } from '../rates/rates';
import type { Rate } from '../rates/rates';
import { claimDueCents, claimed, claimsKey, claimsPath, counted, useClaims } from './claims';
import type { Claim, ClaimStatusChange } from './claims';

type Draft = { amount: string; date: string; note: string };
const EMPTY: Draft = { amount: '', date: '', note: '' };

function ClaimedHeader() {
  return (
    <>
      <th>Claimed</th>
      <th>Due total</th>
      <th>Claims</th>
    </>
  );
}

const CHANGES: { change: ClaimStatusChange; label: string }[] = [
  { change: 'open', label: 'Open' },
  { change: 'settle', label: 'Settle' },
  { change: 'void', label: 'Void' },
];

// One claim with its status and the buttons that change it. Every button is offered; the server
// refuses a change the claim's status does not allow, and the row shows that error until its next action.
function ClaimRow({ sheet, user, claim, rates, grade, editing, onEdit, onCancel }: {
  sheet: string;
  user: string;
  claim: Claim;
  rates: Rate[];
  grade: Grade;
  editing: boolean;
  onEdit: () => void;
  onCancel: () => void;
}) {
  const queryClient = useQueryClient();
  const status = useMutation({
    mutationFn: (change: ClaimStatusChange) =>
      postJson<Claim[]>(byUser(`${claimsPath(sheet)}/${claim.id}/${change}`, user), {}),
    onSuccess: (updated) => queryClient.setQueryData(claimsKey(sheet), updated),
  });

  const act = (change: ClaimStatusChange) => {
    status.reset();
    status.mutate(change);
  };

  return (
    <li data-testid={`claim-row-${claim.id}`}>
      <span data-testid="claim-amount">{claim.amount}</span>{' '}
      <span data-testid="claim-date">{claim.date}</span>{' '}
      <span data-testid="claim-note">{claim.note}</span>{' '}
      due <span data-testid="claim-due">{showDue(claimDueCents(claim, rateOf(rates, grade, claim.date)))}</span>{' '}
      <span data-testid="claim-status">{claim.status}</span>{' '}
      {claim.number !== null && '#'}
      <span data-testid="claim-number">{claim.number ?? ''}</span>{' '}
      {CHANGES.map(({ change, label }) => (
        <button
          key={change}
          type="button"
          data-testid={`claim-${change}`}
          disabled={status.isPending}
          onClick={() => act(change)}
        >
          {label}
        </button>
      ))}{' '}
      <button
        type="button"
        data-testid="claim-edit"
        onClick={() => {
          status.reset();
          onEdit();
        }}
      >
        Edit
      </button>
      {editing && (
        <button type="button" data-testid="claim-cancel" onClick={onCancel}>
          Cancel
        </button>
      )}
      {status.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}A {claim.status} claim cannot be changed that way.
        </span>
      )}
    </li>
  );
}

// A person's claimed total, then their claims (newest first) under one form that adds a claim or,
// once a claim's editor is opened, edits it. A blank date takes the sheet's today on the server.
function PersonClaims({ sheet, user, person }: { sheet: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const { data: all = [] } = useClaims(sheet);
  const claims = all.filter((c) => c.person === person.id);
  const { data: rates = [] } = useRates(sheet);
  const today = useSheetToday(sheet);
  const rate = rateOf(rates, person.grade, today);
  const [draft, setDraft] = useState<Draft>(EMPTY);
  const [editing, setEditing] = useState<number | null>(null);

  const save = useMutation({
    mutationFn: ({ id, body }: { id: number | null; body: Draft }) =>
      id === null
        ? postJson<Claim[]>(byUser(`${peoplePath(sheet)}/${person.id}/claims`, user), body)
        : putJson<Claim[]>(byUser(`${claimsPath(sheet)}/${id}`, user), body),
    onSuccess: (updated) => {
      queryClient.setQueryData(claimsKey(sheet), updated);
      setDraft(EMPTY);
      setEditing(null);
    },
  });

  const change = (next: Partial<Draft>) => {
    setDraft((d) => ({ ...d, ...next }));
    save.reset();
  };

  const open = (c: Claim) => {
    setEditing(c.id);
    setDraft({ amount: c.amount, date: c.date, note: c.note });
    save.reset();
  };

  const cancel = () => {
    setEditing(null);
    setDraft(EMPTY);
    save.reset();
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    save.reset();
    // The server refuses an amount that is not 0.00 or more with up to two decimals, or a bad date.
    save.mutate({ id: editing, body: draft });
  };

  return (
    <>
      <td data-testid="person-claimed">{claimed(counted(claims))}</td>
      <td data-testid="person-due-total">{showDue(dueCents(claimed(counted(claims)), rate))}</td>
      <td>
        <form onSubmit={submit}>
          <input
            data-testid="claim-form-amount"
            placeholder="amount"
            value={draft.amount}
            onChange={(e) => change({ amount: e.target.value })}
          />{' '}
          <input
            data-testid="claim-form-date"
            placeholder="yyyy-mm-dd"
            value={draft.date}
            onChange={(e) => change({ date: e.target.value })}
          />{' '}
          <input
            data-testid="claim-form-note"
            placeholder="note"
            value={draft.note}
            onChange={(e) => change({ note: e.target.value })}
          />{' '}
          <button type="submit" data-testid="claim-form-submit" disabled={save.isPending}>
            {editing === null ? 'Add claim' : 'Save claim'}
          </button>
          {save.isError && (
            <span data-testid="claim-form-error" role="alert">
              {' '}A claim needs an amount of 0.00 or more with up to two decimals, and a valid date.
            </span>
          )}
        </form>
        <ul>
          {claims.map((c) => (
            <ClaimRow
              key={c.id}
              sheet={sheet}
              user={user}
              claim={c}
              rates={rates}
              grade={person.grade}
              editing={editing === c.id}
              onEdit={() => open(c)}
              onCancel={cancel}
            />
          ))}
        </ul>
      </td>
    </>
  );
}

export const contributions = [
  PersonColumnHeaders.fill({ order: 10, Component: ClaimedHeader }),
  PersonColumns.fill({ order: 10, Component: PersonClaims }),
];
