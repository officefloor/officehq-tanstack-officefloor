import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { postJson, RefusedError } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { useUser } from '../session/useUser';
import { templatesKey, templatesQuery } from './templatesQuery';
import type { Template } from './templatesQuery';

/** The sheet's claim templates, and the form a template is saved through. Saving a name again replaces that template;
 *  a blank name, or an amount the claim form would refuse, is refused. */
function Templates({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const templates = useQuery(templatesQuery(sheetId));
  const [name, setName] = useState('');
  const [amount, setAmount] = useState('');
  const [note, setNote] = useState('');
  const [error, setError] = useState('');

  const [user] = useUser();
  const once = useSubmitOnce();
  const save = useMutation({
    mutationFn: (body: { name: string; amount: string; note: string }) =>
      postJson<Template>(`/api/sheets/${encodeURIComponent(sheetId)}/templates`, body, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: templatesKey(sheetId) });
      setName('');
      setAmount('');
      setNote('');
    },
    onError: (e: Error) => setError(e instanceof RefusedError ? e.message : 'The template could not be saved.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    once(() => save.mutateAsync({ name: name.trim(), amount: amount.trim(), note: note.trim() }));
  };

  const edit = (set: (v: string) => void) => (v: string) => {
    set(v);
    setError('');
  };

  return (
    <section data-testid="templates">
      <h3>Claim templates</h3>
      <form onSubmit={submit}>
        <input data-testid="template-form-name" placeholder="Name" value={name} onChange={(e) => edit(setName)(e.target.value)} />{' '}
        <input data-testid="template-form-amount" placeholder="Amount" value={amount} onChange={(e) => edit(setAmount)(e.target.value)} />{' '}
        <input data-testid="template-form-note" placeholder="Note" value={note} onChange={(e) => edit(setNote)(e.target.value)} />{' '}
        <button type="submit" data-testid="template-form-submit" disabled={save.isPending}>
          Save template
        </button>
        {error && <p data-testid="template-form-error">{error}</p>}
      </form>
      {templates.isError && <p data-testid="templates-load-error">Could not load the templates.</p>}
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

// Keyed by sheet, so a typed value or an error never carries over to another sheet.
export const contribution = SheetPanel.fill({
  order: 6.7,
  Component: ({ sheetId }) => <Templates key={`templates-${sheetId}`} sheetId={sheetId} />,
});
