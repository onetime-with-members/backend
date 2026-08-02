package side.onetime.infra.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

/**
 * 이미지 저장소 추상화.
 *
 * 구현체는 {@code storage.type} 프로퍼티로 선택됩니다.
 * <ul>
 *     <li>{@code s3}    - AWS S3 ({@link S3FileStorage}, 기본값)</li>
 *     <li>{@code local} - 서버 로컬 디스크 ({@link LocalFileStorage})</li>
 * </ul>
 */
public interface FileStorage {

    Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/png", "image/jpeg", "image/webp", "image/gif");

    Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp", "gif");

    /**
     * 업로드 이미지의 형식을 검증합니다. 구현체는 저장 전에 반드시 호출해야 합니다.
     *
     * 로컬 저장소는 파일을 API와 같은 오리진(api.onetime.run)에서 서빙하므로,
     * svg/html 같은 스크립트 해석 가능한 형식이 올라가면 저장형 XSS가 됩니다.
     * nginx 쪽에서도 해당 확장자를 차단하지만, 애초에 디스크에 남기지 않습니다.
     *
     * 확장자가 없는 경우는 허용합니다. QR 이미지의 원본 파일명이 {@code "qr"} 입니다.
     */
    static void validateImage(MultipartFile image) {
        String contentType = image.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("허용되지 않은 이미지 형식입니다: " + contentType);
        }

        String filename = image.getOriginalFilename();
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        if (dot >= 0) {
            String extension = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                throw new IllegalArgumentException("허용되지 않은 이미지 확장자입니다: " + extension);
            }
        }
    }

    /**
     * 이미지를 업로드하고 저장소 내 고유 키를 반환합니다.
     *
     * @param directoryName 저장할 디렉토리명 (예: {@code qr}, {@code banner/3})
     * @param image         업로드할 이미지 파일
     * @return 저장소에 기록된 키 (퍼블릭 URL 아님)
     * @throws IOException 업로드 중 오류 발생 시
     */
    String uploadImage(String directoryName, MultipartFile image) throws IOException;

    /**
     * 키에 해당하는 파일의 퍼블릭 URL을 반환합니다.
     */
    String getPublicUrl(String key);

    /**
     * 퍼블릭 URL에서 저장소 키를 추출합니다.
     */
    String extractKey(String publicUrl);

    /**
     * 키에 해당하는 파일을 삭제합니다.
     */
    void deleteFile(String key);
}
