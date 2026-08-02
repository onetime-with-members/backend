package side.onetime.infra.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 서버 로컬 디스크 기반 저장소 구현체.
 *
 * 파일은 {@code storage.local.root} 아래에 S3와 동일한 키 구조로 저장되고,
 * nginx가 {@code storage.local.public-base-url} 경로로 정적 서빙합니다.
 */
@Component
@ConditionalOnProperty(name = "storage.type", havingValue = "local")
public class LocalFileStorage implements FileStorage {

    private final Path root;
    private final String publicBaseUrl;

    public LocalFileStorage(
            @Value("${storage.local.root}") String root,
            @Value("${storage.local.public-base-url}") String publicBaseUrl
    ) {
        // 비어 있으면 getPublicUrl 이 루트 상대 경로를 뱉어 QR/배너 링크가 조용히 깨진다. 기동 시점에 막는다.
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            throw new IllegalStateException(
                    "storage.type=local 인 경우 storage.local.public-base-url(FILE_PUBLIC_BASE_URL)이 필요합니다.");
        }
        this.root = Path.of(root).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
    }

    @Override
    public String uploadImage(String directoryName, MultipartFile image) throws IOException {
        String key = directoryName + "/" + UUID.randomUUID() + "_" + sanitize(image.getOriginalFilename());
        Path target = resolve(key);

        Files.createDirectories(target.getParent());
        try (var in = image.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return key;
    }

    @Override
    public String getPublicUrl(String key) {
        return publicBaseUrl + "/" + key;
    }

    /**
     * 퍼블릭 URL에서 키를 추출합니다.
     *
     * 마이그레이션 이전에 저장된 S3 형식 URL도 처리할 수 있도록,
     * 베이스 URL 접두사가 있으면 떼어내고 없으면 호스트 뒤 경로 전체를 키로 봅니다.
     *
     * URL 파서를 쓰지 않고 문자열로 자릅니다. 업로드 원본 파일명에 공백이나 한글이
     * 그대로 들어가는데({@code "스크린샷 2025-09-12.png"}), 그런 URL은 URI 파서가 거부합니다.
     */
    @Override
    public String extractKey(String publicUrl) {
        if (publicUrl == null || publicUrl.isBlank()) {
            throw new IllegalArgumentException("유효하지 않은 파일 URL : " + publicUrl);
        }
        if (publicUrl.startsWith(publicBaseUrl + "/")) {
            return publicUrl.substring(publicBaseUrl.length() + 1);
        }
        int schemeEnd = publicUrl.indexOf("://");
        int pathStart = publicUrl.indexOf('/', schemeEnd < 0 ? 0 : schemeEnd + 3);
        if (pathStart < 0 || pathStart == publicUrl.length() - 1) {
            throw new IllegalArgumentException("유효하지 않은 파일 URL : " + publicUrl);
        }
        return publicUrl.substring(pathStart + 1);
    }

    @Override
    public void deleteFile(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new IllegalStateException("파일 삭제 실패: " + key, e);
        }
    }

    /**
     * 키를 루트 하위 실제 경로로 변환합니다. 루트를 벗어나면 거부합니다. (경로 탈출 방지)
     */
    private Path resolve(String key) {
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("허용되지 않은 파일 경로: " + key);
        }
        return target;
    }

    /**
     * 업로드 원본 파일명에서 경로 구분자를 제거합니다.
     */
    private String sanitize(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "file";
        }
        return originalFilename.replaceAll("[/\\\\]", "_");
    }
}
