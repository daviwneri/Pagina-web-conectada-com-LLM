import { useState } from 'react'
import { autenticar } from '../api.js'

export default function Login({ onEntrar }) {
  const [modo, setModo] = useState('login')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(false)

  async function enviar(e) {
    e.preventDefault()
    setErro('')
    setCarregando(true)
    try {
      onEntrar(await autenticar(modo, username, password))
    } catch (err) {
      setErro(err.message)
    } finally {
      setCarregando(false)
    }
  }

  return (
    <main className="login">
      <h1>Dev. Web</h1>
      <p className="sub">Pergunte qualquer conceito e receba uma explicação didática.</p>

      <form onSubmit={enviar}>
        <label>
          Usuário
          <input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" required />
        </label>
        <label>
          Senha
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)}
                 autoComplete={modo === 'login' ? 'current-password' : 'new-password'} required />
        </label>
        {erro && <p className="erro" role="alert">{erro}</p>}
        <button type="submit" disabled={carregando}>
          {modo === 'login' ? 'Entrar' : 'Criar conta'}
        </button>
      </form>

      <button className="link" onClick={() => { setModo(modo === 'login' ? 'register' : 'login'); setErro('') }}>
        {modo === 'login' ? 'Ainda não tenho conta' : 'Já tenho conta'}
      </button>
    </main>
  )
}
