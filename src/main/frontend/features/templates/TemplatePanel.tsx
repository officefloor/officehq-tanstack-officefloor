import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { templatesKey, useTemplates, type Template } from './useTemplates';

// The sheet's claim templates and the form that saves one: a blank name, or an amount the claim form
// would refuse, is refused; saving a name again replaces that template. Claims already made from a
// template keep the amount and note they were made with.
export function TemplatePanel() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const templates = useTemplates(sheet);
  const [name, setName] = useState('');
  const [amount, setAmount] = useState('');
  const [note, setNote] = useState('');
  const queryClient = useQueryClient();
  const submitOnce = useSubmitOnce();
  const save = useMutation({
    mutationFn: (body: { name: string; amount: string; note: string }) =>
      postJson<Template[]>(`/api/sheets/${encodeURIComponent(sheet)}/templates`, { ...body, user }),
    onSuccess: (saved) => {
      setName('');
      setAmount('');
      setNote('');
      queryClient.setQueryData(templatesKey(sheet), saved);
    },
  });
  const changed = (set: (v: string) => void) => (v: string) => {
    set(v);
    save.reset();
  };
  return (
    <>
      <ul data-testid="template-list">
        {templates.map((t) => (
          <li key={t.name} data-testid={`template-row-${t.name}`}>
            <span data-testid="template-name">{t.name}</span>{' '}
            <span data-testid="template-amount">{t.amount}</span>{' '}
            <span data-testid="template-note">{t.note}</span>
          </li>
        ))}
      </ul>
      <form
        data-testid="template-form"
        onSubmit={(e) => {
          e.preventDefault();
          submitOnce(() => {
            save.reset();
            return save.mutateAsync({ name: name.trim(), amount: amount.trim(), note });
          });
        }}
      >
        <input data-testid="template-form-name" value={name} placeholder="Template name" onChange={(e) => changed(setName)(e.target.value)} />{' '}
        <input
          data-testid="template-form-amount"
          value={amount}
          placeholder="Amount"
          inputMode="decimal"
          onChange={(e) => changed(setAmount)(e.target.value)}
        />{' '}
        <input data-testid="template-form-note" value={note} placeholder="Note" onChange={(e) => changed(setNote)(e.target.value)} />{' '}
        <button type="submit" data-testid="template-form-submit">Save template</button>
        {save.isError && (
          <span data-testid="template-form-error" role="alert"> Could not save the template: the name must not be blank and the amount must be 0.00 or more with at most two decimals.</span>
        )}
      </form>
    </>
  );
}
