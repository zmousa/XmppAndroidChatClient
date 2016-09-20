package com.zenus.chatclient;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;

import com.zenus.chatclient.model.DaoMaster;
import com.zenus.chatclient.model.DaoSession;
import com.zenus.chatclient.xmpp.LocalBinder;
import com.zenus.chatclient.xmpp.ObservableChatMessage;
import com.zenus.chatclient.xmpp.XmppService;

public class AppApplication extends Application {
    public static final String APP_TAG = "ChatClient";
    private static AppApplication instance;
    public DaoSession daoSession;
    private ObservableChatMessage observableChatMessage;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        setupDatabaseManager();
        observableChatMessage = new ObservableChatMessage();
    }

    public static synchronized AppApplication getInstance() {
        return instance;
    }

    public static Context getContext() {
        return AppApplication.getInstance().getApplicationContext();
    }

    private void setupDatabaseManager() {
        try {
            DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, "ChatClientDB.sqlite", null);
            SQLiteDatabase db = helper.getWritableDatabase();
            DaoMaster daoMaster = new DaoMaster(db);
            daoSession = daoMaster.newSession();
        } catch (Exception e){
        }
    }

    public String getUserLoginName(){
        return "user1";
    }

    public DaoSession getDaoSession() {
        return daoSession;
    }

    public DaoSession getDBSession() {
        return getInstance().getDaoSession();
    }

    private XmppService xmppService;
    private boolean xmppServiceBounded;
    private final ServiceConnection mConnection = new ServiceConnection() {

        @SuppressWarnings("unchecked")
        @Override
        public void onServiceConnected(final ComponentName name, final IBinder service) {
            xmppService = ((LocalBinder<XmppService>) service).getService();
            xmppServiceBounded = true;
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            xmppService = null;
            xmppServiceBounded = false;
        }
    };

    public void bindXmppService() {
        bindService(new Intent(this, XmppService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    public void unbindXmppService() {
        if (mConnection != null) {
            unbindService(mConnection);
        }
    }

    public boolean isXmppServiceBounded(){
        return xmppServiceBounded;
    }

    public ObservableChatMessage getObservableChatMessage() {
        return observableChatMessage;
    }
}
