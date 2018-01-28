package com.google.firebase.codelab.friendlychat;

/**
 * Created by ammarasmro on 2018-01-27.
 */

public class FriendlyConversation {
    private String id;
    private String name;
    private String photoUrl;

    public FriendlyConversation() {
    }

    public FriendlyConversation(String text, String name, String photoUrl, String imageUrl) {
        this.name = name;
        this.photoUrl = photoUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }


}
