import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.ServiceActor;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.groups.GroupFull;
import com.vk.api.sdk.objects.users.UserXtrCounters;
import com.vk.api.sdk.objects.wall.WallpostAttachment;
import com.vk.api.sdk.objects.wall.WallpostFull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
/*
User:
https://oauth.vk.com/authorize?client_id=7026342&scope=photos,audio,video,docs,notes,pages,status,offers,questions,wall,groups,email,notifications,stats,ads,offline,docs,pages,stats,notifications&response_type=token

Group:
https://oauth.vk.com/authorize?client_id=7075211&group_ids=184947586&scope=stories,photos,app_widget,messages&response_type=token

-appToken=3b48facc3b48facc3b48faccbc3b23cc6a33b483b48facc664114857d1ddbb08c116495 -appId=7026342 -accessToken=a7d7af1bbd96f8072f0daf9abc3d3b9090f6648e9d89f15d59bfeb0e6e976a25c1a5b2b213f96b9acad7a -userId=dentrav -groupId=public183613661 -groupsFromIds=public183673027,eugeneloveyou
-appToken=3b48facc3b48facc3b48faccbc3b23cc6a33b483b48facc664114857d1ddbb08c116495 -appId=7026342 -accessToken=271c4283afac92eed96c9582a2ecc7361f6a41b2d67d1503b6557ed858902a57196e44a07ac5975af9ee8 -userId=534759816 -groupId=yandex_taxi_momentum -groupsFromIds=yandex.taxi,cabbyrus,pro.taxi -emailsToNotify=travin.denis.const@gmail.com,Stuffing.1@yandex.ru
-appToken=b4f5f575b4f5f575b4f5f575c7b49e6f11bb4f5b4f5f575e9d3e19d49e753427045c479 -appId=7051876 -accessToken=09a24d3b840b86300058f05dc77b51ebc6a63da01f9b5530dcd97910e82f48aa0dfe298fa5cf80f5fd285 -userId=534759816 -groupId=vpiski_saint_p -groupsFromIds=vpisssska,vpiskaofficial,byhaipim -emailsToNotify=travin.denis.const@gmail.com,Stuffing.1@yandex.ru
для фильмов: 0d1419606de99e66d49b301aba208b4a38525fef4cae0ed41e35fc19db30891b3f3ba0b3863a28681a523

retranslator: d4ebfe3a350591035172cff4f247e63559aad0f0518ebbb80f2d7ecde7df1fcdf01406e0f88addd7e9e50
 */


public class vkBot {
    static final Integer requestsPerDay = 500;

    static String appToken;
    static Integer appId;

    static String accessToken;
    static Integer userId;

    static Integer groupId;
    static String groupIdString;

    static List<Integer> groupsFromId = new ArrayList<Integer>();

    static List<String> emailsToNotify = new ArrayList<>();

    static Integer timeoutInMS = 500;

    static TransportClient transportClient;
    static VkApiClient vk;
    static List<Integer> lastMessageIds;
    static UserActor actor;
    static int getWallTimeOutInSec;
    static ServiceActor serviceActor;

    public static void main(String[] args) {
        try {
            transportClient = HttpTransportClient.getInstance();
            vk = new VkApiClient(transportClient);

            for (String arg : args) {
                if ("-appToken".equalsIgnoreCase(arg.split("=")[0])) {
                    appToken = arg.split("=")[1];
                }
                if ("-appId".equalsIgnoreCase(arg.split("=")[0])) {
                    appId = Integer.parseInt(arg.split("=")[1]);
                }
            }

            if (appToken == null || appId == null) {
                System.out.println("Invalid application token or id input");
                return;
            }

            serviceActor = new ServiceActor(appId, appToken);

            for (String arg : args) {
                if ("-accessToken".equalsIgnoreCase(arg.split("=")[0])) {
                    accessToken = arg.split("=")[1];
                }
                if ("-userId".equalsIgnoreCase(arg.split("=")[0])) {
                    userId = getUserId(vk, serviceActor, arg.split("=")[1]);
                }
                if ("-groupId".equalsIgnoreCase(arg.split("=")[0])) {
                    groupIdString = arg.split("=")[1];
                    groupId = getGroupId(vk, serviceActor, arg.split("=")[1]);
                }
                if ("-groupsFromIds".equalsIgnoreCase(arg.split("=")[0])) {
                    for (String groupId : arg.split("=")[1].split(",")) {
                        groupsFromId.add(getGroupId(vk, serviceActor, groupId.trim()));
                    }
                }
                if ("-emailsToNotify".equalsIgnoreCase(arg.split("=")[0])) {
                    Collections.addAll(emailsToNotify, arg.split("=")[1].split(","));
                }
            }


            actor = new UserActor(userId, accessToken);

            lastMessageIds = new ArrayList<Integer>();

            for (Integer group : groupsFromId) {
                try {
                    List<WallpostFull> messages = getMessages(vk, actor, group);

                    if (messages.size() == 0) {
                        lastMessageIds.add(-1);
                    } else {
                        if (messages.get(0).getIsPinned() == null) {
                            lastMessageIds.add(messages.get(0).getId());
                        } else {
                            if (messages.size() == 1) {
                                lastMessageIds.add(-1);
                            } else {
                                lastMessageIds.add(messages.get(1).getId());
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println(String.format("Fail for %d group", group));
                }
            }

            System.out.println("Program in use");

            getWallTimeOutInSec = (24 * 60 * 60 * groupsFromId.size()) / requestsPerDay + 5;

            System.out.println(String.format("Check groups every %d seconds", getWallTimeOutInSec));

        } catch (ClientException | ApiException | InterruptedException e) {
            System.out.println("FATAL FAIL, reason: ");
            System.out.println(e.getMessage());
            for (String email : emailsToNotify) {
                MailSender.SendMail(email,
                        String.format("BOT for %s group CRITICAL FAIL", groupIdString),
                        String.format("Critical fail, manual rerun needed, reason: %s", e.getMessage()));
            }
            throw new RuntimeException(e);
        }

        for (String email : emailsToNotify) {
            MailSender.SendMail(email,
                    String.format("BOT for %s group normal start", groupIdString),
                    "Bot start working correct");
        }
        while (true) {
            try {
                for (int i = 0; i < groupsFromId.size(); i++) {
                    try {
                        List<WallpostFull> messages = getMessages(vk, actor, groupsFromId.get(i));
                        if (messages.size() != 0) {
                            WallpostFull message = messages.get(0);

                            boolean flagToPost = false;

                            if (message.getIsPinned() == null) {
                                flagToPost = true;
                            } else {
                                if (messages.size() != 1) {
                                    flagToPost = true;
                                    message = messages.get(1);
                                }
                            }

                            if (flagToPost && !lastMessageIds.get(i).equals(message.getId())) {
                                lastMessageIds.set(i, message.getId());

                                if (!message.isMarkedAsAds()) {
                                    postMessage(vk, actor, message);
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.out.println(String.format("Fail for %d group", groupsFromId.get(i)));
                    }
                }

                int waiterCount = getWallTimeOutInSec;
                while (waiterCount > 0) {
                    System.out.println(String.format("Wait now for %d sec", waiterCount));
                    waiterCount -= 5;
                    Thread.sleep(5 * 1000);
                }

            } catch (InterruptedException e) {
                System.out.println("COMMON FAIL - rerun app, reason: ");
                System.out.println(e.getMessage());
                try {
                    Thread.sleep(5 * 60 * 1000);
                } catch (InterruptedException gh) {
                    // Ignored
                }
                for (String email : emailsToNotify) {
                    MailSender.SendMail(email,
                            String.format("BOT for %s group FAIL", groupIdString),
                            String.format("Common fail, auto rerun try, reason: %s", e.getMessage()));
                }
            }
        }
    }

    private static Integer getUserId(VkApiClient vk, ServiceActor actor, String userId) throws ClientException, ApiException, InterruptedException {
        Thread.sleep(timeoutInMS);
        if (userId.matches("[0-9]+")) {
            return Integer.parseInt(userId);
        } else {
            List<UserXtrCounters> users = vk.users()
                    .get(actor)
                    .userIds(userId)
                    .execute();
            if (users.size() != 1) {
                throw new AssertionError("Invalid user id");
            } else {
                return users.get(0).getId();
            }
        }
    }

    private static Integer getGroupId(VkApiClient vk, ServiceActor actor, String groupId) throws ClientException, ApiException, InterruptedException {
        Thread.sleep(timeoutInMS);
        if (groupId.matches("[public]+[0-9]+")) {
            return - Integer.parseInt(groupId.replaceAll("public", ""));
        } else {
            List<GroupFull> groups = vk.groups()
                    .getById(actor)
                    .groupIds(groupId)
                    .execute();
            if (groups.size() != 1) {
                throw new AssertionError("Invalid group id");
            } else {
                return - groups.get(0).getId();
            }
        }
    }

    private static void postMessage(VkApiClient vk, UserActor actor, WallpostFull message) throws ClientException, ApiException, InterruptedException {
        Thread.sleep(timeoutInMS);
        System.out.println("\n\nPost message:\n" + message.getText());

        if (message.getAttachments() != null &&
                message.getAttachments().size() != 0) {

            List<String> attachments = new ArrayList<String>();
            for (WallpostAttachment attachment : message.getAttachments()) {
                switch (attachment.getType()) {
                    case PHOTO:
                        attachments.add(String.format("photo%s_%s",
                                attachment.getPhoto().getOwnerId(),
                                attachment.getPhoto().getId()));
                        break;
                    case VIDEO:
                        attachments.add(String.format("video%s_%s",
                                attachment.getVideo().getOwnerId(),
                                attachment.getVideo().getId()));
                        break;
                    case AUDIO:
                        attachments.add(String.format("audio%s_%s",
                                attachment.getAudio().getOwnerId(),
                                attachment.getAudio().getId()));
                        break;
                    case POLL:
                        attachments.add(String.format("poll%s_%s",
                                attachment.getPoll().getOwnerId(),
                                attachment.getPoll().getId()));
                        break;
                    case POSTED_PHOTO:
                        attachments.add(String.format("posted_photo%s_%s",
                                attachment.getPostedPhoto().getOwnerId(),
                                attachment.getPostedPhoto().getId()));
                        break;
                    case ALBUM:
                        attachments.add(String.format("album%s_%s",
                                attachment.getAlbum().getOwnerId(),
                                attachment.getAlbum().getId()));
                        break;
                    case LINK:
                        attachments.add(attachment.getLink().toString());
                        break;
                }
            }
            if (message.getText().isEmpty() && attachments.size() == 0) {
                System.out.println("It's a shitty trap!");
            } else {
                vk.wall()
                        .post(actor)
                        .ownerId(groupId)
                        .fromGroup(true)
                        .message(message.getText().replaceAll("(?:\\s|\\A)[##]+([A-Za-z0-9-_А-Яа-я@.]+)", ""))
                        .attachments(attachments)
                        .execute();
            }
        } else {
            if (message.getText().isEmpty()) {
                System.out.println("It's a repost trap!");
            } else {
                vk.wall()
                        .post(actor)
                        .ownerId(groupId)
                        .fromGroup(true)
                        .message(message.getText().replaceAll("(?:\\s|\\A)[##]+([A-Za-z0-9-_А-Яа-я@.]+)", ""))
                        .execute();
            }
        }
    }

    private static List<WallpostFull> getMessages(VkApiClient vk, UserActor actor, Integer group) throws ClientException, ApiException, InterruptedException {
        Thread.sleep(timeoutInMS);
        return vk.wall()
                .get(actor)
                .ownerId(group)
                .count(2)
                .execute()
                .getItems();
    }
}
