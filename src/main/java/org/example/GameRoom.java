package org.example;

import java.util.ArrayList;
import java.util.List;

/**
 * 게임 방 정보를 담는 클래스
 * 수정사항: addPlayer 메서드에서 인원수 체크 로직을 players.size() 기준으로 변경하여 동기화 문제 해결
 */
public class GameRoom {
    private String roomId;           // 방 고유 ID
    private String roomName;         // 방 이름
    private String hostName;         // 방장 이름
    private int currentPlayers;      // 현재 인원 (서버 정보용)
    private int maxPlayers;          // 최대 인원 (기본 4명)
    private String category;         // 카테고리
    private int timeLimit;           // 제한 시간 (초)
    private RoomStatus status;       // 방 상태
    private List<String> players;    // 참여 플레이어 목록

    public enum RoomStatus {
        WAITING,    // 대기 중
        PLAYING,    // 게임 중
        FINISHED    // 게임 종료
    }

    public GameRoom(String roomId, String roomName, String hostName, String category, int timeLimit) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.hostName = hostName;
        this.category = category;
        this.timeLimit = timeLimit;
        this.currentPlayers = 1;
        this.maxPlayers = 4;
        this.status = RoomStatus.WAITING;
        this.players = new ArrayList<>();
        this.players.add(hostName);
    }

    // [핵심 수정] 플레이어 추가 로직 변경
    public boolean addPlayer(String playerName) {
        // 이미 있는 플레이어라면 성공으로 처리
        if (players.contains(playerName)) {
            return true;
        }

        // [수정] currentPlayers(숫자) 대신 players.size()(실제 리스트)를 기준으로 판단
        // 이렇게 해야 서버에서 숫자만 4로 오고 명단이 비어있을 때도 정상적으로 추가됨
        if (players.size() < maxPlayers) {
            players.add(playerName);
            currentPlayers = players.size(); // 숫자도 실제 크기에 맞춰 갱신
            return true;
        }
        return false;
    }

    // 플레이어 제거
    public boolean removePlayer(String playerName) {
        if (players.remove(playerName)) {
            currentPlayers = players.size(); // 리스트 크기에 맞춰 갱신

            // 방장이 나가면 다음 사람을 방장으로
            if (playerName.equals(hostName) && !players.isEmpty()) {
                hostName = players.get(0);
            }
            return true;
        }
        return false;
    }

    // 방이 가득 찼는지 확인
    public boolean isFull() {
        return players.size() >= maxPlayers;
    }

    // 게임 시작 가능한지 확인
    public boolean canStartGame() {
        return players.size() == maxPlayers && status == RoomStatus.WAITING;
    }

    // Getters and Setters
    public String getRoomId() { return roomId; }
    public String getRoomName() { return roomName; }
    public String getHostName() { return hostName; }
    public int getCurrentPlayers() { return currentPlayers; } // 단순히 표시용으로 사용
    public int getMaxPlayers() { return maxPlayers; }
    public String getCategory() { return category; }
    public int getTimeLimit() { return timeLimit; }
    public RoomStatus getStatus() { return status; }
    public void setStatus(RoomStatus status) { this.status = status; }
    public List<String> getPlayers() { return new ArrayList<>(players); }

    @Override
    public String toString() {
        return String.format("%s [%d/%d] - %s (%s)",
                roomName, currentPlayers, maxPlayers, category,
                status == RoomStatus.WAITING ? "대기중" : status == RoomStatus.PLAYING ? "게임중" : "종료");
    }

    // 방 정보를 프로토콜 문자열로 변환
    public String toProtocolString() {
        StringBuilder sb = new StringBuilder();
        sb.append(roomId).append("|");
        sb.append(roomName).append("|");
        sb.append(hostName).append("|");
        sb.append(currentPlayers).append("|");
        sb.append(maxPlayers).append("|");
        sb.append(category).append("|");
        sb.append(timeLimit).append("|");
        sb.append(status.name()).append("|");
        sb.append(String.join(",", players));
        return sb.toString();
    }

    // 프로토콜 문자열에서 방 정보 복원
    public static GameRoom fromProtocolString(String protocol) {
        String[] parts = protocol.split("\\|");
        if (parts.length < 9) return null;

        GameRoom room = new GameRoom(parts[0], parts[1], parts[2], parts[5], Integer.parseInt(parts[6]));
        room.currentPlayers = Integer.parseInt(parts[3]); // 서버에서 온 숫자 적용
        room.status = RoomStatus.valueOf(parts[7]);

        // 플레이어 목록 복원
        room.players.clear();
        if (!parts[8].isEmpty()) {
            String[] playerNames = parts[8].split(",");
            for (String name : playerNames) {
                room.players.add(name);
            }
        }

        // [안전장치] 만약 복원된 리스트 크기가 currentPlayers보다 작다면 리스트 크기를 우선하지 않더라도
        // addPlayer 메서드에서 처리가 가능하도록 위에서 수정함.

        return room;
    }
}