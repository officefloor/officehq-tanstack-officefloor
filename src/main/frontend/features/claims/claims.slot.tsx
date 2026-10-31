import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { gradeOn, usePeople, type Person } from '../people/usePeople';
import { periodOf, usePeriods } from '../periods/usePeriods';
import { rateOn, showDue, useRates } from '../rates/useRates';
import { useFloor, useLate } from '../settings/useSettings';
import { useSheetToday } from '../sheet/useSheet';
import { useSelectedClaims } from '../bulk/useSelectedClaims';
import {
  AMOUNT,
  CLAIM_NUMBER,
  claimDueCents,
  claimed,
  claimsKey,
  dueBy,
  live,
  overdue,
  overdueFigures,
  personDueCents,
  useClaims,
  type Claim,
} from './useClaims';

const STATUS_REFUSED = "This claim's status cannot change that way (whoever opened a claim cannot settle it).";
const OVER_LIMIT = "A clerk cannot settle a claim whose due is over the sheet's clerk limit.";
const NOT_STEWARD = 'Only a steward can approve a pending claim, and never the user who opened it.';
const NO_REASON = 'A rejection needs a reason.';
const NOT_STEWARD_MOVE = 'Only a steward can reassign a claim.';
const CANNOT_MOVE = 'Only a DRAFT, OPEN or PENDING claim can be reassigned to another person.';
const NO_TARGET = 'Choose another person to reassign this claim to.';
const NOT_DRAFT = 'Only a DRAFT claim can be edited: to change an open claim, void it and add a new one.';

const ACTIONS = ['open', 'settle', 'approve', 'reject', 'void'] as const;
type Action = (typeof ACTIONS)[number];
const ACTION_LABELS: Record<Action, string> = { open: 'Open', settle: 'Settle', approve: 'Approve', reject: 'Reject', void: 'Void' };

/**
 * A person's claims: what they have claimed in all and their due-total, the sum of their claims' rounded dues,
 * one form to add a claim or edit the one whose editor is open (only a DRAFT can be edited), and the claims, newest first, each
 * with its period (the one whose dates contain its date, if any), its due (fixed once it is settled, else bound by the last statement, else at the rate in force on the claim's date, at least the sheet's floor; in the
 * due-total too), its status, who opened it, whether a statement binds it and the buttons to open, settle, approve, reject (with a reason) or void it (its opener cannot settle
 * or approve it). A clerk's settle over the clerk limit leaves the claim PENDING until a steward approves
 * or rejects it; while PENDING it shows its live figure, not the one a statement bound it to.
 * Only OPEN, PENDING and SETTLED claims count toward claimed and the due-total, and not the OPEN or
 * PENDING ones while the person is held.
 * A claim with a negative amount is a correction: it names, by number, a SETTLED claim of the person
 * that it corrects (refused otherwise), shows that number, and its due has no floor and no late charge.
 * Each claim shows its due-by (its date plus the sheet's terms) and whether it is overdue (OPEN and
 * due by before the sheet's today); the person shows how many of the claims that count are overdue
 * and their dues added up.
 * A steward can reassign a DRAFT, OPEN or PENDING claim to another person on the sheet: it keeps its
 * number and date, is no longer bound by a statement, and is listed as its new person's newest claim.
 */
function PersonClaims({ user, sheet, person }: { user: string; sheet: string; person: Person }) {
  const queryClient = useQueryClient();
  const { data: all = [] } = useClaims(sheet);
  const claims = all.filter((c) => c.personId === person.id);
  // While the person is held, their OPEN and PENDING claims count toward neither claimed nor the due-total.
  const liveClaims = live(claims, () => person.held);
  const { data: everyone = [] } = usePeople(sheet);
  const others = everyone.filter((p) => p.id !== person.id);
  const { data: rates = [] } = useRates(sheet);
  const { data: periods = [] } = usePeriods(sheet);
  const today = useSheetToday(sheet);
  const pct = rateOn(rates, gradeOn(person, today), today);
  const floor = useFloor(sheet);
  const lateBy = useLate(sheet);
  const terms = lateBy.terms;
  const late = overdueFigures(liveClaims, lateBy, floor, (d) => rateOn(rates, gradeOn(person, d), d));
  const [editing, setEditing] = useState<number | null>(null);
  const [amount, setAmount] = useState('');
  const [date, setDate] = useState('');
  const [note, setNote] = useState('');
  const [corrects, setCorrects] = useState('');
  const [invalid, setInvalid] = useState(false);
  // The claim whose last status change or edit was refused: its row shows the error.
  const [refused, setRefused] = useState<{ id: number; message: string } | null>(null);
  // The claims ticked for a bulk action (the bulk-open panel acts on them).
  const [selected, setSelected] = useSelectedClaims();
  // The reason typed for rejecting each pending claim, by claim id.
  const [reasons, setReasons] = useState<Record<number, string>>({});
  // The person chosen to reassign each claim to, by claim id.
  const [moveTo, setMoveTo] = useState<Record<number, string>>({});
  const base = `/api/sheets/${encodeURIComponent(sheet)}`;

  const submitOnce = useSubmitOnce();
  const save = useMutation({
    mutationFn: (c: { id: number | null; amount: string; date: string; note: string; corrects: string }) => {
      const body = { amount: c.amount, date: c.date, note: c.note, corrects: c.corrects, user };
      return c.id === null
        ? postJson<Claim>(`${base}/people/${person.id}/claims`, body)
        : putJson<Claim>(`${base}/claims/${c.id}`, body);
    },
    onSuccess: async () => {
      close();
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    },
    // An edit of a claim no longer a DRAFT is refused on the claim itself; the editor stays open.
    onError: async (e, c) => {
      if (c.id !== null && e instanceof HttpError && e.status === 409 && e.body.includes('DRAFT')) {
        setRefused({ id: c.id, message: NOT_DRAFT });
        await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
      }
    },
  });

  const changeStatus = useMutation({
    mutationFn: (c: { id: number; action: Action; reason?: string }) =>
      postJson<Claim>(`${base}/claims/${c.id}/status`, { action: c.action, user, reason: c.reason }),
    onMutate: () => setRefused(null),
    onSuccess: (_, c) => {
      if (c.action === 'reject') setReasons((r) => ({ ...r, [c.id]: '' }));
    },
    onError: (e, c) =>
      setRefused({
        id: c.id,
        message: !(e instanceof HttpError)
          ? STATUS_REFUSED
          : e.body.includes('clerk limit')
            ? OVER_LIMIT
            : e.body.includes('approve')
              ? NOT_STEWARD
              : e.body.includes('reason')
                ? NO_REASON
                : STATUS_REFUSED,
      }),
    onSettled: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
  });

  const reassign = useMutation({
    mutationFn: (c: { id: number; toPersonId: number }) =>
      postJson<Claim>(`${base}/claims/${c.id}/reassign`, { toPersonId: c.toPersonId, user }),
    onMutate: () => setRefused(null),
    onSuccess: (_, c) => setMoveTo((m) => ({ ...m, [c.id]: '' })),
    onError: (e, c) =>
      setRefused({
        id: c.id,
        message: e instanceof HttpError && e.body.includes('steward')
          ? NOT_STEWARD_MOVE
          : e instanceof HttpError && e.body.includes('retired')
            ? 'That person is retired before this claim\'s date.'
            : e instanceof HttpError && e.body.includes('closed')
              ? 'This claim is dated in a closed period.'
              : CANNOT_MOVE,
      }),
    onSettled: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
  });

  // An error goes as soon as its form is changed or submitted again, or an editor is opened.
  const clearError = () => {
    save.reset();
    setInvalid(false);
    setRefused(null);
  };
  const close = () => {
    setEditing(null);
    setAmount('');
    setDate('');
    setNote('');
    setCorrects('');
  };
  const open = (c: Claim) => {
    clearError();
    // Only a DRAFT can be edited: to change an open claim, void it and add a new one.
    if (c.status !== 'DRAFT') {
      cancel();
      setRefused({ id: c.id, message: NOT_DRAFT });
      return;
    }
    setEditing(c.id);
    setAmount(c.amount);
    setDate(c.date);
    setNote(c.note);
    setCorrects(c.corrects === null ? '' : String(c.corrects));
  };
  const cancel = () => {
    clearError();
    close();
  };

  return (
    <>
      <td data-testid="person-claimed">{claimed(liveClaims)}</td>
      <td data-testid="person-due-total">{showDue(personDueCents(liveClaims, floor, pct, (d) => rateOn(rates, gradeOn(person, d), d), lateBy))}</td>
      <td data-testid="person-overdue-count">{late.count}</td>
      <td data-testid="person-overdue-total">{showDue(late.cents)}</td>
      <td>
        <form
          data-testid="claim-form"
          onSubmit={(e) => {
            e.preventDefault();
            // A negative amount is a correction: it must name the number of the claim it corrects.
            if (!AMOUNT.test(amount.trim()) || (amount.trim().startsWith('-') && !CLAIM_NUMBER.test(corrects.trim()))) {
              // Refused here, before anything is sent: the store is untouched.
              save.reset();
              setInvalid(true);
              return;
            }
            setInvalid(false);
            submitOnce(() => save.mutateAsync({ id: editing, amount: amount.trim(), date: date.trim(), note, corrects: corrects.trim() }));
          }}
        >
          <input
            data-testid="claim-form-amount"
            aria-label="Amount"
            placeholder="0.00"
            value={amount}
            onChange={(e) => {
              setAmount(e.target.value);
              clearError();
            }}
          />{' '}
          <input
            data-testid="claim-form-date"
            aria-label="Date"
            placeholder="yyyy-mm-dd"
            value={date}
            onChange={(e) => {
              setDate(e.target.value);
              clearError();
            }}
          />{' '}
          <input
            data-testid="claim-form-note"
            aria-label="Note"
            placeholder="note"
            value={note}
            onChange={(e) => {
              setNote(e.target.value);
              clearError();
            }}
          />{' '}
          <input
            data-testid="claim-form-corrects"
            aria-label="Corrects claim number"
            placeholder="corrects no."
            value={corrects}
            onChange={(e) => {
              setCorrects(e.target.value);
              clearError();
            }}
          />{' '}
          <button type="submit" data-testid="claim-form-submit" disabled={save.isPending}>
            {editing === null ? 'Add claim' : 'Save claim'}
          </button>
          {(invalid || (save.isError && refused === null)) && (
            <span data-testid="claim-form-error" role="alert">
              {' '}
              {invalid
                ? 'The amount must have up to two decimals; a negative amount must name the number of the settled claim it corrects.'
                : save.error instanceof HttpError && save.error.status === 409 && save.error.body.includes('retired')
                  ? `${person.name} is retired from ${person.retiredFrom}: a claim cannot be dated after it.`
                  : save.error instanceof HttpError && save.error.body.includes('correction')
                    ? `A correction must name a settled claim of ${person.name}.`
                  : 'Could not save this claim.'}
            </span>
          )}
        </form>
        <ul data-testid="claim-list">
          {claims.map((c) => (
            <li key={c.id} data-testid={`claim-row-${c.id}`}>
              <input
                type="checkbox"
                data-testid="claim-select"
                aria-label="Select claim"
                checked={selected.includes(c.id)}
                onChange={(e) =>
                  setSelected(e.target.checked ? [...selected, c.id] : selected.filter((id) => id !== c.id))
                }
              />{' '}
              <span data-testid="claim-amount">{c.amount}</span>{' '}
              <span data-testid="claim-date">{c.date}</span>{' '}
              <span data-testid="claim-period">{periodOf(periods, c.date)?.name ?? ''}</span>{' '}
              <span data-testid="claim-note">{c.note}</span>{' '}
              <span data-testid="claim-due">{showDue(claimDueCents(c, floor, rateOn(rates, gradeOn(person, c.date), c.date), lateBy))}</span>{' '}
              <span data-testid="claim-number">{c.number ?? ''}</span>{' '}
              <span data-testid="claim-corrects">{c.corrects ?? ''}</span>{' '}
              <span data-testid="claim-status">{c.status}</span>{' '}
              <span data-testid="claim-opened-by">{c.openedBy ?? ''}</span>{' '}
              <span data-testid="claim-stated">{c.stated ? 'yes' : 'no'}</span>{' '}
              <span data-testid="claim-due-by">{dueBy(c, terms)}</span>{' '}
              <span data-testid="claim-overdue">{overdue(c, terms, today) ? 'yes' : 'no'}</span>{' '}
              {ACTIONS.map((action) => (
                <span key={action}>
                  {action === 'reject' && (
                    <>
                      <input
                        data-testid="claim-reject-reason"
                        aria-label="Reason for rejecting"
                        placeholder="reason"
                        value={reasons[c.id] ?? ''}
                        onChange={(e) => {
                          setReasons({ ...reasons, [c.id]: e.target.value });
                          if (refused?.id === c.id) setRefused(null);
                        }}
                      />{' '}
                    </>
                  )}
                  <button
                    type="button"
                    data-testid={`claim-${action}`}
                    disabled={changeStatus.isPending}
                    onClick={() => {
                      if (action === 'reject' && (reasons[c.id] ?? '').trim() === '') {
                        // Refused here, before anything is sent: the store is untouched.
                        setRefused({ id: c.id, message: NO_REASON });
                        return;
                      }
                      changeStatus.mutate({ id: c.id, action, reason: action === 'reject' ? (reasons[c.id] ?? '').trim() : undefined });
                    }}
                  >
                    {ACTION_LABELS[action]}
                  </button>{' '}
                </span>
              ))}
              <select
                data-testid="claim-reassign-to"
                aria-label="Reassign to"
                value={moveTo[c.id] ?? ''}
                onChange={(e) => {
                  setMoveTo({ ...moveTo, [c.id]: e.target.value });
                  if (refused?.id === c.id) setRefused(null);
                }}
              >
                <option value="">reassign to…</option>
                {others.map((p) => (
                  <option key={p.id} value={String(p.id)}>
                    {p.name}
                  </option>
                ))}
              </select>{' '}
              <button
                type="button"
                data-testid="claim-reassign"
                disabled={reassign.isPending}
                onClick={() => {
                  const to = moveTo[c.id] ?? '';
                  if (to === '') {
                    // Refused here, before anything is sent: the store is untouched.
                    setRefused({ id: c.id, message: NO_TARGET });
                    return;
                  }
                  reassign.mutate({ id: c.id, toPersonId: Number(to) });
                }}
              >
                Reassign
              </button>{' '}
              <button type="button" data-testid="claim-edit" onClick={() => open(c)}>
                Edit
              </button>
              {editing === c.id && (
                <>
                  {' '}
                  <button type="button" data-testid="claim-cancel" onClick={cancel}>
                    Cancel
                  </button>
                </>
              )}
              {refused?.id === c.id && (
                <span data-testid="claim-error" role="alert">
                  {' '}
                  {refused.message}
                </span>
              )}
            </li>
          ))}
        </ul>
      </td>
    </>
  );
}

export const contributions = [
  PersonColumns.fill({
    order: 10,
    Component: () => (
      <>
        <th>Claimed</th>
        <th>Due</th>
        <th>Overdue</th>
        <th>Overdue due</th>
        <th>Claims</th>
      </>
    ),
  }),
  PersonCells.fill({ order: 10, Component: PersonClaims }),
];
