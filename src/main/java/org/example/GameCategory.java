package org.example;

import java.util.*;

/**
 * 게임의 주제어 카테고리와 각 카테고리에 속한 키워드들을 관리하는 열거형(Enum).
 * - 각 카테고리는 여러 개의 키워드를 가짐.
 * - 랜덤 키워드를 제공하는 기능을 포함.
 */
public enum GameCategory {
    //각 카테고리와 해당 키워드 목록 정의
    직업("경찰", "소방관", "의사", "교사", "프로그래머", "요리사", "변호사", "간호사"),
    동물("사자", "호랑이", "코끼리", "기린", "펭귄", "돌고래", "강아지", "고양이"),
    음식("피자", "치킨", "햄버거", "초밥", "파스타", "라면", "김치찌개", "삼겹살"),
    영화("어벤져스", "타이타닉", "겨울왕국", "기생충", "해리포터", "스타워즈", "쥬라기공원", "인터스텔라"),
    스포츠("축구", "야구", "농구", "배구", "테니스", "수영", "골프", "배드민턴");

    //필드
    private final List<String> keywords; //카테고리에 속한 키워드 리스트

    //생성자: 가변 인자로 키워드들을 받아 리스트로 초기화
    GameCategory(String... keywords) {
        this.keywords = Arrays.asList(keywords);
    }

    //현재 카테고리의 모든 키워드 리스트를 반환하는 메소드
    public List<String> getKeywords() {
        return keywords;
    }

    //현재 카테고리에서 랜덤으로 키워드 하나를 선택하여 반환하는 메소드
    public String getRandomKeyword() {
        Random random = new Random();
        return keywords.get(random.nextInt(keywords.size()));
    }

    //모든 카테고리 객체 배열을 반환하는 정적 메소드
    public static GameCategory[] getAllCategories() {
        return values();
    }

    //모든 카테고리의 이름(문자열) 배열을 반환하는 정적 메소드 (UI의 JComboBox 등에서 사용)
    public static String[] getCategoryNames() {
        GameCategory[] categories = values();
        String[] names = new String[categories.length];
        for (int i = 0; i < categories.length; i++) {
            names[i] = categories[i].name();
        }
        return names;
    }
}
