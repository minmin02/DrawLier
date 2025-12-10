package org.example;

import javax.swing.Timer;
import java.util.*;

/**
 * 서버 측에서 개별 게임 방의 상태와 로직을 관리하는 클래스.
 * - 플레이어 목록, 게임 진행 상태(턴, 라운드), 역할(라이어) 등을 관리.
 * - 턴 기반 타이머를 통해 게임을 진행.
 */
public class GameRoom {
    //방 정보
    private String roomId;
    private String roomName;
    private String hostName;
    private String category;
    private int maxPlayers;

    //게임 진행 관련 필드
    private List<String> players; //현재 방에 있는 플레이어 목록
    private int currentTurnIndex; //현재 턴인 플레이어의 인덱스
    private int currentRound; //현재 라운드
    private boolean isGameRunning; //게임이 진행 중인지 여부
    private Timer turnTimer; //턴 시간을 재는 타이머
    private GameEventListener eventListener; //게임 이벤트(턴 변경, 종료 등)를 서버에 알리는 리스너

    //게임 역할 관련 필드
    private String selectedCategory; //현재 게임의 카테고리
    private String selectedKeyword; //시민에게 주어진 키워드
    private String liarName; //라이어의 이름
    private Map<String, Boolean> playerRoles; //Key: 플레이어 이름, Value: 라이어 여부

    //투표 관련 필드
    private Map<String, String> votes; //Key: 투표자, Value: 피투표자
    private boolean votingPhase = false; //투표 단계인지 여부

    //게임 설정 (상수)
    private static final int TURN_TIME_SECONDS = 15; //각 턴의 시간 (초)
    private static final int MAX_ROUNDS = 1; //최대 라운드 수
    private static final int REQUIRED_PLAYERS = 4; //게임 시작에 필요한 인원

    private int remainingSeconds; //현재 턴의 남은 시간

    //서버(DrawServer)에서 게임 이벤트를 처리하기 위한 인터페이스
    public interface GameEventListener {
        void onTurnChanged(int turnIndex, int round, String currentPlayer); //턴이 변경될 때 호출
        void onGameEnded(); //게임이 종료(투표 단계로 전환)될 때 호출
        void onTimerTick(int remainingSeconds); //1초마다 타이머가 호출
    }

    //생성자
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

    //방에 플레이어를 추가하는 메소드
    public void addPlayer(String playerName) {
        if (!players.contains(playerName) && players.size() < maxPlayers) {
            players.add(playerName);
        }
    }

    //방에서 플레이어를 제거하는 메소드
    public void removePlayer(String playerName) {
        players.remove(playerName);
    }

    //플레이어 목록을 통째로 갱신하는 메소드
    public void updatePlayers(List<String> newPlayers) {
        this.players.clear();
        this.players.addAll(newPlayers);
    }

    //게임 시작에 필요한 인원인지 확인하는 메소드
    public boolean canStartGame() {
        return players.size() == REQUIRED_PLAYERS && !isGameRunning;
    }

    //게임을 시작하는 메소드
    public void startGameWithCategory(String selectedCategory, GameEventListener listener) {
        if (!canStartGame()) return;

        this.selectedCategory = selectedCategory;

        //1. 카테고리에 맞는 랜덤 키워드 선택
        try {
            GameCategory gameCategory = GameCategory.valueOf(selectedCategory);
            this.selectedKeyword = gameCategory.getRandomKeyword();
        } catch (IllegalArgumentException e) {
            System.err.println("유효하지 않은 카테고리: " + selectedCategory);
            return;
        }

        //2. 플레이어 중 한 명을 라이어로 랜덤 선택
        Random random = new Random();
        int liarIndex = random.nextInt(players.size());
        this.liarName = players.get(liarIndex);

        //3. 각 플레이어에게 역할(시민/라이어) 할당
        this.playerRoles = new HashMap<>();
        for (int i = 0; i < players.size(); i++) {
            String player = players.get(i);
            boolean isLiar = (i == liarIndex);
            playerRoles.put(player, isLiar);
        }

        //4. 게임 상태 초기화 및 시작
        this.eventListener = listener;
        this.isGameRunning = true;
        this.currentTurnIndex = 0;
        this.currentRound = 1;
        this.remainingSeconds = TURN_TIME_SECONDS;

        startTurnTimer(); //첫 턴 타이머 시작
        notifyTurnChange(); //첫 턴 정보 전송
    }

    //턴 타이머를 시작하는 메소드
    private void startTurnTimer() {
        if (turnTimer != null) {
            turnTimer.stop();
        }
        remainingSeconds = TURN_TIME_SECONDS;
        turnTimer = new Timer(1000, e -> { //1초마다 실행
            remainingSeconds--;
            if (eventListener != null) {
                eventListener.onTimerTick(remainingSeconds); //남은 시간 정보 전송
            }
            if (remainingSeconds <= 0) {
                nextTurn(); //시간이 다 되면 다음 턴으로
            }
        });
        turnTimer.start();
    }

    //다음 턴으로 넘기는 메소드
    private void nextTurn() {
        if (turnTimer != null) {
            turnTimer.stop();
        }
        currentTurnIndex++;

        //모든 플레이어가 한 번씩 턴을 가졌으면 라운드 증가
        if (currentTurnIndex >= players.size()) {
            currentTurnIndex = 0;
            currentRound++;
            //최대 라운드에 도달하면 게임 종료
            if (currentRound > MAX_ROUNDS) {
                endGame();
                return;
            }
        }

        startTurnTimer(); //새 턴 타이머 시작
        notifyTurnChange(); //턴 변경 정보 전송
    }

    //턴 변경 정보를 리스너(서버)에 알리는 메소드
    private void notifyTurnChange() {
        if (eventListener != null && !players.isEmpty()) {
            String currentPlayer = players.get(currentTurnIndex);
            eventListener.onTurnChanged(currentTurnIndex, currentRound, currentPlayer);
        }
    }

    //게임을 종료하고 투표 단계로 전환하는 메소드
    private void endGame() {
        isGameRunning = false;
        votingPhase = true; //투표 단계 시작
        if (turnTimer != null) {
            turnTimer.stop();
            turnTimer = null;
        }
        if (eventListener != null) {
            eventListener.onGameEnded(); //게임 종료 이벤트 전송
        }
    }

    //투표를 기록하는 메소드
    public void addVote(String voter, String votedPlayer) {
        if (votingPhase && players.contains(voter) && players.contains(votedPlayer)) {
            votes.put(voter, votedPlayer);
        }
    }

    //모든 플레이어가 투표했는지 확인하는 메소드
    public boolean hasAllVoted() {
        return votes.size() == players.size();
    }

    //가장 많이 득표한 플레이어를 찾는 메소드
    public String getMostVotedPlayer() {
        if (votes.isEmpty()) return null;

        Map<String, Integer> voteCount = new HashMap<>();
        for (String votedPlayer : votes.values()) {
            voteCount.put(votedPlayer, voteCount.getOrDefault(votedPlayer, 0) + 1);
        }

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

    //최다 득표자가 라이어인지 확인하는 메소드
    public boolean isMostVotedLiar() {
        String mostVoted = getMostVotedPlayer();
        return mostVoted != null && mostVoted.equals(liarName);
    }

    //라이어가 제출한 정답이 맞는지 확인하는 메소드
    public boolean checkLiarAnswer(String answer) {
        if (answer == null || selectedKeyword == null) return false;
        //대소문자 구분 없이, 공백 제거 후 비교
        return answer.trim().equalsIgnoreCase(selectedKeyword.trim());
    }

    //현재 턴인 플레이어인지 확인하는 메소드
    public boolean isPlayerTurn(String playerName) {
        if (!isGameRunning || players.isEmpty()) return false;
        return players.get(currentTurnIndex).equals(playerName);
    }

    //해당 플레이어가 라이어인지 확인하는 메소드
    public boolean isLiar(String playerName) {
        return playerRoles.getOrDefault(playerName, false);
    }

    //플레이어에게 역할과 키워드 정보를 담은 프로토콜 문자열을 반환하는 메소드
    public String getPlayerInfo(String playerName) {
        if (isLiar(playerName)) {
            return "LIAR|" + selectedCategory; //라이어에게는 카테고리만
        } else {
            return "CITIZEN|" + selectedKeyword; //시민에게는 키워드
        }
    }

    //현재 게임 상태를 프로토콜 문자열로 반환하는 메소드
    public String getGameStateString() {
        if (players.isEmpty()) {
            return "/gameState|0|1|NONE|" + TURN_TIME_SECONDS;
        }
        String currentPlayer = players.get(currentTurnIndex);
        return String.format("/gameState|%d|%d|%s|%d",
                currentTurnIndex, currentRound, currentPlayer, remainingSeconds);
    }

    //--- Getters and Setters ---
    public String getRoomId() { return roomId; }
    public String getRoomName() { return roomName; }
    public String getHostName() { return hostName; }
    public String getCategory() { return category; }
    public int getMaxPlayers() { return maxPlayers; }
    public List<String> getPlayers() { return new ArrayList<>(players); }
    public boolean isGameRunning() { return isGameRunning; }
    public int getRemainingSeconds() { return remainingSeconds; }
    public String getLiarName() { return liarName; }
    public String getSelectedKeyword() { return selectedKeyword; }
    public boolean isVotingPhase() { return votingPhase; }
    
    //방장 변경을 위한 Setter
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }
}
