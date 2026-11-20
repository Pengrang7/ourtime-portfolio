package com.placeboard.map;

import java.net.URI;
import java.util.Map;
import java.util.function.Predicate;

public interface MapHttpFetcher {
    String MOBILE_USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 "
            + "(KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1";
    String DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36";

    /**
     * 리다이렉트를 따라가며 GET 요청을 보낸다. 모든 경유 주소는 allowedUri를 통과해야 한다.
     */
    MapHttpResponse get(URI uri, Map<String, String> headers, Predicate<URI> allowedUri);

    record MapHttpResponse(int status, URI finalUri, String body) {
        public boolean ok() {
            return status >= 200 && status < 300;
        }
    }
}
