import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { HttpError, getJson, postJson, putJson } from '../../api/http';
import { useSingleSubmit } from '../../api/useSingleSubmit';
import { asNumber, useSearchParam } from '../../url/useSearchParam';
import { useCurrentUser } from '../session/SignIn';
import { useSheetToday } from '../sheet/SheetToday';
import { due } from '../rates/Rates';

// A person's claims, listed under their row newest first. One form per row adds a claim, or edits
// the claim whose editor is open (URL state `editClaim`, the claim's id). Amounts come from the
// server as strings with two decimals and are added up in whole cents, never as floats. A claim's
// status moves DRAFT -> OPEN -> SETTLED, or DRAFT/OPEN -> VOID; the server refuses any other change.
// Opening gives a claim the sheet's next number, which voiding clears; a draft has none.
// The user who opens a claim is recorded and may not settle it.
// Settling fixes a claim's due at the rate of that moment; every other claim's due follows the current rate.
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'SETTLED' | 'VOID';
export type Claim = { id: number; personId: number; amount: string; date: string; note: string; status: ClaimStatus;
  /** Given when the claim is opened; null for a draft or a voided claim. */
  number: number | null;
  /** The due fixed when the claim was settled; null while it is live (not settled, or settled without a rate). */
  settledDue: string | null;
  /** The user who opened the claim; null until it is opened. That user may not settle it. */
  openedBy: string | null };

const ACTIONS = [
  { action: 'open', label: 'Open' },
  { action: 'settle', label: 'Settle' },
  { action: 'void', label: 'Void' },
];

type ClaimBody = { amount: string; date: string; note: string };

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

const toCents = (amount: string) => {
  const [whole, frac = ''] = amount.split('.');
  return Number(whole) * 100 + Number(frac.padEnd(2, '0'));
};

/** Only OPEN and SETTLED claims count toward claimed, due-total and every total. */
export const counts = (c: Claim) => c.status === 'OPEN' || c.status === 'SETTLED';

/** A claim's figure: the due fixed when it was settled, else its live due at `pct` (n/a without one). */
export const claimDue = (c: Claim, pct: string | undefined): string => c.settledDue ?? due(c.amount, pct);

/** The counted claims' amounts added up, with two decimals. */
export function claimed(claims: Claim[]): string {
  const cents = claims.filter(counts).reduce((sum, c) => sum + toCents(c.amount), 0);
  return `${Math.floor(cents / 100)}.${String(cents % 100).padStart(2, '0')}`;
}

export function ClaimedTotal({ sheet }: { sheet: string }) {
  const claims = useClaims(sheet);
  return (
    <p>
      Claimed total: <strong data-testid="rollup-claimed-total">{claimed(claims)}</strong>
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

/** One claim: its fields, status and due, with its edit and status buttons. A refused status change
 *  shows the row's error, which the row's next action clears. */
function ClaimRow({ sheet, claim: c, pct, onEdit }: {
  sheet: string; claim: Claim; pct: string | undefined; onEdit: () => void;
}) {
  const queryClient = useQueryClient();
  const [user] = useCurrentUser();
  const by = `?user=${encodeURIComponent(user ?? '')}`;
  const change = useMutation({
    mutationFn: (action: string) => postJson<Claim>(`${sheetPath(sheet)}/claims/${c.id}/${action}${by}`, {}),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
  });

  return (
    <li data-testid={`claim-row-${c.id}`}>
      #<span data-testid="claim-number">{c.number ?? ''}</span>{' '}
      <span data-testid="claim-amount">{c.amount}</span>{' '}
      <span data-testid="claim-date">{c.date}</span>{' '}
      <span data-testid="claim-note">{c.note}</span>{' '}
      <span data-testid="claim-status">{c.status}</span>{' '}
      Opened by <span data-testid="claim-opened-by">{c.openedBy ?? ''}</span>{' '}
      Due <span data-testid="claim-due">{claimDue(c, pct)}</span>{' '}
      <button type="button" data-testid="claim-edit" onClick={() => { change.reset(); onEdit(); }}>
        Edit
      </button>
      {ACTIONS.map(({ action, label }) => (
        <button key={action} type="button" data-testid={`claim-${action}`} disabled={change.isPending}
          onClick={() => { change.reset(); change.mutate(action); }}>
          {label}
        </button>
      ))}
      {change.isError && (
        <span role="alert" data-testid="claim-error">
          {' '}
          {(change.error instanceof HttpError && change.error.reason) || 'The claim could not be changed.'}
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
          // A missing, negative or over-precise amount is refused by the server (400).
          return save.mutateAsync({ amount, date: date ?? '', note });
        });
      }}
    >
      <input data-testid="claim-form-amount" placeholder="Amount" inputMode="decimal" value={amount}
        onChange={(e) => changed(setAmount)(e.target.value)} />{' '}
      <input type="date" data-testid="claim-form-date" value={date ?? today ?? ''}
        onChange={(e) => changed(setDate)(e.target.value)} />{' '}
      <input data-testid="claim-form-note" placeholder="Note" value={note}
        onChange={(e) => changed(setNote)(e.target.value)} />{' '}
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
          {(save.error instanceof HttpError && save.error.status === 400 && save.error.reason)
            || 'The claim could not be saved.'}
        </span>
      )}
    </form>
  );
}
