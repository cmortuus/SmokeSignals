package account;

import org.json.JSONObject;

public class Peer {

    private final long userid;
    private String username;
    private String discriminator;

    public Peer(long userid, String username, String discriminator) {
        this.userid = userid;
        this.username = username;
        this.discriminator = discriminator;
    }

    public Peer(JSONObject json) {
        if (!json.has("userid") || !json.has("username") || !json.has("discriminator"))
            throw new IllegalArgumentException("missing fields");
        userid = json.getLong("userid");
        username = json.getString("username");
        discriminator = json.getString("discriminator");
    }

    public long getUserId() {
        return userid;
    }

    public String getUsername() {
        return username;
    }

    public String getFullUsername() {
        return username+'#'+discriminator;
    }

    public String getDiscriminator() {
        return discriminator;
    }

    public void updateUsername(String username) {
        this.username = username;
    }

    public void updateDiscriminator(String discriminator) {
        this.discriminator = discriminator;
    }

    public JSONObject toJSONObject() {
        return new JSONObject()
                .put("userid", userid)
                .put("username", username)
                .put("discriminator", discriminator);
    }

}
