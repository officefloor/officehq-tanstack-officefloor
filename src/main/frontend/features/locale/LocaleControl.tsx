import { useLayoutEffect, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { showFiguresIn } from './figures';
import { LOCALES, localeKey, useLocale, type Locale } from './useLocale';

// The signed-in user's locale, chosen and saved per user (no audit line). It decides only how figures are shown
// to that user (see figures.ts); the whole screen is shown in it.
export function LocaleControl() {
  const [user] = useSearchParam('user', asString);
  const locale = useLocale(user);
  // The choice in the select; until one is made, the saved locale.
  const [choice, setChoice] = useState<Locale | ''>('');
  const queryClient = useQueryClient();
  const submitOnce = useSubmitOnce();
  const save = useMutation({
    mutationFn: (value: Locale) =>
      postJson<{ locale: Locale }>(`/api/users/${encodeURIComponent(user)}/locale`, { locale: value }),
    onSuccess: (saved) => {
      queryClient.setQueryData(localeKey(user), saved);
      setChoice('');
    },
  });
  useLayoutEffect(() => showFiguresIn(document.getElementById('root') ?? document.body, locale), [locale]);
  return (
    <form
      data-testid="user-locale-form"
      onSubmit={(e) => {
        e.preventDefault();
        submitOnce(() => save.mutateAsync(choice === '' ? locale : choice));
      }}
    >
      Locale:{' '}
      <select
        data-testid="user-locale"
        value={choice === '' ? locale : choice}
        onChange={(e) => {
          setChoice(e.target.value as Locale);
          save.reset();
        }}
      >
        {LOCALES.map((l) => (
          <option key={l} value={l}>{l}</option>
        ))}
      </select>{' '}
      <button type="submit" data-testid="user-locale-save">Save locale</button>{' '}
      <span data-testid="user-locale-current">{locale}</span>
      {save.isError && <span data-testid="user-locale-error" role="alert"> Could not save the locale.</span>}
    </form>
  );
}
