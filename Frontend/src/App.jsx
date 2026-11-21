import { useState } from 'react'
import Login from './components/Login.jsx'
import Chat from './components/Chat.jsx'

export default function App() {
  const [sessao, setSessao] = useState(() => {
    const salva = localStorage.getItem('sessao')
    return salva ? JSON.parse(salva) : null
  })

  function entrar(dados) {
    localStorage.setItem('sessao', JSON.stringify(dados))
    setSessao(dados)
  }

  function sair() {
    localStorage.removeItem('sessao')
    setSessao(null)
  }

  return sessao ? (
    <Chat token={sessao.token} username={sessao.username} onSair={sair} />
  ) : (
    <Login onEntrar={entrar} />
  )
}
