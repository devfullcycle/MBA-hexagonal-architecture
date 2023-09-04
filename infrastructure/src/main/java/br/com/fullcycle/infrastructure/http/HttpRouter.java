package br.com.fullcycle.infrastructure.http;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public interface HttpRouter {

    <T> HttpRouter POST(String pattern, HttpHandler<T> handler);

    <T> HttpRouter GET(String pattern, HttpHandler<T> handler);

    interface HttpHandler<T> {
        HttpResponse<T> handle(HttpRequest req);
    }

    interface HttpRequest {

        <T> T body(Class<T> tClass);

        String pathParam(String name);

        Optional<String> queryParam(String name);
    }

    record HttpResponse<T>(int statusCode, Map<String, String> headers, T body) {

        public static HttpResponse.Builder created(final URI uri) {
            return new Builder(201).location(uri);
        }

        public static HttpResponse.Builder notFound() {
            return new Builder(404);
        }

        public static HttpResponse.Builder unprocessableEntity() {
            return new Builder(422);
        }

        public static <T> HttpResponse<T> ok(final T body) {
            return new HttpResponse<>(200, Map.of(), body);
        }

        public static class Builder {

            private final int statusCode;
            private final Map<String, String> headers;

            public Builder(int statusCode) {
                this.statusCode = statusCode;
                this.headers = new HashMap<>(2);
            }

            public <T> HttpResponse<T> build() {
                return new HttpResponse<>(statusCode, headers, null);
            }

            public <T> HttpResponse<T> body(T body) {
                return new HttpResponse<>(statusCode, headers, body);
            }

            public Builder location(URI uri) {
                this.headers.put("Location", uri.toASCIIString());
                return this;
            }
        }
    }
}
