package com.circleci;

public class LoadRequests {

    public static LoadRequest check(int limit) {
        return new CheckRequest(0, limit);
    }

    public static LoadRequest merge(int limit) {
        return new MergeRequest(0, limit);
    }

    public static LoadRequest refresh() {
        return new RefreshRequest(0, 25);
    }

    public static LoadRequest more(int offset) {
        return new MoreRequest(offset, 10);
    }

}

abstract class LoadRequest {

    private int offset;
    private int limit;

    public LoadRequest(int offset, int limit) {
        this.offset = offset;
        this.limit = limit;
    }

    public int getOffset() {
        return offset;
    }

    public int getLimit() {
        return limit;
    }

}

class CheckRequest extends LoadRequest {
    public CheckRequest(int offset, int limit) {
        super(offset, limit);
    }
}

class RefreshRequest extends LoadRequest {
    public RefreshRequest(int offset, int limit) {
        super(offset, limit);
    }
}

class MergeRequest extends LoadRequest {
    public MergeRequest(int offset, int limit) {
        super(offset, limit);
    }
}

class MoreRequest extends LoadRequest {
    public MoreRequest(int offset, int limit) {
        super(offset, limit);
    }
}