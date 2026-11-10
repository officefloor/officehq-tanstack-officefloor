import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useUser } from '../session/session';
import { AMOUNT } from '../claims/claims';
import { templatesKey, useTemplates } from './templates';
import type { Template } from './templates';

type Fields = { name: string; amount: string; note: string };
const EMPTY: Fields = { name: '', amount: '', note: '' };

// The sheet's claim templates: a form that saves one (a name, an amount and a note; saving a name
// again replaces it), each listed below. A blank name, or an amount the claim form would refuse, is
// refused. Each person's row adds a draft claim from one.
function Templates({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const templates = useTemplates(sheetId);
  const [fields, setFields] = useState<Fields>(EMPTY);
  const [error, setError] = useState('');
  const [user] = useUser();

  const once = useOnce();
  const save = useMutation({
    mutationFn: (body: Fields) =>
      postJson<Template>(`/api/sheets/${encodeURIComponent(sheetId)}/templates`, body, user),
    onSuccess: () => {
      setFields(EMPTY);
      return queryClient.invalidateQueries({ queryKey: templatesKey(sheetId) });
    },
    onError: () => setError('That template could not be saved.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const name = fields.name.trim();
    const amount = fields.amount.trim();
    if (name === '') {
      setError('A template name is required.');
      return;
    }
    if (amount === '') {
      setError('An amount is required.');
      return;
    }
    if (!AMOUNT.test(amount)) {
      setError('An amount is a number with up to two decimals.');
      return;
    }
    once((done) => save.mutate({ name, amount, note: fields.note }, { onSettled: done }));
  };

  const change = (next: Partial<Fields>) => {
    setFields((f) => ({ ...f, ...next }));
    setError('');
  };

  return (
    <section data-testid="templates">
      <h2>Claim templates</h2>
      <form onSubmit={submit}>
        <input
          data-testid="template-form-name"
          placeholder="Name"
          value={fields.name}
          onChange={(e) => change({ name: e.target.value })}
        />{' '}
        <input
          data-testid="template-form-amount"
          placeholder="Amount"
          inputMode="decimal"
          value={fields.amount}
          onChange={(e) => change({ amount: e.target.value })}
        />{' '}
        <input
          data-testid="template-form-note"
          placeholder="Note"
          value={fields.note}
          onChange={(e) => change({ note: e.target.value })}
        />{' '}
        <button type="submit" data-testid="template-form-submit" disabled={save.isPending}>
          Save template
        </button>
        {error && (
          <p role="alert" data-testid="template-form-error">
            {error}
          </p>
        )}
      </form>
      <table>
        <tbody>
          {(templates.data ?? []).map((t) => (
            <tr key={t.name} data-testid={`template-row-${t.name}`}>
              <td data-testid="template-name">{t.name}</td>
              <td data-testid="template-amount">{t.amount}</td>
              <td data-testid="template-note">{t.note}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: -5.4, Component: Templates });
