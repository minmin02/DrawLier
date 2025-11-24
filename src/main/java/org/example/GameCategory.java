// GameCategory.java - 새로운 파일 생성
package org.example;

import java.util.*;

public enum GameCategory {
    직업("경찰", "소방관", "의사", "교사", "프로그래머", "요리사", "변호사", "간호사"),
    동물("사자", "호랑이", "코끼리", "기린", "펭귄", "돌고래", "강아지", "고양이"),
    음식("피자", "치킨", "햄버거", "초밥", "파스타", "라면", "김치찌개", "삼겹살"),
    영화("어벤져스", "타이타닉", "겨울왕국", "기생충", "해리포터", "스타워즈", "쥬라기공원", "인터스텔라"),
    스포츠("축구", "야구", "농구", "배구", "테니스", "수영", "골프", "배드민턴");

    private final List<String> keywords;

    GameCategory(String... keywords) {
        this.keywords = Arrays.asList(keywords);
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public String getRandomKeyword() {
        Random random = new Random();
        return keywords.get(random.nextInt(keywords.size()));
    }

    public static GameCategory[] getAllCategories() {
        return values();
    }

    public static String[] getCategoryNames() {
        GameCategory[] categories = values();
        String[] names = new String[categories.length];
        for (int i = 0; i < categories.length; i++) {
            names[i] = categories[i].name();
        }
        return names;
    }
}