import { useState, type ChangeEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useSubmitOnce } from '../../mutation/useSubmitOnce';
import { useUser } from '../session/useSession';

/** A claim template of the sheet: its name, amount (two decimals) and note. */
export type Template = { name: string; amount: string; note: string };

/** The key every feature reads the sheet's claim templates under. */
export const templatesKey = (sheetId: string) => ['sheet', sheetId, 'templates'] as const;

/** The sheet's claim templates, by name. */
export function useTemplates(sheetId: string) {
  return useQuery({
    queryKey: templatesKey(sheetId),
    queryFn: () => getJson<Template[]>(`/api/sheets/${encodeURIComponent(sheetId)}/templates`),
  });
}

type TemplateBody = { name: string; amount: string; note: string };

// The claim templates: a form saves one (a name saved again replaces that template); a blank name or an amount the
// claim form would refuse is refused. A person's row adds a claim from one (see the claim form).
function TemplatesPanel({ sheetId }: { sheetId: string; user: string }) {
  const templates = useTemplates(sheetId);
  const [name, setName] = useState('');
  const [amount, setAmount] = useState('');
  const [note, setNote] = useState('');
  const [user] = useUser();
  const queryClient = useQueryClient();
  const save = useMutation({
    mutationFn: async (body: TemplateBody) => {
      if (body.name.trim() === '') throw new Error('Enter a template name');
      // `by` is the signed-in user the change is recorded against.
      return postJson<Template>(`/api/sheets/${encodeURIComponent(sheetId)}/templates`, { ...body, by: user ?? '' });
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: templatesKey(sheetId) });
      setName('');
      setAmount('');
      setNote('');
    },
  });
  const field = (set: (v: string) => void) => (e: ChangeEvent<HTMLInputElement>) => {
    set(e.target.value);
    save.reset();
  };

  const submit = useSubmitOnce(save);
  return (
    <section data-testid="templates-panel">
      <h2>Claim templates</h2>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          submit({ name, amount, note });
        }}
      >
        <input data-testid="template-form-name" placeholder="Name" value={name} onChange={field(setName)} />{' '}
        <input data-testid="template-form-amount" placeholder="Amount" inputMode="decimal" value={amount} onChange={field(setAmount)} />{' '}
        <input data-testid="template-form-note" placeholder="Note" value={note} onChange={field(setNote)} />{' '}
        <button type="submit" data-testid="template-form-submit" disabled={save.isPending}>
          Save template
        </button>
        {save.isError && (
          <span data-testid="template-form-error" role="alert">
            {' '}
            {save.error.message}
          </span>
        )}
      </form>
      <ul data-testid="template-list">
        {(templates.data ?? []).map((t) => (
          <li key={t.name} data-testid={`template-row-${t.name}`}>
            <span data-testid="template-name">{t.name}</span>{' '}
            <span data-testid="template-amount">{Number(t.amount).toFixed(2)}</span>{' '}
            <span data-testid="template-note">{t.note}</span>
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 8.6, Component: TemplatesPanel });
