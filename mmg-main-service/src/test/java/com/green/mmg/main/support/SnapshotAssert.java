package com.green.mmg.main.support;

import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 응답 JSON을 파일 스냅샷과 비교. 첫 실행 = 파일 자동 생성 + 통과,
 * 이후 실행 = STRICT 비교 (필드명/타입/순서/null 1바이트 동결).
 *
 * <p>Phase 3-B JPA 전환 검증: 전환 전 실행으로 snapshot 생성 → 전환 후 동일 실행으로 diff = 0 확인.</p>
 *
 * <p>스냅샷 위치: {@code src/test/resources/snapshots/{name}.json}</p>
 *
 * <p>스냅샷 갱신 (의도적 응답 변경 시): SNAPSHOT_UPDATE=true 환경변수 또는 -DsnapshotUpdate=true</p>
 */
public final class SnapshotAssert {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    /** 모듈 디렉터리 기준. 빌드 스크립트가 -Dproject.dir 주입 (root build.gradle test task). */
    private static final Path SNAPSHOT_DIR = Paths.get(
            System.getProperty("project.dir", "."),
            "src", "test", "resources", "snapshots"
    );

    private SnapshotAssert() {}

    public static void assertMatches(String snapshotName, String actualJson) {
        try {
            Path file = SNAPSHOT_DIR.resolve(snapshotName + ".json");
            boolean updateMode = Boolean.parseBoolean(System.getProperty("snapshotUpdate", "false"))
                    || "true".equalsIgnoreCase(System.getenv("SNAPSHOT_UPDATE"));

            if (!Files.exists(file) || updateMode) {
                Files.createDirectories(SNAPSHOT_DIR);
                Files.writeString(file, prettyPrint(actualJson), StandardCharsets.UTF_8);
                System.out.println("[snapshot] " + (updateMode ? "updated" : "created") + ": " + file);
                return;
            }

            String expected = Files.readString(file, StandardCharsets.UTF_8);
            JSONAssert.assertEquals(expected, actualJson, JSONCompareMode.STRICT);
        } catch (IOException e) {
            throw new RuntimeException("snapshot IO failed: " + snapshotName, e);
        } catch (org.json.JSONException e) {
            throw new RuntimeException("snapshot JSON compare failed: " + snapshotName, e);
        }
    }

    private static String prettyPrint(String json) {
        Object obj = MAPPER.readValue(json, Object.class);
        return MAPPER.writeValueAsString(obj);
    }
}
