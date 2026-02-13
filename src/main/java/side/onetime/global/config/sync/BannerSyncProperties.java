package side.onetime.global.config.sync;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.sync")
public record BannerSyncProperties(
        String targetUrl,
        String apiKey
) {
	/**
	 * 테스트 서버용: 데이터를 내보낼 수 있는 상태인지 (url과 키 둘 다 필요)
	 */
	public boolean canExport() {
		return StringUtils.isNotBlank(targetUrl) && StringUtils.isNotBlank(apiKey);
	}
	
	/**
	 * 운영 서버용: 데이터를 받을 수 있는 상태인지 (키만 있으면 됨)
	 */
	public boolean canReceive() {
		return StringUtils.isNotBlank(apiKey);
	}
}
