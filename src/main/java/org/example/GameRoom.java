package org.example;

import java.util.ArrayList;
import java.util.List;

/*
 * 게임 방 정보를 담는 클래스
 * 수정사항: 방장 퇴장 시 다음 인덱스 플레이어에게 방장 권한을 위임하는 로직을 확인 및 강화.
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

    // 플레이어 추가
    public boolean addPlayer(String playerName) {
        if (players.contains(playerName)) {
            return true;
        }

        if (players.size() < maxPlayers) {
            players.add(playerName);
            currentPlayers = players.size();
            return true;
        }
        return false;
    }

    /**
     * 플레이어를 제거하고 방장이 퇴장했을 경우 다음 플레이어에게 권한을 위임합니다.
     * @param playerName 제거할 플레이어 이름
     * @return 성공 여부
     */
    public boolean removePlayer(String playerName) {
        boolean wasHost = playerName.equals(hostName);

        if (players.remove(playerName)) {
            currentPlayers = players.size();

            // 1. 나간 플레이어가 방장이었고 (wasHost == true)
            // 2. 방에 다른 플레이어가 남아 있다면 (!players.isEmpty())
            if (wasHost && !players.isEmpty()) {
                // 남아있는 플레이어 목록의 첫 번째 사람(다음 인덱스)에게 방장 권한 위임
                hostName = players.get(0);
            }
            // 3. 방에 아무도 없다면 (players.isEmpty()), hostName은 마지막으로 나간 사람 이름으로 유지되지만,
            // 이 경우 서버 측에서 해당 방을 삭제해야 합니다. (클라이언트 로직에서는 방장 위임만 처리)

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

        String protocolHostName = parts[2];
        GameRoom room = new GameRoom(parts[0], parts[1], protocolHostName, parts[5], Integer.parseInt(parts[6]));
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

        // 복원된 플레이어 목록이 비어있지 않고, 서버가 보낸 호스트 이름(protocolHostName)이
        // 현재 목록의 첫 번째 플레이어와 다를 경우, 서버 정보를 신뢰하여 갱신합니다.
        if (!room.players.isEmpty()) {
            // 호스트 이름은 서버가 정해주는 것이므로, 프로토콜 문자열의 호스트 이름을 따릅니다.
            room.hostName = protocolHostName;

            // [안전 장치] 만약 프로토콜의 호스트 이름이 복원된 players 목록의 첫 번째 사람이 아니라면,
            // players.get(0)이 새로운 호스트가 되도록 로직을 추가합니다. (이것이 방장 위임의 결과여야 함)
            if (!room.players.get(0).equals(room.hostName)) {
                room.hostName = room.players.get(0);
            }
        } else {
            // 방이 비어있다면, hostName은 마지막 호스트 이름으로 유지됩니다.
            room.hostName = protocolHostName;
        }

        return room;
    }
}