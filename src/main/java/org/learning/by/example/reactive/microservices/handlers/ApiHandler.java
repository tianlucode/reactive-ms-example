package org.learning.by.example.reactive.microservices.handlers;

import org.learning.by.example.reactive.microservices.model.HelloRequest;
import org.learning.by.example.reactive.microservices.model.HelloResponse;
import org.learning.by.example.reactive.microservices.services.HelloService;
import org.learning.by.example.reactive.microservices.services.QuoteService;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

public class ApiHandler {

    private static final String NAME = "name";

    private final ErrorHandler errorHandler;
    private final HelloService helloService;
    private final QuoteService quoteService;

    private static final Mono<String> DEFAULT_NAME = Mono.just("world");

    public ApiHandler(final HelloService helloService, final QuoteService quoteService, final ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
        this.helloService = helloService;
        this.quoteService = quoteService;
    }

    public Mono<ServerResponse> defaultHello(final ServerRequest request) {
        return DEFAULT_NAME
                .publish(this::getServerResponse)
                .onErrorResume(errorHandler::throwableError);
    }

    public Mono<ServerResponse> getHello(final ServerRequest request) {
        return Mono.just(request.pathVariable(NAME))
                .publish(this::getServerResponse)
                .onErrorResume(errorHandler::throwableError);
    }

    public Mono<ServerResponse> postHello(final ServerRequest request) {
        return request.bodyToMono(HelloRequest.class)
                .flatMap(helloRequest -> Mono.just(helloRequest.getName()))
                .publish(this::getServerResponse)
                .onErrorResume(errorHandler::throwableError);
    }

    Mono<ServerResponse> getServerResponse(Mono<String> monoName) {
        return monoName.publish(this::createHelloResponse)
                .publish(this::convertToServerResponse);
    }

    Mono<HelloResponse> createHelloResponse(Mono<String> monoName) {
        return monoName.publish(helloService::greetings).flatMap(
                greetings -> randomQuote().flatMap(
                        content -> Mono.just(new HelloResponse(greetings, content))));
    }

    Mono<String> randomQuote() {
        return Mono.fromSupplier(quoteService::get)
                .flatMap(quoteMono -> quoteMono.flatMap(quote -> Mono.just(quote.getContent())));
    }

    Mono<ServerResponse> convertToServerResponse(Mono<HelloResponse> helloResponseMono) {
        return helloResponseMono.flatMap(helloResponse ->
                ServerResponse.ok().body(Mono.just(helloResponse), HelloResponse.class));
    }
}