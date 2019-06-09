import account.Account;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

public class FileLoader {

    public static ArrayList<Account> loadAccounts(String filename) {
        File account_file = new File(filename);
        ArrayList<Account> accounts = new ArrayList<>();
        if (!account_file.exists()) return accounts;
        if (!account_file.isFile()) throw new IllegalArgumentException("specified path is not a file");
        if (!account_file.canRead()) throw new IllegalArgumentException("missing permissions to read specified file");
        try (Scanner scn = new Scanner(account_file)) {
            StringBuilder sb = new StringBuilder();
            while (scn.hasNextLine()) sb.append(scn.nextLine());
            if (!isJson(sb.toString())) throw new IllegalArgumentException("specified file is not formatted correctly (json)");
            new JSONArray(sb.toString()).iterator().forEachRemaining(o -> accounts.add(new Account(new JSONObject(o.toString()))));
            return accounts;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return accounts;
        }
    }

    public static void saveAccounts(ArrayList<Account> accounts, String filename) throws IOException {
        File account_file = new File(filename);
        try (PrintWriter pw = new PrintWriter(account_file)) {
            JSONArray json = new JSONArray();
            for (Account a : accounts) json.put(a.toJSONObject());
            pw.print(json.toString());
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

}
