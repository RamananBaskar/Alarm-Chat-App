
package com.project.alarmchatbot;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class Message {
    @PrimaryKey(autoGenerate = true)
    private int id;

    public static String SENT_BY_ME = "me";
    public static String SENT_BY_BOT = "bot";


    private String message;
    private String sentBy;
    private String timestamp;

    public Message(String message, String sentBy,String timestamp) {
        this.message = message;
        this.sentBy = sentBy;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSentBy() {
        return sentBy;
    }

    public void setSentBy(String sentBy) {
        this.sentBy = sentBy;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setId(int id) {
        this.id = id;
    }

}
