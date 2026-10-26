import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson, RefusedError } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { useUser } from '../session/useUser';
import { claimsKey } from './claimsQuery';
import type { Claim } from './claimsQuery';

const AMOUNT = /^-?\d+(\.\d{1,2})?$/;
const NUMBER = /^(\d+|.+-\d+)$/;
const DATE = /^\d{4}-\d{2}-\d{2}$/;

type Body = { amount: string; date: string; note: string; corrects: string };

/**
 * The one form a person's claims are added and edited through. Opening a claim's editor fills it. A negative amount is a
 * correction: it names the number of the settled claim of the same person it corrects (the server checks that it is one).
 * While an editor is being opened or closed (`held`) the form takes no input.
 */
export function ClaimForm({ sheetId, personId, editing, held, onDone }: {
  sheetId: string;
  personId: number;
  editing: Claim | undefined;
  held: boolean;
  onDone: () => void;
}) {
  const queryClient = useQueryClient();
  const [amount, setAmount] = useState('');
  const [date, setDate] = useState('');
  const [note, setNote] = useState('');
  const [corrects, setCorrects] = useState('');
  const [error, setError] = useState('');

  // Opening an editor (or closing it) refills the form. A blank date is the sheet's today.
  useEffect(() => {
    setAmount(editing?.amount ?? '');
    setDate(editing?.date ?? '');
    setNote(editing?.note ?? '');
    setCorrects(editing?.corrects == null ? '' : String(editing.corrects));
    setError('');
  }, [editing?.id]); // eslint-disable-line react-hooks/exhaustive-deps

  const [user] = useUser();
  const once = useSubmitOnce();
  const save = useMutation({
    mutationFn: (body: Body) =>
      editing
        ? putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${editing.id}`, body, user)
        : postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/claims`, body, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
      if (editing) {
        onDone();
      } else {
        setAmount('');
        setDate('');
        setNote('');
        setCorrects('');
      }
    },
    onError: (e) => setError(e instanceof RefusedError ? e.message : 'The claim could not be saved.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const a = amount.trim();
    const d = date.trim();
    const c = corrects.trim();
    if (!AMOUNT.test(a)) {
      setError('The amount must be a number with up to two decimals.');
      return;
    }
    if (Number(a) < 0 && c === '') {
      setError('A negative amount is a correction: name the number of the settled claim it corrects.');
      return;
    }
    if (Number(a) >= 0 && c !== '') {
      setError('Only a claim with a negative amount corrects another.');
      return;
    }
    if (c !== '' && !NUMBER.test(c)) {
      setError(`Not a claim number: ${c}`);
      return;
    }
    if (d !== '' && !DATE.test(d)) {
      setError('The date must be yyyy-mm-dd.');
      return;
    }
    once(() => save.mutateAsync({ amount: a, date: d, note: note.trim(), corrects: c }));
  };

  const edit = <T,>(set: (v: T) => void) => (v: T) => {
    set(v);
    setError('');
  };

  return (
    <form data-testid="claim-form" onSubmit={submit}>
      <input
        data-testid="claim-form-amount"
        disabled={held}
        placeholder="Amount"
        value={amount}
        onChange={(e) => edit(setAmount)(e.target.value)}
      />{' '}
      <input
        data-testid="claim-form-date"
        disabled={held}
        placeholder="yyyy-mm-dd"
        value={date}
        onChange={(e) => edit(setDate)(e.target.value)}
      />{' '}
      <input
        data-testid="claim-form-note"
        disabled={held}
        placeholder="Note"
        value={note}
        onChange={(e) => edit(setNote)(e.target.value)}
      />{' '}
      <input
        data-testid="claim-form-corrects"
        disabled={held}
        placeholder="Corrects no."
        value={corrects}
        onChange={(e) => edit(setCorrects)(e.target.value)}
      />{' '}
      <button type="submit" data-testid="claim-form-submit" disabled={held || save.isPending}>
        {editing ? 'Save claim' : 'Add claim'}
      </button>
      {error && <p data-testid="claim-form-error">{error}</p>}
    </form>
  );
}
