package com.placeboard.map;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 도로명/지번 주소에서 시·도, 시·군·구, 읍·면·동을 추출한다.
 */
public final class KoreanAddressParser {
    private static final Map<String, String> SIDO = Map.ofEntries(
            Map.entry("서울", "서울특별시"), Map.entry("서울시", "서울특별시"), Map.entry("서울특별시", "서울특별시"),
            Map.entry("부산", "부산광역시"), Map.entry("부산광역시", "부산광역시"),
            Map.entry("대구", "대구광역시"), Map.entry("대구광역시", "대구광역시"),
            Map.entry("인천", "인천광역시"), Map.entry("인천광역시", "인천광역시"),
            Map.entry("광주", "광주광역시"), Map.entry("광주광역시", "광주광역시"),
            Map.entry("대전", "대전광역시"), Map.entry("대전광역시", "대전광역시"),
            Map.entry("울산", "울산광역시"), Map.entry("울산광역시", "울산광역시"),
            Map.entry("세종", "세종특별자치시"), Map.entry("세종특별자치시", "세종특별자치시"),
            Map.entry("경기", "경기도"), Map.entry("경기도", "경기도"),
            Map.entry("강원", "강원특별자치도"), Map.entry("강원도", "강원특별자치도"), Map.entry("강원특별자치도", "강원특별자치도"),
            Map.entry("충북", "충청북도"), Map.entry("충청북도", "충청북도"),
            Map.entry("충남", "충청남도"), Map.entry("충청남도", "충청남도"),
            Map.entry("전북", "전북특별자치도"), Map.entry("전라북도", "전북특별자치도"), Map.entry("전북특별자치도", "전북특별자치도"),
            Map.entry("전남", "전라남도"), Map.entry("전라남도", "전라남도"),
            Map.entry("경북", "경상북도"), Map.entry("경상북도", "경상북도"),
            Map.entry("경남", "경상남도"), Map.entry("경상남도", "경상남도"),
            Map.entry("제주", "제주특별자치도"), Map.entry("제주도", "제주특별자치도"), Map.entry("제주특별자치도", "제주특별자치도")
    );
    private static final Pattern DONG_TOKEN = Pattern.compile("^[가-힣0-9·]+(동|읍|면|가)$");
    private static final Pattern PARENTHESIS_DONG = Pattern.compile("\\(([가-힣0-9·]+(?:동|읍|면|가))(?:,|\\))");

    private KoreanAddressParser() {
    }

    public record Region(String country, String sido, String sigungu, String dong) {
    }

    public static Region parse(String roadAddress, String jibunAddress) {
        String base = firstNonBlank(roadAddress, jibunAddress);
        if (base == null) return new Region(null, null, null, null);
        List<String> tokens = tokens(base);
        if (!tokens.isEmpty() && (tokens.get(0).equals("대한민국") || tokens.get(0).equals("한국"))) {
            tokens = tokens.subList(1, tokens.size());
        }
        String sido = tokens.isEmpty() ? null : SIDO.get(tokens.get(0));
        if (sido == null) return new Region(null, null, null, null);

        String sigungu = null;
        if (!sido.equals("세종특별자치시") && tokens.size() > 1 && isSigungu(tokens.get(1))) {
            sigungu = tokens.get(1);
            if (sigungu.endsWith("시") && tokens.size() > 2 && tokens.get(2).endsWith("구")) {
                sigungu = sigungu + " " + tokens.get(2);
            }
        }
        return new Region("KR", sido, sigungu, findDong(roadAddress, jibunAddress));
    }

    private static String findDong(String roadAddress, String jibunAddress) {
        if (jibunAddress != null) {
            for (String token : tokens(jibunAddress)) {
                if (!SIDO.containsKey(token) && DONG_TOKEN.matcher(token).matches()) return token;
            }
        }
        if (roadAddress != null) {
            Matcher matcher = PARENTHESIS_DONG.matcher(roadAddress);
            if (matcher.find()) return matcher.group(1);
        }
        return null;
    }

    private static boolean isSigungu(String token) {
        return token.endsWith("시") || token.endsWith("군") || token.endsWith("구");
    }

    private static List<String> tokens(String value) {
        return Arrays.stream(value.trim().split("\\s+")).filter(token -> !token.isBlank()).toList();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }
}
