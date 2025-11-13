package org.example;

import java.util.ArrayList;
import java.util.List;

/**
 * 게임 방 정보를 담는 클래스
 */
public class GameRoom {
    private String roomId;           // 방 고유 ID
    private String roomName;         // 방 이름
    private String hostName;         // 방장 이름
    private int currentPlayers;      // 현재 인원
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

    // 플레이어 추가
// GameRoom.java 내부의 addPlayer 메서드 수정

    // 플레이어 추가
    public boolean addPlayer(String playerName) {
        // [수정] 이미 있는 플레이어라면 추가하지 않고 true 반환 (또는 무시)
        if (players.contains(playerName)) {
            return true;
        }

        if (currentPlayers < maxPlayers && status == RoomStatus.WAITING) {
            players.add(playerName);
            currentPlayers++;
            return true;
        }
        return false;
    }

    // 플레이어 제거
    public boolean removePlayer(String playerName) {
        if (players.remove(playerName)) {
            currentPlayers--;
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
        return currentPlayers >= maxPlayers;
    }

    // 게임 시작 가능한지 확인
    public boolean canStartGame() {
        return currentPlayers == maxPlayers && status == RoomStatus.WAITING;
    }

    // Getters and Setters
    public String getRoomId() { return roomId; }
    public String getRoomName() { return roomName; }
    public String getHostName() { return hostName; }
    public int getCurrentPlayers() { return currentPlayers; }
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
        room.currentPlayers = Integer.parseInt(parts[3]);
        room.status = RoomStatus.valueOf(parts[7]);

        // 플레이어 목록 복원
        room.players.clear();
        if (!parts[8].isEmpty()) {
            String[] playerNames = parts[8].split(",");
            for (String name : playerNames) {
                room.players.add(name);
            }
        }

        return room;
    }
}