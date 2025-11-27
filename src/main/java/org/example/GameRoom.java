package org.example;

import javax.swing.Timer;
import java.util.*;

/**
 * 턴 기반 게임룸 관리 클래스
 * - 4명의 플레이어
 * - 각 플레이어당 15초씩 4라운드
 * - 투표 시스템 추가
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
    private String selectedCategory;
    private String selectedKeyword;
    private String liarName;
    private Map<String, Boolean> playerRoles;

    // 투표 관련 필드
    private Map<String, String> votes; // 투표자 -> 피투표자
    private boolean votingPhase = false;

    // 게임 설정
    private static final int TURN_TIME_SECONDS = 1;
    private static final int MAX_ROUNDS = 1;
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
        this.playerRoles = new HashMap<>();
        this.votes = new HashMap<>();
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

        // 2. 라이어 랜덤 선택
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

        // 4. 게임 시작
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

        if (currentTurnIndex >= players.size()) {
            currentTurnIndex = 0;
            currentRound++;

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
        votingPhase = true;

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

    // 투표 관련 메서드
    public void addVote(String voter, String votedPlayer) {
        if (votingPhase && players.contains(voter) && players.contains(votedPlayer)) {
            votes.put(voter, votedPlayer);
        }
    }

    public boolean hasAllVoted() {
        // 모든 플레이어가 투표했는지 확인
        return votes.size() == players.size();
    }

    public String getMostVotedPlayer() {
        if (votes.isEmpty()) {
            return null;
        }

        Map<String, Integer> voteCount = new HashMap<>();
        for (String votedPlayer : votes.values()) {
            voteCount.put(votedPlayer, voteCount.getOrDefault(votedPlayer, 0) + 1);
        }

        // 최다 득표자 찾기
        String mostVoted = null;
        int maxVotes = 0;
        for (Map.Entry<String, Integer> entry : voteCount.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                mostVoted = entry.getKey();
            }
        }

        return mostVoted;
    }

    public boolean isMostVotedLiar() {
        String mostVoted = getMostVotedPlayer();
        return mostVoted != null && mostVoted.equals(liarName);
    }

    public boolean checkLiarAnswer(String answer) {
        if (answer == null || selectedKeyword == null) {
            return false;
        }

        // 대소문자 구분 없이, 공백 제거 후 비교
        String normalizedAnswer = answer.trim().toLowerCase();
        String normalizedKeyword = selectedKeyword.trim().toLowerCase();

        return normalizedAnswer.equals(normalizedKeyword);
    }

    // 현재 턴인 플레이어인지 확인
    public boolean isPlayerTurn(String playerName) {
        if (!isGameRunning || players.isEmpty()) {
            return false;
        }
        return players.get(currentTurnIndex).equals(playerName);
    }

    public boolean isLiar(String playerName) {
        return playerRoles.getOrDefault(playerName, false);
    }

    public String getPlayerInfo(String playerName) {
        if (isLiar(playerName)) {
            return "LIAR|" + selectedCategory;
        } else {
            return "CITIZEN|" + selectedKeyword;
        }
    }

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
    public String getSelectedCategory() { return selectedCategory; }
    public String getSelectedKeyword() { return selectedKeyword; }
    public String getLiarName() { return liarName; }
    public Map<String, Boolean> getPlayerRoles() { return new HashMap<>(playerRoles); }
    public boolean isVotingPhase() { return votingPhase; }
    public Map<String, String> getVotes() { return new HashMap<>(votes); }
}