import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSingleFlight } from '../../query/useSingleFlight';
import { SheetBody } from '../../slots/defs/sheetBody';
import { LOCALES, localeKey, useLocale, type UserLocale } from './useLocale';

/** The signed-in user's locale, shown and changeable: how the screen shows them figures (useFigure). It is kept
 *  for the user, whatever sheet is loaded, and writes no audit line. */
function LocaleControl({ user }: { user: string }) {
  const locale = useLocale();
  const [chosen, setChosen] = useState<string | null>(null);
  const queryClient = useQueryClient();
  const submitOnce = useSingleFlight();
  const save = useMutation({
    mutationFn: (raw: string) => postJson<UserLocale>('/api/locale', { locale: raw }),
    onSuccess: (saved) => {
      queryClient.setQueryData(localeKey(user), saved);
      setChosen(null);
    },
  });

  return (
    <form
      data-testid="user-locale-form"
      onSubmit={(e) => {
        e.preventDefault();
        submitOnce(() => {
          save.reset();
          return save.mutateAsync(chosen ?? locale);
        });
      }}
    >
      Locale: <strong data-testid="user-locale-current">{locale}</strong>{' '}
      <select
        data-testid="user-locale"
        value={chosen ?? locale}
        onChange={(e) => {
          setChosen(e.target.value);
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
        Save locale
      </button>
      {save.isError && <p data-testid="user-locale-error">{save.error.message}</p>}
    </form>
  );
}

export const contribution = SheetBody.fill({ order: -20, Component: ({ user }) => <LocaleControl user={user} /> });
