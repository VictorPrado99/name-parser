package aggregators;

public record ResultData(
        Integer lineOffset,
        Integer charOffset
) {

    @Override
    public String toString() {
        return "[lineOffset=" + lineOffset +
                ", charOffset=" + charOffset +
                "]";
    }
}
