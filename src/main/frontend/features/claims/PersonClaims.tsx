import { useEffect, useRef, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { HttpError } from '../../api/http';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { listPeriods, periodOf, periodsKey } from '../periods/api';
import { claimDue, showDue, type GradeOn, type Late, type Rate } from '../rates/api';
import { useNoteThreshold, useProofThreshold, type DatedOn } from '../settings/api';
import { addClaim, addProof, claimsKey, deferClaim, redateClaim, remindClaim, dueBy, isOverdue, lockClaim, moveClaim, reassignClaim, showTags, splitClaim, unlockClaim, updateClaim, type Claim, type ClaimAction, type ClaimInput } from './api';

const EMPTY: ClaimInput = { amount: '', date: '', note: '', corrects: '', ref: '', tags: '' };

const LABELS: Record<ClaimAction, string> = { open: 'Open', settle: 'Settle', void: 'Void', approve: 'Approve', reject: 'Reject', resettle: 'Re-settle' };
const DONE: Record<ClaimAction, string> = { open: 'opened', settle: 'settled', void: 'voided', approve: 'approved', reject: 'rejected', resettle: 're-settled' };

/** A person's claims: a form that adds a claim or edits one, and the claims newest first, each
 *  with its status, its due (fixed when settled, else at the rate in force on the claim's date of the
 *  person's grade in force then (`gradeOn`), or bound by the person's last statement; none when
 *  there is no such rate; an overdue one gains its late charge on top, see `charges`; then at least the sheet's `floor`), its due-by (fixed when it was added) and whether it is
 *  overdue (OPEN and due-by before the sheet's `today`), whether
 *  it is stated,
 *  the period its due-by falls in (none when no period contains it), its status log (oldest first), who opened it, and buttons that
 *  change its status (the user who opened a claim cannot settle it; a clerk settling one whose due is
 *  above the sheet's clerk limit makes it PENDING, which a steward other than its opener approves,
 *  or any steward rejects with a reason typed beside it), a checkbox that selects it for a bulk action, and for a correction (a negative amount, no floor and no late
 *  charge) the number of the SETTLED claim it corrects. Only a DRAFT can be edited:
 *  editing any other claim, or one due by a day in a closed period, is refused with an error on the claim. Opening a
 *  claim's editor locks it for other users until it is saved or cancelled or the sheet's today changes:
 *  another user's edit button on it is refused likewise, and the editor of a claim the user holds is
 *  open again when the sheet is re-loaded. A role the permission matrix allows (by default a steward) can move a DRAFT, OPEN or
 *  PENDING claim to another person picked beside it (any other refused with an error on the claim), and
 *  re-settle a SETTLED one (anyone else, any other status or a claim in a closed period refused likewise).
 *  An OPEN or PENDING claim can be reminded with a button beside it, once per sheet's today (a second reminder
 *  that day, or any other claim, refused with an error on the claim); the last day it was reminded is shown.
 *  An OPEN claim can be deferred until a date typed beside it, after the sheet's today (any other claim, date,
 *  or a claim in a closed period refused with an error on the claim); whether it is deferred, and until when, is shown.
 *  Its proofs count is shown and raised by one with a button beside it (a SETTLED or VOID claim, or one in a
 *  closed period, refused with an error on the claim); one with no proofs whose due is above the sheet's proof
 *  threshold in force on its date cannot be settled or approved.
 *  A steward can re-date an OPEN or PENDING claim to a date typed beside it: its due-by and any bound figure
 *  stay, and so do its period and number (anyone else, any other status, a date after its person's retirement,
 *  or a claim in a closed period refused with an error on the claim).
 *  An OPEN claim can be split by an amount typed beside it (more than 0.00, less than its own): it keeps the
 *  rest and is unbound, and a new OPEN claim of that amount, opened by the user, shows its number as split-from
 *  (a PENDING, deferred or settled claim, one in a closed period, or a role that may not open a claim refused
 *  with an error on the claim).
 *  A DRAFT's amount can be changed in its row, by an amount typed beside its edit button: saved as the
 *  claim form saves it (its date, note and corrected claim kept), refused likewise with an error on the claim. */
export function PersonClaims({ sheetId, user, personId, claims, gradeOn, rates, floor, today, charges, selected, onSelect, people }: {
  sheetId: string; user: string; personId: number; claims: Claim[]; gradeOn: GradeOn; rates: Rate[] | undefined;
  floor: DatedOn; today: string; charges: Late;
  /** The claims selected for a bulk action, and how one is selected or unselected. */
  selected: ReadonlySet<number>; onSelect: (id: number, on: boolean) => void;
  /** Everyone on the sheet, the people a claim can be moved to (all but `personId`). */
  people: Array<{ id: number; name: string }>;
}) {
  const queryClient = useQueryClient();
  const periods = useQuery({ queryKey: periodsKey(sheetId), queryFn: () => listPeriods(sheetId) });
  const [form, setForm] = useState<ClaimInput>(EMPTY);
  const [editing, setEditing] = useState<number | null>(null);
  // The claim whose edit was refused (not a DRAFT, in a closed period, or locked by another user), and
  // why; cleared by the next action on any claim here.
  const [editRefused, setEditRefused] = useState<{ id: number; message: string } | null>(null);
  // The rejection reason typed for each claim.
  const [reasons, setReasons] = useState<Record<number, string>>({});

  const save = useMutation({
    mutationFn: (v: { id: number | null; input: ClaimInput }) =>
      v.id === null ? addClaim(sheetId, user, personId, v.input) : updateClaim(sheetId, user, v.id, v.input),
    onSuccess: () => {
      setForm(EMPTY);
      setEditing(null);
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });

  // Opening a claim's editor takes its lock; a refusal leaves the editor open (a save is refused too)
  // with the error on the claim, until it is cancelled.
  const locking = useMutation({
    mutationFn: (id: number) => lockClaim(sheetId, user, id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
  });
  const unlocking = useMutation({
    mutationFn: (id: number) => unlockClaim(sheetId, user, id),
    onSettled: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
  });

  // The claim of this person whose lock the user holds: its editor is open, also after a re-load,
  // and closes when the lock goes (the sheet's today changed).
  const held = claims.find((c) => c.lockedBy === user && c.status === 'DRAFT');
  const heldWas = useRef<number | null>(null);
  useEffect(() => {
    const was = heldWas.current;
    heldWas.current = held?.id ?? null;
    if (held && editing === null) {
      setForm({ amount: held.amount, date: held.date, note: held.note, corrects: held.corrects ?? '', ref: held.ref ?? '', tags: showTags(held.tags) });
      setEditing(held.id);
    } else if (!held && was !== null && editing === was && !locking.isPending) {
      setForm(EMPTY);
      setEditing(null);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [held?.id]);

  const submitOnce = useSubmitOnce();
  const actOnce = useSubmitOnce();

  // The claim whose last status change was refused; cleared by the next action on any claim here.
  const move = useMutation({
    mutationFn: (v: { id: number; action: ClaimAction }) =>
      moveClaim(sheetId, user, v.id, v.action, reasons[v.id] ?? ''),
    onSuccess: (_, v) => {
      if (v.action === 'reject') setReasons((r) => ({ ...r, [v.id]: '' }));
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });

  // The person picked for each claim to move to.
  const [targets, setTargets] = useState<Record<number, string>>({});
  const others = people.filter((p) => p.id !== personId);

  // The date typed for each claim to defer it until.
  const [deferDates, setDeferDates] = useState<Record<number, string>>({});

  const deferral = useMutation({
    mutationFn: (v: { id: number; until: string }) => deferClaim(sheetId, user, v.id, v.until),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
  });

  const defer = (id: number) => {
    const until = (deferDates[id] ?? '').trim();
    actOnce(() => {
      move.reset();
      reassign.reset();
      deferral.reset();
      reminder.reset();
      redating.reset();
      proof.reset();
      division.reset();
      inline.reset();
      setEditRefused(null);
      locking.reset();
      return deferral.mutateAsync({ id, until });
    });
  };

  const reminder = useMutation({
    mutationFn: (id: number) => remindClaim(sheetId, user, id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
  });

  const remind = (id: number) => {
    actOnce(() => {
      move.reset();
      reassign.reset();
      deferral.reset();
      reminder.reset();
      redating.reset();
      proof.reset();
      division.reset();
      inline.reset();
      setEditRefused(null);
      locking.reset();
      return reminder.mutateAsync(id);
    });
  };

  // The new date typed for each claim to re-date it to.
  const [redates, setRedates] = useState<Record<number, string>>({});

  const redating = useMutation({
    mutationFn: (v: { id: number; date: string }) => redateClaim(sheetId, user, v.id, v.date),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
  });

  const redate = (id: number) => {
    const date = (redates[id] ?? '').trim();
    actOnce(() => {
      move.reset();
      reassign.reset();
      deferral.reset();
      reminder.reset();
      redating.reset();
      proof.reset();
      division.reset();
      inline.reset();
      setEditRefused(null);
      locking.reset();
      return redating.mutateAsync({ id, date });
    });
  };

  // The amount typed for each claim to split off it.
  const [splitAmounts, setSplitAmounts] = useState<Record<number, string>>({});

  const division = useMutation({
    mutationFn: (v: { id: number; amount: string }) => splitClaim(sheetId, user, v.id, v.amount),
    onSuccess: (_, v) => {
      setSplitAmounts((a) => ({ ...a, [v.id]: '' }));
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });

  const split = (id: number) => {
    const amount = (splitAmounts[id] ?? '').trim();
    actOnce(() => {
      move.reset();
      reassign.reset();
      deferral.reset();
      reminder.reset();
      redating.reset();
      proof.reset();
      division.reset();
      inline.reset();
      setEditRefused(null);
      locking.reset();
      return division.mutateAsync({ id, amount });
    });
  };

  // The amount typed for each claim to change it to in its row.
  const [inlineAmounts, setInlineAmounts] = useState<Record<number, string>>({});

  const inline = useMutation({
    mutationFn: (v: { id: number; input: ClaimInput }) => updateClaim(sheetId, user, v.id, v.input),
    onSuccess: (_, v) => {
      setInlineAmounts((a) => ({ ...a, [v.id]: '' }));
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });

  const saveInline = (c: Claim) => {
    const input = { amount: (inlineAmounts[c.id] ?? '').trim(), date: c.date, note: c.note, corrects: c.corrects ?? '', ref: c.ref ?? '', tags: showTags(c.tags) };
    actOnce(() => {
      move.reset();
      reassign.reset();
      deferral.reset();
      reminder.reset();
      redating.reset();
      proof.reset();
      division.reset();
      inline.reset();
      setEditRefused(null);
      locking.reset();
      return inline.mutateAsync({ id: c.id, input });
    });
  };

  const proof = useMutation({
    mutationFn: (id: number) => addProof(sheetId, user, id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
  });

  const prove = (id: number) => {
    actOnce(() => {
      move.reset();
      reassign.reset();
      deferral.reset();
      reminder.reset();
      redating.reset();
      proof.reset();
      division.reset();
      inline.reset();
      setEditRefused(null);
      locking.reset();
      return proof.mutateAsync(id);
    });
  };

  const proofThreshold = useProofThreshold(sheetId);
  // Whether a claim needs a proof before it is settled or approved: none yet, and its due above the threshold.
  const needsProof = (c: Claim) => {
    const threshold = proofThreshold(c.date);
    const due = claimDue(c, rates, gradeOn, floor, charges);
    return c.proofs === 0 && threshold !== null && due !== null && Number(due) > Number(threshold);
  };

  const noteThreshold = useNoteThreshold(sheetId);
  // Whether a claim needs a note before it is opened: an empty one, and its due above the threshold.
  const needsNote = (c: Claim) => {
    const threshold = noteThreshold(c.date);
    const due = claimDue(c, rates, gradeOn, floor, charges);
    return (c.note ?? '').trim() === '' && threshold !== null && due !== null && Number(due) > Number(threshold);
  };

  const reassign = useMutation({
    mutationFn: (v: { id: number; to: number }) => reassignClaim(sheetId, user, v.id, v.to),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
  });

  const moveTo = (id: number) => {
    const to = Number(targets[id] ?? others[0]?.id ?? NaN);
    actOnce(() => {
      move.reset();
      reassign.reset();
      deferral.reset();
      reminder.reset();
      redating.reset();
      proof.reset();
      division.reset();
      inline.reset();
      setEditRefused(null);
      locking.reset();
      return reassign.mutateAsync({ id, to });
    });
  };

  const act = (id: number, action: ClaimAction) => {
    actOnce(() => {
      move.reset();
      reassign.reset();
      deferral.reset();
      reminder.reset();
      redating.reset();
      proof.reset();
      division.reset();
      inline.reset();
      setEditRefused(null);
      locking.reset();
      return move.mutateAsync({ id, action });
    });
  };

  const change = (patch: Partial<ClaimInput>) => {
    setForm((f) => ({ ...f, ...patch }));
    save.reset();
  };

  const open = (c: Claim) => {
    move.reset();
    reassign.reset();
    deferral.reset();
    reminder.reset();
    redating.reset();
    proof.reset();
    division.reset();
    inline.reset();
    locking.reset();
    if (c.status !== 'DRAFT') {
      setEditRefused({ id: c.id, message: `A ${c.status} claim cannot be edited. To change it, void it and add a new one.` });
      return;
    }
    if (periodOf(periods.data, c.dueBy)?.closed) {
      setEditRefused({ id: c.id, message: 'A claim due by a day inside a closed period cannot be edited.' });
      return;
    }
    if (c.lockedBy !== null && c.lockedBy !== undefined && c.lockedBy !== user) {
      setEditRefused({ id: c.id, message: `${c.lockedBy} is editing this claim; it cannot be edited until they save or cancel.` });
      return;
    }
    setEditRefused(null);
    setForm({ amount: c.amount, date: c.date, note: c.note, corrects: c.corrects ?? '', ref: c.ref ?? '', tags: showTags(c.tags) });
    setEditing(c.id);
    save.reset();
    locking.mutate(c.id);
  };

  const cancel = () => {
    const id = editing;
    setForm(EMPTY);
    setEditing(null);
    save.reset();
    locking.reset();
    setEditRefused(null);
    if (id !== null) unlocking.mutate(id);
  };

  return (
    <div>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          const v = { id: editing, input: form };
          submitOnce(() => {
            save.reset();
            return save.mutateAsync(v);
          });
        }}
      >
        <input data-testid="claim-form-amount" placeholder="Amount" value={form.amount}
          onChange={(e) => change({ amount: e.target.value })} />{' '}
        <input type="date" data-testid="claim-form-date" value={form.date}
          onChange={(e) => change({ date: e.target.value })} />{' '}
        <input data-testid="claim-form-note" placeholder="Note" value={form.note}
          onChange={(e) => change({ note: e.target.value })} />{' '}
        <input data-testid="claim-form-corrects" placeholder="Corrects no." value={form.corrects}
          onChange={(e) => change({ corrects: e.target.value })} />{' '}
        <input data-testid="claim-form-ref" placeholder="Reference" value={form.ref}
          onChange={(e) => change({ ref: e.target.value })} />{' '}
        <input data-testid="claim-form-tags" placeholder="Tags, comma separated" value={form.tags}
          onChange={(e) => change({ tags: e.target.value })} />{' '}
        <button type="submit" data-testid="claim-form-submit" disabled={save.isPending || locking.isPending}>
          {editing === null ? 'Add claim' : 'Save claim'}
        </button>
        {save.isError && (
          <span data-testid="claim-form-error">{save.error instanceof HttpError && save.error.status === 423
            ? 'A claim due by a day inside a closed period cannot be added or edited.'
            : save.error instanceof HttpError && save.error.status === 409
            ? 'Only a DRAFT claim can be edited.'
            : save.error instanceof HttpError && save.error.status === 412
            ? 'Another user is editing this claim; it cannot be saved until they save or cancel.'
            : save.error instanceof HttpError && save.error.status === 403
            ? 'Your role may not change anything on this sheet.'
            : 'A claim needs an amount with up to two decimals; a negative one (a correction) must name, by number, a SETTLED claim of this person; a reference cannot be one already used on the sheet.'}</span>
        )}
      </form>
      <table>
        <tbody>
          {claims.map((c) => (
            <tr key={c.id} data-testid={`claim-row-${c.id}`}>
              <td>
                <input type="checkbox" data-testid="claim-select" checked={selected.has(c.id)}
                  onChange={(e) => onSelect(c.id, e.target.checked)} />
              </td>
              <td data-testid="claim-number">{c.number ?? ''}</td>
              <td data-testid="claim-amount">{Number(c.amount).toFixed(2)}</td>
              <td data-testid="claim-date">{c.date}</td>
              <td data-testid="claim-note">{c.note}</td>
              <td data-testid="claim-corrects">{c.corrects ?? ''}</td>
              <td data-testid="claim-ref">{c.ref ?? ''}</td>
              <td data-testid="claim-tags">{showTags(c.tags)}</td>
              <td data-testid="claim-due">{showDue(claimDue(c, rates, gradeOn, floor, charges))}</td>
              <td>
                Due by <span data-testid="claim-due-by">{dueBy(c)}</span>{' '}
                overdue <span data-testid="claim-overdue">{isOverdue(c, today, charges.lateFrom(c)) ? 'yes' : 'no'}</span>
              </td>
              <td data-testid="claim-status">{c.status}</td>
              <td data-testid="claim-opened-by">{c.openedBy ?? ''}</td>
              <td>
                Stated <span data-testid="claim-stated">{c.stated ? 'yes' : 'no'}</span>
              </td>
              <td>
                Deferred <span data-testid="claim-deferred">{c.deferred ? 'yes' : 'no'}</span>{' '}
                until <span data-testid="claim-deferred-until">{c.deferredUntil ?? ''}</span>
              </td>
              <td>
                Reminded <span data-testid="claim-reminded">{c.reminded ?? ''}</span>{' '}
                <button type="button" data-testid="claim-remind" disabled={reminder.isPending}
                  onClick={() => remind(c.id)}>
                  Remind
                </button>
              </td>
              <td>
                Proofs <span data-testid="claim-proofs">{c.proofs ?? 0}</span>{' '}
                <button type="button" data-testid="claim-proof" disabled={proof.isPending}
                  onClick={() => prove(c.id)}>
                  Add proof
                </button>
              </td>
              <td>
                Late entry <span data-testid="claim-late-entry">{c.lateEntry ? 'yes' : 'no'}</span>{' '}
                Split from <span data-testid="claim-split-from">{c.splitFrom ?? ''}</span>{' '}
                <input data-testid="claim-split-amount" placeholder="Split amount" value={splitAmounts[c.id] ?? ''}
                  onChange={(e) => {
                    setSplitAmounts((a) => ({ ...a, [c.id]: e.target.value }));
                    if (division.variables?.id === c.id) division.reset();
                  }} />{' '}
                <button type="button" data-testid="claim-split" disabled={division.isPending}
                  onClick={() => split(c.id)}>
                  Split
                </button>
              </td>
              <td data-testid="claim-period">{periodOf(periods.data, c.dueBy)?.name ?? ''}</td>
              <td>
                {(c.log ?? []).map((line, i) => <div key={i} data-testid="claim-log-line">{line}</div>)}
              </td>
              <td>
                {(['open', 'settle', 'void', 'approve', 'resettle'] as const).map((a) => (
                  <button key={a} type="button" data-testid={`claim-${a}`} disabled={move.isPending}
                    onClick={() => act(c.id, a)}>
                    {LABELS[a]}
                  </button>
                ))}{' '}
                <input data-testid="claim-reject-reason" placeholder="Reason" value={reasons[c.id] ?? ''}
                  onChange={(e) => {
                    setReasons((r) => ({ ...r, [c.id]: e.target.value }));
                    if (move.variables?.id === c.id) move.reset();
                  }} />{' '}
                <button type="button" data-testid="claim-reject" disabled={move.isPending}
                  onClick={() => act(c.id, 'reject')}>
                  {LABELS.reject}
                </button>{' '}
                <input type="date" data-testid="claim-defer-date" value={deferDates[c.id] ?? ''}
                  onChange={(e) => {
                    setDeferDates((d) => ({ ...d, [c.id]: e.target.value }));
                    if (deferral.variables?.id === c.id) deferral.reset();
                  }} />{' '}
                <button type="button" data-testid="claim-defer" disabled={deferral.isPending}
                  onClick={() => defer(c.id)}>
                  Defer
                </button>{' '}
                <input type="date" data-testid="claim-date-new" value={redates[c.id] ?? ''}
                  onChange={(e) => {
                    setRedates((d) => ({ ...d, [c.id]: e.target.value }));
                    if (redating.variables?.id === c.id) redating.reset();
                  }} />{' '}
                <button type="button" data-testid="claim-redate" disabled={redating.isPending}
                  onClick={() => redate(c.id)}>
                  Re-date
                </button>{' '}
                <select data-testid="claim-reassign-to" value={targets[c.id] ?? String(others[0]?.id ?? '')}
                  onChange={(e) => {
                    setTargets((t) => ({ ...t, [c.id]: e.target.value }));
                    if (reassign.variables?.id === c.id) reassign.reset();
                  }}>
                  {others.map((p) => <option key={p.id} value={String(p.id)}>{p.name}</option>)}
                </select>{' '}
                <button type="button" data-testid="claim-reassign" disabled={reassign.isPending || others.length === 0}
                  onClick={() => moveTo(c.id)}>
                  Reassign
                </button>{' '}
                <button type="button" data-testid="claim-edit" onClick={() => open(c)}>
                  Edit
                </button>{' '}
                <input data-testid="claim-inline-amount" placeholder="Amount" value={inlineAmounts[c.id] ?? ''}
                  onChange={(e) => {
                    setInlineAmounts((a) => ({ ...a, [c.id]: e.target.value }));
                    if (inline.variables?.id === c.id) inline.reset();
                  }} />{' '}
                <button type="button" data-testid="claim-inline-save" disabled={inline.isPending}
                  onClick={() => saveInline(c)}>
                  Save amount
                </button>
                {editing === c.id && (
                  <button type="button" data-testid="claim-cancel" onClick={cancel}>
                    Cancel
                  </button>
                )}
                {editRefused?.id === c.id && (
                  <span data-testid="claim-error">{editRefused.message}</span>
                )}
                {locking.isError && locking.variables === c.id && (
                  <span data-testid="claim-error">{locking.error instanceof HttpError && locking.error.status === 412
                    ? 'Another user is editing this claim; it cannot be edited until they save or cancel.'
                    : locking.error instanceof HttpError && locking.error.status === 423
                    ? 'A claim due by a day inside a closed period cannot be edited.'
                    : locking.error instanceof HttpError && locking.error.status === 409
                    ? `A ${c.status} claim cannot be edited. To change it, void it and add a new one.`
                    : 'Your role may not edit a claim.'}</span>
                )}
                {move.isError && move.variables?.id === c.id && (
                  <span data-testid="claim-error">{move.error instanceof HttpError && move.error.status === 423
                    ? 'A claim due by a day inside a closed period cannot be changed.'
                    : move.error instanceof HttpError && move.error.status === 400
                    ? 'A rejection needs a reason.'
                    : (move.variables.action === 'settle' || move.variables.action === 'approve') && needsProof(c)
                    ? 'A claim whose due is above the proof threshold needs a proof first.'
                    : move.variables.action === 'open' && c.status === 'DRAFT' && needsNote(c)
                    ? 'A claim whose due is above the note threshold needs a note before it is opened.'
                    : c.deferred && move.variables.action === 'settle'
                    ? `A deferred claim cannot be settled until ${c.deferredUntil}.`
                    : c.status === 'OPEN' && move.variables.action === 'settle'
                    ? 'The user who opened a claim cannot settle it, nor can a role the permissions do not allow to settle.'
                    : c.status === 'PENDING' && move.variables.action === 'approve'
                    ? 'Only a steward who did not open a claim can approve it.'
                    : c.status === 'PENDING' && move.variables.action === 'reject'
                    ? 'Only a steward can reject a claim.'
                    : c.status === 'SETTLED' && move.variables.action === 'resettle'
                    ? 'Only a steward can re-settle a claim.'
                    : `A ${c.status} claim cannot be ${DONE[move.variables.action]}, or your role may not ${move.variables.action} a claim.`}</span>
                )}
                {deferral.isError && deferral.variables?.id === c.id && (
                  <span data-testid="claim-error">{deferral.error instanceof HttpError && deferral.error.status === 423
                    ? 'A claim due by a day inside a closed period cannot be deferred.'
                    : deferral.error instanceof HttpError && deferral.error.status === 400
                    ? 'A claim can only be deferred to a date after the sheet\'s today.'
                    : `A ${c.status} claim cannot be deferred.`}</span>
                )}
                {reminder.isError && reminder.variables === c.id && (
                  <span data-testid="claim-error">{reminder.error instanceof HttpError && reminder.error.status === 409
                    ? (c.status === 'OPEN' || c.status === 'PENDING'
                      ? `The claim was already reminded on ${c.reminded ?? today}.`
                      : `A ${c.status} claim cannot be reminded.`)
                    : 'Your role may not remind a claim.'}</span>
                )}
                {redating.isError && redating.variables?.id === c.id && (
                  <span data-testid="claim-error">{redating.error instanceof HttpError && redating.error.status === 423
                    ? 'A claim due by a day inside a closed period cannot be re-dated.'
                    : redating.error instanceof HttpError && redating.error.status === 400
                    ? 'A claim needs a date on or before its person\'s retirement.'
                    : c.status === 'OPEN' || c.status === 'PENDING'
                    ? 'Only a steward can re-date a claim.'
                    : `A ${c.status} claim cannot be re-dated.`}</span>
                )}
                {division.isError && division.variables?.id === c.id && (
                  <span data-testid="claim-error">{division.error instanceof HttpError && division.error.status === 423
                    ? 'A claim due by a day inside a closed period cannot be split.'
                    : division.error instanceof HttpError && division.error.status === 400
                    ? `A split needs an amount more than 0.00 and less than ${Number(c.amount).toFixed(2)}.`
                    : c.status === 'OPEN' && !c.deferred
                    ? 'Your role may not open a claim, so it cannot split one.'
                    : `A ${c.deferred ? 'deferred' : c.status} claim cannot be split.`}</span>
                )}
                {proof.isError && proof.variables === c.id && (
                  <span data-testid="claim-error">{proof.error instanceof HttpError && proof.error.status === 423
                    ? 'A claim due by a day inside a closed period cannot gain a proof.'
                    : `A ${c.status} claim cannot gain a proof.`}</span>
                )}
                {inline.isError && inline.variables?.id === c.id && (
                  <span data-testid="claim-error">{inline.error instanceof HttpError && inline.error.status === 423
                    ? 'A claim due by a day inside a closed period cannot be edited.'
                    : inline.error instanceof HttpError && inline.error.status === 409
                    ? `A ${c.status} claim cannot be edited. To change it, void it and add a new one.`
                    : inline.error instanceof HttpError && inline.error.status === 412
                    ? 'Another user is editing this claim; it cannot be saved until they save or cancel.'
                    : inline.error instanceof HttpError && inline.error.status === 403
                    ? 'Your role may not change anything on this sheet.'
                    : 'A claim needs an amount with up to two decimals; a negative one (a correction) must name, by number, a SETTLED claim of this person.'}</span>
                )}
                {reassign.isError && reassign.variables?.id === c.id && (
                  <span data-testid="claim-error">{reassign.error instanceof HttpError && reassign.error.status === 423
                    ? 'A claim due by a day inside a closed period cannot be reassigned.'
                    : c.status === 'SETTLED' || c.status === 'VOID'
                    ? `A ${c.status} claim cannot be reassigned.`
                    : 'Only a role the permissions allow can reassign a claim, and only to another person.'}</span>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
