import test from 'node:test'
import assert from 'node:assert/strict'
import { abrirStream } from '../src/api.js'

test('reconecta após EOF prematuro e termina após a resposta final', async (t) => {
  let chamadas = 0
  t.mock.method(globalThis, 'fetch', async (url, req) => {
    assert.equal(url, '/api/perguntas/a/stream')
    assert.equal(req.headers.Authorization, 'Bearer jwt')
    chamadas++
    const evento = 'event:resposta\ndata:{"correlationId":"a","texto":"ok","erro":false}\n\n'
    return new Response(chamadas === 1 ? '' : evento)
  })
  globalThis.window = { fetch, setTimeout: (fn) => setTimeout(fn, 0), clearTimeout }
  globalThis.document = { removeEventListener() {} }
  try {
    const respostas = []
    const estados = []
    await abrirStream('jwt', 'a', new AbortController().signal, {
      onResposta: (r) => respostas.push(r),
      onStatus: (s) => estados.push(s),
    })
    assert.equal(chamadas, 2)
    assert.deepEqual(respostas, [{ correlationId: 'a', texto: 'ok', erro: false }])
    assert.ok(estados.includes('Reconectando…'))
  } finally {
    delete globalThis.window
    delete globalThis.document
  }
})
