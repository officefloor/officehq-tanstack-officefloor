import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { PersonRow } from '../../slots/defs/personRow';
import { gradeOn, usePeople } from '../people/people';
import type { Person } from '../people/people';
import { useFloor, useLate, useSettings } from '../settings/settings';
import { claimDue, rateOn, useRates } from '../rates/rates';
import { periodOf, usePeriods } from '../periods/periods';
import { useSheetToday } from '../sheet/sheet';
import { claimsKey, dueBy, live, overdue, sumAmounts, useClaims, useEditingClaim } from './claims';
import type { Claim } from './claims';
import { setSelected, useSelectedClaims } from './selection';
import { setLockRefused, useLockRefused } from './lock';
import { useVoidReasons, voidReasonsKey } from '../voidreasons/voidreasons';
import { useTemplates } from '../templates/templates';

const AMOUNT = /^-?\d+(\.\d{1,2})?$/;

/** Release the user's lock on a claim's editor as the editor is cancelled (or moves to another claim). Nothing is
 *  shown when it fails: the lock is the server's, and it goes anyway when the sheet's today changes. */
function releaseClaim(sheetId: string, user: string, claimId: string) {
  void putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claimId}/unlock`, { user }).catch(() => undefined);
}
const NUMBER = /^(\d+|.+-\d+)$/;

/** One form on the person's row that adds a claim, or edits the claim whose editor is open. A blank
 *  date means the sheet's today (for a new claim, the sheet's entry date while it is set). A negative amount is a correction, which names the number of the SETTLED claim
 *  of the same person it corrects (the server checks that claim). A reference (trimmed; blank: none) must not be
 *  used by another claim on the sheet, compared without regard to case (the server checks that). Tags are separated by
 *  commas (blank: none; the server trims, lower-cases and drops repeats). A new claim the server refuses clears the form,
 *  as one it adds does, leaving its error showing. A new claim may instead be made from a template chosen on the row:
 *  a DRAFT with the template's amount and note as they are now and no date, added by the server by every rule of this
 *  form; its refusal shows as this form's error. */
function ClaimForm({ sheetId, user, person, editing, onAdded }: {
  sheetId: string; user: string; person: Person; editing?: Claim; onAdded: () => void;
}) {
  const queryClient = useQueryClient();
  const today = useSheetToday(sheetId);
  const entryDate = useSettings(sheetId).data?.entryDate ?? '';
  const blankDate = editing ? today : entryDate || today;
  const [, setEditing] = useEditingClaim();
  const [amount, setAmount] = useState(editing?.amount ?? '');
  const [date, setDate] = useState(editing?.date ?? '');
  const [note, setNote] = useState(editing?.note ?? '');
  const [corrects, setCorrects] = useState(editing?.corrects ?? '');
  const [ref, setRef] = useState(editing?.ref ?? '');
  const [tags, setTags] = useState(editing?.tags.join(', ') ?? '');
  const [error, setError] = useState('');
  const [template, setTemplate] = useState('');
  const templates = useTemplates(sheetId).data ?? [];
  const once = useSubmitOnce();

  const fromTemplate = useMutation({
    mutationFn: (name: string) =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/template-claims`, { user, template: name }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
      onAdded();
    },
    onError: (e) => setError(e instanceof HttpError && (e.status === 409 || e.status === 400) && e.body
      ? e.body
      : 'Could not add a claim from this template.'),
    onSettled: once.done,
  });

  const addFromTemplate = () => {
    setError('');
    if (template === '') {
      setError('Choose a template.');
      return;
    }
    if (!once.start()) return;
    fromTemplate.mutate(template);
  };

  const save = useMutation({
    mutationFn: (body: { user: string; amount: string; date: string; note: string; corrects: string; ref: string; tags: string }) =>
      editing
        ? putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${editing.id}`, body)
        : postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/claims`, body),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
      if (editing) setEditing(undefined);
      else onAdded();
    },
    onError: (e) => {
      setError(e instanceof HttpError && (e.status === 409 || e.status === 400) && e.body
        ? e.body
        : 'Could not save this claim: the amount must be 0.00 or more with up to two decimals, and the date yyyy-mm-dd.');
      // a refused new claim is consumed as an added one is: the next starts from a blank form (date = today)
      if (!editing) {
        setAmount('');
        setDate('');
        setNote('');
        setCorrects('');
        setRef('');
        setTags('');
      }
    },
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (editing) setLockRefused(editing.id, '');
    if (!AMOUNT.test(amount.trim())) {
      setError('The amount must be a number with up to two decimals.');
      return;
    }
    const negative = amount.trim().startsWith('-') && Number(amount.trim()) < 0;
    if (negative && !NUMBER.test(corrects.trim())) {
      setError('A negative amount is a correction: name the number of the SETTLED claim it corrects.');
      return;
    }
    if (!negative && corrects.trim() !== '') {
      setError('Only a correction (a negative amount) names a claim it corrects.');
      return;
    }
    if (!once.start()) return;
    save.mutate({ user, amount: amount.trim(), date: date.trim(), note, corrects: corrects.trim(), ref: ref.trim(), tags });
  };

  const change = (set: (v: string) => void) => (e: { target: { value: string } }) => {
    set(e.target.value);
    setError('');
    if (editing) setLockRefused(editing.id, '');
  };

  return (
    <form data-testid="claim-form" onSubmit={submit}>
      <input data-testid="claim-form-amount" placeholder="Amount" value={amount} onChange={change(setAmount)} />{' '}
      <input data-testid="claim-form-date" placeholder={blankDate || 'yyyy-mm-dd'} value={date} onChange={change(setDate)} />{' '}
      <input data-testid="claim-form-note" placeholder="Note" value={note} onChange={change(setNote)} />{' '}
      <input data-testid="claim-form-corrects" placeholder="Corrects no." value={corrects} onChange={change(setCorrects)} />{' '}
      <input data-testid="claim-form-ref" placeholder="Reference" value={ref} onChange={change(setRef)} />{' '}
      <input data-testid="claim-form-tags" placeholder="Tags (comma separated)" value={tags} onChange={change(setTags)} />{' '}
      <button type="submit" data-testid="claim-form-submit" disabled={save.isPending}>
        {editing ? 'Save claim' : 'Add claim'}
      </button>
      {editing && (
        <button
          type="button"
          data-testid="claim-form-cancel"
          onClick={() => {
            releaseClaim(sheetId, user, String(editing.id));
            setEditing(undefined);
          }}
        >
          Cancel
        </button>
      )}
      {!editing && (
        <>
          {' '}
          <select data-testid="claim-template-select" value={template} onChange={change(setTemplate)}>
            <option value="">Template…</option>
            {templates.map((t) => (
              <option key={t.name} value={t.name}>
                {t.name}
              </option>
            ))}
          </select>{' '}
          <button type="button" data-testid="claim-template-add" disabled={fromTemplate.isPending} onClick={addFromTemplate}>
            Add from template
          </button>
        </>
      )}
      {error && (
        <p data-testid="claim-form-error" role="alert">
          {error}
        </p>
      )}
    </form>
  );
}

const ACTIONS = [
  { action: 'open', label: 'Open' },
  { action: 'settle', label: 'Settle' },
  { action: 'approve', label: 'Approve' },
  { action: 'reject', label: 'Reject' },
  { action: 'void', label: 'Void' },
  { action: 'resettle', label: 'Re-settle' },
  { action: 'delete', label: 'Delete' },
  { action: 'restore', label: 'Restore' },
] as const;

/** One claim with its status, its due-by and whether it is overdue, who opened it and the buttons that change it
 *  (a steward approves or rejects, with a reason, a PENDING claim, and re-settles a SETTLED one at today's figure; a DRAFT is deleted, and restored to a DRAFT on the sheet-today it was deleted). A change the status does not
 *  allow, a settle (or approve) by the user who opened it or last edited it, or an approve or reject by a clerk, is refused by the server and shown as the claim's error;
 *  so is editing a claim that is no longer a DRAFT, or one whose editor another user holds open (opening the editor locks
 *  the claim until it is saved or cancelled, or the sheet's today changes), or one dated in a closed period (the server refuses those too; it refuses saving
 *  an edit of one whose due-by, its period, falls in a closed period). A steward re-dates an OPEN or PENDING claim; the server refuses anyone else, a date after its person's
 *  retirement, and a claim whose due-by is inside a closed period. */
function ClaimRow({ sheetId, user, claim, rate }: { sheetId: string; user: string; claim: Claim; rate?: string }) {
  const queryClient = useQueryClient();
  const [editingId, setEditing] = useEditingClaim();
  const [error, setError] = useState('');
  const refused = useLockRefused(claim.id);
  const clearError = () => {
    setError('');
    setLockRefused(claim.id, '');
  };
  const [reason, setReason] = useState('');
  const voidReasons = useVoidReasons(sheetId).data ?? [];
  const [voidReason, setVoidReason] = useState('');
  const periods = usePeriods(sheetId).data ?? [];
  const period = periodOf(periods, dueBy(claim));
  // The editor opens for a claim dated outside a closed period; saving it is refused when its due-by is inside one.
  const datedIn = periodOf(periods, claim.date);
  const floor = useFloor(sheetId);
  const today = useSheetToday(sheetId);
  const late = useLate(sheetId);
  const selected = useSelectedClaims(sheetId).has(claim.id);
  const others = (usePeople(sheetId).data ?? []).filter((p) => p.id !== claim.personId);
  const [moveTo, setMoveTo] = useState('');
  const [deferUntil, setDeferUntil] = useState('');
  const [redateTo, setRedateTo] = useState('');
  const [splitAmount, setSplitAmount] = useState('');
  const [inlineAmount, setInlineAmount] = useState('');
  const [comment, setComment] = useState('');

  const change = useMutation({
    mutationFn: (action: string) =>
      putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/status`, action === 'reject' ? { user, action, reason }
        : action === 'void' && voidReason !== '' ? { user, action, reason: voidReason } : { user, action }),
    onSuccess: async (_, action) => {
      if (action === 'reject') setReason('');
      if (action === 'void') {
        setVoidReason('');
        await queryClient.invalidateQueries({ queryKey: voidReasonsKey(sheetId) });
      }
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (e, action) =>
      setError(e instanceof HttpError && (e.status === 409 || e.status === 400) && e.body ? e.body : `Could not ${action} this ${claim.status} claim.`),
  });

  const reassign = useMutation({
    mutationFn: (personId: string) =>
      putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/reassign`, { user, personId }),
    onSuccess: async () => {
      setMoveTo('');
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (e) =>
      setError(e instanceof HttpError && (e.status === 409 || e.status === 400 || e.status === 404) && e.body
        ? e.body : `Could not move this ${claim.status} claim.`),
  });

  const proof = useMutation({
    mutationFn: () =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/proof`, { user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (e) =>
      setError(e instanceof HttpError && (e.status === 409 || e.status === 400 || e.status === 404) && e.body
        ? e.body : `Could not add a proof to this ${claim.status} claim.`),
  });

  const defer = useMutation({
    mutationFn: (until: string) =>
      putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/defer`, { user, until }),
    onSuccess: async () => {
      setDeferUntil('');
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (e) =>
      setError(e instanceof HttpError && (e.status === 409 || e.status === 400 || e.status === 404) && e.body
        ? e.body : `Could not defer this ${claim.status} claim.`),
  });

  const remind = useMutation({
    mutationFn: () =>
      putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/remind`, { user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (e) =>
      setError(e instanceof HttpError && (e.status === 409 || e.status === 400 || e.status === 404) && e.body
        ? e.body : `Could not remind this ${claim.status} claim.`),
  });

  const addComment = useMutation({
    mutationFn: (text: string) =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/comments`, { user, text }),
    onSuccess: async () => {
      setComment('');
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (e) =>
      setError(e instanceof HttpError && (e.status === 409 || e.status === 400 || e.status === 403 || e.status === 404) && e.body
        ? e.body : `Could not comment this ${claim.status} claim.`),
  });

  const redate = useMutation({
    mutationFn: (date: string) =>
      putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/redate`, { user, date }),
    onSuccess: async () => {
      setRedateTo('');
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (e) =>
      setError(e instanceof HttpError && (e.status === 409 || e.status === 400 || e.status === 404) && e.body
        ? e.body : `Could not re-date this ${claim.status} claim.`),
  });

  // Opening the editor locks the claim for other users until it is saved or cancelled (or the sheet's today
  // changes); the editor stays open on a refusal, whose error shows on the claim, and the server refuses its save.
  const lock = useMutation({
    mutationFn: () =>
      putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/lock`, { user }),
    onError: (e) =>
      setLockRefused(claim.id, e instanceof HttpError && (e.status === 409 || e.status === 400 || e.status === 403 || e.status === 404) && e.body
        ? e.body : `Could not open this ${claim.status} claim for editing.`),
  });

  const split = useMutation({
    mutationFn: (amount: string) =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/split`, { user, amount }),
    onSuccess: async () => {
      setSplitAmount('');
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (e) =>
      setError(e instanceof HttpError && (e.status === 409 || e.status === 400 || e.status === 404) && e.body
        ? e.body : `Could not split this ${claim.status} claim.`),
  });

  // The row's own amount entry: the claim form's edit with only the amount changed (its date, note and the
  // claim it corrects, its reference and its tags stay as they are), so the server refuses it by the same rules and audits CLAIM_EDITED.
  const inline = useMutation({
    mutationFn: (amount: string) =>
      putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}`,
        { user, amount, date: claim.date, note: claim.note, corrects: claim.corrects ?? '', ref: claim.ref ?? '' }),
    onSuccess: async () => {
      setInlineAmount('');
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (e) =>
      setError(e instanceof HttpError && (e.status === 409 || e.status === 400 || e.status === 403 || e.status === 404) && e.body
        ? e.body : `Could not change the amount of this ${claim.status} claim.`),
  });

  return (
    <li data-testid={`claim-row-${claim.id}`}>
      <input
        type="checkbox"
        data-testid="claim-select"
        checked={selected}
        onChange={(e) => setSelected(sheetId, claim.id, e.target.checked)}
      />{' '}
      <span data-testid="claim-amount">{claim.amount}</span>{' '}
      <span data-testid="claim-date">{claim.date}</span>{' '}
      <span data-testid="claim-note">{claim.note}</span>{' '}
      Ref <span data-testid="claim-ref">{claim.ref ?? ''}</span>{' '}
      Tags <span data-testid="claim-tags">{claim.tags.join(', ')}</span>{' '}
      <span data-testid="claim-number">{claim.number ?? ''}</span>{' '}
      Corrects <span data-testid="claim-corrects">{claim.corrects ?? ''}</span>{' '}
      <span data-testid="claim-status">{claim.status}</span>{' '}
      <span data-testid="claim-opened-by">{claim.openedBy ?? ''}</span>{' '}
      <span data-testid="claim-due">{claimDue(claim, rate, floor, late)}</span>{' '}
      Due by <span data-testid="claim-due-by">{dueBy(claim)}</span>{' '}
      Overdue <span data-testid="claim-overdue">{overdue(claim, today, late.grace(claim)) ? 'yes' : 'no'}</span>{' '}
      Period <span data-testid="claim-period">{period?.name ?? ''}</span>{' '}
      Late entry <span data-testid="claim-late-entry">{claim.lateEntry ? 'yes' : 'no'}</span>{' '}
      Stated <span data-testid="claim-stated">{claim.stated ? 'yes' : 'no'}</span>{' '}
      Deferred <span data-testid="claim-deferred">{claim.deferred ? 'yes' : 'no'}</span>{' '}
      until <span data-testid="claim-deferred-until">{claim.deferredUntil ?? ''}</span>{' '}
      Proofs <span data-testid="claim-proofs">{claim.proofs}</span>{' '}
      Split from <span data-testid="claim-split-from">{claim.splitFrom ?? ''}</span>{' '}
      Reminded <span data-testid="claim-reminded">{claim.reminded ?? ''}</span>{' '}
      Batch <span data-testid="claim-batch">{claim.batch ?? ''}</span>{' '}
      <ol data-testid="claim-log">
        {claim.log.map((line, i) => (
          <li key={i} data-testid="claim-log-line">{line}</li>
        ))}
      </ol>{' '}
      <ol data-testid="claim-comments">
        {claim.comments.map((line, i) => (
          <li key={i} data-testid="claim-comments-line">{line}</li>
        ))}
      </ol>{' '}
      <input
        data-testid="claim-comment-text"
        placeholder="Comment"
        value={comment}
        onChange={(e) => {
          clearError();
          setComment(e.target.value);
        }}
      />{' '}
      <button
        type="button"
        data-testid="claim-comment-add"
        disabled={addComment.isPending}
        onClick={() => {
          clearError();
          if (comment.trim() === '') {
            setError('Write the comment before adding it.');
            return;
          }
          addComment.mutate(comment.trim());
        }}
      >
        Comment
      </button>{' '}
      <button
        type="button"
        data-testid="claim-edit"
        onClick={() => {
          if (claim.status !== 'DRAFT') {
            setError(`A ${claim.status} claim cannot be edited; only a DRAFT can. Void it and add a new one.`);
            return;
          }
          if (datedIn?.closed) {
            setError(`This claim is dated in the closed period ${datedIn.name}; it cannot be edited.`);
            return;
          }
          clearError();
          if (editingId !== '' && editingId !== String(claim.id)) releaseClaim(sheetId, user, editingId);
          setEditing(String(claim.id));
          lock.mutate();
        }}
      >
        Edit
      </button>{' '}
      <input
        data-testid="claim-inline-amount"
        placeholder="Amount"
        value={inlineAmount}
        onChange={(e) => {
          clearError();
          setInlineAmount(e.target.value);
        }}
      />{' '}
      <button
        type="button"
        data-testid="claim-inline-save"
        disabled={inline.isPending}
        onClick={() => {
          clearError();
          const amount = inlineAmount.trim();
          if (!AMOUNT.test(amount)) {
            setError('The amount must be a number with up to two decimals.');
            return;
          }
          const negative = amount.startsWith('-') && Number(amount) < 0;
          if (negative && !NUMBER.test((claim.corrects ?? '').trim())) {
            setError('A negative amount is a correction: name the number of the SETTLED claim it corrects.');
            return;
          }
          if (!negative && (claim.corrects ?? '').trim() !== '') {
            setError('Only a correction (a negative amount) names a claim it corrects.');
            return;
          }
          inline.mutate(amount);
        }}
      >
        Save amount
      </button>{' '}
      {editingId === String(claim.id) && (
        <button
          type="button"
          data-testid="claim-cancel"
          onClick={() => {
            clearError();
            releaseClaim(sheetId, user, String(claim.id));
            setEditing(undefined);
          }}
        >
          Cancel edit
        </button>
      )}
      <input
        data-testid="claim-reject-reason"
        placeholder="Reject reason"
        value={reason}
        onChange={(e) => {
          clearError();
          setReason(e.target.value);
        }}
      />{' '}
      {voidReasons.length > 0 && (
        <select
          data-testid="claim-void-reason"
          value={voidReason}
          onChange={(e) => {
            clearError();
            setVoidReason(e.target.value);
          }}
        >
          <option value="">Void reason…</option>
          {voidReasons.map((r) => (
            <option key={r.id} value={r.reason}>
              {r.reason}
            </option>
          ))}
        </select>
      )}{' '}
      {ACTIONS.map(({ action, label }) => (
        <button
          key={action}
          type="button"
          data-testid={`claim-${action}`}
          disabled={change.isPending}
          onClick={() => {
            clearError();
            change.mutate(action);
          }}
        >
          {label}
        </button>
      ))}{' '}
      <select
        data-testid="claim-reassign-to"
        value={moveTo}
        onChange={(e) => {
          clearError();
          setMoveTo(e.target.value);
        }}
      >
        <option value="">Move to…</option>
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
          clearError();
          if (moveTo === '') {
            setError('Choose the person to move this claim to.');
            return;
          }
          reassign.mutate(moveTo);
        }}
      >
        Move
      </button>{' '}
      <button
        type="button"
        data-testid="claim-proof"
        disabled={proof.isPending}
        onClick={() => {
          clearError();
          proof.mutate();
        }}
      >
        Add proof
      </button>{' '}
      <input
        data-testid="claim-defer-date"
        placeholder="Defer until yyyy-mm-dd"
        value={deferUntil}
        onChange={(e) => {
          clearError();
          setDeferUntil(e.target.value);
        }}
      />{' '}
      <button
        type="button"
        data-testid="claim-defer"
        disabled={defer.isPending}
        onClick={() => {
          clearError();
          if (!/^\d{4}-\d{2}-\d{2}$/.test(deferUntil.trim())) {
            setError('Give the date to defer this claim until as yyyy-mm-dd.');
            return;
          }
          defer.mutate(deferUntil.trim());
        }}
      >
        Defer
      </button>{' '}
      <button
        type="button"
        data-testid="claim-remind"
        disabled={remind.isPending}
        onClick={() => {
          clearError();
          remind.mutate();
        }}
      >
        Remind
      </button>{' '}
      <input
        data-testid="claim-date-new"
        placeholder="Re-date to yyyy-mm-dd"
        value={redateTo}
        onChange={(e) => {
          clearError();
          setRedateTo(e.target.value);
        }}
      />{' '}
      <button
        type="button"
        data-testid="claim-redate"
        disabled={redate.isPending}
        onClick={() => {
          clearError();
          if (!/^\d{4}-\d{2}-\d{2}$/.test(redateTo.trim())) {
            setError('Give the new date of this claim as yyyy-mm-dd.');
            return;
          }
          redate.mutate(redateTo.trim());
        }}
      >
        Re-date
      </button>{' '}
      <input
        data-testid="claim-split-amount"
        placeholder="Split off amount"
        value={splitAmount}
        onChange={(e) => {
          clearError();
          setSplitAmount(e.target.value);
        }}
      />{' '}
      <button
        type="button"
        data-testid="claim-split"
        disabled={split.isPending}
        onClick={() => {
          clearError();
          if (!/^\d+(\.\d{1,2})?$/.test(splitAmount.trim())) {
            setError('The amount to split off must be a number with up to two decimals.');
            return;
          }
          split.mutate(splitAmount.trim());
        }}
      >
        Split
      </button>
      {(error || refused) && (
        <p data-testid="claim-error" role="alert">
          {error || refused}
        </p>
      )}
    </li>
  );
}

/** A person's claimed total and their claims, newest first, with the form that adds or edits them. */
function PersonClaims({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const claims = (useClaims(sheetId).data ?? []).filter((c) => c.personId === person.id);
  const [editingId] = useEditingClaim();
  const editing = claims.find((c) => String(c.id) === editingId);
  const [added, setAdded] = useState(0);
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);

  return (
    <>
      <td data-testid="person-claimed">{sumAmounts(live(claims, [person], today))}</td>
      <td>
        {/* Keyed by the claim being edited (and each add), so the form fills with its values or starts blank. */}
        <ClaimForm
          key={editing ? `edit-${editing.id}` : `new-${added}`}
          sheetId={sheetId}
          user={user}
          person={person}
          editing={editing}
          onAdded={() => setAdded((n) => n + 1)}
        />
        <ul>
          {claims.map((c) => (
            <ClaimRow key={c.id} sheetId={sheetId} user={user} claim={c} rate={rateOn(rates, gradeOn(person, c.date), c.date)} />
          ))}
        </ul>
      </td>
    </>
  );
}

export const contribution = PersonRow.fill({ order: 10, Component: PersonClaims });
