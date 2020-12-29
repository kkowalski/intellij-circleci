package com.circleci;

import com.circleci.api.JSON;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

import static java.net.HttpURLConnection.HTTP_OK;

public class TestAPIServer {

    private static final String LOCALHOST = "127.0.0.1";
    private HttpServer myServer;

    private Object object;

    public String setup() throws IOException {
        myServer = HttpServer.create();
        myServer.bind(new InetSocketAddress(LOCALHOST, 0), 1);
        myServer.start();
        String url = "http://" + LOCALHOST + ":" + myServer.getAddress().getPort();

        addBuildsPath();

        return url;
    }

    public void setResponse(Object object) {
        this.object = object;
    }

    private void addBuildsPath() {
        myServer.createContext("/api/v1.1/project", ex -> {
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(HTTP_OK, 0);
            ex.getResponseBody().write(JSON.toJson(object).getBytes("koi8-r"));
            ex.close();
        });
    }

}
