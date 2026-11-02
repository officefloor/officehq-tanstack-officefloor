import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSignedInUser } from './useSession';

/** Sign in by typing a name; there is no password. */
export function SignIn() {
  const [, setUser] = useSignedInUser();
  const [name, setName] = useState('');
  const signIn = useMutation({
    mutationFn: (n: string) => postJson<{ name: string }>('/api/session', { name: n }),
    onSuccess: (s) => setUser(s.name),
  });

  return (
    <form
      data-testid="signin-form"
      onSubmit={(e) => {
        e.preventDefault();
        signIn.reset();
        signIn.mutate(name);
      }}
    >
      <label>
        Name{' '}
        <input
          data-testid="signin-name"
          value={name}
          onChange={(e) => {
            setName(e.target.value);
            signIn.reset();
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="signin-submit" disabled={signIn.isPending}>
        Sign in
      </button>
      {signIn.isError && <p data-testid="signin-error">{signIn.error.message}</p>}
    </form>
  );
}

/** The signed-in name, with a way to sign out. */
export function CurrentUser() {
  const [user, setUser] = useSignedInUser();
  return (
    <p>
      Signed in as <strong data-testid="current-user">{user}</strong>{' '}
      <button type="button" data-testid="signout" onClick={() => setUser(undefined)}>
        Sign out
      </button>
    </p>
  );
}
