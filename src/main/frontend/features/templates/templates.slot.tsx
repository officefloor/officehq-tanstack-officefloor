import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { templatesKey, templatesPath, useTemplates, type ClaimTemplate } from './templates';
import { Fig } from '../locale/Fig';

type Draft = { name: string; amount: string; note: string };
const EMPTY: Draft = { name: '', amount: '', note: '' };

// The sheet's claim templates, and a form that saves one (a name saved again replaces it). Each person's row adds a
// DRAFT claim from a template, copying its amount and note as they are then.
function Templates({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const { data: templates = [] } = useTemplates(sheet);
  const [draft, setDraft] = useState<Draft>(EMPTY);

  const save = useMutation({
    mutationFn: (body: Draft) => postJson<ClaimTemplate[]>(byUser(templatesPath(sheet), user), body),
    onSuccess: (updated) => {
      queryClient.setQueryData(templatesKey(sheet), updated);
      setDraft(EMPTY);
    },
  });
  const send = useSubmitOnce(save);

  const change = (next: Partial<Draft>) => {
    setDraft((d) => ({ ...d, ...next }));
    save.reset();
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    // The server refuses a blank name or an amount the claim form would refuse.
    send(draft);
  };

  return (
    <section>
      <h2>Claim templates</h2>
      <form onSubmit={submit}>
        <input data-testid="template-form-name" placeholder="name" value={draft.name} onChange={(e) => change({ name: e.target.value })} />{' '}
        <input data-testid="template-form-amount" placeholder="amount" value={draft.amount} onChange={(e) => change({ amount: e.target.value })} />{' '}
        <input data-testid="template-form-note" placeholder="note" value={draft.note} onChange={(e) => change({ note: e.target.value })} />{' '}
        <button type="submit" data-testid="template-form-submit" disabled={save.isPending}>
          Save template
        </button>
        {save.isError && (
          <span data-testid="template-form-error" role="alert">
            {' '}A template needs a name and an amount with up to two decimals, not negative.
          </span>
        )}
      </form>
      <ul>
        {templates.map((t) => (
          <li key={t.name} data-testid={`template-row-${t.name}`}>
            <span data-testid="template-name">{t.name}</span>{' '}
            <span data-testid="template-amount"><Fig>{t.amount}</Fig></span>{' '}
            <span data-testid="template-note">{t.note}</span>
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 9.7, Component: Templates });
