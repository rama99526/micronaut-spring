/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.spring.web;

import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpHeaders;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.OncePerRequestHttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import org.reactivestreams.Publisher;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

/**
 * A filter that adds support for {@link ResponseEntity} as a return type.
 *
 * @author graemerocher
 * @since 1.0
 */
@Filter("/**")
public class ResponseEntityServerFilter extends OncePerRequestHttpServerFilter {
    @Override
    protected Publisher<MutableHttpResponse<?>> doFilterOnce(HttpRequest<?> request, ServerFilterChain chain) {
        final Publisher<MutableHttpResponse<?>> responsePublisher = chain.proceed(request);
        return Publishers.map(responsePublisher, mutableHttpResponse -> {
            final Object body = mutableHttpResponse.body();
            if (body instanceof HttpEntity) {
                HttpEntity entity = (HttpEntity) body;
                if (entity instanceof ResponseEntity) {
                    mutableHttpResponse.status(((ResponseEntity) entity).getStatusCodeValue());
                }
                final HttpHeaders headers = entity.getHeaders();
                final MutableHttpHeaders micronautHeaders = mutableHttpResponse.getHeaders();
                for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                    final String key = entry.getKey();
                    final List<String> value = entry.getValue();
                    for (String v : value) {
                        micronautHeaders.add(key, v);
                    }
                }
                final Object b = entity.getBody();
                if (b != null) {
                    ((MutableHttpResponse<Object>) mutableHttpResponse).body(b);
                }
            } else if (body instanceof HttpHeaders) {
                HttpHeaders httpHeaders = (HttpHeaders) body;
                mutableHttpResponse.body(null);
                httpHeaders.forEach((s, strings) -> {
                    final MutableHttpHeaders headers = mutableHttpResponse.getHeaders();
                    for (String string : strings) {
                        headers.add(s, string);
                    }
                });
            }
            return mutableHttpResponse;
        });
    }
}
