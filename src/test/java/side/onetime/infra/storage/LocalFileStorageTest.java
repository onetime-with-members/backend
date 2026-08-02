package side.onetime.infra.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFileStorageTest {

    private static final String BASE_URL = "https://api.onetime.run/files";

    private LocalFileStorage storage(Path root) {
        return new LocalFileStorage(root.toString(), BASE_URL);
    }

    @Test
    @DisplayName("업로드하면 디렉토리 하위에 키 구조 그대로 저장되고, 그 키로 삭제된다")
    void uploadAndDelete(@TempDir Path root) throws IOException {
        LocalFileStorage sut = storage(root);
        MockMultipartFile image = new MockMultipartFile(
                "image", "qr.png", "image/png", "hello".getBytes());

        String key = sut.uploadImage("qr", image);

        assertTrue(key.startsWith("qr/"), "키는 디렉토리 접두사를 유지해야 한다: " + key);
        Path stored = root.resolve(key);
        assertTrue(Files.exists(stored));
        assertEquals("hello", Files.readString(stored));

        sut.deleteFile(key);
        assertFalse(Files.exists(stored));
    }

    @Test
    @DisplayName("퍼블릭 URL은 베이스 URL + 키이고, 왕복 변환이 성립한다")
    void publicUrlRoundTrip(@TempDir Path root) {
        LocalFileStorage sut = storage(root);
        String key = "banner/3/abc_logo.png";

        String url = sut.getPublicUrl(key);

        assertEquals(BASE_URL + "/" + key, url);
        assertEquals(key, sut.extractKey(url));
    }

    @Test
    @DisplayName("마이그레이션 이전에 저장된 S3 형식 URL에서도 키를 추출한다")
    void extractKeyFromLegacyS3Url(@TempDir Path root) {
        LocalFileStorage sut = storage(root);

        String key = sut.extractKey(
                "https://onetime-bucket.s3.ap-northeast-2.amazonaws.com/banner/3/abc_logo.png");

        assertEquals("banner/3/abc_logo.png", key);
    }

    @Test
    @DisplayName("공백과 한글이 든 파일명도 업로드 -> URL -> 키 왕복이 성립한다")
    void roundTripWithSpaceAndHangulFilename(@TempDir Path root) throws IOException {
        LocalFileStorage sut = storage(root);
        MockMultipartFile image = new MockMultipartFile(
                "image", "스크린샷 2025-09-12 오후 7.31.54.png", "image/png", "x".getBytes());

        String key = sut.uploadImage("banner/8", image);
        String url = sut.getPublicUrl(key);

        // URI 파서를 쓰면 여기서 터진다. 실제 배너 파일명이 이 형태다.
        assertEquals(key, sut.extractKey(url));
        assertTrue(Files.exists(root.resolve(key)));

        sut.deleteFile(sut.extractKey(url));
        assertFalse(Files.exists(root.resolve(key)));
    }

    @Test
    @DisplayName("public-base-url이 비어 있으면 기동 시점에 실패한다")
    void failsFastWhenPublicBaseUrlMissing(@TempDir Path root) {
        assertThrows(IllegalStateException.class,
                () -> new LocalFileStorage(root.toString(), "  "));
    }

    @Test
    @DisplayName("없는 파일 삭제는 예외 없이 통과한다 (S3 동작과 동일)")
    void deleteMissingFileIsNoop(@TempDir Path root) {
        LocalFileStorage sut = storage(root);

        sut.deleteFile("qr/does-not-exist");
    }

    @Test
    @DisplayName("루트를 벗어나는 키는 거부한다")
    void rejectsPathTraversal(@TempDir Path root) {
        LocalFileStorage sut = storage(root);

        assertThrows(IllegalArgumentException.class,
                () -> sut.deleteFile("../../etc/passwd"));
        assertThrows(IllegalArgumentException.class,
                () -> sut.deleteFile("/etc/passwd"));
    }

    @Test
    @DisplayName("루트 자신을 가리키는 키는 거부한다 — 저장소 디렉토리가 지워지면 안 된다")
    void rejectsKeyResolvingToRoot(@TempDir Path root) {
        LocalFileStorage sut = storage(root);

        // 베이스 URL 뒤에 아무것도 없는 URL -> 빈 키
        assertThrows(IllegalArgumentException.class,
                () -> sut.deleteFile(sut.extractKey(BASE_URL + "/")));
        // 상쇄되어 루트로 정규화되는 키
        assertThrows(IllegalArgumentException.class,
                () -> sut.deleteFile("qr/.."));

        assertTrue(Files.exists(root), "저장소 루트가 살아있어야 한다");
    }

    @Test
    @DisplayName("이미지가 아닌 형식은 업로드를 거부한다 — 같은 오리진에서 서빙되므로 저장형 XSS가 된다")
    void rejectsNonImageUpload(@TempDir Path root) {
        LocalFileStorage sut = storage(root);

        assertThrows(IllegalArgumentException.class, () -> sut.uploadImage("banner/1",
                new MockMultipartFile("i", "x.svg", "image/svg+xml", "<svg onload=alert(1)>".getBytes())));
        assertThrows(IllegalArgumentException.class, () -> sut.uploadImage("banner/1",
                new MockMultipartFile("i", "x.html", "text/html", "<script>".getBytes())));
        // content-type을 위조해도 확장자에서 걸린다
        assertThrows(IllegalArgumentException.class, () -> sut.uploadImage("banner/1",
                new MockMultipartFile("i", "x.html", "image/png", "<script>".getBytes())));
    }

    @Test
    @DisplayName("확장자 없는 QR 이미지는 계속 허용한다 (원본 파일명이 \"qr\")")
    void allowsExtensionlessQrImage(@TempDir Path root) throws IOException {
        LocalFileStorage sut = storage(root);

        String key = sut.uploadImage("qr",
                new MockMultipartFile("qr", "qr", "image/png", "png-bytes".getBytes()));

        assertTrue(Files.exists(root.resolve(key)));
    }

    @Test
    @DisplayName("원본 파일명의 경로 구분자는 제거되어 루트를 벗어나지 못한다")
    void sanitizesOriginalFilename(@TempDir Path root) throws IOException {
        LocalFileStorage sut = storage(root);
        MockMultipartFile image = new MockMultipartFile(
                "image", "../../evil.png", "image/png", "x".getBytes());

        String key = sut.uploadImage("qr", image);

        // 구분자가 사라지면 ".." 는 그냥 파일명의 일부라 상위로 올라가지 못한다
        assertEquals("qr", key.substring(0, key.indexOf('/')));
        assertFalse(key.substring(key.indexOf('/') + 1).contains("/"), "키에 하위 경로가 생기면 안 된다: " + key);
        assertTrue(Files.exists(root.resolve(key)));
        assertTrue(root.resolve(key).normalize().startsWith(root), "루트를 벗어났다: " + key);
    }
}
