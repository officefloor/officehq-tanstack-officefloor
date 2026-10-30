import { useEffect, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { claimsKey, type Claim } from './useClaims';

// The claim form on a person's row: adds a claim to that person, or — while one of their claims'
// editor is open (`editClaim` in the URL) — saves that claim. Opening the editor fills the form
// with the claim's values. A blank date takes the sheet's today.
export function ClaimForm({ personId, claims }: { personId: string; claims: Claim[] }) {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const [editParam, setEditing] = useSearchParam('editClaim', asString);
  const edited = claims.find((c) => c.id === editParam);
  // A claim not under this person has no editor open here.
  const editing = edited?.id ?? '';
  const [amount, setAmount] = useState('');
  const [date, setDate] = useState('');
  const [note, setNote] = useState('');
  const queryClient = useQueryClient();

  const clear = () => {
    setAmount('');
    setDate('');
    setNote('');
  };

  const save = useMutation({
    mutationFn: (body: { amount: string; date: string; note: string }) =>
      editing !== ''
        ? putJson<Claim>(`/api/sheets/${encodeURIComponent(sheet)}/claims/${encodeURIComponent(editing)}`, { ...body, user })
        : postJson<Claim>(
            `/api/sheets/${encodeURIComponent(sheet)}/people/${encodeURIComponent(personId)}/claims`,
            { ...body, user },
          ),
    onSuccess: () => {
      clear();
      if (editing !== '') setEditing(undefined);
      void queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    },
  });

  // Opening (or closing) an editor fills the form with that claim's values (or empties it).
  const editedId = edited?.id;
  useEffect(() => {
    save.reset();
    if (edited) {
      setAmount(edited.amount);
      setDate(edited.date);
      setNote(edited.note);
    } else {
      clear();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [editedId]);

  const changed = <T,>(set: (v: T) => void) => (v: T) => {
    set(v);
    save.reset();
  };

  return (
    <form
      data-testid="claim-form"
      onSubmit={(e) => {
        e.preventDefault();
        save.reset();
        save.mutate({ amount: amount.trim(), date: date.trim(), note });
      }}
    >
      <input
        data-testid="claim-form-amount"
        value={amount}
        placeholder="Amount"
        inputMode="decimal"
        onChange={(e) => changed(setAmount)(e.target.value)}
      />{' '}
      <input type="date" data-testid="claim-form-date" value={date} onChange={(e) => changed(setDate)(e.target.value)} />{' '}
      <input data-testid="claim-form-note" value={note} placeholder="Note" onChange={(e) => changed(setNote)(e.target.value)} />{' '}
      <button type="submit" data-testid="claim-form-submit">{editing !== '' ? 'Save claim' : 'Add claim'}</button>
      {save.isError && (
        <span data-testid="claim-form-error" role="alert"> Could not save: the amount must be 0.00 or more, with at most two decimals.</span>
      )}
    </form>
  );
}
