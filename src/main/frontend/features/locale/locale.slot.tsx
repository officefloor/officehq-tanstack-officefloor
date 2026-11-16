import { useEffect, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPage } from '../../slots/defs/sheetPage';
import { LOCALES, localeKey, showFiguresIn, useUserLocale, type Locale, type UserLocale } from './locale';

/** The signed-in user's locale: the select and button that save it (no audit record), the locale as stored, and the
 *  screen's figures shown in it (under de as 1.234,56). Entries are still typed with a dot. */
function UserLocaleControl({ user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const current = useUserLocale(user);
  const [choice, setChoice] = useState<Locale | ''>('');
  const locale: Locale = current.data?.locale ?? 'en';

  const submit = useSubmitOnce();
  const save = useMutation({
    mutationFn: (l: Locale) => putJson<UserLocale>(`/api/users/${encodeURIComponent(user)}/locale`, { locale: l }),
    onSuccess: (saved) => {
      queryClient.setQueryData(localeKey(user), saved);
      setChoice('');
    },
    onSettled: submit.end,
  });

  useEffect(() => {
    const root = document.getElementById('root');
    return root ? showFiguresIn(root, locale) : undefined;
  }, [locale]);

  return (
    <section data-testid="user-locale-panel">
      Locale{' '}
      <select
        data-testid="user-locale"
        value={choice === '' ? locale : choice}
        onChange={(e) => {
          setChoice(e.target.value as Locale);
          save.reset();
        }}
      >
        {LOCALES.map((l) => (
          <option key={l} value={l}>
            {l}
          </option>
        ))}
      </select>{' '}
      <button
        type="button"
        data-testid="user-locale-save"
        disabled={save.isPending}
        onClick={() => {
          if (!submit.begin()) return;
          save.reset();
          save.mutate(choice === '' ? locale : choice);
        }}
      >
        Save locale
      </button>{' '}
      Current <span data-testid="user-locale-current">{current.data?.locale ?? ''}</span>
      {save.isError && <span data-testid="user-locale-error"> Could not save the locale.</span>}
    </section>
  );
}

export const contribution = SheetPage.fill({ order: -20, Component: UserLocaleControl });
