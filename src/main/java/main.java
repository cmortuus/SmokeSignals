public class main {


    public static void main(String[] args) {
        SocialMediaFeed socialMediaFeed = new SocialMediaFeed();
        String mainUser = "Caleb";
        String otherUser = "Christian";
        try {
            if (!mainUser.matches("\\D\\W")) {
                User Caleb = new User(mainUser);
                if (!otherUser.matches("\\D\\W")) {
                    Caleb.createRoom(otherUser);
                } else {
                    System.out.println("Chose a new other username. You cannot have special chars");
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
