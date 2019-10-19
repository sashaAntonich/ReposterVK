package models;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;

public class Critic {
    final String proxyUrl = "http://api.scraperapi.com?api_key=4524cc553b16122c2a433229bc69248b&url=";

    ArrayList<Integer> movieIds;
    ArrayList<String> criticRatings;

    public Critic(int criticId) throws IOException {
        movieIds = new ArrayList<>();
        criticRatings = new ArrayList<>();
        init(criticId);
    }

    public void init(int criticId) throws IOException {
        String criticUrl = "https://www.kinopoisk.ru/user/%d/votes/list/ord/rating/page/%d/#list";
        Document doc = Jsoup.connect(String.format(criticUrl, criticId, 1)).get();

        if (checkCaptcha(doc)) {
            doc = Jsoup.connect(proxyUrl + String.format(criticUrl, criticId, 1)).get();
        }

        int ratingsCount = Integer.parseInt(doc.select("div[class='pagesFromTo']").text().split(" ")[2]);
        int pageCounter = 1;

        while (true) {
            ArrayList<Element> nameElements = doc.select("div[class='nameRus'] > a");
            ArrayList<Element> ratingElements = doc.select("div[class*='item'] > div[class='vote']");

            for (int i = 0; i < nameElements.size(); i++) {
                movieIds.add(Integer.parseInt(nameElements.get(i).attributes().get("href").split("/")[2]));
                criticRatings.add(ratingElements.get(i).text());
            }

            ratingsCount -= nameElements.size();

            if (ratingsCount > 49) {
                doc = Jsoup.connect(String.format(criticUrl, criticId, 1)).get();

                if (checkCaptcha(doc)) {
                    doc = Jsoup.connect(proxyUrl + String.format(criticUrl, criticId, ++pageCounter)).get();
                }
            } else {
                break;
            }
        }
    }

    public boolean checkCaptcha(Document doc) {
        return doc.location().contains("showcaptcha");
    }

    public int getFilmId(int index) {
        return movieIds.get(index);
    }

    public String getRating(int index) {
        return criticRatings.get(index);
    }

    public int getCount() {
        return movieIds.size();
    }
}
