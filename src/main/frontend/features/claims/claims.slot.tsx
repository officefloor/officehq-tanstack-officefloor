import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import type { Person } from '../people/usePeople';
import { periodOf, usePeriods } from '../periods/usePeriods';
import { rateOn, showDue, useRates } from '../rates/useRates';
import { useSheetToday } from '../sheet/useSheet';
import {
  AMOUNT,
  claimDueCents,
  claimed,
  claimsKey,
  live,
  personDueCents,
  useClaims,
  type Claim,
} from './useClaims';

const STATUS_REFUSED = "This claim's status cannot change that way (whoever opened a claim cannot settle it).";
const NOT_DRAFT = 'Only a DRAFT claim can be edited: to change an open claim, void it and add a new one.';

/**
 * A person's claims: what they have claimed in all and their due-total, the sum of their claims' rounded dues,
 * one form to add a claim or edit the one whose editor is open (only a DRAFT can be edited), and the claims, newest first, each
 * with its period (the one whose dates contain its date, if any), its due (fixed once it is settled, else bound by the last statement, else at the rate in force on the claim's date; in the
 * due-total too), its status, who opened it, whether a statement binds it and the buttons to open, settle or void it (its opener cannot settle it). Only OPEN and SETTLED claims
 * count toward claimed and the due-total, and not the OPEN ones while the person is held.
 */
function PersonClaims({ user, sheet, person }: { user: string; sheet: string; person: Person }) {
  const queryClient = useQueryClient();
  const { data: all = [] } = useClaims(sheet);
  const claims = all.filter((c) => c.personId === person.id);
  // While the person is held, their OPEN claims count toward neither claimed nor the due-total.
  const liveClaims = live(claims, () => person.held);
  const { data: rates = [] } = useRates(sheet);
  const { data: periods = [] } = usePeriods(sheet);
  const today = useSheetToday(sheet);
  const pct = rateOn(rates, person.grade, today);
  const [editing, setEditing] = useState<number | null>(null);
  const [amount, setAmount] = useState('');
  const [date, setDate] = useState('');
  const [note, setNote] = useState('');
  const [invalid, setInvalid] = useState(false);
  // The claim whose last status change or edit was refused: its row shows the error.
  const [refused, setRefused] = useState<{ id: number; message: string } | null>(null);
  const base = `/api/sheets/${encodeURIComponent(sheet)}`;

  const submitOnce = useSubmitOnce();
  const save = useMutation({
    mutationFn: (c: { id: number | null; amount: string; date: string; note: string }) => {
      const body = { amount: c.amount, date: c.date, note: c.note, user };
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
    mutationFn: (c: { id: number; action: 'open' | 'settle' | 'void' }) =>
      postJson<Claim>(`${base}/claims/${c.id}/status`, { action: c.action, user }),
    onMutate: () => setRefused(null),
    onError: (_e, c) => setRefused({ id: c.id, message: STATUS_REFUSED }),
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
  };
  const cancel = () => {
    clearError();
    close();
  };

  return (
    <>
      <td data-testid="person-claimed">{claimed(liveClaims)}</td>
      <td data-testid="person-due-total">{showDue(personDueCents(liveClaims, pct, (d) => rateOn(rates, person.grade, d)))}</td>
      <td>
        <form
          data-testid="claim-form"
          onSubmit={(e) => {
            e.preventDefault();
            if (!AMOUNT.test(amount.trim())) {
              // Refused here, before anything is sent: the store is untouched.
              save.reset();
              setInvalid(true);
              return;
            }
            setInvalid(false);
            submitOnce(() => save.mutateAsync({ id: editing, amount: amount.trim(), date: date.trim(), note }));
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
          <button type="submit" data-testid="claim-form-submit" disabled={save.isPending}>
            {editing === null ? 'Add claim' : 'Save claim'}
          </button>
          {(invalid || (save.isError && refused === null)) && (
            <span data-testid="claim-form-error" role="alert">
              {' '}
              {invalid
                ? 'The amount must be 0.00 or more, with up to two decimals.'
                : save.error instanceof HttpError && save.error.status === 409 && save.error.body.includes('retired')
                  ? `${person.name} is retired from ${person.retiredFrom}: a claim cannot be dated after it.`
                  : 'Could not save this claim.'}
            </span>
          )}
        </form>
        <ul data-testid="claim-list">
          {claims.map((c) => (
            <li key={c.id} data-testid={`claim-row-${c.id}`}>
              <span data-testid="claim-amount">{c.amount}</span>{' '}
              <span data-testid="claim-date">{c.date}</span>{' '}
              <span data-testid="claim-period">{periodOf(periods, c.date)?.name ?? ''}</span>{' '}
              <span data-testid="claim-note">{c.note}</span>{' '}
              <span data-testid="claim-due">{showDue(claimDueCents(c, rateOn(rates, person.grade, c.date)))}</span>{' '}
              <span data-testid="claim-number">{c.number ?? ''}</span>{' '}
              <span data-testid="claim-status">{c.status}</span>{' '}
              <span data-testid="claim-opened-by">{c.openedBy ?? ''}</span>{' '}
              <span data-testid="claim-stated">{c.stated ? 'yes' : 'no'}</span>{' '}
              {(['open', 'settle', 'void'] as const).map((action) => (
                <span key={action}>
                  <button
                    type="button"
                    data-testid={`claim-${action}`}
                    disabled={changeStatus.isPending}
                    onClick={() => changeStatus.mutate({ id: c.id, action })}
                  >
                    {action === 'open' ? 'Open' : action === 'settle' ? 'Settle' : 'Void'}
                  </button>{' '}
                </span>
              ))}
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
        <th>Claims</th>
      </>
    ),
  }),
  PersonCells.fill({ order: 10, Component: PersonClaims }),
];
