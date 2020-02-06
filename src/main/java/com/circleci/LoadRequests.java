package com.circleci;

public class LoadRequests {

    public static LoadRequest check() {
        return new CheckRequest();
    }

    public static LoadRequest merge() {
        return new MergeRequest();
    }

    public static LoadRequest refresh() {
        return new RefreshRequest();
    }

    public static LoadRequest more() {
        return new MoreRequest();
    }

}

abstract class LoadRequest {

}

class CheckRequest extends LoadRequest {
}

class RefreshRequest extends LoadRequest {
}

class MergeRequest extends LoadRequest {
}

class MoreRequest extends LoadRequest {
}