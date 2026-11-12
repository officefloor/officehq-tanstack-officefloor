import { type ChangeEvent, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { type Person, peopleKey, usePeople } from '../people/people';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { claimDue, useRates } from '../rates/rates';
import { floorOf, useSettings } from '../settings/settings';
import { periodOf, usePeriods } from '../periods/periods';
import { useSheetToday } from '../sheet/sheet';
import { dueBy, lateOf, overdue } from '../overdue/overdue';
import { type Claim, type ClaimStatus, claimsKey, useClaims, useSelectedClaims } from './claims';

type Fields = { amount: string; date: string; note: string; corrects: string };
/** `review` for a steward approving (SETTLED) or rejecting (OPEN, with a reason) a PENDING claim. */
type StatusChange = { id: number; status: ClaimStatus; review?: boolean; reason?: string };
/** A steward moving a claim to another person (`to`, their id). */
type Reassign = { id: number; to: string };
const EMPTY: Fields = { amount: '', date: '', note: '', corrects: '' };

function ClaimsHead() {
  return <th>Claims</th>;
}

// A person's claims, newest first, each with its status and the buttons that move it on, and the
// form that adds one or edits the one whose editor is open.
function PersonClaims({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const settings = useSettings(sheet).data;
  const floor = floorOf(settings);
  const today = useSheetToday(sheet);
  const periods = usePeriods(sheet);
  const [fields, setFields] = useState<Fields>(EMPTY);
  const [editing, setEditing] = useState<number | undefined>();
  const [reasons, setReasons] = useState<Record<number, string>>({});
  const [targets, setTargets] = useState<Record<number, string>>({});
  const [deferDates, setDeferDates] = useState<Record<number, string>>({});
  const [splitAmounts, setSplitAmounts] = useState<Record<number, string>>({});
  const people = usePeople(sheet);
  const [selected, setSelected] = useSelectedClaims();
  const queryClient = useQueryClient();
  const base = `/api/sheets/${encodeURIComponent(sheet)}`;

  const save = useMutation({
    mutationFn: (f: Fields) =>
      editing !== undefined
        ? putJson<Claim>(`${base}/claims/${editing}`, { ...f, user })
        : postJson<Claim>(`${base}/people/${person.id}/claims`, { ...f, user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
      setFields(EMPTY);
      setEditing(undefined);
    },
  });

  const submit = useSubmitOnce(save);

  // The server refuses a change the claim's status does not allow; the error shows on that claim.
  const move = useMutation({
    mutationFn: (m: StatusChange) =>
      postJson<Claim>(`${base}/claims/${m.id}/status`, { status: m.status, user, review: m.review ?? false, reason: m.reason ?? '' }),
    onSuccess: async (_, m) => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
      if (m.review) setReasons((r) => ({ ...r, [m.id]: '' }));
    },
  });
  const moveTo = (id: number, status: ClaimStatus, review?: { reason: string }) => {
    move.reset();
    reassign.reset();
    resettle.reset();
    defer.reset();
    proof.reset();
    split.reset();
    move.mutate({ id, status, review: review !== undefined, reason: review?.reason });
  };

  // Moving a claim re-reads the people too: both people's figures follow it.
  const reassign = useMutation({
    mutationFn: (r: Reassign) => postJson<Claim>(`${base}/claims/${r.id}/reassign`, { to: r.to, user }),
    onSuccess: async (_, r) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
      ]);
      setTargets((t) => ({ ...t, [r.id]: '' }));
    },
  });
  const reassignTo = (id: number) => {
    move.reset();
    reassign.reset();
    resettle.reset();
    defer.reset();
    proof.reset();
    split.reset();
    reassign.mutate({ id, to: targets[id] ?? '' });
  };
  // A steward re-settling a SETTLED claim freezes it again at today's figure.
  const resettle = useMutation({
    mutationFn: (id: number) => postJson<Claim>(`${base}/claims/${id}/resettle`, { user }),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
      ]);
    },
  });
  const resettleClaim = (id: number) => {
    move.reset();
    reassign.reset();
    resettle.reset();
    defer.reset();
    proof.reset();
    split.reset();
    resettle.mutate(id);
  };
  // Deferring an OPEN claim until a date after today takes it out of every figure until then.
  const defer = useMutation({
    mutationFn: (d: { id: number; until: string }) =>
      postJson<Claim>(`${base}/claims/${d.id}/defer`, { until: d.until, user }),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
      ]);
    },
  });
  const deferClaim = (id: number) => {
    move.reset();
    reassign.reset();
    resettle.reset();
    defer.reset();
    proof.reset();
    split.reset();
    defer.mutate({ id, until: (deferDates[id] ?? '').trim() });
  };
  // A proof raises the claim's proofs count by one; not on a SETTLED or VOID claim.
  const proof = useMutation({
    mutationFn: (id: number) => postJson<Claim>(`${base}/claims/${id}/proof`, { user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    },
  });
  const addProof = (id: number) => {
    move.reset();
    reassign.reset();
    resettle.reset();
    defer.reset();
    proof.reset();
    split.reset();
    proof.mutate(id);
  };
  // Splitting an amount off an OPEN claim lists a new OPEN claim with it; both people's figures follow.
  const split = useMutation({
    mutationFn: (d: { id: number; amount: string }) =>
      postJson<Claim>(`${base}/claims/${d.id}/split`, { amount: d.amount, user }),
    onSuccess: async (_, d) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
      ]);
      setSplitAmounts((a) => ({ ...a, [d.id]: '' }));
    },
  });
  const splitClaim = (id: number) => {
    move.reset();
    reassign.reset();
    resettle.reset();
    defer.reset();
    proof.reset();
    split.reset();
    split.mutate({ id, amount: (splitAmounts[id] ?? '').trim() });
  };
  // The refusal of the last status change, move, re-settle, deferral, proof or split of the claim, as the server gave it; null when none.
  const refusal = (id: number) => {
    const error =
      move.isError && move.variables?.id === id
        ? move.error
        : reassign.isError && reassign.variables?.id === id
          ? reassign.error
          : resettle.isError && resettle.variables === id
            ? resettle.error
            : defer.isError && defer.variables?.id === id
              ? defer.error
              : proof.isError && proof.variables === id
                ? proof.error
                : split.isError && split.variables?.id === id
                  ? split.error
                  : null;
    if (error === null) return null;
    return error instanceof HttpError && error.reason ? `${error.reason}.` : 'The change was refused.';
  };

  const set = (key: keyof Fields) => (e: ChangeEvent<HTMLInputElement>) => {
    setFields({ ...fields, [key]: e.target.value });
    save.reset();
  };
  const open = (c: Claim) => {
    save.reset();
    move.reset();
    reassign.reset();
    resettle.reset();
    defer.reset();
    proof.reset();
    split.reset();
    setEditing(c.id);
    setFields({ amount: c.amount, date: c.date, note: c.note, corrects: c.corrects ?? '' });
  };
  const cancel = () => {
    save.reset();
    setEditing(undefined);
    setFields(EMPTY);
  };

  return (
    <td>
      <ul>
        {(claims.data ?? [])
          .filter((c) => c.personId === person.id)
          .map((c) => (
            <li key={c.id} data-testid={`claim-row-${c.id}`}>
              <input
                type="checkbox"
                data-testid="claim-select"
                aria-label="Select claim"
                checked={selected.includes(c.id)}
                onChange={(e) => setSelected(e.target.checked ? [...selected, c.id] : selected.filter((id) => id !== c.id))}
              />{' '}
              <span data-testid="claim-number">{c.number ?? ''}</span>{' '}
              <span data-testid="claim-amount">{c.amount}</span>{' '}
              <span data-testid="claim-date">{c.date}</span>{' '}
              <span data-testid="claim-note">{c.note}</span>{' '}
              <span data-testid="claim-corrects">{c.corrects ?? ''}</span>{' '}
              <span data-testid="claim-period">{periodOf(periods.data, c.date)?.name ?? ''}</span>{' '}
              <span data-testid="claim-due">{claimDue(c, rates.data, person.grades, floor, lateOf(settings, today))}</span>{' '}
              <span data-testid="claim-due-by">{dueBy(c)}</span>{' '}
              <span data-testid="claim-overdue">{overdue(c, today) ? 'yes' : 'no'}</span>{' '}
              <span data-testid="claim-status">{c.status}</span>{' '}
              <span data-testid="claim-opened-by">{c.openedBy ?? ''}</span>{' '}
              <span data-testid="claim-stated">{c.stated ? 'yes' : 'no'}</span>{' '}
              <span data-testid="claim-deferred">{c.deferred ? 'yes' : 'no'}</span>{' '}
              <span data-testid="claim-deferred-until">{c.deferredUntil ?? ''}</span>{' '}
              <span data-testid="claim-split-from">{c.splitFrom ?? ''}</span>{' '}
              <button type="button" data-testid="claim-open" disabled={move.isPending} onClick={() => moveTo(c.id, 'OPEN')}>
                Open
              </button>{' '}
              <button type="button" data-testid="claim-settle" disabled={move.isPending} onClick={() => moveTo(c.id, 'SETTLED')}>
                Settle
              </button>{' '}
              <button type="button" data-testid="claim-void" disabled={move.isPending} onClick={() => moveTo(c.id, 'VOID')}>
                Void
              </button>{' '}
              <button
                type="button"
                data-testid="claim-resettle"
                disabled={resettle.isPending}
                onClick={() => resettleClaim(c.id)}
              >
                Re-settle
              </button>{' '}
              <button
                type="button"
                data-testid="claim-approve"
                disabled={move.isPending}
                onClick={() => moveTo(c.id, 'SETTLED', { reason: '' })}
              >
                Approve
              </button>{' '}
              <input
                data-testid="claim-reject-reason"
                placeholder="Reason"
                aria-label="Reject reason"
                value={reasons[c.id] ?? ''}
                onChange={(e) => {
                  setReasons({ ...reasons, [c.id]: e.target.value });
                  if (move.variables?.id === c.id) move.reset();
                }}
              />{' '}
              <button
                type="button"
                data-testid="claim-reject"
                disabled={move.isPending}
                onClick={() => moveTo(c.id, 'OPEN', { reason: reasons[c.id] ?? '' })}
              >
                Reject
              </button>{' '}
              <select
                data-testid="claim-reassign-to"
                aria-label="Move to"
                value={targets[c.id] ?? ''}
                onChange={(e) => {
                  setTargets({ ...targets, [c.id]: e.target.value });
                  if (reassign.variables?.id === c.id) reassign.reset();
                }}
              >
                <option value="">Move to…</option>
                {(people.data ?? [])
                  .filter((p) => p.id !== person.id)
                  .map((p) => (
                    <option key={p.id} value={String(p.id)}>
                      {p.name}
                    </option>
                  ))}
              </select>{' '}
              <button
                type="button"
                data-testid="claim-reassign"
                disabled={reassign.isPending}
                onClick={() => reassignTo(c.id)}
              >
                Move
              </button>{' '}
              <input
                data-testid="claim-defer-date"
                type="date"
                aria-label="Defer until"
                value={deferDates[c.id] ?? ''}
                onChange={(e) => {
                  setDeferDates({ ...deferDates, [c.id]: e.target.value });
                  if (defer.variables?.id === c.id) defer.reset();
                }}
              />{' '}
              <button type="button" data-testid="claim-defer" disabled={defer.isPending} onClick={() => deferClaim(c.id)}>
                Defer
              </button>{' '}
              <span data-testid="claim-proofs">{c.proofs ?? 0}</span>{' '}
              <button type="button" data-testid="claim-proof" disabled={proof.isPending} onClick={() => addProof(c.id)}>
                Add proof
              </button>{' '}
              <input
                data-testid="claim-split-amount"
                placeholder="Split off"
                aria-label="Amount to split off"
                inputMode="decimal"
                value={splitAmounts[c.id] ?? ''}
                onChange={(e) => {
                  setSplitAmounts({ ...splitAmounts, [c.id]: e.target.value });
                  if (split.variables?.id === c.id) split.reset();
                }}
              />{' '}
              <button type="button" data-testid="claim-split" disabled={split.isPending} onClick={() => splitClaim(c.id)}>
                Split
              </button>{' '}
              <button type="button" data-testid="claim-edit" onClick={() => open(c)}>
                Edit
              </button>
              {editing === c.id && (
                <button type="button" data-testid="claim-cancel" onClick={cancel}>
                  Cancel
                </button>
              )}
              {refusal(c.id) !== null && (
                <span data-testid="claim-error" role="alert">
                  {' '}
                  {refusal(c.id)}
                </span>
              )}
              <ol data-testid="claim-log">
                {(c.log ?? []).map((line, i) => (
                  <li key={i} data-testid="claim-log-line">
                    {line}
                  </li>
                ))}
              </ol>
            </li>
          ))}
      </ul>
      <form
        data-testid="claim-form"
        onSubmit={(e) => {
          e.preventDefault();
          submit(fields);
        }}
      >
        <input data-testid="claim-form-amount" placeholder="Amount" inputMode="decimal" value={fields.amount} onChange={set('amount')} />
        <input data-testid="claim-form-date" placeholder="Date (yyyy-mm-dd)" value={fields.date} onChange={set('date')} />
        <input data-testid="claim-form-note" placeholder="Note" value={fields.note} onChange={set('note')} />
        <input
          data-testid="claim-form-corrects"
          placeholder="Corrects claim no."
          inputMode="numeric"
          value={fields.corrects}
          onChange={set('corrects')}
        />
        <button type="submit" data-testid="claim-form-submit" disabled={save.isPending}>
          {editing !== undefined ? 'Save claim' : 'Add claim'}
        </button>
        {save.isError && (
          <span data-testid="claim-form-error" role="alert">
            {' '}
            {save.error instanceof HttpError && save.error.reason ? `${save.error.reason}.` : 'The claim was refused.'}
          </span>
        )}
      </form>
    </td>
  );
}

export const contributions = [
  PersonColumnHeads.fill({ order: 20, Component: ClaimsHead }),
  PersonColumns.fill({ order: 20, Component: PersonClaims }),
];
