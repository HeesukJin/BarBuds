package com.example.barbuds;

public class User {
    private String name;
    private String bio;
    private String profilePicturePath;

    public User(String name, String bio, String profilePicturePath){
        this.name=name;
        this.bio=bio;
        this.profilePicturePath=profilePicturePath;
    }

    public User(){
        this.name ="";
        this.bio ="";
        this.profilePicturePath="";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getProfilePicturePath() {
        return profilePicturePath;
    }

    public void setProfilePicturePath(String profilePicturePath) {
        this.profilePicturePath = profilePicturePath;
    }
}
