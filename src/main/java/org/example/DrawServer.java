package org.example;

//JavaChatServer.java (Java Chatting Server)

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class DrawServer extends JFrame {

    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    //JTextArea textArea;
    private JTextField txtPortNumber;
    private JTextField setTime;  //시간 설정
    private Map<String, ArrayList<String>> categoryValue = new HashMap<>(); //카테고리 배열
    private JLabel clientCount;
    private ServerSocket socket; // 서버소켓
    private Socket client_socket; // accept() 에서 생성된 client 소켓, AcceptServer에서 지역변수로 선언해도 됩니다.
    private Vector<UserService> UserVec = new Vector<>(); // 연결된 사용자를 저장할 벡터, ArrayList와 같이 동적 배열을 만들어주는 컬렉션 객체
    private static final int BUF_LEN = 128; // Windows 처럼 BUF_LEN 을 정의
    JLabel [] playerSlot = new JLabel[4];

    /**
     * Launch the application.
     */
    public static void main(String[] args) {   // 스윙 비주얼 디자이너를 이용해 GUI를 만들면 자동으로 생성되는 main 함수
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    DrawServer frame = new DrawServer();      // DrawServer 클래스의 객체 생성
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
    private void setCategory(){
        //직업 카테고리
        ArrayList<String> jobs = new ArrayList<String>(Arrays.asList("소방관", "경찰", "의사"));
        categoryValue.put("직업", jobs);

        //동물 카테고리
        ArrayList<String> animals = new ArrayList<String>(Arrays.asList("사자", "표범", "기린"));
        categoryValue.put("동물", animals);

        //음식 카테고리
        ArrayList<String> foods = new ArrayList<String>(Arrays.asList("김밥", "떡볶이", "스테이크"));
        categoryValue.put("음식", foods);
    }
    public DrawServer() {
        setCategory(); //카테고리 초기화
//        textArea = new JTextArea();
//        textArea.setEditable(false);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 500, 600);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setBackground(Color.WHITE);
        contentPane.setLayout(null);

        //타이틀
        JLabel titleLable = new JLabel("DrawLier 서버");
        Font titleFont = titleLable.getFont();
        Font newFont = new Font(titleFont.getName(), Font.BOLD, 20);
        titleLable.setFont(newFont);
        titleLable.setBounds(175, 10, 338, 30);
        contentPane.add(titleLable);


        //포트 번호 GUI
        JLabel lblNewLabel = new JLabel("생성할 방 번호");
        lblNewLabel.setBounds(12, 264, 87, 26);
        contentPane.add(lblNewLabel);
        txtPortNumber = new JTextField();
        txtPortNumber.setHorizontalAlignment(SwingConstants.CENTER);
        txtPortNumber.setText("30000");
        txtPortNumber.setBounds(111, 264, 199, 26);
        contentPane.add(txtPortNumber);
        txtPortNumber.setColumns(10);

        //시간 설정 GUI
        JLabel timeLabel = new JLabel("시간 설정");
        timeLabel.setBounds(12, 230, 87, 26);
        contentPane.add(timeLabel);
        setTime = new JTextField();
        setTime.setHorizontalAlignment(SwingConstants.CENTER);
        setTime.setText("300");
        setTime.setBounds(111, 230, 199, 26);
        contentPane.add(setTime);
        setTime.setColumns(10);

        //카테고리 설정 GUI
        JLabel categoryLabel = new JLabel("카테고리 선택");
        categoryLabel.setBounds(12, 200, 87, 26);
        contentPane.add(categoryLabel);
        //카테고리 드롭다운
        String [] categories = categoryValue.keySet().toArray(new String[0]);
        JComboBox<String> categoryDropdown = new JComboBox<>(categories);
        categoryDropdown.setBounds(111, 200, 199, 26);
        contentPane.add(categoryDropdown);

        //인원 GUI
        JLabel clientCountlbl = new JLabel("현재 인원");
        clientCountlbl.setBounds(12, 170, 199, 26);
        contentPane.add(clientCountlbl);

        clientCount = new JLabel(UserVec.size() + " / 4");
        clientCount.setBounds(111, 170, 199, 26);
        contentPane.add(clientCount);

        //타이머 라벨
        timeLabel.setBounds(12, 230, 87, 26);
        contentPane.add(timeLabel);

        //플레이어 현황 GUI
        JPanel playerPanel = new JPanel();
        playerPanel.setLayout(new GridLayout(2,2, 10, 10));
        playerPanel.setBounds(12, 300, 464, 150);
        playerPanel.setBorder(BorderFactory.createTitledBorder("플레이어 현황"));
        playerPanel.setBackground(Color.WHITE);
        //플레이어 현황 슬롯 생성
        //JLabel [] playerSlot = new JLabel[4];
        for(int i = 0; i < 4; i++){
            playerSlot[i] = new JLabel("대기중", SwingConstants.CENTER);
            playerSlot[i].setOpaque(true);
            playerSlot[i].setBackground(new Color(230, 230, 230));
            playerSlot[i].setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            playerPanel.add(playerSlot[i]);
        }
        contentPane.add(playerPanel);

        //방 만들기 UI
        JButton btnServerStart = new JButton("방 만들기"); //서버 스타트
        btnServerStart.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    socket = new ServerSocket(Integer.parseInt(txtPortNumber.getText()));
                    btnServerStart.setText("방 생성 완료!");
                } catch (NumberFormatException | IOException e1) {
                    e1.printStackTrace();
                }
                btnServerStart.setEnabled(false); // 서버를 더이상 실행시키지 못 하게 막는다
                txtPortNumber.setEnabled(false); // 더이상 포트번호 수정못 하게 막는다
                AcceptServer accept_server = new AcceptServer();   // 멀티 스레드 객체 생성
                accept_server.start();
            }
        });
        btnServerStart.setBounds(12, 40, 464, 35);
        contentPane.add(btnServerStart);

        //게임 시작 UI
        JButton btnGameStart = new JButton("게임 시작"); //게임 스타트
        btnGameStart.setBounds(12, 480, 464, 35);
        contentPane.add(btnGameStart);
        JLabel lblGameStart = new JLabel("4명이 모두 접속해야 게임을 시작할 수 있습니다.");
        lblGameStart.setBounds(110, 510, 464, 26);
        contentPane.add(lblGameStart);
    }


    // 새로운 참가자 accept() 하고 user thread를 새로 생성한다. 한번 만들어서 계속 사용하는 스레드
    class AcceptServer extends Thread {
        private static final int MAX_CLIENTS = 4;
        public void run() {
            while (true) { // 사용자 접속을 계속해서 받기 위해 while문
                try {
                    //AppendText("Waiting clients ...");
                    client_socket = socket.accept(); // accept가 일어나기 전까지는 무한 대기중

                    if(UserVec.size() >= MAX_CLIENTS){
                        //AppendText("최대 인원");
                        try(OutputStream os = client_socket.getOutputStream();
                            DataOutputStream dos = new DataOutputStream(os);
                        ){
                            dos.writeUTF("정원 초과로 입장 불가.");
                        }
                        client_socket.close();
                        continue;
                    }
                    //AppendText("새로운 참가자 from " + client_socket);
                    // User 당 하나씩 Thread 생성
                    UserService new_user = new UserService(client_socket);
                    UserVec.add(new_user); // 새로운 참가자 배열에 추가
                    int index = UserVec.indexOf(new_user);
                    SwingUtilities.invokeLater(() -> {
                        clientCount.setText(UserVec.size() + " / 4");
                        playerSlot[index].setText(new_user.UserName);
                        playerSlot[index].setBackground(new Color(180, 220, 255));
                    });
                    new_user.start(); // 만든 객체의 스레드 실행
                } catch (IOException e) {
                    //AppendText("!!!! accept 에러 발생... !!!!");
                }
            }
        }
    }
    // 지금은 보다 심플한 코드를 위해 서버는 계속 켜져 있는 것으로 가정하였으나(AcceptServer 스레스 종료 X)
    // 서버를 정상적으로 종료하고 싶은 경우, 서버 GUI에 종료 버튼을 만들어 서버소켓을 닫는 스레드를 추가로 만들거나,
    // 또는 GUI 창이 닫히는 순간(addWindowListener의 windowClosing 메서드 등에서)
    // ServerSocket.close()를 호출하여 accept()를 깨워서 AcceptServer를 종료하는 방법이나 플래그 신호 사용 방법 등이 있을 수 있음


    //JtextArea에 문자열을 출력해 주는 기능을 수행하는 맴버 함수
//    public void AppendText(String str) {
//        textArea.append(str + "\n");   //전달된 문자열 str을 textArea에 추가
//        //textArea.setCaretPosition(textArea.getText().length());  // textArea의 커서(캐럿) 위치를 텍스트 영역의 마지막으로 이동
//    }


    // User 당 생성되는 Thread, 유저의 수만큼 스레스 생성
    // 이 UserService 스레드는 '소켓 객체'를 이용해서 실제 특정 유저와 메시지를 주고 받는 기능을 수행하는 스레드
    // 이 스레드 클래스의 run() 메소드 안의 dis.readUTF()에서 대기하다가 메시지가 들어오면 -> Write All로 전체 접속한 사용자한테 전송(단톡방)
    class UserService extends Thread {
        private InputStream is;
        private OutputStream os;
        private DataInputStream dis;
        private DataOutputStream dos;
        private Socket client_socket;
        private Vector<UserService> user_vc; // 제네릭 타입 사용
        private String UserName = "";

        public UserService(Socket client_socket) {
            // 매개변수로 넘어온 소켓 객체 저장
            this.client_socket = client_socket;
            this.user_vc = UserVec;
            try {
                is = client_socket.getInputStream();
                dis = new DataInputStream(is);
                os = client_socket.getOutputStream();
                dos = new DataOutputStream(os);
                String line1 = dis.readUTF();      // 제일 처음 연결되면 클라이언트의 SendMessage("/login " + UserName);에 의해 "/login UserName" 문자열이 들어옴
                String[] msg = line1.split(" ");   //line1이라는 문자열을 공백(" ")을 기준으로 분할
                UserName = msg[1].trim();          //분할된 문자열 배열 msg의 두 번째 요소(인덱스 1)를 가져와 trim 메소드를 사용하여 앞뒤의 공백을 제거
                //AppendText("새로운 참가자 " + UserName + " 입장.");
                WriteOne("Welcome to Java chat server\n");
                WriteOne(UserName + "님 환영합니다.\n"); // 연결된 사용자에게 정상접속을 알림
            } catch (Exception e) {
                //AppendText("userService error");
            }
        }


        // 클라이언트로 메시지 전송
        public void WriteOne(String msg) {
            try {
                dos.writeUTF(msg);
            } catch (IOException e) {
                //AppendText("dos.write() error");
                try {
                    dos.close();
                    dis.close();
                    client_socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                UserVec.removeElement(this); // 에러가난 현재 객체를 벡터에서 지운다
                //AppendText("사용자 퇴장. 현재 참가자 수 " + UserVec.size());
            }
        }


        //모든 다중 클라이언트에게 순차적으로 채팅 메시지 전달
        public void WriteAll(String str) {
            for (int i = 0; i < user_vc.size(); i++) {
                UserService user = user_vc.get(i);     // get(i) 메소드는 user_vc 컬렉션의 i번째 요소를 반환
                user.WriteOne(str);
            }
        }

        // 특정 클라이언트를 제외한 나머지 클라이언트에게 메시지 전달 (그림 데이터 브로드캐스트용)
        public void WriteAllExceptMe(String str) {
            for (int i = 0; i < user_vc.size(); i++) {
                UserService user = user_vc.get(i);
                // 자기 자신이 아닌 경우에만 전송
                if (user != this) {
                    user.WriteOne(str);
                }
            }
        }


        public void run() {
            // dis.readUTF()에서 대기하다가 메시지가 들어오면 -> Write All로 전체 접속한 사용자한테 메시지 전송(단톡방), 이걸 클라이언트별로 무한히 실행
            // 추가적으로 지금은 dis.readUTF()에서 예외가 발생하면 '예외처리에 의해 정상적으로 스레드가 종료하게 작성'되었으나
            // '/exit'가 들어와도 종료하게 코드를 추가하면 더 완성도 있는 코드가 됩니다.
            // 지금은 다양한 사용자 프로토콜(/list, /to, /exit 등)을 정의하고 있지 않지만 추후 /exit 프로토콜 등의 정의시 추가
            while (true) {
                try {
                    String msg = dis.readUTF();
                    msg = msg.trim();   //msg를 가져와 trim 메소드를 사용하여 앞뒤의 공백을 제거
                    
                    // 그림 데이터 처리: /draw 또는 /clear 명령어인 경우
                    if (msg.startsWith("/draw") || msg.startsWith("/clear")) {
                        // 그림 데이터는 본인을 제외한 다른 모든 클라이언트에게 전송
                        WriteAllExceptMe(msg);
                    } 
                    // 일반 채팅 메시지 처리
                    else {
                        //AppendText(msg); // server 화면에 출력
                        WriteAll(msg + "\n"); // 모든 클라이언트에게 전송
                    }
                } catch (IOException e) {
                    //AppendText("dis.readUTF() error");
                    try {
                        dos.close();
                        dis.close();
                        client_socket.close();
                        int index = UserVec.indexOf(this);
                        UserVec.removeElement(this); // 에러가 난 현재 객체를 벡터에서 지운다
                        SwingUtilities.invokeLater(() ->{
                            clientCount.setText(UserVec.size() + " / 4");
                            if(index >= 0){
                                playerSlot[index].setText("대기 중");
                                playerSlot[index].setBackground(new Color(230, 230, 230));
                            }
                        });
                        //AppendText("사용자 퇴장. 남은 참가자 수 " + UserVec.size());
                        break;
                    } catch (Exception ee) {
                        break;
                    }
                }
            }
        }

    }
}
