import { useEffect, useRef, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { HttpError } from '../../api/http';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { listPeriods, periodOf, periodsKey } from '../periods/api';
import { claimDue, showDue, type GradeOn, type Late, type Rate } from '../rates/api';
import { isPendingLate, isReviewLate, useNoteThreshold, usePendingSla, useProofKinds, useProofThreshold, usePolicyVersions, useReviewSla, type DatedOn } from '../settings/api';
import { claimFromTemplate, listTemplates, templatesKey } from '../templates/api';
import { addClaim, addClaimLine, claimTimeline, addProof, PROOF_KINDS, proofsOf, claimsKey, commentClaim, listVoidReasons, voidReasonsKey, deferClaim, redateClaim, setClaimDueBy, remindClaim, waiveClaim, assignReview, holdClaim, dueBy, isOverdue, lockClaim, moveClaim, reassignClaim, shareClaim, showShares, showTags, splitClaim, settlePartClaim, unlockClaim, updateClaim, type Claim, type ProofKind, type ClaimAction, type ClaimInput, type ClaimLineInput } from './api';
import { Fig } from '../session/Fig';

const EMPTY: ClaimInput = { amount: '', date: '', note: '', corrects: '', ref: '', tags: '' };

const LABELS: Record<ClaimAction, string> = { open: 'Open', settle: 'Settle', void: 'Void', approve: 'Approve', reject: 'Reject', resettle: 'Re-settle', delete: 'Delete', restore: 'Restore', review: 'Review' };
const DONE: Record<ClaimAction, string> = { open: 'opened', settle: 'settled', void: 'voided', approve: 'approved', reject: 'rejected', resettle: 're-settled', delete: 'deleted', restore: 'restored', review: 'reviewed' };

/** A person's claims: a form that adds a claim or edits one, and the claims newest first, each
 *  with its status, its due (fixed when settled, else at the rate in force on the claim's date of the
 *  person's grade in force then (`gradeOn`), or bound by the person's last statement; none when
 *  there is no such rate; at least the sheet's `floor`; then an overdue one gains its late charge on that floored figure on top, see `charges`), its due-by (fixed when it was added) and whether it is
 *  overdue (OPEN and due-by before the sheet's `today`), whether
 *  it is stated,
 *  the period its due-by falls in (none when no period contains it), its status log (oldest first), who opened it, and buttons that
 *  change its status (the user who opened a claim cannot settle it; a clerk settling one whose due is
 *  above the sheet's clerk limit makes it PENDING, which a steward (or the person's approver) other than its opener approves,
 *  or any steward (or the person's approver) rejects with a reason typed beside it; opening one whose due is above the review threshold in
 *  force on its date makes it REVIEW, unnumbered and counted nowhere, which only a steward's review button
 *  opens), a checkbox that selects it for a bulk action, and for a correction (a negative amount, no floor and no late
 *  charge) the number of the SETTLED claim it corrects. A claim is numbered only when settled or approved (claims
 *  numbered earlier keep theirs): an OPEN, PENDING or REVIEW claim shows none. A DRAFT can be deleted with a button beside it (any other
 *  status, or one in a closed period, refused with an error on the claim): DELETED, it counts nowhere and cannot be
 *  edited, opened or voided, and a restore button makes it a DRAFT again on the sheet's today it was deleted
 *  (refused on a later day). Only a DRAFT can be edited:
 *  editing any other claim, or one due by a day in a closed period, is refused with an error on the claim. Opening a
 *  claim's editor locks it for other users until it is saved or cancelled or the sheet's today changes:
 *  another user's edit button on it is refused likewise, and the editor of a claim the user holds is
 *  open again when the sheet is re-loaded. A role the permission matrix allows (by default a steward) can move a DRAFT, OPEN or
 *  PENDING claim to another person picked beside it (any other refused with an error on the claim), and
 *  re-settle a SETTLED one (anyone else, any other status or a claim in a closed period refused likewise).
 *  An OPEN or PENDING claim can be reminded with a button beside it, once per sheet's today (a second reminder
 *  that day, or any other claim, refused with an error on the claim); the last day it was reminded is shown.
 *  A steward can waive an OPEN or PENDING claim's late charge with a button beside it (anyone else, any other
 *  status, or a claim already waived refused with an error on the claim); whether it is waived is shown.
 *  An OPEN or PENDING claim can be held and released with a toggle beside it (any other status refused with
 *  an error on the claim); held, it still counts in every total; whether it is held is shown.
 *  An OPEN claim can be deferred until a date typed beside it, after the sheet's today (any other claim, date,
 *  or a claim in a closed period refused with an error on the claim); whether it is deferred, and until when, is shown.
 *  Its proofs count is shown, with how many are of each kind, and raised by one with a button beside it, of the
 *  kind chosen beside it (none: a receipt; a SETTLED or VOID claim, or one in a closed period, refused with an
 *  error on the claim); one with no proofs (while the sheet's proof kinds are set: none of one of them) whose
 *  due is above the sheet's proof threshold in force on its date cannot be settled or approved.
 *  A claim can be commented with text typed beside it (an empty comment refused with an error on the claim);
 *  its comments are shown oldest first as `<user> on <date>: <text>`.
 *  A DRAFT can gain a line (a label, a whole quantity of at least 1 and a unit amount of two decimals) typed
 *  beside it, which counts as editing it (refused like an edit, and for a bad line, with an error on the
 *  claim); its lines are shown as `<label> <qty> x <unit>`, and their total is its amount: an edit typing
 *  another amount is refused.
 *  A steward can re-date an OPEN or PENDING claim to a date typed beside it: its due-by and any bound figure
 *  stay, and so do its period and number (anyone else, any other status, a date after its person's retirement,
 *  or a claim in a closed period refused with an error on the claim).
 *  An OPEN claim can be split by an amount typed beside it (more than 0.00, less than its own): it keeps the
 *  rest and is unbound, and a new OPEN claim of that amount, opened by the user, shows its number as split-from
 *  (a PENDING, deferred or settled claim, one in a closed period, or a role that may not open a claim refused
 *  with an error on the claim).
 *  An OPEN claim can be part-settled by an amount typed beside it: the amount is split off as the split does
 *  and the new claim settled at once by the user as the settle does (PENDING above a clerk's limit), the
 *  four-eyes and proof rules judged on the original claim; a refused split or settle refuses it all, with an
 *  error on the claim.
 *  A DRAFT's amount can be changed in its row, by an amount typed beside its edit button: saved as the
 *  claim form saves it (its date, note and corrected claim kept), refused likewise with an error on the claim.
 *  A claim can be shared with another person picked beside it at a share typed there (see `shareClaim`): its
 *  holders show as `<name> <share>`; a refused share shows an error on the claim. */
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
  // The void reason chosen for each claim (blank: none), from the sheet's list.
  const [voidReasons, setVoidReasons] = useState<Record<number, string>>({});
  // The date typed for each claim to settle it as of (blank: the sheet's today).
  const [settleDates, setSettleDates] = useState<Record<number, string>>({});
  const voidReasonList = useQuery({ queryKey: voidReasonsKey(sheetId), queryFn: () => listVoidReasons(sheetId) });

  const save = useMutation({
    mutationFn: (v: { id: number | null; input: ClaimInput }) =>
      v.id === null ? addClaim(sheetId, user, personId, v.input) : updateClaim(sheetId, user, v.id, v.input),
    onSuccess: () => {
      setForm(EMPTY);
      setEditing(null);
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    // A refused save of the editor shows its error and puts the form back to adding, the claim's
    // lock released: the next submit adds a claim rather than re-trying the refused edit.
    onError: (_e, v) => {
      if (v.id === null) return;
      setForm(EMPTY);
      setEditing(null);
      unlocking.mutate(v.id);
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
      moveClaim(sheetId, user, v.id, v.action, (v.action === 'void' ? voidReasons[v.id] : reasons[v.id]) ?? '',
        settleDates[v.id] ?? ''),
    onSuccess: (_, v) => {
      if (v.action === 'reject') setReasons((r) => ({ ...r, [v.id]: '' }));
      if (v.action === 'settle') setSettleDates((d) => ({ ...d, [v.id]: '' }));
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
      sharing.reset();
      deferral.reset();
      reminder.reset();
      waiver.reset();
      assigning.reset();
      holding.reset();
      redating.reset();
      dueSetting.reset();
      proof.reset();
      commenting.reset();
      lining.reset();
      division.reset();
      partSettling.reset();
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
      sharing.reset();
      deferral.reset();
      reminder.reset();
      waiver.reset();
      assigning.reset();
      holding.reset();
      redating.reset();
      dueSetting.reset();
      proof.reset();
      commenting.reset();
      lining.reset();
      division.reset();
      partSettling.reset();
      inline.reset();
      setEditRefused(null);
      locking.reset();
      return reminder.mutateAsync(id);
    });
  };

  const waiver = useMutation({
    mutationFn: (id: number) => waiveClaim(sheetId, user, id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
  });

  // The user typed for each claim to assign its review to.
  const [assignees, setAssignees] = useState<Record<number, string>>({});

  const assigning = useMutation({
    mutationFn: (v: { id: number; to: string }) => assignReview(sheetId, user, v.id, v.to),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
  });

  const assign = (id: number) => {
    const to = assignees[id] ?? '';
    actOnce(() => {
      move.reset();
      reassign.reset();
      sharing.reset();
      deferral.reset();
      reminder.reset();
      waiver.reset();
      assigning.reset();
      holding.reset();
      redating.reset();
      dueSetting.reset();
      proof.reset();
      commenting.reset();
      lining.reset();
      division.reset();
      partSettling.reset();
      inline.reset();
      setEditRefused(null);
      locking.reset();
      return assigning.mutateAsync({ id, to });
    });
  };

  const holding = useMutation({
    mutationFn: (id: number) => holdClaim(sheetId, user, id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
  });

  const toggleHold = (id: number) => {
    actOnce(() => {
      move.reset();
      reassign.reset();
      sharing.reset();
      deferral.reset();
      reminder.reset();
      waiver.reset();
      assigning.reset();
      holding.reset();
      redating.reset();
      dueSetting.reset();
      proof.reset();
      commenting.reset();
      lining.reset();
      division.reset();
      partSettling.reset();
      inline.reset();
      setEditRefused(null);
      locking.reset();
      return holding.mutateAsync(id);
    });
  };

  const waive = (id: number) => {
    actOnce(() => {
      move.reset();
      reassign.reset();
      sharing.reset();
      deferral.reset();
      reminder.reset();
      waiver.reset();
      assigning.reset();
      holding.reset();
      redating.reset();
      dueSetting.reset();
      proof.reset();
      commenting.reset();
      lining.reset();
      division.reset();
      partSettling.reset();
      inline.reset();
      setEditRefused(null);
      locking.reset();
      return waiver.mutateAsync(id);
    });
  };

  // The new date typed for each claim to re-date it to.
  // Each claim's timeline as last read from the audit file (absent: not shown); re-read on each press.
  const [timelines, setTimelines] = useState<Record<number, string[]>>({});
  const timeline = useMutation({
    mutationFn: (id: number) => claimTimeline(sheetId, id),
    onMutate: (id) => setTimelines((t) => { const rest = { ...t }; delete rest[id]; return rest; }),
    onSuccess: (lines, id) => setTimelines((t) => ({ ...t, [id]: lines })),
  });

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
      sharing.reset();
      deferral.reset();
      reminder.reset();
      waiver.reset();
      assigning.reset();
      holding.reset();
      redating.reset();
      dueSetting.reset();
      proof.reset();
      commenting.reset();
      lining.reset();
      division.reset();
      partSettling.reset();
      inline.reset();
      setEditRefused(null);
      locking.reset();
      return redating.mutateAsync({ id, date });
    });
  };

  // The due-by typed for each claim to set it to.
  const [dueBys, setDueBys] = useState<Record<number, string>>({});

  const dueSetting = useMutation({
    mutationFn: (v: { id: number; date: string }) => setClaimDueBy(sheetId, user, v.id, v.date),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
  });

  const setDueBy = (id: number) => {
    const date = (dueBys[id] ?? '').trim();
    actOnce(() => {
      move.reset();
      reassign.reset();
      sharing.reset();
      deferral.reset();
      reminder.reset();
      waiver.reset();
      assigning.reset();
      holding.reset();
      redating.reset();
      dueSetting.reset();
      proof.reset();
      commenting.reset();
      lining.reset();
      division.reset();
      partSettling.reset();
      inline.reset();
      setEditRefused(null);
      locking.reset();
      return dueSetting.mutateAsync({ id, date });
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

  // The amount typed for each claim to part-settle.
  const [partAmounts, setPartAmounts] = useState<Record<number, string>>({});

  const partSettling = useMutation({
    mutationFn: (v: { id: number; amount: string }) => settlePartClaim(sheetId, user, v.id, v.amount),
    onSuccess: (_, v) => {
      setPartAmounts((a) => ({ ...a, [v.id]: '' }));
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });

  const split = (id: number) => {
    const amount = (splitAmounts[id] ?? '').trim();
    actOnce(() => {
      move.reset();
      reassign.reset();
      sharing.reset();
      deferral.reset();
      reminder.reset();
      waiver.reset();
      assigning.reset();
      holding.reset();
      redating.reset();
      dueSetting.reset();
      proof.reset();
      commenting.reset();
      lining.reset();
      division.reset();
      partSettling.reset();
      inline.reset();
      setEditRefused(null);
      locking.reset();
      return division.mutateAsync({ id, amount });
    });
  };

  const settlePart = (id: number) => {
    const amount = (partAmounts[id] ?? '').trim();
    actOnce(() => {
      move.reset();
      reassign.reset();
      sharing.reset();
      deferral.reset();
      reminder.reset();
      waiver.reset();
      assigning.reset();
      holding.reset();
      redating.reset();
      dueSetting.reset();
      proof.reset();
      commenting.reset();
      lining.reset();
      division.reset();
      partSettling.reset();
      inline.reset();
      setEditRefused(null);
      locking.reset();
      return partSettling.mutateAsync({ id, amount });
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
      sharing.reset();
      deferral.reset();
      reminder.reset();
      waiver.reset();
      assigning.reset();
      holding.reset();
      redating.reset();
      dueSetting.reset();
      proof.reset();
      commenting.reset();
      lining.reset();
      division.reset();
      partSettling.reset();
      inline.reset();
      setEditRefused(null);
      locking.reset();
      return inline.mutateAsync({ id: c.id, input });
    });
  };

  // The proof kind chosen for each claim ('': none, a receipt).
  const [proofKinds, setProofKinds] = useState<Record<number, ProofKind | ''>>({});

  const proof = useMutation({
    mutationFn: (id: number) => addProof(sheetId, user, id, proofKinds[id] ?? ''),
    onSuccess: (_, id) => {
      setProofKinds((k) => ({ ...k, [id]: '' }));
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });

  const prove = (id: number) => {
    actOnce(() => {
      move.reset();
      reassign.reset();
      sharing.reset();
      deferral.reset();
      reminder.reset();
      waiver.reset();
      assigning.reset();
      holding.reset();
      redating.reset();
      dueSetting.reset();
      proof.reset();
      commenting.reset();
      lining.reset();
      division.reset();
      partSettling.reset();
      inline.reset();
      setEditRefused(null);
      locking.reset();
      return proof.mutateAsync(id);
    });
  };

  // The comment typed for each claim.
  const [commentTexts, setCommentTexts] = useState<Record<number, string>>({});

  const commenting = useMutation({
    mutationFn: (v: { id: number; text: string }) => commentClaim(sheetId, user, v.id, v.text),
    onSuccess: (_, v) => {
      setCommentTexts((t) => ({ ...t, [v.id]: '' }));
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });

  const comment = (id: number) => {
    const text = (commentTexts[id] ?? '').trim();
    actOnce(() => {
      move.reset();
      reassign.reset();
      sharing.reset();
      deferral.reset();
      reminder.reset();
      waiver.reset();
      assigning.reset();
      holding.reset();
      redating.reset();
      dueSetting.reset();
      proof.reset();
      commenting.reset();
      lining.reset();
      division.reset();
      partSettling.reset();
      inline.reset();
      setEditRefused(null);
      locking.reset();
      return commenting.mutateAsync({ id, text });
    });
  };

  // The line typed for each claim.
  const [lineInputs, setLineInputs] = useState<Record<number, ClaimLineInput>>({});
  const lineOf = (id: number): ClaimLineInput => lineInputs[id] ?? { label: '', qty: '', unit: '' };

  const lining = useMutation({
    mutationFn: (v: { id: number; line: ClaimLineInput }) => addClaimLine(sheetId, user, v.id, v.line),
    onSuccess: (_, v) => {
      setLineInputs((t) => ({ ...t, [v.id]: { label: '', qty: '', unit: '' } }));
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });

  const addLine = (id: number) => {
    const line = lineOf(id);
    actOnce(() => {
      move.reset();
      reassign.reset();
      sharing.reset();
      deferral.reset();
      reminder.reset();
      waiver.reset();
      assigning.reset();
      holding.reset();
      redating.reset();
      dueSetting.reset();
      proof.reset();
      commenting.reset();
      lining.reset();
      division.reset();
      partSettling.reset();
      inline.reset();
      setEditRefused(null);
      locking.reset();
      return lining.mutateAsync({ id, line: { label: line.label.trim(), qty: line.qty.trim(), unit: line.unit.trim() } });
    });
  };

  const proofThreshold = useProofThreshold(sheetId);
  // Whether a claim needs a proof before it is settled or approved: none yet (or none of one of the sheet's
  // proof kinds), and its due above the threshold.
  const requiredKinds = useProofKinds(sheetId);
  const needsProof = (c: Claim) => {
    const threshold = proofThreshold(gradeOn(c.date), c.date);
    const due = claimDue(c, rates, gradeOn, floor, charges);
    const missing = c.proofs === 0 || requiredKinds.some((kind) => proofsOf(c, kind) === 0);
    return missing && threshold !== null && due !== null && Number(due) > Number(threshold);
  };

  const noteThreshold = useNoteThreshold(sheetId);
  const reviewSla = useReviewSla(sheetId);
  const policy = usePolicyVersions(sheetId);
  const pendingSla = usePendingSla(sheetId);
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

  // The person picked for each claim to share it with, and the share typed.
  const [shareWith, setShareWith] = useState<Record<number, string>>({});
  const [sharePcts, setSharePcts] = useState<Record<number, string>>({});

  const sharing = useMutation({
    mutationFn: (v: { id: number; with: number; share: string }) => shareClaim(sheetId, user, v.id, v.with, v.share),
    onSuccess: (_, v) => {
      setSharePcts((s) => ({ ...s, [v.id]: '' }));
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });

  const share = (id: number) => {
    const to = Number(shareWith[id] ?? people[0]?.id ?? NaN);
    const pct = (sharePcts[id] ?? '').trim();
    actOnce(() => {
      move.reset();
      reassign.reset();
      sharing.reset();
      deferral.reset();
      reminder.reset();
      waiver.reset();
      assigning.reset();
      holding.reset();
      redating.reset();
      dueSetting.reset();
      proof.reset();
      commenting.reset();
      lining.reset();
      division.reset();
      partSettling.reset();
      inline.reset();
      setEditRefused(null);
      locking.reset();
      return sharing.mutateAsync({ id, with: to, share: pct });
    });
  };

  const moveTo = (id: number) => {
    const to = Number(targets[id] ?? others[0]?.id ?? NaN);
    actOnce(() => {
      move.reset();
      reassign.reset();
      sharing.reset();
      deferral.reset();
      reminder.reset();
      waiver.reset();
      assigning.reset();
      holding.reset();
      redating.reset();
      dueSetting.reset();
      proof.reset();
      commenting.reset();
      lining.reset();
      division.reset();
      partSettling.reset();
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
      sharing.reset();
      deferral.reset();
      reminder.reset();
      waiver.reset();
      assigning.reset();
      holding.reset();
      redating.reset();
      dueSetting.reset();
      proof.reset();
      commenting.reset();
      lining.reset();
      division.reset();
      partSettling.reset();
      inline.reset();
      setEditRefused(null);
      locking.reset();
      return move.mutateAsync({ id, action });
    });
  };

  const change = (patch: Partial<ClaimInput>) => {
    setForm((f) => ({ ...f, ...patch }));
    save.reset();
    fromTemplate.reset();
  };

  // A DRAFT claim from the template picked beside the form: its amount and note as they stand now,
  // dated like a claim with no date, refused like the form (its error shown as the form's).
  const templates = useQuery({ queryKey: templatesKey(sheetId), queryFn: () => listTemplates(sheetId) });
  const [template, setTemplate] = useState('');
  const fromTemplate = useMutation({
    mutationFn: (name: string) => claimFromTemplate(sheetId, user, personId, name),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
  });

  const addFromTemplate = () => {
    const name = template;
    actOnce(() => {
      save.reset();
      fromTemplate.reset();
      move.reset();
      reassign.reset();
      sharing.reset();
      deferral.reset();
      reminder.reset();
      waiver.reset();
      assigning.reset();
      holding.reset();
      redating.reset();
      dueSetting.reset();
      proof.reset();
      commenting.reset();
      lining.reset();
      division.reset();
      partSettling.reset();
      inline.reset();
      setEditRefused(null);
      return fromTemplate.mutateAsync(name);
    });
  };

  const open = (c: Claim) => {
    move.reset();
    reassign.reset();
    sharing.reset();
    deferral.reset();
    reminder.reset();
    waiver.reset();
    assigning.reset();
    holding.reset();
    redating.reset();
    dueSetting.reset();
    proof.reset();
    commenting.reset();
    lining.reset();
    division.reset();
    partSettling.reset();
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
            fromTemplate.reset();
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
            : 'A claim needs an amount with up to two decimals; a negative one (a correction) must name, by number, a SETTLED claim of this person; a reference cannot be one already used on the sheet; a claim with lines keeps their total as its amount.'}</span>
        )}
      </form>
      <div>
        <select data-testid="claim-template-select" value={template}
          onChange={(e) => {
            setTemplate(e.target.value);
            fromTemplate.reset();
          }}>
          <option value="">Pick a template</option>
          {(templates.data ?? []).map((t) => (
            <option key={t.name} value={t.name}>{t.name}</option>
          ))}
        </select>{' '}
        <button type="button" data-testid="claim-template-add" disabled={fromTemplate.isPending}
          onClick={addFromTemplate}>
          Add from template
        </button>
        {fromTemplate.isError && !save.isError && (
          <span data-testid="claim-form-error">{fromTemplate.error instanceof HttpError && fromTemplate.error.status === 423
            ? 'A claim due by a day inside a closed period cannot be added.'
            : fromTemplate.error instanceof HttpError && fromTemplate.error.status === 403
            ? 'Your role may not change anything on this sheet.'
            : 'Pick a template; its claim is refused by the claim form\'s rules.'}</span>
        )}
      </div>
      <table>
        <tbody>
          {claims.map((c) => (
            <tr key={c.id} data-testid={`claim-row-${c.id}`}>
              <td>
                <input type="checkbox" data-testid="claim-select" checked={selected.has(c.id)}
                  onChange={(e) => onSelect(c.id, e.target.checked)} />
              </td>
              <td data-testid="claim-number">{c.number ?? ''}</td>
              <td data-testid="claim-amount"><Fig>{Number(c.amount).toFixed(2)}</Fig></td>
              <td data-testid="claim-date">{c.date}</td>
              <td data-testid="claim-note">{c.note}</td>
              <td data-testid="claim-corrects">{c.corrects ?? ''}</td>
              <td data-testid="claim-ref">{c.ref ?? ''}</td>
              <td data-testid="claim-tags">{showTags(c.tags)}</td>
              <td>Policy <span data-testid="claim-policy">{policy.on(c.date)}</span></td>
              <td data-testid="claim-due"><Fig>{showDue(claimDue(c, rates, gradeOn, floor, charges))}</Fig></td>
              <td>
                Due by <span data-testid="claim-due-by">{dueBy(c)}</span>{' '}
                overdue <span data-testid="claim-overdue">{isOverdue(c, today, charges.lateFrom(c)) ? 'yes' : 'no'}</span>
              </td>
              <td data-testid="claim-status">{c.status}</td>
              <td>Review late <span data-testid="claim-review-late">{isReviewLate(c, today, reviewSla) ? 'yes' : 'no'}</span></td>
              <td>Pending late <span data-testid="claim-pending-late">{isPendingLate(c, today, pendingSla) ? 'yes' : 'no'}</span></td>
              <td>Batch <span data-testid="claim-batch">{c.settleBatch ?? ''}</span></td>
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
                Waived <span data-testid="claim-waived">{c.waived ? 'yes' : 'no'}</span>{' '}
                <button type="button" data-testid="claim-waive" disabled={waiver.isPending}
                  onClick={() => waive(c.id)}>
                  Waive late charge
                </button>
              </td>
              <td>
                Review by <span data-testid="claim-review-assigned">{c.reviewAssignee ?? ''}</span>{' '}
                <input data-testid="claim-review-user" value={assignees[c.id] ?? ''}
                  onChange={(e) => {
                    setAssignees((a) => ({ ...a, [c.id]: e.target.value }));
                    if (assigning.variables?.id === c.id) assigning.reset();
                  }} />{' '}
                <button type="button" data-testid="claim-review-assign" disabled={assigning.isPending}
                  onClick={() => assign(c.id)}>
                  Assign review
                </button>
              </td>
              <td>
                Held <span data-testid="claim-held">{c.held ? 'yes' : 'no'}</span>{' '}
                <button type="button" data-testid="claim-hold" disabled={holding.isPending}
                  onClick={() => toggleHold(c.id)}>
                  {c.held ? 'Release' : 'Hold'}
                </button>
              </td>
              <td>
                Proofs <span data-testid="claim-proofs">{c.proofs ?? 0}</span>{' '}
                ({PROOF_KINDS.map((kind, i) => (
                  <span key={kind}>{i > 0 && ', '}{kind} <span data-testid={`claim-proofs-${kind}`}>{proofsOf(c, kind)}</span></span>
                ))}){' '}
                <select data-testid="claim-proof-kind" value={proofKinds[c.id] ?? ''}
                  onChange={(e) => {
                    setProofKinds((k) => ({ ...k, [c.id]: e.target.value as ProofKind | '' }));
                    if (proof.variables === c.id) proof.reset();
                  }}>
                  <option value="">Kind (receipt)</option>
                  {PROOF_KINDS.map((kind) => <option key={kind} value={kind}>{kind}</option>)}
                </select>{' '}
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
                <input data-testid="claim-settle-part-amount" placeholder="Part amount" value={partAmounts[c.id] ?? ''}
                  onChange={(e) => {
                    setPartAmounts((a) => ({ ...a, [c.id]: e.target.value }));
                    if (partSettling.variables?.id === c.id) partSettling.reset();
                  }} />{' '}
                <button type="button" data-testid="claim-settle-part" disabled={partSettling.isPending}
                  onClick={() => settlePart(c.id)}>
                  Settle part
                </button>
              </td>
              <td data-testid="claim-period">{periodOf(periods.data, c.dueBy)?.name ?? ''}</td>
              <td>
                {(c.log ?? []).map((line, i) => <div key={i} data-testid="claim-log-line">{line}</div>)}
                <button type="button" data-testid="claim-timeline" onClick={() => timeline.mutate(c.id)}>
                  Timeline
                </button>
                {(timelines[c.id] ?? []).map((line, i) => <div key={i} data-testid="claim-timeline-line">{line}</div>)}
              </td>
              <td>
                {(c.comments ?? []).map((line, i) => <div key={i} data-testid="claim-comments-line">{line}</div>)}
                <input data-testid="claim-comment-text" placeholder="Comment" value={commentTexts[c.id] ?? ''}
                  onChange={(e) => {
                    setCommentTexts((t) => ({ ...t, [c.id]: e.target.value }));
                    if (commenting.variables?.id === c.id) commenting.reset();
                  }} />{' '}
                <button type="button" data-testid="claim-comment-add" disabled={commenting.isPending}
                  onClick={() => comment(c.id)}>
                  Comment
                </button>
              </td>
              <td>
                {(c.lines ?? []).map((line, i) => <div key={i} data-testid="claim-lines-line">{line}</div>)}
                {(['label', 'qty', 'unit'] as const).map((k) => (
                  <input key={k} data-testid={`claim-line-${k}`} size={k === 'label' ? 10 : 5}
                    placeholder={k === 'label' ? 'Line label' : k === 'qty' ? 'Qty' : 'Unit amount'}
                    value={lineOf(c.id)[k]}
                    onChange={(e) => {
                      const value = e.target.value;
                      setLineInputs((t) => ({ ...t, [c.id]: { ...lineOf(c.id), ...t[c.id], [k]: value } }));
                      if (lining.variables?.id === c.id) lining.reset();
                    }} />
                ))}{' '}
                <button type="button" data-testid="claim-line-add" disabled={lining.isPending}
                  onClick={() => addLine(c.id)}>
                  Add line
                </button>
              </td>
              <td>
                {(['open', 'settle', 'void', 'approve', 'review', 'resettle', 'delete', 'restore'] as const).map((a) => (
                  <button key={a} type="button" data-testid={`claim-${a}`} disabled={move.isPending}
                    onClick={() => act(c.id, a)}>
                    {LABELS[a]}
                  </button>
                ))}{' '}
                <input type="date" data-testid="claim-settle-date" title="Settle as of" value={settleDates[c.id] ?? ''}
                  onChange={(e) => {
                    setSettleDates((d) => ({ ...d, [c.id]: e.target.value }));
                    if (move.variables?.id === c.id) move.reset();
                  }} />{' '}
                <select data-testid="claim-void-reason" value={voidReasons[c.id] ?? ''}
                  onChange={(e) => {
                    setVoidReasons((r) => ({ ...r, [c.id]: e.target.value }));
                    if (move.variables?.id === c.id) move.reset();
                  }}>
                  <option value="">No void reason</option>
                  {(voidReasonList.data ?? []).map((r) => <option key={r.reason} value={r.reason}>{r.reason}</option>)}
                </select>{' '}
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
                <input type="date" data-testid="claim-due-by-new" value={dueBys[c.id] ?? ''}
                  onChange={(e) => {
                    setDueBys((d) => ({ ...d, [c.id]: e.target.value }));
                    if (dueSetting.variables?.id === c.id) dueSetting.reset();
                  }} />{' '}
                <button type="button" data-testid="claim-set-due-by" disabled={dueSetting.isPending}
                  onClick={() => setDueBy(c.id)}>
                  Set due-by
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
                Shared with <span data-testid="claim-shares">{showShares(c)}</span>{' '}
                <select data-testid="claim-share-select" value={shareWith[c.id] ?? String(people[0]?.id ?? '')}
                  onChange={(e) => {
                    setShareWith((t) => ({ ...t, [c.id]: e.target.value }));
                    if (sharing.variables?.id === c.id) sharing.reset();
                  }}>
                  {people.map((p) => <option key={p.id} value={String(p.id)}>{p.name}</option>)}
                </select>{' '}
                <input data-testid="claim-share-pct" placeholder="Share %" size={6} value={sharePcts[c.id] ?? ''}
                  onChange={(e) => {
                    setSharePcts((s) => ({ ...s, [c.id]: e.target.value }));
                    if (sharing.variables?.id === c.id) sharing.reset();
                  }} />{' '}
                <button type="button" data-testid="claim-share-add" disabled={sharing.isPending || people.length === 0}
                  onClick={() => share(c.id)}>
                  Share
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
                    : move.error instanceof HttpError && move.error.status === 400 && move.variables.action === 'void'
                    ? 'A void needs a reason chosen from the void reasons.'
                    : move.error instanceof HttpError && move.error.status === 400
                    ? 'A rejection needs a reason.'
                    : (move.variables.action === 'settle' || move.variables.action === 'approve') && needsProof(c)
                    ? `A claim whose due is above the proof threshold needs a proof${requiredKinds.length > 0 ? ` of each kind (${requiredKinds.join(', ')})` : ''} first.`
                    : move.variables.action === 'open' && c.status === 'DRAFT' && needsNote(c)
                    ? 'A claim whose due is above the note threshold needs a note before it is opened.'
                    : move.variables.action === 'settle' && (settleDates[c.id] ?? '') !== ''
                    ? 'A claim can be settled only as of a date on or before the sheet\'s today, on which it already existed.'
                    : c.deferred && move.variables.action === 'settle'
                    ? `A deferred claim cannot be settled until ${c.deferredUntil}.`
                    : c.status === 'OPEN' && move.variables.action === 'settle'
                    ? 'The user who opened a claim cannot settle it, nor can a role the permissions do not allow to settle.'
                    : c.status === 'PENDING' && move.variables.action === 'approve'
                    ? 'Only a steward or the person\'s approver who did not open or last edit a claim can approve it.'
                    : c.status === 'REVIEW' && move.variables.action === 'review'
                    ? 'Only a steward can review a claim.'
                    : c.status === 'REVIEW' && move.variables.action === 'open'
                    ? 'A claim in REVIEW is opened by a steward reviewing it.'
                    : c.status === 'PENDING' && move.variables.action === 'reject'
                    ? 'Only a steward or the person\'s approver can reject a claim.'
                    : c.status === 'SETTLED' && move.variables.action === 'resettle'
                    ? 'Only a steward can re-settle a claim.'
                    : c.status === 'DELETED' && move.variables.action === 'restore'
                    ? 'A deleted claim can be restored only on the day it was deleted.'
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
                {waiver.isError && waiver.variables === c.id && (
                  <span data-testid="claim-error">{waiver.error instanceof HttpError && waiver.error.status === 409
                    ? (c.waived
                      ? 'The claim\'s late charge was already waived.'
                      : c.status === 'OPEN' || c.status === 'PENDING'
                      ? 'Only a steward can waive a claim\'s late charge.'
                      : `A ${c.status} claim's late charge cannot be waived.`)
                    : 'Your role may not waive a claim\'s late charge.'}</span>
                )}
                {holding.isError && holding.variables === c.id && (
                  <span data-testid="claim-error">{holding.error instanceof HttpError && holding.error.status === 409
                    ? `A ${c.status} claim cannot be ${c.held ? 'released' : 'held'}.`
                    : 'Your role may not hold a claim.'}</span>
                )}
                {assigning.isError && assigning.variables?.id === c.id && (
                  <span data-testid="claim-error">{assigning.error instanceof HttpError && assigning.error.status === 400
                    ? 'A review needs a user to be assigned to.'
                    : assigning.error instanceof HttpError && assigning.error.status === 409
                    ? (c.status === 'REVIEW'
                      ? 'Only a steward can assign a claim\'s review.'
                      : `A ${c.status} claim's review cannot be assigned.`)
                    : 'Your role may not assign a claim\'s review.'}</span>
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
                {dueSetting.isError && dueSetting.variables?.id === c.id && (
                  <span data-testid="claim-error">{dueSetting.error instanceof HttpError && dueSetting.error.status === 423
                    ? 'A claim due by a day inside a closed period cannot have its due-by set.'
                    : dueSetting.error instanceof HttpError && dueSetting.error.status === 400
                    ? 'A due-by needs to be a date.'
                    : c.status === 'DRAFT' || c.status === 'OPEN' || c.status === 'PENDING'
                    ? 'Only a steward can set a claim\'s due-by.'
                    : `A ${c.status} claim's due-by cannot be set.`}</span>
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
                {partSettling.isError && partSettling.variables?.id === c.id && (
                  <span data-testid="claim-error">{partSettling.error instanceof HttpError && partSettling.error.status === 423
                    ? 'A claim due by a day inside a closed period cannot be part-settled.'
                    : partSettling.error instanceof HttpError && partSettling.error.status === 400
                    ? `A part-settle needs an amount more than 0.00 and less than ${Number(c.amount).toFixed(2)}.`
                    : c.status === 'OPEN' && !c.deferred
                    ? 'The part cannot be split off or settled: your role, four eyes or a missing proof refuses it.'
                    : `A ${c.deferred ? 'deferred' : c.status} claim cannot be part-settled.`}</span>
                )}
                {proof.isError && proof.variables === c.id && (
                  <span data-testid="claim-error">{proof.error instanceof HttpError && proof.error.status === 423
                    ? 'A claim due by a day inside a closed period cannot gain a proof.'
                    : proof.error instanceof HttpError && proof.error.status === 400
                    ? 'A proof is a receipt or a photo.'
                    : `A ${c.status} claim cannot gain a proof.`}</span>
                )}
                {commenting.isError && commenting.variables?.id === c.id && (
                  <span data-testid="claim-error">{commenting.error instanceof HttpError && commenting.error.status === 400
                    ? 'A comment cannot be empty.'
                    : 'Your role may not change anything on this sheet.'}</span>
                )}
                {lining.isError && lining.variables?.id === c.id && (
                  <span data-testid="claim-error">{lining.error instanceof HttpError && lining.error.status === 423
                    ? 'A claim due by a day inside a closed period cannot be edited.'
                    : lining.error instanceof HttpError && lining.error.status === 409
                    ? `A ${c.status} claim cannot be edited, so it cannot gain a line.`
                    : lining.error instanceof HttpError && lining.error.status === 412
                    ? 'Another user is editing this claim; it cannot gain a line until they save or cancel.'
                    : lining.error instanceof HttpError && lining.error.status === 403
                    ? 'Your role may not change anything on this sheet.'
                    : 'A line needs a label, a whole quantity of at least 1 and a unit amount with up to two decimals.'}</span>
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
                    : 'A claim needs an amount with up to two decimals; a negative one (a correction) must name, by number, a SETTLED claim of this person; a claim with lines keeps their total as its amount.'}</span>
                )}
                {reassign.isError && reassign.variables?.id === c.id && (
                  <span data-testid="claim-error">{reassign.error instanceof HttpError && reassign.error.status === 423
                    ? 'A claim due by a day inside a closed period cannot be reassigned.'
                    : c.status === 'SETTLED' || c.status === 'VOID'
                    ? `A ${c.status} claim cannot be reassigned.`
                    : 'Only a role the permissions allow can reassign a claim, and only to another person.'}</span>
                )}
                {sharing.isError && sharing.variables?.id === c.id && (
                  <span data-testid="claim-error">{sharing.error instanceof HttpError && sharing.error.status === 409
                    ? `A ${c.status} claim cannot be shared.`
                    : sharing.error instanceof HttpError && sharing.error.status === 403
                    ? 'Your role may not change anything on this sheet.'
                    : 'A claim is shared with another person not already holding a share, at a share more than 0.00 with up to two decimals; its shares add up to at most 100.00.'}</span>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
