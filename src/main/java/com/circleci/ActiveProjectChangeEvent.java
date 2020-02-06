package com.circleci;

import com.circleci.api.model.Project;

public class ActiveProjectChangeEvent {

    private Project previous;
    private Project current;

    public ActiveProjectChangeEvent(Project previous, Project current) {
        this.previous = previous;
        this.current = current;
    }

    public Project getPrevious() {
        return previous;
    }

    public Project getCurrent() {
        return current;
    }

}
