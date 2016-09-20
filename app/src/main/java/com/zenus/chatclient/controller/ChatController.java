package com.zenus.chatclient.controller;


import com.zenus.chatclient.AppApplication;
import com.zenus.chatclient.model.ChatMessage;
import com.zenus.chatclient.model.ChatMessageDao;
import com.zenus.chatclient.model.ChatRoom;
import com.zenus.chatclient.model.ChatRoomDao;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ChatController {
    public static List<ChatRoom> getChatRooms(){
        return AppApplication.getInstance().getDaoSession().getChatRoomDao().queryBuilder()
                .orderDesc(ChatRoomDao.Properties.LastModifiedDate)
                .list();
    }

    public static List<ChatMessage> getTopChatMessages(long room){
        List<ChatMessage> result = AppApplication.getInstance().getDaoSession().getChatMessageDao().queryBuilder()
                .where(ChatMessageDao.Properties.ChatRoomId.eq(room))
                .where(ChatMessageDao.Properties.IsNew.eq(true))
                .orderAsc(ChatMessageDao.Properties.CreateDate)
                .list();

        if (result != null && result.size() < 10)
            result = AppApplication.getInstance().getDaoSession().getChatMessageDao().queryBuilder()
                    .where(ChatMessageDao.Properties.ChatRoomId.eq(room))
                    .limit(10)
                    .orderAsc(ChatMessageDao.Properties.CreateDate)
                    .list();
        return result;
    }

    public static List<ChatMessage> getTopChatMessages(long room, int number, int page){
        List<ChatMessage> result;
        if (page == 0) {
            result = AppApplication.getInstance().getDaoSession().getChatMessageDao().queryBuilder()
                    .where(ChatMessageDao.Properties.ChatRoomId.eq(room))
                    .where(ChatMessageDao.Properties.IsNew.eq(true))
                    .orderDesc(ChatMessageDao.Properties.CreateDate)
                    .list();

            if (result != null && result.size() < number)
                result = AppApplication.getInstance().getDaoSession().getChatMessageDao().queryBuilder()
                        .where(ChatMessageDao.Properties.ChatRoomId.eq(room))
                        .orderDesc(ChatMessageDao.Properties.CreateDate)
                        .limit(number)
                        .list();
        } else {
            result = AppApplication.getInstance().getDaoSession().getChatMessageDao().queryBuilder()
                    .where(ChatMessageDao.Properties.ChatRoomId.eq(room))
                    .orderDesc(ChatMessageDao.Properties.CreateDate)
                    .offset(number * page)
                    .limit(number)
                    .list();
        }
        Collections.reverse(result);
        return result;
    }

    public static List<ChatMessage> getNewChatMessages(long room){
        List<ChatMessage> result = AppApplication.getInstance().getDaoSession().getChatMessageDao().queryBuilder()
                .where(ChatMessageDao.Properties.ChatRoomId.eq(room))
                .where(ChatMessageDao.Properties.IsNew.eq(true))
                .orderAsc(ChatMessageDao.Properties.CreateDate)
                .list();

        return result;
    }

    public static long saveChatMessage(ChatMessage chatMessage){
        long rowId = AppApplication.getInstance().getDBSession().getChatMessageDao().insertOrReplace(chatMessage);
        changeRoomLastModifyDate(chatMessage.getChatRoomId(), chatMessage.getCreateDate());
        return rowId;
    }

    public static void deleteChatMessage(ChatMessage chatMessage){
        AppApplication.getInstance().getDBSession().getChatMessageDao().delete(chatMessage);
    }

    public static void addIfNotExistChatRoom(long chatRoomId, String chatRoomName, String username){
        ChatRoom chatRoom = AppApplication.getInstance().getDBSession().getChatRoomDao().load(chatRoomId);
        if (chatRoom == null){
            ChatRoom newItem = new ChatRoom();
            newItem.setId(chatRoomId);
            newItem.setGroupName(chatRoomName);
            newItem.setIsGroup(true);
            if (chatRoomName.isEmpty()) {
                newItem.setUserName(username);
                newItem.setIsGroup(false);
                newItem.setLastModifiedDate(Calendar.getInstance().getTime());
            }
            AppApplication.getInstance().getDBSession().getChatRoomDao().insertOrReplace(newItem);
        }
    }

    public static void changeRoomLastModifyDate(long roomId, Date lastModifyDate){
        ChatRoom chatRoom = AppApplication.getInstance().getInstance().getDaoSession().getChatRoomDao().load(roomId);
        chatRoom.setLastModifiedDate(lastModifyDate);
        AppApplication.getInstance().getInstance().getDaoSession().getChatRoomDao().update(chatRoom);
    }

    public static int getNumberOfNewMessageForRoom(long room){
        List<ChatMessage> result = AppApplication.getInstance().getDaoSession().getChatMessageDao().queryBuilder()
                .where(ChatMessageDao.Properties.ChatRoomId.eq(room))
                .where(ChatMessageDao.Properties.IsNew.eq(true))
                .orderDesc(ChatMessageDao.Properties.CreateDate)
                .list();

        return result != null? result.size() : 0;
    }

    public static void markNewMessagesAsRead(long room){
        List<ChatMessage> result = AppApplication.getInstance().getDaoSession().getChatMessageDao().queryBuilder()
                .where(ChatMessageDao.Properties.ChatRoomId.eq(room))
                .where(ChatMessageDao.Properties.IsNew.eq(true))
                .list();

        if (result != null && result.size() > 0) {
            for (ChatMessage chatMessage : result){
                chatMessage.setIsNew(false);
                AppApplication.getInstance().getInstance().getDaoSession().getChatMessageDao().update(chatMessage);
            }
        }
    }

    public static String getLastMessageForRoom(long room){
        List<ChatMessage> result = AppApplication.getInstance().getDaoSession().getChatMessageDao().queryBuilder()
                .where(ChatMessageDao.Properties.ChatRoomId.eq(room))
                .orderDesc(ChatMessageDao.Properties.CreateDate)
                .limit(1)
                .list();

        return result != null && result.size() > 0? result.get(0).getMessage() : "";
    }

    public static void fillTestData(){
        List<ChatRoom> result = getChatRooms();
        if (result != null && result.size() >0)
            return;

        ChatRoom chatRoom1 = new ChatRoom(new Long(1), "Group1", "", true, new java.util.Date());
        ChatRoom chatRoom2 = new ChatRoom(new Long(2), "Group2", "", true, new java.util.Date());
        ChatRoom chatRoom3 = new ChatRoom(new Long(3), "", "admin", false, new java.util.Date());

        AppApplication.getInstance().getDBSession().getChatRoomDao().insertOrReplace(chatRoom1);
        AppApplication.getInstance().getDBSession().getChatRoomDao().insertOrReplace(chatRoom2);
        AppApplication.getInstance().getDBSession().getChatRoomDao().insertOrReplace(chatRoom3);

        ChatMessage chatMessage1 = new ChatMessage(new Long(1), "message1", "zico", new java.util.Date(), false, new Long(1));
        ChatMessage chatMessage2 = new ChatMessage(new Long(2), "message2", "admin", new java.util.Date(), false, new Long(1));
        ChatMessage chatMessage3 = new ChatMessage(new Long(3), "message3", "zico", new java.util.Date(), true, new Long(1));

        ChatMessage chatMessage4 = new ChatMessage(new Long(4), "message4", "zico", new java.util.Date(), false, new Long(2));
        ChatMessage chatMessage5 = new ChatMessage(new Long(5), "message5", "admin", new java.util.Date(), false, new Long(2));

        ChatMessage chatMessage6 = new ChatMessage(new Long(6), "message6", "admin", new java.util.Date(), true, new Long(3));

        AppApplication.getInstance().getDBSession().getChatMessageDao().insertOrReplace(chatMessage1);
        AppApplication.getInstance().getDBSession().getChatMessageDao().insertOrReplace(chatMessage2);
        AppApplication.getInstance().getDBSession().getChatMessageDao().insertOrReplace(chatMessage3);
        AppApplication.getInstance().getDBSession().getChatMessageDao().insertOrReplace(chatMessage4);
        AppApplication.getInstance().getDBSession().getChatMessageDao().insertOrReplace(chatMessage5);
        AppApplication.getInstance().getDBSession().getChatMessageDao().insertOrReplace(chatMessage6);
    }

    public static long getChatRoomByUsername(String to, boolean isGroup) {
        List<ChatRoom> result = AppApplication.getInstance().getDaoSession().getChatRoomDao().queryBuilder()
                .whereOr(ChatRoomDao.Properties.UserName.eq(to), ChatRoomDao.Properties.GroupName.eq(to))
                .limit(1)
                .list();
        if (result != null && result.size() > 0){
            return result.get(0).getId();
        } else {
            ChatRoom chatRoom = new ChatRoom();
            chatRoom.setIsGroup(isGroup);
            if (isGroup)
                chatRoom.setGroupName(to);
            else
                chatRoom.setUserName(to);
            chatRoom.setLastModifiedDate(new java.util.Date());

            return AppApplication.getInstance().getDBSession().getChatRoomDao().insertOrReplace(chatRoom);
        }
    }
}
