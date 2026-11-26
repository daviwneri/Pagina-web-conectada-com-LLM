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

export function abrirStream(token, id, signal, { onResposta, onStatus }) {
  let concluida = false
  return fetchEventSource(`/api/perguntas/${encodeURIComponent(id)}/stream`, {
    headers: { Authorization: `Bearer ${token}` },
    signal,
    openWhenHidden: true,
    async onopen(res) {
      if (res.status === 401) throw new FatalError('SESSAO_EXPIRADA')
      if (res.status === 404) throw new FatalError('Pergunta indisponível ou expirada.')
      if (!res.ok) throw new Error(`Falha na conexão: ${res.status}`)
      onStatus('Pensando…')
    },
    onmessage(ev) {
      if (ev.event !== 'resposta' || concluida) return
      const resposta = JSON.parse(ev.data)
      if (resposta.correlationId !== id) throw new FatalError('Resposta incompatível com a pergunta.')
      concluida = true
      onResposta(resposta)
    },
    onclose() {
      if (!concluida) throw new Error('Conexão interrompida antes da resposta.')
    },
    onerror(err) {
      if (err instanceof FatalError) throw err
      onStatus('Reconectando…')
      return 2000
    },
  })
}
