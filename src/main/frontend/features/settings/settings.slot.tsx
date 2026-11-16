import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPage } from '../../slots/defs/sheetPage';
import { settingsKey, useSettings, type Setting } from './settings';

/** The sheet's settings: the clerk limit (the most a clerk may settle a claim's due for; blank for no limit). */
function Settings({ sheetId, user }: { sheetId: string; user: string }) {
  const settings = useSettings(sheetId);
  const queryClient = useQueryClient();
  const [limit, setLimit] = useState('');

  const submit = useSubmitOnce();
  const save = useMutation({
    mutationFn: (value: string) =>
      putJson<Setting>(`/api/sheets/${encodeURIComponent(sheetId)}/settings/clerk-limit`, { value, by: user }),
    onSuccess: () => {
      setLimit('');
      void queryClient.invalidateQueries({ queryKey: settingsKey(sheetId) });
    },
    onSettled: submit.end,
  });
  const current = settings.data?.find((s) => s.key === 'clerk-limit')?.value ?? '';

  return (
    <section data-testid="settings">
      <h3>Settings</h3>
      <form
        data-testid="settings-form"
        onSubmit={(e) => {
          e.preventDefault();
          if (!submit.begin()) return;
          save.reset();
          save.mutate(limit);
        }}
      >
        <label>
          Clerk limit{' '}
          <input
            data-testid="setting-clerk-limit"
            inputMode="decimal"
            value={limit}
            onChange={(e) => {
              setLimit(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="settings-save" disabled={save.isPending}>
          Save settings
        </button>
        {save.isError && (
          <span data-testid="settings-error">The clerk limit must be an amount with up to two decimals, or blank.</span>
        )}
      </form>
      {settings.isError && <p data-testid="settings-load-error">Could not load settings.</p>}
      <p>
        Clerk limit in force <span data-testid="setting-clerk-limit-current">{current}</span>
      </p>
    </section>
  );
}

export const contribution = SheetPage.fill({ order: 6, Component: Settings });
