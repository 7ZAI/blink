package com.blink.gateway.crypt;

import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.nio.channels.Channels;
/**
 * @Author binblink
 * @Date 2025/10/2
 */
@Deprecated
public class CryptoResponseWrapper extends ServerHttpResponseDecorator {

    private final ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();

    public CryptoResponseWrapper(ServerHttpResponse delegate) {
        super(delegate);
    }

    @Override
    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
        Flux<DataBuffer> bufferFlux = Flux.from(body);
        return super.writeWith(bufferFlux.doOnNext(dataBuffer -> {
            try {
                Channels.newChannel(bodyStream).write(dataBuffer.asByteBuffer().asReadOnlyBuffer());
            } catch (Exception e) {
                // 处理异常
                e.printStackTrace();
            }
        }));
    }

    public byte[] getBody() {
        return bodyStream.toByteArray();
    }
}
