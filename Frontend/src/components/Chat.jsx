import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { useEffect, useRef, useState } from 'react'
import { abrirStream, enviarPergunta } from '../api.js'

export default function Chat({ token, username, onSair }) {
  const [itens, setItens] = useState([])        // { id, pergunta, resposta, estado }
  const [texto, setTexto] = useState('')
  const [status, setStatus] = useState('conectando')
  const [aviso, setAviso] = useState('')
  const fim = useRef(null)

  // Mantém UM stream SSE aberto, vinculado ao usuário do token.
  useEffect(() => {
    const fechar = abrirStream(token, {
      onStatus: setStatus,
      onSessaoExpirada: onSair,
      onResposta: (r) =>
        setItens((atual) =>
          atual.map((it) =>
            it.id === r.correlationId
              ? { ...it, resposta: r.texto, estado: r.erro ? 'erro' : 'ok' }
              : it
          )
        ),
    })
    return fechar
  }, [token]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    fim.current?.scrollIntoView({ behavior: 'smooth' })
  }, [itens])

  async function enviar(e) {
    e.preventDefault()
    const pergunta = texto.trim()
    if (!pergunta) return
    setAviso('')
    try {
      const id = await enviarPergunta(token, pergunta)
      setItens((atual) => [...atual, { id, pergunta, resposta: null, estado: 'aguardando' }])
      setTexto('')
    } catch (err) {
      if (err.message === 'SESSAO_EXPIRADA') return onSair()
      setAviso(err.message)
    }
  }

  return (
    <div className="chat">
      <header>
        <div>
          <strong>Tutor CC</strong>
          <span className={`status ${status}`}>{status === 'conectado' ? 'Conectado' : 'Reconectando…'}</span>
        </div>
        <div>
          <span>{username}</span>
          <button className="link" onClick={onSair}>Sair</button>
        </div>
      </header>

      <section className="mensagens" aria-live="polite">
        {itens.length === 0 && (
          <p className="vazio">Faça sua primeira pergunta, por exemplo: “O que é escopo?”</p>
        )}
        {itens.map((it) => (
          <article key={it.id}>
            <p className="pergunta">{it.pergunta}</p>
            {it.estado === 'aguardando' ? (
              <p className="resposta aguardando">Pensando…</p>
            ) : (
              <div className={`resposta markdown ${it.estado}`}>
                <ReactMarkdown remarkPlugins={[remarkGfm]} skipHtml>
                  {it.resposta}
                </ReactMarkdown>
              </div>
            )}
          </article>
        ))}
        <div ref={fim} />
      </section>

      <form className="entrada" onSubmit={enviar}>
        <input value={texto} onChange={(e) => setTexto(e.target.value)}
               placeholder="Digite sua pergunta" maxLength={2000} />
        <button type="submit" disabled={!texto.trim()}>Enviar</button>
      </form>
      {aviso && <p className="erro" role="alert">{aviso}</p>}
    </div>
  )
}
