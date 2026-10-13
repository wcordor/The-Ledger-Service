package com.github.wcordor.ledger.dtos.userDTO;

import org.openapitools.jackson.nullable.JsonNullable;

public class UserPatchDTO {

    private JsonNullable<String> firstName = JsonNullable.undefined();
    private JsonNullable<String> lastName = JsonNullable.undefined();
    /*private JsonNullable<String> username = JsonNullable.undefined();
    private JsonNullable<String> password = JsonNullable.undefined();*/

    protected UserPatchDTO() {}
    
    public UserPatchDTO(JsonNullable<String> firstName, JsonNullable<String> lastName/*,
        JsonNullable<String> username, JsonNullable<String> password*/) {
        
        this.firstName = firstName;
        this.lastName = lastName;
        /*this.username = username;
        this.password = password;*/
    }

    public JsonNullable<String> getFirstName() {
        return firstName;
    }

    public void setFirstName(JsonNullable<String> firstName) {
        this.firstName = firstName;
    }

    public JsonNullable<String> getLastName() {
        return lastName;
    }

    public void setLastName(JsonNullable<String> lastName) {
        this.lastName = lastName;
    }
/*
    public JsonNullable<String> getUsername() {
        return username;
    }

    public void setUsername(JsonNullable<String> username) {
        this.username = username;
    }

    public JsonNullable<String> getPassword() {
        return password;
    }

    public void setPassword(JsonNullable<String> password) {
        this.password = password;
    }*/

}
