import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson, putJson } from '../../api/http';
import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import { gradeOn, peopleKey, usePeople } from '../people/people';
import type { Person } from '../people/people';
import { useUser } from '../session/session';
import { CLAIM_AMOUNT, CLAIM_NUMBER, claimsKey, sumAmounts, useClaims } from './claims';
import type { Claim } from './claims';
import { selectClaim, useSelectedClaims } from './selection';
import { claimDue, rateOn, useRates } from '../rates/rates';
import { periodOf, usePeriods } from '../periods/periods';
import { useFloor, useLate } from '../access/access';
import { useSheetToday } from '../sheet/sheet';
import { dueBy, isOverdue } from '../overdue/overdue';

// A clerk's settle above the clerk limit leaves the claim PENDING; a steward approves it or rejects
// it with a reason (typed in the row).
type Action = 'open' | 'settle' | 'void' | 'approve' | 'reject';
const ACTIONS: { action: Action; label: string }[] = [
  { action: 'open', label: 'Open' },
  { action: 'settle', label: 'Settle' },
  { action: 'void', label: 'Void' },
  { action: 'approve', label: 'Approve' },
  { action: 'reject', label: 'Reject' },
];

type Fields = { amount: string; date: string; note: string; corrects: string };
const EMPTY: Fields = { amount: '', date: '', note: '', corrects: '' };

function ClaimsHead() {
  return (
    <>
      <th>Claimed</th>
      <th>Claims</th>
    </>
  );
}

// A person's claims: what they have claimed in all, a form that adds a claim (or edits the one
// whose editor is open), and their claims, newest first.
function PersonClaims({ sheetId, person }: { sheetId: string; person: Person }) {
  const queryClient = useQueryClient();
  const all = useClaims(sheetId);
  const claims = (all.data ?? []).filter((c) => c.personId === person.id);
  const [fields, setFields] = useState<Fields>(EMPTY);
  const [editing, setEditing] = useState<string | null>(null);
  const [error, setError] = useState('');
  // The claim whose last status change or edit was refused, and why: its error shows until its
  // next action.
  const [refused, setRefused] = useState<{ id: string; message: string } | null>(null);
  // The reason typed for rejecting each claim, by claim id.
  const [reasons, setReasons] = useState<Record<string, string>>({});
  // The person picked to move each claim to (a steward's move), by claim id.
  const [moveTo, setMoveTo] = useState<Record<string, string>>({});
  const others = (usePeople(sheetId).data ?? []).filter((p) => p.id !== person.id);
  const [user] = useUser();
  const rates = useRates(sheetId).data ?? [];
  const periods = usePeriods(sheetId).data ?? [];
  const floor = useFloor(sheetId);
  const today = useSheetToday(sheetId);
  const late = useLate(sheetId);
  const selected = useSelectedClaims(sheetId);

  const reset = () => {
    setRefused(null);
    setFields(EMPTY);
    setEditing(null);
    setError('');
  };

  const once = useOnce();
  const save = useMutation({
    mutationFn: ({ id, body }: { id: string | null; body: Fields }) => {
      const sheet = `/api/sheets/${encodeURIComponent(sheetId)}`;
      return id === null
        ? postJson<Claim>(`${sheet}/people/${encodeURIComponent(person.id)}/claims`, body, user)
        : putJson<Claim>(`${sheet}/claims/${encodeURIComponent(id)}`, body, user);
    },
    onSuccess: () => {
      reset();
      void queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (_e, { id }) => {
      // Only a draft can be edited: an edit of an open (or later) claim is refused on the claim.
      const claim = id === null ? undefined : claims.find((c) => c.id === id);
      if (claim !== undefined && claim.status !== 'DRAFT') {
        setRefused({ id: claim.id, message: 'Only a draft claim can be edited.' });
      } else {
        setError('That claim could not be saved.');
      }
    },
  });

  const move = useMutation({
    mutationFn: ({ id, action }: { id: string; action: Action }) =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheetId)}/claims/${encodeURIComponent(id)}/${action}`,
        action === 'reject' ? { reason: (reasons[id] ?? '').trim() } : {},
        user,
      ),
    onSuccess: (_c, { id, action }) => {
      if (action === 'reject') setReasons((r) => ({ ...r, [id]: '' }));
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (_e, { id }) => setRefused({ id, message: 'That status change is not allowed.' }),
  });

  // A steward moves a DRAFT, OPEN or PENDING claim to another person; it is listed as theirs, newest.
  const reassign = useMutation({
    mutationFn: ({ id, personId }: { id: string; personId: string }) =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheetId)}/claims/${encodeURIComponent(id)}/reassign`,
        { personId },
        user,
      ),
    onSuccess: (_c, { id }) => {
      setMoveTo((m) => ({ ...m, [id]: '' }));
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (_e, { id }) => setRefused({ id, message: 'That claim cannot be moved.' }),
  });

  const moveClaim = (id: string) => {
    setRefused(null);
    const personId = moveTo[id] ?? '';
    if (personId === '') {
      setRefused({ id, message: 'Pick the person to move the claim to.' });
      return;
    }
    reassign.mutate({ id, personId });
  };

  const act = (id: string, action: Action) => {
    setRefused(null);
    if (action === 'reject' && (reasons[id] ?? '').trim() === '') {
      setRefused({ id, message: 'A rejection needs a reason.' });
      return;
    }
    move.mutate({ id, action });
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setRefused(null);
    const amount = fields.amount.trim();
    if (amount === '') {
      setError('An amount is required.');
      return;
    }
    if (!CLAIM_AMOUNT.test(amount)) {
      setError('An amount is a number with up to two decimals.');
      return;
    }
    const corrects = fields.corrects.trim();
    if (amount.startsWith('-') && !CLAIM_NUMBER.test(corrects)) {
      setError('A negative amount is a correction: name the number of the settled claim it corrects.');
      return;
    }
    once((done) => save.mutate({ id: editing, body: { ...fields, amount, corrects } }, { onSettled: done }));
  };

  const change = (next: Partial<Fields>) => {
    setFields((f) => ({ ...f, ...next }));
    setError('');
    setRefused((r) => (r !== null && r.id === editing ? null : r));
  };

  const open = (c: Claim) => {
    setRefused(null);
    setEditing(c.id);
    setFields({ amount: c.amount, date: c.date, note: c.note, corrects: c.corrects });
    setError('');
  };

  return (
    <>
      <td data-testid="person-claimed">{sumAmounts(claims, () => person.held)}</td>
      <td>
        <form onSubmit={submit} data-testid="claim-form">
          <input
            data-testid="claim-form-amount"
            placeholder="Amount"
            inputMode="decimal"
            value={fields.amount}
            onChange={(e) => change({ amount: e.target.value })}
          />{' '}
          <input
            type="date"
            data-testid="claim-form-date"
            value={fields.date}
            onChange={(e) => change({ date: e.target.value })}
          />{' '}
          <input
            data-testid="claim-form-note"
            placeholder="Note"
            value={fields.note}
            onChange={(e) => change({ note: e.target.value })}
          />{' '}
          <input
            data-testid="claim-form-corrects"
            placeholder="Corrects no."
            inputMode="numeric"
            value={fields.corrects}
            onChange={(e) => change({ corrects: e.target.value })}
          />{' '}
          <button type="submit" data-testid="claim-form-submit" disabled={save.isPending}>
            {editing === null ? 'Add claim' : 'Save claim'}
          </button>
          {error && (
            <p role="alert" data-testid="claim-form-error">
              {error}
            </p>
          )}
        </form>
        {all.isError && (
          <p role="alert" data-testid="claims-error">
            The claims could not be read.
          </p>
        )}
        <ul data-testid="claim-list">
          {claims.map((c) => (
            <li key={c.id} data-testid={`claim-row-${c.id}`}>
              <input
                type="checkbox"
                data-testid="claim-select"
                aria-label="Select claim"
                checked={selected.has(c.id)}
                onChange={(e) => selectClaim(sheetId, c.id, e.target.checked)}
              />{' '}
              <span data-testid="claim-number">{c.number}</span>{' '}
              <span data-testid="claim-amount">{c.amount}</span>{' '}
              <span data-testid="claim-date">{c.date}</span>{' '}
              <span data-testid="claim-note">{c.note}</span>{' '}
              <span data-testid="claim-due">{claimDue(c, rateOn(rates, gradeOn(person.grades, c.date), c.date), floor, late)}</span>{' '}
              <span data-testid="claim-status">{c.status}</span>{' '}
              <span data-testid="claim-corrects">{c.corrects}</span>{' '}
              <span data-testid="claim-opened-by">{c.openedBy}</span>{' '}
              <span data-testid="claim-stated">{c.stated ? 'yes' : 'no'}</span>{' '}
              <span data-testid="claim-period">{periodOf(periods, c.date)?.name ?? ''}</span>{' '}
              <span data-testid="claim-due-by">{dueBy(c)}</span>{' '}
              <span data-testid="claim-overdue">{isOverdue(c, today) ? 'yes' : 'no'}</span>{' '}
              <input
                data-testid="claim-reject-reason"
                placeholder="Reject reason"
                value={reasons[c.id] ?? ''}
                onChange={(e) => {
                  setReasons((r) => ({ ...r, [c.id]: e.target.value }));
                  setRefused((r) => (r !== null && r.id === c.id ? null : r));
                }}
              />{' '}
              {ACTIONS.map(({ action, label }) => (
                <button
                  key={action}
                  type="button"
                  data-testid={`claim-${action}`}
                  onClick={() => act(c.id, action)}
                >
                  {label}
                </button>
              ))}{' '}
              <select
                data-testid="claim-reassign-to"
                aria-label="Move to"
                value={moveTo[c.id] ?? ''}
                onChange={(e) => {
                  setMoveTo((m) => ({ ...m, [c.id]: e.target.value }));
                  setRefused((r) => (r !== null && r.id === c.id ? null : r));
                }}
              >
                <option value="">Move to…</option>
                {others.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name}
                  </option>
                ))}
              </select>{' '}
              <button type="button" data-testid="claim-reassign" onClick={() => moveClaim(c.id)}>
                Move
              </button>{' '}
              {editing === c.id ? (
                <button type="button" data-testid="claim-cancel" onClick={reset}>
                  Cancel
                </button>
              ) : (
                <button type="button" data-testid="claim-edit" onClick={() => open(c)}>
                  Edit
                </button>
              )}
              <ol data-testid="claim-log">
                {c.log.map((line, i) => (
                  <li key={i} data-testid="claim-log-line">
                    {line}
                  </li>
                ))}
              </ol>
              {refused?.id === c.id && (
                <p role="alert" data-testid="claim-error">
                  {refused.message}
                </p>
              )}
            </li>
          ))}
        </ul>
      </td>
    </>
  );
}

export const contributions = [
  PersonHead.fill({ order: 10, Component: ClaimsHead }),
  PersonRow.fill({ order: 10, Component: PersonClaims }),
];
