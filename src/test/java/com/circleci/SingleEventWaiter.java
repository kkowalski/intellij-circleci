package com.circleci;

import com.intellij.util.messages.Topic;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class SingleEventWaiter {

    private com.intellij.openapi.project.Project intellijProject;

    public SingleEventWaiter(com.intellij.openapi.project.Project intellijProject) {
        this.intellijProject = intellijProject;
    }

    AtomicBoolean eventSeen = new AtomicBoolean(false);

    public <L> void waitForEvent(Topic<L> eventTopic, L listener) throws InterruptedException {
        intellijProject.getMessageBus()
                .connect()
                .subscribe(eventTopic, listener);

        waitAndCheck(eventSeen::get, 60, ChronoUnit.SECONDS);
    }

    private void waitAndCheck(Supplier<Boolean> operation, int timeout, TemporalUnit unit) throws InterruptedException {
        Instant start = Instant.now();
        Instant deadline = start.plus(timeout, unit);
        while (true) {
            boolean result = operation.get();
            if (result || Instant.now().isAfter(deadline)) {
                return;
            }
            Thread.sleep(100L);
        }
    }

}
