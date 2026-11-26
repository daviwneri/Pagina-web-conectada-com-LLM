import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { useCallback, useEffect, useRef, useState } from 'react'
import { abrirStream, enviarPergunta } from '../api.js'

function PerguntaPendente({ id, token, concluir, onSair }) {
  const [status, setStatus] = useState('Pensando…')
  useEffect(() => {
    const ctrl = new AbortController()
    abrirStream(token, id, ctrl.signal, {
      onResposta: (r) => concluir(id, r.texto, r.erro),
      onStatus: setStatus,
    }).catch((err) => {
      if (ctrl.signal.aborted) return
      if (err.message === 'SESSAO_EXPIRADA') onSair()
      else concluir(id, err.message, true)
    })
    return () => ctrl.abort()
  }, [id, token, concluir, onSair])
  return <p className="resposta aguardando">{status}</p>
}

export default function Chat({ token, username, onSair }) {
  const [itens, setItens] = useState([])        // { id, pergunta, resposta, estado }
  const [texto, setTexto] = useState('')
  const [aviso, setAviso] = useState('')
  const fim = useRef(null)

  const concluir = useCallback((id, resposta, erro) => {
    setItens((atual) => atual.map((item) => item.id === id
      ? { ...item, resposta, estado: erro ? 'erro' : 'ok' }
      : item))
  }, [])

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
              <PerguntaPendente id={it.id} token={token} concluir={concluir} onSair={onSair} />
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
