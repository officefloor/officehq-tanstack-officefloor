import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../mutation/useSubmitOnce';
import { LOCALES, localeKey, useLocale, type Locale, type UserLocale } from '../locale/useLocale';
import { useUser } from './useSession';

// Who is signed in, a way to sign out, and the user's locale (en unless chosen; saved per user, no audit line).
export function UserBar() {
  const [user, setUser] = useUser();
  const name = user ?? '';
  const locale = useLocale(name);
  const current = locale.data?.locale ?? 'en';
  const [chosen, setChosen] = useState<Locale | null>(null);
  const queryClient = useQueryClient();
  const save = useMutation({
    mutationFn: (to: Locale) => putJson<UserLocale>(`/api/users/${encodeURIComponent(name)}/locale`, { locale: to }),
    onSuccess: (saved) => {
      queryClient.setQueryData(localeKey(name), saved);
      setChosen(null);
    },
  });
  const submit = useSubmitOnce(save);
  return (
    <div data-testid="user-bar">
      Signed in as <strong data-testid="current-user">{user}</strong>{' '}
      <button type="button" data-testid="signout" onClick={() => setUser(undefined)}>
        Sign out
      </button>{' '}
      <form
        style={{ display: 'inline' }}
        onSubmit={(e) => {
          e.preventDefault();
          submit(chosen ?? current);
        }}
      >
        Locale{' '}
        <select
          data-testid="user-locale"
          value={chosen ?? current}
          onChange={(e) => {
            setChosen(e.target.value as Locale);
            save.reset();
          }}
        >
          {LOCALES.map((l) => (
            <option key={l} value={l}>
              {l}
            </option>
          ))}
        </select>{' '}
        <button type="submit" data-testid="user-locale-save" disabled={save.isPending}>
          Save
        </button>{' '}
        <span data-testid="user-locale-current">{locale.data?.locale ?? ''}</span>
        {save.error && <span data-testid="user-locale-error"> {save.error.message}</span>}
      </form>
    </div>
  );
}
