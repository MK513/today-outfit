package com.todayoutfit.image;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param storage 사용할 저장소 구현 (local). S3 등을 추가하면 여기에 값과 설정 묶음을 더한다.
 * @param local   로컬 디스크 저장소 설정
 */
@ConfigurationProperties("app.image")
public record ImageProperties(String storage, Local local) {

    /**
     * @param dir        사진 저장 폴더 (사용자별 하위 폴더로 나뉜다)
     * @param publicPath 클라이언트가 사진을 불러오는 URL 경로 (context-path 포함)
     */
    public record Local(Path dir, String publicPath) {
    }
}
