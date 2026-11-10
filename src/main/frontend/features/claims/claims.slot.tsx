import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson, putJson } from '../../api/http';
import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import { gradeOn, liveHolds, peopleKey, usePeople } from '../people/people';
import type { Person } from '../people/people';
import { useUser } from '../session/session';
import { AMOUNT, CLAIM_AMOUNT, CLAIM_NUMBER, claimsKey, isDeferred, sumAmounts, useClaims } from './claims';
import type { Claim } from './claims';
import { selectClaim, useSelectedClaims } from './selection';
import { claimDue, rateOn, useRates } from '../rates/rates';
import { periodOf, usePeriods } from '../periods/periods';
import { useFloor, useLate } from '../access/access';
import { useSheetToday } from '../sheet/sheet';
import { dueBy, isOverdue } from '../overdue/overdue';
import { useVoidReasons } from '../voidreasons/voidReasons';

// A clerk's settle above the clerk limit leaves the claim PENDING; a steward approves it or rejects
// it with a reason (typed in the row). A steward re-settles a SETTLED claim at its figure today.
type Action = 'open' | 'settle' | 'void' | 'approve' | 'reject' | 'resettle';
type Fields = { amount: string; date: string; note: string; corrects: string; ref: string; tags: string };
const EMPTY: Fields = { amount: '', date: '', note: '', corrects: '', ref: '', tags: '' };

const ACTIONS: { action: Action; label: string }[] = [
  { action: 'open', label: 'Open' },
  { action: 'settle', label: 'Settle' },
  { action: 'void', label: 'Void' },
  { action: 'approve', label: 'Approve' },
  { action: 'reject', label: 'Reject' },
  { action: 'resettle', label: 'Re-settle' },
];


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
  // The claim the editor is on now, for a lock answered after the user moved on.
  const editingNow = useRef<string | null>(null);
  editingNow.current = editing;
  // The claim whose lock this user's editor was given: until then its edit button stays.
  const [granted, setGranted] = useState<string | null>(null);
  const [error, setError] = useState('');
  // The claim whose last status change or edit was refused, and why: its error shows until its
  // next action.
  const [refused, setRefused] = useState<{ id: string; message: string } | null>(null);
  // The reason typed for rejecting each claim, by claim id.
  const [reasons, setReasons] = useState<Record<string, string>>({});
  // The void reason chosen for each claim, by claim id: needed while the sheet has any.
  const [voidReasons, setVoidReasons] = useState<Record<string, string>>({});
  const voidReasonList = (useVoidReasons(sheetId).data ?? []).map((r) => r.reason);
  // The person picked to move each claim to (a steward's move), by claim id.
  const [moveTo, setMoveTo] = useState<Record<string, string>>({});
  // The date typed to defer each claim until, by claim id.
  const [deferUntil, setDeferUntil] = useState<Record<string, string>>({});
  // The new date typed to re-date each claim to, by claim id.
  const [newDate, setNewDate] = useState<Record<string, string>>({});
  // The amount typed in each draft's own row, to save as its new amount, by claim id.
  const [inlineAmount, setInlineAmount] = useState<Record<string, string>>({});
  // The amount typed to split off each claim, by claim id.
  const [splitAmount, setSplitAmount] = useState<Record<string, string>>({});
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
    setGranted(null);
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
        setError('That claim could not be saved (is its reference already used on the sheet?).');
      }
    },
  });

  const move = useMutation({
    mutationFn: ({ id, action }: { id: string; action: Action }) =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheetId)}/claims/${encodeURIComponent(id)}/${action}`,
        action === 'reject'
          ? { reason: (reasons[id] ?? '').trim() }
          : action === 'void'
            ? { reason: voidReasons[id] ?? '' }
            : {},
        user,
      ),
    onSuccess: (_c, { id, action }) => {
      if (action === 'reject') setReasons((r) => ({ ...r, [id]: '' }));
      if (action === 'void') setVoidReasons((r) => ({ ...r, [id]: '' }));
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

  // An OPEN claim is deferred until a date after the sheet's today.
  const defer = useMutation({
    mutationFn: ({ id, until }: { id: string; until: string }) =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheetId)}/claims/${encodeURIComponent(id)}/defer`,
        { until },
        user,
      ),
    onSuccess: (_c, { id }) => {
      setDeferUntil((d) => ({ ...d, [id]: '' }));
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (_e, { id }) =>
      setRefused({ id, message: 'Only an OPEN claim can be deferred, to a date after today.' }),
  });

  // A steward re-dates an OPEN or PENDING claim; its due-by and any stated figure are kept.
  const redate = useMutation({
    mutationFn: ({ id, date }: { id: string; date: string }) =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheetId)}/claims/${encodeURIComponent(id)}/redate`,
        { date },
        user,
      ),
    onSuccess: (_c, { id }) => {
      setNewDate((d) => ({ ...d, [id]: '' }));
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (_e, { id }) =>
      setRefused({
        id,
        message: 'That claim cannot be re-dated: a steward re-dates an open claim, not due in a closed period and not after retirement.',
      }),
  });

  const redateClaim = (id: string) => {
    setRefused(null);
    const date = (newDate[id] ?? '').trim();
    if (date === '') {
      setRefused({ id, message: 'Choose the new date for the claim.' });
      return;
    }
    redate.mutate({ id, date });
  };

  // An amount is split off an OPEN claim into a new OPEN claim, listed newest.
  const split = useMutation({
    mutationFn: ({ id, amount }: { id: string; amount: string }) =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheetId)}/claims/${encodeURIComponent(id)}/split`,
        { amount },
        user,
      ),
    onSuccess: (_c, { id }) => {
      setSplitAmount((a) => ({ ...a, [id]: '' }));
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (_e, { id }) =>
      setRefused({ id, message: 'Only an OPEN claim can be split, by an amount less than its own.' }),
  });

  const splitClaim = (id: string) => {
    setRefused(null);
    const amount = (splitAmount[id] ?? '').trim();
    if (!AMOUNT.test(amount)) {
      setRefused({ id, message: 'A split needs an amount with up to two decimals.' });
      return;
    }
    split.mutate({ id, amount });
  };

  // A draft's amount changed in its row: the claim editor's save with only the amount changed, so
  // the same rules (editable draft, not locked by another, no closed period, no viewer) hold.
  const inline = useMutation({
    mutationFn: ({ c, amount }: { c: Claim; amount: string }) =>
      putJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheetId)}/claims/${encodeURIComponent(c.id)}`,
        { amount, date: c.date, note: c.note, corrects: c.corrects, ref: c.ref, tags: c.tags.join(',') },
        user,
      ),
    onSuccess: (_r, { c }) => {
      setInlineAmount((a) => ({ ...a, [c.id]: '' }));
      // A saved edit releases this user's lock: an editor this user had open on it is closed.
      if (editingNow.current === c.id) reset();
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (_e, { c }) =>
      setRefused({ id: c.id, message: 'That claim cannot be edited now: it is not an editable draft, or someone else is editing it.' }),
  });

  const saveInline = (c: Claim) => {
    setRefused(null);
    const amount = (inlineAmount[c.id] ?? '').trim();
    if (!CLAIM_AMOUNT.test(amount)) {
      setRefused({ id: c.id, message: 'An amount is a number with up to two decimals.' });
      return;
    }
    if (amount.startsWith('-') && !CLAIM_NUMBER.test(c.corrects.trim())) {
      setRefused({ id: c.id, message: 'A negative amount is a correction of a settled claim.' });
      return;
    }
    inline.mutate({ c, amount });
  };

  // A proof is added to a claim that is not SETTLED or VOID.
  const proof = useMutation({
    mutationFn: (id: string) =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheetId)}/claims/${encodeURIComponent(id)}/proof`,
        {},
        user,
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
    onError: (_e, id) => setRefused({ id, message: 'A proof cannot be added to that claim.' }),
  });

  // An OPEN or PENDING claim is reminded, once per sheet's today.
  const remind = useMutation({
    mutationFn: (id: string) =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheetId)}/claims/${encodeURIComponent(id)}/remind`,
        {},
        user,
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
    onError: (_e, id) =>
      setRefused({ id, message: 'Only an OPEN or PENDING claim can be reminded, once a day.' }),
  });

  const remindClaim = (id: string) => {
    setRefused(null);
    remind.mutate(id);
  };

  const addProof = (id: string) => {
    setRefused(null);
    proof.mutate(id);
  };

  const deferClaim = (id: string) => {
    setRefused(null);
    const until = (deferUntil[id] ?? '').trim();
    if (until === '') {
      setRefused({ id, message: 'Choose the date to defer the claim until.' });
      return;
    }
    defer.mutate({ id, until });
  };

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
    if (action === 'void' && voidReasonList.length > 0 && !voidReasonList.includes(voidReasons[id] ?? '')) {
      setRefused({ id, message: 'A void needs a reason from the sheet\'s list.' });
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
    once((done) => save.mutate({ id: editing, body: { ...fields, amount, corrects, ref: fields.ref.trim() } }, { onSettled: done }));
  };

  const change = (next: Partial<Fields>) => {
    setFields((f) => ({ ...f, ...next }));
    setError('');
    setRefused((r) => (r !== null && r.id === editing ? null : r));
  };

  // The edit button locks the claim for this user until its editor is saved or cancelled (or the
  // sheet's today changes). A claim that cannot be edited (not a draft, due by a date in a closed period), one
  // another user is editing, or any edit of a viewer's, is refused on the claim and takes no lock.
  const lock = useMutation({
    mutationFn: (c: Claim) =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheetId)}/claims/${encodeURIComponent(c.id)}/lock`,
        {},
        user,
      ),
    onSuccess: (_r, c) => {
      if (editingNow.current !== c.id) return;
      setGranted(c.id);
      setFields({ amount: c.amount, date: c.date, note: c.note, corrects: c.corrects, ref: c.ref, tags: c.tags.join(', ') });
      setError('');
    },
    onError: (_e, c) =>
      setRefused({ id: c.id, message: 'That claim cannot be edited now: it is not an editable draft, or someone else is editing it.' }),
  });

  const unlock = useMutation({
    mutationFn: (id: string) =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheetId)}/claims/${encodeURIComponent(id)}/unlock`,
        {},
        user,
      ),
  });

  // The form is the claim's editor from the press on, its values filled once the lock is taken; a
  // refused editor stays empty, and saving it is refused too (the lock is the server's to give).
  const open = (c: Claim) => {
    setRefused(null);
    setEditing(c.id);
    setGranted(null);
    setFields(EMPTY);
    setError('');
    lock.mutate(c);
  };

  // An editor this user still holds open (its lock) is open again when the sheet is loaded again.
  const restored = useRef(false);
  useEffect(() => {
    if (restored.current || !all.isFetchedAfterMount) return;
    restored.current = true;
    const mine = claims.find((c) => c.lockedBy !== '' && c.lockedBy === user);
    if (mine !== undefined && editing === null) {
      setEditing(mine.id);
      setGranted(mine.id);
      setFields({ amount: mine.amount, date: mine.date, note: mine.note, corrects: mine.corrects, ref: mine.ref, tags: mine.tags.join(', ') });
    }
  }, [all.isFetchedAfterMount, claims, user, editing]);

  const cancel = () => {
    if (editing !== null) unlock.mutate(editing);
    reset();
  };

  return (
    <>
      <td data-testid="person-claimed">{sumAmounts(claims, () => liveHolds(person, today), today)}</td>
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
          <input
            data-testid="claim-form-ref"
            placeholder="Reference"
            value={fields.ref}
            onChange={(e) => change({ ref: e.target.value })}
          />{' '}
          <input
            data-testid="claim-form-tags"
            placeholder="Tags, comma separated"
            value={fields.tags}
            onChange={(e) => change({ tags: e.target.value })}
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
              <span data-testid="claim-due">{claimDue(c, rateOn(rates, gradeOn(person.grades, c.date), c.date), floor, late, gradeOn(person.grades, c.date))}</span>{' '}
              <span data-testid="claim-status">{c.status}</span>{' '}
              <span data-testid="claim-corrects">{c.corrects}</span>{' '}
              <span data-testid="claim-ref">{c.ref}</span>{' '}
              <span data-testid="claim-tags">{c.tags.join(', ')}</span>{' '}
              <span data-testid="claim-opened-by">{c.openedBy}</span>{' '}
              <span data-testid="claim-stated">{c.stated ? 'yes' : 'no'}</span>{' '}
              <span data-testid="claim-period">{periodOf(periods, c.dueBy)?.name ?? ''}</span>{' '}
              <span data-testid="claim-due-by">{dueBy(c)}</span>{' '}
              <span data-testid="claim-overdue">{isOverdue(c, today, late, gradeOn(person.grades, c.date)) ? 'yes' : 'no'}</span>{' '}
              <span data-testid="claim-deferred">{isDeferred(c, today) ? 'yes' : 'no'}</span>{' '}
              <span data-testid="claim-deferred-until">{c.deferredUntil}</span>{' '}
              <span data-testid="claim-proofs">{c.proofs}</span>{' '}
              <span data-testid="claim-split-from">{c.splitFrom}</span>{' '}
              <span data-testid="claim-late-entry">{c.lateEntry ? 'yes' : 'no'}</span>{' '}
              <span data-testid="claim-reminded">{c.remindedOn}</span>{' '}
              <button type="button" data-testid="claim-remind" onClick={() => remindClaim(c.id)}>
                Remind
              </button>{' '}
              <button type="button" data-testid="claim-proof" onClick={() => addProof(c.id)}>
                Add proof
              </button>{' '}
              <input
                data-testid="claim-reject-reason"
                placeholder="Reject reason"
                value={reasons[c.id] ?? ''}
                onChange={(e) => {
                  setReasons((r) => ({ ...r, [c.id]: e.target.value }));
                  setRefused((r) => (r !== null && r.id === c.id ? null : r));
                }}
              />{' '}
              <select
                data-testid="claim-void-reason"
                aria-label="Void reason"
                value={voidReasons[c.id] ?? ''}
                onChange={(e) => {
                  setVoidReasons((r) => ({ ...r, [c.id]: e.target.value }));
                  setRefused((r) => (r !== null && r.id === c.id ? null : r));
                }}
              >
                <option value="">No void reason</option>
                {voidReasonList.map((r) => (
                  <option key={r} value={r}>
                    {r}
                  </option>
                ))}
              </select>{' '}
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
              <input
                type="date"
                data-testid="claim-defer-date"
                aria-label="Defer until"
                value={deferUntil[c.id] ?? ''}
                onChange={(e) => {
                  setDeferUntil((d) => ({ ...d, [c.id]: e.target.value }));
                  setRefused((r) => (r !== null && r.id === c.id ? null : r));
                }}
              />{' '}
              <button type="button" data-testid="claim-defer" onClick={() => deferClaim(c.id)}>
                Defer
              </button>{' '}
              <input
                type="date"
                data-testid="claim-date-new"
                aria-label="New date"
                value={newDate[c.id] ?? ''}
                onChange={(e) => {
                  setNewDate((d) => ({ ...d, [c.id]: e.target.value }));
                  setRefused((r) => (r !== null && r.id === c.id ? null : r));
                }}
              />{' '}
              <button type="button" data-testid="claim-redate" onClick={() => redateClaim(c.id)}>
                Re-date
              </button>{' '}
              <input
                data-testid="claim-split-amount"
                placeholder="Split amount"
                inputMode="decimal"
                value={splitAmount[c.id] ?? ''}
                onChange={(e) => {
                  setSplitAmount((a) => ({ ...a, [c.id]: e.target.value }));
                  setRefused((r) => (r !== null && r.id === c.id ? null : r));
                }}
              />{' '}
              <button type="button" data-testid="claim-split" onClick={() => splitClaim(c.id)}>
                Split
              </button>{' '}
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
              <input
                data-testid="claim-inline-amount"
                placeholder="Amount"
                inputMode="decimal"
                value={inlineAmount[c.id] ?? ''}
                onChange={(e) => {
                  setInlineAmount((a) => ({ ...a, [c.id]: e.target.value }));
                  setRefused((r) => (r !== null && r.id === c.id ? null : r));
                }}
              />{' '}
              <button type="button" data-testid="claim-inline-save" onClick={() => saveInline(c)}>
                Save amount
              </button>{' '}
              {editing === c.id && (
                <button type="button" data-testid="claim-cancel" onClick={cancel}>
                  Cancel
                </button>
              )}
              {granted !== c.id && (
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
