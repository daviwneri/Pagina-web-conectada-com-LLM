package com.devweb.gateway.sse;

import com.devweb.gateway.chat.RespostaMessage;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SsePorTarefaTest {
    private final SseRegistry registry = new SseRegistry();
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new SseController(registry)).build();

    private MvcResult abrir(String id) throws Exception {
        return mvc.perform(get("/api/perguntas/{id}/stream", id)
                        .principal(new TestingAuthenticationToken("ana", null)))
                .andExpect(request().asyncStarted()).andReturn();
    }

    private void verificarFinal(MvcResult stream, String id, String texto) throws Exception {
        mvc.perform(asyncDispatch(stream)).andExpect(status().isOk())
                .andExpect(content().string(containsString("event:resposta")))
                .andExpect(content().string(containsString("id:" + id)))
                .andExpect(content().string(containsString(texto)));
    }

    @Test
    void resultadoAnteriorAoStreamPodeSerRecuperadoInclusiveNaReconexao() throws Exception {
        registry.criar("a", "ana");
        registry.entregar(new RespostaMessage("a", "ana", "primeira resposta", false));
        registry.entregar(new RespostaMessage("a", "ana", "duplicada", false));
        verificarFinal(abrir("a"), "a", "primeira resposta");
        verificarFinal(abrir("a"), "a", "primeira resposta");
    }

    @Test
    void duasPerguntasSimultaneasEncerramIndividualmenteInclusiveNoErro() throws Exception {
        registry.criar("a", "ana");
        registry.criar("b", "ana");
        MvcResult a = abrir("a");
        MvcResult b = abrir("b");
        registry.entregar(new RespostaMessage("a", "outra", "indevida", false));
        registry.entregar(new RespostaMessage("b", "ana", "falha da IA", true));
        verificarFinal(b, "b", "falha da IA");
        assertFalse(a.getResponse().getContentAsString().contains("event:resposta"));
        registry.entregar(new RespostaMessage("a", "ana", "exemplo", false));
        verificarFinal(a, "a", "exemplo");
    }

    @Test
    void tarefaAlheiaEIdDesconhecidoRetornam404() throws Exception {
        registry.criar("a", "ana");
        for (String id : new String[]{"a", "ausente"}) {
            mvc.perform(get("/api/perguntas/{id}/stream", id)
                            .principal(new TestingAuthenticationToken("bia", null)))
                    .andExpect(status().isNotFound());
        }
    }
}
