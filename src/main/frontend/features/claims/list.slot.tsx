import { useState, type ReactNode } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser, useSheetToday } from '../session/session';
import type { Person } from '../people/people';
import { figureOf, useRates } from '../rates/rates';
import { periodOn, usePeriods } from '../periods/periods';
import { useSettings } from '../roles/roles';
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
  const periods = usePeriods();
  const settings = useSettings();
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
            <span data-testid="claim-period">{periodOn(periods.data, c.date)?.name ?? ''}</span>{' '}
            <span data-testid="claim-due">{figureOf(c, rates.data, person.grade, settings.data?.floor)}</span>{' '}
            <span data-testid="claim-stated">{c.stated ? 'yes' : 'no'}</span>{' '}
            <span data-testid="claim-opened-by">{c.openedBy ?? ''}</span>{' '}
            <ClaimStatusActions key={`${sheet}:${c.id}`} sheet={sheet} claim={c} onEdit={() => setEditing(String(c.id))}>
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
// by the server and shows the claim's error, as is settling a claim the signed-in user opened, or a clerk settling one whose due is above the clerk limit; the error goes as soon as the next action on the row
// is taken. Only a DRAFT can be edited: the edit button of any other claim shows the claim's error
// instead of opening its editor (an open claim is voided and added anew).
function ClaimStatusActions({ sheet, claim, onEdit, children }: {
  sheet: string; claim: Claim; onEdit: () => void; children: ReactNode;
}) {
  const [user] = useCurrentUser();
  const queryClient = useQueryClient();
  const change = useMutation({
    mutationFn: (action: string) => putJson<Claim>(`${claimsPath(sheet)}/${claim.id}/${action}`, undefined, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet) }),
  });
  const submitChange = useSubmitOnce(change);
  const [editRefused, setEditRefused] = useState(false);
  const periods = usePeriods();
  const closed = !!periodOn(periods.data, claim.date)?.closed;
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
            setEditRefused(false);
            submitChange(action);
          }}
        >
          {label}
        </button>
      ))}{' '}
      <span onClickCapture={() => { change.reset(); setEditRefused(false); }}>
        <button
          type="button"
          data-testid="claim-edit"
          onClick={() => {
            if (claim.status === 'DRAFT') onEdit();
            else setEditRefused(true);
          }}
        >
          Edit
        </button>
        {children}
      </span>
      {(change.isError || editRefused) && (
        <span data-testid="claim-error">
          {editRefused
            ? `Only a draft claim can be edited; void this ${claim.status} claim and add a new one.`
            : closed
            ? 'This claim is dated in a closed period, so its status cannot change.'
            : change.variables === 'settle' && claim.openedBy === user
            ? 'You opened this claim, so someone else must settle it.'
            : change.variables === 'settle' && change.error instanceof HttpError && change.error.status === 403
            ? 'Its due is above the clerk limit, so only a steward can settle it.'
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
          The amount must be a number, 0.00 or more, with up to two decimals, and the date a calendar date
          not in a closed period nor after the person's retirement date; only a draft claim can be edited.
        </span>
      )}
    </form>
  );
}

export const contributions = [
  PeopleColumns.fill({ order: 20, Component: ClaimsHeader }),
  PersonCells.fill({ order: 20, Component: PersonClaims }),
];
