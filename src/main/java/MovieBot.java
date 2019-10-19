import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.client.actors.ServiceActor;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.enums.MessagesFilter;
import com.vk.api.sdk.objects.groups.GroupFull;
import com.vk.api.sdk.objects.messages.ConversationWithMessage;
import com.vk.api.sdk.objects.photos.Photo;
import com.vk.api.sdk.objects.photos.PhotoUpload;
import com.vk.api.sdk.objects.photos.responses.MessageUploadResponse;
import com.vk.api.sdk.objects.photos.responses.WallUploadResponse;
import com.vk.api.sdk.objects.users.UserXtrCounters;
import com.vk.api.sdk.objects.wall.WallpostAttachment;
import com.vk.api.sdk.objects.wall.WallpostFull;
import models.Critic;
import models.Movie;

import java.io.IOException;
import java.util.*;
/*
User:
https://oauth.vk.com/authorize?client_id={app_id}&scope=photos,audio,video,docs,notes,pages,status,offers,questions,wall,groups,email,notifications,stats,ads,offline,docs,pages,stats,notifications&response_type=token

Group:
https://oauth.vk.com/authorize?client_id=7075211&group_ids=184947586&scope=stories,photos,app_widget,messages&response_type=token

-appToken=3b48facc3b48facc3b48faccbc3b23cc6a33b483b48facc664114857d1ddbb08c116495 -appId=7026342 -accessToken=a7d7af1bbd96f8072f0daf9abc3d3b9090f6648e9d89f15d59bfeb0e6e976a25c1a5b2b213f96b9acad7a -userId=dentrav -groupId=public183613661 -groupsFromIds=public183673027,eugeneloveyou
-appToken=3b48facc3b48facc3b48faccbc3b23cc6a33b483b48facc664114857d1ddbb08c116495 -appId=7026342 -accessToken=aff56f6ec0fe682e3dbccfdf667da6b866b5343354aaf4e255faf171a9d52cc72ea0f4298637980e42478 -userId=534759816 -groupId=yandex_taxi_momentum -groupsFromIds=yandex.taxi,cabbyrus,pro.taxi -emailsToNotify=travin.denis.const@gmail.com,Stuffing.1@yandex.ru
-appToken=b4f5f575b4f5f575b4f5f575c7b49e6f11bb4f5b4f5f575e9d3e19d49e753427045c479 -appId=7051876 -accessToken=09a24d3b840b86300058f05dc77b51ebc6a63da01f9b5530dcd97910e82f48aa0dfe298fa5cf80f5fd285 -userId=534759816 -groupId=vpiski_saint_p -groupsFromIds=vpisssska,vpiskaofficial,byhaipim -emailsToNotify=travin.denis.const@gmail.com,Stuffing.1@yandex.ru
для фильмов: 0d1419606de99e66d49b301aba208b4a38525fef4cae0ed41e35fc19db30891b3f3ba0b3863a28681a523


я для фильмов: cc8d7b7f33722054bf4e59726607acaa3172e7c44bb36135a3978061ee99ec9a773c5b3660cf8c184188c
 */


public class MovieBot {
    public static void main(String[] args) throws ClientException, ApiException {
        int groupId = 184947586;
        int userId = 87642881;
        String accessToken = "0d1419606de99e66d49b301aba208b4a38525fef4cae0ed41e35fc19db30891b3f3ba0b3863a28681a523";
        String userAccessToken = "cc8d7b7f33722054bf4e59726607acaa3172e7c44bb36135a3978061ee99ec9a773c5b3660cf8c184188c";

        TransportClient transportClient = HttpTransportClient.getInstance();
        VkApiClient vk = new VkApiClient(transportClient);

        GroupActor groupActor = new GroupActor(groupId, accessToken);
        UserActor actor = new UserActor(userId, userAccessToken);

        Critic critic;

        try {
            critic = new Critic(2092256);
        } catch (IOException e) {
            throw new AssertionError("Все в пезде");
        }

        int criticSize = critic.getCount();
        int lastFilmId = 0;
        Date lastPostDate = new Date();

        while (true) {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Date currentDate = new Date();
            if (lastFilmId < criticSize && (lastFilmId == 0 ||
                    currentDate.getTime() - lastPostDate.getTime() > 1000 * 60 * 60)) {

                try {
                    Movie movieToPost = new Movie(critic.getFilmId(lastFilmId));

                    PhotoUpload serverResponse = vk.photos().getWallUploadServer(actor).execute();
                    WallUploadResponse uploadResponse = vk.upload().photoWall(serverResponse.getUploadUrl().toString(), movieToPost.getMovieImageFile()).execute();
                    List<Photo> photoList = vk.photos().saveWallPhoto(actor, uploadResponse.getPhoto())
                            .server(uploadResponse.getServer())
                            .hash(uploadResponse.getHash())
                            .execute();

                    Photo photo = photoList.get(0);
                    String attachId = "photo" + photo.getOwnerId() + "_" + photo.getId();


                    StringBuilder messageToAnswer = new StringBuilder(movieToPost.getMovieName() + "\n" +
                            "Рейтинг кинопоиска: " + movieToPost.getMovieRating() + "\n" +
                            "Рейтинг Папани: " + critic.getRating(lastFilmId) + "\n" +
                            "Режисер: " + movieToPost.getMovieDirector() + "\n" +
                            "Описание: \n" + movieToPost.getMovieDescription() + "\n");

                    for (String genre : movieToPost.getMovieGenre().split(",")) {
                        messageToAnswer.append("#ArthasMovie_").append(genre.trim().toUpperCase()).append("\ngetConversations");
                    }

                    vk.wall()
                            .post(actor)
                            .ownerId(-groupId)
                            .fromGroup(true)
                            .message(messageToAnswer.toString())
                            .attachments(attachId)
                            .execute();

                } catch (IOException e) {
                    System.out.println("Постер сломался");
                }

                lastPostDate = currentDate;
                lastFilmId++;
            }
            List<ConversationWithMessage> conversations = vk.messages().getConversations(groupActor).filter(MessagesFilter.UNANSWERED).execute().getItems();

            for (ConversationWithMessage conversation : conversations) {
                String messageToAnswer = "Вы ввели что-то плохое или я сломался :(";
                int sendToId = conversation.getLastMessage().getFromId();

                String lastMessageText = conversation.getLastMessage().getText();

                if (lastMessageText != null && !lastMessageText.isEmpty() && lastMessageText.matches("[0-9A-z :,.А-я]+")) {
                    try {
                        Movie movie = new Movie(lastMessageText.trim());
                        messageToAnswer = movie.getMovieName() + "\n" +
                                 "Жанр: " + movie.getMovieGenre() + "\n" +
                                "Рейтинг: " + movie.getMovieRating() + "\n" +
                            "Режисер: " + movie.getMovieDirector() + "\n" +
                            "Описание: \n" + movie.getMovieDescription();


                        PhotoUpload serverResponse = vk.photos().getMessagesUploadServer(groupActor).execute();
                        MessageUploadResponse uploadResponse = vk.upload().photoMessage(serverResponse.getUploadUrl().toString(), movie.getMovieImageFile()).execute();
                        List<Photo> photoList = vk.photos().saveMessagesPhoto(groupActor, uploadResponse.getPhoto())
                                .server(uploadResponse.getServer())
                                .hash(uploadResponse.getHash())
                                .execute();

                        Photo photo = photoList.get(0);
                        String attachId = "photo" + photo.getOwnerId() + "_" + photo.getId();
                        vk.messages().send(groupActor).userId(sendToId).randomId(new Random().nextInt()).message(messageToAnswer).attachment(attachId).execute();
                        break;

                    } catch (Exception e) {
                        messageToAnswer = "Не могу найти такой фильм";
                    }
                }

                vk.messages().send(groupActor).userId(sendToId).randomId(new Random().nextInt()).message(messageToAnswer).execute();
            }
        }

    }
}
