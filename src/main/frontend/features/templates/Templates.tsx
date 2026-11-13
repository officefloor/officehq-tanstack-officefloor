import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { AMOUNT } from '../claims/claims';
import { templatesKey, useTemplates } from './templates';
import type { ClaimTemplate } from './templates';
import { useOneAtATime } from '../../query/useOneAtATime';

const EMPTY = { name: '', amount: '', note: '' };

// The sheet's claim templates and the form that saves one; a blank name or an amount the claim form would refuse is
// refused, and saving a name again replaces its template.
export function Templates() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const templates = useTemplates().data ?? [];
  const [form, setForm] = useState(EMPTY);
  const [error, setError] = useState('');
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const save = useMutation({
    mutationFn: (input: typeof EMPTY) =>
      postJson<ClaimTemplate[]>(`/api/sheets/${encodeURIComponent(sheet)}/templates?user=${encodeURIComponent(user)}`, input),
    onSuccess: async (saved) => {
      setForm(EMPTY);
      setError('');
      queryClient.setQueryData(templatesKey(sheet), saved);
      await queryClient.invalidateQueries({ queryKey: templatesKey(sheet) });
    },
    onError: () => setError('That template was refused: it needs a name and an amount the claim form accepts.'),
  });

  if (!user || !sheet) return null;

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (!form.name.trim()) {
      setError('Type the template name.');
      return;
    }
    if (!AMOUNT.test(form.amount.trim())) {
      setError('Type an amount with at most two decimals.');
      return;
    }
    once(() => save.mutateAsync(form));
  };

  const change = (patch: Partial<typeof EMPTY>) => {
    setForm((f) => ({ ...f, ...patch }));
    setError('');
  };

  return (
    <section data-testid="templates">
      <ul data-testid="template-list">
        {templates.map((t) => (
          <li key={t.name} data-testid={`template-row-${t.name}`}>
            <span data-testid="template-name">{t.name}</span> <span data-testid="template-amount">{t.amount}</span>{' '}
            <span data-testid="template-note" style={{ whiteSpace: 'pre' }}>
              {t.note}
            </span>
          </li>
        ))}
      </ul>
      <form onSubmit={submit} data-testid="template-form">
        <input data-testid="template-form-name" placeholder="Template name" value={form.name} onChange={(e) => change({ name: e.target.value })} />
        <input data-testid="template-form-amount" placeholder="Amount" value={form.amount} onChange={(e) => change({ amount: e.target.value })} />
        <input data-testid="template-form-note" placeholder="Note" value={form.note} onChange={(e) => change({ note: e.target.value })} />
        <button type="submit" data-testid="template-form-submit" disabled={save.isPending}>
          Save template
        </button>
        {error && <span data-testid="template-form-error">{error}</span>}
      </form>
    </section>
  );
}
