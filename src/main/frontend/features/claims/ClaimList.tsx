import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { claimDue, showDue } from '../rates/due';
import { claimsKey, type Claim } from './useClaims';
import { periodOf, usePeriods } from '../periods/usePeriods';
import { useFloor, useLatePct, useTerms } from '../settings/useSettings';
import { useSheetToday } from '../sheet/useSheetToday';
import { dueBy, isOverdue } from './overdue';

// A person's claims, newest first, each with its amount, date, note, status, number, opened-by, period (the one whose dates contain its date, blank for none), stated (yes when the
// person's last statement bound it), corrects (the number a correction corrects, blank for other claims) and due (fixed when it was settled, else bound by the last statement; otherwise its amount at the person's grade's rate in force on the claim's date raised to the sheet's floor, n/a when none applies; an overdue claim gains the sheet's late percent of that figure for every full 30 days overdue), due-by (its date plus the sheet's terms), overdue (yes when it is OPEN and its due-by is before the sheet's today), the status buttons (open, settle, void —
// one the claim's status does not allow is refused with the claim's error) and an edit button that
// opens a DRAFT claim in the person's claim form (cancel closes it again); editing any other claim is
// refused with the claim's error — to change an open claim, void it and add a new one.
export function ClaimList({ claims, rateOn }: { claims: Claim[]; rateOn: (date: string) => string | undefined }) {
  return (
    <ul data-testid="claims">
      {claims.map((c) => (
        <ClaimRow key={c.id} claim={c} rate={rateOn(c.date)} />
      ))}
    </ul>
  );
}

function ClaimRow({ claim: c, rate }: { claim: Claim; rate: string | undefined }) {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const [editing, setEditing] = useSearchParam('editClaim', asString);
  const queryClient = useQueryClient();
  const period = periodOf(usePeriods(sheet), c.date);
  const floor = useFloor(sheet);
  const terms = useTerms(sheet);
  const today = useSheetToday(sheet);
  const latePct = useLatePct(sheet);

  const status = useMutation({
    mutationFn: (action: 'open' | 'settle' | 'void') =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheet)}/claims/${encodeURIComponent(c.id)}/status`,
        { action, user },
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
  });

  const [editRefused, setEditRefused] = useState(false);

  // The next action on the row removes an old error.
  const act = (run: () => void) => {
    status.reset();
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
      <span data-testid="claim-amount">{c.amount}</span>{' '}
      <span data-testid="claim-date">{c.date}</span>{' '}
      <span data-testid="claim-note">{c.note}</span>{' '}
      <span data-testid="claim-status">{c.status}</span>{' '}
      <span data-testid="claim-number">{c.number}</span>{' '}
      <span data-testid="claim-due">{showDue(claimDue(c, rate, floor, { pct: latePct, terms, today }))}</span>{' '}
      <span data-testid="claim-due-by">{dueBy(c, terms)}</span>{' '}
      <span data-testid="claim-overdue">{isOverdue(c, terms, today) ? 'yes' : 'no'}</span>{' '}
      <span data-testid="claim-opened-by">{c.openedBy}</span>{' '}
      <span data-testid="claim-period">{period?.name ?? ''}</span>{' '}
      <span data-testid="claim-stated">{c.stated ? 'yes' : 'no'}</span>{' '}
      <span data-testid="claim-corrects">{c.corrects}</span>{' '}
      <button type="button" data-testid="claim-open" onClick={() => act(() => status.mutate('open'))}>
        Open
      </button>{' '}
      <button type="button" data-testid="claim-settle" onClick={() => act(() => status.mutate('settle'))}>
        Settle
      </button>{' '}
      <button type="button" data-testid="claim-void" onClick={() => act(() => status.mutate('void'))}>
        Void
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
          Not allowed: a {c.status} claim cannot make that status change (the user who opened a claim cannot settle it, a clerk cannot settle a claim whose due is above the clerk limit, and no claim dated inside a closed period can change status).
        </span>
      )}
    </li>
  );
}
