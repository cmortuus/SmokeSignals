import java.io.IOException;

public class main {
    public static void main(String[] args) {

//        try {
//            User me = new User("Christian");
//            String room = me.joinExistingRoom("Caleb#962250");
//            me.sendToRoom(room, "Did it work?");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        try {
            User me = new User("Caleb");
            String room = me.joinExistingRoom("Christian#234234");
        } catch (IOException e) {
            e.printStackTrace();
        }

//        String mainUser = "Caleb";
//        String[] otherUsers = {"Christian#234234", "Mathiew#123456"};
//        try {
//            if (!mainUser.matches("\\D\\W")) {
//                User Caleb = new User(mainUser);
//                SocialMediaFeed socialMediaFeed = new SocialMediaFeed(Caleb);
//                for(String otherUser : otherUsers) {
//                    if (!otherUser.matches("\\D\\W")) {
//                        if (User.isValidUserFormat(otherUser)) {
//                            Caleb.createRoom(otherUser);
//                        }else{
//                            System.out.println("That username does not mathch the form uname#123456");
//                        }
//                    } else {
//                        System.out.println("Chose a new other username. You cannot have special chars");
//                    }
//                }
//            } else {
//                System.out.println("Chose a new main username. You cannot have special chars");
//            }
//        } catch (IllegalArgumentException e) {
//            e.printStackTrace();
//            System.out.println("You must use a different user name. You cannot use one with special chars");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }
}
