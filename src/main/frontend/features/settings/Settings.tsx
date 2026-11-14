import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { changeSetting, listSettings, settingsKey, SETTING_KEYS } from './api';

/** The sheet's settings: each one's value in force and a form that changes them. A field shows the
 *  value in force until it is edited; saving saves every field. */
export function Settings({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const settings = useQuery({ queryKey: settingsKey(sheetId), queryFn: () => listSettings(sheetId) });
  const current = (key: string) => settings.data?.find((s) => s.key === key)?.value ?? '';
  const [form, setForm] = useState<Record<string, string>>({});

  const save = useMutation({
    mutationFn: async (values: Record<string, string>) => {
      for (const [key, value] of Object.entries(values)) await changeSetting(sheetId, user, key, value);
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: settingsKey(sheetId) }),
  });

  const submitOnce = useSubmitOnce();

  const change = (key: string, value: string) => {
    setForm((f) => ({ ...f, [key]: value }));
    save.reset();
  };

  return (
    <section>
      <h3>Settings</h3>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          const values = Object.fromEntries(SETTING_KEYS.map((key) => [key, form[key] ?? current(key)]));
          submitOnce(() => {
            save.reset();
            return save.mutateAsync(values).then(() => setForm({}));
          });
        }}
      >
        {SETTING_KEYS.map((key) => (
          <div key={key}>
            <label>
              {key === 'clerk-limit' ? 'Clerk limit' : key}{' '}
              <input data-testid={`setting-${key}`} value={form[key] ?? current(key)} placeholder="No limit"
                onChange={(e) => change(key, e.target.value)} />
            </label>{' '}
            Now: <span data-testid={`setting-${key}-current`}>{current(key)}</span>
          </div>
        ))}
        <button type="submit" data-testid="settings-save" disabled={save.isPending}>
          Save settings
        </button>
        {save.isError && (
          <span data-testid="settings-error">The clerk limit must be blank or 0.00 or more with up to two decimals.</span>
        )}
      </form>
    </section>
  );
}
