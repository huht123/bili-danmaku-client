package com.ggemo.bilidanmakuclient.improveter;

import com.ggemo.bilidanmakuclient.improveter.Client;
import com.ggemo.bilidanmakuclient.improveter.handler.HandlerHolder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

@Slf4j
public class HandleDataLoop {
    private Socket socket;
    private long roomId;

    private DataInputStream input = null;

    private HandlerHolder handlerHolder;

    public HandleDataLoop(Socket socket, long roomId, HandlerHolder handlerHolder) {
        this.socket = socket;
        this.roomId = roomId;
        this.handlerHolder = handlerHolder;
    }

    public void start() throws IOException {
        if (socket != null) {
            int bufferSize = 10 * 1024;
            try {
                bufferSize = socket.getReceiveBufferSize();
                log.info("连接成功" + "真实直播间ID：" + roomId);
            } catch (SocketException e) {
                log.error(e.toString());
            }
            byte[] ret = new byte[bufferSize];
//            try {
            while (true) {
                input = new DataInputStream(socket.getInputStream());
                int retLength = input.read(ret);
                if (retLength > 0) {
                    byte[] recvData = new byte[retLength];
                    System.arraycopy(ret, 0, recvData, 0, retLength);
                    analyzeData(recvData);
                }
            }
//            } catch (Exception e) {
//                log.info("自动重连" + " 真实直播间ID：" + roomId);
//                client.cleanHeartBeatTask();
//                new Thread(client::start).start();
//                e.printStackTrace();
//            }
//                DMJ_UiT_Text.setCaretPosition(DMJ_UiT_Text.getText().length());
        }
    }

    private void analyzeData(byte[] data) {

        int dataLength = data.length;
        if (dataLength < 16) {
            log.info("错误的数据");
        } else {
            DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(data));
            try {
                int msgLength = inputStream.readInt();
                if (msgLength < 16) {
                    log.info("可能需要扩大缓冲区大小");
                } else if (msgLength > 16 && msgLength == dataLength) {
                    // 其实是两个char
                    inputStream.readInt();
                    int action = inputStream.readInt() - 1;
                    // 直播间在线用户数目
                    if (action == 2) {
                        inputStream.readInt();
                        int userCount = inputStream.readInt();
                        handlerHolder.handleUserCount(userCount);
                    } else if (action == 4) {
                        inputStream.readInt();
                        int msgBodyLength = dataLength - 16;
                        byte[] msgBody = new byte[msgBodyLength];
                        if (inputStream.read(msgBody) == msgBodyLength) {
                            String jsonStr = new String(msgBody, StandardCharsets.UTF_8);
                            handlerHolder.handleCmd(jsonStr);
//                            System.out.println(jsonStr);
                            log.info(jsonStr);
                        }
                    }
                } else if (msgLength > 16 && msgLength < dataLength) {
                    byte[] singleData = new byte[msgLength];
                    System.arraycopy(data, 0, singleData, 0, msgLength);
                    analyzeData(singleData);
                    int remainLen = dataLength - msgLength;
                    byte[] remainDate = new byte[remainLen];
                    System.arraycopy(data, msgLength, remainDate, 0, remainLen);
                    analyzeData(remainDate);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
