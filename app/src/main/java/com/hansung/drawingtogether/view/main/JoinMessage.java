package com.hansung.drawingtogether.view.main;

import java.util.List;

import lombok.Getter;


// fixme nayeon 이 클래스는 어디 패키지에 위치해야할까요...????

@Getter
public class JoinMessage {
    String master; // 마스터 이름 (master:"이름")
    String name; // 사용자 이름 (name:"이름")

    String to; // 중간자 이름 (to:"이름") - 마스터가 알아낸 사용자 리스트에서 마지막번째 사람의 이름
    List<String> userList; // 사용자 리스트
    //String loadingData; // 데이터를 받을 토픽 (topic_data)

    public JoinMessage(String master, String to, List<String> userList /*, String loadingData*/) { // "master":"이름"/"userList":"이름1,이름2,이름3"/"loadingData":"..."
        this.master = master;
        this.to = to;
        this.userList = userList;
        //this.loadingData = loadingData;
    }

    public JoinMessage(String name) {
        this.name = name;
    } // "name":"이름"
}