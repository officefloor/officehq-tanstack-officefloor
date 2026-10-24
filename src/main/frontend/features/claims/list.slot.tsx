import { useState, type ReactNode } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser, useSheetToday } from '../session/session';
import { gradeOn, type Person } from '../people/people';
import { chargedOf, useRates } from '../rates/rates';
import { periodOn, usePeriods } from '../periods/periods';
import { DEFAULT_TERMS, useSettings } from '../roles/roles';
import { claimsPath, claimsQueryKey, dueByOf, isOverdue, personClaimsPath, useClaims, useEditingClaim, useSelectedClaims, type Claim } from './claims';

// A person's claims, newest first, with the claim form that adds one — or, while one of their
// claims' editor is open, saves its changes.
function ClaimsHeader() {
  return <th>Claims</th>;
}

function PersonClaims({ person }: { person: Person }) {
  const [sheet] = useCurrentSheet();
  const [editing, setEditing] = useEditingClaim();
  const { selected, toggle } = useSelectedClaims();
  const claims = useClaims();
  const rates = useRates();
  const periods = usePeriods();
  const settings = useSettings();
  const today = useSheetToday();
  const terms = settings.data?.terms ?? DEFAULT_TERMS;
  if (!sheet) return null;
  const mine = (claims.data ?? []).filter((c) => c.personId === person.id);
  const claim = editing ? mine.find((c) => String(c.id) === editing) : undefined;
  return (
    <td>
      <ul data-testid="claims">
        {mine.map((c) => (
          <li key={c.id} data-testid={`claim-row-${c.id}`}>
            <input
              type="checkbox"
              data-testid="claim-select"
              checked={selected.has(c.id)}
              onChange={(e) => toggle(c.id, e.target.checked)}
            />{' '}
            <span data-testid="claim-number">{c.number ?? ''}</span>{' '}
            <span data-testid="claim-amount">{c.amount}</span>{' '}
            <span data-testid="claim-date">{c.date}</span>{' '}
            <span data-testid="claim-note" style={{ whiteSpace: 'pre' }}>{c.note}</span>{' '}
            <span data-testid="claim-period">{periodOn(periods.data, c.date)?.name ?? ''}</span>{' '}
            <span data-testid="claim-due">{chargedOf(c, rates.data, gradeOn(person, c.date), settings.data, today)}</span>{' '}
            <span data-testid="claim-stated">{c.stated ? 'yes' : 'no'}</span>{' '}
            <span data-testid="claim-opened-by">{c.openedBy ?? ''}</span>{' '}
            <span data-testid="claim-due-by">{dueByOf(c, terms)}</span>{' '}
            <span data-testid="claim-corrects">{c.corrects ?? ''}</span>{' '}
            <span data-testid="claim-overdue">{isOverdue(c, terms, today) ? 'yes' : 'no'}</span>{' '}
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
  { action: 'approve', label: 'Approve' },
  { action: 'reject', label: 'Reject' },
  { action: 'void', label: 'Void' },
] as const;

// A claim's status and the buttons that change it. A change its status does not allow is refused
// by the server and shows the claim's error, as is settling or approving a claim the signed-in user opened, a
// non-steward approving or rejecting a PENDING claim, or rejecting one without a reason (typed beside the reject
// button). A clerk settling a claim whose due is above the clerk limit leaves it PENDING. The error goes as soon as
// the next action on the row is taken. Only a DRAFT can be edited: the edit button of any other claim shows the claim's error
// instead of opening its editor (an open claim is voided and added anew).
function ClaimStatusActions({ sheet, claim, onEdit, children }: {
  sheet: string; claim: Claim; onEdit: () => void; children: ReactNode;
}) {
  const [user] = useCurrentUser();
  const queryClient = useQueryClient();
  const [reason, setReason] = useState('');
  const change = useMutation({
    mutationFn: ({ action, reason }: { action: string; reason?: string }) =>
      putJson<Claim>(`${claimsPath(sheet)}/${claim.id}/${action}`
        + (reason === undefined ? '' : `?reason=${encodeURIComponent(reason)}`), undefined, user),
    onSuccess: (_, { action }) => {
      if (action === 'reject') setReason('');
      return queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet) });
    },
  });
  const submitChange = useSubmitOnce(change);
  const [editRefused, setEditRefused] = useState(false);
  const action = change.variables?.action;
  const periods = usePeriods();
  const closed = !!periodOn(periods.data, claim.date)?.closed;
  return (
    <>
      <span data-testid="claim-status">{claim.status}</span>{' '}
      <input
        data-testid="claim-reject-reason"
        placeholder="Reason to reject"
        value={reason}
        onChange={(e) => { setReason(e.target.value); change.reset(); setEditRefused(false); }}
      />{' '}
      {ACTIONS.map(({ action, label }) => (
        <button
          key={action}
          type="button"
          data-testid={`claim-${action}`}
          disabled={change.isPending}
          onClick={() => {
            setEditRefused(false);
            submitChange(action === 'reject' ? { action, reason } : { action });
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
            : (action === 'approve' || action === 'reject') && change.error instanceof HttpError && change.error.status === 403
            ? 'Only a steward can approve or reject a pending claim.'
            : action === 'settle' && claim.openedBy === user
            ? 'You opened this claim, so someone else must settle it.'
            : action === 'approve' && claim.openedBy === user && claim.status === 'PENDING'
            ? 'You opened this claim, so someone else must approve it.'
            : action === 'reject' && claim.status === 'PENDING' && reason.trim() === ''
            ? 'Give a reason to reject this claim.'
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
  const [corrects, setCorrects] = useState(claim?.corrects == null ? '' : String(claim.corrects));
  const queryClient = useQueryClient();
  const save = useMutation({
    mutationFn: (body: { amount: string; date: string; note: string; corrects: string }) =>
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
        setCorrects('');
      }
    },
  });
  const submit = useSubmitOnce(save);

  return (
    <form
      data-testid="claim-form"
      onSubmit={(e) => {
        e.preventDefault();
        submit({ amount, date, note, corrects });
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
      <input
        data-testid="claim-form-corrects"
        placeholder="Corrects claim no."
        inputMode="numeric"
        value={corrects}
        onChange={(e) => { setCorrects(e.target.value); save.reset(); }}
      />
      <button type="submit" data-testid="claim-form-submit" disabled={save.isPending}>
        {claim ? 'Save claim' : 'Add claim'}
      </button>
      {save.isError && (
        <span data-testid="claim-form-error">
          The amount must be a number with up to two decimals, and the date a calendar date not in a closed
          period nor after the person's retirement date; a negative amount is a correction and must name, by
          number, a settled claim of this person (only a correction may); only a draft claim can be edited.
        </span>
      )}
    </form>
  );
}

export const contributions = [
  PeopleColumns.fill({ order: 20, Component: ClaimsHeader }),
  PersonCells.fill({ order: 20, Component: PersonClaims }),
];
