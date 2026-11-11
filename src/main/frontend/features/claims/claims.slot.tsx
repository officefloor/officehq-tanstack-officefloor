import { Fragment, useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { getJson, postJson, putJson } from '../../api/http';
import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import { gradeOn, retiredHolds, peopleKey, usePeople } from '../people/people';
import type { Person } from '../people/people';
import { useUser } from '../session/session';
import { AMOUNT, CLAIM_AMOUNT, CLAIM_NUMBER, claimsKey, isPendingLate, isReviewLate, showsDeferred, sumAmounts, useClaims } from './claims';
import type { Claim } from './claims';
import { selectClaim, useSelectedClaims } from './selection';
import { PCT, claimDue, rateOn, useRates } from '../rates/rates';
import { periodOf, usePeriods } from '../periods/periods';
import { PROOF_KINDS, useFloor, useLate, usePendingSla, useReviewSla } from '../access/access';
import { useSheetToday } from '../sheet/sheet';
import { dueBy, isOverdue } from '../overdue/overdue';
import { useVoidReasons } from '../voidreasons/voidReasons';
import { useTemplates } from '../templates/templates';
import { Col } from '../columns/columns';

// A clerk's settle above the clerk limit leaves the claim PENDING; a steward approves it or rejects
// it with a reason (typed in the row). A steward re-settles a SETTLED claim at its figure today.
// A claim is settled as of a date typed beside its settle button (blank: today), its due frozen as it stood then.
// A steward reviews a claim in REVIEW (above the review threshold when opened): it is OPEN, numbered once settled.
// A DRAFT is deleted, and restored to a DRAFT on the sheet's today it was deleted.
type Action = 'open' | 'review' | 'settle' | 'void' | 'approve' | 'reject' | 'resettle' | 'delete' | 'restore';
type Fields = { amount: string; date: string; note: string; corrects: string; ref: string; tags: string };
const EMPTY: Fields = { amount: '', date: '', note: '', corrects: '', ref: '', tags: '' };

const ACTIONS: { action: Action; label: string }[] = [
  { action: 'open', label: 'Open' },
  { action: 'review', label: 'Review' },
  { action: 'settle', label: 'Settle' },
  { action: 'void', label: 'Void' },
  { action: 'approve', label: 'Approve' },
  { action: 'reject', label: 'Reject' },
  { action: 'resettle', label: 'Re-settle' },
  { action: 'delete', label: 'Delete' },
  { action: 'restore', label: 'Restore' },
];


function ClaimsHead() {
  return (
    <>
      <Col name="claimed"><th>Claimed</th></Col>
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
  // The date typed to settle each claim as of ('' or missing: as of the sheet's today), by claim id.
  const [settleOn, setSettleOn] = useState<Record<string, string>>({});
  // The date typed to defer each claim until, by claim id.
  const [deferUntil, setDeferUntil] = useState<Record<string, string>>({});
  // The kind chosen for each claim's next proof ('' or missing: none chosen, a receipt).
  const [proofKind, setProofKind] = useState<Record<string, string>>({});
  // The new date typed to re-date each claim to, by claim id.
  const [newDate, setNewDate] = useState<Record<string, string>>({});
  // The due-by typed beside each claim's set-due-by button.
  const [newDueBy, setNewDueBy] = useState<Record<string, string>>({});
  // The amount typed in each draft's own row, to save as its new amount, by claim id.
  const [inlineAmount, setInlineAmount] = useState<Record<string, string>>({});
  // The amount typed to split off each claim, by claim id.
  const [splitAmount, setSplitAmount] = useState<Record<string, string>>({});
  // The amount typed to part-settle off each claim, by claim id.
  const [partAmount, setPartAmount] = useState<Record<string, string>>({});
  // The person picked to share each claim with and the percent typed for them, by claim id.
  const [shareWith, setShareWith] = useState<Record<string, { personId: string; share: string }>>({});
  // The user typed to assign each REVIEW claim to review, by claim id.
  const [reviewUser, setReviewUser] = useState<Record<string, string>>({});
  const [commentText, setCommentText] = useState<Record<string, string>>({});
  // The line typed to add to each draft (label, quantity, unit amount), by claim id.
  // Each claim's timeline as last shown: its audit lines, newest first.
  const [timelines, setTimelines] = useState<Record<string, string[]>>({});
  const [lineFields, setLineFields] = useState<Record<string, { label: string; qty: string; unit: string }>>({});
  // The template picked to add a draft claim from.
  const [template, setTemplate] = useState('');
  const templates = useTemplates(sheetId).data ?? [];
  const everyone = usePeople(sheetId).data ?? [];
  const others = everyone.filter((p) => p.id !== person.id);
  const nameOf = (id: string) => everyone.find((p) => p.id === id)?.name ?? id;
  const [user] = useUser();
  const rates = useRates(sheetId).data ?? [];
  const periods = usePeriods(sheetId).data ?? [];
  const floor = useFloor(sheetId);
  const today = useSheetToday(sheetId);
  const late = useLate(sheetId);
  const reviewSla = useReviewSla(sheetId);
  const pendingSla = usePendingSla(sheetId);
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
    onError: (_e, { id, body }) => {
      // Only a draft can be edited: an edit of an open (or later) claim is refused on the claim.
      const claim = id === null ? undefined : claims.find((c) => c.id === id);
      if (claim !== undefined && claim.status !== 'DRAFT') {
        setRefused({ id: claim.id, message: 'Only a draft claim can be edited.' });
      } else if (claim !== undefined && claim.lines.length > 0 && body.amount !== claim.amount) {
        // A claim with lines takes their total as its amount: no save of this editor could change
        // it, so the editor is closed (its lock released) and the form is back to adding.
        unlock.mutate(claim.id);
        reset();
        setError('A claim with lines takes their total as its amount.');
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
            : action === 'settle'
              ? { on: (settleOn[id] ?? '').trim() }
              : {},
        user,
      ),
    onSuccess: (_c, { id, action }) => {
      if (action === 'reject') setReasons((r) => ({ ...r, [id]: '' }));
      if (action === 'void') setVoidReasons((r) => ({ ...r, [id]: '' }));
      if (action === 'settle') setSettleOn((d) => ({ ...d, [id]: '' }));
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

  // A steward sets a DRAFT, OPEN or PENDING claim's due-by directly; a holiday added later does not move it.
  const setDueBy = useMutation({
    mutationFn: ({ id, dueBy }: { id: string; dueBy: string }) =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheetId)}/claims/${encodeURIComponent(id)}/due-by`,
        { dueBy },
        user,
      ),
    onSuccess: (_c, { id }) => {
      setNewDueBy((d) => ({ ...d, [id]: '' }));
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (_e, { id }) =>
      setRefused({
        id,
        message: "That claim's due-by cannot be set: a steward sets it on a DRAFT, OPEN or PENDING claim, not in a closed period.",
      }),
  });

  const setClaimDueBy = (id: string) => {
    setRefused(null);
    const dueBy = (newDueBy[id] ?? '').trim();
    if (dueBy === '') {
      setRefused({ id, message: 'Choose the new due-by for the claim.' });
      return;
    }
    setDueBy.mutate({ id, dueBy });
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

  // Part-settle: an amount is split off an OPEN claim and the new claim settled at once, or nothing.
  const settlePart = useMutation({
    mutationFn: ({ id, amount }: { id: string; amount: string }) =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheetId)}/claims/${encodeURIComponent(id)}/settle-part`,
        { amount },
        user,
      ),
    onSuccess: (_c, { id }) => {
      setPartAmount((a) => ({ ...a, [id]: '' }));
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (_e, { id }) =>
      setRefused({
        id,
        message: 'That part cannot be settled: the claim must be OPEN, the part less than its amount, and you may settle it.',
      }),
  });

  const settlePartClaim = (id: string) => {
    setRefused(null);
    const amount = (partAmount[id] ?? '').trim();
    if (!AMOUNT.test(amount)) {
      setRefused({ id, message: 'A part-settle needs an amount with up to two decimals.' });
      return;
    }
    settlePart.mutate({ id, amount });
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

  // A proof, of the kind chosen (none: a receipt), is added to a claim that is not SETTLED or VOID.
  const proof = useMutation({
    mutationFn: ({ id, kind }: { id: string; kind: string }) =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheetId)}/claims/${encodeURIComponent(id)}/proof`,
        { kind },
        user,
      ),
    onSuccess: (_c, { id }) => {
      setProofKind((k) => ({ ...k, [id]: '' }));
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (_e, { id }) => setRefused({ id, message: 'A proof cannot be added to that claim.' }),
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

  // A steward waives an OPEN or PENDING claim's late charge, once.
  const waive = useMutation({
    mutationFn: (id: string) =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheetId)}/claims/${encodeURIComponent(id)}/waive`,
        {},
        user,
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
    onError: (_e, id) =>
      setRefused({ id, message: 'Only a steward can waive the late charge of an OPEN or PENDING claim, once.' }),
  });

  // An OPEN or PENDING claim is held, or released when held.
  const hold = useMutation({
    mutationFn: (id: string) =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheetId)}/claims/${encodeURIComponent(id)}/hold`,
        {},
        user,
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
    onError: (_e, id) =>
      setRefused({ id, message: 'Only an OPEN or PENDING claim can be held or released.' }),
  });

  // A share of a DRAFT, REVIEW, OPEN or PENDING claim with another person, at a percent: refused for its
  // own person, a person already holding one, or past 100.00 in all.
  const share = useMutation({
    mutationFn: ({ id, personId, share }: { id: string; personId: string; share: string }) =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheetId)}/claims/${encodeURIComponent(id)}/share`,
        { personId, share },
        user,
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
    onError: (_e, { id }) =>
      setRefused({
        id,
        message: 'That claim cannot be shared so: not with its own person or one already holding a share, not past 100.00 in all, and not once settled, void or deleted.',
      }),
  });

  // A steward assigns a REVIEW claim to a user to review: refused for a blank user or any other status.
  const assignReview = useMutation({
    mutationFn: ({ id, reviewer }: { id: string; reviewer: string }) =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheetId)}/claims/${encodeURIComponent(id)}/review-assign`,
        { user: reviewer },
        user,
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
    onError: (_e, { id }) =>
      setRefused({ id, message: 'Only a steward can assign a REVIEW claim, to a named user.' }),
  });

  // A comment is kept on a claim in any status, with who wrote it and the sheet's today.
  const comment = useMutation({
    mutationFn: ({ id, text }: { id: string; text: string }) =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheetId)}/claims/${encodeURIComponent(id)}/comments`,
        { text },
        user,
      ),
    onSuccess: (_c, { id }) => {
      setCommentText((t) => ({ ...t, [id]: '' }));
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (_e, { id }) => setRefused({ id, message: 'That claim cannot be commented.' }),
  });

  // A line added to a draft is an edit of it: its amount becomes the lines' total, by the edit's rules.
  const line = useMutation({
    mutationFn: ({ id, label, qty, unit }: { id: string; label: string; qty: string; unit: string }) =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheetId)}/claims/${encodeURIComponent(id)}/lines`,
        { label, qty, unit },
        user,
      ),
    onSuccess: (_c, { id }) => {
      setLineFields((l) => ({ ...l, [id]: { label: '', qty: '', unit: '' } }));
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (_e, { id }) =>
      setRefused({ id, message: 'A line cannot be added now: the claim is not an editable draft, or someone else is editing it.' }),
  });

  const addLine = (id: string) => {
    setRefused(null);
    const f = lineFields[id] ?? { label: '', qty: '', unit: '' };
    const label = f.label.trim();
    const qty = f.qty.trim();
    const unit = f.unit.trim();
    if (label === '') {
      setRefused({ id, message: 'A line needs a label.' });
      return;
    }
    if (!/^\d{1,9}$/.test(qty) || Number(qty) < 1) {
      setRefused({ id, message: 'A quantity is a whole number of at least 1.' });
      return;
    }
    if (!/^\d{1,13}(\.\d{1,2})?$/.test(unit)) {
      setRefused({ id, message: 'A unit amount is a number with up to two decimals.' });
      return;
    }
    line.mutate({ id, label, qty, unit });
  };

  const setLineField = (id: string, key: 'label' | 'qty' | 'unit', value: string) => {
    setLineFields((l) => ({ ...l, [id]: { ...(l[id] ?? { label: '', qty: '', unit: '' }), [key]: value } }));
    setRefused((r) => (r !== null && r.id === id ? null : r));
  };

  // A draft claim from a template: its amount and note as the template has them now, undated, by
  // every rule of the claim form; a refusal shows on the claim form.
  const fromTemplate = useMutation({
    mutationFn: (name: string) =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheetId)}/people/${encodeURIComponent(person.id)}/template-claims`,
        { template: name },
        user,
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
    onError: () => setError('A claim could not be added from that template.'),
  });

  const addFromTemplate = () => {
    setError('');
    setRefused(null);
    if (template === '') {
      setError('Choose a template.');
      return;
    }
    once((done) => fromTemplate.mutate(template, { onSettled: done }));
  };

  const addComment = (id: string) => {
    setRefused(null);
    const text = (commentText[id] ?? '').trim();
    if (text === '') {
      setRefused({ id, message: 'A comment needs some text.' });
      return;
    }
    comment.mutate({ id, text });
  };

  // A claim's timeline: the audit lines about it as the audit file holds them, newest first.
  const timeline = useMutation({
    mutationFn: (id: string) =>
      getJson<{ lines: string[] }>(
        `/api/sheets/${encodeURIComponent(sheetId)}/claims/${encodeURIComponent(id)}/timeline`,
        user,
      ),
    onSuccess: (data, id) => setTimelines((t) => ({ ...t, [id]: data.lines })),
    onError: (_e, id) => setRefused({ id, message: 'The timeline could not be read.' }),
  });

  const showTimeline = (id: string) => {
    setRefused(null);
    setTimelines((t) => ({ ...t, [id]: [] }));
    timeline.mutate(id);
  };

  const remindClaim = (id: string) => {
    setRefused(null);
    remind.mutate(id);
  };

  const waiveClaim = (id: string) => {
    setRefused(null);
    waive.mutate(id);
  };

  const holdClaim = (id: string) => {
    setRefused(null);
    hold.mutate(id);
  };

  const shareClaim = (id: string) => {
    setRefused(null);
    const { personId = '', share: pct = '' } = shareWith[id] ?? {};
    if (personId === '') {
      setRefused({ id, message: 'Choose the person to share the claim with.' });
      return;
    }
    if (!PCT.test(pct.trim()) || Number(pct) <= 0) {
      setRefused({ id, message: 'A share is a percent with up to two decimals, more than 0.00.' });
      return;
    }
    share.mutate({ id, personId, share: pct.trim() });
  };

  const assignReviewClaim = (id: string) => {
    setRefused(null);
    const reviewer = (reviewUser[id] ?? '').trim();
    if (reviewer === '') {
      setRefused({ id, message: 'Name the user to assign the review to.' });
      return;
    }
    assignReview.mutate({ id, reviewer });
  };

  const setShareField = (id: string, field: 'personId' | 'share', value: string) => {
    setShareWith((w) => ({ ...w, [id]: { ...(w[id] ?? { personId: '', share: '' }), [field]: value } }));
    setRefused((r) => (r !== null && r.id === id ? null : r));
  };

  const addProof = (id: string) => {
    setRefused(null);
    proof.mutate({ id, kind: proofKind[id] ?? '' });
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
      <Col name="claimed"><td data-testid="person-claimed">{sumAmounts(claims, () => retiredHolds(person, today), today)}</td></Col>
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
          <select
            data-testid="claim-template-select"
            value={template}
            onChange={(e) => {
              setTemplate(e.target.value);
              setError('');
            }}
          >
            <option value="">Template…</option>
            {templates.map((t) => (
              <option key={t.name} value={t.name}>
                {t.name}
              </option>
            ))}
          </select>{' '}
          <button
            type="button"
            data-testid="claim-template-add"
            disabled={fromTemplate.isPending}
            onClick={addFromTemplate}
          >
            Add from template
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
              <span data-testid="claim-due">{claimDue(c, rateOn(rates, gradeOn(person.grades, c.date), c.date, c.id), floor, late, gradeOn(person.grades, c.date))}</span>{' '}
              <span data-testid="claim-status">{c.status}</span>{' '}
              <span data-testid="claim-corrects">{c.corrects}</span>{' '}
              <span data-testid="claim-ref">{c.ref}</span>{' '}
              <span data-testid="claim-tags">{c.tags.join(', ')}</span>{' '}
              <span data-testid="claim-opened-by">{c.openedBy}</span>{' '}
              <span data-testid="claim-stated">{c.stated ? 'yes' : 'no'}</span>{' '}
              <span data-testid="claim-period">{periodOf(periods, c.dueBy)?.name ?? ''}</span>{' '}
              <span data-testid="claim-due-by">{dueBy(c)}</span>{' '}
              <span data-testid="claim-overdue">{isOverdue(c, today, late, gradeOn(person.grades, c.date)) ? 'yes' : 'no'}</span>{' '}
              <span data-testid="claim-deferred">{showsDeferred(c, today) ? 'yes' : 'no'}</span>{' '}
              <span data-testid="claim-deferred-until">{c.deferredUntil}</span>{' '}
              <span data-testid="claim-proofs">{c.proofs}</span>{' '}
              (receipts <span data-testid="claim-proofs-receipt">{c.proofReceipts}</span>, photos{' '}
              <span data-testid="claim-proofs-photo">{c.proofPhotos}</span>){' '}
              <span data-testid="claim-split-from">{c.splitFrom}</span>{' '}
              <span data-testid="claim-review-late">{isReviewLate(c, today, reviewSla) ? 'yes' : 'no'}</span>{' '}
              <span data-testid="claim-pending-late">{isPendingLate(c, today, pendingSla) ? 'yes' : 'no'}</span>{' '}
              <span data-testid="claim-late-entry">{c.lateEntry ? 'yes' : 'no'}</span>{' '}
              <span data-testid="claim-reminded">{c.remindedOn}</span>{' '}
              <span data-testid="claim-batch">{c.batch}</span>{' '}
              <button type="button" data-testid="claim-remind" onClick={() => remindClaim(c.id)}>
                Remind
              </button>{' '}
              <span data-testid="claim-waived">{c.waived ? 'yes' : 'no'}</span>{' '}
              <button type="button" data-testid="claim-waive" onClick={() => waiveClaim(c.id)}>
                Waive late charge
              </button>{' '}
              <span data-testid="claim-held">{c.held ? 'yes' : 'no'}</span>{' '}
              <button type="button" data-testid="claim-hold" onClick={() => holdClaim(c.id)}>
                {c.held ? 'Release' : 'Hold'}
              </button>{' '}
              <span data-testid="claim-review-assigned">{c.reviewUser}</span>{' '}
              <input
                data-testid="claim-review-user"
                placeholder="Reviewer"
                value={reviewUser[c.id] ?? ''}
                onChange={(e) => {
                  setReviewUser((u) => ({ ...u, [c.id]: e.target.value }));
                  setRefused((r) => (r !== null && r.id === c.id ? null : r));
                }}
              />{' '}
              <button type="button" data-testid="claim-review-assign" onClick={() => assignReviewClaim(c.id)}>
                Assign review
              </button>{' '}
              <span data-testid="claim-shares">{c.shares.map((h) => `${nameOf(h.personId)} ${h.share}`).join(', ')}</span>{' '}
              <select
                data-testid="claim-share-select"
                aria-label="Share with"
                value={shareWith[c.id]?.personId ?? ''}
                onChange={(e) => setShareField(c.id, 'personId', e.target.value)}
              >
                <option value="">Share with…</option>
                {everyone.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name}
                  </option>
                ))}
              </select>{' '}
              <input
                data-testid="claim-share-pct"
                placeholder="Share %"
                inputMode="decimal"
                value={shareWith[c.id]?.share ?? ''}
                onChange={(e) => setShareField(c.id, 'share', e.target.value)}
              />{' '}
              <button type="button" data-testid="claim-share-add" onClick={() => shareClaim(c.id)}>
                Share
              </button>{' '}
              <select
                data-testid="claim-proof-kind"
                aria-label="Proof kind"
                value={proofKind[c.id] ?? ''}
                onChange={(e) => {
                  setProofKind((k) => ({ ...k, [c.id]: e.target.value }));
                  setRefused((r) => (r !== null && r.id === c.id ? null : r));
                }}
              >
                <option value="">kind: receipt</option>
                {PROOF_KINDS.map((kind) => (
                  <option key={kind} value={kind}>
                    {kind}
                  </option>
                ))}
              </select>{' '}
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
                <Fragment key={action}>
                  {action === 'settle' && (
                    <input
                      type="date"
                      data-testid="claim-settle-date"
                      aria-label="Settle as of"
                      value={settleOn[c.id] ?? ''}
                      onChange={(e) => {
                        setSettleOn((d) => ({ ...d, [c.id]: e.target.value }));
                        setRefused((r) => (r !== null && r.id === c.id ? null : r));
                      }}
                    />
                  )}
                  <button type="button" data-testid={`claim-${action}`} onClick={() => act(c.id, action)}>
                    {label}
                  </button>
                </Fragment>
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
                type="date"
                data-testid="claim-due-by-new"
                aria-label="New due-by"
                value={newDueBy[c.id] ?? ''}
                onChange={(e) => {
                  setNewDueBy((d) => ({ ...d, [c.id]: e.target.value }));
                  setRefused((r) => (r !== null && r.id === c.id ? null : r));
                }}
              />{' '}
              <button type="button" data-testid="claim-set-due-by" onClick={() => setClaimDueBy(c.id)}>
                Set due-by
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
              <input
                data-testid="claim-settle-part-amount"
                placeholder="Part to settle"
                inputMode="decimal"
                value={partAmount[c.id] ?? ''}
                onChange={(e) => {
                  setPartAmount((a) => ({ ...a, [c.id]: e.target.value }));
                  setRefused((r) => (r !== null && r.id === c.id ? null : r));
                }}
              />{' '}
              <button type="button" data-testid="claim-settle-part" onClick={() => settlePartClaim(c.id)}>
                Settle part
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
              <input
                data-testid="claim-comment-text"
                placeholder="Comment"
                value={commentText[c.id] ?? ''}
                onChange={(e) => {
                  setCommentText((t) => ({ ...t, [c.id]: e.target.value }));
                  setRefused((r) => (r !== null && r.id === c.id ? null : r));
                }}
              />{' '}
              <button type="button" data-testid="claim-comment-add" onClick={() => addComment(c.id)}>
                Comment
              </button>
              <input
                data-testid="claim-line-label"
                placeholder="Line"
                value={lineFields[c.id]?.label ?? ''}
                onChange={(e) => setLineField(c.id, 'label', e.target.value)}
              />{' '}
              <input
                data-testid="claim-line-qty"
                placeholder="Qty"
                inputMode="numeric"
                value={lineFields[c.id]?.qty ?? ''}
                onChange={(e) => setLineField(c.id, 'qty', e.target.value)}
              />{' '}
              <input
                data-testid="claim-line-unit"
                placeholder="Unit"
                inputMode="decimal"
                value={lineFields[c.id]?.unit ?? ''}
                onChange={(e) => setLineField(c.id, 'unit', e.target.value)}
              />{' '}
              <button type="button" data-testid="claim-line-add" onClick={() => addLine(c.id)}>
                Add line
              </button>
              <ol data-testid="claim-lines">
                {c.lines.map((l, i) => (
                  <li key={i} data-testid="claim-lines-line">
                    {l}
                  </li>
                ))}
              </ol>
              <ol data-testid="claim-comments">
                {c.comments.map((line, i) => (
                  <li key={i} data-testid="claim-comments-line">
                    {line}
                  </li>
                ))}
              </ol>
              <button type="button" data-testid="claim-timeline" onClick={() => showTimeline(c.id)}>
                Timeline
              </button>
              <ol data-testid="claim-timeline-lines">
                {(timelines[c.id] ?? []).map((line, i) => (
                  <li key={i} data-testid="claim-timeline-line">
                    {line}
                  </li>
                ))}
              </ol>
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
