import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson } from '../../api/http';
import { ClaimRowValues } from '../../slots/defs/claimRow';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import type { Person } from '../people/people';
import { SIGNED_AMOUNT, claimsKey, useClaims } from './claims';
import type { Claim, ClaimInput } from './claims';
import { useOneAtATime } from '../../query/useOneAtATime';
import { setClaimError, useClaimError } from './claimError';
import { contains, usePeriods } from '../periods/periods';
import { useSettings } from '../settings/settings';
import { tagsKey } from '../tags/tags';

const EMPTY: ClaimInput = { amount: '', date: '', note: '', corrects: '', ref: '', tags: '' };

// A person's claims, newest first, and the form that adds one — or, while a claim's editor is
// open, saves that claim's changes. Opening an editor (the edit button) locks the claim for every
// other user and fills the form with the claim's values; a refusal (not editable, a viewer, or
// locked by another user) shows as the claim's error. Saving or cancelling the editor releases the
// lock. A claim the signed-in user holds the lock on comes back with its editor open when the sheet
// is loaded again. A refusal the screen can see (not a DRAFT, due by a date in a closed period, a viewer, locked
// by another user) shows at once; otherwise the editor opens at once and the lock is taken behind it —
// a save waits for it, and a refused lock closes the editor and shows the claim's error.
export function PersonClaims({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const claims = (useClaims().data ?? []).filter((c) => c.personId === person.id);
  const [form, setForm] = useState<ClaimInput>(EMPTY);
  const [editing, setEditing] = useState<number | undefined>(undefined);
  const [error, setError] = useState('');
  const queryClient = useQueryClient();
  const once = useOneAtATime();
  const periods = usePeriods().data ?? [];
  const viewer = (useSettings().data?.roles ?? []).some((r) => r.user === user && r.role === 'viewer');
  const editingNow = useRef<number | undefined>(undefined);
  editingNow.current = editing;
  const locking = useRef<Promise<void>>(Promise.resolve());

  const claimsUrl = `/api/sheets/${encodeURIComponent(sheet)}/claims`;

  // Show the claim as (un)locked by the user at once, so a released lock does not re-open its editor.
  const markLocked = (id: number, lockedBy: string | null) =>
    queryClient.setQueryData<Claim[]>(claimsKey(sheet), (all) => all?.map((c) => (c.id === id ? { ...c, lockedBy } : c)));

  const release = (id: number) => {
    markLocked(id, null);
    postJson<Claim>(`${claimsUrl}/${id}/unlock?user=${encodeURIComponent(user)}`, {}).catch(() => undefined);
  };

  const close = () => {
    if (editing !== undefined) release(editing);
    setEditing(undefined);
    setForm(EMPTY);
    setError('');
  };

  const fill = (c: Claim) => {
    setEditing(c.id);
    setForm({ amount: c.amount, date: c.date, note: c.note, corrects: c.corrects ?? '', ref: c.ref ?? '', tags: c.tags.join(', ') });
    setError('');
  };

  const save = useMutation({
    mutationFn: async (input: ClaimInput) => {
      if (editing === undefined) {
        return postJson<Claim>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/claims?user=${encodeURIComponent(user)}`, input);
      }
      await locking.current;
      return putJson<Claim>(`/api/sheets/${encodeURIComponent(sheet)}/claims/${editing}?user=${encodeURIComponent(user)}`, input);
    },
    onSuccess: async () => {
      if (editing !== undefined) markLocked(editing, null);
      setEditing(undefined);
      setForm(EMPTY);
      setError('');
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: tagsKey(sheet) }),
      ]);
    },
    onError: () => setError('That claim could not be saved.'),
  });

  const refused = (c: Claim) =>
    `This ${c.status} claim cannot be edited (only a DRAFT not due in a closed period, not by a viewer, nor while another user is editing it).`;

  // Why the edit button is refused, from what the screen holds; '' when the server is asked.
  const refusal = (c: Claim) =>
    c.status !== 'DRAFT' ||
    viewer ||
    (c.lockedBy !== null && c.lockedBy !== user) ||
    periods.some((p) => p.closed && contains(p, c.dueBy))
      ? refused(c)
      : '';

  // Re-open the editor of a claim the user still holds the lock on (the sheet was loaded again).
  const held = editing === undefined ? claims.find((c) => c.lockedBy === user) : undefined;
  useEffect(() => {
    if (held) fill(held);
  }, [held?.id]);

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (!SIGNED_AMOUNT.test(form.amount.trim())) {
      setError('Type an amount with at most two decimals.');
      return;
    }
    const date = form.date.trim();
    if (date && !/^\d{4}-\d{2}-\d{2}$/.test(date)) {
      setError('Type a date as yyyy-mm-dd.');
      return;
    }
    once(() => save.mutateAsync(form));
  };

  const change = (patch: Partial<ClaimInput>) => {
    setForm((f) => ({ ...f, ...patch }));
    setError('');
  };

  const edit = (c: Claim) => {
    const why = refusal(c);
    setClaimError(sheet, c.id, why);
    if (why) return;
    if (editing !== undefined) release(editing);
    markLocked(c.id, user);
    fill(c);
    const taken = postJson<Claim>(`${claimsUrl}/${c.id}/lock?user=${encodeURIComponent(user)}`, {}).then(
      () => undefined,
      async (e) => {
        markLocked(c.id, null);
        if (editingNow.current === c.id) {
          setEditing(undefined);
          setForm(EMPTY);
          setError('');
        }
        setClaimError(sheet, c.id, refused(c));
        await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
        throw e;
      },
    );
    taken.catch(() => undefined);
    locking.current = taken;
  };

  return (
    <td data-testid="person-claims">
      <ul>
        {claims.map((c) => (
          <li key={c.id} data-testid={`claim-row-${c.id}`}>
            <span data-testid="claim-amount">{c.amount}</span> <span data-testid="claim-date">{c.date}</span>{' '}
            <span data-testid="claim-note" style={{ whiteSpace: 'pre' }}>
              {c.note}
            </span>{' '}
            <ClaimRowValues.Slot claim={c} person={person} />{' '}
            {editing === c.id ? (
              <button type="button" data-testid="claim-cancel" onClick={close}>
                Cancel
              </button>
            ) : (
              <button type="button" data-testid="claim-edit" onClick={() => edit(c)}>
                Edit
              </button>
            )}{' '}
            <ClaimInlineAmount claim={c} refusal={refusal(c)} />
          </li>
        ))}
      </ul>
      <form onSubmit={submit} data-testid="claim-form">
        <input
          data-testid="claim-form-amount"
          placeholder="Amount"
          value={form.amount}
          onChange={(e) => change({ amount: e.target.value })}
        />
        <input
          data-testid="claim-form-date"
          placeholder="yyyy-mm-dd"
          value={form.date}
          onChange={(e) => change({ date: e.target.value })}
        />
        <input
          data-testid="claim-form-note"
          placeholder="Note"
          value={form.note}
          onChange={(e) => change({ note: e.target.value })}
        />
        <input
          data-testid="claim-form-corrects"
          placeholder="Corrects claim no."
          value={form.corrects}
          onChange={(e) => change({ corrects: e.target.value })}
        />
        <input
          data-testid="claim-form-ref"
          placeholder="Reference"
          value={form.ref}
          onChange={(e) => change({ ref: e.target.value })}
        />
        <input
          data-testid="claim-form-tags"
          placeholder="Tags, comma separated"
          value={form.tags}
          onChange={(e) => change({ tags: e.target.value })}
        />
        <button type="submit" data-testid="claim-form-submit" disabled={save.isPending}>
          {editing === undefined ? 'Add claim' : 'Save claim'}
        </button>
        {error && <span data-testid="claim-form-error">{error}</span>}
      </form>
    </td>
  );
}

// A claim's amount changed in its row: the entry and save button beside the edit button. The save is
// the editor's save with only the amount changed, so it follows the claim form's rules (and writes
// CLAIM_EDITED); a refusal shows as the claim's error, at once when the screen can see it.
function ClaimInlineAmount({ claim, refusal }: { claim: Claim; refusal: string }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [, setError] = useClaimError(claim.id);
  const [amount, setAmount] = useState('');
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const save = useMutation({
    mutationFn: (value: string) =>
      putJson<Claim>(`/api/sheets/${encodeURIComponent(sheet)}/claims/${claim.id}?user=${encodeURIComponent(user)}`, {
        amount: value,
        date: claim.date,
        note: claim.note,
        corrects: claim.corrects ?? '',
        ref: claim.ref ?? '',
        tags: claim.tags.join(','),
      }),
    onSuccess: async () => {
      setAmount('');
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    },
    onError: () => setError('That claim could not be saved.'),
  });

  return (
    <>
      <input
        data-testid="claim-inline-amount"
        placeholder="Amount"
        value={amount}
        onChange={(e) => {
          setAmount(e.target.value);
          setError('');
        }}
      />
      <button
        type="button"
        data-testid="claim-inline-save"
        disabled={save.isPending}
        onClick={() => {
          setError('');
          if (refusal) {
            setError(refusal);
            return;
          }
          const value = amount.trim();
          if (!SIGNED_AMOUNT.test(value)) {
            setError('Type an amount with at most two decimals.');
            return;
          }
          once(() => save.mutateAsync(value));
        }}
      >
        Save amount
      </button>
    </>
  );
}
