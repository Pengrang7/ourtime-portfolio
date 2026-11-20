package com.placeboard.map;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

class FakeMapHttpFetcher implements MapHttpFetcher {
    private final Map<String, MapHttpResponse> responses = new HashMap<>();
    final List<String> requested = new ArrayList<>();

    FakeMapHttpFetcher redirect(String from, String to) {
        responses.put(from, new MapHttpResponse(200, URI.create(to), ""));
        return this;
    }

    FakeMapHttpFetcher body(String url, String body) {
        responses.put(url, new MapHttpResponse(200, URI.create(url), body));
        return this;
    }

    FakeMapHttpFetcher status(String url, int status) {
        responses.put(url, new MapHttpResponse(status, URI.create(url), ""));
        return this;
    }

    @Override
    public MapHttpResponse get(URI uri, Map<String, String> headers, Predicate<URI> allowedUri) {
        requested.add(uri.toString());
        MapHttpResponse response = responses.get(uri.toString());
        if (response == null) throw new AssertionError("예상하지 못한 요청: " + uri);
        return response;
    }
}
