package com.codingwithmitch.googlemaps2018;

import android.app.Application;

import com.codingwithmitch.googlemaps2018.models.User;


public class UserClient extends Application {

    private User user = null;

    public User getUser() {
        return user;
    }
      // set when user get authenticated.
    public void setUser(User user) {
        this.user = user;
    }

}
//lec.9 , to retrieve users from user list in a chatroom & retrieve GPS coordinates of those users.
//ChatroomActivity.java
// retrieve users id of users in the user lists from chatroom and use that to query the user location list & retrieves all GPS coordinates.
