package com.project.alarmchatbot;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MessageDao {
    @Insert
    void insert(Message message);

    @Query("SELECT * FROM messages ORDER BY id ASC")
    List<Message> getAllMessages();
}
