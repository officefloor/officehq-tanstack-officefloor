import { useEffect, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJsonLive } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { AppSession } from '../../slots/defs/appSession';
import { useCurrentUser } from '../session/session';
import { LOCALES, localePath, localeQueryKey, showFiguresIn, useUserLocale, type Locale, type UserLocale } from './locale';

// The signed-in user's locale: chosen and saved here (no audit line), the saved one shown. While it is de, every
// figure on the screen is shown as 1.234,56.
function UserLocalePicker() {
  const [user] = useCurrentUser();
  const saved = useUserLocale();
  const [choice, setChoice] = useState<Locale | ''>('');
  const queryClient = useQueryClient();
  const save = useMutation({
    mutationFn: (locale: Locale) => putJsonLive<UserLocale>(localePath(user ?? ''), { locale }, user),
    onSuccess: (data) => queryClient.setQueryData(localeQueryKey(data.user), data),
  });
  const submit = useSubmitOnce(save);
  const current = saved.data?.locale ?? 'en';

  useEffect(() => {
    if (!user) return undefined;
    const root = document.getElementById('root');
    return root ? showFiguresIn(root, current) : undefined;
  }, [user, current]);

  if (!user) return null;
  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        submit(choice || current);
      }}
    >
      <select
        data-testid="user-locale"
        value={choice || current}
        onChange={(e) => { setChoice(e.target.value as Locale); save.reset(); }}
      >
        {LOCALES.map((l) => <option key={l} value={l}>{l}</option>)}
      </select>
      <button type="submit" data-testid="user-locale-save" disabled={save.isPending}>
        Save locale
      </button>
      <span>
        Locale <strong data-testid="user-locale-current">{saved.data ? saved.data.locale : ''}</strong>
      </span>
      {save.isError && <span data-testid="user-locale-error">Could not save the locale.</span>}
    </form>
  );
}

export const contribution = AppSession.fill({ order: 5, Component: UserLocalePicker });
