package com.zenus.chatclient.xmpp;


import com.zenus.chatclient.model.ChatMessage;

import java.util.Observable;

public class ObservableChatMessage extends Observable {
    private ChatMessage message;

    public ChatMessage getMessage() {
        return message;
    }

    public void setMessage(ChatMessage message) {
        this.message = message;
        setChanged();
        notifyObservers();
    }
}
