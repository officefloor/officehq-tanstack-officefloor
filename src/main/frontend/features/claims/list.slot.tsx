import { type ChangeEvent, useEffect, useState } from 'react';
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
import { useVoidReasons } from '../voidreasons/voidreasons';
import { useTemplates } from '../templates/templates';
import { type Claim, type ClaimStatus, claimsKey, useClaims, useEditingClaim, useSelectedClaims } from './claims';

type Fields = { amount: string; date: string; note: string; corrects: string; ref: string; tags: string };
/** `review` for a steward approving (SETTLED) or rejecting (OPEN, with a reason) a PENDING claim. */
type StatusChange = { id: number; status: ClaimStatus; review?: boolean; reason?: string };
/** A steward moving a claim to another person (`to`, their id). */
type Reassign = { id: number; to: string };
const EMPTY: Fields = { amount: '', date: '', note: '', corrects: '', ref: '', tags: '' };

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
  // The claim whose editor is open (and locked by this user), kept in the URL so it outlives a re-load
  // of the sheet; this person's form edits it when it is one of theirs.
  const [editingId, setEditing] = useEditingClaim();
  // The claim whose editor was refused here, without asking the server: it is not a DRAFT.
  const [uneditable, setUneditable] = useState<Claim | undefined>(undefined);
  const editingClaim = (claims.data ?? []).find((c) => c.id === editingId && c.personId === person.id);
  const editing = editingClaim?.id;
  const [reasons, setReasons] = useState<Record<number, string>>({});
  // The void reason chosen per claim, from the sheet's list; a void needs one while the list is not empty.
  const voidReasons = useVoidReasons(sheet).data ?? [];
  const [voidChoices, setVoidChoices] = useState<Record<number, string>>({});
  const [targets, setTargets] = useState<Record<number, string>>({});
  const [deferDates, setDeferDates] = useState<Record<number, string>>({});
  const [redates, setRedates] = useState<Record<number, string>>({});
  const [splitAmounts, setSplitAmounts] = useState<Record<number, string>>({});
  const [inlineAmounts, setInlineAmounts] = useState<Record<number, string>>({});
  const [commentTexts, setCommentTexts] = useState<Record<number, string>>({});
  const [lineFields, setLineFields] = useState<Record<number, { label: string; qty: string; unit: string }>>({});
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

  // A DRAFT claim made from a template: the server takes the template's amount and note as they are
  // now and dates it as the form dates a blank date; a refusal shows as the form's error.
  const templates = useTemplates(sheet).data ?? [];
  const [template, setTemplate] = useState('');
  const fromTemplate = useMutation({
    mutationFn: (name: string) => postJson<Claim>(`${base}/people/${person.id}/template-claims`, { template: name, user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    },
  });
  const submitTemplate = useSubmitOnce(fromTemplate);

  // Opening a claim's editor locks it for every other user; the server refuses a claim that cannot be
  // edited or that another user has locked, and the error shows on that claim.
  const lock = useMutation({
    mutationFn: (c: Claim) => postJson<Claim>(`${base}/claims/${c.id}/lock`, { user }),
    onSuccess: (_, c) => {
      if (editingId !== undefined && editingId !== c.id) release(editingId);
      setEditing(c.id);
      setFields({ amount: c.amount, date: c.date, note: c.note, corrects: c.corrects ?? '', ref: c.ref ?? '', tags: c.tags.join(', ') });
    },
  });
  // Cancelling an editor releases its lock.
  const release = (id: number) => {
    void postJson<Claim>(`${base}/claims/${id}/unlock`, { user }).catch(() => undefined);
  };

  // The server refuses a change the claim's status does not allow; the error shows on that claim.
  const move = useMutation({
    mutationFn: (m: StatusChange) =>
      postJson<Claim>(`${base}/claims/${m.id}/status`, { status: m.status, user, review: m.review ?? false, reason: m.reason ?? '' }),
    onSuccess: async (_, m) => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
      if (m.review) setReasons((r) => ({ ...r, [m.id]: '' }));
    },
  });
  const moveTo = (id: number, status: ClaimStatus, review?: { reason: string }, voidReason?: string) => {
    move.reset();
    lock.reset();
    setUneditable(undefined);
    reassign.reset();
    resettle.reset();
    defer.reset();
    remind.reset();
    comment.reset();
    line.reset();
    proof.reset();
    split.reset();
    redate.reset();
    inline.reset();
    move.mutate({ id, status, review: review !== undefined, reason: review?.reason ?? voidReason });
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
    lock.reset();
    setUneditable(undefined);
    reassign.reset();
    resettle.reset();
    defer.reset();
    remind.reset();
    comment.reset();
    line.reset();
    proof.reset();
    split.reset();
    redate.reset();
    inline.reset();
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
    lock.reset();
    setUneditable(undefined);
    reassign.reset();
    resettle.reset();
    defer.reset();
    remind.reset();
    comment.reset();
    line.reset();
    proof.reset();
    split.reset();
    redate.reset();
    inline.reset();
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
    lock.reset();
    setUneditable(undefined);
    reassign.reset();
    resettle.reset();
    defer.reset();
    remind.reset();
    comment.reset();
    line.reset();
    proof.reset();
    split.reset();
    redate.reset();
    inline.reset();
    defer.mutate({ id, until: (deferDates[id] ?? '').trim() });
  };
  // Reminding an OPEN or PENDING claim marks it reminded on the sheet's today; once per day.
  const remind = useMutation({
    mutationFn: (id: number) => postJson<Claim>(`${base}/claims/${id}/remind`, { user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    },
  });
  const remindClaim = (id: number) => {
    move.reset();
    lock.reset();
    setUneditable(undefined);
    reassign.reset();
    resettle.reset();
    defer.reset();
    remind.reset();
    comment.reset();
    line.reset();
    proof.reset();
    split.reset();
    redate.reset();
    inline.reset();
    remind.mutate(id);
  };
  // A comment is kept on the claim, in any status, with its writer and the sheet's today; not an empty one.
  const comment = useMutation({
    mutationFn: (d: { id: number; text: string }) =>
      postJson<Claim>(`${base}/claims/${d.id}/comment`, { text: d.text, user }),
    onSuccess: async (_, d) => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
      setCommentTexts((t) => ({ ...t, [d.id]: '' }));
    },
  });
  const addComment = (id: number) => {
    move.reset();
    lock.reset();
    setUneditable(undefined);
    reassign.reset();
    resettle.reset();
    defer.reset();
    remind.reset();
    comment.reset();
    line.reset();
    proof.reset();
    split.reset();
    redate.reset();
    inline.reset();
    comment.mutate({ id, text: commentTexts[id] ?? '' });
  };
  // A line on a DRAFT claim makes its amount the sum of its lines; an edit of the claim, refused like one.
  const line = useMutation({
    mutationFn: (d: { id: number; label: string; qty: string; unit: string }) =>
      postJson<Claim>(`${base}/claims/${d.id}/line`, { label: d.label, qty: d.qty, unit: d.unit, user }),
    onSuccess: async (_, d) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
      ]);
      setLineFields((l) => ({ ...l, [d.id]: { label: '', qty: '', unit: '' } }));
    },
  });
  const addLine = (id: number) => {
    move.reset();
    lock.reset();
    setUneditable(undefined);
    reassign.reset();
    resettle.reset();
    defer.reset();
    remind.reset();
    comment.reset();
    line.reset();
    proof.reset();
    split.reset();
    redate.reset();
    inline.reset();
    const f = lineFields[id] ?? { label: '', qty: '', unit: '' };
    line.mutate({ id, label: f.label.trim(), qty: f.qty.trim(), unit: f.unit.trim() });
  };
  const setLine = (id: number, key: 'label' | 'qty' | 'unit') => (e: ChangeEvent<HTMLInputElement>) => {
    setLineFields({ ...lineFields, [id]: { ...(lineFields[id] ?? { label: '', qty: '', unit: '' }), [key]: e.target.value } });
    if (line.variables?.id === id) line.reset();
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
    lock.reset();
    setUneditable(undefined);
    reassign.reset();
    resettle.reset();
    defer.reset();
    remind.reset();
    comment.reset();
    line.reset();
    proof.reset();
    split.reset();
    redate.reset();
    inline.reset();
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
    lock.reset();
    setUneditable(undefined);
    reassign.reset();
    resettle.reset();
    defer.reset();
    remind.reset();
    comment.reset();
    line.reset();
    proof.reset();
    split.reset();
    redate.reset();
    inline.reset();
    split.mutate({ id, amount: (splitAmounts[id] ?? '').trim() });
  };
  // A steward re-dates an OPEN or PENDING claim; its due-by, and so its period (and per-period number), stay as they are.
  const redate = useMutation({
    mutationFn: (d: { id: number; date: string }) =>
      postJson<Claim>(`${base}/claims/${d.id}/redate`, { date: d.date, user }),
    onSuccess: async (_, d) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
      ]);
      setRedates((a) => ({ ...a, [d.id]: '' }));
    },
  });
  const redateClaim = (id: number) => {
    move.reset();
    lock.reset();
    setUneditable(undefined);
    reassign.reset();
    resettle.reset();
    defer.reset();
    remind.reset();
    comment.reset();
    line.reset();
    proof.reset();
    split.reset();
    redate.mutate({ id, date: (redates[id] ?? '').trim() });
  };
  // A draft's amount changed in its row: an edit as the claim form makes it, keeping the claim's date, note,
  // the claim it corrects, its reference and its tags, so the server applies the form's rules and records CLAIM_EDITED.
  const inline = useMutation({
    mutationFn: (d: { claim: Claim; amount: string }) =>
      putJson<Claim>(`${base}/claims/${d.claim.id}`, {
        amount: d.amount,
        date: d.claim.date,
        note: d.claim.note,
        corrects: d.claim.corrects ?? '',
        ref: d.claim.ref ?? '',
        tags: d.claim.tags.join(','),
        user,
      }),
    onSuccess: async (_, d) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
      ]);
      setInlineAmounts((a) => ({ ...a, [d.claim.id]: '' }));
    },
  });
  const saveInline = (c: Claim) => {
    move.reset();
    lock.reset();
    setUneditable(undefined);
    reassign.reset();
    resettle.reset();
    defer.reset();
    remind.reset();
    comment.reset();
    line.reset();
    proof.reset();
    split.reset();
    redate.reset();
    inline.reset();
    inline.mutate({ claim: c, amount: (inlineAmounts[c.id] ?? '').trim() });
  };
  // The refusal of the last status change, move, re-settle, deferral, reminder, comment, proof, split or re-dating of the claim, as the server gave it; null when none.
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
              : remind.isError && remind.variables === id
                ? remind.error
                : comment.isError && comment.variables?.id === id
                  ? comment.error
                : line.isError && line.variables?.id === id
                  ? line.error
                : proof.isError && proof.variables === id
                  ? proof.error
                  : split.isError && split.variables?.id === id
                    ? split.error
                    : redate.isError && redate.variables?.id === id
                      ? redate.error
                      : inline.isError && inline.variables?.claim.id === id
                        ? inline.error
                        : lock.isError && lock.variables?.id === id
                          ? lock.error
                          : null;
    if (error === null) return uneditable?.id === id ? `A ${uneditable.status} claim cannot be edited.` : null;
    return error instanceof HttpError && error.reason ? `${error.reason}.` : 'The change was refused.';
  };

  const set = (key: keyof Fields) => (e: ChangeEvent<HTMLInputElement>) => {
    setFields({ ...fields, [key]: e.target.value });
    save.reset();
    fromTemplate.reset();
  };
  const open = (c: Claim) => {
    save.reset();
    fromTemplate.reset();
    move.reset();
    lock.reset();
    setUneditable(undefined);
    reassign.reset();
    resettle.reset();
    defer.reset();
    remind.reset();
    comment.reset();
    line.reset();
    proof.reset();
    split.reset();
    redate.reset();
    inline.reset();
    // Only a DRAFT can be edited: any other claim is refused at once, as the server would refuse it.
    if (c.status !== 'DRAFT') {
      setUneditable(c);
      return;
    }
    lock.mutate(c);
  };
  const cancel = () => {
    save.reset();
    fromTemplate.reset();
    lock.reset();
    setUneditable(undefined);
    if (editing !== undefined) release(editing);
    setEditing(undefined);
    setFields(EMPTY);
  };

  // An editor still open when the sheet is shown again fills the form with its claim.
  useEffect(() => {
    if (editingClaim) {
      setFields({
        amount: editingClaim.amount,
        date: editingClaim.date,
        note: editingClaim.note,
        corrects: editingClaim.corrects ?? '',
        ref: editingClaim.ref ?? '',
        tags: editingClaim.tags.join(', '),
      });
    }
  }, [editing]);

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
              <span data-testid="claim-ref">{c.ref ?? ''}</span>{' '}
              <span data-testid="claim-tags">{c.tags.join(', ')}</span>{' '}
              <span data-testid="claim-period">{periodOf(periods.data, c.dueBy)?.name ?? ''}</span>{' '}
              <span data-testid="claim-due">{claimDue(c, rates.data, person.grades, floor, lateOf(settings, today, [person]))}</span>{' '}
              <span data-testid="claim-due-by">{dueBy(c)}</span>{' '}
              <span data-testid="claim-overdue">{overdue(c, today, lateOf(settings, today, [person])) ? 'yes' : 'no'}</span>{' '}
              <span data-testid="claim-status">{c.status}</span>{' '}
              <span data-testid="claim-opened-by">{c.openedBy ?? ''}</span>{' '}
              <span data-testid="claim-stated">{c.stated ? 'yes' : 'no'}</span>{' '}
              <span data-testid="claim-deferred">{c.deferred ? 'yes' : 'no'}</span>{' '}
              <span data-testid="claim-deferred-until">{c.deferredUntil ?? ''}</span>{' '}
              <span data-testid="claim-reminded">{c.remindedOn ?? ''}</span>{' '}
              <span data-testid="claim-split-from">{c.splitFrom ?? ''}</span>{' '}
              <span data-testid="claim-batch">{c.batch ?? ''}</span>{' '}
              <span data-testid="claim-late-entry">{c.lateEntry ? 'yes' : 'no'}</span>{' '}
              <button type="button" data-testid="claim-open" disabled={move.isPending} onClick={() => moveTo(c.id, 'OPEN')}>
                Open
              </button>{' '}
              <button type="button" data-testid="claim-settle" disabled={move.isPending} onClick={() => moveTo(c.id, 'SETTLED')}>
                Settle
              </button>{' '}
              {voidReasons.length > 0 && (
                <select
                  data-testid="claim-void-reason"
                  aria-label="Void reason"
                  value={voidChoices[c.id] ?? ''}
                  onChange={(e) => {
                    setVoidChoices({ ...voidChoices, [c.id]: e.target.value });
                    if (move.variables?.id === c.id) move.reset();
                  }}
                >
                  <option value="">(void reason)</option>
                  {voidReasons.map((r) => (
                    <option key={r.reason} value={r.reason}>
                      {r.reason}
                    </option>
                  ))}
                </select>
              )}{' '}
              <button
                type="button"
                data-testid="claim-void"
                disabled={move.isPending}
                onClick={() => moveTo(c.id, 'VOID', undefined, voidChoices[c.id] ?? '')}
              >
                Void
              </button>{' '}
              <button type="button" data-testid="claim-delete" disabled={move.isPending} onClick={() => moveTo(c.id, 'DELETED')}>
                Delete
              </button>{' '}
              <button type="button" data-testid="claim-restore" disabled={move.isPending} onClick={() => moveTo(c.id, 'DRAFT')}>
                Restore
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
              {/* A steward's review makes a REVIEW claim OPEN; the server refuses anyone else. */}
              <button
                type="button"
                data-testid="claim-review"
                disabled={move.isPending}
                onClick={() => moveTo(c.id, 'OPEN', { reason: '' })}
              >
                Review
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
              <button type="button" data-testid="claim-remind" disabled={remind.isPending} onClick={() => remindClaim(c.id)}>
                Remind
              </button>{' '}
              <input
                data-testid="claim-date-new"
                type="date"
                aria-label="New date"
                value={redates[c.id] ?? ''}
                onChange={(e) => {
                  setRedates({ ...redates, [c.id]: e.target.value });
                  if (redate.variables?.id === c.id) redate.reset();
                }}
              />{' '}
              <button type="button" data-testid="claim-redate" disabled={redate.isPending} onClick={() => redateClaim(c.id)}>
                Re-date
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
              <input
                data-testid="claim-inline-amount"
                placeholder="Amount"
                aria-label="New amount"
                inputMode="decimal"
                value={inlineAmounts[c.id] ?? ''}
                onChange={(e) => {
                  setInlineAmounts({ ...inlineAmounts, [c.id]: e.target.value });
                  if (inline.variables?.claim.id === c.id) inline.reset();
                }}
              />{' '}
              <button type="button" data-testid="claim-inline-save" disabled={inline.isPending} onClick={() => saveInline(c)}>
                Save amount
              </button>{' '}
              <button type="button" data-testid="claim-edit" disabled={lock.isPending} onClick={() => open(c)}>
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
              <input
                data-testid="claim-comment-text"
                placeholder="Comment"
                aria-label="Comment"
                value={commentTexts[c.id] ?? ''}
                onChange={(e) => {
                  setCommentTexts({ ...commentTexts, [c.id]: e.target.value });
                  if (comment.variables?.id === c.id) comment.reset();
                }}
              />{' '}
              <button type="button" data-testid="claim-comment-add" disabled={comment.isPending} onClick={() => addComment(c.id)}>
                Comment
              </button>
              <input
                data-testid="claim-line-label"
                placeholder="Line"
                aria-label="Line label"
                value={lineFields[c.id]?.label ?? ''}
                onChange={setLine(c.id, 'label')}
              />{' '}
              <input
                data-testid="claim-line-qty"
                placeholder="Qty"
                aria-label="Line quantity"
                inputMode="numeric"
                value={lineFields[c.id]?.qty ?? ''}
                onChange={setLine(c.id, 'qty')}
              />{' '}
              <input
                data-testid="claim-line-unit"
                placeholder="Unit"
                aria-label="Line unit amount"
                inputMode="decimal"
                value={lineFields[c.id]?.unit ?? ''}
                onChange={setLine(c.id, 'unit')}
              />{' '}
              <button type="button" data-testid="claim-line-add" disabled={line.isPending} onClick={() => addLine(c.id)}>
                Add line
              </button>
              <ol data-testid="claim-lines">
                {(c.lines ?? []).map((l, i) => (
                  <li key={i} data-testid="claim-lines-line">
                    {l}
                  </li>
                ))}
              </ol>
              <ol data-testid="claim-comments">
                {(c.comments ?? []).map((line, i) => (
                  <li key={i} data-testid="claim-comments-line">
                    {line}
                  </li>
                ))}
              </ol>
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
          // Submitting the form is the next action on the row: a refused editor opening no longer shows.
          lock.reset();
          setUneditable(undefined);
          fromTemplate.reset();
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
        <input data-testid="claim-form-ref" placeholder="Reference" value={fields.ref} onChange={set('ref')} />
        <input data-testid="claim-form-tags" placeholder="Tags (comma separated)" value={fields.tags} onChange={set('tags')} />
        <button type="submit" data-testid="claim-form-submit" disabled={save.isPending}>
          {editing !== undefined ? 'Save claim' : 'Add claim'}
        </button>
        <select
          data-testid="claim-template-select"
          aria-label="Claim template"
          value={template}
          onChange={(e) => {
            setTemplate(e.target.value);
            save.reset();
            fromTemplate.reset();
          }}
        >
          <option value="">Template…</option>
          {templates.map((t) => (
            <option key={t.name} value={t.name}>
              {t.name}
            </option>
          ))}
        </select>
        <button
          type="button"
          data-testid="claim-template-add"
          disabled={fromTemplate.isPending}
          onClick={() => {
            save.reset();
            lock.reset();
            setUneditable(undefined);
            submitTemplate(template);
          }}
        >
          Add from template
        </button>
        {(save.isError || fromTemplate.isError) && (
          <span data-testid="claim-form-error" role="alert">
            {' '}
            {(() => {
              const error = save.isError ? save.error : fromTemplate.error;
              return error instanceof HttpError && error.reason ? `${error.reason}.` : 'The claim was refused.';
            })()}
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
