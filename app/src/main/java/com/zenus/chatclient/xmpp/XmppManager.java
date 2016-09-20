package com.zenus.chatclient.xmpp;

import android.os.AsyncTask;
import android.util.Log;

import com.zenus.chatclient.AppApplication;
import com.zenus.chatclient.controller.ChatController;
import com.zenus.chatclient.model.ChatMessage;
import com.zenus.chatclient.util.Logger;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.TLSUtils;
import org.jivesoftware.smackx.ping.PingFailedListener;
import org.jivesoftware.smackx.receipts.DeliveryReceipt;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptRequest;
import org.jivesoftware.smackx.receipts.ReceiptReceivedListener;

import java.util.Calendar;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import de.duenndns.ssl.MemorizingTrustManager;

public class XmppManager implements ConnectionListener, StanzaListener, PingFailedListener {
    public static XmppManager instance = null;
    private String serverAddress;
    private String loginUser;
    private String passwordUser;
    private  XMPPTCPConnection mXmpptcpConnection;
    private  XMPPConnection mXmppConnection;
    private final int XMPP_PORT = 5222;

    public static XmppManager getInstance() {
        if (instance == null)
            instance = new XmppManager();
        return instance;
    }

    public XmppManager() {
        initSettings();
        setConnection();
    }

    private void initSettings(){
        serverAddress = "ec2-00-00-00-00.us-west-2.compute.amazonaws.com";
        loginUser = "user";
        passwordUser = "pass";
    }

    private void setConnection() {
        Logger.log("Xmpp", "SetXMPP Connection");
        try {
            XMPPTCPConnectionConfiguration.Builder connectionConfiguration = XMPPTCPConnectionConfiguration.builder();
            connectionConfiguration.setServiceName(serverAddress);
            connectionConfiguration.setPort(XMPP_PORT);
            connectionConfiguration.setHost(serverAddress);
            Logger.log("Xmpp", "Username :" + loginUser + " Password:" + passwordUser);
            connectionConfiguration.setUsernameAndPassword(loginUser, passwordUser);
            connectionConfiguration.setSecurityMode(ConnectionConfiguration.SecurityMode.ifpossible);
            connectionConfiguration.setDebuggerEnabled(true);
            connectionConfiguration.setCompressionEnabled(true);
            TLSUtils.acceptAllCertificates(connectionConfiguration);

//            connectionConfiguration.setResource("ChatApp");

            SSLContext sc = SSLContext.getInstance("TLS");
            MemorizingTrustManager mtm = new MemorizingTrustManager(AppApplication.getContext());
            sc.init(null, new X509TrustManager[]{mtm}, new java.security.SecureRandom());
            connectionConfiguration.setCustomSSLContext(sc);
            connectionConfiguration.setHostnameVerifier(
                    mtm.wrapHostnameVerifier(new org.apache.http.conn.ssl.StrictHostnameVerifier()));

            mXmpptcpConnection = new XMPPTCPConnection(connectionConfiguration.build());
            mXmpptcpConnection.addAsyncStanzaListener(this, new AcceptAll());
            mXmpptcpConnection.addConnectionListener(this);
            mXmpptcpConnection.setPacketReplyTimeout(30000);
            ProviderManager.addExtensionProvider(DeliveryReceipt.ELEMENT, DeliveryReceipt.NAMESPACE, new DeliveryReceipt.Provider());
            ProviderManager.addExtensionProvider(DeliveryReceiptRequest.ELEMENT, new DeliveryReceiptRequest().getNamespace(), new DeliveryReceiptRequest.Provider());

            AsyncTask<Void, Void, Boolean> connectionThread = new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected synchronized Boolean doInBackground(Void... arg0) {

                    try {
                        mXmpptcpConnection.connect();
                    } catch (Exception e) {
                        Logger.log("Xmpp", "XMPP Connection exception :" + e.getMessage());
                    }
                    return true;
                }
            };
            connectionThread.execute();

        } catch (Exception ex) {
            Logger.log("Xmpp", "XMPP Connection exception :" + ex.getMessage());
        }
    }

    @Override
    public void connected(XMPPConnection connection) {
        Logger.log("Xmpp", "connected");
        mXmppConnection = connection;
        try {
            mXmpptcpConnection.login();
        } catch (Exception e) {
            e.printStackTrace();
        }
        DeliveryReceiptManager.setDefaultAutoReceiptMode(DeliveryReceiptManager.AutoReceiptMode.always);
        DeliveryReceiptManager.getInstanceFor(connection).autoAddDeliveryReceiptRequests();
        DeliveryReceiptManager.getInstanceFor(connection).addReceiptReceivedListener(new ReceiptReceivedListener() {
            @Override
            public void onReceiptReceived(String fromJid, String toJid, String receiptId, Stanza receipt) {
                Logger.log("Xmpp", "onReceiptReceived :" + fromJid + "-> " + toJid + " : " + receiptId);
                DeliveryReceipt receiptdata = receipt.getExtension(DeliveryReceipt.ELEMENT, DeliveryReceipt.NAMESPACE);

                if (receiptdata == null) {
                    return;
                }
                //TODO
//                notifyAllonReceiptRecived((Message)receipt);
            }
        });

    }

    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {
        Logger.log("Xmpp", "authenticated");
        mXmppConnection = connection;
    }

    @Override
    public void connectionClosed() {

    }

    @Override
    public void connectionClosedOnError(Exception e) {

    }

    @Override
    public void reconnectionSuccessful() {

    }

    @Override
    public void reconnectingIn(int seconds) {

    }

    @Override
    public void reconnectionFailed(Exception e) {

    }

    public String sendMessage(String message, String to)
    {
        to = to + "@" + serverAddress;
        Log.i("XMPPClient", "Sending text [" + message + "] to [" + to + "]");
        Message msg = new Message(to, Message.Type.chat);
        msg.setBody(message);
        msg.addExtension(new DeliveryReceipt(msg.getPacketID()));
        try{
            mXmppConnection.sendStanza(msg);
            DeliveryReceiptManager.addDeliveryReceiptRequest(msg);
        }catch (SmackException.NotConnectedException ex){
            Logger.log("Xmpp", "Send message exception:" + ex.getMessage());
        }
        return msg.getStanzaId();
    }

    @Override
    public void processPacket(Stanza stanza) throws SmackException.NotConnectedException {
        if (stanza instanceof Message){
            Message message = (Message) stanza;
            Logger.log("Xmpp", "process Packet Message : " + message.getTo() + ":" + message.getFrom() + ":" + message.getBody());

            if (message.getBody() != null) {
                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setMessage(message.getBody());
                chatMessage.setCreateDate(Calendar.getInstance().getTime());
                chatMessage.setIsNew(true);
                chatMessage.setUserName(getUserNameFromFullName(message.getFrom()));
                chatMessage.setChatRoomId(ChatController.getChatRoomByUsername(getUserNameFromFullName(message.getFrom()), message.getType().equals(Message.Type.groupchat)));
                ChatController.saveChatMessage(chatMessage);
                AppApplication.getInstance().getObservableChatMessage().setMessage(chatMessage);
            }
        }
    }

    private String getUserNameFromFullName(String fullName){
        if (fullName.contains("@"))
            return fullName.split("@")[0];
        return fullName;
    }

    @Override
    public void pingFailed() {

    }

    static class AcceptAll implements StanzaFilter {
        @Override
        public boolean accept(Stanza packet) {
            return true;
        }
    }

    public void disconnect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mXmpptcpConnection.disconnect();
            }
        }).start();
    }
}
