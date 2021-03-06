package com.hartz.inventory.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.hartz.inventory.SharedPrefsHelper;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by hrtz on 12/26/2016.
 */

public class User {
    private String name;
    private String role;
    private String token;


    /**
     * create new user object from json
     * @param jsonResponse json text to be made into object
     * @return true if success, false if not
     */
    public boolean createNew(String jsonResponse, Context context){
        try {
            JSONObject userObject = new JSONObject(jsonResponse);
            if(userObject.getBoolean("error")) return false;
            name = userObject.getString("id");
            role = userObject.getString("role");
            token = userObject.getString("token");
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(SharedPrefsHelper.NAME_PREFS, name);
            editor.putString(SharedPrefsHelper.LAST_NAME_PREFS, name);
            editor.putString(SharedPrefsHelper.ROLE_PREFS, role);
            editor.putString(SharedPrefsHelper.TOKEN_PREFS, token);
            editor.apply();
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
