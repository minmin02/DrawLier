# DrawLier 프로젝트 문서

## 프로젝트 개요
DrawLier는 "라이어 게임"을 기반으로 한 멀티플레이어 그림 그리기 게임입니다. 4명의 플레이어가 참가하여, 제시된 키워드를 그림으로 표현하면서 라이어를 찾아내는 추리 게임입니다.

---

## 1. UI/UX 스토리보드 (화면별 게임 흐름)

### 1.1 전체 게임 흐름도

```
[로그인] → [방 목록] → [게임 대기실] → [게임 시작] → [투표] → [결과]
   ↓          ↓            ↓              ↓          ↓        ↓
  입장     방 생성/참가   4명 대기     그림 그리기   라이어 찾기  승패 확인
```

### 1.2 화면별 상세 흐름

#### 1.2.1 로그인 화면 (LiarGameClientLoginUI)
**목적**: 서버 접속 및 사용자 인증

**화면 구성**:
- 배경 이미지: `loginBackground.png`
- 입력 필드:
  - 서버 IP 주소 (기본값: 127.0.0.1)
  - 닉네임
  - 포트 번호 (기본값: 30000)
- 시작하기 버튼

**사용자 흐름**:
1. 사용자가 서버 정보와 닉네임 입력
2. "시작하기" 버튼 클릭
3. 서버 연결 시도
4. 성공 시 → 방 목록 화면으로 이동
5. 실패 시 → 오류 메시지 표시

---

#### 1.2.2 방 목록 화면 (RoomListUI)
**목적**: 게임 방 조회 및 생성/참가

**화면 구성**:
- 배경 이미지: `RoomListBackGround.png`
- 상단: 게임 방 목록 제목, 접속자 이름
- 중앙: 방 목록 테이블
  - 컬럼: 방 이름, 방장, 인원, 카테고리, 상태
- 하단: 버튼 3개
  - 새로고침 버튼
  - 방 참가 버튼
  - 방 만들기 버튼

**사용자 흐름**:

**A. 방 만들기**
1. "방 만들기" 버튼 클릭
2. 다이얼로그 팝업 (배경: `createRoom.png`)
   - 방 이름 입력
   - 카테고리 선택 (직업/동물/음식/영화/스포츠)
3. "생성" 버튼 클릭
4. 서버에 방 생성 요청 → 게임 대기실로 이동

**B. 방 참가**
1. 목록에서 방 선택
2. "방 참가" 버튼 클릭
3. 검증:
   - 방이 가득 찼는지 확인 (4/4)
   - 게임이 이미 시작되었는지 확인
4. 통과 시 → 게임 대기실로 이동

---

#### 1.2.3 게임 대기실 & 플레이 화면 (JavaChatClientView)
**목적**: 게임 진행 및 그림 그리기

**화면 구성**:
- 배경 이미지: `back.png`
- 상단 패널:
  - 좌측: 방 이름, 카테고리, 방장 정보
  - 우측: 현재 턴 정보, 남은 시간
- 중앙 분할 패널:
  - 좌측 (60%): 그림 그리기 패널
    - 도구 모음: 나가기, 굵기 선택, 색상 선택, 지우개, 전체 지우기
  - 우측 (40%): 채팅 패널
    - 채팅 메시지 영역
    - 이모티콘 버튼
    - 텍스트 입력 필드
- 우측 사이드바: 플레이어 목록 (4명)
  - 참가자: 플레이어 이미지 (`player.png`)
  - 대기 슬롯: 대기 이미지 (`waiting.png`)
  - 방장 표시: 👑 아이콘
- 하단: 게임 시작 버튼 (방장만 활성화)

**게임 대기 중 흐름**:
1. 플레이어들이 방에 입장
2. 인원이 4명이 될 때까지 대기
3. 방장이 "게임 시작" 버튼 클릭
4. 모든 플레이어에게 역할 배정

**역할 배정 다이얼로그**:
- **라이어 (1명)**:
  - 배경: `backGround.png`
  - 메시지: "당신은 라이어입니다! 카테고리: [카테고리명]"
  - 확인 버튼: `check.png`

- **시민 (3명)**:
  - 배경: `backGround2.png`
  - 메시지: "당신은 시민입니다! 키워드: [키워드]"
  - 확인 버튼: `check2.png`

**게임 진행 흐름** (턴 기반):
1. 라운드 1/4 시작
2. 첫 번째 플레이어의 턴 (15초)
   - 해당 플레이어만 그림 그리기 가능
   - 다른 플레이어는 관전 모드
   - 타이머 표시: "남은 시간: XX초"
3. 턴 종료 시 자동으로 다음 플레이어로 이동
4. 4명의 플레이어가 모두 그리면 라운드 완료
5. 총 4라운드 진행
6. 모든 라운드 종료 → 투표 화면으로 이동

**그림 그리기 기능**:
- 색상 선택: 컬러 피커
- 굵기 선택: 1, 2, 4, 8, 12 픽셀
- 지우개: 흰색으로 그리기
- 전체 지우기: 캔버스 초기화
- 실시간 동기화: 모든 플레이어가 같은 그림 공유

**채팅 기능**:
- 텍스트 채팅
- 이모티콘 18종 지원
- 시스템 메시지 (입장/퇴장/방장 변경)

---

#### 1.2.4 투표 화면 (VotingUI)
**목적**: 라이어로 의심되는 플레이어 투표

**화면 구성**:
- 배경 이미지: `Voting.png`
- 2x2 그리드 레이아웃
- 각 플레이어마다 버튼 생성
  - 아이콘: 👤
  - 플레이어 이름
  - 자기 자신: "(나)" 표시 및 비활성화
- 하단: "투표하기" 버튼 (`VoteButton.png`)

**사용자 흐름**:
1. 4개의 플레이어 버튼 표시
2. 자신을 제외한 플레이어 선택 가능
3. 플레이어 선택 시 버튼 하이라이트
4. "투표하기" 버튼 클릭
5. 서버에 투표 전송
6. 모든 플레이어의 투표 완료 대기

**투표 결과 처리**:
- 최다 득표자가 라이어인 경우:
  - 최다 득표자(라이어) → 정답 입력 화면
  - 나머지 플레이어 → 대기 화면 (`WaitingUI.png`)
- 최다 득표자가 라이어가 아닌 경우:
  - 모든 플레이어 → 결과 화면 (라이어 승리)

---

#### 1.2.5 정답 입력 화면 (InsertAnswerUI)
**목적**: 라이어의 마지막 기회 - 키워드 맞추기

**화면 구성**:
- 배경 이미지: `InsertAnswer.png`
- 제목: "마지막 기회! 키워드를 맞춰보세요!"
- 중앙: 텍스트 입력 필드
  - 큰 글씨, 중앙 정렬
  - 빨간색 테두리 강조
- 하단: "정답 제출" 버튼 (`Submit.png`)

**사용자 흐름**:
1. 라이어가 키워드 입력
2. "정답 제출" 버튼 클릭 또는 Enter
3. 서버에 정답 전송
4. 제출 완료 알림 표시
5. 최종 결과 대기
6. 결과 화면으로 이동

**정답 검증**:
- 정답 맞춤 → 라이어 승리
- 정답 틀림 → 시민 승리

---

#### 1.2.6 결과 화면 (ResultUI)
**목적**: 게임 결과 표시 및 로비 복귀

**화면 구성**:
- 배경 이미지 (승패에 따라):
  - 시민 승리: `CitizenWin.png`
  - 라이어 승리: `LiarWin.png`
- 중앙: 결과 상세 메시지
  - 투표 결과
  - 라이어 이름
  - 키워드 정보
- 하단: "로비로 이동" 버튼 (`GoToLobby.png`)

**표시되는 결과 메시지 예시**:

**A. 시민 승리 케이스**:
- 투표로 라이어를 찾고, 라이어가 정답을 틀린 경우:
  ```
  라이어: [이름]
  라이어가 정답을 맞추지 못했습니다!
  정답: [키워드]
  ```

**B. 라이어 승리 케이스**:
- 투표에서 라이어가 아닌 플레이어가 선출된 경우:
  ```
  [이름]님이 억울하게 투표되었습니다!
  실제 라이어는 다른 플레이어였습니다.
  ```
- 라이어가 정답을 맞춘 경우:
  ```
  라이어: [이름]
  라이어가 정답을 맞췄습니다!
  정답: [키워드]
  ```

**사용자 흐름**:
1. 결과 확인
2. "로비로 이동" 버튼 클릭
3. 방 목록 화면으로 복귀
4. 기존 연결 유지 (재로그인 불필요)

---

## 2. 소스파일 구성 및 목록 (실행 흐름 기반)

### 2.1 프로젝트 구조

```
DrawLier-main/
├── src/
│   └── main/
│       ├── java/org/example/          # Java 소스 파일
│       │   ├── LiarGameClientLoginUI.java
│       │   ├── RoomListUI.java
│       │   ├── JavaChatClientView.java
│       │   ├── DrawingPanel.java
│       │   ├── ChatPanel.java
│       │   ├── VotingUI.java
│       │   ├── InsertAnswerUI.java
│       │   ├── ResultUI.java
│       │   ├── DrawServer.java
│       │   ├── GameRoom.java
│       │   ├── GameCategory.java
│       │   ├── BackgroundPanel.java
│       │   ├── UIComponents.java
│       │   └── UIUtils.java
│       │
│       └── resources/                  # 리소스 파일
│           ├── loginUI/                # 로그인 화면 이미지
│           ├── RoomList/               # 방 목록 화면 이미지
│           ├── game/                   # 게임 화면 이미지
│           ├── PlayUI/                 # 게임 진행 UI 이미지
│           ├── info/                   # 역할 안내 이미지
│           ├── ResultTabUI/            # 결과 화면 이미지
│           └── Imoji/                  # 이모티콘 이미지 (18개)
│
├── pom.xml                             # Maven 빌드 설정
└── DrawLier_프로젝트_문서.md           # 프로젝트 문서 (본 문서)
```

---

### 2.2 실행 흐름에 따른 소스파일 설명

#### 단계 1: 프로그램 시작 (로그인)

##### 1.1 LiarGameClientLoginUI.java
**위치**: `src/main/java/org/example/LiarGameClientLoginUI.java`

**주요 기능**:
- 애플리케이션의 진입점 (main 메서드 포함)
- 사용자로부터 서버 IP, 닉네임, 포트 정보 입력 받기
- 서버 연결 초기화
- 입력 검증 및 오류 처리

**주요 클래스/컴포넌트**:
- `LiarGameClientLoginUI`: 메인 프레임 클래스
- `RoundBorder`: 둥근 모서리 텍스트 필드 테두리 클래스
- `PlaceholderTextField`: 플레이스홀더 기능이 있는 커스텀 텍스트 필드
- `Myaction`: 버튼 클릭 이벤트 처리 클래스

**핵심 코드 흐름**:
```java
main()
  → LiarGameClientLoginUI 생성자
  → initializeUI() (UI 초기화)
  → btnEnterGame 클릭
  → Myaction.actionPerformed()
  → 입력 검증
  → RoomListUI 생성 및 화면 전환
```

**사용 리소스**:
- `/loginBackground.png`: 배경 이미지
- `/loginUI/ServerIp.png`: 서버 IP 레이블
- `/loginUI/NickName.png`: 닉네임 레이블
- `/loginUI/Port.png`: 포트 레이블
- `/loginUI/StartButton.png`: 시작 버튼

---

##### 1.2 BackgroundPanel.java
**위치**: `src/main/java/org/example/BackgroundPanel.java`

**주요 기능**:
- 배경 이미지를 표시하는 커스텀 JPanel
- 이미지 로딩 및 자동 크기 조정
- 재사용 가능한 배경 컴포넌트

**사용 위치**:
- `LiarGameClientLoginUI`: 로그인 화면 배경
- 기타 다양한 UI에서 배경 이미지 표시

---

#### 단계 2: 방 목록 및 방 생성/참가

##### 2.1 RoomListUI.java
**위치**: `src/main/java/org/example/RoomListUI.java`

**주요 기능**:
- 서버의 게임 방 목록 조회 및 표시
- 새로운 게임 방 생성
- 기존 게임 방 참가
- 실시간 방 목록 업데이트
- 플레이어 입/퇴장 알림 처리

**주요 클래스/컴포넌트**:
- `RoomListUI`: 메인 프레임 클래스
- `ListenNetwork`: 서버 메시지 수신 스레드
- `roomMap`: 방 ID와 GameRoom 객체 매핑
- `tableModel`: 방 목록 테이블 데이터 모델

**핵심 코드 흐름**:

**A. 방 목록 조회**:
```java
connectToServer()
  → 소켓 연결 및 로그인
  → ListenNetwork 스레드 시작
  → requestRoomList() 호출
  → 서버로부터 "/roomList" 응답
  → updateRoomList() 실행
  → 테이블에 방 정보 표시
```

**B. 방 생성**:
```java
btnCreateRoom 클릭
  → openCreateRoomDialog() 호출
  → 다이얼로그에서 방 이름, 카테고리 입력
  → createRoom() 호출
  → GameRoom 객체 생성
  → 서버에 "/createRoom [방정보]" 전송
  → "/roomCreated" 응답 대기
  → JavaChatClientView로 화면 전환 (isHost=true)
```

**C. 방 참가**:
```java
방 선택 후 btnJoinRoom 클릭
  → joinSelectedRoom() 호출
  → 참가 가능 여부 검증
  → 서버에 "/joinRoom [방ID]" 전송
  → "/joinedRoom" 응답 대기
  → JavaChatClientView로 화면 전환 (isHost=false)
```

**네트워크 프로토콜**:
- `/login [닉네임]`: 로그인 요청
- `/getRoomList`: 방 목록 요청
- `/createRoom [방정보]`: 방 생성 요청
- `/joinRoom [방ID]`: 방 참가 요청
- `/roomList [방목록]`: 방 목록 응답
- `/roomCreated [방ID]`: 방 생성 완료
- `/joinedRoom [방ID|플레이어목록]`: 방 참가 완료

**사용 리소스**:
- `/RoomList/RoomListBackGround.png`: 배경
- `/RoomList/GameListTitle.png`: 제목
- `/RoomList/Reroad.png`: 새로고침 버튼
- `/RoomList/EnterRoomBtn.png`: 방 참가 버튼
- `/RoomList/CreateRoomBtn.png`: 방 만들기 버튼
- `/RoomList/createRoom.png`: 방 생성 다이얼로그 배경
- `/RoomList/RoomName.png`: 방 이름 레이블
- `/RoomList/category.png`: 카테고리 레이블
- `/RoomList/createBtn.png`: 생성 버튼
- `/RoomList/cancelBtn.png`: 취소 버튼

---

##### 2.2 GameCategory.java
**위치**: `src/main/java/org/example/GameCategory.java`

**주요 기능**:
- 게임 카테고리 및 키워드 관리
- 카테고리별 키워드 목록 저장
- 랜덤 키워드 선택

**카테고리 및 키워드**:
```java
직업: 경찰, 소방관, 의사, 교사, 프로그래머, 요리사, 변호사, 간호사
동물: 사자, 호랑이, 코끼리, 기린, 펭귄, 돌고래, 강아지, 고양이
음식: 피자, 치킨, 햄버거, 초밥, 파스타, 라면, 김치찌개, 삼겹살
영화: 어벤져스, 타이타닉, 겨울왕국, 기생충, 해리포터, 스타워즈, 쥬라기공원, 인터스텔라
스포츠: 축구, 야구, 농구, 배구, 테니스, 수영, 골프, 배드민턴
```

**주요 메서드**:
- `getRandomKeyword()`: 카테고리에서 랜덤 키워드 반환
- `getCategoryNames()`: 모든 카테고리 이름 배열 반환

---

#### 단계 3: 게임 대기 및 플레이

##### 3.1 JavaChatClientView.java
**위치**: `src/main/java/org/example/JavaChatClientView.java`

**주요 기능**:
- 게임 대기실 및 플레이 화면 통합
- 턴 기반 그림 그리기 제어
- 실시간 채팅 및 이모티콘
- 플레이어 목록 관리
- 게임 상태 동기화
- 역할 배정 안내

**주요 클래스/컴포넌트**:
- `JavaChatClientView`: 메인 게임 화면 클래스
- `DrawingPanel`: 그림 그리기 패널 (별도 클래스)
- `ChatPanel`: 채팅 패널 (별도 클래스)
- `ListenNetwork`: 서버 메시지 수신 스레드

**핵심 코드 흐름**:

**A. 초기화 및 대기**:
```java
생성자(userName, socket, dis, dos, room, isHost)
  → loadEmojis() (이모티콘 로딩)
  → initializeUI() (UI 구성)
  → ListenNetwork 스레드 시작
  → updatePlayerList() (플레이어 목록 표시)
  → updateStartButtonState() (시작 버튼 상태 업데이트)
```

**B. 게임 시작**:
```java
btnStartGame 클릭 (방장만 가능)
  → startGame() 호출
  → 4명 검증
  → "/gameStart [카테고리]" 전송
  → 서버가 역할 배정
  → "/gameStart LIAR|[카테고리]" 또는 "CITIZEN|[키워드]" 수신
  → 역할 안내 다이얼로그 표시
  → 게임 시작
```

**C. 턴 진행**:
```java
서버로부터 "/gameState|턴인덱스|라운드|현재플레이어|남은시간" 수신
  → updateTurnInfo() 호출
  → 현재 턴인 플레이어 UI 업데이트
  → 내 턴이면 그리기/채팅 활성화
  → 내 턴이 아니면 비활성화
  → 타이머 표시 업데이트
```

**D. 그림 그리기**:
```java
사용자가 그림 그리기
  → DrawingPanel.onDrawEvent() 콜백 호출
  → "/draw x1,y1,x2,y2,color,width" 프로토콜 전송
  → 서버가 모든 클라이언트에게 브로드캐스트
  → 다른 플레이어 화면에 실시간 반영
```

**E. 게임 종료**:
```java
모든 라운드 완료
  → 서버가 "/gameEnded" 전송
  → openVotingUI() 호출
  → VotingUI로 화면 전환
```

**주요 메서드**:
- `initializeUI()`: UI 컴포넌트 초기화
- `createToolPanel()`: 그리기 도구 패널 생성
- `createPlayerPanel()`: 플레이어 목록 패널 생성
- `updateTurnInfo()`: 턴 정보 업데이트 및 권한 제어
- `updatePlayerList()`: 플레이어 목록 갱신
- `startGame()`: 게임 시작 요청
- `leaveRoom()`: 방 나가기 및 로비 복귀

**네트워크 프로토콜**:
- `/gameStart [카테고리]`: 게임 시작 요청
- `/gameStart [역할]|[정보]`: 역할 배정 응답
- `/gameState|턴|라운드|플레이어|시간`: 게임 상태 동기화
- `/draw [좌표 및 속성]`: 그림 그리기 명령
- `/clear`: 캔버스 전체 지우기
- `/playerJoined [이름]`: 플레이어 입장
- `/playerLeft [이름]`: 플레이어 퇴장
- `/hostChanged [이름]`: 방장 변경
- `/gameEnded`: 게임 종료
- `/leaveRoom`: 방 나가기

**사용 리소스**:
- `/game/back.png`: 배경
- `/game/exit.png`: 나가기 버튼
- `/game/size.png`: 굵기 레이블
- `/game/color.png`: 색상 선택 버튼
- `/game/erase.png`: 지우개 버튼
- `/game/allerase.png`: 전체 지우기 버튼
- `/game/category.png`: 카테고리 아이콘
- `/game/host.png`: 방장 아이콘
- `/game/player.png`: 플레이어 슬롯
- `/game/waiting.png`: 대기 슬롯
- `/info/backGround.png`: 라이어 역할 안내 배경
- `/info/check.png`: 라이어 확인 버튼
- `/info/backGround2.png`: 시민 역할 안내 배경
- `/info/check2.png`: 시민 확인 버튼

---

##### 3.2 DrawingPanel.java
**위치**: `src/main/java/org/example/DrawingPanel.java`

**주요 기능**:
- 마우스 입력으로 그림 그리기
- 그림 데이터를 버퍼에 저장
- 네트워크 동기화를 위한 그림 명령 전송
- 서버로부터 받은 그림 명령 처리
- 캔버스 지우기 기능

**주요 인터페이스**:
```java
public interface DrawingCallback {
    void sendProtocol(String msg);
    Color getCurrentColor();
    int getStrokeWidth();
    Color getDrawingBgColor();
}
```

**핵심 코드 흐름**:
```java
마우스 드래그
  → mouseDragged() 이벤트
  → 현재 색상과 굵기로 선 그리기
  → 버퍼에 그림 저장
  → callback.sendProtocol("/draw x1,y1,x2,y2,r,g,b,width") 호출
  → 화면 갱신
```

**네트워크 동기화**:
```java
서버로부터 "/draw x1,y1,x2,y2,r,g,b,width" 수신
  → processDrawCommand() 호출
  → 좌표 및 속성 파싱
  → 로컬 버퍼에 그림 그리기
  → 화면 갱신
```

**주요 메서드**:
- `paintComponent()`: 화면에 그림 렌더링
- `processDrawCommand()`: 네트워크 그림 명령 처리
- `clear()`: 캔버스 초기화
- `setEnabled()`: 그리기 활성화/비활성화

---

##### 3.3 ChatPanel.java
**위치**: `src/main/java/org/example/ChatPanel.java`

**주요 기능**:
- 채팅 메시지 송수신
- 이모티콘 선택 및 전송
- 시스템 메시지 표시
- 메시지 히스토리 관리

**주요 인터페이스**:
```java
public interface ChatCallback {
    void sendProtocol(String msg);
}
```

**핵심 코드 흐름**:

**A. 텍스트 채팅**:
```java
사용자가 메시지 입력 후 전송
  → sendMessage() 호출
  → "[닉네임]: 메시지" 형식으로 전송
  → callback.sendProtocol() 호출
  → 서버가 모든 클라이언트에게 브로드캐스트
  → appendChatMessage() 호출하여 화면에 표시
```

**B. 이모티콘 전송**:
```java
이모티콘 버튼 클릭
  → 이모티콘 선택 다이얼로그 표시
  → 이모티콘 이미지 버튼 클릭
  → "[닉네임]: [EMO:이모티콘명]" 형식으로 전송
  → 다른 클라이언트에서 이모티콘 이미지로 표시
```

**주요 메서드**:
- `appendChatMessage()`: 채팅 메시지 추가
- `appendSystemMessage()`: 시스템 메시지 추가
- `sendMessage()`: 메시지 전송
- `showEmojiDialog()`: 이모티콘 선택 창 표시

**사용 리소스**:
- `/Imoji/*.png`: 이모티콘 이미지 18개
  - cold_sweat.png, down.png, grin.png, hankey.png, joy.png, lying.png,
  - open_mouth.png, partying_face.png, rage.png, scream.png,
  - shushing_face.png, skull_and_crossbones.png, star-struck.png,
  - thinking.png, up.png, yawning_face.png, zany.png, zipper_mouth_face.png
- `/game/send.png`: 전송 버튼

---

#### 단계 4: 투표

##### 4.1 VotingUI.java
**위치**: `src/main/java/org/example/VotingUI.java`

**주요 기능**:
- 라이어로 의심되는 플레이어에게 투표
- 투표 결과 수신 및 처리
- 다음 단계로 화면 전환

**핵심 코드 흐름**:

**A. 투표 진행**:
```java
VotingUI 생성자
  → initializeUI() (4명의 플레이어 버튼 생성)
  → ListenVoteResult 스레드 시작
  → 사용자가 플레이어 선택
  → submitVote() 호출
  → "/vote [선택한플레이어]" 전송
  → 모든 플레이어의 투표 완료 대기
```

**B. 투표 결과 처리**:
```java
서버로부터 "/voteResult [최다득표자]|[라이어여부]" 수신
  → 최다 득표자가 라이어인 경우:
    - 최다 득표자 → InsertAnswerUI로 전환
    - 다른 플레이어 → 대기 화면 표시
  → 최다 득표자가 라이어가 아닌 경우:
    - 모든 플레이어 → ResultUI로 전환 (라이어 승리)
```

**네트워크 프로토콜**:
- `/vote [플레이어이름]`: 투표 전송
- `/voteResult [이름]|[true/false]`: 투표 결과
- `/finalResult [승자]|[라이어이름]|[메시지]`: 최종 결과

**주요 메서드**:
- `initializeUI()`: 투표 UI 초기화
- `selectPlayer()`: 플레이어 선택 처리
- `submitVote()`: 투표 전송
- `ListenVoteResult`: 투표 결과 수신 스레드

**사용 리소스**:
- `/PlayUI/Voting.png`: 배경
- `/PlayUI/VoteButton.png`: 투표 버튼
- `/PlayUI/WaitingUI.png`: 대기 화면

---

#### 단계 5: 라이어 정답 입력

##### 5.1 InsertAnswerUI.java
**위치**: `src/main/java/org/example/InsertAnswerUI.java`

**주요 기능**:
- 라이어가 키워드를 맞추는 마지막 기회 제공
- 정답 입력 및 전송
- 최종 결과 대기

**핵심 코드 흐름**:
```java
InsertAnswerUI 생성자
  → initializeUI() (입력 필드 및 제출 버튼 생성)
  → ListenResult 스레드 시작
  → 라이어가 키워드 입력
  → submitAnswer() 호출
  → "/liarAnswer [입력한키워드]" 전송
  → 서버가 정답 검증
  → "/finalResult [승자]|[라이어]|[메시지]" 수신
  → ResultUI로 전환
```

**정답 검증 로직** (서버 측):
- 입력한 키워드와 실제 키워드 비교
- 대소문자 구분 없음, 공백 제거 후 비교
- 정답 → 라이어 승리
- 오답 → 시민 승리

**네트워크 프로토콜**:
- `/liarAnswer [키워드]`: 라이어의 정답 제출
- `/finalResult [승자]|[라이어이름]|[메시지]`: 최종 게임 결과

**주요 메서드**:
- `initializeUI()`: UI 초기화
- `submitAnswer()`: 정답 제출
- `ListenResult`: 결과 수신 스레드

**사용 리소스**:
- `/PlayUI/InsertAnswer.png`: 배경
- `/PlayUI/Submit.png`: 제출 버튼

---

#### 단계 6: 결과 및 로비 복귀

##### 6.1 ResultUI.java
**위치**: `src/main/java/org/example/ResultUI.java`

**주요 기능**:
- 게임 최종 결과 표시
- 승패 및 상세 정보 안내
- 로비로 복귀 기능

**핵심 코드 흐름**:
```java
ResultUI 생성자(userName, citizenWin, message, ...)
  → initializeUI()
  → 승패에 따라 배경 이미지 선택
  → 결과 메시지 표시
  → "로비로 이동" 버튼 클릭
  → "/leaveRoom" 전송
  → RoomListUI로 전환 (기존 소켓 재사용)
```

**주요 메서드**:
- `initializeUI()`: 결과 화면 UI 초기화

**사용 리소스**:
- `/ResultTabUI/CitizenWin.png`: 시민 승리 배경
- `/ResultTabUI/LiarWin.png`: 라이어 승리 배경
- `/PlayUI/GoToLobby.png`: 로비 이동 버튼

---

#### 단계 7: 서버 (별도 실행)

##### 7.1 DrawServer.java
**위치**: `src/main/java/org/example/DrawServer.java`

**주요 기능**:
- 멀티 클라이언트 연결 관리
- 게임 방 생성 및 관리
- 플레이어 입/퇴장 처리
- 게임 시작 및 턴 진행 제어
- 투표 및 결과 처리
- 메시지 브로드캐스트

**주요 클래스/컴포넌트**:
- `DrawServer`: 서버 메인 클래스
- `ClientHandler`: 각 클라이언트 연결을 처리하는 스레드
- `rooms`: 방 ID와 GameRoom 객체 매핑
- `clients`: 닉네임과 ClientHandler 매핑

**핵심 서버 로직**:

**A. 클라이언트 연결**:
```java
main()
  → ServerSocket(30000) 생성
  → 클라이언트 연결 대기
  → 연결 수락 시 ClientHandler 스레드 생성
  → ClientHandler.run() 실행
```

**B. 방 관리**:
```java
"/createRoom" 수신
  → GameRoom 객체 생성
  → rooms 맵에 추가
  → "/roomCreated [방ID]" 응답
  → 방 목록 브로드캐스트

"/joinRoom [방ID]" 수신
  → 방 존재 및 입장 가능 여부 확인
  → 플레이어 추가
  → "/joinedRoom [방ID|플레이어목록]" 응답
  → 방의 다른 플레이어에게 "/playerJoined [이름]" 브로드캐스트
  → "/updatePlayerList [목록]" 브로드캐스트
```

**C. 게임 진행**:
```java
"/gameStart [카테고리]" 수신 (방장만 가능)
  → GameRoom.startGameWithCategory() 호출
  → 라이어 랜덤 선택
  → 각 플레이어에게 역할 배정
    - 라이어: "/gameStart LIAR|[카테고리]"
    - 시민: "/gameStart CITIZEN|[키워드]"
  → 턴 타이머 시작
  → 1초마다 "/gameState|턴|라운드|플레이어|시간" 브로드캐스트
  → 턴 종료 시 자동으로 다음 턴 진행
  → 모든 라운드 종료 시 "/gameEnded" 브로드캐스트
```

**D. 투표 처리**:
```java
"/vote [플레이어]" 수신
  → GameRoom.addVote() 호출
  → 모든 플레이어가 투표했는지 확인
  → 최다 득표자 계산
  → 최다 득표자가 라이어인지 확인
  → "/voteResult [이름]|[라이어여부]" 브로드캐스트
  → 라이어이면 정답 입력 대기
  → 라이어가 아니면 즉시 "/finalResult" 전송 (라이어 승리)
```

**E. 라이어 정답 처리**:
```java
"/liarAnswer [키워드]" 수신
  → GameRoom.checkLiarAnswer() 호출
  → 정답 비교 (대소문자 무시)
  → 정답이면 라이어 승리, 오답이면 시민 승리
  → "/finalResult [승자]|[라이어]|[메시지]" 브로드캐스트
```

**F. 방 나가기 및 방장 위임**:
```java
"/leaveRoom" 또는 연결 끊김
  → 플레이어를 방에서 제거
  → 방장이 나갔을 경우:
    - 다음 플레이어에게 방장 위임
    - "/hostChanged [새방장]" 브로드캐스트
  → "/playerLeft [이름]" 브로드캐스트
  → 방이 비었으면 방 삭제
```

**주요 네트워크 프로토콜 요약**:

| 프로토콜 | 방향 | 설명 |
|---------|------|------|
| `/login [닉네임]` | C→S | 로그인 요청 |
| `/loginOK` | S→C | 로그인 성공 |
| `/getRoomList` | C→S | 방 목록 요청 |
| `/roomList [데이터]` | S→C | 방 목록 응답 |
| `/createRoom [데이터]` | C→S | 방 생성 요청 |
| `/roomCreated [방ID]` | S→C | 방 생성 완료 |
| `/joinRoom [방ID]` | C→S | 방 참가 요청 |
| `/joinedRoom [데이터]` | S→C | 방 참가 완료 |
| `/leaveRoom` | C→S | 방 나가기 |
| `/playerJoined [이름]` | S→C | 플레이어 입장 알림 |
| `/playerLeft [이름]` | S→C | 플레이어 퇴장 알림 |
| `/hostChanged [이름]` | S→C | 방장 변경 알림 |
| `/updatePlayerList [목록]` | S→C | 플레이어 목록 갱신 |
| `/gameStart [카테고리]` | C→S | 게임 시작 요청 |
| `/gameStart [역할정보]` | S→C | 역할 배정 |
| `/gameState|데이터` | S→C | 게임 상태 동기화 |
| `/draw [좌표]` | C→S→C | 그림 그리기 명령 |
| `/clear` | C→S→C | 캔버스 지우기 |
| `/gameEnded` | S→C | 게임 종료 |
| `/vote [이름]` | C→S | 투표 전송 |
| `/voteResult [데이터]` | S→C | 투표 결과 |
| `/liarAnswer [키워드]` | C→S | 라이어 정답 제출 |
| `/finalResult [데이터]` | S→C | 최종 게임 결과 |

---

##### 7.2 GameRoom.java
**위치**: `src/main/java/org/example/GameRoom.java`

**주요 기능**:
- 개별 게임 방의 상태 관리
- 턴 기반 게임 진행 제어
- 타이머 관리 (각 턴 15초)
- 라이어 및 키워드 관리
- 투표 시스템
- 게임 이벤트 리스너

**주요 필드**:
- `players`: 플레이어 목록 (최대 4명)
- `currentTurnIndex`: 현재 턴 인덱스
- `currentRound`: 현재 라운드 (1~4)
- `selectedCategory`: 선택된 카테고리
- `selectedKeyword`: 실제 키워드
- `liarName`: 라이어 플레이어 이름
- `playerRoles`: 플레이어별 역할 (라이어 여부)
- `votes`: 투표 정보
- `turnTimer`: 턴 타이머

**게임 설정 상수**:
```java
TURN_TIME_SECONDS = 15    // 각 턴 제한 시간
MAX_ROUNDS = 4            // 총 라운드 수
REQUIRED_PLAYERS = 4      // 필수 플레이어 수
```

**핵심 메서드**:
- `startGameWithCategory()`: 게임 시작 및 역할 배정
- `startTurnTimer()`: 턴 타이머 시작
- `nextTurn()`: 다음 턴으로 이동
- `endGame()`: 게임 종료 및 투표 단계 진입
- `addVote()`: 투표 추가
- `getMostVotedPlayer()`: 최다 득표자 계산
- `isMostVotedLiar()`: 최다 득표자가 라이어인지 확인
- `checkLiarAnswer()`: 라이어의 정답 검증
- `isPlayerTurn()`: 특정 플레이어의 턴인지 확인

**이벤트 리스너**:
```java
public interface GameEventListener {
    void onTurnChanged(int turnIndex, int round, String currentPlayer);
    void onGameEnded();
    void onTimerTick(int remainingSeconds);
}
```

---

#### 유틸리티 클래스

##### 8.1 UIComponents.java
**위치**: `src/main/java/org/example/UIComponents.java`

**주요 기능**:
- 재사용 가능한 UI 컴포넌트 제공
- 커스텀 버튼 (둥근 모서리 버튼)

**주요 클래스**:
- `RoundedButton`: 둥근 모서리와 그라데이션 효과가 있는 버튼

---

##### 8.2 UIUtils.java
**위치**: `src/main/java/org/example/UIUtils.java`

**주요 기능**:
- UI 관련 유틸리티 메서드 모음
- 버튼 효과 적용 (투명 배경, 테두리 제거)

**주요 메서드**:
- `applyButtonEffects()`: 이미지 버튼에 투명 효과 적용

---

### 2.3 리소스 파일 목록

#### 로그인 UI (`/loginUI/`)
- `ServerIp.png`: 서버 IP 레이블 이미지
- `NickName.png`: 닉네임 레이블 이미지
- `Port.png`: 포트 레이블 이미지
- `StartButton.png`: 시작 버튼 이미지
- `loginBackground.png`: 로그인 배경 (루트)

#### 방 목록 UI (`/RoomList/`)
- `RoomListBackGround.png`: 배경 이미지
- `GameListTitle.png`: 게임 방 목록 제목
- `Reroad.png`: 새로고침 버튼
- `EnterRoomBtn.png`: 방 참가 버튼
- `CreateRoomBtn.png`: 방 만들기 버튼
- `createRoom.png`: 방 생성 다이얼로그 배경
- `RoomName.png`: 방 이름 레이블
- `category.png`: 카테고리 레이블
- `createBtn.png`: 생성 버튼
- `cancelBtn.png`: 취소 버튼

#### 게임 화면 (`/game/`)
- `back.png`: 게임 화면 배경
- `exit.png`: 나가기 버튼
- `size.png`: 굵기 레이블
- `color.png`: 색상 선택 버튼
- `erase.png`: 지우개 버튼
- `allerase.png`: 전체 지우기 버튼
- `category.png`: 카테고리 아이콘
- `host.png`: 방장 아이콘
- `player.png`: 플레이어 슬롯 이미지
- `waiting.png`: 대기 슬롯 이미지
- `send.png`: 채팅 전송 버튼
- `Strict.png`: 기타 UI 요소

#### 역할 안내 (`/info/`)
- `backGround.png`: 라이어 역할 안내 배경
- `check.png`: 라이어 확인 버튼
- `backGround2.png`: 시민 역할 안내 배경
- `check2.png`: 시민 확인 버튼

#### 게임 진행 UI (`/PlayUI/`)
- `Voting.png`: 투표 화면 배경
- `VoteButton.png`: 투표하기 버튼
- `WaitingUI.png`: 대기 화면
- `InsertAnswer.png`: 정답 입력 화면 배경
- `Submit.png`: 정답 제출 버튼
- `GoToLobby.png`: 로비로 이동 버튼
- `Exit.png`: 기타 버튼

#### 결과 화면 (`/ResultTabUI/`)
- `CitizenWin.png`: 시민 승리 배경
- `LiarWin.png`: 라이어 승리 배경

#### 이모티콘 (`/Imoji/`)
- `cold_sweat.png`: 식은땀 이모티콘
- `down.png`: 싫어요 이모티콘
- `grin.png`: 활짝 웃는 이모티콘
- `hankey.png`: 똥 이모티콘
- `joy.png`: 기쁨의 눈물 이모티콘
- `lying.png`: 거짓말 이모티콘
- `open_mouth.png`: 입 벌린 이모티콘
- `partying_face.png`: 파티 이모티콘
- `rage.png`: 화난 이모티콘
- `scream.png`: 비명 이모티콘
- `shushing_face.png`: 쉿 이모티콘
- `skull_and_crossbones.png`: 해골 이모티콘
- `star-struck.png`: 별눈 이모티콘
- `thinking.png`: 생각하는 이모티콘
- `up.png`: 좋아요 이모티콘
- `yawning_face.png`: 하품 이모티콘
- `zany.png`: 우스꽝스러운 이모티콘
- `zipper_mouth_face.png`: 입 잠근 이모티콘

---

## 3. 주요 기능 설명

### 3.1 사용자 인증 및 연결 관리

**구현 위치**: `LiarGameClientLoginUI.java`, `DrawServer.java`

**기능 상세**:
- 클라이언트는 서버 IP, 포트, 닉네임을 입력하여 서버에 연결
- 소켓 연결 후 `/login [닉네임]` 프로토콜로 로그인 요청
- 서버는 닉네임 중복 검사 후 `/loginOK` 응답
- 연결 실패 시 오류 메시지 표시 및 재시도

**핵심 코드**:
```java
// LiarGameClientLoginUI.java
socket = new Socket(serverIp, Integer.parseInt(serverPort));
dis = new DataInputStream(socket.getInputStream());
dos = new DataOutputStream(socket.getOutputStream());
dos.writeUTF("/login " + userName);
String response = dis.readUTF();
if (!response.equals("/loginOK")) {
    throw new Exception("로그인 실패");
}
```

---

### 3.2 방 생성 및 관리

**구현 위치**: `RoomListUI.java`, `DrawServer.java`, `GameRoom.java`

**기능 상세**:
- 사용자는 방 이름과 카테고리를 지정하여 새 게임 방 생성
- 각 방은 고유한 UUID로 식별됨
- 방 정보: 방 ID, 방 이름, 방장, 카테고리, 최대 인원(4명)
- 서버는 방 목록을 관리하고 변경 사항을 모든 클라이언트에게 브로드캐스트

**방 생성 프로세스**:
1. 클라이언트가 방 생성 다이얼로그에서 정보 입력
2. UUID 기반 방 ID 생성
3. `GameRoom` 객체 생성 및 방 정보 설정
4. 서버에 `/createRoom [방정보]` 전송
5. 서버가 방을 `rooms` 맵에 추가
6. 방 생성자에게 `/roomCreated [방ID]` 응답
7. 게임 화면으로 자동 전환

**방 참가 프로세스**:
1. 클라이언트가 방 목록에서 방 선택
2. 입장 가능 여부 검증 (인원, 게임 진행 상태)
3. 서버에 `/joinRoom [방ID]` 전송
4. 서버가 플레이어를 방에 추가
5. 참가자에게 `/joinedRoom [방ID|플레이어목록]` 응답
6. 방의 다른 플레이어들에게 `/playerJoined [이름]` 브로드캐스트
7. 플레이어 목록 갱신

---

### 3.3 게임 시작 및 역할 배정

**구현 위치**: `JavaChatClientView.java`, `DrawServer.java`, `GameRoom.java`

**기능 상세**:
- 방장만 게임 시작 가능
- 4명의 플레이어가 모두 모여야 시작 가능
- 게임 시작 시 자동으로 라이어 1명 선택 (랜덤)
- 나머지 3명은 시민 역할
- 라이어는 카테고리만 알 수 있음
- 시민은 실제 키워드를 알 수 있음

**역할 배정 로직**:
```java
// GameRoom.java
Random random = new Random();
int liarIndex = random.nextInt(players.size());
this.liarName = players.get(liarIndex);

for (int i = 0; i < players.size(); i++) {
    String player = players.get(i);
    boolean isLiar = (i == liarIndex);
    playerRoles.put(player, isLiar);
}

// 각 플레이어에게 역할 정보 전송
if (isLiar(playerName)) {
    return "LIAR|" + selectedCategory;
} else {
    return "CITIZEN|" + selectedKeyword;
}
```

**역할 안내 UI**:
- 역할 정보를 받은 후 모달 다이얼로그로 표시
- 라이어: 빨간색 테마, 카테고리 표시
- 시민: 파란색 테마, 키워드 표시
- 확인 버튼을 누르면 게임 시작

---

### 3.4 턴 기반 게임 진행

**구현 위치**: `GameRoom.java`, `DrawServer.java`, `JavaChatClientView.java`

**기능 상세**:
- 4명의 플레이어가 순서대로 그림 그리기
- 각 플레이어는 15초의 턴 시간 부여
- 자신의 턴에만 그리기 도구 및 채팅 활성화
- 다른 플레이어의 턴에는 관전 모드
- 총 4라운드 진행 (4명 × 4라운드 = 16턴)

**턴 타이머 구현**:
```java
// GameRoom.java
private void startTurnTimer() {
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
```

**게임 상태 동기화**:
- 서버가 1초마다 `/gameState|턴|라운드|플레이어|시간` 브로드캐스트
- 클라이언트는 이 정보를 받아 UI 업데이트
- 현재 턴 플레이어의 UI 강조 표시
- 타이머 표시 업데이트

**턴 전환 로직**:
```java
private void nextTurn() {
    currentTurnIndex++;

    if (currentTurnIndex >= players.size()) {
        currentTurnIndex = 0;
        currentRound++;

        if (currentRound > MAX_ROUNDS) {
            endGame();
            return;
        }
    }

    startTurnTimer();
    notifyTurnChange();
}
```

---

### 3.5 실시간 그림 그리기 동기화

**구현 위치**: `DrawingPanel.java`, `JavaChatClientView.java`, `DrawServer.java`

**기능 상세**:
- 한 플레이어가 그린 그림이 모든 플레이어에게 실시간 전송
- 마우스 드래그 이벤트를 네트워크 프로토콜로 변환
- 선 그리기, 색상 변경, 지우개, 전체 지우기 지원

**그림 그리기 프로토콜**:
```
/draw x1,y1,x2,y2,r,g,b,width
```
- (x1, y1): 시작 좌표
- (x2, y2): 끝 좌표
- (r, g, b): RGB 색상 값
- width: 선 굵기

**전체 지우기 프로토콜**:
```
/clear
```

**그림 동기화 흐름**:
1. 플레이어 A가 마우스로 그림 그리기
2. `DrawingPanel`에서 로컬 버퍼에 그림 저장
3. `/draw` 프로토콜 생성 및 서버 전송
4. 서버가 같은 방의 모든 플레이어에게 브로드캐스트
5. 플레이어 B, C, D가 프로토콜 수신
6. 각 클라이언트의 `DrawingPanel.processDrawCommand()` 호출
7. 로컬 버퍼에 그림 그리기
8. 화면 갱신

**핵심 코드**:
```java
// DrawingPanel.java - 그림 그리기
public void mouseDragged(MouseEvent e) {
    if (!isEnabled) return;

    int x2 = e.getX();
    int y2 = e.getY();

    Graphics2D g2 = (Graphics2D) bufferImage.getGraphics();
    g2.setColor(callback.getCurrentColor());
    g2.setStroke(new BasicStroke(callback.getStrokeWidth()));
    g2.drawLine(lastX, lastY, x2, y2);

    // 네트워크 전송
    String protocol = String.format("/draw %d,%d,%d,%d,%d,%d,%d,%d",
        lastX, lastY, x2, y2,
        color.getRed(), color.getGreen(), color.getBlue(),
        callback.getStrokeWidth());
    callback.sendProtocol(protocol);

    lastX = x2;
    lastY = y2;
    repaint();
}

// DrawingPanel.java - 네트워크 그림 처리
public void processDrawCommand(String msg) {
    String[] tokens = msg.substring(6).split(",");
    int x1 = Integer.parseInt(tokens[0]);
    int y1 = Integer.parseInt(tokens[1]);
    int x2 = Integer.parseInt(tokens[2]);
    int y2 = Integer.parseInt(tokens[3]);
    int r = Integer.parseInt(tokens[4]);
    int g = Integer.parseInt(tokens[5]);
    int b = Integer.parseInt(tokens[6]);
    int width = Integer.parseInt(tokens[7]);

    Graphics2D g2 = (Graphics2D) bufferImage.getGraphics();
    g2.setColor(new Color(r, g, b));
    g2.setStroke(new BasicStroke(width));
    g2.drawLine(x1, y1, x2, y2);
    repaint();
}
```

---

### 3.6 채팅 및 이모티콘 시스템

**구현 위치**: `ChatPanel.java`, `JavaChatClientView.java`

**기능 상세**:
- 텍스트 채팅: 일반 메시지 전송
- 이모티콘: 18종의 감정 표현 이미지
- 시스템 메시지: 입장/퇴장/게임 상태 알림
- 메시지 히스토리: 스크롤 가능한 채팅 로그

**채팅 프로토콜**:
```
[닉네임]: 메시지 내용
[닉네임]: [EMO:이모티콘명]
[시스템] 시스템 메시지
```

**이모티콘 전송 흐름**:
1. 이모티콘 버튼 클릭
2. 18개의 이모티콘이 표시된 다이얼로그 팝업
3. 이모티콘 이미지 버튼 클릭
4. `[EMO:이모티콘명]` 형식으로 메시지 전송
5. 수신 측에서 이모티콘명을 파싱
6. 해당 이모티콘 이미지를 채팅창에 표시

**이모티콘 로딩**:
```java
// JavaChatClientView.java
private void loadEmojis() {
    emojiMap = new HashMap<>();
    File[] files = new File("/Imoji/").listFiles();

    for (File file : files) {
        if (file.getName().endsWith(".png")) {
            String emojiKey = file.getName().replace(".png", "");
            ImageIcon icon = new ImageIcon(file.getPath());
            Image scaled = icon.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH);
            emojiMap.put(emojiKey, new ImageIcon(scaled));
        }
    }
}
```

**채팅 메시지 표시**:
```java
// ChatPanel.java
public void appendChatMessage(String msg) {
    if (msg.contains("[EMO:")) {
        // 이모티콘 메시지 처리
        int start = msg.indexOf("[EMO:");
        int end = msg.indexOf("]", start);
        String emojiName = msg.substring(start + 5, end);

        ImageIcon emoji = emojiMap.get(emojiName);
        if (emoji != null) {
            // 이미지로 표시
        }
    } else {
        // 일반 텍스트 메시지
        txtChat.append(msg + "\n");
    }
}
```

---

### 3.7 투표 시스템

**구현 위치**: `VotingUI.java`, `DrawServer.java`, `GameRoom.java`

**기능 상세**:
- 게임 종료 후 자동으로 투표 화면 전환
- 각 플레이어는 자신을 제외한 3명 중 1명에게 투표
- 자신에게는 투표 불가 (버튼 비활성화)
- 모든 플레이어가 투표할 때까지 대기
- 최다 득표자를 라이어로 지목

**투표 처리 로직**:
```java
// GameRoom.java
public void addVote(String voter, String votedPlayer) {
    if (votingPhase && players.contains(voter) && players.contains(votedPlayer)) {
        votes.put(voter, votedPlayer);
    }
}

public String getMostVotedPlayer() {
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
```

**투표 결과 분기**:
1. **최다 득표자가 라이어인 경우**:
   - 라이어에게 마지막 기회 부여 (키워드 맞추기)
   - 다른 플레이어는 대기 화면

2. **최다 득표자가 라이어가 아닌 경우**:
   - 즉시 라이어 승리로 게임 종료
   - 결과 화면 표시

---

### 3.8 라이어 정답 입력 및 검증

**구현 위치**: `InsertAnswerUI.java`, `DrawServer.java`, `GameRoom.java`

**기능 상세**:
- 투표로 라이어가 지목된 경우에만 활성화
- 라이어는 30초 내에 키워드를 입력
- 정답 검증은 대소문자 구분 없음, 공백 제거 후 비교
- 정답을 맞추면 라이어 승리, 틀리면 시민 승리

**정답 검증 로직**:
```java
// GameRoom.java
public boolean checkLiarAnswer(String answer) {
    if (answer == null || selectedKeyword == null) {
        return false;
    }

    String normalizedAnswer = answer.trim().toLowerCase();
    String normalizedKeyword = selectedKeyword.trim().toLowerCase();

    return normalizedAnswer.equals(normalizedKeyword);
}
```

**서버 처리 흐름**:
```java
// DrawServer.java - ClientHandler
if (msg.startsWith("/liarAnswer ")) {
    String answer = msg.substring(12);
    boolean correct = room.checkLiarAnswer(answer);

    String winner = correct ? "LIAR" : "CITIZEN";
    String resultMsg;

    if (correct) {
        resultMsg = String.format("라이어: %s\n라이어가 정답을 맞췄습니다!\n정답: %s",
            room.getLiarName(), room.getSelectedKeyword());
    } else {
        resultMsg = String.format("라이어: %s\n라이어가 정답을 맞추지 못했습니다!\n정답: %s",
            room.getLiarName(), room.getSelectedKeyword());
    }

    broadcastToRoom(roomId, "/finalResult " + winner + "|" + room.getLiarName() + "|" + resultMsg);
}
```

---

### 3.9 게임 결과 표시 및 로비 복귀

**구현 위치**: `ResultUI.java`, `RoomListUI.java`

**기능 상세**:
- 게임 결과를 시각적으로 표시 (승리 팀에 따라 배경 이미지 변경)
- 상세 결과 메시지 표시:
  - 라이어 이름
  - 투표 결과
  - 정답 키워드
- "로비로 이동" 버튼으로 방 목록 화면 복귀
- 기존 소켓 연결 유지 (재로그인 불필요)

**결과 메시지 종류**:

**시민 승리**:
```
라이어: [이름]
라이어가 정답을 맞추지 못했습니다!
정답: [키워드]
```

**라이어 승리 (투표 실패)**:
```
[이름]님이 억울하게 투표되었습니다!
실제 라이어는 다른 플레이어였습니다.
```

**라이어 승리 (정답 맞춤)**:
```
라이어: [이름]
라이어가 정답을 맞췄습니다!
정답: [키워드]
```

**로비 복귀 로직**:
```java
// ResultUI.java
btnConfirm.addActionListener(e -> {
    try {
        if (dos != null) {
            dos.writeUTF("/leaveRoom");
        }
    } catch (Exception ex) {
        ex.printStackTrace();
    }

    SwingUtilities.invokeLater(() -> {
        // 기존 소켓 재사용
        new RoomListUI(userName, serverIp, serverPort, socket, dis, dos).setVisible(true);
        dispose();
    });
});
```

---

### 3.10 플레이어 입/퇴장 및 방장 위임

**구현 위치**: `DrawServer.java`, `RoomListUI.java`, `JavaChatClientView.java`

**기능 상세**:
- 플레이어 입장 시 모든 방 멤버에게 알림
- 플레이어 퇴장 시 자동으로 방에서 제거
- 방장 퇴장 시 다음 플레이어에게 자동으로 방장 위임
- 방이 비면 자동으로 방 삭제

**방장 위임 로직**:
```java
// DrawServer.java
private void handlePlayerLeave(String roomId, String playerName) {
    GameRoom room = rooms.get(roomId);
    if (room == null) return;

    room.removePlayer(playerName);

    // 방장이 나갔는지 확인
    if (room.getHostName().equals(playerName)) {
        List<String> players = room.getPlayers();

        if (!players.isEmpty()) {
            // 첫 번째 플레이어를 새 방장으로 지정
            String newHost = players.get(0);
            room.setHostName(newHost);

            // 모든 플레이어에게 방장 변경 알림
            broadcastToRoom(roomId, "/hostChanged " + newHost);
        } else {
            // 방이 비었으면 삭제
            rooms.remove(roomId);
            broadcastRoomList();
        }
    }

    // 플레이어 목록 업데이트
    String playerList = String.join(",", room.getPlayers());
    broadcastToRoom(roomId, "/updatePlayerList " + playerList);
    broadcastToRoom(roomId, "/playerLeft " + playerName);
}
```

**클라이언트 측 처리**:
```java
// JavaChatClientView.java
else if (msg.startsWith("/hostChanged ")) {
    String newHostName = msg.substring(13);

    SwingUtilities.invokeLater(() -> {
        currentRoom.setHostName(newHostName);
        isHost = userName.equals(newHostName);

        chatPanel.appendSystemMessage("[시스템] 방장이 " + newHostName + "님으로 변경되었습니다.");
        lblHostName.setText(": " + newHostName);
        updatePlayerList(currentRoom.getPlayers());
        updateStartButtonState();
    });
}
```

---

## 4. 네트워크 아키텍처

### 4.1 통신 구조

```
[클라이언트 1]  ←→  [서버]  ←→  [클라이언트 2]
[클라이언트 3]  ←→          ←→  [클라이언트 4]
```

- **프로토콜**: TCP/IP 소켓 통신
- **포트**: 30000 (기본값)
- **데이터 전송**: `DataInputStream` / `DataOutputStream`
- **인코딩**: UTF-8

### 4.2 메시지 형식

모든 메시지는 UTF-8 문자열로 전송되며, `/` 로 시작하는 명령어 형식을 따릅니다.

**기본 형식**:
```
/명령어 [매개변수1] [매개변수2] ...
```

**구분자**:
- 공백(` `): 명령어와 매개변수 구분
- 파이프(`|`): 복합 데이터 필드 구분
- 세미콜론(`;`): 여러 데이터 항목 구분
- 쉼표(`,`): 배열 데이터 구분

### 4.3 서버 스레드 구조

```
[메인 스레드]
   └─ ServerSocket 대기
       ├─ [ClientHandler 스레드 1]
       ├─ [ClientHandler 스레드 2]
       ├─ [ClientHandler 스레드 3]
       └─ [ClientHandler 스레드 4]

[GameRoom]
   └─ [Timer 스레드] (턴 타이머)
```

---

## 5. 게임 규칙

### 5.1 기본 규칙

1. **인원**: 4명 (필수)
2. **역할**:
   - 라이어 1명: 카테고리만 알 수 있음
   - 시민 3명: 실제 키워드를 알 수 있음
3. **라운드**: 총 4라운드
4. **턴 시간**: 각 플레이어당 15초

### 5.2 게임 진행

1. **역할 배정**: 게임 시작 시 랜덤으로 라이어 선택
2. **그림 그리기**: 각 플레이어가 순서대로 키워드를 표현하는 그림 그리기
   - 시민: 정확한 키워드를 바탕으로 그림
   - 라이어: 카테고리만 알고 있으므로 추측하여 그림
3. **투표**: 4라운드 종료 후 라이어로 의심되는 플레이어에게 투표
4. **정답 입력**: 라이어가 지목되면 키워드 맞추기 기회 부여

### 5.3 승리 조건

**시민 승리**:
- 투표로 라이어를 찾아내고, 라이어가 키워드를 맞추지 못한 경우

**라이어 승리**:
- 투표에서 라이어가 아닌 플레이어가 지목된 경우
- 라이어가 지목되었지만 키워드를 정확히 맞춘 경우

---

## 6. 기술 스택

### 6.1 개발 환경

- **언어**: Java 8 이상
- **빌드 도구**: Maven
- **IDE**: IntelliJ IDEA / Eclipse

### 6.2 주요 라이브러리

- **GUI**: Java Swing
- **네트워크**: Java Socket API
  - `java.net.Socket`
  - `java.net.ServerSocket`
  - `java.io.DataInputStream`
  - `java.io.DataOutputStream`
- **타이머**: `javax.swing.Timer`
- **이미지 처리**: `javax.imageio.ImageIO`

### 6.3 디자인 패턴

- **MVC 패턴**: UI와 비즈니스 로직 분리
- **콜백 패턴**: DrawingPanel, ChatPanel의 이벤트 처리
- **싱글톤 패턴**: 서버의 방 관리
- **스레드 풀**: 클라이언트별 독립적인 처리 스레드

---

## 7. 실행 방법

### 7.1 서버 실행

```bash
# DrawServer 클래스 실행
java -cp target/classes org.example.DrawServer
```

서버는 포트 30000에서 클라이언트 연결을 대기합니다.

### 7.2 클라이언트 실행

```bash
# LiarGameClientLoginUI 클래스 실행
java -cp target/classes org.example.LiarGameClientLoginUI
```

또는 IDE에서 `LiarGameClientLoginUI`의 `main` 메서드를 실행합니다.

### 7.3 Maven 빌드

```bash
# 프로젝트 빌드
mvn clean package

# 실행
java -jar target/DrawLier-1.0.jar
```

---

## 8. 향후 개선 사항

### 8.1 기능 개선

- [ ] 타임아웃 후 자동 투표 (AFK 방지)
- [ ] 게임 리플레이 기능
- [ ] 그림 저장 기능
- [ ] 더 다양한 카테고리 및 키워드 추가
- [ ] 난이도 조절 (키워드 힌트 제공)

### 8.2 UI/UX 개선

- [ ] 애니메이션 효과 추가
- [ ] 사운드 효과 및 배경 음악
- [ ] 커스텀 아바타 및 프로필
- [ ] 다크 모드 지원

### 8.3 성능 및 안정성

- [ ] 연결 끊김 시 재연결 로직
- [ ] 예외 처리 강화
- [ ] 로그 시스템 구축
- [ ] 데이터베이스 연동 (게임 기록 저장)

### 8.4 멀티플랫폼

- [ ] 웹 버전 개발 (HTML5 Canvas)
- [ ] 모바일 앱 개발 (Android/iOS)
- [ ] 크로스 플랫폼 지원

---

## 9. 트러블슈팅

### 9.1 연결 오류

**증상**: "서버 연결 실패" 메시지

**해결 방법**:
1. 서버가 실행 중인지 확인
2. 방화벽 설정 확인 (포트 30000 허용)
3. IP 주소 및 포트 번호 확인

### 9.2 한글 깨짐

**증상**: 채팅 또는 UI에서 한글이 깨져 보임

**해결 방법**:
```java
System.setProperty("file.encoding", "UTF-8");
```

### 9.3 이미지 로딩 실패

**증상**: 버튼이나 배경 이미지가 표시되지 않음

**해결 방법**:
1. 리소스 파일 경로 확인 (`/resources/`)
2. 이미지 파일 존재 여부 확인
3. 빌드 시 리소스가 포함되었는지 확인

---

## 10. 라이선스 및 크레딧

### 10.1 개발팀

- 프로젝트명: DrawLier
- 개발 기간: 2024년

### 10.2 참고 자료

- 라이어 게임 규칙: [위키백과 - 라이어 게임](https://ko.wikipedia.org/wiki/라이어_게임)
- Java Swing 튜토리얼: [Oracle Java Tutorials](https://docs.oracle.com/javase/tutorial/uiswing/)

---

**문서 작성일**: 2025-12-03
**버전**: 1.0
