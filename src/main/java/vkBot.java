import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.ServiceActor;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.groups.GroupFull;
import com.vk.api.sdk.objects.users.UserXtrCounters;
import com.vk.api.sdk.objects.wall.WallPostFull;
import com.vk.api.sdk.objects.wall.WallpostAttachment;

import java.util.ArrayList;
import java.util.List;
/*
https://oauth.vk.com/authorize?client_id={group_id}&scope=photos,audio,video,docs,notes,pages,status,offers,questions,wall,groups,email,notifications,stats,ads,offline,docs,pages,stats,notifications&response_type=token

-appToken=3b48facc3b48facc3b48faccbc3b23cc6a33b483b48facc664114857d1ddbb08c116495 -appId=7026342 -accessToken=a7d7af1bbd96f8072f0daf9abc3d3b9090f6648e9d89f15d59bfeb0e6e976a25c1a5b2b213f96b9acad7a -userId=dentrav -groupId=public183613661 -groupsFromIds=public183673027,eugeneloveyou
-appToken=3b48facc3b48facc3b48faccbc3b23cc6a33b483b48facc664114857d1ddbb08c116495 -appId=7026342 -accessToken=aff56f6ec0fe682e3dbccfdf667da6b866b5343354aaf4e255faf171a9d52cc72ea0f4298637980e42478 -userId=534759816 -groupId=yandex_taxi_momentum -groupsFromIds=yandex.taxi,cabbyrus,pro.taxi,public183673027
 */


public class vkBot {
    static final Integer requestsPerDay = 1000;

    static String appToken;
    static Integer appId;

    static String accessToken;
    static Integer userId;

    static Integer groupId;
    static List<Integer> groupsFromId = new ArrayList<Integer>();

    static Integer timeoutInMS = 500;

    public static void main(String[] args) {
        try {
            TransportClient transportClient = HttpTransportClient.getInstance();
            VkApiClient vk = new VkApiClient(transportClient);

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

            ServiceActor serviceActor = new ServiceActor(appId, appToken);

            for (String arg : args) {
                if ("-accessToken".equalsIgnoreCase(arg.split("=")[0])) {
                    accessToken = arg.split("=")[1];
                }
                if ("-userId".equalsIgnoreCase(arg.split("=")[0])) {
                    userId = getUserId(vk, serviceActor, arg.split("=")[1]);
                }
                if ("-groupId".equalsIgnoreCase(arg.split("=")[0])) {
                    groupId = getGroupId(vk, serviceActor, arg.split("=")[1]);
                }
                if ("-groupsFromIds".equalsIgnoreCase(arg.split("=")[0])) {
                    for (String groupId : arg.split("=")[1].split(",")) {
                        groupsFromId.add(getGroupId(vk, serviceActor, groupId.trim()));
                    }
                }
            }


            UserActor actor = new UserActor(userId, accessToken);

            List<Integer> lastMessageIds = new ArrayList<Integer>();

            for (Integer group : groupsFromId) {
                List<WallPostFull> messages = getMessages(vk, actor, group);

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
            }

            System.out.println("Program in use");

            int getWallTimeOutInSec = (24 * 60 * 60 * groupsFromId.size()) / requestsPerDay + 5;

            System.out.println(String.format("Check groups every %d seconds", getWallTimeOutInSec));

            while (true) {
                for (int i = 0; i < groupsFromId.size(); i++) {
                    List<WallPostFull> messages = getMessages(vk, actor, groupsFromId.get(i));
                    if (messages.size() != 0) {
                        WallPostFull message = messages.get(0);

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

                            postMessage(vk, actor, message);
                        }
                    }
                }

                int waiterCount = getWallTimeOutInSec;
                while (waiterCount > 0) {
                    System.out.println(String.format("Wait now for %d sec", waiterCount));
                    waiterCount -= 5;
                    Thread.sleep(5 * 1000);
                }
            }
        } catch (ClientException | ApiException | InterruptedException e){
            System.out.println("FAIL - rerun app, reason:");
            System.out.println(e.getMessage());
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

    private static void postMessage(VkApiClient vk, UserActor actor, WallPostFull message) throws ClientException, ApiException, InterruptedException {
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
            vk.wall()
                    .post(actor)
                    .ownerId(groupId)
                    .fromGroup(true)
                    .message(message.getText())
                    .attachments(attachments)
                    .execute();
        } else {
            vk.wall()
                    .post(actor)
                    .ownerId(groupId)
                    .fromGroup(true)
                    .message(message.getText())
                    .execute();
        }
    }

    private static List<WallPostFull> getMessages(VkApiClient vk, UserActor actor, Integer group) throws ClientException, ApiException, InterruptedException {
        Thread.sleep(timeoutInMS);
        return vk.wall()
                .get(actor)
                .ownerId(group)
                .execute()
                .getItems();
    }
}
