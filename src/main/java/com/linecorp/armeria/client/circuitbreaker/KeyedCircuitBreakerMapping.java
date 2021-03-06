/*
 * Copyright 2016 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.linecorp.armeria.client.circuitbreaker;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import com.linecorp.armeria.client.ClientCodec;
import com.linecorp.armeria.client.ClientOptions;

import io.netty.channel.EventLoop;

/**
 * A {@link CircuitBreakerMapping} that binds a {@link CircuitBreaker} to its key. {@link KeySelector} is used
 * to resolve the key from remote invocation parameters. If there is no circuit breaker bound to the key,
 * A new one is created by using the given circuit breaker factory.
 */
public class KeyedCircuitBreakerMapping<K> implements CircuitBreakerMapping {

    private final ConcurrentMap<K, CircuitBreaker> mapping = new ConcurrentHashMap<>();

    private final KeySelector<K> keySelector;

    private final Function<K, CircuitBreaker> factory;

    /**
     * Creates a new {@link KeyedCircuitBreakerMapping} with the given {@link KeySelector} and
     * {@link CircuitBreaker} factory.
     *
     * @param keySelector A function that returns the key of the given remote invocation parameters.
     * @param factory A function that takes a key and creates a new {@link CircuitBreaker} for the key.
     */
    public KeyedCircuitBreakerMapping(KeySelector<K> keySelector, Function<K, CircuitBreaker> factory) {
        this.keySelector = requireNonNull(keySelector, "keySelector");
        this.factory = requireNonNull(factory, "factory");
    }

    @Override
    public CircuitBreaker get(EventLoop eventLoop, URI uri, ClientOptions options, ClientCodec codec,
                              Method method, Object[] args) throws Exception {
        final K key = keySelector.get(eventLoop, uri, options, codec, method, args);
        final CircuitBreaker circuitBreaker = mapping.get(key);
        if (circuitBreaker != null) {
            return circuitBreaker;
        }
        return mapping.computeIfAbsent(key, mapKey -> factory.apply(key));
    }

    /**
     * Returns the mapping key of the given remote invocation parameters.
     */
    @FunctionalInterface
    public interface KeySelector<K> {

        /**
         * A {@link KeySelector} that returns remote method name as a key.
         */
        KeySelector<String> METHOD =
                (eventLoop, uri, options, codec, method, args) -> method.getName();

        /**
         * A {@link KeySelector} that returns a key consisted of remote host name and port number.
         */
        KeySelector<String> HOST =
                (eventLoop, uri, options, codec, method, args) ->
                        uri.getPort() < 0 ? uri.getHost() : uri.getHost() + ':' + uri.getPort();

        /**
         * A {@link KeySelector} that returns a key consisted of remote host name, port number, and method name.
         */
        KeySelector<String> HOST_AND_METHOD =
                (eventLoop, uri, options, codec, method, args) ->
                        HOST.get(eventLoop, uri, options, codec, method, args) + '#' + method.getName();

        K get(EventLoop eventLoop, URI uri, ClientOptions options, ClientCodec codec, Method method,
              Object[] args) throws Exception;

    }

}
