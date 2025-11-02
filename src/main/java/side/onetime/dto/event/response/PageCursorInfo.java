package side.onetime.dto.event.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PageCursorInfo<T>(
        T nextCursor,
        boolean hasNext
) {
    public static <T> PageCursorInfo<T> of(T nextCursor, boolean hasNext) {
        return new PageCursorInfo<>(nextCursor, hasNext);
    }
}
