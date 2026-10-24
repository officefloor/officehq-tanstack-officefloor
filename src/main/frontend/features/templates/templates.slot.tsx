import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { templatesPath, templatesQueryKey, useTemplates, type ClaimTemplate } from './templates';

// The sheet's claim templates, and the form that saves one. Saving a name again replaces that template. A blank
// name, or an amount the claim form would refuse, is refused by the server and shows the error beside the form,
// which goes as soon as the form changes.
function Templates() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const templates = useTemplates();
  const [name, setName] = useState('');
  const [amount, setAmount] = useState('');
  const [note, setNote] = useState('');
  const queryClient = useQueryClient();
  const save = useMutation({
    mutationFn: (body: { name: string; amount: string; note: string }) =>
      putJson<ClaimTemplate>(templatesPath(sheet ?? ''), body, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: templatesQueryKey(sheet ?? '') });
      setName('');
      setAmount('');
      setNote('');
    },
  });
  const submit = useSubmitOnce(save);

  if (!user || !sheet) return null;
  return (
    <section data-testid="templates">
      <h2>Claim templates</h2>
      <ul>
        {(templates.data ?? []).map((t) => (
          <li key={t.id} data-testid={`template-row-${t.name}`}>
            {t.name}{' '}
            <span data-testid="template-amount">{t.amount}</span>{' '}
            <span data-testid="template-note">{t.note}</span>
          </li>
        ))}
      </ul>
      <form
        data-testid="template-form"
        onSubmit={(e) => {
          e.preventDefault();
          submit({ name, amount, note });
        }}
      >
        <input
          data-testid="template-form-name"
          placeholder="Template name"
          value={name}
          onChange={(e) => { setName(e.target.value); save.reset(); }}
        />
        <input
          data-testid="template-form-amount"
          placeholder="Amount"
          inputMode="decimal"
          value={amount}
          onChange={(e) => { setAmount(e.target.value); save.reset(); }}
        />
        <input
          data-testid="template-form-note"
          placeholder="Note"
          value={note}
          onChange={(e) => { setNote(e.target.value); save.reset(); }}
        />
        <button type="submit" data-testid="template-form-submit" disabled={save.isPending}>
          Save template
        </button>
        {save.isError && (
          <span data-testid="template-form-error">
            A template needs a name, and an amount the claim form accepts (a number with up to two decimals, not negative).
          </span>
        )}
      </form>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 6.7, Component: Templates });
