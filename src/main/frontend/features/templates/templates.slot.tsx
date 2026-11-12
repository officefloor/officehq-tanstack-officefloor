import { type ChangeEvent, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { type Template, templatesKey, useTemplates } from './templates';
import { useFigure } from '../locale/locale';

type Fields = { name: string; amount: string; note: string };
const EMPTY: Fields = { name: '', amount: '', note: '' };

// The sheet's claim templates, and the form a template is saved with. A blank name, or an amount the
// claim form would refuse, is refused; saving a name again replaces that template.
function Templates() {
  const fig = useFigure();
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const templates = useTemplates(sheet);
  const [fields, setFields] = useState<Fields>(EMPTY);
  const queryClient = useQueryClient();

  const save = useMutation({
    mutationFn: (f: Fields) =>
      postJson<Template>(`/api/sheets/${encodeURIComponent(sheet)}/templates`, { ...f, user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: templatesKey(sheet) });
      setFields(EMPTY);
    },
  });
  const submit = useSubmitOnce(save);
  const set = (key: keyof Fields) => (e: ChangeEvent<HTMLInputElement>) => {
    setFields({ ...fields, [key]: e.target.value });
    save.reset();
  };

  return (
    <section data-testid="templates">
      <h2>Claim templates</h2>
      <ul>
        {(templates.data ?? []).map((t) => (
          <li key={t.name} data-testid={`template-row-${t.name}`}>
            <span data-testid="template-name">{t.name}</span>{' '}
            <span data-testid="template-amount">{fig(t.amount)}</span>{' '}
            <span data-testid="template-note">{t.note}</span>
          </li>
        ))}
      </ul>
      <form
        data-testid="template-form"
        onSubmit={(e) => {
          e.preventDefault();
          submit(fields);
        }}
      >
        <input data-testid="template-form-name" placeholder="Template name" value={fields.name} onChange={set('name')} />
        <input
          data-testid="template-form-amount"
          placeholder="Amount"
          inputMode="decimal"
          value={fields.amount}
          onChange={set('amount')}
        />
        <input data-testid="template-form-note" placeholder="Note" value={fields.note} onChange={set('note')} />
        <button type="submit" data-testid="template-form-submit" disabled={save.isPending}>
          Save template
        </button>
        {save.isError && (
          <span data-testid="template-form-error" role="alert">
            {' '}
            {save.error instanceof HttpError && save.error.reason ? `${save.error.reason}.` : 'The template was refused.'}
          </span>
        )}
      </form>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 47.6, Component: Templates });
