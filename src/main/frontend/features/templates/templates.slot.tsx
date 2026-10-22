import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetBody } from '../../slots/defs/sheet';
import { templatesKey, useTemplates } from './templates';
import type { Template } from './templates';

const AMOUNT = /^\d+(\.\d{1,2})?$/;

function TemplateList({ sheetId }: { sheetId: string }) {
  const templates = useTemplates(sheetId).data ?? [];
  return (
    <ul>
      {templates.map((t) => (
        <li key={t.name} data-testid={`template-row-${t.name}`}>
          <span data-testid="template-name">{t.name}</span>{' '}
          <span data-testid="template-amount">{t.amount}</span>{' '}
          <span data-testid="template-note">{t.note}</span>
        </li>
      ))}
    </ul>
  );
}

/** The form a template is saved with: saving a name again replaces that template (claims made from it keep theirs).
 *  A blank name or an amount the claim form would refuse is refused. */
function TemplateForm({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [amount, setAmount] = useState('');
  const [note, setNote] = useState('');
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const save = useMutation({
    mutationFn: (body: { user: string; name: string; amount: string; note: string }) =>
      postJson<Template>(`/api/sheets/${encodeURIComponent(sheetId)}/templates`, body),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: templatesKey(sheetId) });
      setName('');
      setAmount('');
      setNote('');
    },
    onError: (e) => setError(e instanceof HttpError && e.body ? e.body : 'Could not save the template.'),
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (name.trim() === '') {
      setError('Give the template a name.');
      return;
    }
    if (!AMOUNT.test(amount.trim())) {
      setError('The amount must be a number of 0.00 or more with up to two decimals.');
      return;
    }
    if (!once.start()) return;
    save.mutate({ user, name: name.trim(), amount: amount.trim(), note });
  };

  const change = (set: (v: string) => void) => (e: { target: { value: string } }) => {
    set(e.target.value);
    setError('');
  };

  return (
    <form data-testid="template-form" onSubmit={submit}>
      <input data-testid="template-form-name" placeholder="Template name" value={name} onChange={change(setName)} />{' '}
      <input data-testid="template-form-amount" placeholder="Amount" value={amount} onChange={change(setAmount)} />{' '}
      <input data-testid="template-form-note" placeholder="Note" value={note} onChange={change(setNote)} />{' '}
      <button type="submit" data-testid="template-form-submit" disabled={save.isPending}>
        Save template
      </button>
      {error && (
        <p data-testid="template-form-error" role="alert">
          {error}
        </p>
      )}
    </form>
  );
}

/** The sheet's claim templates and the form they are saved with. Each person's row adds a DRAFT claim from one. */
function Templates({ sheetId, user }: { sheetId: string; user: string }) {
  return (
    <section data-testid="templates">
      <h2>Claim templates</h2>
      <TemplateForm sheetId={sheetId} user={user} />
      <TemplateList sheetId={sheetId} />
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 7.7, Component: Templates });
