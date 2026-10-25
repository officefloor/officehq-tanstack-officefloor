import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useUser } from './useSession';
import { useSubmitOnce } from '../../mutation/useSubmitOnce';

// Sign in by typing a name; there is no password.
export function SignIn() {
  const [, setUser] = useUser();
  const [name, setName] = useState('');
  const signIn = useMutation({
    mutationFn: (n: string) => postJson<{ name: string }>('/api/signin', { name: n }),
    onSuccess: (u) => setUser(u.name),
  });

  const submit = useSubmitOnce(signIn);
  return (
    <form
      data-testid="signin-form"
      onSubmit={(e) => {
        e.preventDefault();
        submit(name);
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
      {signIn.isError && (
        <p data-testid="signin-error" role="alert">
          {signIn.error.message}
        </p>
      )}
    </form>
  );
}
