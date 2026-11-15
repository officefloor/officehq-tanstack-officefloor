import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { listTemplates, saveTemplate, templatesKey, type TemplateInput } from './api';

const EMPTY: TemplateInput = { name: '', amount: '', note: '' };

/** The sheet's claim templates and a form that saves one (a blank name, or an amount the claim form
 *  would refuse, refused; saving a name again replaces that template). Each person's row adds a claim
 *  from one. */
export function Templates({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const templates = useQuery({ queryKey: templatesKey(sheetId), queryFn: () => listTemplates(sheetId) });
  const [form, setForm] = useState<TemplateInput>(EMPTY);

  const save = useMutation({
    mutationFn: (input: TemplateInput) => saveTemplate(sheetId, user, input),
    onSuccess: () => {
      setForm(EMPTY);
      return queryClient.invalidateQueries({ queryKey: templatesKey(sheetId) });
    },
  });

  const submitOnce = useSubmitOnce();

  const change = (patch: Partial<TemplateInput>) => {
    setForm((f) => ({ ...f, ...patch }));
    save.reset();
  };

  return (
    <section>
      <h3>Claim templates</h3>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          const input = form;
          submitOnce(() => {
            save.reset();
            return save.mutateAsync(input);
          });
        }}
      >
        <input data-testid="template-form-name" placeholder="Name" value={form.name}
          onChange={(e) => change({ name: e.target.value })} />{' '}
        <input data-testid="template-form-amount" placeholder="Amount" value={form.amount}
          onChange={(e) => change({ amount: e.target.value })} />{' '}
        <input data-testid="template-form-note" placeholder="Note" value={form.note}
          onChange={(e) => change({ note: e.target.value })} />{' '}
        <button type="submit" data-testid="template-form-submit" disabled={save.isPending}>
          Save template
        </button>
        {save.isError && (
          <span data-testid="template-form-error">A template needs a name and an amount with up to two decimals.</span>
        )}
      </form>
      <table>
        <tbody>
          {(templates.data ?? []).map((t) => (
            <tr key={t.name} data-testid={`template-row-${t.name}`}>
              <td data-testid="template-name">{t.name}</td>
              <td data-testid="template-amount">{Number(t.amount).toFixed(2)}</td>
              <td data-testid="template-note">{t.note}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
