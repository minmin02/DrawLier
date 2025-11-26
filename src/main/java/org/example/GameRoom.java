package org.example;

import javax.swing.Timer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 턴 기반 게임룸 관리 클래스
 * - 4명의 플레이어
 * - 각 플레이어당 15초씩 4라운드
 * - 투표 시스템 관리
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

    // 게임 설정
    private static final int TURN_TIME_SECONDS = 15;
    private static final int MAX_ROUNDS = 4;
    private static final int REQUIRED_PLAYERS = 4;

    private int remainingSeconds;

    // 투표 관련
    private Map<String, String> votes; // voterName -> votedPlayer
    private boolean isVotingPhase;

    public interface GameEventListener {
        void onTurnChanged(int turnIndex, int round, String currentPlayer);
        void onGameEnded();
        void onTimerTick(int remainingSeconds);
        void onVoteComplete(String maxVotedPlayer, Map<String, Integer> voteCount);
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
        this.votes = new HashMap<>();
        this.isVotingPhase = false;
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

            // 4라운드가 끝나면 게임 종료 및 투표 시작
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

        // 투표 단계 시작
        startVotingPhase();
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

    // 게임 상태를 문자열로 반환 (클라이언트에게 전송용)
    public String getGameStateString() {
        if (players.isEmpty()) {
            return "/gameState|0|1|NONE|15";
        }

        String currentPlayer = players.get(currentTurnIndex);
        return String.format("/gameState|%d|%d|%s|%d",
                currentTurnIndex, currentRound, currentPlayer, remainingSeconds);
    }

    // ===== 투표 관련 메서드 =====

    /**
     * 투표 단계 시작
     */
    private void startVotingPhase() {
        this.isVotingPhase = true;
        this.votes.clear();
    }

    /**
     * 플레이어의 투표 추가
     * @return 모든 플레이어가 투표를 완료했는지 여부
     */
    public boolean addVote(String voterName, String votedPlayer) {
        if (!isVotingPhase) {
            return false;
        }

        // 자기 자신에게 투표 불가
        if (voterName.equals(votedPlayer)) {
            return false;
        }

        // 존재하지 않는 플레이어에게 투표 불가
        if (!players.contains(votedPlayer)) {
            return false;
        }

        votes.put(voterName, votedPlayer);

        // 모든 플레이어가 투표했는지 확인
        if (votes.size() == players.size()) {
            processVoteResult();
            return true;
        }

        return false;
    }

    /**
     * 투표 결과 집계
     */
    private void processVoteResult() {
        // 득표수 집계
        Map<String, Integer> voteCount = new HashMap<>();
        for (String player : players) {
            voteCount.put(player, 0);
        }

        for (String votedPlayer : votes.values()) {
            voteCount.put(votedPlayer, voteCount.get(votedPlayer) + 1);
        }

        // 최다 득표자 찾기
        String maxVotedPlayer = "";
        int maxVotes = 0;

        for (Map.Entry<String, Integer> entry : voteCount.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                maxVotedPlayer = entry.getKey();
            }
        }

        // 리스너에게 결과 전달
        if (eventListener != null) {
            eventListener.onVoteComplete(maxVotedPlayer, voteCount);
        }

        // 투표 단계 종료
        isVotingPhase = false;
    }

    /**
     * 현재 투표 진행 상황 반환
     */
    public String getVoteStatus() {
        return votes.size() + "/" + players.size();
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
    public boolean isVotingPhase() { return isVotingPhase; }
    public int getRemainingSeconds() { return remainingSeconds; }
    public String getCurrentPlayer() {
        if (players.isEmpty()) return "";
        return players.get(currentTurnIndex);
    }
}