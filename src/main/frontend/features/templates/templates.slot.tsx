import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { AMOUNT } from '../claims/useClaims';
import { templatesKey, useTemplates, type ClaimTemplate } from './useTemplates';

/**
 * The sheet's claim templates and a form to save one: a name, an amount and a note. Saving a name
 * again replaces its template; a blank name, or an amount the claim form would refuse, is refused.
 * Each person's row adds a DRAFT claim from one (see the claims feature).
 */
function TemplatesPanel({ user, sheet }: { user: string; sheet: string }) {
  const queryClient = useQueryClient();
  const { data: templates = [] } = useTemplates(sheet);
  const [name, setName] = useState('');
  const [amount, setAmount] = useState('');
  const [note, setNote] = useState('');
  const [invalid, setInvalid] = useState(false);

  const submitOnce = useSubmitOnce();
  const save = useMutation({
    mutationFn: (t: ClaimTemplate) =>
      postJson<ClaimTemplate>(`/api/sheets/${encodeURIComponent(sheet)}/claim-templates`, { ...t, user }),
    onSuccess: async () => {
      setName('');
      setAmount('');
      setNote('');
      await queryClient.invalidateQueries({ queryKey: templatesKey(sheet) });
    },
  });

  // An error goes as soon as its form is changed or submitted again.
  const clearError = () => {
    save.reset();
    setInvalid(false);
  };

  return (
    <section data-testid="templates-panel">
      <h2>Claim templates</h2>
      <form
        data-testid="template-form"
        onSubmit={(e) => {
          e.preventDefault();
          const n = name.trim();
          const a = amount.trim();
          // A template names no correction, so a negative amount is refused as the claim form would.
          if (n === '' || !AMOUNT.test(a) || a.startsWith('-')) {
            // Refused here, before anything is sent: the store and the audit file are untouched.
            save.reset();
            setInvalid(true);
            return;
          }
          setInvalid(false);
          submitOnce(() => save.mutateAsync({ name: n, amount: a, note }));
        }}
      >
        <input
          data-testid="template-form-name"
          aria-label="Template name"
          placeholder="name"
          value={name}
          onChange={(e) => {
            setName(e.target.value);
            clearError();
          }}
        />{' '}
        <input
          data-testid="template-form-amount"
          aria-label="Template amount"
          placeholder="0.00"
          value={amount}
          onChange={(e) => {
            setAmount(e.target.value);
            clearError();
          }}
        />{' '}
        <input
          data-testid="template-form-note"
          aria-label="Template note"
          placeholder="note"
          value={note}
          onChange={(e) => {
            setNote(e.target.value);
            clearError();
          }}
        />{' '}
        <button type="submit" data-testid="template-form-submit" disabled={save.isPending}>
          Save template
        </button>
        {(invalid || save.isError) && (
          <span data-testid="template-form-error" role="alert">
            {' '}
            {invalid
              ? 'Enter a name and an amount with up to two decimals, not negative.'
              : 'Could not save this template.'}
          </span>
        )}
      </form>
      <table data-testid="template-list">
        <tbody>
          {templates.map((t) => (
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

export const contribution = SheetPanels.fill({ order: 9, Component: TemplatesPanel });
