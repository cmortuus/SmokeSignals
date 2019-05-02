public class main {
    public static void main(String[] args) {

        String mainUser = "Caleb";
        String[] otherUsers = {"Christian#234234", "Mathiew#123456"};
        try {
            if (!mainUser.matches("\\D\\W")) {
                User Caleb = new User(mainUser);
                SocialMediaFeed socialMediaFeed = new SocialMediaFeed(Caleb);
                for(String otherUser : otherUsers) {
                    if (!otherUser.matches("\\D\\W")) {
                        if (User.isValidUserFormat(otherUser)) {
                            Caleb.createRoom(otherUser);
                        }else{
                            System.out.println("That username does not mathch the form uname#123456");
                        }
                    } else {
                        System.out.println("Chose a new other username. You cannot have special chars");
                    }
                }
            } else {
                System.out.println("Chose a new main username. You cannot have special chars");
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            System.out.println("You must use a different user name. You cannot use one with special chars");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
