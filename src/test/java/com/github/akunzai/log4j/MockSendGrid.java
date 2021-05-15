package com.github.akunzai.log4j;

import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;

import java.util.*;

public class MockSendGrid extends SendGrid {

    private final List<Request> requests = new ArrayList<>();

    public MockSendGrid(String apiKey) {
        super(apiKey);
    }

    @Override
    public Response makeCall(Request request) {
        requests.add(request);
        Response response = new Response();
        response.setStatusCode(200);
        response.setBody("{\"message\":\"success\"}");
        Map<String, String> headers = new HashMap<>();
        // https://sendgrid.com/docs/glossary/x-message-id/
        headers.put("X-Message-ID", UUID.randomUUID().toString());
        response.setHeaders(headers);
        return response;
    }

    public List<Request> getRequests() {
        return this.requests;
    }
}
