package com.Smoke.Signals;

import javax.crypto.SecretKey;
import java.io.IOException;

class RecieveLogging extends Pubsub {

    RecieveLogging(User yourself) {
        super(yourself, "Error_Reporting", true);
    }

    @Override
    void doesEverything() {
        new Thread(() -> {
            try {
                // initiate handshake
                new Thread(() -> {
                    String lastPeers = "";
                    while (true) {
                        try {
                            String currentPeers = ipfs.pubsub.peers(roomName).toString();
                            if (!currentPeers.equals(lastPeers)) {
                                lastPeers = currentPeers;
                                debug("peers: " + currentPeers);
                            }
                        } catch (IOException ignore) {
                        }
                        if (!ready)
                            try {
                                debug("attempting to start a handshake");
                                ipfs.pubsub.pub(roomName, createOutgoingRsaText());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException ignore) {
                        }
                    }
                }).start();

                // write out each line of the stream to a file and check if they are one of the users
                room.forEach(stringObjectMap -> {
                    if (stringObjectMap.isEmpty()) return;
                    try {

                        String base64Data = stringObjectMap.values().toString().split(",")[1].trim();
                        byte[] decodedBytes = MyBase64.decode(base64Data);
                        String decodedString = new String(decodedBytes).replaceAll(" ", "+");

                        String sender = stringObjectMap.toString().split(",", 2)[0].substring(6);

                        Pair<SecretKey, String> authorAesKey = yourself.getUserAesKey(sender);
                        if (authorAesKey == null) { // if we have not received an aes key from the author yet then perform a handshake
                            shakeHands(sender, decodedString);
                            return;
                        }

                        String decryptedMessage;
                        try {
                            decryptedMessage = Encryption.decrypt(decodedString, authorAesKey.getKey(), authorAesKey.getValue());
                        } catch (Exception e) {
                            shakeHands(sender, decodedString);
                            return;
                        }

                        if (!isJson(decryptedMessage)) return;
                        Message message = parseMessage(decryptedMessage);
                        if (account.getPeer(message.getAuthorId()).getDiscriminator().equals("000000"))
                            writeToPubsub(String.valueOf(message.getAuthorId()), MessageType.IDENTITY_REQUEST);

//                        Insted of the switch statment
                        if (message.getMessageType().equals(MessageType.ERROR)) {
                            messages.add(message);
                            messageLookup.put(message.getMessageId(), message);
                            saveMessages();

                            System.out.println(message.getMessageId() + "  " +
                                    getTime(message.getTimestampLong()) + "  " +
                                    account.getPeer(message.getAuthorId()).getUsername() + "  " +
                                    message.getContent());
                        }

                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
