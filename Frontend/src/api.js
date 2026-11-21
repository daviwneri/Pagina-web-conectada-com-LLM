import { fetchEventSource } from '@microsoft/fetch-event-source'

class FatalError extends Error {}

export async function autenticar(modo, username, password) {
  const res = await fetch(`/api/auth/${modo}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  })
  const dados = await res.json().catch(() => ({}))
  if (!res.ok) {
    if (dados.erro) throw new Error(dados.erro)
    if (res.status === 400) {
      throw new Error('Verifique usuário (mín. 3) e senha (mín. 6 caracteres).')
    }
    throw new Error('Servidor indisponível. Verifique se o Gateway está rodando.')
  }
  return dados // { token, username }
}

export async function enviarPergunta(token, texto) {
  const res = await fetch('/api/perguntas', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    body: JSON.stringify({ texto }),
  })
  if (res.status === 401) throw new FatalError('SESSAO_EXPIRADA')
  const dados = await res.json().catch(() => ({}))
  if (!res.ok) throw new Error(dados.erro || 'Não foi possível enviar a pergunta.')
  return dados.correlationId
}

/** Abre o stream SSE autenticado (JWT no header). Retorna uma função para fechá-lo. */
export function abrirStream(token, { onResposta, onStatus, onSessaoExpirada }) {
  const ctrl = new AbortController()

  fetchEventSource('/api/stream', {
    headers: { Authorization: `Bearer ${token}` },
    signal: ctrl.signal,
    openWhenHidden: true, // mantém o stream mesmo com a aba em segundo plano
    async onopen(res) {
      if (res.status === 401) throw new FatalError('SESSAO_EXPIRADA')
      if (!res.ok) throw new Error(`Stream falhou: ${res.status}`)
      onStatus('conectado')
    },
    onmessage(ev) {
      if (ev.event === 'resposta') onResposta(JSON.parse(ev.data))
    },
    onclose() {
      onStatus('reconectando')
      throw new Error('stream fechado') // força reconexão automática
    },
    onerror(err) {
      if (err instanceof FatalError) {
        onSessaoExpirada()
        throw err // para de tentar
      }
      onStatus('reconectando')
      return 2000 // tenta de novo em 2s
    },
  })

  return () => ctrl.abort()
}
