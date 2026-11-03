import { useEffect, useState, type ChangeEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSingleFlight } from '../../query/useSingleFlight';
import { PERSON_ROW_SPAN, PersonCells, PersonColumnHead, PersonDetail } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheetBody';
import { gradeOn, holdsById, usePeople } from '../people/usePeople';
import { periodOf, usePeriods } from '../periods/usePeriods';
import { rateFor, useRates } from '../rates/useRates';
import { claimDueNow, isOverdue } from '../overdue/useOverdue';
import { useSignedInUser } from '../session/useSession';
import { useSettings } from '../settings/useSettings';
import { useSheetToday } from '../today/useSheetToday';
import { claimed, claimsKey, isDeferred, useClaims, type Claim } from './useClaims';
import { useClaimSelection } from './useClaimSelection';

/** A person's claimed: the sum of their counted (OPEN, PENDING or SETTLED; an open one dated within a hold only when a statement binds it; not a deferred one) claims' amounts. */
function PersonClaimed({ sheetId, personId }: { sheetId: string; personId: number }) {
  const claims = useClaims(sheetId);
  const people = usePeople(sheetId);
  const today = useSheetToday(sheetId).data?.today;
  return (
    <td data-testid="person-claimed">
      {claimed((claims.data ?? []).filter((c) => c.personId === personId), holdsById(people.data, today), today)}
    </td>
  );
}

type ClaimAction = 'open' | 'settle' | 'approve' | 'reject' | 'void' | 'resettle' | 'defer' | 'proof' | 'remind' | 'split' | 'redate';

/** A claim's status changes; the server refuses one its status does not allow, a settle by the user
 *  who opened the claim, and an approve or reject by anyone but a steward (an approve also by the
 *  user who opened the claim), a re-settle by anyone but a steward or of a claim not SETTLED, and a void of a SETTLED claim by anyone but a steward, more than 7 days after its settlement or in a closed period. A reject takes the reason typed on the claim's row.
 *  A settle or approve of a claim whose due is above the proof threshold in force on its date is refused while it has no proof. */
const ACTIONS: [Exclude<ClaimAction, 'defer' | 'proof' | 'remind' | 'split' | 'redate'>, string][] = [
  ['open', 'Open'],
  ['settle', 'Settle'],
  ['approve', 'Approve'],
  ['reject', 'Reject'],
  ['void', 'Void'],
  ['resettle', 'Re-settle'],
];

/** A person's claims, newest first, under their row, with a form that adds a claim, or edits one
 *  when its editor is open. A blank date takes the sheet's today. A negative amount is a correction:
 *  the form names, by number, the settled claim of the same person it corrects. */
function PersonClaims({ sheetId, personId }: { sheetId: string; personId: number }) {
  const claims = useClaims(sheetId);
  const people = usePeople(sheetId);
  const rates = useRates(sheetId);
  const settings = useSettings(sheetId).data;
  const sheet = useSheetToday(sheetId).data;
  const today = sheet?.today;
  const periods = usePeriods(sheetId);
  const [user] = useSignedInUser();
  const queryClient = useQueryClient();
  const selection = useClaimSelection(sheetId);
  const [editing, setEditing] = useState<number | null>(null);
  const [refused, setRefused] = useState<{ id: number; message: string } | null>(null);
  const [amount, setAmount] = useState('');
  const [date, setDate] = useState('');
  const [note, setNote] = useState('');
  const [corrects, setCorrects] = useState('');
  const [reasons, setReasons] = useState<Record<number, string>>({});
  const [targets, setTargets] = useState<Record<number, string>>({});
  const [deferDates, setDeferDates] = useState<Record<number, string>>({});
  const [splitAmounts, setSplitAmounts] = useState<Record<number, string>>({});
  const [newDates, setNewDates] = useState<Record<number, string>>({});

  const clearForm = () => {
    setEditing(null);
    setAmount('');
    setDate('');
    setNote('');
    setCorrects('');
  };

  // Mark a claim's editor lock as held (or not) by this user in the cached list, ahead of the re-read.
  const markLock = (id: number, lockedBy: string | null) =>
    queryClient.setQueryData<Claim[]>(claimsKey(sheetId), (old) =>
      old?.map((c) => (c.id === id ? { ...c, lockedBy } : c)),
    );

  const fillForm = (c: Claim) => {
    setEditing(c.id);
    setAmount(c.amount);
    setDate(c.date);
    setNote(c.note);
    setCorrects(c.corrects === null ? '' : String(c.corrects));
  };

  const submitOnce = useSingleFlight();
  const save = useMutation({
    mutationFn: (f: { id: number | null; amount: string; date: string; note: string; corrects: string }) => {
      const sheet = `/api/sheets/${encodeURIComponent(sheetId)}`;
      const path = f.id === null ? `${sheet}/people/${personId}/claims` : `${sheet}/claims/${f.id}`;
      return postJson<Claim>(path, { amount: f.amount, date: f.date, note: f.note, corrects: f.corrects });
    },
    onSuccess: async (_, f) => {
      if (f.id !== null) markLock(f.id, null);
      clearForm();
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });

  // Opening a claim's editor locks the claim for every other user until it is saved or cancelled (or the
  // sheet's today changes); the server refuses a claim another user holds, and any it would not let be edited.
  const lock = useMutation({
    mutationFn: (l: { id: number; action: 'lock' | 'unlock' }) =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${l.id}/${l.action}`, {}),
    onSuccess: (_, l) => {
      if (l.action === 'lock') {
        queryClient.setQueryData<Claim[]>(claimsKey(sheetId), (old) =>
          old?.map((c) =>
            c.id === l.id ? { ...c, lockedBy: user } : c.personId === personId && c.lockedBy === user ? { ...c, lockedBy: null } : c,
          ),
        );
      }
    },
  });

  // A status change (open, settle, approve, reject, void) of one claim, or deferring an OPEN one until the
  // date typed on its row, or adding a proof to it, or reminding an OPEN or PENDING one (once per sheet-today), or splitting the amount typed on its row off an OPEN one
  // (a new OPEN claim listed under the person), or re-dating an OPEN or PENDING one to the date typed on its row;
  // a refusal shows on that claim's row.
  const action = useMutation({
    mutationFn: (a: { id: number; action: ClaimAction; reason: string; until: string; amount: string; date: string }) =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${a.id}/${a.action}`, { reason: a.reason, until: a.until, amount: a.amount, date: a.date }),
    onSuccess: (_, a) => {
      if (a.action === 'reject') setReasons((r) => ({ ...r, [a.id]: '' }));
      if (a.action === 'defer') setDeferDates((d) => ({ ...d, [a.id]: '' }));
      if (a.action === 'split') setSplitAmounts((m) => ({ ...m, [a.id]: '' }));
      if (a.action === 'redate') setNewDates((d) => ({ ...d, [a.id]: '' }));
    },
    onSettled: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });

  // A steward moves a DRAFT, OPEN or PENDING claim to the person chosen on its row; a refusal shows on that claim's row.
  const reassign = useMutation({
    mutationFn: (r: { id: number; personId: number | null }) =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/reassign`, { claimId: r.id, personId: r.personId }),
    onSuccess: (_, r) => setTargets((t) => ({ ...t, [r.id]: '' })),
    onSettled: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });

  const move = (id: number) => {
    setRefused(null);
    action.reset();
    reassign.reset();
    const target = targets[id] ?? '';
    reassign.mutate({ id, personId: target === '' ? null : Number(target) });
  };

  const act = (id: number, a: ClaimAction) => {
    setRefused(null);
    action.reset();
    reassign.reset();
    action.mutate({
      id,
      action: a,
      reason: a === 'reject' ? reasons[id] ?? '' : '',
      until: a === 'defer' ? (deferDates[id] ?? '').trim() : '',
      amount: a === 'split' ? (splitAmounts[id] ?? '').trim() : '',
      date: a === 'redate' ? (newDates[id] ?? '').trim() : '',
    });
  };

  // Only a DRAFT outside a closed period (the one containing its due-by), not locked by another user, can be edited, and not by a viewer: editing
  // any other claim is refused on its row, opens no editor and takes no lock (to change an open claim, void it and
  // add a new one). What this screen already knows is refused at once; the server refuses the rest, and the editor
  // then goes back to what it was.
  const openEditor = (c: Claim) => {
    save.reset();
    action.reset();
    reassign.reset();
    lock.reset();
    const role = settings?.roles.find((r) => r.user === user)?.role;
    const known =
      sheet?.archived ? 'the sheet is archived'
      : role === 'viewer' ? 'a viewer can change nothing'
      : c.status !== 'DRAFT' ? 'only a DRAFT claim can be edited'
      : periodOf(periods.data, c.dueBy)?.closed ? `the period ${periodOf(periods.data, c.dueBy)?.name} is closed`
      : c.lockedBy !== null && c.lockedBy !== user ? `the claim is being edited by ${c.lockedBy}`
      : null;
    if (known !== null) {
      setRefused({ id: c.id, message: known });
      return;
    }
    setRefused(null);
    const before = { editing, amount, date, note, corrects };
    fillForm(c);
    lock.mutate(
      { id: c.id, action: 'lock' },
      {
        onError: (e) => {
          setRefused({ id: c.id, message: e.message });
          setEditing(before.editing);
          setAmount(before.amount);
          setDate(before.date);
          setNote(before.note);
          setCorrects(before.corrects);
        },
      },
    );
  };

  const cancel = () => {
    save.reset();
    if (editing !== null) {
      markLock(editing, null);
      lock.mutate({ id: editing, action: 'unlock' });
    }
    clearForm();
  };

  // A claim of this person whose lock this user still holds (after the sheet is re-loaded) shows its editor open.
  const mine = (claims.data ?? []).filter((c) => c.personId === personId);
  const held = mine.find((c) => c.status === 'DRAFT' && c.lockedBy !== null && c.lockedBy === user);
  useEffect(() => {
    if (editing === null && held !== undefined && !lock.isPending) fillForm(held);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [held?.id, editing, lock.isPending]);

  const field = (set: (v: string) => void) => (e: ChangeEvent<HTMLInputElement>) => {
    set(e.target.value);
    save.reset();
  };

  // A claim's due: the one fixed when it was settled, else the greater of the floor in force on its date and its
  // figure (the rate figure its last statement bound it to, else its amount at the rate in force on its date for the
  // grade its person had on that date; 'n/a' when none applies) plus its late charge while it is overdue.
  const person = people.data?.find((p) => p.id === personId);

  return (
    <tr>
      <td colSpan={PERSON_ROW_SPAN}>
        <form
          data-testid="claim-form"
          onSubmit={(e) => {
            e.preventDefault();
            setRefused(null);
            submitOnce(() => {
              save.reset();
              return save.mutateAsync({ id: editing, amount, date, note, corrects });
            });
          }}
        >
          <label>
            Amount <input data-testid="claim-form-amount" inputMode="decimal" value={amount} onChange={field(setAmount)} />
          </label>{' '}
          <label>
            Date{' '}
            <input
              type="text"
              placeholder="yyyy-mm-dd"
              data-testid="claim-form-date"
              value={date}
              onChange={field(setDate)}
            />
          </label>{' '}
          <label>
            Note <input data-testid="claim-form-note" value={note} onChange={field(setNote)} />
          </label>{' '}
          <label>
            Corrects{' '}
            <input
              data-testid="claim-form-corrects"
              inputMode="numeric"
              placeholder="claim no."
              value={corrects}
              onChange={field(setCorrects)}
            />
          </label>{' '}
          <button type="submit" data-testid="claim-form-submit" disabled={save.isPending || (lock.isPending && lock.variables?.action === 'lock')}>
            {editing === null ? 'Add claim' : 'Save claim'}
          </button>
          {save.isError && <p data-testid="claim-form-error">{save.error.message}</p>}
        </form>
        <table data-testid="claims-list">
          <tbody>
            {mine.map((c) => (
              <tr key={c.id} data-testid={`claim-row-${c.id}`}>
                <td>
                  <input
                    type="checkbox"
                    data-testid="claim-select"
                    checked={selection.ids.has(c.id)}
                    onChange={(e) => selection.toggle(c.id, e.target.checked)}
                  />
                </td>
                <td data-testid="claim-number">{c.number ?? ''}</td>
                <td data-testid="claim-amount">{c.amount}</td>
                <td data-testid="claim-date">{c.date}</td>
                <td data-testid="claim-period">{periodOf(periods.data, c.dueBy)?.name ?? ''}</td>
                <td data-testid="claim-note">{c.note}</td>
                <td data-testid="claim-due">
                  {claimDueNow(c, claims.data, rateFor(rates.data, gradeOn(person, c.date), c.date), settings, today, gradeOn(person, c.date))}
                </td>
                <td data-testid="claim-due-by">{c.dueBy}</td>
                <td data-testid="claim-overdue">
                  {today === undefined ? '' : isOverdue(c, today, settings, gradeOn(person, c.date)) ? 'yes' : 'no'}
                </td>
                <td data-testid="claim-corrects">{c.corrects ?? ''}</td>
                <td data-testid="claim-late-entry">{c.lateEntry ? 'yes' : 'no'}</td>
                <td data-testid="claim-stated">{c.stated ? 'yes' : 'no'}</td>
                <td data-testid="claim-status">{c.status}</td>
                <td data-testid="claim-opened-by">{c.openedBy ?? ''}</td>
                <td data-testid="claim-proofs">{c.proofs}</td>
                <td data-testid="claim-reminded">{c.reminded ?? ''}</td>
                <td data-testid="claim-split-from">{c.splitFrom ?? ''}</td>
                <td data-testid="claim-deferred">{isDeferred(c, today) ? 'yes' : 'no'}</td>
                <td data-testid="claim-deferred-until">{c.deferredUntil ?? ''}</td>
                <td data-testid="claim-log">
                  {c.log.map((line, i) => (
                    <div key={i} data-testid="claim-log-line">
                      {line}
                    </div>
                  ))}
                </td>
                <td>
                  <input
                    data-testid="claim-reject-reason"
                    placeholder="reject reason"
                    value={reasons[c.id] ?? ''}
                    onChange={(e) => {
                      const v = e.target.value;
                      setReasons((r) => ({ ...r, [c.id]: v }));
                    }}
                  />{' '}
                  {ACTIONS.map(([a, label]) => (
                    <button
                      key={a}
                      type="button"
                      data-testid={`claim-${a}`}
                      disabled={action.isPending}
                      onClick={() => act(c.id, a)}
                    >
                      {label}
                    </button>
                  ))}
                  <button
                    type="button"
                    data-testid="claim-proof"
                    disabled={action.isPending}
                    onClick={() => act(c.id, 'proof')}
                  >
                    Add proof
                  </button>{' '}
                  <button
                    type="button"
                    data-testid="claim-remind"
                    disabled={action.isPending}
                    onClick={() => act(c.id, 'remind')}
                  >
                    Remind
                  </button>{' '}
                  <input
                    type="text"
                    data-testid="claim-defer-date"
                    placeholder="defer until yyyy-mm-dd"
                    value={deferDates[c.id] ?? ''}
                    onChange={(e) => {
                      const v = e.target.value;
                      setDeferDates((d) => ({ ...d, [c.id]: v }));
                      if (action.variables?.id === c.id) action.reset();
                    }}
                  />{' '}
                  <button
                    type="button"
                    data-testid="claim-defer"
                    disabled={action.isPending}
                    onClick={() => act(c.id, 'defer')}
                  >
                    Defer
                  </button>{' '}
                  <input
                    data-testid="claim-split-amount"
                    inputMode="decimal"
                    placeholder="split off amount"
                    value={splitAmounts[c.id] ?? ''}
                    onChange={(e) => {
                      const v = e.target.value;
                      setSplitAmounts((m) => ({ ...m, [c.id]: v }));
                      if (action.variables?.id === c.id) action.reset();
                    }}
                  />{' '}
                  <button
                    type="button"
                    data-testid="claim-split"
                    disabled={action.isPending}
                    onClick={() => act(c.id, 'split')}
                  >
                    Split
                  </button>{' '}
                  <input
                    type="text"
                    data-testid="claim-date-new"
                    placeholder="new date yyyy-mm-dd"
                    value={newDates[c.id] ?? ''}
                    onChange={(e) => {
                      const v = e.target.value;
                      setNewDates((d) => ({ ...d, [c.id]: v }));
                      if (action.variables?.id === c.id) action.reset();
                    }}
                  />{' '}
                  <button
                    type="button"
                    data-testid="claim-redate"
                    disabled={action.isPending}
                    onClick={() => act(c.id, 'redate')}
                  >
                    Re-date
                  </button>{' '}
                  <select
                    data-testid="claim-reassign-to"
                    value={targets[c.id] ?? ''}
                    onChange={(e) => {
                      const v = e.target.value;
                      setTargets((t) => ({ ...t, [c.id]: v }));
                    }}
                  >
                    <option value="">move to…</option>
                    {(people.data ?? [])
                      .filter((p) => p.id !== personId)
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
                    onClick={() => move(c.id)}
                  >
                    Reassign
                  </button>
                  {reassign.isError && reassign.variables?.id === c.id && (
                    <span data-testid="claim-error">{reassign.error.message}</span>
                  )}
                  {action.isError && action.variables?.id === c.id && (
                    <span data-testid="claim-error">{action.error.message}</span>
                  )}
                  {refused?.id === c.id && <span data-testid="claim-error">{refused.message}</span>}
                </td>
                <td>
                  {editing === c.id ? (
                    <button type="button" data-testid="claim-cancel" onClick={cancel}>
                      Cancel
                    </button>
                  ) : (
                    <button type="button" data-testid="claim-edit" onClick={() => openEditor(c)}>
                      Edit
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {claims.isError && <p data-testid="claims-error">{claims.error.message}</p>}
      </td>
    </tr>
  );
}

/** Open the selected claims together: the server opens, in list order, each that can be opened and
 *  leaves the rest; the bar shows how many were opened and how many refused (gone while it runs). */
function BulkOpen({ sheetId }: { sheetId: string }) {
  const selection = useClaimSelection(sheetId);
  const queryClient = useQueryClient();
  const submitOnce = useSingleFlight();
  const bulk = useMutation({
    mutationFn: (claimIds: number[]) =>
      postJson<{ opened: number; refused: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/bulk-open`, { claimIds }),
    onSuccess: () => selection.clear(),
    onSettled: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });
  return (
    <p data-testid="bulk-open-bar">
      <button
        type="button"
        data-testid="bulk-open"
        disabled={bulk.isPending}
        onClick={() =>
          submitOnce(() => {
            bulk.reset();
            return bulk.mutateAsync([...selection.ids]);
          })
        }
      >
        Open selected ({selection.ids.size})
      </button>
      {bulk.isSuccess && (
        <>
          {' '}
          Opened <strong data-testid="bulk-opened">{bulk.data.opened}</strong>, refused{' '}
          <strong data-testid="bulk-refused">{bulk.data.refused}</strong>
        </>
      )}
      {bulk.isError && <span data-testid="bulk-open-error">{bulk.error.message}</span>}
    </p>
  );
}

/** Settle the selected claims together: the server settles, in list order and by the rules of a single
 *  settle, each that can be settled (one sent to PENDING counts) and leaves the rest; the bar shows how
 *  many were settled and how many refused (gone while it runs). */
function BulkSettle({ sheetId }: { sheetId: string }) {
  const selection = useClaimSelection(sheetId);
  const queryClient = useQueryClient();
  const submitOnce = useSingleFlight();
  const bulk = useMutation({
    mutationFn: (claimIds: number[]) =>
      postJson<{ settled: number; refused: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/bulk-settle`, { claimIds }),
    onSuccess: () => selection.clear(),
    onSettled: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });
  return (
    <p data-testid="bulk-settle-bar">
      <button
        type="button"
        data-testid="bulk-settle"
        disabled={bulk.isPending}
        onClick={() =>
          submitOnce(() => {
            bulk.reset();
            return bulk.mutateAsync([...selection.ids]);
          })
        }
      >
        Settle selected ({selection.ids.size})
      </button>
      {bulk.isSuccess && (
        <>
          {' '}
          Settled <strong data-testid="bulk-settled">{bulk.data.settled}</strong>, refused{' '}
          <strong data-testid="bulk-settle-refused">{bulk.data.refused}</strong>
        </>
      )}
      {bulk.isError && <span data-testid="bulk-settle-error">{bulk.error.message}</span>}
    </p>
  );
}

/** The sheet's claimed-total: everyone's claimed added up. */
function ClaimedTotal({ sheetId }: { sheetId: string }) {
  const claims = useClaims(sheetId);
  const people = usePeople(sheetId);
  const today = useSheetToday(sheetId).data?.today;
  return (
    <p>
      Claimed total: <strong data-testid="rollup-claimed-total">{claimed(claims.data ?? [], holdsById(people.data, today), today)}</strong>
    </p>
  );
}

export const contributions = [
  PersonColumnHead.fill({ order: 10, Component: () => <th>Claimed</th> }),
  PersonCells.fill({ order: 10, Component: PersonClaimed }),
  PersonDetail.fill({ order: 10, Component: PersonClaims }),
  SheetBody.fill({ order: 15, Component: BulkOpen }),
  SheetBody.fill({ order: 16, Component: BulkSettle }),
  SheetBody.fill({ order: 20, Component: ({ sheetId }) => <ClaimedTotal sheetId={sheetId} /> }),
];
