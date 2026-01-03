package com.h.ooohygpicture.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class MqConfig {

    // 1. 定义名称常量
    public static final String AI_DRAW_QUEUE = "ai_draw_queue"; // 正常队列
    public static final String AI_DRAW_EXCHANGE = "ai_draw_exchange"; // 正常交换机
    public static final String AI_DRAW_ROUTING_KEY = "ai_draw_routing_key";

    // 死信这一套
    public static final String DLX_EXCHANGE = "ai_dlx_exchange"; // 死信交换机
    public static final String DLX_QUEUE = "ai_dlx_queue"; // 死信队列
    public static final String DLX_ROUTING_KEY = "ai_dlx_key"; // 死信路由键

    // ==========================================
    // 💀 死信队列配置 (垃圾回收站)
    // ==========================================

    /**
     * 1. 声明死信交换机
     */
    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    /**
     * 2. 声明死信队列
     */
    @Bean
    public Queue dlxQueue() {
        return new Queue(DLX_QUEUE);
    }

    /**
     * 3. 绑定：凡是发给死信交换机的，且 key 是 ai_dlx_key 的，都扔到死信队列里
     */
    @Bean
    public Binding dlxBinding() {
        return BindingBuilder.bind(dlxQueue()).to(dlxExchange()).with(DLX_ROUTING_KEY);
    }

    // ==========================================
    // 👷 正常队列配置 (工作车间)
    // ==========================================

    /**
     * 4. 声明正常队列 (重点在这里！！！)
     * 我们要给这个队列贴上“遗嘱”：如果我挂了，把消息转给谁。
     */
    @Bean
    public Queue aiDrawQueue() {
        Map<String, Object> args = new HashMap<>();
        // 绑定死信交换机
        args.put("x-dead-letter-exchange", DLX_EXCHANGE);
        // 绑定死信路由键
        args.put("x-dead-letter-routing-key", DLX_ROUTING_KEY);

        // durable: true 表示持久化，服务器重启了队列还在
        return QueueBuilder.durable(AI_DRAW_QUEUE).withArguments(args).build();
    }

    // 正常交换机和绑定关系（可选，如果你是用默认交换机发消息，下面这两段可以不写，但为了规范建议写上）
    @Bean
    public DirectExchange aiDrawExchange() {
        return new DirectExchange(AI_DRAW_EXCHANGE);
    }

    @Bean
    public Binding aiDrawBinding() {
        return BindingBuilder.bind(aiDrawQueue()).to(aiDrawExchange()).with(AI_DRAW_ROUTING_KEY);
    }
}
