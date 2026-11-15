import { useEffect, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { LOCALES, localeKey, saveLocale, useLocale } from './locale';
import { useUser } from './session';

/** The signed-in user's locale: a select and a save button; saved for them on every sheet. */
export function UserLocaleSelect() {
  const [user] = useUser();
  const current = useLocale();
  const qc = useQueryClient();
  const [chosen, setChosen] = useState<string>(current);
  useEffect(() => setChosen(current), [current]);
  const save = useMutation({
    mutationFn: (locale: string) => saveLocale(user!, locale),
    onSuccess: (saved) => qc.setQueryData(localeKey(user!), saved),
  });

  return (
    <p>
      Locale{' '}
      <select
        data-testid="user-locale"
        value={chosen}
        onChange={(e) => {
          setChosen(e.target.value);
          save.reset();
        }}
      >
        {LOCALES.map((l) => (
          <option key={l} value={l}>{l}</option>
        ))}
      </select>{' '}
      <button type="button" data-testid="user-locale-save" disabled={save.isPending} onClick={() => save.mutate(chosen)}>
        Save
      </button>{' '}
      Now <span data-testid="user-locale-current">{current}</span>
      {save.isError && <span data-testid="user-locale-error"> Choose en or de.</span>}
    </p>
  );
}
