package org.example;

import javax.swing.Timer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 턴 기반 게임룸 관리 클래스
 * - 4명의 플레이어
 * - 각 플레이어당 15초씩 4라운드
 */
public class GameRoom {
    private String roomId;
    private String roomName;
    private String hostName;
    private String category;
    private int maxPlayers;

    private List<String> players;
    private int currentTurnIndex;
    private int currentRound;
    private boolean isGameRunning;
    private Timer turnTimer;
    private GameEventListener eventListener;

    // 게임 역할 관련 필드
    private String selectedCategory;      // 선택된 대분류
    private String selectedKeyword;       // 선택된 소분류 (키워드)
    private String liarName;              // 라이어로 지정된 플레이어
    private Map<String, Boolean> playerRoles; // 플레이어별 역할 (true: 라이어, false: 일반)

    // 게임 설정
    private static final int TURN_TIME_SECONDS = 15;
    private static final int MAX_ROUNDS = 4;
    private static final int REQUIRED_PLAYERS = 4;

    private int remainingSeconds;

    public interface GameEventListener {
        void onTurnChanged(int turnIndex, int round, String currentPlayer);
        void onGameEnded();
        void onTimerTick(int remainingSeconds);
    }

    public GameRoom(String roomId, String roomName, String hostName, String category, int maxPlayers) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.hostName = hostName;
        this.category = category;
        this.maxPlayers = maxPlayers;
        this.players = new ArrayList<>();
        this.currentTurnIndex = 0;
        this.currentRound = 1;
        this.isGameRunning = false;
        this.remainingSeconds = TURN_TIME_SECONDS;
        this.playerRoles = new HashMap<>();  // ⭐ 추가
    }

    public void addPlayer(String playerName) {
        if (!players.contains(playerName) && players.size() < maxPlayers) {
            players.add(playerName);
        }
    }

    public void removePlayer(String playerName) {
        players.remove(playerName);
    }

    public boolean canStartGame() {
        return players.size() == REQUIRED_PLAYERS && !isGameRunning;
    }

    // ⭐ 기존 메서드 (호환성 유지)
    public void startGame(GameEventListener listener) {
        if (!canStartGame()) {
            return;
        }

        this.eventListener = listener;
        this.isGameRunning = true;
        this.currentTurnIndex = 0;
        this.currentRound = 1;
        this.remainingSeconds = TURN_TIME_SECONDS;

        startTurnTimer();
        notifyTurnChange();
    }

    // ⭐ 새로운 메서드: 카테고리 기반 게임 시작
    public void startGameWithCategory(String selectedCategory, GameEventListener listener) {
        if (!canStartGame()) {
            return;
        }

        this.selectedCategory = selectedCategory;

        // 1. 카테고리에서 랜덤 키워드 선택
        try {
            GameCategory gameCategory = GameCategory.valueOf(selectedCategory);
            this.selectedKeyword = gameCategory.getRandomKeyword();
        } catch (IllegalArgumentException e) {
            System.err.println("유효하지 않은 카테고리: " + selectedCategory);
            return;
        }

        // 2. 라이어 랜덤 선택 (4명 중 1명)
        Random random = new Random();
        int liarIndex = random.nextInt(players.size());
        this.liarName = players.get(liarIndex);

        // 3. 역할 할당
        this.playerRoles = new HashMap<>();
        for (int i = 0; i < players.size(); i++) {
            String player = players.get(i);
            boolean isLiar = (i == liarIndex);
            playerRoles.put(player, isLiar);
        }

        // 4. 기존 게임 시작 로직 실행
        this.eventListener = listener;
        this.isGameRunning = true;
        this.currentTurnIndex = 0;
        this.currentRound = 1;
        this.remainingSeconds = TURN_TIME_SECONDS;

        startTurnTimer();
        notifyTurnChange();
    }

    private void startTurnTimer() {
        if (turnTimer != null) {
            turnTimer.stop();
        }

        remainingSeconds = TURN_TIME_SECONDS;

        turnTimer = new Timer(1000, e -> {
            remainingSeconds--;

            if (eventListener != null) {
                eventListener.onTimerTick(remainingSeconds);
            }

            if (remainingSeconds <= 0) {
                nextTurn();
            }
        });

        turnTimer.start();
    }

    private void nextTurn() {
        if (turnTimer != null) {
            turnTimer.stop();
        }

        currentTurnIndex++;

        // 모든 플레이어가 턴을 마쳤으면 다음 라운드로
        if (currentTurnIndex >= players.size()) {
            currentTurnIndex = 0;
            currentRound++;

            // 4라운드가 끝나면 게임 종료
            if (currentRound > MAX_ROUNDS) {
                endGame();
                return;
            }
        }

        remainingSeconds = TURN_TIME_SECONDS;
        startTurnTimer();
        notifyTurnChange();
    }

    private void notifyTurnChange() {
        if (eventListener != null && !players.isEmpty()) {
            String currentPlayer = players.get(currentTurnIndex);
            eventListener.onTurnChanged(currentTurnIndex, currentRound, currentPlayer);
        }
    }

    private void endGame() {
        isGameRunning = false;
        if (turnTimer != null) {
            turnTimer.stop();
            turnTimer = null;
        }

        if (eventListener != null) {
            eventListener.onGameEnded();
        }
    }

    public void stopGame() {
        endGame();
    }

    // 현재 턴인 플레이어인지 확인
    public boolean isPlayerTurn(String playerName) {
        if (!isGameRunning || players.isEmpty()) {
            return false;
        }
        return players.get(currentTurnIndex).equals(playerName);
    }

    // ⭐ 새로운 메서드들
    // 플레이어가 라이어인지 확인
    public boolean isLiar(String playerName) {
        return playerRoles.getOrDefault(playerName, false);
    }

    // 플레이어에게 보여줄 정보 가져오기 (LIAR|카테고리 or CITIZEN|키워드)
    public String getPlayerInfo(String playerName) {
        if (isLiar(playerName)) {
            return "LIAR|" + selectedCategory;
        } else {
            return "CITIZEN|" + selectedKeyword;
        }
    }

    // 게임 상태를 문자열로 반환 (클라이언트에게 전송용)
    public String getGameStateString() {
        if (players.isEmpty()) {
            return "/gameState|0|1|NONE|15";
        }

        String currentPlayer = players.get(currentTurnIndex);
        return String.format("/gameState|%d|%d|%s|%d",
                currentTurnIndex, currentRound, currentPlayer, remainingSeconds);
    }

    // Getters
    public String getRoomId() { return roomId; }
    public String getRoomName() { return roomName; }
    public String getHostName() { return hostName; }
    public String getCategory() { return category; }
    public int getMaxPlayers() { return maxPlayers; }
    public List<String> getPlayers() { return new ArrayList<>(players); }
    public int getCurrentTurnIndex() { return currentTurnIndex; }
    public int getCurrentRound() { return currentRound; }
    public boolean isGameRunning() { return isGameRunning; }
    public int getRemainingSeconds() { return remainingSeconds; }
    public String getCurrentPlayer() {
        if (players.isEmpty()) return "";
        return players.get(currentTurnIndex);
    }

    // ⭐ 새로운 Getter들
    public String getSelectedCategory() { return selectedCategory; }
    public String getSelectedKeyword() { return selectedKeyword; }
    public String getLiarName() { return liarName; }
    public Map<String, Boolean> getPlayerRoles() {
        return new HashMap<>(playerRoles);
    }
}