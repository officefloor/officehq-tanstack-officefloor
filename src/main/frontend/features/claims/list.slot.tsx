import { useState, type ReactNode } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser, useSheetToday } from '../session/session';
import type { Person } from '../people/people';
import { dueOf, rateOn, useRates } from '../rates/rates';
import { claimsPath, claimsQueryKey, personClaimsPath, useClaims, useEditingClaim, type Claim } from './claims';

// A person's claims, newest first, with the claim form that adds one — or, while one of their
// claims' editor is open, saves its changes.
function ClaimsHeader() {
  return <th>Claims</th>;
}

function PersonClaims({ person }: { person: Person }) {
  const [sheet] = useCurrentSheet();
  const [editing, setEditing] = useEditingClaim();
  const claims = useClaims();
  const rates = useRates();
  if (!sheet) return null;
  const mine = (claims.data ?? []).filter((c) => c.personId === person.id);
  const claim = editing ? mine.find((c) => String(c.id) === editing) : undefined;
  return (
    <td>
      <ul data-testid="claims">
        {mine.map((c) => (
          <li key={c.id} data-testid={`claim-row-${c.id}`}>
            <span data-testid="claim-number">{c.number ?? ''}</span>{' '}
            <span data-testid="claim-amount">{c.amount}</span>{' '}
            <span data-testid="claim-date">{c.date}</span>{' '}
            <span data-testid="claim-note" style={{ whiteSpace: 'pre' }}>{c.note}</span>{' '}
            <span data-testid="claim-due">{c.due ?? dueOf(c.amount, rateOn(rates.data, person.grade, c.date))}</span>{' '}
            <span data-testid="claim-opened-by">{c.openedBy ?? ''}</span>{' '}
            <ClaimStatusActions key={`${sheet}:${c.id}`} sheet={sheet} claim={c}>
              <button type="button" data-testid="claim-edit" onClick={() => setEditing(String(c.id))}>
                Edit
              </button>
              {claim?.id === c.id && (
                <button type="button" data-testid="claim-cancel" onClick={() => setEditing(undefined)}>
                  Cancel
                </button>
              )}
            </ClaimStatusActions>
          </li>
        ))}
      </ul>
      {/* Keyed by the claim being edited, so opening an editor fills the form with its values. */}
      <ClaimForm key={`${sheet}:${claim?.id ?? ''}`} sheet={sheet} person={person} claim={claim} />
    </td>
  );
}

const ACTIONS = [
  { action: 'open', label: 'Open' },
  { action: 'settle', label: 'Settle' },
  { action: 'void', label: 'Void' },
] as const;

// A claim's status and the buttons that change it. A change its status does not allow is refused
// by the server and shows the claim's error, as is settling a claim the signed-in user opened; the error goes as soon as the next action on the row
// is taken.
function ClaimStatusActions({ sheet, claim, children }: { sheet: string; claim: Claim; children: ReactNode }) {
  const [user] = useCurrentUser();
  const queryClient = useQueryClient();
  const change = useMutation({
    mutationFn: (action: string) => putJson<Claim>(`${claimsPath(sheet)}/${claim.id}/${action}`, undefined, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet) }),
  });
  const submitChange = useSubmitOnce(change);
  return (
    <>
      <span data-testid="claim-status">{claim.status}</span>{' '}
      {ACTIONS.map(({ action, label }) => (
        <button
          key={action}
          type="button"
          data-testid={`claim-${action}`}
          disabled={change.isPending}
          onClick={() => {
            submitChange(action);
          }}
        >
          {label}
        </button>
      ))}{' '}
      <span onClickCapture={() => change.reset()}>{children}</span>
      {change.isError && (
        <span data-testid="claim-error">
          {change.variables === 'settle' && claim.openedBy === user
            ? 'You opened this claim, so someone else must settle it.'
            : `That status change is not allowed for a ${claim.status} claim.`}
        </span>
      )}
    </>
  );
}

function ClaimForm({ sheet, person, claim }: { sheet: string; person: Person; claim?: Claim }) {
  const [user] = useCurrentUser();
  const [, setEditing] = useEditingClaim();
  const today = useSheetToday();
  const [amount, setAmount] = useState(claim?.amount ?? '');
  const [date, setDate] = useState(claim?.date ?? '');
  const [note, setNote] = useState(claim?.note ?? '');
  const queryClient = useQueryClient();
  const save = useMutation({
    mutationFn: (body: { amount: string; date: string; note: string }) =>
      claim
        ? putJson<Claim>(`${claimsPath(sheet)}/${claim.id}`, body, user)
        : postJson<Claim>(personClaimsPath(sheet, person.id), body, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet) });
      if (claim) {
        setEditing(undefined);
      } else {
        setAmount('');
        setDate('');
        setNote('');
      }
    },
  });
  const submit = useSubmitOnce(save);

  return (
    <form
      data-testid="claim-form"
      onSubmit={(e) => {
        e.preventDefault();
        submit({ amount, date, note });
      }}
    >
      <input
        data-testid="claim-form-amount"
        placeholder="Amount"
        inputMode="decimal"
        value={amount}
        onChange={(e) => { setAmount(e.target.value); save.reset(); }}
      />
      <input
        data-testid="claim-form-date"
        placeholder={today ?? 'YYYY-MM-DD'}
        value={date}
        onChange={(e) => { setDate(e.target.value); save.reset(); }}
      />
      <input
        data-testid="claim-form-note"
        placeholder="Note"
        value={note}
        onChange={(e) => { setNote(e.target.value); save.reset(); }}
      />
      <button type="submit" data-testid="claim-form-submit" disabled={save.isPending}>
        {claim ? 'Save claim' : 'Add claim'}
      </button>
      {save.isError && (
        <span data-testid="claim-form-error">
          The amount must be a number, 0.00 or more, with up to two decimals, and the date a calendar date.
        </span>
      )}
    </form>
  );
}

export const contributions = [
  PeopleColumns.fill({ order: 20, Component: ClaimsHeader }),
  PersonCells.fill({ order: 20, Component: PersonClaims }),
];
