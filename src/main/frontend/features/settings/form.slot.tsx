import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { type Settings, settingsKey, useSettings } from './settings';

type Key = 'clerk-limit' | 'floor';

// The sheet's settings in force, and the form that changes them: the clerk limit (blank for none) and
// the floor (blank for its default). Saving changes the setting last touched, so a field left blank
// does not clear the other.
function SheetSettings() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const settings = useSettings(sheet);
  const [values, setValues] = useState<Record<Key, string>>({ 'clerk-limit': '', floor: '' });
  const [key, setKey] = useState<Key>('clerk-limit');
  const queryClient = useQueryClient();

  const save = useMutation({
    mutationFn: ({ key, value }: { key: Key; value: string }) =>
      postJson<Settings>(`/api/sheets/${encodeURIComponent(sheet)}/settings`, { key, value, user }),
    onSuccess: async (_, { key }) => {
      await queryClient.invalidateQueries({ queryKey: settingsKey(sheet) });
      setValues((v) => ({ ...v, [key]: '' }));
    },
  });
  const submit = useSubmitOnce(save);

  const field = (k: Key, placeholder: string) => (
    <input
      data-testid={`setting-${k}`}
      placeholder={placeholder}
      inputMode="decimal"
      value={values[k]}
      onFocus={() => setKey(k)}
      onChange={(e) => {
        setValues({ ...values, [k]: e.target.value });
        setKey(k);
        save.reset();
      }}
    />
  );

  return (
    <section data-testid="settings">
      <h2>Settings</h2>
      <form
        data-testid="settings-form"
        onSubmit={(e) => {
          e.preventDefault();
          submit({ key, value: values[key] });
        }}
      >
        Clerk limit <strong data-testid="setting-clerk-limit-current">{settings.data?.clerkLimit ?? ''}</strong>{' '}
        {field('clerk-limit', 'Clerk limit (blank for none)')}{' '}
        Floor <strong data-testid="setting-floor-current">{settings.data?.floor ?? ''}</strong>{' '}
        {field('floor', 'Floor (blank for 1.00)')}
        <button type="submit" data-testid="settings-save" disabled={save.isPending}>
          Save settings
        </button>
        {save.isError && (
          <span data-testid="settings-error" role="alert">
            {' '}
            {save.error instanceof HttpError && save.error.reason ? `${save.error.reason}.` : 'The settings were refused.'}
          </span>
        )}
      </form>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 48, Component: SheetSettings });
