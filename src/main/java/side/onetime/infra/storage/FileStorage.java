package side.onetime.infra.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

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
