import { useRef, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPage } from '../../slots/defs/sheetPage';
import { DEFAULT_FLOOR, settingsKey, useSettings, type Setting } from './settings';

type Key = 'clerk-limit' | 'floor';
const KEYS: Key[] = ['clerk-limit', 'floor'];

/** The sheet's settings: the clerk limit (the most a clerk may settle a claim's due for; blank for no limit) and the
 *  floor (the least a claim's live due may be; 1.00 until set). Saving stores the fields entered since the last save
 *  (a field focused counts, so a blank typed over a blank still saves), the clerk limit when none was. */
function Settings({ sheetId, user }: { sheetId: string; user: string }) {
  const settings = useSettings(sheetId);
  const queryClient = useQueryClient();
  const [values, setValues] = useState<Record<Key, string>>({ 'clerk-limit': '', floor: '' });
  const touched = useRef(new Set<Key>());

  const submit = useSubmitOnce();
  const save = useMutation({
    mutationFn: async (entries: [Key, string][]) => {
      for (const [key, value] of entries) {
        await putJson<Setting>(`/api/sheets/${encodeURIComponent(sheetId)}/settings/${key}`, { value, by: user });
      }
    },
    onSuccess: () => {
      setValues({ 'clerk-limit': '', floor: '' });
      touched.current.clear();
    },
    onSettled: () => {
      submit.end();
      void queryClient.invalidateQueries({ queryKey: settingsKey(sheetId) });
    },
  });
  const current = (key: Key) => settings.data?.find((s) => s.key === key)?.value ?? '';

  const field = (key: Key, label: string) => (
    <label>
      {label}{' '}
      <input
        data-testid={`setting-${key}`}
        inputMode="decimal"
        value={values[key]}
        onFocus={() => touched.current.add(key)}
        onChange={(e) => {
          touched.current.add(key);
          const value = e.target.value;
          setValues((was) => ({ ...was, [key]: value }));
          save.reset();
        }}
      />
    </label>
  );

  return (
    <section data-testid="settings">
      <h3>Settings</h3>
      <form
        data-testid="settings-form"
        onSubmit={(e) => {
          e.preventDefault();
          if (!submit.begin()) return;
          save.reset();
          const keys = KEYS.filter((k) => touched.current.has(k));
          save.mutate((keys.length > 0 ? keys : (['clerk-limit'] as Key[])).map((k) => [k, values[k]]));
        }}
      >
        {field('clerk-limit', 'Clerk limit')} {field('floor', 'Floor')}{' '}
        <button type="submit" data-testid="settings-save" disabled={save.isPending}>
          Save settings
        </button>
        {save.isError && (
          <span data-testid="settings-error">
            The clerk limit must be an amount with up to two decimals, or blank; the floor an amount with up to two
            decimals.
          </span>
        )}
      </form>
      {settings.isError && <p data-testid="settings-load-error">Could not load settings.</p>}
      <p>
        Clerk limit in force <span data-testid="setting-clerk-limit-current">{current('clerk-limit')}</span>
      </p>
      <p>
        Floor in force <span data-testid="setting-floor-current">{current('floor') || DEFAULT_FLOOR}</span>
      </p>
    </section>
  );
}

export const contribution = SheetPage.fill({ order: 6, Component: Settings });
