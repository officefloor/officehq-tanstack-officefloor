import { useState, type ChangeEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSingleFlight } from '../../query/useSingleFlight';
import { SheetBody } from '../../slots/defs/sheetBody';
import { templatesKey, useTemplates, type Template } from './useTemplates';
import { useFigure } from '../locale/useLocale';

/** The sheet's claim templates: a form that saves one (a name, an amount the claim form would take, a
 *  note; saving a name again replaces it) and the templates saved. A person's row adds a claim from one. */
function Templates({ sheetId }: { sheetId: string }) {
  const fig = useFigure();
  const templates = useTemplates(sheetId);
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [amount, setAmount] = useState('');
  const [note, setNote] = useState('');

  const submitOnce = useSingleFlight();
  const save = useMutation({
    mutationFn: (f: { name: string; amount: string; note: string }) =>
      postJson<Template>(`/api/sheets/${encodeURIComponent(sheetId)}/templates`, f),
    onSuccess: async () => {
      setName('');
      setAmount('');
      setNote('');
      await queryClient.invalidateQueries({ queryKey: templatesKey(sheetId) });
    },
  });

  const field = (set: (v: string) => void) => (e: ChangeEvent<HTMLInputElement>) => {
    set(e.target.value);
    save.reset();
  };

  return (
    <section data-testid="templates">
      <form
        data-testid="template-form"
        onSubmit={(e) => {
          e.preventDefault();
          submitOnce(() => {
            save.reset();
            return save.mutateAsync({ name, amount, note });
          });
        }}
      >
        <label>
          Template <input data-testid="template-form-name" value={name} onChange={field(setName)} />
        </label>{' '}
        <label>
          Amount <input data-testid="template-form-amount" inputMode="decimal" value={amount} onChange={field(setAmount)} />
        </label>{' '}
        <label>
          Note <input data-testid="template-form-note" value={note} onChange={field(setNote)} />
        </label>{' '}
        <button type="submit" data-testid="template-form-submit" disabled={save.isPending}>
          Save template
        </button>
        {save.isError && <p data-testid="template-form-error">{save.error.message}</p>}
      </form>
      <table data-testid="templates-list">
        <tbody>
          {(templates.data ?? []).map((t) => (
            <tr key={t.name} data-testid={`template-row-${t.name}`}>
              <td data-testid="template-name">{t.name}</td>
              <td data-testid="template-amount">{fig(t.amount)}</td>
              <td data-testid="template-note">{t.note}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {templates.isError && <p data-testid="templates-error">{templates.error.message}</p>}
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 6, Component: ({ sheetId }) => <Templates sheetId={sheetId} /> });
