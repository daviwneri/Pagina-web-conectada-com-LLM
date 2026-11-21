package com.devweb.worker.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String PERGUNTAS_EXCHANGE = "perguntas.exchange";
    public static final String PERGUNTAS_QUEUE = "perguntas.queue";
    public static final String PERGUNTAS_KEY = "pergunta";

    public static final String RESPOSTAS_EXCHANGE = "respostas.exchange";
    public static final String RESPOSTAS_QUEUE = "respostas.queue";
    public static final String RESPOSTAS_KEY = "resposta";

    // Exchanges e filas DURÁVEIS (sobrevivem a reinício do broker)
    @Bean
    DirectExchange perguntasExchange() {
        return new DirectExchange(PERGUNTAS_EXCHANGE, true, false);
    }

    @Bean
    Queue perguntasQueue() {
        return QueueBuilder.durable(PERGUNTAS_QUEUE).build();
    }

    @Bean
    Binding perguntasBinding() {
        return BindingBuilder.bind(perguntasQueue()).to(perguntasExchange()).with(PERGUNTAS_KEY);
    }

    @Bean
    DirectExchange respostasExchange() {
        return new DirectExchange(RESPOSTAS_EXCHANGE, true, false);
    }

    @Bean
    Queue respostasQueue() {
        return QueueBuilder.durable(RESPOSTAS_QUEUE).build();
    }

    @Bean
    Binding respostasBinding() {
        return BindingBuilder.bind(respostasQueue()).to(respostasExchange()).with(RESPOSTAS_KEY);
    }

    // Serialização JSON. O tipo é inferido pelo parâmetro do @RabbitListener,
    // então Gateway e Worker podem ter pacotes diferentes para os DTOs.
    @Bean
    MessageConverter jacksonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper mapper = new DefaultJackson2JavaTypeMapper();
        mapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        mapper.setTrustedPackages("*");
        converter.setJavaTypeMapper(mapper);
        return converter;
    }
}
