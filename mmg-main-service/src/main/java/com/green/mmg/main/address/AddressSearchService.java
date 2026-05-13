package com.green.mmg.main.address;

import com.green.mmg.main.address.model.AddressSearchRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressSearchService {

    private final RestTemplate restTemplate;  // Phase 2-Backfill-D-2-B: 싱글톤 Bean 주입 (timeout 적용)

    @Value("${naver.client-id}")
    private String clientId;

    @Value("${naver.client-secret}")
    private String clientSecret;

    @Value("${naver.search-client-id}")
    private String searchClientId;

    @Value("${naver.search-client-secret}")
    private String searchClientSecret;

    // ── 1. 통합 검색 (지역검색 우선, 없으면 Geocoding)
    public List<AddressSearchRes> search(String query) {
        List<AddressSearchRes> result = searchByLocal(query);
        if (result.isEmpty()) {
            result = searchByGeocoding(query);
        }
        return result;
    }

    // ── 2. 네이버 지역검색 (장소명 검색)
    private List<AddressSearchRes> searchByLocal(String query) {
        try {
            String url = UriComponentsBuilder
                    .fromUriString("https://openapi.naver.com/v1/search/local.json")
                    .queryParam("query", query)
                    .queryParam("display", 5)
                    .build()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Naver-Client-Id", searchClientId);
            headers.set("X-Naver-Client-Secret", searchClientSecret);

            ResponseEntity<Map> response = restTemplate
                    .exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

            List<Map<String, Object>> items =
                    (List<Map<String, Object>>) response.getBody().get("items");

            List<AddressSearchRes> result = new ArrayList<>();
            if (items != null) {
                for (Map<String, Object> item : items) {
                    String roadAddr  = clean((String) item.get("roadAddress"));
                    String jibunAddr = clean((String) item.get("address"));
                    double lng = Double.parseDouble(String.valueOf(item.get("mapx"))) / 1e7;
                    double lat = Double.parseDouble(String.valueOf(item.get("mapy"))) / 1e7;
                    result.add(new AddressSearchRes(
                            roadAddr.isEmpty()  ? jibunAddr : roadAddr,
                            jibunAddr.isEmpty() ? roadAddr  : jibunAddr,
                            lat, lng
                    ));
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("지역 검색 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // ── 3. Geocoding (도로명/지번 주소 검색)
    private List<AddressSearchRes> searchByGeocoding(String query) {
        try {
            String url = UriComponentsBuilder
                    .fromUriString("https://maps.apigw.ntruss.com/map-geocode/v2/geocode")
                    .queryParam("query", query)
                    .build()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-NCP-APIGW-API-KEY-ID", clientId);
            headers.set("X-NCP-APIGW-API-KEY", clientSecret);

            ResponseEntity<Map> response = restTemplate
                    .exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

            List<Map<String, Object>> addresses =
                    (List<Map<String, Object>>) response.getBody().get("addresses");

            List<AddressSearchRes> result = new ArrayList<>();
            if (addresses != null) {
                for (Map<String, Object> addr : addresses) {
                    result.add(new AddressSearchRes(
                            (String) addr.get("roadAddress"),
                            (String) addr.get("jibunAddress"),
                            Double.parseDouble((String) addr.get("y")),
                            Double.parseDouble((String) addr.get("x"))
                    ));
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Geocoding 검색 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // ── 4. Reverse Geocoding (좌표 → 주소, 지도 클릭 시 사용)
    public AddressSearchRes reverseGeocode(double lat, double lng) {
        try {
            String url = UriComponentsBuilder
                    .fromUriString("https://maps.apigw.ntruss.com/map-reversegeocode/v2/gc")
                    .queryParam("coords", lng + "," + lat)
                    .queryParam("output", "json")
                    .queryParam("orders", "roadaddr,addr")
                    .build()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-NCP-APIGW-API-KEY-ID", clientId);
            headers.set("X-NCP-APIGW-API-KEY", clientSecret);

            ResponseEntity<Map> response = restTemplate
                    .exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

            List<Map<String, Object>> results =
                    (List<Map<String, Object>>) response.getBody().get("results");

            if (results == null || results.isEmpty()) {
                return new AddressSearchRes("주소를 찾을 수 없습니다.", "", lat, lng);
            }

            String roadAddress  = "";
            String jibunAddress = "";

            for (Map<String, Object> r : results) {
                String name   = (String) r.get("name");
                Map<String, Object> region = (Map<String, Object>) r.get("region");
                Map<String, Object> land   = (Map<String, Object>) r.get("land");

                if (region == null) continue;

                String area1 = getAreaName(region, "area1");
                String area2 = getAreaName(region, "area2");
                String area3 = getAreaName(region, "area3");

                if ("roadaddr".equals(name) && land != null) {
                    String landName = clean((String) land.get("name"));
                    String num1     = clean((String) land.get("number1"));
                    String num2     = clean((String) land.get("number2"));
                    roadAddress = area1 + " " + area2 + " " + area3 + " " + landName
                            + " " + num1 + (num2.isEmpty() ? "" : "-" + num2);
                } else if ("addr".equals(name) && land != null) {
                    String num1 = clean((String) land.get("number1"));
                    String num2 = clean((String) land.get("number2"));
                    jibunAddress = area1 + " " + area2 + " " + area3
                            + " " + num1 + (num2.isEmpty() ? "" : "-" + num2);
                }
            }

            return new AddressSearchRes(
                    roadAddress.isBlank()  ? jibunAddress : roadAddress,
                    jibunAddress.isBlank() ? roadAddress  : jibunAddress,
                    lat, lng
            );

        } catch (Exception e) {
            log.warn("Reverse Geocoding 실패: {}", e.getMessage());
            return new AddressSearchRes("주소를 찾을 수 없습니다.", "", lat, lng);
        }
    }

    private String getAreaName(Map<String, Object> region, String key) {
        Map<String, Object> area = (Map<String, Object>) region.get(key);
        if (area == null) return "";
        String name = (String) area.get("name");
        return name == null ? "" : name;
    }

    private String clean(String s) {
        if (s == null) return "";
        return s.replaceAll("<[^>]*>", "").trim();
    }
}