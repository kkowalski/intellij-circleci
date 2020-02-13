package com.circleci;

import com.circleci.api.model.Build;
import com.intellij.util.messages.Topic;

import java.util.EventListener;
import java.util.List;

public interface CheckingListener extends EventListener {

    Topic<CheckingListener> TOPIC = Topic.create("CircleCI List Loading", CheckingListener.class);

    void listUpdated(List<Build> builds);

}
