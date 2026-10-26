import { useState, type ChangeEvent } from 'react';
import { useIsMutating, useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson } from '../../api/http';
import { ClaimCells } from '../../slots/defs/claimRow';
import { PersonCells, PersonHeads } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useUser } from '../session/useSession';
import { useHeld } from '../people/people.slot';
import { claimLockKey, claimSaveKey, claimsKey, sumAmounts, useClaims, useEditingClaim, type Claim } from './useClaims';
import { useSubmitOnce } from '../../mutation/useSubmitOnce';
import { useTemplates } from '../templates/templates.slot';
import { useColumnShown } from '../columns/useColumns';
import { useColumnShown } from '../columns/useColumns';

const AMOUNT = /^-?\d+(\.\d{1,2})?$/;

type ClaimBody = { amount: string; date: string; note: string; corrects: string; ref: string; tags: string };

// A person's claim form: adds a claim, or — with one of their claims' editor open — saves its
// changes. It is re-mounted for each claim opened, so it starts filled with that claim's values.
// Its template select adds a DRAFT claim from a sheet's template (its amount and note as they stand
// then, no date) by the same rules; a refusal shows in the form's error.
function ClaimForm({ sheetId, personId, editing }: { sheetId: string; personId: number; editing?: Claim }) {
  const [, setEditing] = useEditingClaim();
  const [amount, setAmount] = useState(editing?.amount ?? '');
  const [date, setDate] = useState(editing?.date ?? '');
  const [note, setNote] = useState(editing?.note ?? '');
  const [corrects, setCorrects] = useState(editing?.corrects ?? '');
  const [ref, setRef] = useState(editing?.ref ?? '');
  const [tags, setTags] = useState(editing?.tags.join(', ') ?? '');
  const [user] = useUser();
  const queryClient = useQueryClient();
  const sheet = `/api/sheets/${encodeURIComponent(sheetId)}`;
  const save = useMutation({
    mutationKey: claimSaveKey(sheetId),
    mutationFn: async (body: ClaimBody) => {
      if (body.amount.trim() === '') throw new Error('Enter an amount');
      if (!AMOUNT.test(body.amount.trim())) throw new Error('An amount is a number with up to two decimals');
      // `by` is the signed-in user the change is recorded against.
      const sent = { ...body, by: user ?? '' };
      return editing
        ? putJson<Claim>(`${sheet}/claims/${editing.id}`, sent)
        : postJson<Claim>(`${sheet}/people/${personId}/claims`, sent);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
      if (editing) {
        setEditing(undefined);
      } else {
        setAmount('');
        setDate('');
        setNote('');
        setCorrects('');
        setRef('');
        setTags('');
      }
    },
  });
  const templates = useTemplates(sheetId);
  const [template, setTemplate] = useState('');
  const fromTemplate = useMutation({
    mutationFn: async (name: string) => {
      if (name === '') throw new Error('Choose a template');
      return postJson<Claim>(`${sheet}/people/${personId}/claims/template`, { template: name, by: user ?? '' });
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });
  const field = (set: (v: string) => void) => (e: ChangeEvent<HTMLInputElement>) => {
    set(e.target.value);
    save.reset();
    fromTemplate.reset();
  };

  // While a claim's editor is being opened, the form may be about to become its editor: it waits.
  const locking = useIsMutating({ mutationKey: claimLockKey(sheetId) }) > 0;
  const submit = useSubmitOnce(save);
  const submitTemplate = useSubmitOnce(fromTemplate);
  const error = save.error ?? fromTemplate.error;
  return (
    <form
      data-testid="claim-form"
      onSubmit={(e) => {
        e.preventDefault();
        fromTemplate.reset();
        submit({ amount, date, note, corrects, ref, tags });
      }}
    >
      <input data-testid="claim-form-amount" placeholder="Amount" inputMode="decimal" value={amount} onChange={field(setAmount)} />{' '}
      <input data-testid="claim-form-date" type="date" value={date} onChange={field(setDate)} />{' '}
      <input data-testid="claim-form-note" placeholder="Note" value={note} onChange={field(setNote)} />{' '}
      <input data-testid="claim-form-corrects" placeholder="Corrects no." value={corrects} onChange={field(setCorrects)} />{' '}
      <input data-testid="claim-form-ref" placeholder="Reference" value={ref} onChange={field(setRef)} />{' '}
      <input data-testid="claim-form-tags" placeholder="Tags, comma separated" value={tags} onChange={field(setTags)} />{' '}
      <button type="submit" data-testid="claim-form-submit" disabled={save.isPending || locking}>
        {editing ? 'Save claim' : 'Add claim'}
      </button>
      {!editing && (
        <>
          {' '}
          <select
            data-testid="claim-template-select"
            value={template}
            onChange={(e) => {
              setTemplate(e.target.value);
              save.reset();
              fromTemplate.reset();
            }}
          >
            <option value="">Template…</option>
            {(templates.data ?? []).map((t) => (
              <option key={t.name} value={t.name}>
                {t.name}
              </option>
            ))}
          </select>{' '}
          <button
            type="button"
            data-testid="claim-template-add"
            disabled={fromTemplate.isPending || locking}
            onClick={() => {
              save.reset();
              submitTemplate(template);
            }}
          >
            Add from template
          </button>
        </>
      )}
      {error && (
        <p data-testid="claim-form-error" role="alert">
          {error.message}
        </p>
      )}
    </form>
  );
}

function ClaimsHead({ sheetId }: { sheetId: string; user: string }) {
  const shown = useColumnShown(sheetId);
  return (
    <>
      {shown('claimed') && <th>Claimed</th>}
      <th>Claims</th>
    </>
  );
}

// A person's claimed total, and their claims, newest first, under the form that adds or edits them. A claim's edit and
// cancel buttons are in its status cell, with its locking.
function ClaimsCells({ sheetId, user, personId }: { sheetId: string; user: string; personId: number }) {
  const shown = useColumnShown(sheetId);
  const claims = useClaims(sheetId);
  const [editingId] = useEditingClaim();
  const held = useHeld(sheetId);
  const mine = (claims.data ?? []).filter((c) => c.personId === personId);
  // Only a DRAFT can be edited: once the claim being edited is no longer one (another user opened it), its form adds.
  const editing = mine.find((c) => c.id === editingId && c.status === 'DRAFT');

  return (
    <>
      {shown('claimed') && <td data-testid="person-claimed">{sumAmounts(mine, held)}</td>}
      <td>
        <ClaimForm key={editing ? `edit-${editing.id}` : 'add'} sheetId={sheetId} personId={personId} editing={editing} />
        <ul data-testid="claim-list">
          {mine.map((c) => (
            <li key={c.id} data-testid={`claim-row-${c.id}`}>
              <span data-testid="claim-amount">{Number(c.amount).toFixed(2)}</span>{' '}
              <span data-testid="claim-date">{c.date}</span>{' '}
              <span data-testid="claim-note">{c.note}</span>{' '}
              <span data-testid="claim-corrects">{c.corrects ?? ''}</span>{' '}
              <span data-testid="claim-ref">{c.ref ?? ''}</span>{' '}
              <span data-testid="claim-tags">{c.tags.join(', ')}</span>{' '}
              <ClaimCells.Slot sheetId={sheetId} user={user} personId={personId} claim={c} />
            </li>
          ))}
        </ul>
      </td>
    </>
  );
}

// The sheet's claimed-total: everyone's counted claims, corrections left out, added up.
function ClaimedTotal({ sheetId }: { sheetId: string; user: string }) {
  const claims = useClaims(sheetId);
  const held = useHeld(sheetId);
  return (
    <section data-testid="claims-rollup">
      <p>
        Claimed total: <strong data-testid="rollup-claimed-total">{sumAmounts(claims.data ?? [], held)}</strong>
      </p>
      {claims.isError && (
        <p data-testid="claims-error" role="alert">
          {claims.error.message}
        </p>
      )}
    </section>
  );
}

export const contributions = [
  PersonHeads.fill({ order: 10, Component: ClaimsHead }),
  PersonCells.fill({ order: 10, Component: ClaimsCells }),
  SheetBody.fill({ order: 20, Component: ClaimedTotal }),
];
