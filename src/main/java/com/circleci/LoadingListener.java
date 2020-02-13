package com.circleci;

import com.intellij.util.messages.Topic;

import java.util.EventListener;

public interface LoadingListener extends EventListener {

    Topic<LoadingListener> TOPIC = Topic.create("CircleCI List Loading", LoadingListener.class);

    void loadingStarted(boolean reload);

    void loadingFinished();
}
