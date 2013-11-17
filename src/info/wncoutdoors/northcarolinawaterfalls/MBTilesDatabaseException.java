package info.wncoutdoors.northcarolinawaterfalls;

public class MBTilesDatabaseException extends Exception {
    public MBTilesDatabaseException() {
    }

    public MBTilesDatabaseException(String detailMessage) {
        super(detailMessage);
    }

    public MBTilesDatabaseException(Throwable throwable) {
        super(throwable);
    }

    public MBTilesDatabaseException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }
}
