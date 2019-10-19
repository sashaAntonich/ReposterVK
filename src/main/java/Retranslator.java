import com.vk.api.sdk.callback.CallbackApi;
import com.vk.api.sdk.callback.longpoll.queries.GetLongPollEventsQuery;
import com.vk.api.sdk.callback.longpoll.responses.GetLongPollEventsResponse;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.enums.MessagesFilter;
import com.vk.api.sdk.objects.groups.LongPollServer;
import com.vk.api.sdk.objects.messages.ConversationWithMessage;
import com.vk.api.sdk.objects.messages.LongpollParams;
import com.vk.api.sdk.objects.messages.Message;
import com.vk.api.sdk.objects.photos.Photo;
import com.vk.api.sdk.objects.photos.PhotoUpload;
import com.vk.api.sdk.objects.photos.responses.MessageUploadResponse;
import com.vk.api.sdk.objects.photos.responses.WallUploadResponse;
import com.vk.api.sdk.queries.messages.MessagesGetLongPollServerQuery;
import models.Critic;
import models.Movie;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class Retranslator {
    public static void main(String[] args) throws ClientException, ApiException, InterruptedException {
        int groupId = 185318871;
        String accessToken = "b10c1fe0acda9a8b944d3ce87d14a8e57d1151b3a5516b48248d93283a9d1039fdbd4c90a99157e01f9d1";

        TransportClient transportClient = HttpTransportClient.getInstance();
        VkApiClient vk = new VkApiClient(transportClient);

        GroupActor groupActor = new GroupActor(groupId, accessToken);

        MessagesGetLongPollServerQuery longPollServer = vk.messages().getLongPollServer(groupActor);
        longPollServer.needPts(true);
        LongpollParams execute = longPollServer.execute();
        GetLongPollEventsQuery events = vk.longPoll().getEvents("https://" + execute.getServer(), execute.getKey(), execute.getTs());
        events.waitTime(25);

        while (true) {
            GetLongPollEventsResponse execute2 = events.execute();
        }
    }
}
