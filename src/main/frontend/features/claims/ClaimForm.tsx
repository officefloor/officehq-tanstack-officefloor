import { useEffect, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { useTemplates } from '../templates/useTemplates';
import { claimsKey, tagList, tagsKey, type Claim } from './useClaims';

// The claim form on a person's row: adds a claim to that person, or — while one of their claims'
// editor is open (`editClaim` in the URL) — saves that claim. Opening the editor fills the form
// with the claim's values. A blank date takes the sheet's entry date while set, else its today. A negative amount is a correction,
// which names in "corrects" the number of the SETTLED claim of the same person it corrects.
// A reference (spaces trimmed, blank for none) must not already be used on the sheet, regardless of case.
// Tags are comma separated: each is trimmed and lower-cased, and repeats are dropped (by the server).
// Beside it, a template select and a button that adds a DRAFT claim with the template's amount and note
// as they are now, with no date, by every rule of the form; a refusal shows the form's error.
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
  const [corrects, setCorrects] = useState('');
  const [ref, setRef] = useState('');
  const [tags, setTags] = useState('');
  const templates = useTemplates(sheet);
  const [template, setTemplate] = useState('');
  const queryClient = useQueryClient();
  const submitOnce = useSubmitOnce();

  const clear = () => {
    setAmount('');
    setDate('');
    setNote('');
    setCorrects('');
    setRef('');
    setTags('');
  };

  const save = useMutation({
    mutationFn: (body: { amount: string; date: string; note: string; corrects: string; ref: string; tags: string }) =>
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
      void queryClient.invalidateQueries({ queryKey: tagsKey(sheet) });
    },
  });

  const fromTemplate = useMutation({
    mutationFn: (name: string) =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheet)}/people/${encodeURIComponent(personId)}/template-claims`,
        { template: name, user },
      ),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    },
  });
  const chosen = templates.some((t) => t.name === template) ? template : (templates[0]?.name ?? '');

  // Opening (or closing) an editor fills the form with that claim's values (or empties it).
  const editedId = edited?.id;
  useEffect(() => {
    save.reset();
    fromTemplate.reset();
    if (edited) {
      setAmount(edited.amount);
      setDate(edited.date);
      setNote(edited.note);
      setCorrects(edited.corrects);
      setRef(edited.ref);
      setTags(tagList(edited.tags));
    } else {
      clear();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [editedId]);

  const changed = <T,>(set: (v: T) => void) => (v: T) => {
    set(v);
    save.reset();
    fromTemplate.reset();
  };

  return (
    <form
      data-testid="claim-form"
      onSubmit={(e) => {
        e.preventDefault();
        submitOnce(() => {
          save.reset();
          fromTemplate.reset();
          return save.mutateAsync({ amount: amount.trim(), date: date.trim(), note, corrects: corrects.trim(), ref: ref.trim(), tags });
        });
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
      <input
        data-testid="claim-form-corrects"
        value={corrects}
        placeholder="Corrects no."
        inputMode="numeric"
        onChange={(e) => changed(setCorrects)(e.target.value)}
      />{' '}
      <input data-testid="claim-form-ref" value={ref} placeholder="Reference" onChange={(e) => changed(setRef)(e.target.value)} />{' '}
      <input data-testid="claim-form-tags" value={tags} placeholder="Tags" onChange={(e) => changed(setTags)(e.target.value)} />{' '}
      <button type="submit" data-testid="claim-form-submit">{editing !== '' ? 'Save claim' : 'Add claim'}</button>
      <select data-testid="claim-template-select" value={chosen} onChange={(e) => changed(setTemplate)(e.target.value)}>
        {templates.map((t) => (
          <option key={t.name} value={t.name}>{t.name}</option>
        ))}
      </select>{' '}
      <button
        type="button"
        data-testid="claim-template-add"
        onClick={() =>
          submitOnce(() => {
            save.reset();
            fromTemplate.reset();
            return fromTemplate.mutateAsync(chosen);
          })
        }
      >
        Add from template
      </button>
      {(save.isError || fromTemplate.isError) && (
        <span data-testid="claim-form-error" role="alert"> Could not save: the amount must have at most two decimals, a negative amount is a correction that must name by number a SETTLED claim of the same person (and only a correction names one), no claim whose due-by falls inside a closed period can be added or changed, none after the person's retirement date, and no reference already used on the sheet.</span>
      )}
    </form>
  );
}
