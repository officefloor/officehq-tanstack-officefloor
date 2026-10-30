import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { claimDue, showDue } from '../rates/due';
import { claimsKey, isDeferred, type Claim } from './useClaims';
import { periodOf, usePeriods } from '../periods/usePeriods';
import { useFloor, useLatePct, usePolicyDate } from '../settings/useSettings';
import { useSheetToday } from '../sheet/useSheetToday';
import { dueBy, isOverdue } from './overdue';
import { ClaimSelect } from './BulkOpen';
import { peopleKey, type Person } from '../people/usePeople';

// A person's claims, newest first, each with its amount, date, note, status, number, opened-by, period (the one whose dates contain its date, blank for none), stated (yes when the
// person's last statement bound it), corrects (the number a correction corrects, blank for other claims) and due (fixed when it was settled, else bound by the last statement; otherwise its amount at the person's grade's rate in force on the claim's date raised to the floor in force on the claim's date, n/a when none applies; an overdue claim gains the late percent in force on the claim's date of that figure for every full 30 days overdue), due-by (fixed when it was created: its date plus the terms of its person's grade in force on that date — the grade's own, else the sheet's, each as in force on that date), overdue (yes when it is OPEN or PENDING and its due-by is before the sheet's today), the status buttons (open, settle, void —
// one the claim's status, or the sheet's permission for the user's role, does not allow is refused with the claim's error; a clerk settling above the clerk limit sends the
// claim to PENDING, which shows its live due unbound by any statement), approve and reject with a reason (a steward's decision
// on a PENDING claim: approve settles it, reject returns it to OPEN; the opener cannot approve it) and an edit button that
// opens a DRAFT claim in the person's claim form (cancel closes it again); editing any other claim is
// refused with the claim's error — to change an open claim, void it and add a new one. A steward (or the roles the sheet's
// reassign permission allows) moves a DRAFT, OPEN or
// PENDING claim to another person on the sheet with the reassign button (it is listed first under them, live again until
// their next statement); a settled or voided claim, or a move by anyone else, is refused with the claim's error.
// A steward re-settles a SETTLED claim (outside any closed period) with the re-settle button: its due is frozen again at the
// figure it would have today as an open claim bound by no statement, and its log gains a SETTLED line; else it is refused with the claim's error.
// An OPEN claim is deferred to a date after the sheet's today with the defer date and button (a claim that is not OPEN, a date
// not after today or a claim dated inside a closed period is refused with the claim's error): until the sheet's today reaches
// that date it counts in no total, is not overdue, cannot be settled and is left unbound by a statement. Each claim shows
// deferred (yes while it is deferred) and the date it was deferred until.
// Each claim shows its proofs count, raised by one with the proof button (a SETTLED or VOID claim, or one dated inside a closed
// period, is refused with the claim's error). A claim whose due is above the proof threshold in force on its date cannot be
// settled or approved while its proofs count is 0: that is refused with the claim's error, before the clerk limit is considered.
// Each claim shows its status log, oldest first: one line per status it has had, with who set it, the sheet's today then and its due then.
export function ClaimList({
  claims,
  people,
  rateOn,
}: {
  claims: Claim[];
  people: Person[];
  rateOn: (date: string) => string | undefined;
}) {
  return (
    <ul data-testid="claims">
      {claims.map((c) => (
        <ClaimRow key={c.id} claim={c} people={people} rate={rateOn(c.date)} />
      ))}
    </ul>
  );
}

function ClaimRow({ claim: c, people, rate }: { claim: Claim; people: Person[]; rate: string | undefined }) {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const [editing, setEditing] = useSearchParam('editClaim', asString);
  const queryClient = useQueryClient();
  const period = periodOf(usePeriods(sheet), c.date);
  const floor = useFloor(sheet);
  const today = useSheetToday(sheet);
  const latePct = useLatePct(sheet);
  const policy = usePolicyDate(sheet);

  const status = useMutation({
    mutationFn: ({ action, reason }: { action: 'open' | 'settle' | 'void' | 'approve' | 'reject'; reason?: string }) =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheet)}/claims/${encodeURIComponent(c.id)}/status`,
        { action, user, reason },
      ),
    onSuccess: (_, { action }) => {
      if (action === 'reject') setReason('');
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    },
  });

  const [reason, setReason] = useState('');
  const [toPersonId, setToPersonId] = useState('');

  const reassign = useMutation({
    mutationFn: (to: string) =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheet)}/claims/${encodeURIComponent(c.id)}/reassign`,
        { toPersonId: to, user },
      ),
    onSuccess: () => {
      setToPersonId('');
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
      ]);
    },
  });

  const resettle = useMutation({
    mutationFn: () =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheet)}/claims/${encodeURIComponent(c.id)}/resettle`,
        { user },
      ),
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
      ]),
  });

  const [deferUntil, setDeferUntil] = useState('');

  const defer = useMutation({
    mutationFn: (until: string) =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheet)}/claims/${encodeURIComponent(c.id)}/defer`,
        { until, user },
      ),
    onSuccess: () => {
      setDeferUntil('');
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
      ]);
    },
  });

  const proof = useMutation({
    mutationFn: () =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheet)}/claims/${encodeURIComponent(c.id)}/proof`,
        { user },
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
  });

  const [editRefused, setEditRefused] = useState(false);

  // The next action on the row removes an old error.
  const act = (run: () => void) => {
    status.reset();
    reassign.reset();
    resettle.reset();
    defer.reset();
    proof.reset();
    setEditRefused(false);
    run();
  };

  const edit = () =>
    act(() => {
      if (c.status === 'DRAFT') setEditing(c.id);
      else setEditRefused(true);
    });

  return (
    <li data-testid={`claim-row-${c.id}`}>
      <ClaimSelect claimId={c.id} />{' '}
      <span data-testid="claim-amount">{c.amount}</span>{' '}
      <span data-testid="claim-date">{c.date}</span>{' '}
      <span data-testid="claim-note">{c.note}</span>{' '}
      <span data-testid="claim-status">{c.status}</span>{' '}
      <span data-testid="claim-number">{c.number}</span>{' '}
      <span data-testid="claim-due">{showDue(claimDue(c, rate, floor, { pct: latePct, today }, policy))}</span>{' '}
      <span data-testid="claim-due-by">{dueBy(c)}</span>{' '}
      <span data-testid="claim-overdue">{isOverdue(c, today) ? 'yes' : 'no'}</span>{' '}
      <span data-testid="claim-opened-by">{c.openedBy}</span>{' '}
      <span data-testid="claim-period">{period?.name ?? ''}</span>{' '}
      <span data-testid="claim-stated">{c.stated ? 'yes' : 'no'}</span>{' '}
      <span data-testid="claim-corrects">{c.corrects}</span>{' '}
      <span data-testid="claim-deferred">{isDeferred(c, today) ? 'yes' : 'no'}</span>{' '}
      <span data-testid="claim-deferred-until">{c.deferredUntil ?? ''}</span>{' '}
      <span data-testid="claim-proofs">{c.proofs ?? '0'}</span>{' '}
      <ol data-testid="claim-log">
        {(c.log ?? []).map((line, i) => (
          <li key={i} data-testid="claim-log-line">
            {line}
          </li>
        ))}
      </ol>{' '}
      <button type="button" data-testid="claim-proof" onClick={() => act(() => proof.mutate())}>
        Add proof
      </button>{' '}
      <button type="button" data-testid="claim-open" onClick={() => act(() => status.mutate({ action: 'open' }))}>
        Open
      </button>{' '}
      <button type="button" data-testid="claim-settle" onClick={() => act(() => status.mutate({ action: 'settle' }))}>
        Settle
      </button>{' '}
      <button type="button" data-testid="claim-resettle" onClick={() => act(() => resettle.mutate())}>
        Re-settle
      </button>{' '}
      <button type="button" data-testid="claim-void" onClick={() => act(() => status.mutate({ action: 'void' }))}>
        Void
      </button>{' '}
      <button type="button" data-testid="claim-approve" onClick={() => act(() => status.mutate({ action: 'approve' }))}>
        Approve
      </button>{' '}
      <input
        data-testid="claim-reject-reason"
        value={reason}
        placeholder="Reason to reject"
        onChange={(e) => act(() => setReason(e.target.value))}
      />{' '}
      <button type="button" data-testid="claim-reject" onClick={() => act(() => status.mutate({ action: 'reject', reason }))}>
        Reject
      </button>{' '}
      <input
        type="date"
        data-testid="claim-defer-date"
        value={deferUntil}
        onChange={(e) => act(() => setDeferUntil(e.target.value))}
      />{' '}
      <button type="button" data-testid="claim-defer" onClick={() => act(() => defer.mutate(deferUntil))}>
        Defer
      </button>{' '}
      <select
        data-testid="claim-reassign-to"
        value={toPersonId}
        onChange={(e) => act(() => setToPersonId(e.target.value))}
      >
        <option value="">Move to…</option>
        {people
          .filter((p) => p.id !== c.personId)
          .map((p) => (
            <option key={p.id} value={p.id}>
              {p.name}
            </option>
          ))}
      </select>{' '}
      <button type="button" data-testid="claim-reassign" onClick={() => act(() => reassign.mutate(toPersonId))}>
        Reassign
      </button>{' '}
      {editing === c.id ? (
        <button type="button" data-testid="claim-cancel" onClick={() => act(() => setEditing(undefined))}>
          Cancel
        </button>
      ) : (
        <button type="button" data-testid="claim-edit" onClick={edit}>
          Edit
        </button>
      )}
      {editRefused && (
        <span data-testid="claim-error" role="alert">
          {' '}
          Not allowed: only a DRAFT claim can be edited; to change a {c.status} claim, void it and add a new one.
        </span>
      )}
      {status.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}
          Not allowed: a {c.status} claim cannot make that status change (a deferred claim cannot be settled until its deferred-until date, the user who opened a claim cannot settle or approve it, a claim whose due is above the proof threshold needs a proof to be settled or approved, only a steward can approve or reject a PENDING claim and a rejection needs a reason, and no claim dated inside a closed period can change status).
        </span>
      )}
      {resettle.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}
          Not allowed: only a steward can re-settle a claim, only a SETTLED one, and not one dated inside a closed period.
        </span>
      )}
      {proof.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}
          Not allowed: a SETTLED or VOID claim, or one dated inside a closed period, cannot take a proof.
        </span>
      )}
      {defer.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}
          Not allowed: only an OPEN claim can be deferred, only to a date after the sheet's today, and not one dated inside a closed period.
        </span>
      )}
      {reassign.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}
          Not allowed: only a steward can move a claim, and only a DRAFT, OPEN or PENDING claim can move to another person.
        </span>
      )}
    </li>
  );
}
