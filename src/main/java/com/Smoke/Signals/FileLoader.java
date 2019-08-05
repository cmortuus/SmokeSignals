package com.Smoke.Signals;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class FileLoader {

    private static final String ACCOUNTS_FOLDER = "accounts/";
    private static final String ACCOUNT_LOOKUP_FILE = "account_lookup.json";

    private static final String ROOMS_FOLDER = "rooms/";

    public static synchronized Account getAccount(String username, String discriminator) throws IOException {
        Account account = null;
        String accountFile = null;

        new File(ACCOUNTS_FOLDER).mkdir();
        File accountsLookupFile = new File(ACCOUNTS_FOLDER+ACCOUNT_LOOKUP_FILE);
        if (accountsLookupFile.createNewFile()) {
            account = new Account(username, discriminator);
            Map<String, String> map = new HashMap<>();
            map.put(username+'#'+discriminator, account.getSaveFilename());
            try (PrintWriter pw = new PrintWriter(accountsLookupFile)) {
                pw.println(new JSONObject(map).toString());
            }
        } else {
            try (Scanner scn = new Scanner(accountsLookupFile)) {
                StringBuilder sb = new StringBuilder();
                while (scn.hasNextLine()) sb.append(scn.nextLine());
                if (!isJson(sb.toString())) throw new RuntimeException("Account lookup file is corrupted!");
                Map<String, Object> map = new JSONObject(sb.toString()).toMap();
                if (map.containsKey(username+'#'+discriminator))
                    accountFile = String.valueOf(map.get(username+'#'+discriminator));
                else {
                    account = new Account(username, discriminator);
                    map.put(username+'#'+discriminator, account.getSaveFilename());
                    try (PrintWriter pw = new PrintWriter(accountsLookupFile)) {
                        pw.println(new JSONObject(map).toString());
                    }
                }
            }
        }

        if (accountFile != null) {
            File accountSaveData = new File(ACCOUNTS_FOLDER+accountFile);
            if (accountSaveData.exists())  {
                try (Scanner scn = new Scanner(accountSaveData)) {
                    StringBuilder sb = new StringBuilder();
                    while (scn.hasNextLine()) sb.append(scn.nextLine());
                    if (!isJson(sb.toString())) throw new RuntimeException("Account save data is corrupted!");
                    account = new Account(new JSONObject(sb.toString()));
                }
            } else throw new RuntimeException("Account save data is missing!");
        }

        return account;
    }

    public static void saveAccount(Account account) throws IOException {
        String accountFile = account.getSaveFilename();
        new File(ACCOUNTS_FOLDER).mkdir();
        File f = new File(ACCOUNTS_FOLDER+accountFile);
        f.createNewFile();
        try (PrintWriter pw = new PrintWriter(f)) {
            pw.println(account.toJSONObject().toString());
        }
    }

    /**
     * Updates the account lookup file used for retrieving the filename of each account data file.
     * This should be called whenever an account's username or discriminator changes.
     *
     * @param oldUsername (String)  old username formatted as username#discriminator
     * @param newUsername (String)  new username formatted as username#discriminator
     * @throws IOException
     */
    public static synchronized void updateAccountReference(String oldUsername, String newUsername) throws IOException {

        if (!isValidUserFormat(oldUsername)) throw new IllegalArgumentException("oldUsername is not formatted correctly (username#discriminator)");
        if (!isValidUserFormat(newUsername)) throw new IllegalArgumentException("newUsername is not formatted correctly (username#discriminator)");

        new File(ACCOUNTS_FOLDER).mkdir();
        File lookup = new File(ACCOUNTS_FOLDER+ACCOUNT_LOOKUP_FILE);
        if (!lookup.exists()) throw new RuntimeException("account lookup file does not exist!");

        try (Scanner scn = new Scanner(lookup)) {
            StringBuilder sb = new StringBuilder();
            while (scn.hasNextLine()) sb.append(scn.nextLine());
            if (!isJson(sb.toString())) throw new RuntimeException("account lookup file is corrupted");
            JSONObject json = new JSONObject(sb.toString());
            Map<String, Object> map = json.toMap();
            if (!map.containsKey(oldUsername)) throw new IllegalArgumentException("oldUsername is not registered within the account lookup file");
            if (map.containsKey(newUsername)) throw new IllegalArgumentException("newUsername is already registered within the account lookup file");
            Object value = map.remove(oldUsername);
            map.put(newUsername, value);

            // save the new references back to the lookup file
            try (PrintWriter pw = new PrintWriter(lookup)) {
                pw.println(new JSONObject(map).toString());
            }
        }
    }

    public static ArrayList<Message> loadMessages(String filename) {
        File messages_file = new File(filename);
        ArrayList<Message> messages = new ArrayList<>();
        if (!messages_file.exists()) return messages;
        if (!messages_file.isFile()) throw new IllegalArgumentException("specified path is not a file");
        if (!messages_file.canRead()) throw new IllegalArgumentException("missing permissions to read specified file");
        try (Scanner scn = new Scanner(messages_file)) {
            StringBuilder sb = new StringBuilder();
            while (scn.hasNextLine()) sb.append(scn.nextLine());
            if (!isJson(sb.toString())) throw new IllegalArgumentException("specified file is not formatted correctly (json)");
            new JSONArray(sb.toString()).iterator().forEachRemaining(o -> messages.add(new Message(new JSONObject(o.toString()))));
            return messages;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return messages;
        }
    }

    public static void saveMessages(ArrayList<Message> messages, String filename) throws IOException {
        File messages_file = new File(filename);
        try (PrintWriter pw = new PrintWriter(messages_file)) {
            JSONArray json = new JSONArray();
            for (Message m : messages) json.put(m.toJSONObject());
            pw.print(json.toString());
        }
    }

    private static boolean isJson(String s) {
        try { new JSONObject(s);
        } catch (JSONException ex) {
            try { new JSONArray(s);
            } catch (JSONException ex1) { return false; }
        } return true;
    }

    /**
     * Checks if the passed string is properly formatted as username#discriminator
     *
     * @param username name to check
     * @return true if the format is valid
     */
    private static boolean isValidUserFormat(String username) {
        return username.matches("(.+#[0-9]+)$");
    }

}
