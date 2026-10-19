import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { HttpError, getJson, postJson, putJson } from '../../api/http';
import { useSingleSubmit } from '../../api/useSingleSubmit';
import { asNumber, useSearchParam } from '../../url/useSearchParam';
import { useCurrentUser } from '../session/SignIn';
import { useSheetToday } from '../sheet/SheetToday';
import { NA, due, fromHundredths, roundScaled, toHundredths } from '../rates/Rates';
import { useFloor, useLatePct, usePolicyDate, useTerms } from '../settings/Settings';
import { periodOf, usePeriods } from '../periods/usePeriods';

// A person's claims, listed under their row newest first. One form per row adds a claim, or edits
// the claim whose editor is open (URL state `editClaim`, the claim's id). Amounts come from the
// server as strings with two decimals and are added up in whole cents, never as floats. A claim's
// status moves DRAFT -> OPEN -> SETTLED, or DRAFT/OPEN -> VOID; the server refuses any other change.
// Opening gives a claim the sheet's next number, which a voided claim keeps; a draft has none.
// Only a draft can be edited: to change an open claim, void it and add a new one.
// The user who opens a claim is recorded and may not settle it.
// Settling fixes a claim's due at the rate of that moment. A person's statement binds each of their
// counting claims to the figure it has then, until their next statement; every other claim's due
// follows the current rate. Each claim shows the period its date falls in, if any.
// A claim with a negative amount is a correction: the form must name, by number, a SETTLED claim of the
// same person that it corrects (the server refuses it otherwise). Its due has no floor and no late charge.
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'SETTLED' | 'VOID';
export type Claim = { id: number; personId: number; amount: string; date: string; note: string; status: ClaimStatus;
  /** Given when the claim is opened, and kept if it is voided; null for a draft. */
  number: number | null;
  /** The due fixed when the claim was settled; null while it is live (not settled, or settled without a rate). */
  settledDue: string | null;
  /** The user who opened the claim; null until it is opened. That user may not settle it. */
  openedBy: string | null;
  /** Whether the person's last statement bound the claim to `statedDue`. */
  stated: boolean;
  /** The figure the last statement bound the claim to; null when unbound, or bound without a rate (n/a). */
  statedDue: string | null;
  /** The number of the SETTLED claim this correction (a negative amount) corrects; null for any other claim. */
  corrects: number | null };

const ACTIONS = [
  { action: 'open', label: 'Open' },
  { action: 'settle', label: 'Settle' },
  { action: 'void', label: 'Void' },
];

type ClaimBody = { amount: string; date: string; note: string; corrects: string };

export const claimsKey = (sheet: string) => ['sheet', sheet, 'claims'];
const sheetPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}`;

export function useClaims(sheet: string): Claim[] {
  return (
    useQuery({
      queryKey: claimsKey(sheet),
      queryFn: () => getJson<Claim[]>(`${sheetPath(sheet)}/claims`),
      enabled: !!sheet,
    }).data ?? []
  );
}

/** Only OPEN and SETTLED claims count toward claimed, due-total and every total; the OPEN claims of a
 *  person on hold (`held`) count in none. */
export const counts = (c: Claim, held = false) => c.status === 'SETTLED' || (c.status === 'OPEN' && !held);

/** What the late charge is worked out from: the sheet's late percent, terms and today; and what a live
 *  figure's rounding is decided by: the policy date (the sheet's today while blank) and the sheet's claims. */
export type Late = { pct: string; terms: number; today: string | undefined; policy: string | undefined; claims: Claim[] };

export function useLate(sheet: string): Late {
  const today = useSheetToday(sheet);
  return { pct: useLatePct(sheet), terms: useTerms(sheet), today, policy: usePolicyDate(sheet) ?? today,
    claims: useClaims(sheet) };
}

/** Whether the claim's live figures (the rate step and the late charge) round half up: it is dated before
 *  the policy date (a correction by the date of the claim it corrects); otherwise they round half to even. */
function roundsHalfUp(c: Claim, late: Late): boolean {
  const corrected = c.corrects === null ? undefined
    : late.claims.find((o) => o.personId === c.personId && o.number === c.corrects);
  return late.policy !== undefined && (corrected ?? c).date < late.policy;
}

/** A claim's due: the due fixed when it was settled (any late charge frozen in), else the figure a
 *  statement bound it to, else its live due at `pct`, at least the sheet's `floor` (n/a without a rate);
 *  an overdue claim's late charge goes on top of the bound or live figure. A correction has no floor and no late charge. */
export const claimDue = (c: Claim, pct: string | undefined, floor: string, late: Late): string =>
  c.settledDue ?? lateCharge(c, c.stated ? c.statedDue ?? NA
    : atLeast(due(c.amount, pct, roundsHalfUp(c, late)), c.corrects === null ? floor : undefined), late);

/** The figure plus the late percent of it for every full 30 days the claim is overdue, rounded to two
 *  decimals by the claim's rounding (see `roundsHalfUp`). A statement never binds the charge: it follows the sheet's today. */
function lateCharge(c: Claim, figure: string, late: Late): string {
  if (figure === NA || c.corrects !== null || !isOverdue(c, late.terms, late.today)) return figure;
  const days = (Date.parse(`${late.today}T00:00:00Z`) - Date.parse(`${dueBy(c, late.terms)}T00:00:00Z`)) / 86_400_000;
  const periods = BigInt(Math.floor(days / 30));
  return fromHundredths(toHundredths(figure) + roundScaled(toHundredths(figure) * toHundredths(late.pct) * periods, roundsHalfUp(c, late)));
}

/** The value raised to the floor; no floor (a correction's) leaves it as it is. */
const atLeast = (value: string, floor: string | undefined) =>
  floor !== undefined && value !== NA && toHundredths(value) < toHundredths(floor) ? floor : value;

/** Whether the claim keeps a figure of its own, whatever the rates: settled, or bound by a statement. */
export const hasFixedDue = (c: Claim) => c.settledDue !== null || (c.stated && c.statedDue !== null);

/** The claim's due-by (yyyy-MM-dd): its date plus the sheet's `terms` in days. */
export function dueBy(c: Claim, terms: number): string {
  const d = new Date(`${c.date}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() + terms);
  return d.toISOString().slice(0, 10);
}

/** Whether the claim is overdue: OPEN, with its due-by before the sheet's `today`. */
export const isOverdue = (c: Claim, terms: number, today: string | undefined) =>
  c.status === 'OPEN' && !!today && dueBy(c, terms) < today;

/** The counted claims' amounts added up, with two decimals; `held` says whose person is on hold. */
export function claimed(claims: Claim[], held: (personId: number) => boolean = () => false): string {
  return fromHundredths(claims.filter((c) => counts(c, held(c.personId))).reduce((sum, c) => sum + toHundredths(c.amount), 0n));
}

export function ClaimedTotal({ sheet, held }: { sheet: string; held: (personId: number) => boolean }) {
  const claims = useClaims(sheet);
  return (
    <p>
      Claimed total: <strong data-testid="rollup-claimed-total">{claimed(claims, held)}</strong>
    </p>
  );
}

/** The claims cell of a person's row: the claim form and the person's claims, newest first, each
 *  with its due at the person's grade's rate in force on its date (`pctOn`, undefined when none is). */
export function PersonClaims({ sheet, personId, claims, pctOn }: {
  sheet: string; personId: number; claims: Claim[]; pctOn: (date: string) => string | undefined;
}) {
  const [editId, setEditId] = useSearchParam('editClaim', asNumber);
  const editing = claims.find((c) => c.id === editId);

  return (
    <>
      {/* Keyed so opening another claim's editor (or closing it) refills the form. */}
      <ClaimForm key={editing ? `edit-${editing.id}` : 'add'} sheet={sheet} personId={personId}
        editing={editing} onDone={() => setEditId(undefined)} />
      <ul>
        {claims.map((c) => (
          <ClaimRow key={c.id} sheet={sheet} claim={c} pct={pctOn(c.date)} onEdit={() => setEditId(c.id)} />
        ))}
      </ul>
    </>
  );
}

/** One claim: its fields, status and due, with its edit and status buttons. A refused status change,
 *  or editing a claim that is not a draft or is dated in a closed period, shows the row's error, which
 *  the row's next action clears. */
function ClaimRow({ sheet, claim: c, pct, onEdit }: {
  sheet: string; claim: Claim; pct: string | undefined; onEdit: () => void;
}) {
  const queryClient = useQueryClient();
  const [user] = useCurrentUser();
  const by = `?user=${encodeURIComponent(user ?? '')}`;
  const period = periodOf(usePeriods(sheet), c.date);
  const floor = useFloor(sheet);
  const late = useLate(sheet);
  const { terms, today } = late;
  const change = useMutation({
    mutationFn: (action: string) => postJson<Claim>(`${sheetPath(sheet)}/claims/${c.id}/${action}${by}`, {}),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
  });
  // Why the last press of Edit was refused; null when it was not.
  const [editRefused, setEditRefused] = useState<string | null>(null);

  return (
    <li data-testid={`claim-row-${c.id}`}>
      #<span data-testid="claim-number">{c.number ?? ''}</span>{' '}
      <span data-testid="claim-amount">{c.amount}</span>{' '}
      <span data-testid="claim-date">{c.date}</span>{' '}
      Period <span data-testid="claim-period">{period?.name ?? ''}</span>{' '}
      <span data-testid="claim-note">{c.note}</span>{' '}
      <span data-testid="claim-status">{c.status}</span>{' '}
      Opened by <span data-testid="claim-opened-by">{c.openedBy ?? ''}</span>{' '}
      Due <span data-testid="claim-due">{claimDue(c, pct, floor, late)}</span>{' '}
      Due by <span data-testid="claim-due-by">{dueBy(c, terms)}</span>{' '}
      Overdue <span data-testid="claim-overdue">{isOverdue(c, terms, today) ? 'yes' : 'no'}</span>{' '}
      Stated <span data-testid="claim-stated">{c.stated ? 'yes' : 'no'}</span>{' '}
      Corrects <span data-testid="claim-corrects">{c.corrects ?? ''}</span>{' '}
      <button type="button" data-testid="claim-edit" onClick={() => {
        change.reset();
        // Only a draft dated outside every closed period can be edited (the server refuses the rest).
        const refused = c.status !== 'DRAFT'
          ? `Only a draft claim can be edited; this one is ${c.status}. To change it, void it and add a new one.`
          : period?.closed ? `The claim is dated in the closed period ${period.name}; it cannot be edited.` : null;
        setEditRefused(refused);
        if (!refused) onEdit();
      }}>
        Edit
      </button>
      {ACTIONS.map(({ action, label }) => (
        <button key={action} type="button" data-testid={`claim-${action}`} disabled={change.isPending}
          onClick={() => { change.reset(); setEditRefused(null); change.mutate(action); }}>
          {label}
        </button>
      ))}
      {(change.isError || editRefused) && (
        <span role="alert" data-testid="claim-error">
          {' '}
          {editRefused
            ?? ((change.error instanceof HttpError && change.error.reason) || 'The claim could not be changed.')}
        </span>
      )}
    </li>
  );
}

function ClaimForm({ sheet, personId, editing, onDone }: {
  sheet: string; personId: number; editing?: Claim; onDone: () => void;
}) {
  const queryClient = useQueryClient();
  const today = useSheetToday(sheet);
  const [amount, setAmount] = useState(editing?.amount ?? '');
  // Undefined until changed: the sheet's today, which the server fills in when the date is blank.
  const [date, setDate] = useState<string | undefined>(editing?.date);
  const [note, setNote] = useState(editing?.note ?? '');
  // The number of the settled claim a correction (a negative amount) corrects; blank for any other claim.
  const [corrects, setCorrects] = useState(editing?.corrects?.toString() ?? '');
  const [user] = useCurrentUser();
  const by = `?user=${encodeURIComponent(user ?? '')}`;
  const submitOnce = useSingleSubmit();
  const save = useMutation({
    mutationFn: (body: ClaimBody) =>
      editing
        ? putJson<Claim>(`${sheetPath(sheet)}/claims/${editing.id}${by}`, body)
        : postJson<Claim>(`${sheetPath(sheet)}/people/${personId}/claims${by}`, body),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
      if (editing) {
        onDone();
      } else {
        setAmount('');
        setDate(undefined);
        setNote('');
        setCorrects('');
      }
    },
  });
  const changed = <T,>(set: (v: T) => void) => (v: T) => {
    save.reset();
    set(v);
  };

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        submitOnce(() => {
          save.reset();
          // A missing or over-precise amount, or a correction not naming a settled claim of the person,
          // is refused by the server (400).
          return save.mutateAsync({ amount, date: date ?? '', note, corrects });
        });
      }}
    >
      <input data-testid="claim-form-amount" placeholder="Amount" inputMode="decimal" value={amount}
        onChange={(e) => changed(setAmount)(e.target.value)} />{' '}
      <input type="date" data-testid="claim-form-date" value={date ?? today ?? ''}
        onChange={(e) => changed(setDate)(e.target.value)} />{' '}
      <input data-testid="claim-form-note" placeholder="Note" value={note}
        onChange={(e) => changed(setNote)(e.target.value)} />{' '}
      <input data-testid="claim-form-corrects" placeholder="Corrects claim #" inputMode="numeric" value={corrects}
        onChange={(e) => changed(setCorrects)(e.target.value)} />{' '}
      <button type="submit" data-testid="claim-form-submit" disabled={save.isPending}>
        {editing ? 'Save claim' : 'Add claim'}
      </button>
      {editing && (
        <button type="button" onClick={onDone}>
          Cancel
        </button>
      )}
      {save.isError && (
        <span role="alert" data-testid="claim-form-error">
          {' '}
          {(save.error instanceof HttpError && (save.error.status === 400 || save.error.status === 409) && save.error.reason)
            || 'The claim could not be saved.'}
        </span>
      )}
    </form>
  );
}
