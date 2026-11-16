import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson } from '../../api/http';
import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPage } from '../../slots/defs/sheetPage';
import { gradeOn, usePeople, type Person } from '../people/people';
import { periodOf, usePeriods } from '../periods/periods';
import { claimDue, rateFor, useRates } from '../rates/rates';
import { dueBy, isOverdue } from '../overdue/overdue';
import { useFloor, useLate } from '../settings/settings';
import { claimsKey, live, total, useClaims, type Claim } from './claims';

type Values = { amount: string; date: string; note: string; corrects: string };
const EMPTY: Values = { amount: '', date: '', note: '', corrects: '' };

function ClaimsHead() {
  return (
    <>
      <th>Claimed</th>
      <th>Claims</th>
    </>
  );
}

type Action = 'open' | 'settle' | 'void';

/** One claim: its values, status, due, due-by (its date plus the sheet's terms), whether it is overdue, and the period its date falls in, with the buttons that edit it or change its status. An action
 *  the status does not allow, or settling a claim the user opened, is refused by the server and shows the claim's
 *  error. Only a DRAFT can be edited: Edit on any other claim shows the claim's error instead of opening the editor. */
function ClaimRow({
  sheetId,
  user,
  claim: c,
  rate,
  floor,
  editing,
  onEdit,
  onCancel,
}: {
  sheetId: string;
  user: string;
  claim: Claim;
  rate: string | undefined;
  floor: string;
  editing: boolean;
  onEdit: () => void;
  onCancel: () => void;
}) {
  const queryClient = useQueryClient();
  const periods = usePeriods(sheetId);
  const late = useLate(sheetId);
  const status = useMutation({
    mutationFn: (action: Action) =>
      putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${c.id}/status`, { action, by: user }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
  });
  const [editRefused, setEditRefused] = useState(false);
  const act = (action: Action) => {
    setEditRefused(false);
    status.reset();
    status.mutate(action);
  };

  return (
    <li data-testid={`claim-row-${c.id}`}>
      <span data-testid="claim-number">{c.number ?? ''}</span>{' '}
      <span data-testid="claim-amount">{c.amount}</span> <span data-testid="claim-date">{c.date}</span>{' '}
      <span data-testid="claim-note">{c.note}</span>{' '}
      <span data-testid="claim-due">{claimDue(c, rate, floor, late)}</span>{' '}
      <span data-testid="claim-due-by">{dueBy(c, late.terms)}</span>{' '}
      <span data-testid="claim-overdue">{isOverdue(c, late.terms, late.today) ? 'yes' : 'no'}</span>{' '}
      <span data-testid="claim-status">{c.status}</span>{' '}
      <span data-testid="claim-opened-by">{c.openedBy ?? ''}</span>{' '}
      <span data-testid="claim-stated">{c.stated ? 'yes' : 'no'}</span>{' '}
      <span data-testid="claim-corrects">{c.corrects ?? ''}</span>{' '}
      <span data-testid="claim-period">{periodOf(periods.data, c.date)?.name ?? ''}</span>{' '}
      <button
        type="button"
        data-testid="claim-edit"
        onClick={() => {
          status.reset();
          if (c.status !== 'DRAFT') {
            setEditRefused(true);
            return;
          }
          setEditRefused(false);
          onEdit();
        }}
      >
        Edit
      </button>
      {editing && (
        <button type="button" data-testid="claim-cancel" onClick={onCancel}>
          Cancel
        </button>
      )}{' '}
      <button type="button" data-testid="claim-open" disabled={status.isPending} onClick={() => act('open')}>
        Open
      </button>
      <button type="button" data-testid="claim-settle" disabled={status.isPending} onClick={() => act('settle')}>
        Settle
      </button>
      <button type="button" data-testid="claim-void" disabled={status.isPending} onClick={() => act('void')}>
        Void
      </button>
      {editRefused && (
        <span data-testid="claim-error"> Only a draft claim can be edited; void it and add a new one instead.</span>
      )}
      {status.isError && (
        <span data-testid="claim-error">
          {' '}
          {c.status === 'OPEN' && c.openedBy === user
            ? 'You opened this claim, so someone else must settle it.'
            : `A ${c.status} claim cannot be changed that way, nor one dated inside a closed period; a clerk cannot settle a claim whose due is above the clerk limit.`}
        </span>
      )}
    </li>
  );
}

/** A person's claimed, and their claims (newest first) with the form that adds one or saves the one
 *  being edited. A blank date takes the sheet's today (the server applies it). */
function PersonClaims({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId).data;
  const floor = useFloor(sheetId);
  const queryClient = useQueryClient();
  const mine = claims.data?.filter((c) => c.personId === person.id) ?? [];
  const [editingId, setEditingId] = useState<number | undefined>(undefined);
  const [values, setValues] = useState<Values>(EMPTY);

  const submit = useSubmitOnce();
  const save = useMutation({
    mutationFn: ({ id, ...v }: Values & { id?: number }) => {
      const base = `/api/sheets/${encodeURIComponent(sheetId)}`;
      const body = { ...v, by: user };
      return id === undefined
        ? postJson<Claim>(`${base}/people/${person.id}/claims`, body)
        : putJson<Claim>(`${base}/claims/${id}`, body);
    },
    onSuccess: () => {
      setValues(EMPTY);
      setEditingId(undefined);
      void queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onSettled: submit.end,
  });

  const change = (v: Partial<Values>) => {
    setValues((was) => ({ ...was, ...v }));
    save.reset();
  };
  const openEditor = (c: Claim) => {
    save.reset();
    setValues({ amount: c.amount, date: c.date, note: c.note, corrects: c.corrects === null ? '' : String(c.corrects) });
    setEditingId(c.id);
  };
  const cancel = () => {
    save.reset();
    setValues(EMPTY);
    setEditingId(undefined);
  };

  return (
    <>
      <td data-testid="person-claimed">{total(live(mine, [person]))}</td>
      <td>
        <form
          data-testid="claim-form"
          onSubmit={(e) => {
            e.preventDefault();
            if (!submit.begin()) return;
            save.reset();
            save.mutate({ ...values, id: editingId });
          }}
        >
          <input
            data-testid="claim-form-amount"
            placeholder="Amount"
            inputMode="decimal"
            value={values.amount}
            onChange={(e) => change({ amount: e.target.value })}
          />{' '}
          <input type="date" data-testid="claim-form-date" value={values.date} onChange={(e) => change({ date: e.target.value })} />{' '}
          <input data-testid="claim-form-note" placeholder="Note" value={values.note} onChange={(e) => change({ note: e.target.value })} />{' '}
          <input
            data-testid="claim-form-corrects"
            placeholder="Corrects no."
            inputMode="numeric"
            value={values.corrects}
            onChange={(e) => change({ corrects: e.target.value })}
          />{' '}
          <button type="submit" data-testid="claim-form-submit" disabled={save.isPending}>
            {editingId === undefined ? 'Add claim' : 'Save claim'}
          </button>
          {save.isError && (
            <span data-testid="claim-form-error">
              The amount must have up to two decimals, and be 0.00 or more unless it corrects, by number, a settled claim of this person; the date not inside a closed period nor after the person's retirement date, and only a draft claim can be edited.
            </span>
          )}
        </form>
        <ul data-testid="claim-list">
          {mine.map((c) => (
            <ClaimRow
              key={c.id}
              sheetId={sheetId}
              user={user}
              claim={c}
              rate={rateFor(rates, gradeOn(person, c.date), c.date)}
              floor={floor}
              editing={editingId === c.id}
              onEdit={() => openEditor(c)}
              onCancel={cancel}
            />
          ))}
        </ul>
      </td>
    </>
  );
}

/** The sheet's claimed-total: everyone's claimed (OPEN and SETTLED claims, but no held person's OPEN ones) added up. */
function ClaimedTotal({ sheetId }: { sheetId: string }) {
  const claims = useClaims(sheetId);
  const people = usePeople(sheetId);
  return (
    <p>
      Claimed total <span data-testid="rollup-claimed-total">{total(live(claims.data ?? [], people.data))}</span>
      {claims.isError && <span data-testid="claims-load-error"> Could not load claims.</span>}
    </p>
  );
}

export const contributions = [
  PeopleHead.fill({ order: 10, Component: ClaimsHead }),
  PersonRow.fill({ order: 10, Component: PersonClaims }),
  SheetPage.fill({ order: 20, Component: ClaimedTotal }),
];
