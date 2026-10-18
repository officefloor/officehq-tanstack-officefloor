import { useState } from 'react';
import type { FormEvent } from 'react';
import { HttpError } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HomeMain } from '../../slots/defs/homeMain';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { useCurrentSheet, useCurrentUser, useSheetToday } from '../session/session';
import { afterRetirement, peopleKey, usePeople } from '../people/people';
import { NA, show, usePersonRate } from '../rates/rates';
import { periodsKey, useClosedPeriodOf, usePeriodOf } from '../periods/periods';
import { rolesKey, settingsKey, useIsSteward } from '../settings/settings';
import { AMOUNT, CLAIM_ACTIONS, addClaim, correctionError, isCorrection, canEdit, canTake, claimAction, claimsKey, needsSteward, selfSettle, total, updateClaim, useClaimDue, useClaims, useCounts, useDueBy, useOverdue, useSelectedClaims } from './claims';
import type { Claim, ClaimAction, ClaimInput } from './claims';

const EMPTY: ClaimInput = { amount: '', date: '', note: '', corrects: '' };
const LABEL: Record<ClaimAction, string> = { open: 'Open', settle: 'Settle', void: 'Void', approve: 'Approve', reject: 'Reject' };
const showDue = (due: bigint | undefined) => (due === undefined ? NA : show(due));
const DONE: Record<ClaimAction, string> = { open: 'opened', settle: 'settled', void: 'voided', approve: 'approved', reject: 'rejected' };

function ClaimColumns() {
  return (
    <>
      <th>Claimed</th>
      <th>Claims</th>
    </>
  );
}

// A person's claimed total, and their claims: a form that adds a claim or, once a claim's edit is
// pressed, edits it; and the claims listed newest first, each with its status and the buttons that
// change it. An empty date means the sheet's today; a claim dated after the person's retirement is
// refused; a negative amount is a correction, which must name by number a settled
// claim of the same person. A refused status change, or the edit of a claim
// that is not a DRAFT, shows on its claim. A clerk settling a claim whose due is above the clerk limit sends
// it to PENDING; a steward other than its opener approves it, or rejects it with the reason typed on its row.
function PersonClaims({ personId }: { personId: number }) {
  const [sheet] = useCurrentSheet();
  const [user] = useCurrentUser();
  const allClaims = useClaims().data ?? [];
  const claims = allClaims.filter((c) => c.personId === personId);
  const person = (usePeople().data ?? []).find((p) => p.id === personId);
  const counted = useCounts();
  const today = useSheetToday();
  const rateOf = usePersonRate();
  const claimDue = useClaimDue();
  const dueBy = useDueBy();
  const overdue = useOverdue();
  const periodOf = usePeriodOf();
  const closedOf = useClosedPeriodOf();
  const isSteward = useIsSteward();
  const [reasons, setReasons] = useState<Record<number, string>>({});
  const [editing, setEditing] = useState<number | undefined>();
  const [form, setForm] = useState<ClaimInput>(EMPTY);
  const [error, setError] = useState('');
  const [claimErrors, setClaimErrors] = useState<Record<number, string>>({});
  const queryClient = useQueryClient();
  const once = useSubmitOnce();
  const selection = useSelectedClaims();

  const clearClaimError = (id: number) =>
    setClaimErrors(({ [id]: _, ...rest }) => rest);
  const refuse = (id: number, message: string) => setClaimErrors((e) => ({ ...e, [id]: message }));

  const act = useMutation({
    mutationFn: ({ claim, action }: { claim: Claim; action: ClaimAction }) =>
      claimAction(sheet!, user, claim.id, action, reasons[claim.id]?.trim() ?? ''),
    onSuccess: (_, { claim, action }) => {
      if (action === 'reject') setReasons(({ [claim.id]: _r, ...rest }) => rest);
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheet!) });
    },
    onError: async (e, { claim, action }) => {
      const reason = e instanceof HttpError ? (e.body as { reason?: string } | undefined)?.reason : undefined;
      refuse(claim.id, reason === 'steward'
        ? `Only a steward can ${action} a pending claim.`
        : `This claim cannot be ${DONE[action]}.`);
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet!) });
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheet!) });
      await queryClient.invalidateQueries({ queryKey: settingsKey(sheet!) });
      await queryClient.invalidateQueries({ queryKey: rolesKey(sheet!) });
    },
  });

  const take = (claim: Claim, action: ClaimAction) => {
    clearClaimError(claim.id);
    if (!canTake(claim, action)) {
      refuse(claim.id, `A ${claim.status} claim cannot be ${DONE[action]}.`);
      return;
    }
    const closed = closedOf(claim.date);
    if (closed) {
      refuse(claim.id, `The claim is dated inside the closed period ${closed.name}.`);
      return;
    }
    if (needsSteward(action) && !isSteward(user)) {
      refuse(claim.id, `Only a steward can ${action} a pending claim.`);
      return;
    }
    if (selfSettle(claim, user, action)) {
      refuse(claim.id, `The user who opened a claim cannot ${action} it.`);
      return;
    }
    if (action === 'reject' && !(reasons[claim.id] ?? '').trim()) {
      refuse(claim.id, 'A rejection must give a reason.');
      return;
    }
    act.mutate({ claim, action });
  };

  const notDraft = (id: number) => refuse(id, 'Only a DRAFT claim can be edited; void it and add a new one.');

  const save = useMutation({
    mutationFn: (input: ClaimInput) =>
      editing !== undefined ? updateClaim(sheet!, user, editing, input) : addClaim(sheet!, user, personId, input),
    onSuccess: async () => {
      setForm(EMPTY);
      setEditing(undefined);
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet!) });
    },
    onError: async (e) => {
      // Another user opened (or voided) the claim since it was shown: the edit is refused on the claim.
      if (editing !== undefined && e instanceof HttpError && (e.body as { reason?: string } | undefined)?.reason === 'status') {
        notDraft(editing);
        setEditing(undefined);
        setForm(EMPTY);
        await queryClient.invalidateQueries({ queryKey: claimsKey(sheet!) });
        return;
      }
      const reason = e instanceof HttpError ? (e.body as { reason?: string } | undefined)?.reason : undefined;
      setError(reason === 'retired'
        ? 'The claim could not be saved: it is dated after the person retired.'
        : e instanceof HttpError && e.status === 400 && isCorrection(form.amount)
        ? 'The claim could not be saved: a correction must name a settled claim of the same person.'
        : e instanceof HttpError && e.status === 409
        ? 'The claim could not be saved: it is dated inside a closed period.'
        : 'The claim could not be saved: the amount must be a number with up to two decimals.');
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheet!) });
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheet!) });
    },
  });

  const change = (patch: Partial<ClaimInput>) => {
    setForm((f) => ({ ...f, ...patch }));
    setError('');
  };

  const open = (claim: Claim) => {
    clearClaimError(claim.id);
    if (!canEdit(claim)) {
      notDraft(claim.id);
      return;
    }
    setEditing(claim.id);
    setForm({ amount: claim.amount, date: claim.date, note: claim.note, corrects: claim.corrects == null ? '' : String(claim.corrects) });
    setError('');
  };

  const cancel = () => {
    setEditing(undefined);
    setForm(EMPTY);
    setError('');
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (!AMOUNT.test(form.amount.trim())) {
      setError('The amount must be a number with up to two decimals.');
      return;
    }
    const wrong = correctionError(allClaims, personId, form.amount, form.corrects);
    if (wrong) {
      setError(wrong);
      return;
    }
    const edited = editing !== undefined ? claims.find((c) => c.id === editing) : undefined;
    const closed = (edited && closedOf(edited.date)) || (form.date !== '' ? closedOf(form.date) : undefined);
    if (closed) {
      setError(`The claim is dated inside the closed period ${closed.name}.`);
      return;
    }
    const date = form.date !== '' ? form.date : (today ?? '');
    if (afterRetirement(person, date)) {
      setError(`The claim cannot be dated after ${person!.name} retired (${person!.retiredFrom}).`);
      return;
    }
    once(() => save.mutateAsync({ amount: form.amount.trim(), date: form.date, note: form.note, corrects: form.corrects.trim() }));
  };

  return (
    <>
      <td data-testid="person-claimed">{total(claims, counted)}</td>
      <td>
        <form data-testid="claim-form" onSubmit={submit}>
          <input
            data-testid="claim-form-amount"
            placeholder="Amount"
            inputMode="decimal"
            value={form.amount}
            onChange={(e) => change({ amount: e.target.value })}
          />
          <input
            type="date"
            data-testid="claim-form-date"
            value={form.date}
            onChange={(e) => change({ date: e.target.value })}
          />
          <input
            data-testid="claim-form-note"
            placeholder="Note"
            value={form.note}
            onChange={(e) => change({ note: e.target.value })}
          />
          <input
            data-testid="claim-form-corrects"
            placeholder="Corrects no."
            inputMode="numeric"
            value={form.corrects}
            onChange={(e) => change({ corrects: e.target.value })}
          />
          <button type="submit" data-testid="claim-form-submit" disabled={save.isPending}>
            {editing !== undefined ? 'Save claim' : 'Add claim'}
          </button>
          {editing !== undefined && (
            <button type="button" data-testid="claim-form-cancel" onClick={cancel}>
              Cancel
            </button>
          )}
          {error && <span data-testid="claim-form-error">{error}</span>}
        </form>
        <ul data-testid="claim-list">
          {claims.map((c) => (
            <li key={c.id} data-testid={`claim-row-${c.id}`}>
              <input
                type="checkbox"
                data-testid="claim-select"
                aria-label="Select claim"
                checked={selection.ids.has(c.id)}
                onChange={(e) => selection.toggle(c.id, e.target.checked)}
              />{' '}
              <span data-testid="claim-amount">{c.amount}</span>{' '}
              <span data-testid="claim-date">{c.date}</span>{' '}
              <span data-testid="claim-note">{c.note}</span>{' '}
              <span data-testid="claim-period">{periodOf(c.date) ?? ''}</span>{' '}
              <span data-testid="claim-due">{showDue(claimDue(c, rateOf(personId, c.date)))}</span>{' '}
              <span data-testid="claim-due-by">{dueBy(c)}</span>{' '}
              <span data-testid="claim-overdue">{overdue(c) ? 'yes' : 'no'}</span>{' '}
              <span data-testid="claim-status">{c.status}</span>{' '}
              <span data-testid="claim-number">{c.periodNumber ?? c.number ?? ''}</span>{' '}
              <span data-testid="claim-corrects">{c.corrects ?? ''}</span>{' '}
              <span data-testid="claim-opened-by">{c.openedBy ?? ''}</span>{' '}
              <button type="button" data-testid="claim-edit" onClick={() => open(c)}>
                Edit
              </button>
              <input
                data-testid="claim-reject-reason"
                placeholder="Reject reason"
                value={reasons[c.id] ?? ''}
                onChange={(e) => {
                  setReasons((r) => ({ ...r, [c.id]: e.target.value }));
                  clearClaimError(c.id);
                }}
              />{' '}
              {CLAIM_ACTIONS.map((a) => (
                <button key={a} type="button" data-testid={`claim-${a}`} onClick={() => take(c, a)}>
                  {LABEL[a]}
                </button>
              ))}
              {claimErrors[c.id] && <span data-testid="claim-error">{claimErrors[c.id]}</span>}
            </li>
          ))}
        </ul>
      </td>
    </>
  );
}

// The sheet's claimed-total: everyone's claimed added up (only OPEN, PENDING and SETTLED claims count, and
// not the OPEN or PENDING claims of a person on hold).
function ClaimedTotal() {
  const [sheet] = useCurrentSheet();
  const claims = useClaims();
  const counted = useCounts();
  if (!sheet) return null;
  return (
    <section data-testid="rollup">
      Claimed total <strong data-testid="rollup-claimed-total">{total(claims.data ?? [], counted)}</strong>
    </section>
  );
}

export const contributions = [
  PersonColumns.fill({ order: 10, Component: ClaimColumns }),
  PersonCells.fill({ order: 10, Component: PersonClaims }),
  HomeMain.fill({ order: 20, Component: ClaimedTotal }),
];
