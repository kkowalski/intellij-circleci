package com.circleci.api;

import com.intellij.util.io.RequestBuilder;

import java.io.IOException;

public class RequestExecutor {

    public void execute(RequestBuilder requestBuilder) {
        try {
            String textResponse = requestBuilder
                    .readString();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
