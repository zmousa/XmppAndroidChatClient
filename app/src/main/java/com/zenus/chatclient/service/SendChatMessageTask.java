package com.zenus.chatclient.service;

import android.os.AsyncTask;

import com.zenus.chatclient.AppApplication;
import com.zenus.chatclient.R;
import com.zenus.chatclient.model.ChatMessage;
import com.zenus.chatclient.util.Logger;
import com.zenus.chatclient.xmpp.XmppManager;

public class SendChatMessageTask extends AsyncTask<Void, String, Void> {
    private SendChatMessageListener sendChatMessageListener;
    private ChatMessage chatMessage;
    private static final String t = SendChatMessageTask.class.getName();
    private String[] msg = null;
    private boolean callbackReceived;

    public SendChatMessageTask(ChatMessage chatMessage){
        this.chatMessage = chatMessage;
    }
    
    @Override
    protected Void doInBackground(Void... params) {
        String id = XmppManager.getInstance().sendMessage(chatMessage.getMessage(), "admin");
        callbackReceived = true;
        if (id == null || "".equals(id)) {
            SendChatMessageTask.this.msg = new String[]{AppApplication.getInstance().getString(R.string.send_message_failed)};
        }

        if (msg != null && msg.length > 0 && !"".equals(msg[0])) {
            Logger.log(t, msg[0]);
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void arg) {
        synchronized (this) {
            if (sendChatMessageListener != null) {
                if (msg != null && msg.length > 0)
                    sendChatMessageListener.taskFailed(msg[0]);
                else
                    sendChatMessageListener.taskComplete();
            }
        }
    }

    public void setSendChatMessageListener(SendChatMessageListener sendChatMessageListener) {
        synchronized (this) {
            this.sendChatMessageListener = sendChatMessageListener;
        }
    }
}
