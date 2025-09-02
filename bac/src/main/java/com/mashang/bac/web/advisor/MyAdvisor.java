package com.mashang.bac.web.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.model.MessageAggregator;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class MyAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {
    /**
     * 拦截器名称
     *
     * @return
     */
    @Override
    public String getName() {
        return "my-advisor";
    }

    /**
     * 设置拦截器层级
     *
     * @return
     */
    @Override
    public int getOrder() {
        return 100;
    }

    /**
     * 非流式处理拦截器
     *
     * @param advisedRequest-拦截请求对象
     * @param chain
     * @return
     */
    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        //将请求拦截的参数传递
        AdvisedRequest modifiedRequest = before(advisedRequest);
        AdvisedResponse advisedResponse = chain.nextAroundCall(modifiedRequest);
        //将响应拦截的参数传递
        observeAfter(advisedResponse);
        //todo 插入mysql(用户输入)
        return advisedResponse;
    }

    /**
     * 拦截器后置操作
     *
     * @param advisedResponse
     * @return
     */
    private void observeAfter(AdvisedResponse advisedResponse) {
        //截获ai输出完整内容
        log.info("AI Response: {}", advisedResponse.response().getResult().getOutput().getText());
        //todo 插入mysql(ai返回)
    }

    /**
     * 拦截器前置操作
     *
     * @param advisedRequest
     * @return
     */
    private AdvisedRequest before(AdvisedRequest advisedRequest) {
        //构造重读-俗称Re2
        Map<String, Object> advisedUserParams = new HashMap<>(advisedRequest.userParams());
        advisedUserParams.put("re2_input_query", advisedRequest.userText());
        //截获请求参数
        log.info("AI Request: {}", advisedRequest.userText());
        return AdvisedRequest.from(advisedRequest).userText("""
                {re2_input_query}
                Read the question again: {re2_input_query}
                """).userParams(advisedUserParams).build();

    }

    /**
     * 流式处理拦截器
     *
     * @param advisedRequest-拦截请求对象
     * @param chain
     * @return
     */
    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        //将请求拦截的参数传递
        advisedRequest = before(advisedRequest);
        Flux<AdvisedResponse> advisedResponseFlux = chain.nextAroundStream(advisedRequest);
        return (new MessageAggregator()).aggregateAdvisedResponse(advisedResponseFlux, this::observeAfter);
    }
}
