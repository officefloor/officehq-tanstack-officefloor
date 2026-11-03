import { useEffect, useState, type ChangeEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { useSingleFlight } from '../../query/useSingleFlight';
import { PERSON_ROW_SPAN, PersonCells, PersonColumnHead, PersonDetail } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheetBody';
import { claimedHoldsById, gradeOn, usePeople } from '../people/usePeople';
import { periodOf, usePeriods } from '../periods/usePeriods';
import { rateFor, useRates } from '../rates/useRates';
import { claimDueNow, isOverdue } from '../overdue/useOverdue';
import { useSignedInUser } from '../session/useSession';
import { roleOf, useSettings } from '../settings/useSettings';
import { useSheetToday } from '../today/useSheetToday';
import { useTemplates } from '../templates/useTemplates';
import { claimed, claimsKey, shownDeferred, shownTags, useClaims, useLastBatch, type Claim } from './useClaims';
import { useClaimSelection } from './useClaimSelection';
import { useFigure } from '../locale/useLocale';
import { Col } from '../columns/useColumns';

/** A person's claimed: the sum of their counted, non-correction (OPEN, PENDING or SETTLED; held or not; not a deferred one) claims' amounts. */
function PersonClaimed({ sheetId, personId }: { sheetId: string; personId: number }) {
  const fig = useFigure();
  const claims = useClaims(sheetId);
  const people = usePeople(sheetId);
  const today = useSheetToday(sheetId).data?.today;
  return (
    <Col id="claimed">
      <td data-testid="person-claimed">
        {fig(claimed((claims.data ?? []).filter((c) => c.personId === personId), claimedHoldsById(people.data, today), today))}
      </td>
    </Col>
  );
}

type ClaimAction = 'open' | 'review' | 'settle' | 'approve' | 'reject' | 'void' | 'resettle' | 'delete' | 'restore' | 'defer' | 'proof' | 'remind' | 'waive' | 'hold' | 'split' | 'settle-part' | 'redate' | 'comment' | 'line';

/** A claim's status changes; the server refuses one its status does not allow, a settle by the user
 *  who opened the claim, and an approve or reject by anyone but a steward or the person's approver (an approve also by the
 *  user who opened the claim), a re-settle by anyone but a steward or of a claim not SETTLED, and a void of a SETTLED claim by anyone but a steward, more than 7 days after its settlement or in a closed period. A reject takes the reason typed on the claim's row.
 *  A delete moves a DRAFT claim (not in a closed period) to DELETED, where it counts nowhere; a restore makes it a DRAFT again, only on the sheet-today it was deleted.
 *  A settle or approve of a claim whose due is above the proof threshold in force on its date is refused while it has no proof.
 *  An open of a claim whose due is above the review threshold in force on its date makes it REVIEW; a claim gets its number only when settled or approved; a review
 *  by a steward makes it OPEN, by anyone else is refused. */
const ACTIONS: [Exclude<ClaimAction, 'defer' | 'proof' | 'remind' | 'waive' | 'hold' | 'split' | 'settle-part' | 'redate' | 'comment' | 'line'>, string][] = [
  ['open', 'Open'],
  ['review', 'Review'],
  ['settle', 'Settle'],
  ['approve', 'Approve'],
  ['reject', 'Reject'],
  ['void', 'Void'],
  ['resettle', 'Re-settle'],
  ['delete', 'Delete'],
  ['restore', 'Restore'],
];

/** A person's claims, newest first, under their row, with a form that adds a claim, or edits one
 *  when its editor is open. A blank date takes the sheet's today. A negative amount is a correction:
 *  the form names, by number, the settled claim of the same person it corrects. A claim may carry an external
 *  reference (trimmed; blank: none), used at most once on the sheet regardless of case. */
/** A claim's timeline: on each press, the audit lines about it re-read from the audit file, newest first,
 *  exactly as the file holds them — those carrying its number, or for a claim with no number those naming its
 *  person, amount and date together. It reads the file; it records nothing. */
function ClaimTimeline({ sheetId, claimId }: { sheetId: string; claimId: number }) {
  const show = useMutation({
    mutationFn: () =>
      getJson<{ lines: string[] }>(
        `/api/sheets/${encodeURIComponent(sheetId)}/claims/${claimId}/timeline`,
      ),
  });
  return (
    <>
      <button
        type="button"
        data-testid="claim-timeline"
        disabled={show.isPending}
        onClick={() => {
          show.reset();
          show.mutate();
        }}
      >
        Timeline
      </button>
      {show.isError && <span data-testid="claim-timeline-error">{show.error.message}</span>}
      {show.isSuccess &&
        show.data.lines.map((line, i) => (
          <div key={i} data-testid="claim-timeline-line">
            {line}
          </div>
        ))}
    </>
  );
}

function PersonClaims({ sheetId, personId }: { sheetId: string; personId: number }) {
  const fig = useFigure();
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
  const [ref, setRef] = useState('');
  const [tags, setTags] = useState('');
  const [template, setTemplate] = useState('');
  const templates = useTemplates(sheetId);
  const [reasons, setReasons] = useState<Record<number, string>>({});
  const [voidReasons, setVoidReasons] = useState<Record<number, string>>({});
  const [targets, setTargets] = useState<Record<number, string>>({});
  const [deferDates, setDeferDates] = useState<Record<number, string>>({});
  const [settleDates, setSettleDates] = useState<Record<number, string>>({});
  const [splitAmounts, setSplitAmounts] = useState<Record<number, string>>({});
  const [settlePartAmounts, setSettlePartAmounts] = useState<Record<number, string>>({});
  const [newDates, setNewDates] = useState<Record<number, string>>({});
  const [comments, setComments] = useState<Record<number, string>>({});
  const [proofKinds, setProofKinds] = useState<Record<number, string>>({});
  const [lineFields, setLineFields] = useState<Record<number, { label: string; qty: string; unit: string }>>({});
  const [inlineAmounts, setInlineAmounts] = useState<Record<number, string>>({});

  const clearForm = () => {
    setEditing(null);
    setAmount('');
    setDate('');
    setNote('');
    setCorrects('');
    setRef('');
    setTags('');
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
    setRef(c.ref ?? '');
    setTags(shownTags(c.tags));
  };

  const submitOnce = useSingleFlight();
  const save = useMutation({
    mutationFn: (f: { id: number | null; amount: string; date: string; note: string; corrects: string; ref: string; tags: string }) => {
      const sheet = `/api/sheets/${encodeURIComponent(sheetId)}`;
      const path = f.id === null ? `${sheet}/people/${personId}/claims` : `${sheet}/claims/${f.id}`;
      return postJson<Claim>(path, { amount: f.amount, date: f.date, note: f.note, corrects: f.corrects, ref: f.ref, tags: f.tags });
    },
    onSuccess: async (_, f) => {
      if (f.id !== null) markLock(f.id, null);
      clearForm();
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });

  // A DRAFT claim from a template: its amount and note as they are now, the date left blank, by every rule
  // of the claim form (a refusal shows as the form's).
  const fromTemplate = useMutation({
    mutationFn: (name: string) =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/template-claim`, { template: name }),
    onSuccess: async () => {
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
  // (a new OPEN claim listed under the person), or part-settling one (the amount typed on its row split off and settled at once), or re-dating an OPEN or PENDING one to the date typed on its row, or commenting it with the text typed on its row,
  // or adding the line (label, quantity, unit amount) typed on its row to a DRAFT one (its amount becomes its lines' total);
  // a refusal shows on that claim's row.
  const action = useMutation({
    mutationFn: (a: { id: number; action: ClaimAction; reason: string; until: string; amount: string; date: string; text: string; label: string; qty: string; unit: string; kind: string; on: string }) =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${a.id}/${a.action}`, { reason: a.reason, until: a.until, amount: a.amount, date: a.date, text: a.text, label: a.label, qty: a.qty, unit: a.unit, kind: a.kind, on: a.on }),
    onSuccess: (changed, a) => {
      if (a.action === 'reject') setReasons((r) => ({ ...r, [a.id]: '' }));
      if (a.action === 'void') setVoidReasons((r) => ({ ...r, [a.id]: '' }));
      if (a.action === 'defer') setDeferDates((d) => ({ ...d, [a.id]: '' }));
      if (a.action === 'split') setSplitAmounts((m) => ({ ...m, [a.id]: '' }));
      if (a.action === 'settle-part') setSettlePartAmounts((m) => ({ ...m, [a.id]: '' }));
      if (a.action === 'redate') setNewDates((d) => ({ ...d, [a.id]: '' }));
      if (a.action === 'comment') setComments((m) => ({ ...m, [a.id]: '' }));
      if (a.action === 'proof') setProofKinds((m) => ({ ...m, [a.id]: '' }));
      if (a.action === 'line') setLineFields((m) => ({ ...m, [a.id]: { label: '', qty: '', unit: '' } }));
      // An open editor of the claim takes the amount its lines now add up to.
      if (a.action === 'line' && editing === a.id) setAmount(changed.amount);
    },
    onSettled: async (_, __, a) => {
      // A settle's as-of date is used once, went it through or not, so it never carries into the next settle.
      if (a.action === 'settle') setSettleDates((d) => ({ ...d, [a.id]: '' }));
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

  // A claim's amount changed on its row: saved as the claim's editor would save it, keeping its date, note and
  // corrects, reference and tags, so the server applies the same rules (and writes CLAIM_EDITED); a refusal shows on that claim's row.
  const inline = useMutation({
    mutationFn: (f: { claim: Claim; amount: string }) =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${f.claim.id}`, {
        amount: f.amount,
        date: f.claim.date,
        note: f.claim.note,
        corrects: f.claim.corrects === null ? '' : String(f.claim.corrects),
        ref: f.claim.ref ?? '',
        tags: f.claim.tags ?? '',
      }),
    onSuccess: (_, f) => {
      setInlineAmounts((m) => ({ ...m, [f.claim.id]: '' }));
      markLock(f.claim.id, null);
      if (editing === f.claim.id) clearForm();
    },
    onSettled: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });

  const saveInline = (c: Claim) => {
    setRefused(null);
    action.reset();
    reassign.reset();
    inline.reset();
    inline.mutate({ claim: c, amount: (inlineAmounts[c.id] ?? '').trim() });
  };

  const move = (id: number) => {
    setRefused(null);
    action.reset();
    reassign.reset();
    inline.reset();
    const target = targets[id] ?? '';
    reassign.mutate({ id, personId: target === '' ? null : Number(target) });
  };

  const act = (id: number, a: ClaimAction) => {
    setRefused(null);
    action.reset();
    reassign.reset();
    inline.reset();
    action.mutate({
      id,
      action: a,
      reason: a === 'reject' ? reasons[id] ?? '' : a === 'void' ? voidReasons[id] ?? '' : '',
      until: a === 'defer' ? (deferDates[id] ?? '').trim() : '',
      amount: a === 'split' ? (splitAmounts[id] ?? '').trim() : a === 'settle-part' ? (settlePartAmounts[id] ?? '').trim() : '',
      date: a === 'redate' ? (newDates[id] ?? '').trim() : '',
      text: a === 'comment' ? comments[id] ?? '' : '',
      label: a === 'line' ? lineFields[id]?.label ?? '' : '',
      qty: a === 'line' ? (lineFields[id]?.qty ?? '').trim() : '',
      unit: a === 'line' ? (lineFields[id]?.unit ?? '').trim() : '',
      kind: a === 'proof' ? proofKinds[id] ?? '' : '',
      on: a === 'settle' ? (settleDates[id] ?? '').trim() : '',
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
    inline.reset();
    lock.reset();
    const role = roleOf(settings, user, today);
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
    const before = { editing, amount, date, note, corrects, ref, tags };
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
          setRef(before.ref);
          setTags(before.tags);
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
    fromTemplate.reset();
  };

  // A claim's due: the one fixed when it was settled, else the due its last statement bound it to, else the greater
  // of the floor in force on its date and its figure (its amount at the rate in force on its date for the grade its
  // person had on that date; 'n/a' when none applies) plus its late charge on that floored figure while it is overdue.
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
              fromTemplate.reset();
              return save.mutateAsync({ id: editing, amount, date, note, corrects, ref, tags });
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
          <label>
            Ref <input data-testid="claim-form-ref" value={ref} onChange={field(setRef)} />
          </label>{' '}
          <label>
            Tags <input data-testid="claim-form-tags" placeholder="comma separated" value={tags} onChange={field(setTags)} />
          </label>{' '}
          <button type="submit" data-testid="claim-form-submit" disabled={save.isPending || (lock.isPending && lock.variables?.action === 'lock')}>
            {editing === null ? 'Add claim' : 'Save claim'}
          </button>
          <label>
            Template{' '}
            <select
              data-testid="claim-template-select"
              value={template}
              onChange={(e) => {
                setTemplate(e.target.value);
                fromTemplate.reset();
                save.reset();
              }}
            >
              <option value="">(choose)</option>
              {(templates.data ?? []).map((t) => (
                <option key={t.name} value={t.name}>
                  {t.name}
                </option>
              ))}
            </select>
          </label>{' '}
          <button
            type="button"
            data-testid="claim-template-add"
            disabled={fromTemplate.isPending}
            onClick={() => {
              setRefused(null);
              submitOnce(() => {
                save.reset();
                fromTemplate.reset();
                return fromTemplate.mutateAsync(template);
              });
            }}
          >
            Add from template
          </button>
          {(save.isError || fromTemplate.isError) && (
            <p data-testid="claim-form-error">{(save.error ?? fromTemplate.error)?.message}</p>
          )}
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
                <td data-testid="claim-amount">{fig(c.amount)}</td>
                <td data-testid="claim-date">{c.date}</td>
                <td data-testid="claim-period">{periodOf(periods.data, c.dueBy)?.name ?? ''}</td>
                <td data-testid="claim-note">{c.note}</td>
                <td data-testid="claim-due">
                  {fig(claimDueNow(c, claims.data, rateFor(rates.data, gradeOn(person, c.date), c.date, c.added), settings, today, gradeOn(person, c.date)))}
                </td>
                <td data-testid="claim-due-by">{c.dueBy}</td>
                <td data-testid="claim-overdue">
                  {today === undefined ? '' : isOverdue(c, today, settings, gradeOn(person, c.date)) ? 'yes' : 'no'}
                </td>
                <td data-testid="claim-corrects">{c.corrects ?? ''}</td>
                <td data-testid="claim-ref">{c.ref ?? ''}</td>
                <td data-testid="claim-tags">{shownTags(c.tags)}</td>
                <td data-testid="claim-late-entry">{c.lateEntry ? 'yes' : 'no'}</td>
                <td data-testid="claim-review-late">{c.reviewLate ? 'yes' : 'no'}</td>
                <td data-testid="claim-waived">{c.waived ? 'yes' : 'no'}</td>
                <td data-testid="claim-held">{c.held ? 'yes' : 'no'}</td>
                <td data-testid="claim-stated">{c.stated ? 'yes' : 'no'}</td>
                <td data-testid="claim-status">{c.status}</td>
                <td data-testid="claim-batch">{c.batch ?? ''}</td>
                <td data-testid="claim-opened-by">{c.openedBy ?? ''}</td>
                <td data-testid="claim-proofs">{c.proofs}</td>
                <td data-testid="claim-proofs-receipt">{c.proofs - c.photos}</td>
                <td data-testid="claim-proofs-photo">{c.photos}</td>
                <td data-testid="claim-reminded">{c.reminded ?? ''}</td>
                <td data-testid="claim-split-from">{c.splitFrom ?? ''}</td>
                <td data-testid="claim-deferred">{shownDeferred(c, today) ? 'yes' : 'no'}</td>
                <td data-testid="claim-deferred-until">{c.deferredUntil ?? ''}</td>
                <td data-testid="claim-log">
                  {c.log.map((line, i) => (
                    <div key={i} data-testid="claim-log-line">
                      {line}
                    </div>
                  ))}
                </td>
                <td>
                  <ClaimTimeline sheetId={sheetId} claimId={c.id} />
                </td>
                <td data-testid="claim-comments">
                  {c.comments.map((line, i) => (
                    <div key={i} data-testid="claim-comments-line">
                      {line}
                    </div>
                  ))}
                  <input
                    type="text"
                    data-testid="claim-comment-text"
                    placeholder="comment"
                    value={comments[c.id] ?? ''}
                    onChange={(e) => {
                      const v = e.target.value;
                      setComments((m) => ({ ...m, [c.id]: v }));
                      if (action.variables?.id === c.id) action.reset();
                    }}
                  />{' '}
                  <button
                    type="button"
                    data-testid="claim-comment-add"
                    disabled={action.isPending}
                    onClick={() => act(c.id, 'comment')}
                  >
                    Comment
                  </button>
                </td>
                <td data-testid="claim-lines">
                  {c.lines.map((line, i) => (
                    <div key={i} data-testid="claim-lines-line">
                      {line}
                    </div>
                  ))}
                  {(['label', 'qty', 'unit'] as const).map((k) => (
                    <input
                      key={k}
                      type="text"
                      data-testid={`claim-line-${k}`}
                      placeholder={k}
                      inputMode={k === 'label' ? undefined : k === 'qty' ? 'numeric' : 'decimal'}
                      value={lineFields[c.id]?.[k] ?? ''}
                      onChange={(e) => {
                        const v = e.target.value;
                        setLineFields((m) => ({ ...m, [c.id]: { ...(m[c.id] ?? { label: '', qty: '', unit: '' }), [k]: v } }));
                        if (action.variables?.id === c.id) action.reset();
                      }}
                    />
                  ))}{' '}
                  <button
                    type="button"
                    data-testid="claim-line-add"
                    disabled={action.isPending}
                    onClick={() => act(c.id, 'line')}
                  >
                    Add line
                  </button>
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
                  {(settings?.voidReasons ?? []).length > 0 && (
                    <select
                      data-testid="claim-void-reason"
                      value={voidReasons[c.id] ?? ''}
                      onChange={(e) => {
                        const v = e.target.value;
                        setVoidReasons((r) => ({ ...r, [c.id]: v }));
                        if (action.variables?.id === c.id) action.reset();
                      }}
                    >
                      <option value="">void reason</option>
                      {(settings?.voidReasons ?? []).map((r) => (
                        <option key={r} value={r}>
                          {r}
                        </option>
                      ))}
                    </select>
                  )}{' '}
                  <input
                    type="text"
                    data-testid="claim-settle-date"
                    placeholder="settle as of yyyy-mm-dd"
                    value={settleDates[c.id] ?? ''}
                    onChange={(e) => {
                      const v = e.target.value;
                      setSettleDates((d) => ({ ...d, [c.id]: v }));
                      if (action.variables?.id === c.id) action.reset();
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
                  <select
                    data-testid="claim-proof-kind"
                    value={proofKinds[c.id] ?? ''}
                    onChange={(e) => {
                      setProofKinds((m) => ({ ...m, [c.id]: e.target.value }));
                      if (action.variables?.id === c.id) action.reset();
                    }}
                  >
                    <option value="">(receipt)</option>
                    <option value="receipt">receipt</option>
                    <option value="photo">photo</option>
                  </select>{' '}
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
                  <button
                    type="button"
                    data-testid="claim-waive"
                    disabled={action.isPending}
                    onClick={() => act(c.id, 'waive')}
                  >
                    Waive late charge
                  </button>{' '}
                  <button
                    type="button"
                    data-testid="claim-hold"
                    disabled={action.isPending}
                    onClick={() => act(c.id, 'hold')}
                  >
                    {c.held ? 'Release' : 'Hold'}
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
                    data-testid="claim-settle-part-amount"
                    inputMode="decimal"
                    placeholder="settle part amount"
                    value={settlePartAmounts[c.id] ?? ''}
                    onChange={(e) => {
                      const v = e.target.value;
                      setSettlePartAmounts((m) => ({ ...m, [c.id]: v }));
                      if (action.variables?.id === c.id) action.reset();
                    }}
                  />{' '}
                  <button
                    type="button"
                    data-testid="claim-settle-part"
                    disabled={action.isPending}
                    onClick={() => act(c.id, 'settle-part')}
                  >
                    Settle part
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
                  {inline.isError && inline.variables?.claim.id === c.id && (
                    <span data-testid="claim-error">{inline.error.message}</span>
                  )}
                  {action.isError && action.variables?.id === c.id && (
                    <span data-testid="claim-error">{action.error.message}</span>
                  )}
                  {refused?.id === c.id && <span data-testid="claim-error">{refused.message}</span>}
                </td>
                <td>
                  <input
                    data-testid="claim-inline-amount"
                    inputMode="decimal"
                    placeholder="amount"
                    value={inlineAmounts[c.id] ?? ''}
                    onChange={(e) => {
                      const v = e.target.value;
                      setInlineAmounts((m) => ({ ...m, [c.id]: v }));
                      if (inline.variables?.claim.id === c.id) inline.reset();
                    }}
                  />{' '}
                  <button
                    type="button"
                    data-testid="claim-inline-save"
                    disabled={inline.isPending}
                    onClick={() => saveInline(c)}
                  >
                    Save amount
                  </button>{' '}
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
 *  many were settled and how many refused (gone while it runs), and the number of the sheet's last
 *  batch (a bulk settle that settled at least one; blank when none). */
function BulkSettle({ sheetId }: { sheetId: string }) {
  const selection = useClaimSelection(sheetId);
  const lastBatch = useLastBatch(sheetId);
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
      </button>{' '}
      Last batch <strong data-testid="bulk-settle-batch">{lastBatch.data ?? ''}</strong>
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

/** Move the selected claims to one person together: the server moves, in list order and by the rules of a
 *  single reassign, each that can be moved and leaves the rest; the bar shows how many were moved and how
 *  many refused (gone while it runs). */
function BulkReassign({ sheetId }: { sheetId: string }) {
  const selection = useClaimSelection(sheetId);
  const people = usePeople(sheetId);
  const queryClient = useQueryClient();
  const submitOnce = useSingleFlight();
  const [target, setTarget] = useState('');
  const bulk = useMutation({
    mutationFn: (r: { claimIds: number[]; personId: number | null }) =>
      postJson<{ reassigned: number; refused: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/bulk-reassign`, r),
    onSuccess: () => selection.clear(),
    onSettled: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });
  return (
    <p data-testid="bulk-reassign-bar">
      <select
        data-testid="bulk-reassign-to"
        value={target}
        onChange={(e) => {
          setTarget(e.target.value);
          bulk.reset();
        }}
      >
        <option value="">move to…</option>
        {(people.data ?? []).map((p) => (
          <option key={p.id} value={String(p.id)}>
            {p.name}
          </option>
        ))}
      </select>{' '}
      <button
        type="button"
        data-testid="bulk-reassign"
        disabled={bulk.isPending}
        onClick={() =>
          submitOnce(() => {
            bulk.reset();
            return bulk.mutateAsync({ claimIds: [...selection.ids], personId: target === '' ? null : Number(target) });
          })
        }
      >
        Reassign selected ({selection.ids.size})
      </button>
      {bulk.isSuccess && (
        <>
          {' '}
          Reassigned <strong data-testid="bulk-reassigned">{bulk.data.reassigned}</strong>, refused{' '}
          <strong data-testid="bulk-reassign-refused">{bulk.data.refused}</strong>
        </>
      )}
      {bulk.isError && <span data-testid="bulk-reassign-error">{bulk.error.message}</span>}
    </p>
  );
}

/** Re-date the selected claims together to the date typed in the bar: the server re-dates, in list order and by
 *  the rules of a single re-date, each that can be re-dated and leaves the rest; the bar shows how many were
 *  re-dated and how many refused (gone while it runs). */
function BulkRedate({ sheetId }: { sheetId: string }) {
  const selection = useClaimSelection(sheetId);
  const queryClient = useQueryClient();
  const submitOnce = useSingleFlight();
  const [date, setDate] = useState('');
  const bulk = useMutation({
    mutationFn: (r: { claimIds: number[]; date: string }) =>
      postJson<{ redated: number; refused: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/bulk-redate`, r),
    onSuccess: () => selection.clear(),
    onSettled: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });
  return (
    <p data-testid="bulk-redate-bar">
      <input
        type="text"
        data-testid="bulk-redate-date"
        placeholder="new date yyyy-mm-dd"
        value={date}
        onChange={(e) => {
          setDate(e.target.value);
          bulk.reset();
        }}
      />{' '}
      <button
        type="button"
        data-testid="bulk-redate"
        disabled={bulk.isPending}
        onClick={() =>
          submitOnce(() => {
            bulk.reset();
            return bulk.mutateAsync({ claimIds: [...selection.ids], date: date.trim() });
          })
        }
      >
        Re-date selected ({selection.ids.size})
      </button>
      {bulk.isSuccess && (
        <>
          {' '}
          Re-dated <strong data-testid="bulk-redated">{bulk.data.redated}</strong>, refused{' '}
          <strong data-testid="bulk-redate-refused">{bulk.data.refused}</strong>
        </>
      )}
      {bulk.isError && <span data-testid="bulk-redate-error">{bulk.error.message}</span>}
    </p>
  );
}

/** Void the selected claims together with the reason chosen in the bar (offered while the sheet has void reasons):
 *  the server voids, in list order and by the rules of a single void, each that can be voided and leaves the rest;
 *  the bar shows how many were voided and how many refused (gone while it runs). */
function BulkVoid({ sheetId }: { sheetId: string }) {
  const selection = useClaimSelection(sheetId);
  const voidReasons = useSettings(sheetId).data?.voidReasons ?? [];
  const queryClient = useQueryClient();
  const submitOnce = useSingleFlight();
  const [reason, setReason] = useState('');
  const bulk = useMutation({
    mutationFn: (r: { claimIds: number[]; reason: string }) =>
      postJson<{ voided: number; refused: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/bulk-void`, r),
    onSuccess: () => {
      selection.clear();
      setReason('');
    },
    onSettled: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });
  return (
    <p data-testid="bulk-void-bar">
      {voidReasons.length > 0 && (
        <>
          <select
            data-testid="bulk-void-reason"
            value={reason}
            onChange={(e) => {
              setReason(e.target.value);
              bulk.reset();
            }}
          >
            <option value="">void reason</option>
            {voidReasons.map((r) => (
              <option key={r} value={r}>
                {r}
              </option>
            ))}
          </select>{' '}
        </>
      )}
      <button
        type="button"
        data-testid="bulk-void"
        disabled={bulk.isPending}
        onClick={() =>
          submitOnce(() => {
            bulk.reset();
            return bulk.mutateAsync({ claimIds: [...selection.ids], reason });
          })
        }
      >
        Void selected ({selection.ids.size})
      </button>
      {bulk.isSuccess && (
        <>
          {' '}
          Voided <strong data-testid="bulk-voided">{bulk.data.voided}</strong>, refused{' '}
          <strong data-testid="bulk-void-refused">{bulk.data.refused}</strong>
        </>
      )}
      {bulk.isError && <span data-testid="bulk-void-error">{bulk.error.message}</span>}
    </p>
  );
}

/** The sheet's claimed-total: everyone's claimed added up. */
function ClaimedTotal({ sheetId }: { sheetId: string }) {
  const fig = useFigure();
  const claims = useClaims(sheetId);
  const people = usePeople(sheetId);
  const today = useSheetToday(sheetId).data?.today;
  return (
    <p>
      Claimed total: <strong data-testid="rollup-claimed-total">{fig(claimed(claims.data ?? [], claimedHoldsById(people.data, today), today))}</strong>
    </p>
  );
}

export const contributions = [
  PersonColumnHead.fill({ order: 10, Component: () => <Col id="claimed"><th>Claimed</th></Col> }),
  PersonCells.fill({ order: 10, Component: PersonClaimed }),
  PersonDetail.fill({ order: 10, Component: PersonClaims }),
  SheetBody.fill({ order: 15, Component: BulkOpen }),
  SheetBody.fill({ order: 16, Component: BulkSettle }),
  SheetBody.fill({ order: 17, Component: BulkReassign }),
  SheetBody.fill({ order: 17.5, Component: BulkRedate }),
  SheetBody.fill({ order: 17.6, Component: BulkVoid }),
  SheetBody.fill({ order: 20, Component: ({ sheetId }) => <ClaimedTotal sheetId={sheetId} /> }),
];
