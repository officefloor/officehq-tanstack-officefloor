import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPage } from '../../slots/defs/sheetPage';
import { templatesKey, useTemplates, type Template } from './templates';

type Values = { name: string; amount: string; note: string };
const EMPTY: Values = { name: '', amount: '', note: '' };

/** The sheet's claim templates: the form that saves one (saving a name again replaces it), and each template with its
 *  amount and note. A blank name, or an amount the claim form would refuse, is refused. */
function Templates({ sheetId, user }: { sheetId: string; user: string }) {
  const templates = useTemplates(sheetId);
  const queryClient = useQueryClient();
  const [values, setValues] = useState<Values>(EMPTY);

  const submit = useSubmitOnce();
  const save = useMutation({
    mutationFn: (v: Values) => postJson<Template>(`/api/sheets/${encodeURIComponent(sheetId)}/templates`, { ...v, by: user }),
    onSuccess: async () => {
      setValues(EMPTY);
      await queryClient.invalidateQueries({ queryKey: templatesKey(sheetId) });
    },
    onSettled: submit.end,
  });
  const change = (v: Partial<Values>) => {
    setValues((was) => ({ ...was, ...v }));
    save.reset();
  };

  return (
    <section data-testid="templates">
      <h3>Claim templates</h3>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          if (!submit.begin()) return;
          save.reset();
          save.mutate(values);
        }}
      >
        <input data-testid="template-form-name" placeholder="Name" value={values.name} onChange={(e) => change({ name: e.target.value })} />{' '}
        <input
          data-testid="template-form-amount"
          placeholder="Amount"
          inputMode="decimal"
          value={values.amount}
          onChange={(e) => change({ amount: e.target.value })}
        />{' '}
        <input data-testid="template-form-note" placeholder="Note" value={values.note} onChange={(e) => change({ note: e.target.value })} />{' '}
        <button type="submit" data-testid="template-form-submit" disabled={save.isPending}>
          Save template
        </button>
        {save.isError && (
          <span data-testid="template-form-error"> A template needs a name, and an amount 0.00 or more with up to two decimals.</span>
        )}
      </form>
      {templates.isError && <p data-testid="templates-load-error">Could not load templates.</p>}
      <ul>
        {(templates.data ?? []).map((t) => (
          <li key={t.name} data-testid={`template-row-${t.name}`}>
            {t.name} <span data-testid="template-amount">{t.amount}</span> <span data-testid="template-note">{t.note}</span>
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetPage.fill({ order: 9.6, Component: Templates });
