import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { PersonColumnHeaders, PersonColumns } from '../../slots/defs/personRow';
import { gradeOn, peoplePath, usePeople } from '../people/people';
import type { Grade, Person } from '../people/people';
import { useSheetToday } from '../sheet/sheet';
import { rateOf, showDue, twoDecimals, useRates } from '../rates/rates';
import type { Rate } from '../rates/rates';
import { periodOf, usePeriods } from '../periods/periods';
import { useFloorCents, useTerms } from '../settings/settings';
import {
  claimDueCents,
  claimed,
  claimsKey,
  claimsPath,
  counted,
  dueBy,
  overdue,
  overdueCents,
  personDueCents,
  useClaims,
  useLate,
} from './claims';
import type { Claim, ClaimStatusChange } from './claims';
import { selectClaim, useSelectedClaims } from './selection';

type Draft = { amount: string; date: string; note: string; corrects: string };
const EMPTY: Draft = { amount: '', date: '', note: '', corrects: '' };

function ClaimedHeader() {
  return (
    <>
      <th>Claimed</th>
      <th>Due total</th>
      <th>Overdue</th>
      <th>Overdue total</th>
      <th>Claims</th>
    </>
  );
}

const CHANGES: { change: ClaimStatusChange; label: string }[] = [
  { change: 'open', label: 'Open' },
  { change: 'settle', label: 'Settle' },
  { change: 'void', label: 'Void' },
  { change: 'approve', label: 'Approve' },
];

// One claim with its status and the buttons that change it. Every button is offered; the server
// refuses a change the claim's status does not allow, and the row shows that error until its next action.
function ClaimRow({ sheet, user, claim, rates, grade, editing, onEdit, onCancel }: {
  sheet: string;
  user: string;
  claim: Claim;
  rates: Rate[];
  grade: Grade | undefined;
  editing: boolean;
  onEdit: () => void;
  onCancel: () => void;
}) {
  const queryClient = useQueryClient();
  const { data: periods = [] } = usePeriods(sheet);
  const floor = useFloorCents(sheet);
  const terms = useTerms(sheet);
  const today = useSheetToday(sheet);
  const late = useLate(sheet);
  const status = useMutation({
    mutationFn: (change: ClaimStatusChange) =>
      postJson<Claim[]>(byUser(`${claimsPath(sheet)}/${claim.id}/${change}`, user), change === 'reject' ? { reason } : {}),
    onSuccess: (updated) => queryClient.setQueryData(claimsKey(sheet), updated),
  });

  const act = useSubmitOnce(status);
  // A steward moves a DRAFT, OPEN or PENDING claim to another person on the sheet, picked beside the Reassign button.
  const { data: people = [] } = usePeople(sheet);
  const [moveTo, setMoveTo] = useState('');
  const move = useMutation({
    mutationFn: (to: number) =>
      postJson<Claim[]>(byUser(`${claimsPath(sheet)}/${claim.id}/reassign`, user), { to }),
    onSuccess: (updated) => {
      setMoveTo('');
      queryClient.setQueryData(claimsKey(sheet), updated);
    },
  });
  const reassign = useSubmitOnce(move);
  const selected = useSelectedClaims(sheet).has(claim.id);
  // Only a draft can be edited: Edit on any other claim shows the row's error instead of the editor.
  const [editRefused, setEditRefused] = useState(false);
  // A steward rejects a PENDING claim with a reason, typed beside the Reject button.
  const [reason, setReason] = useState('');

  return (
    <li data-testid={`claim-row-${claim.id}`}>
      <input
        type="checkbox"
        data-testid="claim-select"
        checked={selected}
        onChange={(e) => selectClaim(sheet, claim.id, e.target.checked)}
      />{' '}
      <span data-testid="claim-amount">{claim.amount}</span>{' '}
      <span data-testid="claim-date">{claim.date}</span>{' '}
      <span data-testid="claim-note">{claim.note}</span>{' '}
      due <span data-testid="claim-due">{showDue(claimDueCents(claim, rateOf(rates, grade, claim.date), floor, late))}</span>{' '}
      due by <span data-testid="claim-due-by">{dueBy(claim, terms)}</span>{' '}
      overdue <span data-testid="claim-overdue">{overdue(claim, terms, today) ? 'yes' : 'no'}</span>{' '}
      <span data-testid="claim-status">{claim.status}</span>{' '}
      stated <span data-testid="claim-stated">{claim.stated ? 'yes' : 'no'}</span>{' '}
      period <span data-testid="claim-period">{periodOf(periods, claim.date)?.name ?? ''}</span>{' '}
      {(claim.periodNumber ?? claim.number) !== null && '#'}
      <span data-testid="claim-number">{claim.periodNumber ?? claim.number ?? ''}</span>{' '}
      {claim.openedBy !== null && 'opened by '}
      <span data-testid="claim-opened-by">{claim.openedBy ?? ''}</span>{' '}
      {claim.corrects !== null && 'corrects #'}
      <span data-testid="claim-corrects">{claim.corrects ?? ''}</span>{' '}
      {CHANGES.map(({ change, label }) => (
        <button
          key={change}
          type="button"
          data-testid={`claim-${change}`}
          disabled={status.isPending}
          onClick={() => {
            setEditRefused(false);
            move.reset();
            act(change);
          }}
        >
          {label}
        </button>
      ))}{' '}
      <input
        data-testid="claim-reject-reason"
        placeholder="reject reason"
        value={reason}
        onChange={(e) => {
          setReason(e.target.value);
          status.reset();
          move.reset();
        }}
      />{' '}
      <button
        type="button"
        data-testid="claim-reject"
        disabled={status.isPending}
        onClick={() => {
          setEditRefused(false);
          move.reset();
          act('reject');
        }}
      >
        Reject
      </button>{' '}
      <button
        type="button"
        data-testid="claim-edit"
        onClick={() => {
          status.reset();
          move.reset();
          if (claim.status !== 'DRAFT') {
            setEditRefused(true);
            return;
          }
          setEditRefused(false);
          onEdit();
        }}
      >
        Edit
      </button>
      <select
        data-testid="claim-reassign-to"
        value={moveTo}
        onChange={(e) => {
          setMoveTo(e.target.value);
          move.reset();
        }}
      >
        <option value="">move to…</option>
        {people
          .filter((p) => p.id !== claim.person)
          .map((p) => (
            <option key={p.id} value={String(p.id)}>
              {p.name}
            </option>
          ))}
      </select>{' '}
      <button
        type="button"
        data-testid="claim-reassign"
        disabled={move.isPending || moveTo === ''}
        onClick={() => {
          setEditRefused(false);
          status.reset();
          reassign(Number(moveTo));
        }}
      >
        Reassign
      </button>{' '}
      {editing && (
        <button type="button" data-testid="claim-cancel" onClick={onCancel}>
          Cancel
        </button>
      )}
      {editRefused && (
        <span data-testid="claim-error" role="alert">
          {' '}A {claim.status} claim cannot be edited; only a DRAFT can (void it and add a new one).
        </span>
      )}
      {status.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}A {claim.status} claim cannot be changed that way (the user who opened it cannot settle or approve it; only a steward approves or rejects a PENDING claim, and a reject needs a reason).
        </span>
      )}
      {move.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}A {claim.status} claim cannot be reassigned (only a steward moves a claim, and only a DRAFT, OPEN or PENDING one, to another person).
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
  const floor = useFloorCents(sheet);
  const today = useSheetToday(sheet);
  const late = useLate(sheet);
  const lateClaims = overdueCents(claims, [person], rates, late, floor);
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
  const send = useSubmitOnce(save);

  const change = (next: Partial<Draft>) => {
    setDraft((d) => ({ ...d, ...next }));
    save.reset();
  };

  const open = (c: Claim) => {
    setEditing(c.id);
    setDraft({ amount: c.amount, date: c.date, note: c.note, corrects: c.corrects === null ? '' : String(c.corrects) });
    save.reset();
  };

  const cancel = () => {
    setEditing(null);
    setDraft(EMPTY);
    save.reset();
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    // The server refuses an amount that is not a number with up to two decimals, a bad date, or a negative amount
    // (a correction) that does not name a SETTLED claim of this person by number.
    send({ id: editing, body: draft });
  };

  return (
    <>
      <td data-testid="person-claimed">{claimed(counted(claims, [person]))}</td>
      <td data-testid="person-due-total">{showDue(personDueCents(claims, rates, person, today, floor, late))}</td>
      <td data-testid="person-overdue-count">{lateClaims.count}</td>
      <td data-testid="person-overdue-total">{twoDecimals(lateClaims.cents)}</td>
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
          <input
            data-testid="claim-form-corrects"
            placeholder="corrects #"
            value={draft.corrects}
            onChange={(e) => change({ corrects: e.target.value })}
          />{' '}
          <button type="submit" data-testid="claim-form-submit" disabled={save.isPending}>
            {editing === null ? 'Add claim' : 'Save claim'}
          </button>
          {save.isError && (
            <span data-testid="claim-form-error" role="alert">
              {' '}A claim needs an amount with up to two decimals, and a valid date not after the person's retirement date; a negative amount is a correction and must name, by number, a SETTLED claim of this person; only a DRAFT claim can be edited.
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
              grade={gradeOn(person, c.date)}
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
